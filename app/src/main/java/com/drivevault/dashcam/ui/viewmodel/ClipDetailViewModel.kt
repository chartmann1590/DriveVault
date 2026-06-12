package com.drivevault.dashcam.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.drivevault.dashcam.data.local.DriveVaultDatabase
import com.drivevault.dashcam.data.repository.SettingsRepository
import com.drivevault.dashcam.domain.model.Clip
import com.drivevault.dashcam.domain.repository.ClipRepository
import com.drivevault.dashcam.`export`.ClipExportResult
import com.drivevault.dashcam.`export`.ClipExporter
import com.drivevault.dashcam.`export`.ClipTrimManager
import com.drivevault.dashcam.`export`.ExportOptions
import com.drivevault.dashcam.firebase.ClipShareManager
import com.drivevault.dashcam.firebase.ClipShareState
import com.drivevault.dashcam.immich.ImmichSyncWorker
import com.drivevault.dashcam.storage.StorageManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

data class ClipDetailUiState(
    val clip: Clip? = null,
    val isPlaying: Boolean = false,
    val showOverlay: Boolean = false,
    val exportResult: ClipExportResult? = null,
    val showExportSheet: Boolean = false,
    val shareState: ClipShareState = ClipShareState.Idle,
    val clipSharingEnabled: Boolean = false,
    // Trim sheet state
    val showTrimSheet: Boolean = false,
    val trimStartMs: Long = 0L,
    val trimEndMs: Long = 0L,
    val trimEstimatedBytes: Long = 0L
)

class ClipDetailViewModel(application: Application) : AndroidViewModel(application) {

    private val clipRepo = ClipRepository(
        DriveVaultDatabase.getInstance(application).clipDao(),
        DriveVaultDatabase.getInstance(application).locationSampleDao(),
        DriveVaultDatabase.getInstance(application).headingSampleDao(),
        DriveVaultDatabase.getInstance(application).snapshotDao()
    )
    private val exporter = ClipExporter(application)
    private val storageManager = StorageManager(application)
    private val shareManager = ClipShareManager(application)
    private val settings = SettingsRepository(application)

