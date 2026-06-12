package com.drivevault.dashcam.data.repository

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val encryptedPrefs: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        "drivevault_secure",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    object Keys {
        val ONBOARDING_COMPLETE = booleanPreferencesKey("onboarding_complete")
        val AUDIO_ENABLED = booleanPreferencesKey("audio_enabled")
        val VIDEO_QUALITY = stringPreferencesKey("video_quality")
        val FPS = intPreferencesKey("fps")
        val BITRATE_PRESET = stringPreferencesKey("bitrate_preset")
        val LOOP_RECORDING = booleanPreferencesKey("loop_recording")
        val MAX_STORAGE_MB = longPreferencesKey("max_storage_mb")
        val AUTO_DELETE_OLDEST = booleanPreferencesKey("auto_delete_oldest")
        val DEFAULT_CAMERA_MODE = stringPreferencesKey("default_camera_mode")
        val STABILIZATION = booleanPreferencesKey("stabilization")
        val TORCH_ENABLED = booleanPreferencesKey("torch_enabled")
        val MIRROR_FRONT_CAMERA = booleanPreferencesKey("mirror_front_camera")
        val PIP_SIZE = stringPreferencesKey("pip_size")
        val SHOW_SPEED = booleanPreferencesKey("show_speed")
        val SPEED_UNIT = stringPreferencesKey("speed_unit")
        val SHOW_GPS_COORDINATES = booleanPreferencesKey("show_gps_coordinates")
        val SHOW_HEADING = booleanPreferencesKey("show_heading")
        val SHOW_TIMESTAMP = booleanPreferencesKey("show_timestamp")
        val SHOW_MINI_MAP = booleanPreferencesKey("show_mini_map")
        val OVERLAY_OPACITY = floatPreferencesKey("overlay_opacity")
        val OVERLAY_POSITION = stringPreferencesKey("overlay_position")
        val RECORD_OVERLAYS = booleanPreferencesKey("record_overlays")
        val MAP_ROUTE_TRAIL = booleanPreferencesKey("map_route_trail")
        val MAP_STYLE = stringPreferencesKey("map_style")
        val CACHE_MAP_TILES = booleanPreferencesKey("cache_map_tiles")
        val BLUR_LOCATION_ON_SHARE = booleanPreferencesKey("blur_location_on_share")
        val HIDE_EXACT_GPS = booleanPreferencesKey("hide_exact_gps")
        val CONFIRM_GPS_SHARE = booleanPreferencesKey("confirm_gps_share")
        val LOCAL_ONLY_MODE = booleanPreferencesKey("local_only_mode")
        val IMMICH_ENABLED = booleanPreferencesKey("immich_enabled")
        val IMMICH_SYNC_MODE = stringPreferencesKey("immich_sync_mode")
        val IMMICH_UPLOAD_VIDEOS = booleanPreferencesKey("immich_upload_videos")
        val IMMICH_UPLOAD_SNAPSHOTS = booleanPreferencesKey("immich_upload_snapshots")
        val IMMICH_UPLOAD_METADATA = booleanPreferencesKey("immich_upload_metadata")
        val IMMICH_UPLOAD_LOCKED_ONLY = booleanPreferencesKey("immich_upload_locked_only")
        val IMMICH_AUTO_ALBUM = booleanPreferencesKey("immich_auto_album")
        val IMMICH_ALBUM_NAME = stringPreferencesKey("immich_album_name")
        val FIREBASE_CRASHLYTICS_ENABLED = booleanPreferencesKey("firebase_crashlytics_enabled")
        val FIREBASE_ANALYTICS_ENABLED = booleanPreferencesKey("firebase_analytics_enabled")
        val FIREBASE_PERFORMANCE_ENABLED = booleanPreferencesKey("firebase_performance_enabled")
        val FIREBASE_REMOTE_CONFIG_ENABLED = booleanPreferencesKey("firebase_remote_config_enabled")
        val FIREBASE_MESSAGING_ENABLED = booleanPreferencesKey("firebase_messaging_enabled")
        val FIREBASE_FIRESTORE_ENABLED = booleanPreferencesKey("firebase_firestore_enabled")
        val FIREBASE_ALLOW_LOCATION_UPLOAD = booleanPreferencesKey("firebase_allow_location_upload")
        val VEHICLE_DETECTION_ENABLED = booleanPreferencesKey("vehicle_detection_enabled")
        val ALLOW_CLIP_SHARING = booleanPreferencesKey("allow_clip_sharing")
    }

    val onboardingComplete: Flow<Boolean> = context.dataStore.data.catch { if (it is IOException) emit(emptyPreferences()) else throw it }.map { it[Keys.ONBOARDING_COMPLETE] ?: false }

    val audioEnabled: Flow<Boolean> = context.dataStore.data.map { it[Keys.AUDIO_ENABLED] ?: true }
    val videoQuality: Flow<String> = context.dataStore.data.map { it[Keys.VIDEO_QUALITY] ?: "1080p" }
    val fps: Flow<Int> = context.dataStore.data.map { it[Keys.FPS] ?: 30 }
    val bitratePreset: Flow<String> = context.dataStore.data.map { it[Keys.BITRATE_PRESET] ?: "BALANCED" }
    val loopRecording: Flow<Boolean> = context.dataStore.data.map { it[Keys.LOOP_RECORDING] ?: false }
    val maxStorageMb: Flow<Long> = context.dataStore.data.map { it[Keys.MAX_STORAGE_MB] ?: 4096L }
    val autoDeleteOldest: Flow<Boolean> = context.dataStore.data.map { it[Keys.AUTO_DELETE_OLDEST] ?: true }
    val defaultCameraMode: Flow<String> = context.dataStore.data.map { it[Keys.DEFAULT_CAMERA_MODE] ?: "BACK" }
    val stabilization: Flow<Boolean> = context.dataStore.data.map { it[Keys.STABILIZATION] ?: true }
    val torchEnabled: Flow<Boolean> = context.dataStore.data.map { it[Keys.TORCH_ENABLED] ?: false }
    val mirrorFrontCamera: Flow<Boolean> = context.dataStore.data.map { it[Keys.MIRROR_FRONT_CAMERA] ?: true }
    val pipSize: Flow<String> = context.dataStore.data.map { it[Keys.PIP_SIZE] ?: "MEDIUM" }

    val showSpeed: Flow<Boolean> = context.dataStore.data.map { it[Keys.SHOW_SPEED] ?: true }
    val speedUnit: Flow<String> = context.dataStore.data.map { it[Keys.SPEED_UNIT] ?: "MPH" }
    val showGpsCoordinates: Flow<Boolean> = context.dataStore.data.map { it[Keys.SHOW_GPS_COORDINATES] ?: true }
    val showHeading: Flow<Boolean> = context.dataStore.data.map { it[Keys.SHOW_HEADING] ?: true }
    val showTimestamp: Flow<Boolean> = context.dataStore.data.map { it[Keys.SHOW_TIMESTAMP] ?: true }
    val showMiniMap: Flow<Boolean> = context.dataStore.data.map { it[Keys.SHOW_MINI_MAP] ?: false }
    val overlayOpacity: Flow<Float> = context.dataStore.data.map { it[Keys.OVERLAY_OPACITY] ?: 0.7f }
    val overlayPosition: Flow<String> = context.dataStore.data.map { it[Keys.OVERLAY_POSITION] ?: "BOTTOM_LEFT" }
    val recordOverlays: Flow<Boolean> = context.dataStore.data.map { it[Keys.RECORD_OVERLAYS] ?: false }

    val mapRouteTrail: Flow<Boolean> = context.dataStore.data.map { it[Keys.MAP_ROUTE_TRAIL] ?: true }
    val mapStyle: Flow<String> = context.dataStore.data.map { it[Keys.MAP_STYLE] ?: "DEFAULT" }
    val cacheMapTiles: Flow<Boolean> = context.dataStore.data.map { it[Keys.CACHE_MAP_TILES] ?: true }

    val blurLocationOnShare: Flow<Boolean> = context.dataStore.data.map { it[Keys.BLUR_LOCATION_ON_SHARE] ?: false }
    val hideExactGps: Flow<Boolean> = context.dataStore.data.map { it[Keys.HIDE_EXACT_GPS] ?: false }
    val confirmGpsShare: Flow<Boolean> = context.dataStore.data.map { it[Keys.CONFIRM_GPS_SHARE] ?: true }
    val localOnlyMode: Flow<Boolean> = context.dataStore.data.map { it[Keys.LOCAL_ONLY_MODE] ?: false }

    val immichEnabled: Flow<Boolean> = context.dataStore.data.map { it[Keys.IMMICH_ENABLED] ?: false }
    val immichSyncMode: Flow<String> = context.dataStore.data.map { it[Keys.IMMICH_SYNC_MODE] ?: "MANUAL" }
    val immichUploadVideos: Flow<Boolean> = context.dataStore.data.map { it[Keys.IMMICH_UPLOAD_VIDEOS] ?: true }
    val immichUploadSnapshots: Flow<Boolean> = context.dataStore.data.map { it[Keys.IMMICH_UPLOAD_SNAPSHOTS] ?: true }
    val immichUploadMetadata: Flow<Boolean> = context.dataStore.data.map { it[Keys.IMMICH_UPLOAD_METADATA] ?: true }
    val immichUploadLockedOnly: Flow<Boolean> = context.dataStore.data.map { it[Keys.IMMICH_UPLOAD_LOCKED_ONLY] ?: false }
    val immichAutoAlbum: Flow<Boolean> = context.dataStore.data.map { it[Keys.IMMICH_AUTO_ALBUM] ?: true }
    val immichAlbumName: Flow<String> = context.dataStore.data.map { it[Keys.IMMICH_ALBUM_NAME] ?: "DriveVault Dashcam" }

    val firebaseCrashlyticsEnabled: Flow<Boolean> = context.dataStore.data.map { it[Keys.FIREBASE_CRASHLYTICS_ENABLED] ?: false }
    val firebaseAnalyticsEnabled: Flow<Boolean> = context.dataStore.data.map { it[Keys.FIREBASE_ANALYTICS_ENABLED] ?: false }
    val firebasePerformanceEnabled: Flow<Boolean> = context.dataStore.data.map { it[Keys.FIREBASE_PERFORMANCE_ENABLED] ?: false }
    val firebaseRemoteConfigEnabled: Flow<Boolean> = context.dataStore.data.map { it[Keys.FIREBASE_REMOTE_CONFIG_ENABLED] ?: false }
    val firebaseMessagingEnabled: Flow<Boolean> = context.dataStore.data.map { it[Keys.FIREBASE_MESSAGING_ENABLED] ?: false }
    val firebaseFirestoreEnabled: Flow<Boolean> = context.dataStore.data.map { it[Keys.FIREBASE_FIRESTORE_ENABLED] ?: false }
    val firebaseAllowLocationUpload: Flow<Boolean> = context.dataStore.data.map { it[Keys.FIREBASE_ALLOW_LOCATION_UPLOAD] ?: false }
    val vehicleDetectionEnabled: Flow<Boolean> = context.dataStore.data.map { it[Keys.VEHICLE_DETECTION_ENABLED] ?: false }
    val allowClipSharing: Flow<Boolean> = context.dataStore.data.map { it[Keys.ALLOW_CLIP_SHARING] ?: false }

    var immichServerUrl: String
        get() = encryptedPrefs.getString("immich_server_url", "") ?: ""
        set(value) = encryptedPrefs.edit().putString("immich_server_url", value).apply()

    var immichApiKey: String
        get() = encryptedPrefs.getString("immich_api_key", "") ?: ""
        set(value) = encryptedPrefs.edit().putString("immich_api_key", value).apply()

    private suspend fun <T> setSetting(key: Preferences.Key<T>, value: T) {
        context.dataStore.edit { it[key] = value }
    }

    suspend fun setOnboardingComplete(complete: Boolean) = setSetting(Keys.ONBOARDING_COMPLETE, complete)
    suspend fun setAudioEnabled(enabled: Boolean) = setSetting(Keys.AUDIO_ENABLED, enabled)
    suspend fun setVideoQuality(quality: String) = setSetting(Keys.VIDEO_QUALITY, quality)
    suspend fun setFps(fps: Int) = setSetting(Keys.FPS, fps)
    suspend fun setBitratePreset(preset: String) = setSetting(Keys.BITRATE_PRESET, preset)
    suspend fun setLoopRecording(enabled: Boolean) = setSetting(Keys.LOOP_RECORDING, enabled)
    suspend fun setMaxStorageMb(mb: Long) = setSetting(Keys.MAX_STORAGE_MB, mb)
    suspend fun setAutoDeleteOldest(enabled: Boolean) = setSetting(Keys.AUTO_DELETE_OLDEST, enabled)
    suspend fun setDefaultCameraMode(mode: String) = setSetting(Keys.DEFAULT_CAMERA_MODE, mode)
    suspend fun setStabilization(enabled: Boolean) = setSetting(Keys.STABILIZATION, enabled)
    suspend fun setTorchEnabled(enabled: Boolean) = setSetting(Keys.TORCH_ENABLED, enabled)
    suspend fun setMirrorFrontCamera(enabled: Boolean) = setSetting(Keys.MIRROR_FRONT_CAMERA, enabled)
    suspend fun setPipSize(size: String) = setSetting(Keys.PIP_SIZE, size)
    suspend fun setShowSpeed(show: Boolean) = setSetting(Keys.SHOW_SPEED, show)
    suspend fun setSpeedUnit(unit: String) = setSetting(Keys.SPEED_UNIT, unit)
    suspend fun setShowGpsCoordinates(show: Boolean) = setSetting(Keys.SHOW_GPS_COORDINATES, show)
    suspend fun setShowHeading(show: Boolean) = setSetting(Keys.SHOW_HEADING, show)
    suspend fun setShowTimestamp(show: Boolean) = setSetting(Keys.SHOW_TIMESTAMP, show)
    suspend fun setShowMiniMap(show: Boolean) = setSetting(Keys.SHOW_MINI_MAP, show)
    suspend fun setOverlayOpacity(opacity: Float) = setSetting(Keys.OVERLAY_OPACITY, opacity)
    suspend fun setOverlayPosition(position: String) = setSetting(Keys.OVERLAY_POSITION, position)
    suspend fun setRecordOverlays(enabled: Boolean) = setSetting(Keys.RECORD_OVERLAYS, enabled)
    suspend fun setMapRouteTrail(enabled: Boolean) = setSetting(Keys.MAP_ROUTE_TRAIL, enabled)
    suspend fun setMapStyle(style: String) = setSetting(Keys.MAP_STYLE, style)
    suspend fun setCacheMapTiles(enabled: Boolean) = setSetting(Keys.CACHE_MAP_TILES, enabled)
    suspend fun setBlurLocationOnShare(enabled: Boolean) = setSetting(Keys.BLUR_LOCATION_ON_SHARE, enabled)
    suspend fun setHideExactGps(enabled: Boolean) = setSetting(Keys.HIDE_EXACT_GPS, enabled)
    suspend fun setConfirmGpsShare(enabled: Boolean) = setSetting(Keys.CONFIRM_GPS_SHARE, enabled)
    suspend fun setLocalOnlyMode(enabled: Boolean) = setSetting(Keys.LOCAL_ONLY_MODE, enabled)
    suspend fun setImmichEnabled(enabled: Boolean) = setSetting(Keys.IMMICH_ENABLED, enabled)
    suspend fun setImmichSyncMode(mode: String) = setSetting(Keys.IMMICH_SYNC_MODE, mode)
    suspend fun setImmichUploadVideos(enabled: Boolean) = setSetting(Keys.IMMICH_UPLOAD_VIDEOS, enabled)
    suspend fun setImmichUploadSnapshots(enabled: Boolean) = setSetting(Keys.IMMICH_UPLOAD_SNAPSHOTS, enabled)
    suspend fun setImmichUploadMetadata(enabled: Boolean) = setSetting(Keys.IMMICH_UPLOAD_METADATA, enabled)
    suspend fun setImmichUploadLockedOnly(enabled: Boolean) = setSetting(Keys.IMMICH_UPLOAD_LOCKED_ONLY, enabled)
    suspend fun setImmichAutoAlbum(enabled: Boolean) = setSetting(Keys.IMMICH_AUTO_ALBUM, enabled)
    suspend fun setImmichAlbumName(name: String) = setSetting(Keys.IMMICH_ALBUM_NAME, name)
    suspend fun setFirebaseCrashlyticsEnabled(enabled: Boolean) = setSetting(Keys.FIREBASE_CRASHLYTICS_ENABLED, enabled)
    suspend fun setFirebaseAnalyticsEnabled(enabled: Boolean) = setSetting(Keys.FIREBASE_ANALYTICS_ENABLED, enabled)
    suspend fun setFirebasePerformanceEnabled(enabled: Boolean) = setSetting(Keys.FIREBASE_PERFORMANCE_ENABLED, enabled)
    suspend fun setFirebaseRemoteConfigEnabled(enabled: Boolean) = setSetting(Keys.FIREBASE_REMOTE_CONFIG_ENABLED, enabled)
    suspend fun setFirebaseMessagingEnabled(enabled: Boolean) = setSetting(Keys.FIREBASE_MESSAGING_ENABLED, enabled)
    suspend fun setFirebaseFirestoreEnabled(enabled: Boolean) = setSetting(Keys.FIREBASE_FIRESTORE_ENABLED, enabled)
    suspend fun setFirebaseAllowLocationUpload(enabled: Boolean) = setSetting(Keys.FIREBASE_ALLOW_LOCATION_UPLOAD, enabled)
    suspend fun setVehicleDetectionEnabled(enabled: Boolean) = setSetting(Keys.VEHICLE_DETECTION_ENABLED, enabled)
    suspend fun setAllowClipSharing(enabled: Boolean) = setSetting(Keys.ALLOW_CLIP_SHARING, enabled)
}
