package com.drivevault.dashcam.data.local.dao

import androidx.room.*
import com.drivevault.dashcam.data.local.entity.SnapshotEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SnapshotDao {
    @Insert
    suspend fun insert(snapshot: SnapshotEntity): Long

    @Delete
    suspend fun delete(snapshot: SnapshotEntity)

    @Query("SELECT * FROM snapshots ORDER BY timestampMillis DESC")
    fun observeAll(): Flow<List<SnapshotEntity>>

    @Query("SELECT * FROM snapshots WHERE id = :id")
    suspend fun getById(id: Long): SnapshotEntity?

    @Query("DELETE FROM snapshots WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("UPDATE snapshots SET immichStatus = :status, immichAssetId = :assetId WHERE id = :id")
    suspend fun updateImmichStatus(id: Long, status: String, assetId: String?)
}
