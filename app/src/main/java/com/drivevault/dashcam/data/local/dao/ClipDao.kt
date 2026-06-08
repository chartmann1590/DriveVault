package com.drivevault.dashcam.data.local.dao

import androidx.room.*
import com.drivevault.dashcam.data.local.entity.ClipEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ClipDao {
    @Insert
    suspend fun insert(clip: ClipEntity): Long

    @Update
    suspend fun update(clip: ClipEntity)

    @Delete
    suspend fun delete(clip: ClipEntity)

    @Query("DELETE FROM clips WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM clips WHERE id = :id")
    suspend fun getById(id: Long): ClipEntity?

    @Query("SELECT * FROM clips WHERE id = :id")
    fun observeById(id: Long): Flow<ClipEntity?>

    @Query("SELECT * FROM clips ORDER BY startTimeMillis DESC")
    fun observeAll(): Flow<List<ClipEntity>>

    @Query("SELECT * FROM clips ORDER BY startTimeMillis DESC")
    suspend fun getAll(): List<ClipEntity>

    @Query("SELECT * FROM clips WHERE startTimeMillis >= :from AND startTimeMillis <= :to ORDER BY startTimeMillis DESC")
    fun observeByTimeRange(from: Long, to: Long): Flow<List<ClipEntity>>

    @Query("SELECT * FROM clips WHERE locked = 1 ORDER BY startTimeMillis DESC")
    fun observeLocked(): Flow<List<ClipEntity>>

    @Query("SELECT * FROM clips WHERE immichStatus = :status ORDER BY startTimeMillis DESC")
    fun observeByImmichStatus(status: String): Flow<List<ClipEntity>>

    @Query("SELECT * FROM clips WHERE immichStatus != 'UPLOADED' ORDER BY startTimeMillis DESC")
    fun observeUnsynced(): Flow<List<ClipEntity>>

    @Query("SELECT * FROM clips WHERE cameraMode = :mode ORDER BY startTimeMillis DESC")
    fun observeByCameraMode(mode: String): Flow<List<ClipEntity>>

    @Query("SELECT * FROM clips WHERE locked = 0 AND interrupted = 0 ORDER BY startTimeMillis ASC")
    suspend fun getOldestUnlocked(): List<ClipEntity>

    @Query("SELECT COUNT(*) FROM clips")
    suspend fun getClipCount(): Int

    @Query("SELECT SUM(fileSizeBytes) FROM clips")
    suspend fun getTotalStorageUsed(): Long?

    @Query("SELECT SUM(fileSizeBytes) FROM clips")
    fun observeTotalStorageUsed(): Flow<Long?>

    @Query("UPDATE clips SET locked = :locked WHERE id = :id")
    suspend fun setLocked(id: Long, locked: Boolean)

    @Query("UPDATE clips SET immichStatus = :status, immichAssetId = :assetId, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateImmichStatus(id: Long, status: String, assetId: String?, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE clips SET immichStatus = :status WHERE id IN (:ids)")
    suspend fun updateImmichStatusBatch(ids: List<Long>, status: String)

    @Query("SELECT * FROM clips WHERE immichStatus = 'FAILED' ORDER BY startTimeMillis DESC")
    suspend fun getFailedUploads(): List<ClipEntity>

    @Query("SELECT * FROM clips WHERE immichStatus = 'PENDING' ORDER BY startTimeMillis DESC")
    suspend fun getPendingUploads(): List<ClipEntity>

    @Query("DELETE FROM clips WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<Long>)

    @Query("SELECT * FROM clips WHERE startTimeMillis >= :from AND startTimeMillis <= :to ORDER BY startTimeMillis DESC")
    suspend fun getByTimeRange(from: Long, to: Long): List<ClipEntity>
}
