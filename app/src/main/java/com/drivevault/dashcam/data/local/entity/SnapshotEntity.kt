package com.drivevault.dashcam.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "snapshots")
data class SnapshotEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val fileUri: String,
    val timestampMillis: Long,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val speedMps: Float = 0f,
    val headingDegrees: Float = 0f,
    val immichStatus: String = "NOT_CONFIGURED",
    val immichAssetId: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
