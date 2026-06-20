package com.drivevault.dashcam.domain.model

import com.drivevault.dashcam.data.local.entity.ClipEntity
import org.junit.Assert.*
import org.junit.Test

class ClipMappingTest {

    private val entity = ClipEntity(
        id = 1L,
        fileUri = "/data/clips/test.mp4",
        thumbnailUri = "/data/clips/test_thumb.jpg",
        secondaryFileUri = "/data/clips/test_front.mp4",
        secondaryThumbnailUri = "/data/clips/test_front_thumb.jpg",
        startTimeMillis = 1000000L,
        endTimeMillis = 1060000L,
        durationMillis = 60000L,
        cameraMode = "BACK",
        width = 1920,
        height = 1080,
        fps = 30,
        bitrate = 8_000_000,
        audioEnabled = true,
        overlayEnabled = false,
        miniMapEnabled = false,
        locked = false,
        startLatitude = 40.7128,
        startLongitude = -74.006,
        endLatitude = 40.7138,
        endLongitude = -74.007,
        averageSpeedMps = 15.0,
        maxSpeedMps = 25.0,
        startHeadingDegrees = 90f,
        endHeadingDegrees = 180f,
        immichStatus = "NOT_CONFIGURED",
        immichAssetId = null,
        immichSecondaryAssetId = null,
        fileSizeBytes = 50_000_000L,
        interrupted = false,
        createdAt = 1700000000000L,
        updatedAt = 1700000000000L
    )

    @Test
    fun toDomain_preservesAllFields() {
        val clip = entity.toDomain()

        assertEquals(1L, clip.id)
        assertEquals("/data/clips/test.mp4", clip.fileUri)
        assertEquals("/data/clips/test_thumb.jpg", clip.thumbnailUri)
        assertEquals("/data/clips/test_front.mp4", clip.secondaryFileUri)
        assertEquals(CameraMode.BACK, clip.cameraMode)
        assertEquals(1920, clip.width)
        assertEquals(1080, clip.height)
        assertEquals(30, clip.fps)
        assertEquals(60_000L, clip.durationMillis)
        assertTrue(clip.audioEnabled)
        assertFalse(clip.overlayEnabled)
        assertEquals(40.7128, clip.startLatitude, 0.0001)
        assertEquals(-74.006, clip.startLongitude, 0.0001)
        assertEquals(15.0, clip.averageSpeedMps, 0.001)
        assertEquals(90f, clip.startHeadingDegrees)
        assertEquals(ImmichStatus.NOT_CONFIGURED, clip.immichStatus)
        assertNull(clip.immichAssetId)
        assertEquals(50_000_000L, clip.fileSizeBytes)
        assertFalse(clip.interrupted)
    }

    @Test
    fun roundTrip_entityToDomainToEntity() {
        val clip = entity.toDomain()
        val back = clip.toEntity()

        assertEquals(entity.id, back.id)
        assertEquals(entity.fileUri, back.fileUri)
        assertEquals(entity.cameraMode, back.cameraMode)
        assertEquals(entity.durationMillis, back.durationMillis)
        assertEquals(entity.startLatitude, back.startLatitude, 0.0001)
        assertEquals(entity.fileSizeBytes, back.fileSizeBytes)
        assertEquals(entity.immichStatus, back.immichStatus)
    }

    @Test
    fun toDomain_unknownCameraMode_fallsBackToBack() {
        val badEntity = entity.copy(cameraMode = "INVALID")
        val clip = badEntity.toDomain()
        assertEquals(CameraMode.BACK, clip.cameraMode)
    }

    @Test
    fun toDomain_unknownImmichStatus_fallsBackToNotConfigured() {
        val badEntity = entity.copy(immichStatus = "INVALID")
        val clip = badEntity.toDomain()
        assertEquals(ImmichStatus.NOT_CONFIGURED, clip.immichStatus)
    }

    @Test
    fun toDomain_allCameraModes_mapCorrectly() {
        listOf("BACK", "FRONT", "DUAL").forEach { mode ->
            val e = entity.copy(cameraMode = mode)
            val clip = e.toDomain()
            assertEquals(mode, clip.cameraMode.name)
        }
    }

    @Test
    fun toDomain_allImmichStatuses_mapCorrectly() {
        listOf("NOT_CONFIGURED", "PENDING", "UPLOADING", "UPLOADED", "FAILED").forEach { status ->
            val e = entity.copy(immichStatus = status)
            val clip = e.toDomain()
            assertEquals(status, clip.immichStatus.name)
        }
    }

    @Test
    fun toEntity_allCameraModes_mapCorrectly() {
        CameraMode.entries.forEach { mode ->
            val clip = entity.toDomain().copy(cameraMode = mode)
            val back = clip.toEntity()
            assertEquals(mode.name, back.cameraMode)
        }
    }

    @Test
    fun interruptedField_isPreserved() {
        val interruptedEntity = entity.copy(interrupted = true)
        val clip = interruptedEntity.toDomain()
        assertTrue(clip.interrupted)
        val back = clip.toEntity()
        assertTrue(back.interrupted)
    }
}
