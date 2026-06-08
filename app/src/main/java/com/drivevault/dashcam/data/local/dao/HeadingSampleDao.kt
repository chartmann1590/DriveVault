package com.drivevault.dashcam.data.local.dao

import androidx.room.*
import com.drivevault.dashcam.data.local.entity.HeadingSampleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HeadingSampleDao {
    @Insert
    suspend fun insert(sample: HeadingSampleEntity): Long

    @Insert
    suspend fun insertAll(samples: List<HeadingSampleEntity>)

    @Query("SELECT * FROM heading_samples WHERE clipId = :clipId ORDER BY timestampMillis ASC")
    suspend fun getByClipId(clipId: Long): List<HeadingSampleEntity>

    @Query("SELECT * FROM heading_samples WHERE clipId = :clipId ORDER BY timestampMillis ASC")
    fun observeByClipId(clipId: Long): Flow<List<HeadingSampleEntity>>

    @Query("DELETE FROM heading_samples WHERE clipId = :clipId")
    suspend fun deleteByClipId(clipId: Long)

    @Query("DELETE FROM heading_samples WHERE clipId IN (:clipIds)")
    suspend fun deleteByClipIds(clipIds: List<Long>)
}
