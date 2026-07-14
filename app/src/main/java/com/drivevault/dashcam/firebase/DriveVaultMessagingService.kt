package com.drivevault.dashcam.firebase

import android.app.NotificationChannel
import android.app.NotificationManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.getSystemService
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class DriveVaultMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "FCM token refreshed")
        // Surface the new token in Crashlytics so we can correlate push delivery
        // with crash reports. The token itself is not sensitive and is only
        // recorded when Crashlytics is enabled, mirroring DriveVaultFirebase.applySettings.
        runCatching {
            FirebaseCrashlytics.getInstance().setCustomKey("fcm_token_available", token.isNotBlank())
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        Log.d(TAG, "FCM message received: ${message.messageId ?: "no-id"}")

        // Data messages arrive here when the app is in the foreground; notification
        // messages are auto-displayed by the system tray. To keep the foreground
        // experience consistent, post a local notification for any data payload
        // that carries a title/body, and fall back to a generic DriveVault notice.
        val title = message.data["title"] ?: message.notification?.title ?: "DriveVault"
        val body = message.data["body"] ?: message.notification?.body ?: "New message"
        postNotification(title, body)
    }

    private fun postNotification(title: String, body: String) {
        val manager = getSystemService<NotificationManager>() ?: return
        val channel = NotificationChannel(
            CHANNEL_ID,
            "DriveVault Updates",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply { description = "Push notifications from the DriveVault project" }
        manager.createNotificationChannel(channel)

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        runCatching { manager.notify(NOTIFICATION_ID, notification) }
    }

    companion object {
        private const val TAG = "DriveVaultFirebase"
        private const val CHANNEL_ID = "drivevault_fcm"
        private const val NOTIFICATION_ID = 1001
    }
}