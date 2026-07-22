package com.drivevault.dashcam.recording

import android.Manifest
import android.annotation.SuppressLint
import android.app.*
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.util.Log
import androidx.camera.core.Preview
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.ProcessLifecycleOwner
import com.drivevault.dashcam.MainActivity
import com.drivevault.dashcam.R
import com.drivevault.dashcam.data.repository.SettingsRepository
import com.drivevault.dashcam.firebase.DriveVaultFirebase
import com.drivevault.dashcam.notifications.NotificationActionReceiver
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first

class RecordingService : LifecycleService() {

    companion object {
        const val CHANNEL_ID = "drivevault_recording"
        const val NOTIFICATION_ID = 1001
        const val ACTION_STOP = "com.drivevault.dashcam.ACTION_STOP"
        const val ACTION_LOCK = "com.drivevault.dashcam.ACTION_LOCK"

        private val _recordingState = MutableStateFlow(RecordingState())
        val recordingState: StateFlow<RecordingState> = _recordingState.asStateFlow()

        private val _detectedObjects = MutableStateFlow<List<DetectedObjectResult>>(emptyList())
        val detectedObjects: StateFlow<List<DetectedObjectResult>> = _detectedObjects.asStateFlow()

        var isServiceRunning = false
            private set

        @Volatile var primarySurfaceProvider: Preview.SurfaceProvider? = null
        @Volatile var secondarySurfaceProvider: Preview.SurfaceProvider? = null
    }

    private val serviceScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var recordingManager: RecordingManager? = null
    private var stopInitiated = false
    private var lastNotificationSeconds = -1

    private lateinit var stopPendingIntent: PendingIntent
    private lateinit var lockPendingIntent: PendingIntent
    private lateinit var contentPendingIntent: PendingIntent

    override fun onCreate() {
        super.onCreate()
        isServiceRunning = true
        createNotificationChannel()
        val stopIntent = Intent(this, NotificationActionReceiver::class.java).apply { action = ACTION_STOP }
        val lockIntent = Intent(this, NotificationActionReceiver::class.java).apply { action = ACTION_LOCK }
        stopPendingIntent = PendingIntent.getBroadcast(this, 0, stopIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        lockPendingIntent = PendingIntent.getBroadcast(this, 1, lockIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        contentPendingIntent = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    @SuppressLint("InlinedApi")
    @androidx.camera.core.ExperimentalGetImage
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        Log.d("RecordingService", "onStartCommand action=${intent?.action}")
        when (intent?.action) {
            ACTION_STOP -> {
                stopRecording()
                return START_NOT_STICKY
            }
            ACTION_LOCK -> {
                lockCurrentClip()
                return START_STICKY
            }
        }

        // recordingManager is checked (not just _recordingState.value.isRecording) because it is
        // assigned synchronously below, before any suspension point. _recordingState.value only
        // flips to isRecording=true asynchronously once the RecordingManager's flow starts
        // emitting, so a rapid double-tap of the record button could otherwise pass this guard
        // twice and spin up two concurrent RecordingManager instances fighting over the camera.
        if (_recordingState.value.isRecording || recordingManager != null) return START_STICKY

        val notification = createNotification("Recording in progress")
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Only declare the microphone/location types when their runtime permission is
                // actually granted - RECORDING_START_PERMISSIONS lets recording begin with just
                // CAMERA, and declaring a type without its permission throws a SecurityException
                // on API 34+.
                var foregroundServiceType = ServiceInfo.FOREGROUND_SERVICE_TYPE_CAMERA
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) ==
                    PackageManager.PERMISSION_GRANTED
                ) {
                    foregroundServiceType = foregroundServiceType or ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
                }
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
                ) {
                    foregroundServiceType = foregroundServiceType or ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
                }
                startForeground(NOTIFICATION_ID, notification, foregroundServiceType)
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
        } catch (error: Exception) {
            DriveVaultFirebase.recordNonFatal("Recording foreground service start failed", error)
            _recordingState.value = _recordingState.value.copy(error = error.message ?: "Recording service could not start")
            stopSelf()
            return START_NOT_STICKY
        }

        val manager = RecordingManager(this, ProcessLifecycleOwner.get())
        recordingManager = manager
        serviceScope.launch {
            val detectionEnabled = SettingsRepository(this@RecordingService).vehicleDetectionEnabled.first()
            manager.setVehicleDetectionEnabled(detectionEnabled)
            manager.setSurfaceProviders(primarySurfaceProvider, secondarySurfaceProvider)
            launch {
                manager.detectedObjects.collect { objects -> _detectedObjects.value = objects }
            }
            Log.d("RecordingService", "collecting recording state")
            manager.startRecording().collect { state ->
                _recordingState.value = state
                updateNotification(state)
            }
            Log.d("RecordingService", "recording state collection ended")
        }
        return START_STICKY
    }

    private fun stopRecording() {
        stopInitiated = true
        serviceScope.launch {
            recordingManager?.stopRecording()
            stopSelf()
        }
    }

    private fun lockCurrentClip() {
        recordingManager?.lockCurrentClip()
    }

    override fun onDestroy() {
        Log.d("RecordingService", "onDestroy")
        if (!stopInitiated) {
            // System killed the service without going through ACTION_STOP.
            // Signal CameraX to stop without blocking the main thread (avoids ANR).
            // The in-progress clip is not saved to DB on this path.
            recordingManager?.cancelRecording()
        }
        serviceScope.cancel()
        isServiceRunning = false
        primarySurfaceProvider = null
        secondarySurfaceProvider = null
        _recordingState.value = RecordingState()
        _detectedObjects.value = emptyList()
        super.onDestroy()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.recording_notification_channel),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            setShowBadge(false)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    private fun createNotification(text: String): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.recording_notification_title))
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(contentPendingIntent)
            .addAction(0, getString(R.string.notification_action_lock), lockPendingIntent)
            .addAction(0, getString(R.string.notification_action_stop), stopPendingIntent)
            .setOngoing(true)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()
    }

    private fun updateNotification(state: RecordingState) {
        if (state.elapsedSeconds == lastNotificationSeconds) return
        lastNotificationSeconds = state.elapsedSeconds
        val text = "Clip: ${state.elapsedSeconds}s / 60s"
        val notification = createNotification(text)
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID, notification)
    }
}
