package com.drivevault.dashcam.ui.screens

import android.content.res.Configuration
import android.graphics.Paint
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.camera.core.CameraSelector
import androidx.camera.core.ConcurrentCamera.SingleCameraConfig
import androidx.camera.core.Preview
import androidx.camera.core.UseCaseGroup
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.core.content.ContextCompat
import com.drivevault.dashcam.permissions.PermissionManager
import com.drivevault.dashcam.recording.DetectedObjectResult
import com.drivevault.dashcam.ui.components.*
import com.drivevault.dashcam.ui.theme.DeepCharcoal
import com.drivevault.dashcam.ui.theme.SafetyRed
import com.drivevault.dashcam.ui.theme.SurfaceContainerHighest
import com.drivevault.dashcam.ui.theme.OnSurface
import com.drivevault.dashcam.ui.theme.OnSurfaceVariant
import com.drivevault.dashcam.ui.theme.ElectricBlue
import com.drivevault.dashcam.ui.theme.SurfaceContainerHigh
import com.drivevault.dashcam.ui.viewmodel.RecordingScreenUiState
import com.drivevault.dashcam.ui.viewmodel.RecordingViewModel
import kotlinx.coroutines.delay
import kotlin.coroutines.resume
import kotlin.math.max

@Composable
fun RecordingScreen(
    viewModel: RecordingViewModel,
    onNavigateToSettings: () -> Unit,
    onNavigateToLibrary: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var dualSupported by remember { mutableStateOf<Boolean?>(null) }
    var cameraMessage by remember { mutableStateOf<String?>(null) }
    var chromeVisible by remember { mutableStateOf(true) }
    var missingRequiredPermissions by remember {
        mutableStateOf(PermissionManager.getMissingPermissions(context))
    }
    val permissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions()
    ) {
        missingRequiredPermissions = PermissionManager.getMissingPermissions(context)
    }

    LaunchedEffect(missingRequiredPermissions) {
        if (PermissionManager.isPermissionGranted(context, android.Manifest.permission.CAMERA)) {
            val provider = awaitProcessCameraProvider(context)
            dualSupported = provider.supportsFrontBackConcurrentCamera()
        }
    }

    LaunchedEffect(uiState.cameraMode, dualSupported) {
        if (uiState.cameraMode == "DUAL" && dualSupported == false) {
            cameraMessage = "Dual camera is not supported by this device/session. Falling back to rear camera."
            viewModel.setCameraMode("BACK")
        } else if (uiState.cameraMode == "DUAL") {
            cameraMessage = "Dual mode records rear and front camera streams when supported by this device."
        }
    }

    LaunchedEffect(uiState.recordingState.isRecording) {
        chromeVisible = !uiState.recordingState.isRecording
    }

    LaunchedEffect(uiState.recordingState.isRecording, chromeVisible) {
        if (uiState.recordingState.isRecording && chromeVisible) {
            delay(4_000L)
            chromeVisible = false
        }
    }

    val tapToRevealModifier = if (uiState.recordingState.isRecording && !chromeVisible) {
        Modifier.pointerInput(Unit) {
            detectTapGestures {
                chromeVisible = true
            }
        }
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DeepCharcoal)
            .then(tapToRevealModifier)
    ) {
        if (PermissionManager.isPermissionGranted(context, android.Manifest.permission.CAMERA)) {
            CameraPreview(
                cameraMode = uiState.cameraMode,
                isRecording = uiState.recordingState.isRecording,
                dualSupported = dualSupported == true,
                lifecycleOwner = lifecycleOwner,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            PermissionRecoveryPanel(
                missingPermissions = missingRequiredPermissions,
                onRequestPermissions = {
                    permissionLauncher.launch(PermissionManager.REQUIRED_PERMISSIONS.toTypedArray())
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        val shouldShowChrome = !uiState.recordingState.isRecording || chromeVisible

        if (uiState.vehicleDetectionEnabled && uiState.detectedObjects.isNotEmpty()) {
            DetectionOverlay(
                detectedObjects = uiState.detectedObjects,
                modifier = Modifier.fillMaxSize()
            )
        }

        if (uiState.showOverlay && shouldShowChrome) {
            RecordingOverlay(
                uiState = uiState,
                isLandscape = isLandscape,
                cameraMessage = uiState.recordingState.error ?: cameraMessage,
                modifier = Modifier.fillMaxSize()
            )
        }

        if (shouldShowChrome) {
            RecordingControls(
                uiState = uiState,
                isLandscape = isLandscape,
                onToggleRecording = {
                    val missing = PermissionManager.getMissingRecordingStartPermissions(context)
                    if (missing.isEmpty()) {
                        viewModel.toggleRecording()
                    } else {
                        permissionLauncher.launch(missing.toTypedArray())
                    }
                },
                onLock = viewModel::lockClip,
                onToggleAudio = { viewModel.setAudioEnabled(!uiState.audioEnabled) },
                onToggleOverlay = viewModel::toggleOverlay,
                onToggleMiniMap = viewModel::toggleMiniMap,
                onCameraModeChange = viewModel::setCameraMode,
                dualSupported = dualSupported == true,
                onNavigateToSettings = onNavigateToSettings,
                onNavigateToLibrary = onNavigateToLibrary,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            MinimalRecordingProgress(
                progress = uiState.recordingState.progressFraction,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}

@Composable
fun CameraPreview(
    cameraMode: String,
    isRecording: Boolean,
    dualSupported: Boolean,
    lifecycleOwner: androidx.lifecycle.LifecycleOwner,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val cameraSelector = when (cameraMode) {
        "FRONT" -> CameraSelector.DEFAULT_FRONT_CAMERA
        else -> CameraSelector.DEFAULT_BACK_CAMERA
    }
    var primaryPreviewView by remember { mutableStateOf<PreviewView?>(null) }
    var secondaryPreviewView by remember { mutableStateOf<PreviewView?>(null) }

    Box(modifier = modifier) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                PreviewView(ctx).also { primaryPreviewView = it }
            }
        )

        if (cameraMode == "DUAL" && dualSupported && !isRecording) {
            Surface(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(top = 72.dp, end = 24.dp)
                    .widthIn(min = 180.dp, max = 260.dp)
                    .aspectRatio(4f / 3f),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                color = DeepCharcoal,
                tonalElevation = 2.dp
            ) {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { ctx ->
                        PreviewView(ctx).also { secondaryPreviewView = it }
                    }
                )
            }
        }

        LaunchedEffect(cameraMode, dualSupported, isRecording, primaryPreviewView, secondaryPreviewView) {
            if (isRecording) return@LaunchedEffect
            if (cameraMode == "DUAL" && dualSupported) {
                bindConcurrentPreview(
                    context = context,
                    lifecycleOwner = lifecycleOwner,
                    primaryPreviewView = primaryPreviewView,
                    secondaryPreviewView = secondaryPreviewView
                )
            } else {
                bindSinglePreview(
                    context = context,
                    lifecycleOwner = lifecycleOwner,
                    previewView = primaryPreviewView,
                    cameraSelector = cameraSelector
                )
            }
        }
    }
}

private suspend fun bindSinglePreview(
    context: android.content.Context,
    lifecycleOwner: androidx.lifecycle.LifecycleOwner,
    previewView: PreviewView?,
    cameraSelector: CameraSelector
) {
    val primary = previewView ?: return
    val provider = awaitProcessCameraProvider(context)
    val preview = Preview.Builder().build().also {
        it.setSurfaceProvider(primary.surfaceProvider)
    }
    withContextOnMain {
        provider.unbindAll()
        provider.bindToLifecycle(lifecycleOwner, cameraSelector, preview)
    }
}

private suspend fun bindConcurrentPreview(
    context: android.content.Context,
    lifecycleOwner: androidx.lifecycle.LifecycleOwner,
    primaryPreviewView: PreviewView?,
    secondaryPreviewView: PreviewView?
) {
    val primary = primaryPreviewView ?: return
    val secondary = secondaryPreviewView ?: return
    val provider = awaitProcessCameraProvider(context)
    val backPreview = Preview.Builder().build().also {
        it.setSurfaceProvider(primary.surfaceProvider)
    }
    val frontPreview = Preview.Builder().build().also {
        it.setSurfaceProvider(secondary.surfaceProvider)
    }
    withContextOnMain {
        provider.unbindAll()
        provider.bindToLifecycle(
            listOf(
                SingleCameraConfig(
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    UseCaseGroup.Builder().addUseCase(backPreview).build(),
                    lifecycleOwner
                ),
                SingleCameraConfig(
                    CameraSelector.DEFAULT_FRONT_CAMERA,
                    UseCaseGroup.Builder().addUseCase(frontPreview).build(),
                    lifecycleOwner
                )
            )
        )
    }
}

private suspend fun awaitProcessCameraProvider(context: android.content.Context): ProcessCameraProvider =
    kotlinx.coroutines.suspendCancellableCoroutine { continuation ->
        val future = ProcessCameraProvider.getInstance(context)
        future.addListener({
            try {
                continuation.resume(future.get())
            } catch (error: Exception) {
                continuation.cancel(error)
            }
        }, ContextCompat.getMainExecutor(context))
    }

private fun ProcessCameraProvider.supportsFrontBackConcurrentCamera(): Boolean {
    return availableConcurrentCameraInfos.any { group ->
        val lensFacings = group.map { it.lensFacing }.toSet()
        lensFacings.contains(CameraSelector.LENS_FACING_BACK) &&
            lensFacings.contains(CameraSelector.LENS_FACING_FRONT)
    }
}

private suspend fun withContextOnMain(block: () -> Unit) {
    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) { block() }
}

@Composable
private fun DetectionOverlay(
    detectedObjects: List<DetectedObjectResult>,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(modifier = modifier) {
        val density = LocalDensity.current
        val viewWidthPx = with(density) { maxWidth.toPx() }
        val viewHeightPx = with(density) { maxHeight.toPx() }

        Canvas(modifier = Modifier.fillMaxSize()) {
            detectedObjects.forEach { obj ->
                // Swap dimensions for 90°/270° rotations (sensor is landscape, display may be portrait)
                val (effectiveW, effectiveH) = when (obj.imageRotationDegrees) {
                    90, 270 -> Pair(obj.imageHeight.toFloat(), obj.imageWidth.toFloat())
                    else -> Pair(obj.imageWidth.toFloat(), obj.imageHeight.toFloat())
                }
                val scale = max(viewWidthPx / effectiveW, viewHeightPx / effectiveH)
                val offsetX = (viewWidthPx - effectiveW * scale) / 2f
                val offsetY = (viewHeightPx - effectiveH * scale) / 2f

                // Rotate bounding box coordinates from image space to display space
                val (bl, bt, br, bb) = when (obj.imageRotationDegrees) {
                    90 -> listOf(
                        obj.imageHeight - obj.boundingBox.bottom,
                        obj.boundingBox.left,
                        obj.imageHeight - obj.boundingBox.top,
                        obj.boundingBox.right
                    )
                    270 -> listOf(
                        obj.boundingBox.top,
                        obj.imageWidth - obj.boundingBox.right,
                        obj.boundingBox.bottom,
                        obj.imageWidth - obj.boundingBox.left
                    )
                    180 -> listOf(
                        obj.imageWidth - obj.boundingBox.right,
                        obj.imageHeight - obj.boundingBox.bottom,
                        obj.imageWidth - obj.boundingBox.left,
                        obj.imageHeight - obj.boundingBox.top
                    )
                    else -> listOf(
                        obj.boundingBox.left, obj.boundingBox.top,
                        obj.boundingBox.right, obj.boundingBox.bottom
                    )
                }

                val screenLeft = bl * scale + offsetX
                val screenTop = bt * scale + offsetY
                val screenRight = br * scale + offsetX
                val screenBottom = bb * scale + offsetY

                val boxColor = if (obj.label == "Vehicle") SafetyRed else ElectricBlue

                drawRect(
                    color = boxColor,
                    topLeft = Offset(screenLeft, screenTop),
                    size = Size(screenRight - screenLeft, screenBottom - screenTop),
                    style = Stroke(width = 4f)
                )

                val labelText = "${obj.label} ${(obj.confidence * 100).toInt()}%"
                val textPaint = Paint().apply {
                    color = Color.White.toArgb()
                    textSize = 32f
                    isFakeBoldText = true
                    setShadowLayer(4f, 0f, 0f, Color.Black.toArgb())
                }
                val bgPaint = Paint().apply {
                    color = boxColor.copy(alpha = 0.7f).toArgb()
                }
                val textWidth = textPaint.measureText(labelText)
                val textHeight = textPaint.textSize
                drawContext.canvas.nativeCanvas.apply {
                    drawRect(
                        screenLeft, screenTop - textHeight - 4f,
                        screenLeft + textWidth + 8f, screenTop,
                        bgPaint
                    )
                    drawText(labelText, screenLeft + 4f, screenTop - 6f, textPaint)
                }
            }
        }
    }
}

@Composable
private fun PermissionRecoveryPanel(
    missingPermissions: List<String>,
    onRequestPermissions: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.background(DeepCharcoal),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .padding(24.dp)
                .widthIn(max = 420.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Settings,
                contentDescription = null,
                modifier = Modifier.size(56.dp),
                tint = SafetyRed
            )
            Text(
                text = "Permissions needed",
                style = MaterialTheme.typography.headlineSmall,
                color = OnSurface
            )
            Text(
                text = if (missingPermissions.isEmpty()) {
                    "Camera permission is required before DriveVault can show preview or record."
                } else {
                    "Grant ${missingPermissions.joinToString { it.substringAfterLast('.') }} to enable dashcam recording."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = OnSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Button(
                onClick = onRequestPermissions,
                colors = ButtonDefaults.buttonColors(containerColor = SafetyRed)
            ) {
                Text("Grant permissions")
            }
        }
    }
}

@Composable
fun RecordingOverlay(
    uiState: RecordingScreenUiState,
    isLandscape: Boolean,
    cameraMessage: String?,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        if (isLandscape) {
            Row(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .statusBarsPadding()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top
            ) {
                RecordingIndicator(isRecording = uiState.recordingState.isRecording)
                if (uiState.showTimestamp) {
                    TimestampOverlayCard(
                        timestamp = uiState.recordingState.currentClipStartTime
                            .takeIf { it > 0 }
                            ?: System.currentTimeMillis()
                    )
                }
            }

            if (uiState.showGps) {
                GpsOverlayCard(
                    telemetry = uiState.telemetryState,
                    showCoordinates = uiState.showGps,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .statusBarsPadding()
                        .padding(horizontal = 24.dp, vertical = 16.dp)
                        .widthIn(max = 260.dp)
                )
            }

            Row(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .navigationBarsPadding()
                    .padding(start = 24.dp, bottom = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                if (uiState.showSpeed) {
                    SpeedOverlayCard(
                        telemetry = uiState.telemetryState,
                        speedUnit = uiState.speedUnit,
                        modifier = Modifier.widthIn(min = 150.dp)
                    )
                }
                if (uiState.showHeading) {
                    HeadingOverlayCard(telemetry = uiState.telemetryState)
                }
            }

            val loc = uiState.telemetryState.location
            if (uiState.showMiniMap && loc != null) {
                com.drivevault.dashcam.map.MiniMapOverlay(
                    latitude = loc.latitude,
                    longitude = loc.longitude,
                    heading = uiState.telemetryState.displayHeading,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .navigationBarsPadding()
                        .padding(end = 24.dp, bottom = 24.dp)
                        .size(220.dp, 150.dp)
                )
            }
        } else {
            Row(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 10.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    RecordingIndicator(isRecording = uiState.recordingState.isRecording)
                    if (uiState.showTimestamp) {
                        TimestampOverlayCard(
                            timestamp = uiState.recordingState.currentClipStartTime
                                .takeIf { it > 0 }
                                ?: System.currentTimeMillis()
                        )
                    }
                }
                if (uiState.showGps) {
                    GpsOverlayCard(
                        telemetry = uiState.telemetryState,
                        showCoordinates = uiState.showGps,
                        modifier = Modifier.widthIn(max = 210.dp)
                    )
                }
            }

            val loc2 = uiState.telemetryState.location
            if (uiState.showMiniMap && loc2 != null) {
                com.drivevault.dashcam.map.MiniMapOverlay(
                    latitude = loc2.latitude,
                    longitude = loc2.longitude,
                    heading = uiState.telemetryState.displayHeading,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(horizontal = 16.dp)
                        .fillMaxWidth()
                        .height(132.dp)
                )
            }

            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(start = 16.dp, end = 16.dp, bottom = 210.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                if (uiState.showSpeed) {
                    SpeedOverlayCard(
                        telemetry = uiState.telemetryState,
                        speedUnit = uiState.speedUnit,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                if (uiState.showHeading) {
                    HeadingOverlayCard(
                        telemetry = uiState.telemetryState,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                }
            }
        }

        if (cameraMessage != null) {
            CameraMessageBanner(
                message = cameraMessage,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(top = if (isLandscape) 76.dp else 84.dp, start = 16.dp, end = 16.dp)
            )
        }
    }
}

@Composable
private fun MinimalRecordingProgress(
    progress: Float,
    modifier: Modifier = Modifier
) {
    LinearProgressIndicator(
        progress = { progress },
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .height(2.dp),
        color = SafetyRed,
        trackColor = SurfaceContainerHighest.copy(alpha = 0.25f)
    )
}

@Composable
private fun CameraMessageBanner(
    message: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.widthIn(max = 560.dp),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
        color = SurfaceContainerHigh.copy(alpha = 0.88f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Filled.Info, contentDescription = null, tint = ElectricBlue)
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = OnSurface
            )
        }
    }
}

@Composable
fun RecordingControls(
    uiState: RecordingScreenUiState,
    isLandscape: Boolean,
    onToggleRecording: () -> Unit,
    onLock: () -> Unit,
    onToggleAudio: () -> Unit,
    onToggleOverlay: () -> Unit,
    onToggleMiniMap: () -> Unit,
    onCameraModeChange: (String) -> Unit,
    dualSupported: Boolean,
    onNavigateToSettings: () -> Unit,
    onNavigateToLibrary: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        if (isLandscape) {
            if (!uiState.recordingState.isRecording) {
                CameraModeSelector(
                    selectedMode = uiState.cameraMode,
                    onModeSelected = onCameraModeChange,
                    dualSupported = dualSupported,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .statusBarsPadding()
                        .padding(top = 16.dp)
                )
            }

            Column(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                FilledTonalIconButton(
                    onClick = onNavigateToLibrary,
                    modifier = Modifier.size(52.dp),
                    shape = androidx.compose.foundation.shape.CircleShape
                ) {
                    Icon(Icons.Filled.VideoLibrary, "Library")
                }
                FilledTonalIconButton(
                    onClick = onNavigateToSettings,
                    modifier = Modifier.size(52.dp),
                    shape = androidx.compose.foundation.shape.CircleShape
                ) {
                    Icon(Icons.Filled.Settings, "Settings")
                }
            }

            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                LockClipButton(isLocked = uiState.recordingState.locked, onClick = onLock)
                RecordButton(isRecording = uiState.recordingState.isRecording, onClick = onToggleRecording)
                SnapshotButton(onClick = { })
            }

            Column(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                AudioToggle(isEnabled = uiState.audioEnabled, onClick = onToggleAudio)
                OverlayToggle(isEnabled = uiState.showOverlay, onClick = onToggleOverlay)
                MiniMapToggle(isEnabled = uiState.showMiniMap, onClick = onToggleMiniMap)
            }
        } else {
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 14.dp)
                    .fillMaxWidth(),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
                color = SurfaceContainerHigh.copy(alpha = 0.84f)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (!uiState.recordingState.isRecording) {
                        CameraModeSelector(
                            selectedMode = uiState.cameraMode,
                            onModeSelected = onCameraModeChange,
                            dualSupported = dualSupported
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AudioToggle(isEnabled = uiState.audioEnabled, onClick = onToggleAudio)
                        OverlayToggle(isEnabled = uiState.showOverlay, onClick = onToggleOverlay)
                        MiniMapToggle(isEnabled = uiState.showMiniMap, onClick = onToggleMiniMap)
                        FilledTonalIconButton(
                            onClick = onNavigateToSettings,
                            modifier = Modifier.size(48.dp),
                            shape = androidx.compose.foundation.shape.CircleShape
                        ) {
                            Icon(Icons.Filled.Settings, "Settings")
                        }
                    }
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        FilledTonalIconButton(
                            onClick = onNavigateToLibrary,
                            modifier = Modifier
                                .align(Alignment.CenterStart)
                                .size(48.dp),
                            shape = androidx.compose.foundation.shape.CircleShape
                        ) {
                            Icon(Icons.Filled.VideoLibrary, "Library")
                        }
                        LockClipButton(
                            isLocked = uiState.recordingState.locked,
                            onClick = onLock,
                            modifier = Modifier.align(Alignment.CenterStart).padding(start = 112.dp)
                        )
                        RecordButton(
                            isRecording = uiState.recordingState.isRecording,
                            onClick = onToggleRecording,
                            modifier = Modifier.align(Alignment.Center)
                        )
                        SnapshotButton(
                            onClick = { },
                            modifier = Modifier.align(Alignment.CenterEnd)
                        )
                    }
                }
            }
        }

        if (uiState.recordingState.isRecording) {
            LinearProgressIndicator(
                progress = { uiState.recordingState.progressFraction },
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(bottom = if (isLandscape) 112.dp else 196.dp)
                    .height(2.dp),
                color = SafetyRed,
                trackColor = SurfaceContainerHighest.copy(alpha = 0.3f)
            )
        }
    }
}

