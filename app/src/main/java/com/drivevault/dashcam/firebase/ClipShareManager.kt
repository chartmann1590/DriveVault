package com.drivevault.dashcam.firebase

import android.content.Context
import android.util.Base64
import com.drivevault.dashcam.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.time.Instant
import java.util.UUID
import java.util.concurrent.TimeUnit

sealed class ClipShareState {
    object Idle : ClipShareState()
    data class Uploading(val progressPercent: Int) : ClipShareState()
    data class Success(val shareUrl: String, val shareId: String) : ClipShareState()
    data class Error(val message: String) : ClipShareState()
}

private const val TUS_CHUNK_BYTES = 6 * 1024 * 1024 // 6 MB chunks

class ClipShareManager(@Suppress("unused") private val context: Context) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(120, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    private val _shareState = MutableStateFlow<ClipShareState>(ClipShareState.Idle)
    val shareState: StateFlow<ClipShareState> = _shareState.asStateFlow()

    suspend fun shareClip(filePath: String, fileSizeBytes: Long) {
        val file = File(filePath)

        // Supabase free tier enforces a 50 MB per-object limit at the TUS CREATE step.
        // Reject early with a clear message rather than surfacing a raw 413.
        if (file.length() > 50L * 1024 * 1024) {
            _shareState.value = ClipShareState.Error(
                "Clip is ${file.length() / (1024 * 1024)}MB — sharing requires clips under 50MB. " +
                "Try a shorter clip."
            )
            return
        }

        val shareId = UUID.randomUUID().toString()
        val storagePath = "$shareId/clip.mp4"

        _shareState.value = ClipShareState.Uploading(0)

        try {
            uploadFileTus(file, "shared-clips", storagePath)
            insertMetadata(shareId, storagePath, fileSizeBytes)
            _shareState.value = ClipShareState.Success(
                shareUrl = "${BuildConfig.SHARE_VIEWER_URL}/view?id=$shareId",
                shareId = shareId
            )
        } catch (e: Exception) {
            _shareState.value = ClipShareState.Error(e.message ?: "Upload failed")
        }
    }

    private suspend fun uploadFileTus(file: File, bucket: String, path: String) =
        withContext(Dispatchers.IO) {
            val totalBytes = file.length()

            // Base64-encode metadata values per TUS spec
            val bucketB64 = Base64.encodeToString(bucket.toByteArray(), Base64.NO_WRAP)
            val pathB64 = Base64.encodeToString(path.toByteArray(), Base64.NO_WRAP)

            // Step 1: create resumable upload session
            val createResp = client.newCall(
                Request.Builder()
                    .url("${BuildConfig.SUPABASE_URL}/storage/v1/upload/resumable")
                    .addHeader("Authorization", "Bearer ${BuildConfig.SUPABASE_ANON_KEY}")
                    .addHeader("apikey", BuildConfig.SUPABASE_ANON_KEY)
                    .addHeader("Tus-Resumable", "1.0.0")
                    .addHeader("Upload-Length", totalBytes.toString())
                    .addHeader("Upload-Metadata", "bucketName $bucketB64,objectName $pathB64")
                    .addHeader("x-upsert", "false")
                    .post(ByteArray(0).toRequestBody(null, 0, 0))
                    .build()
            ).execute()

            if (!createResp.isSuccessful) {
                throw IOException("TUS create failed: ${createResp.code} ${createResp.body?.string()}")
            }

            val location = createResp.header("Location")
                ?: throw IOException("TUS create: no Location header")

            // Location may be a relative path or full URL
            val uploadUrl = if (location.startsWith("http")) location
            else "${BuildConfig.SUPABASE_URL}$location"

            // Step 2: upload in chunks, reporting progress
            var offset = 0L
            val buf = ByteArray(TUS_CHUNK_BYTES)

            file.inputStream().use { input ->
                while (offset < totalBytes) {
                    val bytesRead = input.read(buf)
                    if (bytesRead <= 0) break
                    val chunk = if (bytesRead == buf.size) buf else buf.copyOf(bytesRead)

                    val patchResp = client.newCall(
                        Request.Builder()
                            .url(uploadUrl)
                            .addHeader("Authorization", "Bearer ${BuildConfig.SUPABASE_ANON_KEY}")
                            .addHeader("apikey", BuildConfig.SUPABASE_ANON_KEY)
                            .addHeader("Tus-Resumable", "1.0.0")
                            .addHeader("Upload-Offset", offset.toString())
                            .patch(chunk.toRequestBody("application/offset+octet-stream".toMediaType()))
                            .build()
                    ).execute()

                    if (!patchResp.isSuccessful) {
                        throw IOException("TUS chunk failed at $offset: ${patchResp.code} ${patchResp.body?.string()}")
                    }

                    offset += bytesRead
                    _shareState.value = ClipShareState.Uploading(((offset * 100) / totalBytes).toInt())
                }
            }
        }

    private suspend fun insertMetadata(shareId: String, storagePath: String, fileSizeBytes: Long) =
        withContext(Dispatchers.IO) {
            val json = JSONObject().apply {
                put("id", shareId)
                put("storage_path", storagePath)
                put("file_size_bytes", fileSizeBytes)
                put("expires_at", Instant.now().plusSeconds(86400).toString())
            }.toString()

            val response = client.newCall(
                Request.Builder()
                    .url("${BuildConfig.SUPABASE_URL}/rest/v1/shared_clips")
                    .addHeader("Authorization", "Bearer ${BuildConfig.SUPABASE_ANON_KEY}")
                    .addHeader("apikey", BuildConfig.SUPABASE_ANON_KEY)
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Prefer", "return=minimal")
                    .post(json.toRequestBody("application/json".toMediaType()))
                    .build()
            ).execute()

            if (!response.isSuccessful) {
                throw IOException("Metadata insert failed: ${response.code} ${response.body?.string()}")
            }
        }

    fun resetState() {
        _shareState.value = ClipShareState.Idle
    }
}
