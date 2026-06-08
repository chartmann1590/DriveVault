package com.drivevault.dashcam.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "location_samples",
    indices = [Index("clipId"), Index("timestampMillis")]
)
data class LocationSampleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val clipId: Long,
    val timestampMillis: Long,
    val latitude: Double,
    val longitude: Double,
    val altitude: Double = 0.0,
    val accuracy: Float = 0f,
    val speedMps: Float = 0f,
    val bearingDegrees: Float = 0f
)
