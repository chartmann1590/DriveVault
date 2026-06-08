package com.drivevault.dashcam.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "heading_samples",
    indices = [Index("clipId"), Index("timestampMillis")]
)
data class HeadingSampleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val clipId: Long,
    val timestampMillis: Long,
    val headingDegrees: Float,
    val source: String
)