    private val _uiState = MutableStateFlow(ClipDetailUiState())
    val uiState: StateFlow<ClipDetailUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            settings.allowClipSharing.collect { enabled ->
                _uiState.update { it.copy(clipSharingEnabled = enabled) }
            }
        }
        viewModelScope.launch {
            shareManager.shareState.collect { state -> _uiState.update { it.copy(shareState = state) } }
        }
    }

    fun loadClip(clipId: Long) {
        viewModelScope.launch {
            clipRepo.observeClipById(clipId).collect { clip ->
                if (clip == null) {
                    _uiState.update { it.copy(clip = null) }
                    return@collect
                }
                val maxShareableMs = ClipTrimManager.maxShareableDurationMs(clip.fileSizeBytes, clip.durationMillis)
                val clampedEnd = maxShareableMs.coerceAtMost(clip.durationMillis)
                _uiState.update { state ->
                    state.copy(
                        clip = clip,
                        trimStartMs = 0L,
                        trimEndMs = clampedEnd,
                        trimEstimatedBytes = ClipTrimManager.estimateSize(clip.fileSizeBytes, clip.durationMillis, clampedEnd)
                    )
                }
            }
        }
    }

    fun updateTrimRange(startMs: Long, endMs: Long) {
        val clip = _uiState.value.clip ?: return
        val estimated = ClipTrimManager.estimateSize(clip.fileSizeBytes, clip.durationMillis, endMs - startMs)
        _uiState.update { it.copy(trimStartMs = startMs, trimEndMs = endMs, trimEstimatedBytes = estimated) }
    }

    fun openTrimSheet() { _uiState.update { it.copy(showTrimSheet = true) } }
    fun closeTrimSheet() { _uiState.update { it.copy(showTrimSheet = false) } }

    fun toggleLock() {
        val clip = _uiState.value.clip ?: return
        viewModelScope.launch {
            clipRepo.setClipLocked(clip.id, !clip.locked)
        }
    }

    fun deleteClip() {
        val clip = _uiState.value.clip ?: return
        viewModelScope.launch {
            storageManager.deleteClipWithFiles(clip.id)
        }
    }

    fun exportVideo() {
        val clip = _uiState.value.clip ?: return
        viewModelScope.launch {
            val result = exporter.exportClip(clip.id, ExportOptions(includeVideo = true, includeMetadata = false, includeGpx = false))
            _uiState.update { it.copy(exportResult = result, showExportSheet = false) }
        }
    }

    fun exportMetadata() {
        val clip = _uiState.value.clip ?: return
        viewModelScope.launch {
            val result = exporter.exportClip(clip.id, ExportOptions(includeVideo = false, includeMetadata = true, includeGpx = false))
            _uiState.update { it.copy(exportResult = result, showExportSheet = false) }
        }
    }

    fun exportGpx() {
        val clip = _uiState.value.clip ?: return
        viewModelScope.launch {
            val result = exporter.exportClip(clip.id, ExportOptions(includeVideo = false, includeMetadata = false, includeGpx = true))
            _uiState.update { it.copy(exportResult = result, showExportSheet = false) }
        }
    }

    fun exportAll() {
        val clip = _uiState.value.clip ?: return
        viewModelScope.launch {
            val result = exporter.exportClip(clip.id, ExportOptions())
            _uiState.update { it.copy(exportResult = result, showExportSheet = false) }
        }
    }

    fun saveToGallery() {
        val clip = _uiState.value.clip ?: return
        viewModelScope.launch { exporter.saveToGallery(clip.id) }
    }

    fun shareClip() {
        val clip = _uiState.value.clip ?: return
        viewModelScope.launch {
            val result = exporter.exportClip(clip.id, ExportOptions())
            _uiState.update { it.copy(exportResult = result, showExportSheet = false) }
        }
    }

    fun uploadToImmich() {
        val clip = _uiState.value.clip ?: return
        viewModelScope.launch {
            clipRepo.updateImmichStatus(clip.id, "PENDING", null)
            ImmichSyncWorker.syncNow(getApplication())
        }
    }

    fun showExportSheet() { _uiState.update { it.copy(showExportSheet = true) } }
    fun hideExportSheet() { _uiState.update { it.copy(showExportSheet = false) } }

    fun getShareIntent() = _uiState.value.clip?.let { clip ->
        _uiState.value.exportResult?.let { result ->
            exporter.createShareIntent(clip.id, result)
        }
    }

    /** Upload the full clip directly (only called when already under 50MB). */
    fun cloudShareClip() {
        val clip = _uiState.value.clip ?: return
        viewModelScope.launch { shareManager.shareClip(clip.fileUri, clip.fileSizeBytes) }
    }

    /** Trim [trimStartMs]..[trimEndMs] into a temp file, then upload. */
    fun cloudShareTrimmed() {
        val clip = _uiState.value.clip ?: return
        val state = _uiState.value
        _uiState.update { it.copy(showTrimSheet = false) }
        viewModelScope.launch {
            val needsTrim = state.trimStartMs > 0L || state.trimEndMs < clip.durationMillis
            if (!needsTrim) {
                shareManager.shareClip(clip.fileUri, clip.fileSizeBytes)
                return@launch
            }
            val cacheDir = getApplication<Application>().cacheDir
            _uiState.update { it.copy(shareState = ClipShareState.Uploading(0)) }
            try {
                val trimmed = ClipTrimManager.trim(clip.fileUri, cacheDir, state.trimStartMs, state.trimEndMs)
                try {
                    shareManager.shareClip(trimmed.absolutePath, trimmed.length())
                } finally {
                    trimmed.delete()
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(shareState = ClipShareState.Error("Trim failed: ${e.message}")) }
            }
        }
    }

    fun resetShareState() = shareManager.resetState()
}
