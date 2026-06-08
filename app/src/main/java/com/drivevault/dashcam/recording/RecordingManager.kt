package com.drivevault.dashcam.recording

import android.content.ContentValues
import android.content.Context
import android.media.MediaScannerConnection
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.camera.core.*
import androidx.camera.core.ConcurrentCamera.SingleCameraConfig
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.*
import androidx.camera.video.VideoCapture
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.drivevault.dashcam.data.local.DriveVaultDatabase
import com.drivevault.dashcam.data.local.entity.ClipEntity
import com.drivevault.dashcam.data.local.entity.LocationSampleEntity
import com.drivevault.dashcam.data.local.entity.HeadingSampleEntity
import com.drivevault.dashcam.data.repository.SettingsRepository
import com.drivevault.dashcam.firebase.DriveVaultFirebase
import com.drivevault.dashcam.sensors.TelemetryManager
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

class RecordingManager(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner
) {

    private val db by lazy { DriveVaultDatabase.getInstance(context) }
    private val clipDao by lazy { db.clipDao() }
    private val locationSampleDao by lazy { db.locationSampleDao() }
    private val headingSampleDao by lazy { db.headingSampleDao() }
    private val settings by lazy { SettingsRepository(context) }

    private val telemetryManager by lazy {
        TelemetryManager(context, LocationServices.getFusedLocationProviderClient(context))
    }

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private var cameraProvider: ProcessCameraProvider? = null
    private var videoCapture: VideoCapture<Recorder>? = null
    private var secondaryVideoCapture: VideoCapture<Recorder>? = null
    private var currentRecording: Recording? = null
    private var secondaryRecording: Recording? = null
    private var currentClipId: Long = 0L
    private var currentFile: File? = null
    private var secondaryFile: File? = null
    private var primaryFinalize: CompletableDeferred<Boolean>? = null
    private var secondaryFinalize: CompletableDeferred<Boolean>? = null
    private var isRecording = false
    private var clipStartTime = 0L
    private val locationSamples = mutableListOf<LocationSampleEntity>()
    private val headingSamples = mutableListOf<HeadingSampleEntity>()

    private val _state = MutableStateFlow(RecordingState())
    fun initializeCamera(provider: ProcessCameraProvider) {
        cameraProvider = provider
    }

    fun startRecording(): Flow<RecordingState> = callbackFlow {
        isRecording = true
        telemetryManager.start()

        val stateJob = launch {
            _state.collect { state ->
                trySend(state)
            }
        }

        scope.launch {
            settings.audioEnabled.collectLatest { audio ->
                _state.update { it.copy(audioEnabled = audio) }
            }
        }
        scope.launch {
            settings.defaultCameraMode.collectLatest { mode ->
                _state.update { it.copy(cameraMode = mode) }
            }
        }

        launch {
            while (isRecording) {
                val startTime = System.currentTimeMillis()
                clipStartTime = startTime
                _state.update { it.copy(isRecording = true, currentClipStartTime = startTime, currentClipElapsed = 0L) }
                try {
                    startNewClip()
                } catch (error: Exception) {
                    Log.e("RecordingManager", "startNewClip failed", error)
                    DriveVaultFirebase.recordNonFatal("Recording clip start failed", error)
                    _state.update { it.copy(isRecording = false, error = error.message ?: "Recording could not start") }
                    isRecording = false
                    break
                }

                repeat(60) { second ->
                    if (!isRecording) return@repeat
                    delay(1000)
                    _state.update { it.copy(currentClipElapsed = (System.currentTimeMillis() - startTime)) }
                    collectTelemetrySample()
                }

                finishCurrentClip()
            }
            close()
        }

        awaitClose {
            isRecording = false
            stateJob.cancel()
            telemetryManager.stop()
        }
    }

    private suspend fun startNewClip() {
        Log.d("RecordingManager", "startNewClip mode=${_state.value.cameraMode}")
        locationSamples.clear()
        headingSamples.clear()

        val dateFormat = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US)
        val stamp = dateFormat.format(Date())
        val fileName = "DriveVault_${stamp}.mp4"
        val clipsDir = File(context.filesDir, "clips").apply { mkdirs() }
        currentFile = File(clipsDir, fileName)
        secondaryFile = null
        primaryFinalize = CompletableDeferred()
        secondaryFinalize = null

        val primaryQualitySelector = qualitySelectorForMode(_state.value.cameraMode)
        val recorder = Recorder.Builder()
            .setQualitySelector(primaryQualitySelector)
            .build()
        videoCapture = VideoCapture.withOutput(recorder)
        secondaryVideoCapture = null

        try {
            val primaryCapture = videoCapture ?: return
            DriveVaultFirebase.logRecordingStarted(
                cameraMode = _state.value.cameraMode,
                dualStreamRequested = _state.value.cameraMode == "DUAL"
            )
            if (_state.value.cameraMode == "DUAL") {
                Log.d("RecordingManager", "binding dual video capture")
                secondaryFile = File(clipsDir, "DriveVault_${stamp}_front.mp4")
                secondaryFinalize = CompletableDeferred()
                secondaryVideoCapture = VideoCapture.withOutput(
                    Recorder.Builder()
                        .setQualitySelector(dualCameraQualitySelector())
                        .build()
                )
                try {
                    bindDualVideoCaptureUseCases(
                        primaryCapture = primaryCapture,
                        secondaryCapture = secondaryVideoCapture ?: return
                    )
                } catch (dualError: Exception) {
                    Log.e("RecordingManager", "dual recording bind failed", dualError)
                    DriveVaultFirebase.recordNonFatal("Dual recording bind failed", dualError)
                    secondaryVideoCapture = null
                    secondaryFile = null
                    secondaryFinalize = null
                    _state.update {
                        it.copy(
                            cameraMode = "BACK",
                            error = "Dual recording unavailable on this device/session. Recording rear camera only."
                        )
                    }
                    bindSingleVideoCaptureUseCase(primaryCapture)
                }
            } else {
                Log.d("RecordingManager", "binding single video capture")
                bindSingleVideoCaptureUseCase(primaryCapture)
            }

            Log.d("RecordingManager", "starting primary recording")
            currentRecording = primaryCapture.output
                .prepareRecording(context, FileOutputOptions.Builder(currentFile!!).build())
                ?.apply {
                    val audioEnabled = _state.value.audioEnabled
                    if (audioEnabled) {
                        try { withAudioEnabled() } catch (_: Exception) {}
                    }
                }
                ?.start(ContextCompat.getMainExecutor(context)) { recordEvent ->
                    when (recordEvent) {
                        is VideoRecordEvent.Start -> Log.d("RecordingManager", "primary recording started")
                        is VideoRecordEvent.Finalize -> {
                            Log.d(
                                "RecordingManager",
                                "primary recording finalized hasError=${recordEvent.hasError()} error=${recordEvent.error} cause=${recordEvent.cause?.message}"
                            )
                            primaryFinalize?.complete(!recordEvent.hasError())
                        }
                        else -> {}
                    }
                }

            val secondaryCapture = secondaryVideoCapture
            val secondaryOut = secondaryFile
            if (secondaryCapture != null && secondaryOut != null) {
                Log.d("RecordingManager", "starting secondary recording")
                secondaryRecording = secondaryCapture.output
                    .prepareRecording(context, FileOutputOptions.Builder(secondaryOut).build())
                    .start(ContextCompat.getMainExecutor(context)) { recordEvent ->
                        when (recordEvent) {
                            is VideoRecordEvent.Start -> Log.d("RecordingManager", "secondary recording started")
                            is VideoRecordEvent.Finalize -> {
                                Log.d(
                                    "RecordingManager",
                                    "secondary recording finalized hasError=${recordEvent.hasError()} error=${recordEvent.error} cause=${recordEvent.cause?.message}"
                                )
                                secondaryFinalize?.complete(!recordEvent.hasError())
                            }
                            else -> {}
                        }
                    }
            }
        } catch (e: Exception) {
            Log.e("RecordingManager", "Recording start failed", e)
            DriveVaultFirebase.recordNonFatal("Recording start failed", e)
            _state.update { it.copy(error = e.message) }
            primaryFinalize?.complete(false)
            secondaryFinalize?.complete(false)
        }
    }

    private suspend fun bindSingleVideoCaptureUseCase(capture: VideoCapture<Recorder>) {
        val provider = cameraProvider ?: awaitCameraProvider().also { cameraProvider = it }
        val selector = when (_state.value.cameraMode) {
            "FRONT" -> CameraSelector.DEFAULT_FRONT_CAMERA
            else -> CameraSelector.DEFAULT_BACK_CAMERA
        }

        withContext(Dispatchers.Main) {
            provider.unbindAll()
            provider.bindToLifecycle(lifecycleOwner, selector, capture)
        }
    }

    private fun qualitySelectorForMode(cameraMode: String): QualitySelector {
        return if (cameraMode == "DUAL") {
            dualCameraQualitySelector()
        } else {
            QualitySelector.from(
                Quality.HIGHEST,
                FallbackStrategy.lowerQualityOrHigherThan(Quality.SD)
            )
        }
    }

    private fun dualCameraQualitySelector(): QualitySelector {
        return QualitySelector.fromOrderedList(
            listOf(Quality.FHD, Quality.HD, Quality.SD),
            FallbackStrategy.lowerQualityOrHigherThan(Quality.SD)
        )
    }

    private suspend fun bindDualVideoCaptureUseCases(
        primaryCapture: VideoCapture<Recorder>,
        secondaryCapture: VideoCapture<Recorder>
    ) {
        val provider = cameraProvider ?: awaitCameraProvider().also { cameraProvider = it }
        if (!provider.supportsFrontBackConcurrentCamera()) {
            throw IllegalStateException("Concurrent front/back camera is not supported")
        }
        withContext(Dispatchers.Main) {
            provider.unbindAll()
            provider.bindToLifecycle(
                listOf(
                    SingleCameraConfig(
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        UseCaseGroup.Builder().addUseCase(primaryCapture).build(),
                        lifecycleOwner
                    ),
                    SingleCameraConfig(
                        CameraSelector.DEFAULT_FRONT_CAMERA,
                        UseCaseGroup.Builder().addUseCase(secondaryCapture).build(),
                        lifecycleOwner
                    )
                )
            )
        }
    }

    private fun ProcessCameraProvider.supportsFrontBackConcurrentCamera(): Boolean {
        return availableConcurrentCameraInfos.any { group ->
            val lensFacings = group.map { it.lensFacing }.toSet()
            lensFacings.contains(CameraSelector.LENS_FACING_BACK) &&
                lensFacings.contains(CameraSelector.LENS_FACING_FRONT)
        }
    }

    private suspend fun awaitCameraProvider(): ProcessCameraProvider = suspendCoroutine { continuation ->
        val future = ProcessCameraProvider.getInstance(context)
        future.addListener({
            try {
                continuation.resume(future.get())
            } catch (error: Exception) {
                continuation.resumeWithException(error)
            }
        }, ContextCompat.getMainExecutor(context))
    }

    private suspend fun finishCurrentClip() {
        Log.d("RecordingManager", "finishCurrentClip")
        try { secondaryRecording?.stop() } catch (_: Exception) {}
        try { currentRecording?.stop() } catch (_: Exception) {}
        val primaryOk = primaryFinalize?.awaitOrFalse() ?: false
        val secondaryOk = secondaryFinalize?.awaitOrFalse() ?: true
        currentRecording = null
        secondaryRecording = null
        if (primaryOk) {
            if (!secondaryOk) {
                secondaryFile = null
                _state.update { it.copy(error = "Secondary camera recording failed; saved primary camera clip.") }
            }
            saveClipToDatabase()
            DriveVaultFirebase.logRecordingStopped(
                cameraMode = _state.value.cameraMode,
                durationMillis = System.currentTimeMillis() - clipStartTime,
                hasSecondaryStream = secondaryFile?.exists() == true
            )
        }
    }

    private suspend fun CompletableDeferred<Boolean>.awaitOrFalse(): Boolean {
        return withTimeoutOrNull(5_000L) { await() } ?: false
    }

    private suspend fun saveClipToDatabase() {
        val file = currentFile ?: return
        val endTime = System.currentTimeMillis()
        val loc = locationSamples
        val head = headingSamples

        val startLoc = loc.firstOrNull()
        val endLoc = loc.lastOrNull()
        val speeds = loc.map { it.speedMps.toDouble() }
        val avgSpeed = if (speeds.isNotEmpty()) speeds.average() else 0.0
        val maxSpeed = speeds.maxOrNull() ?: 0.0

        val entity = ClipEntity(
            fileUri = file.absolutePath,
            secondaryFileUri = secondaryFile?.takeIf { it.exists() }?.absolutePath,
            startTimeMillis = clipStartTime,
            endTimeMillis = endTime,
            durationMillis = endTime - clipStartTime,
            cameraMode = _state.value.cameraMode,
            fileSizeBytes = file.length() + (secondaryFile?.takeIf { it.exists() }?.length() ?: 0L),
            startLatitude = startLoc?.latitude ?: 0.0,
            startLongitude = startLoc?.longitude ?: 0.0,
            endLatitude = endLoc?.latitude ?: 0.0,
            endLongitude = endLoc?.longitude ?: 0.0,
            averageSpeedMps = avgSpeed,
            maxSpeedMps = maxSpeed,
            startHeadingDegrees = head.firstOrNull()?.headingDegrees ?: 0f,
            endHeadingDegrees = head.lastOrNull()?.headingDegrees ?: 0f,
            audioEnabled = _state.value.audioEnabled,
            overlayEnabled = _state.value.overlayEnabled,
            miniMapEnabled = _state.value.miniMapEnabled,
            locked = _state.value.locked
        )

        currentClipId = clipDao.insert(entity)
        if (loc.isNotEmpty()) locationSampleDao.insertAll(loc.map { it.copy(clipId = currentClipId) })
        if (head.isNotEmpty()) headingSampleDao.insertAll(head.map { it.copy(clipId = currentClipId) })
    }

    private fun collectTelemetrySample() {
        val state = telemetryManager.telemetryState.value
        state.location?.let { loc ->
            locationSamples.add(
                LocationSampleEntity(
                    clipId = currentClipId,
                    timestampMillis = loc.timestampMillis,
                    latitude = loc.latitude,
                    longitude = loc.longitude,
                    altitude = loc.altitude,
                    accuracy = loc.accuracy,
                    speedMps = loc.speedMps,
                    bearingDegrees = loc.bearing
                )
            )
        }
        state.heading?.let { head ->
            headingSamples.add(
                HeadingSampleEntity(
                    clipId = currentClipId,
                    timestampMillis = head.timestampMillis,
                    headingDegrees = head.headingDegrees,
                    source = head.source.name
                )
            )
        }
    }

    suspend fun stopRecording() {
        isRecording = false
        finishCurrentClip()
        telemetryManager.stop()
        scope.cancel()
        _state.value = RecordingState()
    }

    fun lockCurrentClip() {
        _state.update { it.copy(locked = !it.locked) }
    }

    fun getCurrentTelemetry() = telemetryManager.telemetryState
}
