package com.drivevault.dashcam.firebase

import android.content.Context
import androidx.work.*
import com.drivevault.dashcam.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import java.time.Instant
import java.util.concurrent.TimeUnit

class ShareCleanupWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    private val client = OkHttpClient()

    override suspend fun doWork(): Result {
        return try {
            cleanupExpired()
            Result.success()
        } catch (_: Exception) {
            Result.retry()
        }
    }

    private suspend fun cleanupExpired() = withContext(Dispatchers.IO) {
        val now = Instant.now().toString()
        val url = "${BuildConfig.SUPABASE_URL}/rest/v1/shared_clips?expires_at=lt.$now&select=id,storage_path"

        val queryResponse = client.newCall(
            Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer ${BuildConfig.SUPABASE_ANON_KEY}")
                .addHeader("apikey", BuildConfig.SUPABASE_ANON_KEY)
                .get()
                .build()
        ).execute()

        if (!queryResponse.isSuccessful) return@withContext
        val body = queryResponse.body?.string() ?: return@withContext
        val records = JSONArray(body)

        for (i in 0 until records.length()) {
            try {
                val record = records.getJSONObject(i)
                val id = record.getString("id")
                val storagePath = record.getString("storage_path")

                // Delete the storage object using the service role key so we bypass
                // the public-only storage RLS policies. The anon key intentionally
                // cannot delete storage objects — only the cleanup worker can.
                //
                // We use the single-object DELETE endpoint
                //   DELETE /storage/v1/object/{bucketId}/{objectPath}
                // rather than the batch POST /object/delete/{bucketId} endpoint:
                // the batch endpoint's body schema has changed across Supabase
                // storage versions and has been observed returning 400 for the
                // documented {prefixes:[...]} body on current projects, while the
                // single-object DELETE consistently returns 200 on success. The
                // worker deletes one object per expired record anyway, so the
                // batch endpoint offered no advantage here.
                val deleteResp = client.newCall(
                    Request.Builder()
                        .url("${BuildConfig.SUPABASE_URL}/storage/v1/object/shared-clips/$storagePath")
                        .addHeader("Authorization", "Bearer ${BuildConfig.SUPABASE_SERVICE_ROLE_KEY}")
                        .addHeader("apikey", BuildConfig.SUPABASE_SERVICE_ROLE_KEY)
                        .delete()
                        .build()
                ).execute()
                deleteResp.close()

                // Delete DB record
                client.newCall(
                    Request.Builder()
                        .url("${BuildConfig.SUPABASE_URL}/rest/v1/shared_clips?id=eq.$id")
                        .addHeader("Authorization", "Bearer ${BuildConfig.SUPABASE_ANON_KEY}")
                        .addHeader("apikey", BuildConfig.SUPABASE_ANON_KEY)
                        .delete()
                        .build()
                ).execute()
            } catch (_: Exception) {
                // continue to next record
            }
        }
    }

    companion object {
        const val WORK_NAME = "share_cleanup_periodic"

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<ShareCleanupWorker>(1, TimeUnit.HOURS)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }
    }
}
