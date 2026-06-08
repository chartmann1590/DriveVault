package com.drivevault.dashcam.domain.repository

import com.drivevault.dashcam.data.local.entity.*
import com.drivevault.dashcam.data.local.dao.*
import com.drivevault.dashcam.domain.model.Clip
import com.drivevault.dashcam.domain.model.toDomain
import com.drivevault.dashcam.domain.model.toEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.io.File

class ClipRepository(
    private val clipDao: ClipDao,
    private val locationSampleDao: LocationSampleDao,
    private val headingSampleDao: HeadingSampleDao,
    private val snapshotDao: SnapshotDao
) {
    fun observeAllClips(): Flow<List<Clip>> = clipDao.observeAll().map { list -> list.map { it.toDomain() } }
    fun observeClipById(id: Long): Flow<Clip?> = clipDao.observeById(id).map { it?.toDomain() }
    fun observeLockedClips(): Flow<List<Clip>> = clipDao.observeLocked().map { list -> list.map { it.toDomain() } }
    fun observeUnsyncedClips(): Flow<List<Clip>> = clipDao.observeUnsynced().map { list -> list.map { it.toDomain() } }
    fun observeClipsByCameraMode(mode: String): Flow<List<Clip>> = clipDao.observeByCameraMode(mode).map { list -> list.map { it.toDomain() } }
    fun observeClipsByTimeRange(from: Long, to: Long): Flow<List<Clip>> = clipDao.observeByTimeRange(from, to).map { list -> list.map { it.toDomain() } }

    suspend fun getClipById(id: Long): Clip? = clipDao.getById(id)?.toDomain()
    suspend fun getAllClips(): List<Clip> = clipDao.getAll().map { it.toDomain() }
    suspend fun getOldestUnlockedClips(): List<Clip> = clipDao.getOldestUnlocked().map { it.toDomain() }
    suspend fun getFailedUploads(): List<Clip> = clipDao.getFailedUploads().map { it.toDomain() }
    suspend fun getPendingUploads(): List<Clip> = clipDao.getPendingUploads().map { it.toDomain() }
    suspend fun getClipsByTimeRange(from: Long, to: Long): List<Clip> = clipDao.getByTimeRange(from, to).map { it.toDomain() }

    suspend fun insertClip(clip: Clip): Long = clipDao.insert(clip.toEntity())
    suspend fun updateClip(clip: Clip) = clipDao.update(clip.toEntity())
    suspend fun deleteClip(clip: Clip) {
        clipDao.delete(clip.toEntity())
        locationSampleDao.deleteByClipId(clip.id)
        headingSampleDao.deleteByClipId(clip.id)
    }
    suspend fun deleteClipsByIds(ids: List<Long>) {
        clipDao.deleteByIds(ids)
        locationSampleDao.deleteByClipIds(ids)
        headingSampleDao.deleteByClipIds(ids)
    }
    suspend fun setClipLocked(id: Long, locked: Boolean) = clipDao.setLocked(id, locked)
    suspend fun updateImmichStatus(id: Long, status: String, assetId: String?) = clipDao.updateImmichStatus(id, status, assetId)
    suspend fun markPendingForUpload(ids: List<Long>) = clipDao.updateImmichStatusBatch(ids, "PENDING")

    suspend fun insertLocationSamples(samples: List<LocationSampleEntity>) = locationSampleDao.insertAll(samples)
    suspend fun getLocationSamples(clipId: Long): List<LocationSampleEntity> = locationSampleDao.getByClipId(clipId)

    suspend fun insertHeadingSamples(samples: List<HeadingSampleEntity>) = headingSampleDao.insertAll(samples)
    suspend fun getHeadingSamples(clipId: Long): List<HeadingSampleEntity> = headingSampleDao.getByClipId(clipId)

    suspend fun getClipCount(): Int = clipDao.getClipCount()
    suspend fun getTotalStorageUsed(): Long = clipDao.getTotalStorageUsed() ?: 0L
    fun observeTotalStorageUsed(): Flow<Long> = clipDao.observeTotalStorageUsed().map { it ?: 0L }
}
