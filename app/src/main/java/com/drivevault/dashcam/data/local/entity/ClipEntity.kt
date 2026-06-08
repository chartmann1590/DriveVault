package com.drivevault.dashcam.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "clips",
    indices = [
        Index("startTimeMillis"),
        Index("locked"),
        Index("immichStatus"),
        Index("cameraMode")
    ]
)
data class ClipEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val fileUri: String,
    val thumbnailUri: String? = null,
    val secondaryFileUri: String? = null,
    val secondaryThumbnailUri: String? = null,
    val startTimeMillis: Long,
    val endTimeMillis: Long,
    val durationMillis: Long,
    val cameraMode: String,
    val width: Int = 1920,
    val height: Int = 1080,
    val fps: Int = 30,
    val bitrate: Int = 8_000_000,
    val audioEnabled: Boolean = true,
    val overlayEnabled: Boolean = false,
    val miniMapEnabled: Boolean = false,
    val locked: Boolean = false,
    val startLatitude: Double = 0.0,
    val startLongitude: Double = 0.0,
    val endLatitude: Double = 0.0,
    val endLongitude: Double = 0.0,
    val averageSpeedMps: Double = 0.0,
    val maxSpeedMps: Double = 0.0,
    val startHeadingDegrees: Float = 0f,
    val endHeadingDegrees: Float = 0f,
    val immichStatus: String = "NOT_CONFIGURED",
    val immichAssetId: String? = null,
    val immichSecondaryAssetId: String? = null,
    val fileSizeBytes: Long = 0L,
    val interrupted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
