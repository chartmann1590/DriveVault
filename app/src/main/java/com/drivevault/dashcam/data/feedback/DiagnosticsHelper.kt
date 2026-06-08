package com.drivevault.dashcam.data.feedback

import android.content.Context
import android.os.Build
import android.os.StatFs
import com.drivevault.dashcam.BuildConfig
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object DiagnosticsHelper {

    fun collect(context: Context): String {
        val sb = StringBuilder()
        sb.appendLine("## Diagnostics")
        sb.appendLine()
        sb.appendLine("- App: ${appName(context)}")
        sb.appendLine("- Package: ${context.packageName}")
        sb.appendLine("- Version: ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
        sb.appendLine("- Device: ${Build.DEVICE}")
        sb.appendLine("- Model: ${Build.MODEL}")
        sb.appendLine("- Manufacturer: ${Build.MANUFACTURER}")
        sb.appendLine("- Android: ${Build.VERSION.RELEASE} / API ${Build.VERSION.SDK_INT}")
        sb.appendLine("- Locale: ${Locale.getDefault().toString()}")
        sb.appendLine("- Time Zone: ${TimeZone.getDefault().id}")
        sb.appendLine("- Storage Free/Total: ${storageInfo(context)}")
        sb.appendLine("- Memory Free/Total: ${memoryInfo()}")
        sb.appendLine("- Timestamp: ${currentTime()}")
        return sb.toString()
    }

    private fun appName(context: Context): String {
        val ai = context.applicationInfo
        return context.packageManager.getApplicationLabel(ai).toString()
    }

    private fun storageInfo(context: Context): String {
        return try {
            val stat = StatFs(context.filesDir.absolutePath)
            val free = stat.availableBlocksLong * stat.blockSizeLong
            val total = stat.blockCountLong * stat.blockSizeLong
            "${formatBytes(free)} / ${formatBytes(total)}"
        } catch (_: Exception) {
            "Unavailable"
        }
    }

    private fun memoryInfo(): String {
        return try {
            val runtime = Runtime.getRuntime()
            val free = runtime.freeMemory()
            val total = runtime.totalMemory()
            "${formatBytes(free.toLong())} / ${formatBytes(total.toLong())}"
        } catch (_: Exception) {
            "Unavailable"
        }
    }

    private fun formatBytes(bytes: Long): String {
        if (bytes < 1024) return "$bytes B"
        val kb = bytes / 1024.0
        if (kb < 1024) return "%.1f KB".format(kb)
        val mb = kb / 1024.0
        if (mb < 1024) return "%.1f MB".format(mb)
        val gb = mb / 1024.0
        return "%.1f GB".format(gb)
    }

    private fun currentTime(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss z", Locale.US)
        return sdf.format(Date())
    }
}
