package com.drivevault.dashcam.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.drivevault.dashcam.recording.RecordingService

class NotificationActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val serviceIntent = Intent(context, RecordingService::class.java).apply {
            action = intent.action
        }
        context.startService(serviceIntent)
    }
}
