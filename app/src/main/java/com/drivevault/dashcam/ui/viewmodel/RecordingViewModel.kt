package com.drivevault.dashcam.ui.viewmodel

import android.app.Application
import android.content.Intent
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.drivevault.dashcam.data.repository.SettingsRepository
import com.drivevault.dashcam.domain.model.TelemetryState
import com.drivevault.dashcam.recording.RecordingService
import com.drivevault.dashcam.recording.RecordingState
import com.drivevault.dashcam.sensors.TelemetryManager
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class RecordingScreenUiState(
    val recordingState: RecordingState = RecordingState(),
    val telemetryState: TelemetryState = TelemetryState(),
    val speedUnit: String = "MPH",
    val showSpeed: Boolean = true,
    val showGps: Boolean = true,
    val showHeading: Boolean = true,
    val showTimestamp: Boolean = true,
    val showMiniMap: Boolean = false,
    val showOverlay: Boolean = true,
    val audioEnabled: Boolean = true,
    val cameraMode: String = "BACK",
    val isDualSupported: Boolean = false
)

class RecordingViewModel(application: Application) : AndroidViewModel(application) {

    private val settings = SettingsRepository(application)
    private val telemetryManager = TelemetryManager(
        application,
        LocationServices.getFusedLocationProviderClient(application)
    )

    private val _uiState = MutableStateFlow(RecordingScreenUiState())
    val uiState: StateFlow<RecordingScreenUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val flows: List<Flow<*>> = listOf(
                RecordingService.recordingState,
                telemetryManager.telemetryState,
                settings.speedUnit,
                settings.showSpeed,
                settings.showGpsCoordinates,
                settings.showHeading,
                settings.showTimestamp,
                settings.showMiniMap,
                settings.audioEnabled,
                settings.defaultCameraMode
            )
            @Suppress("UNCHECKED_CAST")
            combine(flows) { values ->
                val recState = values[0] as RecordingState
                val vmTeleState = values[1] as TelemetryState
                RecordingScreenUiState(
                    recordingState = recState,
                    // During recording, use the service's telemetry to avoid running two
                    // concurrent GPS subscriptions. Fall back to the ViewModel's own manager
                    // for the standby overlay when the service is not active.
                    telemetryState = if (recState.isRecording) recState.telemetryState else vmTeleState,
                    speedUnit = values[2] as String,
                    showSpeed = values[3] as Boolean,
                    showGps = values[4] as Boolean,
                    showHeading = values[5] as Boolean,
                    showTimestamp = values[6] as Boolean,
                    showMiniMap = values[7] as Boolean,
                    audioEnabled = values[8] as Boolean,
                    cameraMode = values[9] as String
                )
            }.collect { _uiState.value = it }
        }
        viewModelScope.launch {
            RecordingService.recordingState
                .map { it.isRecording }
                .distinctUntilChanged()
                .collect { isRecording ->
                    if (isRecording) telemetryManager.stop() else telemetryManager.start()
                }
        }
        telemetryManager.start()
    }

    fun toggleRecording() {
        val app = getApplication<Application>()
        runCatching {
            if (_uiState.value.recordingState.isRecording) {
                val intent = Intent(app, RecordingService::class.java).apply {
                    action = RecordingService.ACTION_STOP
                }
                app.startService(intent)
            } else {
                val intent = Intent(app, RecordingService::class.java)
                app.startForegroundService(intent)
            }
        }.onFailure { error ->
            Toast.makeText(app, "Recording could not start: ${error.message ?: "permission or service error"}", Toast.LENGTH_LONG).show()
        }
    }

    fun lockClip() {
        val app = getApplication<Application>()
        val intent = Intent(app, RecordingService::class.java).apply {
            action = RecordingService.ACTION_LOCK
        }
        app.startService(intent)
    }

    fun setAudioEnabled(enabled: Boolean) {
        viewModelScope.launch { settings.setAudioEnabled(enabled) }
    }

    fun setCameraMode(mode: String) {
        viewModelScope.launch { settings.setDefaultCameraMode(mode) }
    }

    fun toggleOverlay() {
        _uiState.update { it.copy(showOverlay = !it.showOverlay) }
    }

    fun toggleMiniMap() {
        viewModelScope.launch { settings.setShowMiniMap(!_uiState.value.showMiniMap) }
    }

    override fun onCleared() {
        super.onCleared()
        telemetryManager.stop()
    }
}
