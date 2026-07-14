package com.drivevault.dashcam.storage

import android.content.Context
import com.drivevault.dashcam.data.local.DriveVaultDatabase
import com.drivevault.dashcam.data.repository.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File

data class StorageInfo(
    val totalUsedBytes: Long,
    val maxAllowedBytes: Long,
    val clipCount: Int,
    val lockedClipCount: Int,
    val availableBytes: Long,
    val usagePercent: Float
) {
    val isLow: Boolean get() = usagePercent > 80f
    val isCritical: Boolean get() = usagePercent > 95f
    val usedMb: Long get() = totalUsedBytes / (1024 * 1024)
    val maxMb: Long get() = maxAllowedBytes / (1024 * 1024)
}

class StorageManager(private val context: Context) {

    private val db by lazy { DriveVaultDatabase.getInstance(context) }
    private val settings by lazy { SettingsRepository(context) }

    fun observeStorageInfo(): Flow<StorageInfo> = combine(
        db.clipDao().observeTotalStorageUsed(),
        db.clipDao().observeAll(),
        settings.maxStorageMb
    ) { used, clips, maxMb ->
        val maxBytes = maxMb * 1024 * 1024
        val totalUsed = used ?: 0L
        val locked = clips.count { it.locked }
        val available = maxBytes - totalUsed
        val percent = if (maxBytes > 0) (totalUsed.toFloat() / maxBytes.toFloat()) * 100f else 0f
        StorageInfo(totalUsed, maxBytes, clips.size, locked, available, percent)
    }

    suspend fun enforceStorageLimit() = withContext(Dispatchers.IO) {
        if (!settings.autoDeleteOldest.first()) return@withContext

        val maxMb = settings.maxStorageMb.first()
        val maxBytes = maxMb * 1024 * 1024
        var currentUsage = db.clipDao().getTotalStorageUsed() ?: 0L

        if (currentUsage <= maxBytes) return@withContext

        val unlocked = db.clipDao().getOldestUnlocked()
        for (clip in unlocked) {
            if (currentUsage <= maxBytes * 0.9) break

            val file = File(clip.fileUri)
            val thumbFile = clip.thumbnailUri?.let { File(it) }
            val secondaryFile = clip.secondaryFileUri?.let { File(it) }
            val secondaryThumb = clip.secondaryThumbnailUri?.let { File(it) }

            file.delete()
            thumbFile?.delete()
            secondaryFile?.delete()
            secondaryThumb?.delete()

            currentUsage -= clip.fileSizeBytes

            db.locationSampleDao().deleteByClipId(clip.id)
            db.headingSampleDao().deleteByClipId(clip.id)
            db.clipDao().deleteById(clip.id)
        }
    }

    suspend fun getDeviceAvailableStorage(): Long = withContext(Dispatchers.IO) {
        android.os.StatFs(context.filesDir.absolutePath).availableBytes
    }

    suspend fun deleteClipWithFiles(clipId: Long) = withContext(Dispatchers.IO) {
        val clip = db.clipDao().getById(clipId) ?: return@withContext

        File(clip.fileUri).delete()
        clip.thumbnailUri?.let { File(it).delete() }
        clip.secondaryFileUri?.let { File(it).delete() }
        clip.secondaryThumbnailUri?.let { File(it).delete() }

        db.locationSampleDao().deleteByClipId(clipId)
        db.headingSampleDao().deleteByClipId(clipId)
        db.clipDao().deleteById(clipId)
    }

    suspend fun deleteClipsWithFiles(clipIds: List<Long>) {
        clipIds.forEach { deleteClipWithFiles(it) }
    }
}
