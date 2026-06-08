package com.drivevault.dashcam.immich

import android.content.Context
import androidx.work.*
import com.drivevault.dashcam.data.local.DriveVaultDatabase
import com.drivevault.dashcam.data.repository.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.TimeUnit

class ImmichSyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val settings = SettingsRepository(applicationContext)
        val db = DriveVaultDatabase.getInstance(applicationContext)
        val clipDao = db.clipDao()
        val client = ImmichClient(applicationContext)

        if (!settings.immichEnabled.first()) return@withContext Result.success()

        val pending = clipDao.getPendingUploads() + clipDao.getFailedUploads()
        if (pending.isEmpty()) return@withContext Result.success()

        for (clip in pending.distinctBy { it.id }) {
            val file = File(clip.fileUri)
            if (!file.exists()) {
                clipDao.updateImmichStatus(clip.id, "FAILED", clip.immichAssetId)
                continue
            }

            clipDao.updateImmichStatus(clip.id, "UPLOADING", clip.immichAssetId)

            val durationSecs = clip.durationMillis / 1000
            when (val result = client.uploadVideo(file, clip.startTimeMillis, durationSecs)) {
                is ImmichResult.Success -> {
                    val assetId = result.data.id
                    clipDao.updateImmichStatus(clip.id, "UPLOADED", assetId)

                    if (settings.immichAutoAlbum.first()) {
                        val albumName = settings.immichAlbumName.first()
                        when (val albumResult = client.findOrCreateAlbum(albumName)) {
                            is ImmichResult.Success -> {
                                client.addAssetToAlbum(albumResult.data, assetId)
                            }
                            else -> {}
                        }
                    }
                }
                is ImmichResult.AuthError -> {
                    clipDao.updateImmichStatus(clip.id, "FAILED", clip.immichAssetId)
                    return@withContext Result.failure()
                }
                else -> {
                    clipDao.updateImmichStatus(clip.id, "FAILED", clip.immichAssetId)
                }
            }
        }
        Result.success()
    }

    companion object {
        const val WORK_NAME = "immich_sync"

        fun scheduleSync(context: Context, syncMode: String) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .apply {
                    when (syncMode) {
                        "WIFI_ONLY" -> setRequiredNetworkType(NetworkType.UNMETERED)
                        "WIFI_CHARGING" -> {
                            setRequiredNetworkType(NetworkType.UNMETERED)
                            setRequiresCharging(true)
                        }
                        "ALWAYS" -> setRequiredNetworkType(NetworkType.CONNECTED)
                        else -> setRequiredNetworkType(NetworkType.CONNECTED)
                    }
                }
                .build()

            val request = PeriodicWorkRequestBuilder<ImmichSyncWorker>(15, TimeUnit.MINUTES)
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                .build()

            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
        }

        fun syncNow(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = OneTimeWorkRequestBuilder<ImmichSyncWorker>()
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueue(request)
        }

        fun cancelSync(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }
    }
}
