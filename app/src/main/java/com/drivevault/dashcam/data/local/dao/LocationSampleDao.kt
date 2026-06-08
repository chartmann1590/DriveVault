package com.drivevault.dashcam.data.local.dao

import androidx.room.*
import com.drivevault.dashcam.data.local.entity.LocationSampleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LocationSampleDao {
    @Insert
    suspend fun insert(sample: LocationSampleEntity): Long

    @Insert
    suspend fun insertAll(samples: List<LocationSampleEntity>)

    @Query("SELECT * FROM location_samples WHERE clipId = :clipId ORDER BY timestampMillis ASC")
    suspend fun getByClipId(clipId: Long): List<LocationSampleEntity>

    @Query("SELECT * FROM location_samples WHERE clipId = :clipId ORDER BY timestampMillis ASC")
    fun observeByClipId(clipId: Long): Flow<List<LocationSampleEntity>>

    @Query("DELETE FROM location_samples WHERE clipId = :clipId")
    suspend fun deleteByClipId(clipId: Long)

    @Query("DELETE FROM location_samples WHERE clipId IN (:clipIds)")
    suspend fun deleteByClipIds(clipIds: List<Long>)
}
