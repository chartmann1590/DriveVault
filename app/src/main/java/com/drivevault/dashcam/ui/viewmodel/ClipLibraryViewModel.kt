package com.drivevault.dashcam.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.drivevault.dashcam.app.DriveVaultApp
import com.drivevault.dashcam.data.local.DriveVaultDatabase
import com.drivevault.dashcam.domain.model.Clip
import com.drivevault.dashcam.domain.model.toDomain
import com.drivevault.dashcam.domain.repository.ClipRepository
import com.drivevault.dashcam.immich.ImmichSyncWorker
import com.drivevault.dashcam.storage.StorageManager
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class ClipLibraryUiState(
    val clips: List<Clip> = emptyList(),
    val selectedClipIds: Set<Long> = emptySet(),
    val isSelectMode: Boolean = false,
    val filter: ClipFilter = ClipFilter.ALL,
    val storageUsedMb: Long = 0,
    val storageMaxMb: Long = 0,
    val clipCount: Int = 0
)

enum class ClipFilter {
    ALL, TODAY, THIS_WEEK, LOCKED, SYNCED, UNSYNCED, FRONT, BACK, DUAL
}

class ClipLibraryViewModel(application: Application) : AndroidViewModel(application) {

    private val clipRepo = ClipRepository(
        DriveVaultDatabase.getInstance(application).clipDao(),
        DriveVaultDatabase.getInstance(application).locationSampleDao(),
        DriveVaultDatabase.getInstance(application).headingSampleDao(),
        DriveVaultDatabase.getInstance(application).snapshotDao()
    )
    private val storageManager = StorageManager(application)

    private val _uiState = MutableStateFlow(ClipLibraryUiState())
    val uiState: StateFlow<ClipLibraryUiState> = _uiState.asStateFlow()

    private val _filter = MutableStateFlow(ClipFilter.ALL)

    init {
        viewModelScope.launch {
            combine(
                clipRepo.observeAllClips(),
                _filter,
                storageManager.observeStorageInfo()
            ) { clips, filter, storage ->
                val filtered = when (filter) {
                    ClipFilter.TODAY -> {
                        val startOfDay = java.util.Calendar.getInstance().apply {
                            set(java.util.Calendar.HOUR_OF_DAY, 0)
                            set(java.util.Calendar.MINUTE, 0)
                            set(java.util.Calendar.SECOND, 0)
                        }.timeInMillis
                        clips.filter { it.startTimeMillis >= startOfDay }
                    }
                    ClipFilter.THIS_WEEK -> {
                        val cal = java.util.Calendar.getInstance()
                        cal.set(java.util.Calendar.DAY_OF_WEEK, cal.firstDayOfWeek)
                        cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
                        val startOfWeek = cal.timeInMillis
                        clips.filter { it.startTimeMillis >= startOfWeek }
                    }
                    ClipFilter.LOCKED -> clips.filter { it.locked }
                    ClipFilter.SYNCED -> clips.filter { it.immichStatus.name == "UPLOADED" }
                    ClipFilter.UNSYNCED -> clips.filter { it.immichStatus.name != "UPLOADED" }
                    ClipFilter.FRONT -> clips.filter { it.cameraMode.name == "FRONT" }
                    ClipFilter.BACK -> clips.filter { it.cameraMode.name == "BACK" }
                    ClipFilter.DUAL -> clips.filter { it.cameraMode.name == "DUAL" }
                    ClipFilter.ALL -> clips
                }
                _uiState.update {
                    it.copy(
                        clips = filtered,
                        filter = filter,
                        storageUsedMb = storage.usedMb,
                        storageMaxMb = storage.maxMb,
                        clipCount = storage.clipCount
                    )
                }
            }.collect()
        }
    }

    fun setFilter(filter: ClipFilter) { _filter.value = filter }

    fun toggleClipSelection(clipId: Long) {
        _uiState.update { state ->
            val selected = state.selectedClipIds.toMutableSet()
            if (selected.contains(clipId)) selected.remove(clipId) else selected.add(clipId)
            state.copy(selectedClipIds = selected, isSelectMode = selected.isNotEmpty())
        }
    }

    fun clearSelection() {
        _uiState.update { it.copy(selectedClipIds = emptySet(), isSelectMode = false) }
    }

    fun selectAll() {
        _uiState.update { it.copy(selectedClipIds = it.clips.map { c -> c.id }.toSet(), isSelectMode = true) }
    }

    fun deleteSelected() {
        viewModelScope.launch {
            val ids = _uiState.value.selectedClipIds.toList()
            storageManager.deleteClipsWithFiles(ids)
            clearSelection()
        }
    }

    fun lockSelected(locked: Boolean) {
        viewModelScope.launch {
            _uiState.value.selectedClipIds.forEach { clipRepo.setClipLocked(it, locked) }
        }
    }

    fun uploadSelectedToImmich() {
        val ids = _uiState.value.selectedClipIds.toList()
        viewModelScope.launch {
            clipRepo.markPendingForUpload(ids)
            ImmichSyncWorker.syncNow(getApplication())
        }
    }

    fun deleteClip(clipId: Long) {
        viewModelScope.launch { storageManager.deleteClipWithFiles(clipId) }
    }
}
