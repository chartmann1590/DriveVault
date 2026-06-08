package com.drivevault.dashcam.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.drivevault.dashcam.data.repository.SettingsRepository
import com.drivevault.dashcam.immich.ImmichClient
import com.drivevault.dashcam.immich.ImmichResult
import com.drivevault.dashcam.immich.ImmichSyncWorker
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class SettingsUiState(
    val audioEnabled: Boolean = true,
    val videoQuality: String = "1080p",
    val fps: Int = 30,
    val bitratePreset: String = "BALANCED",
    val loopRecording: Boolean = false,
    val maxStorageMb: Long = 4096,
    val autoDeleteOldest: Boolean = true,
    val defaultCameraMode: String = "BACK",
    val stabilization: Boolean = true,
    val mirrorFrontCamera: Boolean = true,
    val showSpeed: Boolean = true,
    val speedUnit: String = "MPH",
    val showGpsCoordinates: Boolean = true,
    val showHeading: Boolean = true,
    val showTimestamp: Boolean = true,
    val showMiniMap: Boolean = false,
    val overlayOpacity: Float = 0.7f,
    val mapRouteTrail: Boolean = true,
    val blurLocationOnShare: Boolean = false,
    val hideExactGps: Boolean = false,
    val confirmGpsShare: Boolean = true,
    val localOnlyMode: Boolean = false,
    val immichEnabled: Boolean = false,
    val immichServerUrl: String = "",
    val immichApiKey: String = "",
    val immichSyncMode: String = "MANUAL",
    val immichUploadVideos: Boolean = true,
    val immichUploadMetadata: Boolean = true,
    val immichUploadLockedOnly: Boolean = false,
    val immichAutoAlbum: Boolean = true,
    val immichAlbumName: String = "DriveVault Dashcam",
    val immichConnectionStatus: String? = null,
    val isTestingConnection: Boolean = false,
    val firebaseCrashlyticsEnabled: Boolean = false,
    val firebaseAnalyticsEnabled: Boolean = false,
    val firebasePerformanceEnabled: Boolean = false,
    val firebaseRemoteConfigEnabled: Boolean = false,
    val firebaseMessagingEnabled: Boolean = false,
    val firebaseFirestoreEnabled: Boolean = false,
    val firebaseStorageEnabled: Boolean = false,
    val firebaseAllowClipUpload: Boolean = false,
    val firebaseAllowLocationUpload: Boolean = false
)

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val settings = SettingsRepository(application)
    private val immichClient = ImmichClient(application)

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val flows: List<Flow<*>> = listOf(
                settings.audioEnabled,
                settings.videoQuality,
                settings.fps,
                settings.bitratePreset,
                settings.loopRecording,
                settings.maxStorageMb,
                settings.autoDeleteOldest,
                settings.defaultCameraMode,
                settings.stabilization,
                settings.mirrorFrontCamera,
                settings.showSpeed,
                settings.speedUnit,
                settings.showGpsCoordinates,
                settings.showHeading,
                settings.showTimestamp,
                settings.showMiniMap,
                settings.overlayOpacity,
                settings.mapRouteTrail,
                settings.blurLocationOnShare,
                settings.hideExactGps,
                settings.confirmGpsShare,
                settings.localOnlyMode,
                settings.immichEnabled,
                settings.immichSyncMode,
                settings.immichUploadVideos,
                settings.immichUploadMetadata,
                settings.immichUploadLockedOnly,
                settings.immichAutoAlbum,
                settings.immichAlbumName,
                settings.firebaseCrashlyticsEnabled,
                settings.firebaseAnalyticsEnabled,
                settings.firebasePerformanceEnabled,
                settings.firebaseRemoteConfigEnabled,
                settings.firebaseMessagingEnabled,
                settings.firebaseFirestoreEnabled,
                settings.firebaseStorageEnabled,
                settings.firebaseAllowClipUpload,
                settings.firebaseAllowLocationUpload
            )
            @Suppress("UNCHECKED_CAST")
            combine(flows) { values ->
                _uiState.update { state ->
                    state.copy(
                        audioEnabled = values[0] as Boolean,
                        videoQuality = values[1] as String,
                        fps = values[2] as Int,
                        bitratePreset = values[3] as String,
                        loopRecording = values[4] as Boolean,
                        maxStorageMb = values[5] as Long,
                        autoDeleteOldest = values[6] as Boolean,
                        defaultCameraMode = values[7] as String,
                        stabilization = values[8] as Boolean,
                        mirrorFrontCamera = values[9] as Boolean,
                        showSpeed = values[10] as Boolean,
                        speedUnit = values[11] as String,
                        showGpsCoordinates = values[12] as Boolean,
                        showHeading = values[13] as Boolean,
                        showTimestamp = values[14] as Boolean,
                        showMiniMap = values[15] as Boolean,
                        overlayOpacity = values[16] as Float,
                        mapRouteTrail = values[17] as Boolean,
                        blurLocationOnShare = values[18] as Boolean,
                        hideExactGps = values[19] as Boolean,
                        confirmGpsShare = values[20] as Boolean,
                        localOnlyMode = values[21] as Boolean,
                        immichEnabled = values[22] as Boolean,
                        immichSyncMode = values[23] as String,
                        immichUploadVideos = values[24] as Boolean,
                        immichUploadMetadata = values[25] as Boolean,
                        immichUploadLockedOnly = values[26] as Boolean,
                        immichAutoAlbum = values[27] as Boolean,
                        immichAlbumName = values[28] as String,
                        firebaseCrashlyticsEnabled = values[29] as Boolean,
                        firebaseAnalyticsEnabled = values[30] as Boolean,
                        firebasePerformanceEnabled = values[31] as Boolean,
                        firebaseRemoteConfigEnabled = values[32] as Boolean,
                        firebaseMessagingEnabled = values[33] as Boolean,
                        firebaseFirestoreEnabled = values[34] as Boolean,
                        firebaseStorageEnabled = values[35] as Boolean,
                        firebaseAllowClipUpload = values[36] as Boolean,
                        firebaseAllowLocationUpload = values[37] as Boolean
                    )
                }
            }.collect()
        }
        _uiState.update {
            it.copy(
                immichServerUrl = settings.immichServerUrl,
                immichApiKey = settings.immichApiKey
            )
        }
    }

    fun setAudioEnabled(v: Boolean) { viewModelScope.launch { settings.setAudioEnabled(v) } }
    fun setVideoQuality(v: String) { viewModelScope.launch { settings.setVideoQuality(v) } }
    fun setFps(v: Int) { viewModelScope.launch { settings.setFps(v) } }
    fun setBitratePreset(v: String) { viewModelScope.launch { settings.setBitratePreset(v) } }
    fun setLoopRecording(v: Boolean) { viewModelScope.launch { settings.setLoopRecording(v) } }
    fun setMaxStorageMb(v: Long) { viewModelScope.launch { settings.setMaxStorageMb(v) } }
    fun setAutoDeleteOldest(v: Boolean) { viewModelScope.launch { settings.setAutoDeleteOldest(v) } }
    fun setDefaultCameraMode(v: String) { viewModelScope.launch { settings.setDefaultCameraMode(v) } }
    fun setStabilization(v: Boolean) { viewModelScope.launch { settings.setStabilization(v) } }
    fun setMirrorFrontCamera(v: Boolean) { viewModelScope.launch { settings.setMirrorFrontCamera(v) } }
    fun setShowSpeed(v: Boolean) { viewModelScope.launch { settings.setShowSpeed(v) } }
    fun setSpeedUnit(v: String) { viewModelScope.launch { settings.setSpeedUnit(v) } }
    fun setShowGpsCoordinates(v: Boolean) { viewModelScope.launch { settings.setShowGpsCoordinates(v) } }
    fun setShowHeading(v: Boolean) { viewModelScope.launch { settings.setShowHeading(v) } }
    fun setShowTimestamp(v: Boolean) { viewModelScope.launch { settings.setShowTimestamp(v) } }
    fun setShowMiniMap(v: Boolean) { viewModelScope.launch { settings.setShowMiniMap(v) } }
    fun setOverlayOpacity(v: Float) { viewModelScope.launch { settings.setOverlayOpacity(v) } }
    fun setMapRouteTrail(v: Boolean) { viewModelScope.launch { settings.setMapRouteTrail(v) } }
    fun setBlurLocationOnShare(v: Boolean) { viewModelScope.launch { settings.setBlurLocationOnShare(v) } }
    fun setHideExactGps(v: Boolean) { viewModelScope.launch { settings.setHideExactGps(v) } }
    fun setConfirmGpsShare(v: Boolean) { viewModelScope.launch { settings.setConfirmGpsShare(v) } }
    fun setLocalOnlyMode(v: Boolean) { viewModelScope.launch { settings.setLocalOnlyMode(v) } }

    fun setImmichEnabled(v: Boolean) {
        viewModelScope.launch {
            settings.setImmichEnabled(v)
            if (v) ImmichSyncWorker.scheduleSync(getApplication(), _uiState.value.immichSyncMode)
            else ImmichSyncWorker.cancelSync(getApplication())
        }
    }

    fun setImmichServerUrl(v: String) {
        settings.immichServerUrl = v
        _uiState.update { it.copy(immichServerUrl = v) }
    }

    fun setImmichApiKey(v: String) {
        settings.immichApiKey = v
        _uiState.update { it.copy(immichApiKey = v) }
    }

    fun setImmichSyncMode(v: String) {
        viewModelScope.launch {
            settings.setImmichSyncMode(v)
            if (_uiState.value.immichEnabled) ImmichSyncWorker.scheduleSync(getApplication(), v)
        }
    }

    fun setImmichUploadVideos(v: Boolean) { viewModelScope.launch { settings.setImmichUploadVideos(v) } }
    fun setImmichUploadMetadata(v: Boolean) { viewModelScope.launch { settings.setImmichUploadMetadata(v) } }
    fun setImmichUploadLockedOnly(v: Boolean) { viewModelScope.launch { settings.setImmichUploadLockedOnly(v) } }
    fun setImmichAutoAlbum(v: Boolean) { viewModelScope.launch { settings.setImmichAutoAlbum(v) } }
    fun setImmichAlbumName(v: String) { viewModelScope.launch { settings.setImmichAlbumName(v) } }
    fun setFirebaseCrashlyticsEnabled(v: Boolean) { viewModelScope.launch { settings.setFirebaseCrashlyticsEnabled(v) } }
    fun setFirebaseAnalyticsEnabled(v: Boolean) { viewModelScope.launch { settings.setFirebaseAnalyticsEnabled(v) } }
    fun setFirebasePerformanceEnabled(v: Boolean) { viewModelScope.launch { settings.setFirebasePerformanceEnabled(v) } }
    fun setFirebaseRemoteConfigEnabled(v: Boolean) { viewModelScope.launch { settings.setFirebaseRemoteConfigEnabled(v) } }
    fun setFirebaseMessagingEnabled(v: Boolean) { viewModelScope.launch { settings.setFirebaseMessagingEnabled(v) } }
    fun setFirebaseFirestoreEnabled(v: Boolean) {
        viewModelScope.launch {
            settings.setFirebaseFirestoreEnabled(v)
            if (!v && !_uiState.value.firebaseStorageEnabled) {
                settings.setFirebaseAllowLocationUpload(false)
            }
        }
    }
    fun setFirebaseStorageEnabled(v: Boolean) {
        viewModelScope.launch {
            settings.setFirebaseStorageEnabled(v)
            if (!v) {
                settings.setFirebaseAllowClipUpload(false)
                if (!_uiState.value.firebaseFirestoreEnabled) {
                    settings.setFirebaseAllowLocationUpload(false)
                }
            }
        }
    }
    fun setFirebaseAllowClipUpload(v: Boolean) {
        viewModelScope.launch {
            settings.setFirebaseAllowClipUpload(v && _uiState.value.firebaseStorageEnabled)
        }
    }
    fun setFirebaseAllowLocationUpload(v: Boolean) {
        viewModelScope.launch {
            settings.setFirebaseAllowLocationUpload(v && (_uiState.value.firebaseFirestoreEnabled || _uiState.value.firebaseStorageEnabled))
        }
    }

    fun testConnection() {
        viewModelScope.launch {
            _uiState.update { it.copy(isTestingConnection = true, immichConnectionStatus = null) }
            when (val result = immichClient.validateConnection()) {
                is ImmichResult.Success -> {
                    _uiState.update { it.copy(isTestingConnection = false, immichConnectionStatus = "Connected as ${result.data.name}") }
                }
                is ImmichResult.AuthError -> {
                    _uiState.update { it.copy(isTestingConnection = false, immichConnectionStatus = "Auth failed: ${result.message}") }
                }
                is ImmichResult.NetworkError -> {
                    _uiState.update { it.copy(isTestingConnection = false, immichConnectionStatus = "Network error: ${result.message}") }
                }
                is ImmichResult.Error -> {
                    _uiState.update { it.copy(isTestingConnection = false, immichConnectionStatus = "Error: ${result.message}") }
                }
            }
        }
    }
}
