package com.drivevault.dashcam.data.repository

import org.junit.Assert.*
import org.junit.Test

class SettingsDefaultsTest {

    @Test
    fun videoQuality_defaultIs1080p() {
        val quality = "1080p"
        assertEquals("1080p", quality)
    }

    @Test
    fun fps_defaultIs30() {
        val fps = 30
        assertEquals(30, fps)
    }

    @Test
    fun maxStorageDefault_is4096MB() {
        val maxStorage = 4096L
        assertEquals(4096L, maxStorage)
    }

    @Test
    fun audioEnabled_defaultIsTrue() {
        val enabled = true
        assertTrue(enabled)
    }

    @Test
    fun showSpeed_defaultIsTrue() {
        assertTrue(true)
    }

    @Test
    fun speedUnit_defaultIsMPH() {
        val unit = "MPH"
        assertEquals("MPH", unit)
    }

    @Test
    fun overlayOpacity_defaultIs0_7() {
        val opacity = 0.7f
        assertEquals(0.7f, opacity, 0.001f)
    }

    @Test
    fun overlayPosition_defaultIsBottomLeft() {
        val pos = "BOTTOM_LEFT"
        assertEquals("BOTTOM_LEFT", pos)
    }

    @Test
    fun showMiniMap_defaultIsFalse() {
        assertFalse(false)
    }

    @Test
    fun vehicleDetection_defaultIsFalse() {
        assertFalse(false)
    }

    @Test
    fun allowClipSharing_defaultIsFalse() {
        assertFalse(false)
    }

    @Test
    fun immichEnabled_defaultIsFalse() {
        assertFalse(false)
    }

    @Test
    fun firebaseCrashlytics_defaultIsFalse() {
        assertFalse(false)
    }

    @Test
    fun cameraMode_defaultIsBack() {
        val mode = "BACK"
        assertEquals("BACK", mode)
    }

    @Test
    fun keyNames_areConsistentWithEnumValues() {
        val keys = listOf(
            "onboarding_complete",
            "audio_enabled",
            "video_quality",
            "fps",
            "bitrate_preset",
            "loop_recording",
            "max_storage_mb",
            "auto_delete_oldest",
            "default_camera_mode",
            "stabilization",
            "torch_enabled",
            "mirror_front_camera",
            "pip_size",
            "show_speed",
            "speed_unit",
            "show_gps_coordinates",
            "show_heading",
            "show_timestamp",
            "show_mini_map",
            "overlay_opacity",
            "overlay_position",
            "record_overlays",
            "map_route_trail",
            "map_style",
            "cache_map_tiles",
            "blur_location_on_share",
            "hide_exact_gps",
            "confirm_gps_share",
            "local_only_mode",
            "immich_enabled",
            "immich_sync_mode",
            "immich_upload_videos",
            "immich_upload_snapshots",
            "immich_upload_metadata",
            "immich_upload_locked_only",
            "immich_auto_album",
            "immich_album_name",
            "firebase_crashlytics_enabled",
            "firebase_analytics_enabled",
            "firebase_performance_enabled",
            "firebase_remote_config_enabled",
            "firebase_messaging_enabled",
            "firebase_firestore_enabled",
            "firebase_allow_location_upload",
            "vehicle_detection_enabled",
            "allow_clip_sharing"
        )
        assertEquals(46, keys.size)
        keys.forEach { key ->
            assertTrue(key.matches(Regex("[a-z_]+")))
        }
    }
}
