package com.drivevault.dashcam.data.feedback

import android.content.Context
import android.net.Uri
import android.util.Base64
import java.io.ByteArrayOutputStream
import java.io.InputStream

object ImageUploadHelper {

    fun uriToBase64(context: Context, uri: Uri): String {
        val inputStream: InputStream = context.contentResolver.openInputStream(uri)
            ?: throw IllegalStateException("Cannot open input stream for URI: $uri")
        inputStream.use { stream ->
            val bos = ByteArrayOutputStream()
            val buffer = ByteArray(8192)
            var bytesRead: Int
            while (stream.read(buffer).also { bytesRead = it } != -1) {
                bos.write(buffer, 0, bytesRead)
            }
            return Base64.encodeToString(bos.toByteArray(), Base64.NO_WRAP)
        }
    }

    fun generateFilename(): String {
        val sdf = java.text.SimpleDateFormat("yyyyMMdd-HHmmss", java.util.Locale.US)
        val timestamp = sdf.format(java.util.Date())
        val random = (1000..9999).random()
        return "issue-$timestamp-$random.png"
    }
}
