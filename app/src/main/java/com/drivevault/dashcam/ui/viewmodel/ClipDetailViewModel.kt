package com.drivevault.dashcam.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.drivevault.dashcam.data.local.DriveVaultDatabase
import com.drivevault.dashcam.domain.model.Clip
import com.drivevault.dashcam.domain.repository.ClipRepository
import com.drivevault.dashcam.`export`.ClipExportResult
import com.drivevault.dashcam.`export`.ClipExporter
import com.drivevault.dashcam.`export`.ExportOptions
import com.drivevault.dashcam.immich.ImmichSyncWorker
import com.drivevault.dashcam.storage.StorageManager
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class ClipDetailUiState(
    val clip: Clip? = null,
    val isPlaying: Boolean = false,
    val showOverlay: Boolean = false,
    val exportResult: ClipExportResult? = null,
    val showExportSheet: Boolean = false
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

    private val _uiState = MutableStateFlow(ClipDetailUiState())
    val uiState: StateFlow<ClipDetailUiState> = _uiState.asStateFlow()

    fun loadClip(clipId: Long) {
        viewModelScope.launch {
            clipRepo.observeClipById(clipId).collect { clip ->
                _uiState.update { it.copy(clip = clip) }
            }
        }
    }

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
}
