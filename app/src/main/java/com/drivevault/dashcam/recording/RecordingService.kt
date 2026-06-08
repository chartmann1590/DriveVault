package com.drivevault.dashcam.recording

import android.app.*
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.ProcessLifecycleOwner
import com.drivevault.dashcam.MainActivity
import com.drivevault.dashcam.R
import com.drivevault.dashcam.firebase.DriveVaultFirebase
import com.drivevault.dashcam.notifications.NotificationActionReceiver
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class RecordingService : LifecycleService() {

    companion object {
        const val CHANNEL_ID = "drivevault_recording"
        const val NOTIFICATION_ID = 1001
        const val ACTION_STOP = "com.drivevault.dashcam.ACTION_STOP"
        const val ACTION_LOCK = "com.drivevault.dashcam.ACTION_LOCK"

        private val _recordingState = MutableStateFlow(RecordingState())
        val recordingState: StateFlow<RecordingState> = _recordingState.asStateFlow()

        var isServiceRunning = false
            private set
    }

    private val serviceScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var recordingManager: RecordingManager? = null

    override fun onCreate() {
        super.onCreate()
        isServiceRunning = true
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
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

        if (_recordingState.value.isRecording) return START_STICKY

        val notification = createNotification("Recording in progress")
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_CAMERA)
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
        } catch (error: Exception) {
            DriveVaultFirebase.recordNonFatal("Recording foreground service start failed", error)
            _recordingState.value = _recordingState.value.copy(error = error.message ?: "Recording service could not start")
            stopSelf()
            return START_NOT_STICKY
        }

        recordingManager = RecordingManager(this, ProcessLifecycleOwner.get())
        serviceScope.launch {
            Log.d("RecordingService", "collecting recording state")
            recordingManager?.startRecording()?.collect { state ->
                _recordingState.value = state
                updateNotification(state)
            }
            Log.d("RecordingService", "recording state collection ended")
        }
        return START_STICKY
    }

    private fun stopRecording() {
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
        serviceScope.launch {
            recordingManager?.stopRecording()
        }
        serviceScope.cancel()
        isServiceRunning = false
        _recordingState.value = RecordingState()
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
        val stopIntent = Intent(this, NotificationActionReceiver::class.java).apply {
            action = ACTION_STOP
        }
        val lockIntent = Intent(this, NotificationActionReceiver::class.java).apply {
            action = ACTION_LOCK
        }
        val stopPending = PendingIntent.getBroadcast(this, 0, stopIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val lockPending = PendingIntent.getBroadcast(this, 1, lockIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val contentIntent = Intent(this, MainActivity::class.java)
        val contentPending = PendingIntent.getActivity(this, 0, contentIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.recording_notification_title))
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(contentPending)
            .addAction(0, getString(R.string.notification_action_lock), lockPending)
            .addAction(0, getString(R.string.notification_action_stop), stopPending)
            .setOngoing(true)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()
    }

    private fun updateNotification(state: RecordingState) {
        val text = "Clip: ${state.elapsedSeconds}s / 60s"
        val notification = createNotification(text)
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID, notification)
    }
}
