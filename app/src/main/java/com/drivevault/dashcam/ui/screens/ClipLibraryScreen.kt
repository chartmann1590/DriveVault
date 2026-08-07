package com.drivevault.dashcam.ui.screens

import android.app.Activity
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.drivevault.dashcam.data.review.ReviewPrompter
import com.drivevault.dashcam.ui.components.ClipCard
import com.drivevault.dashcam.ui.components.StorageUsageCard
import com.drivevault.dashcam.ui.theme.*
import com.drivevault.dashcam.ui.viewmodel.ClipFilter
import com.drivevault.dashcam.ui.viewmodel.ClipLibraryUiState
import com.drivevault.dashcam.ui.viewmodel.ClipLibraryViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClipLibraryScreen(
    viewModel: ClipLibraryViewModel,
    onNavigateToClip: (Long) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    var showDeleteDialog by remember { mutableStateOf(false) }
    var deleteTarget by remember { mutableStateOf<Long?>(null) }
    val activity = LocalContext.current as? Activity

    // Seeing real saved clips is proof the dashcam is working — a genuine moment of delivered
    // value, unlike an empty-state app-open.
    LaunchedEffect(uiState.clips.isNotEmpty()) {
        if (uiState.clips.isNotEmpty()) {
            activity?.let { ReviewPrompter.maybeRequestReview(it) }
        }
    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Text(
                            if (uiState.isSelectMode) "${uiState.selectedClipIds.size} selected"
                            else "Clips",
                            style = MaterialTheme.typography.headlineSmall,
                            color = OnSurface
                        )
                    },
                    navigationIcon = {
                        if (uiState.isSelectMode) {
                            IconButton(onClick = viewModel::clearSelection) {
                                Icon(Icons.Filled.Close, "Clear selection", tint = OnSurface)
                            }
                        } else {
                            IconButton(onClick = onNavigateBack) {
                                Icon(Icons.Filled.ArrowBack, "Back", tint = OnSurface)
                            }
                        }
                    },
                    actions = {
                        if (uiState.isSelectMode) {
                            IconButton(onClick = viewModel::selectAll) {
                                Icon(Icons.Filled.SelectAll, "Select all", tint = OnSurface)
                            }
                            IconButton(onClick = { showDeleteDialog = true }) {
                                Icon(Icons.Filled.Delete, "Delete", tint = SafetyRed)
                            }
                            IconButton(onClick = viewModel::uploadSelectedToImmich) {
                                Icon(Icons.Filled.CloudUpload, "Upload", tint = ElectricBlue)
                            }
                            IconButton(onClick = { viewModel.lockSelected(true) }) {
                                Icon(Icons.Filled.Lock, "Lock", tint = AmberWarning)
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = DeepCharcoal
                    )
                )
                FilterChips(
                    selectedFilter = uiState.filter,
                    onFilterSelected = viewModel::setFilter
                )
            }
        },
        containerColor = DeepCharcoal
    ) { padding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            StorageUsageCard(
                usedMb = uiState.storageUsedMb,
                maxMb = uiState.storageMaxMb,
                clipCount = uiState.clipCount,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )

            if (uiState.clips.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Filled.Videocam,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = OnSurfaceVariant.copy(alpha = 0.3f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "No clips recorded yet",
                            style = MaterialTheme.typography.titleMedium,
                            color = OnSurfaceVariant
                        )
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(uiState.clips, key = { it.id }) { clip ->
                        ClipCard(
                            clip = clip,
                            onClick = {
                                if (uiState.isSelectMode) {
                                    viewModel.toggleClipSelection(clip.id)
                                } else {
                                    onNavigateToClip(clip.id)
                                }
                            },
                            isSelected = uiState.selectedClipIds.contains(clip.id),
                            isSelectMode = uiState.isSelectMode
                        )
                    }
                }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Clips?") },
            text = { Text("This will permanently delete ${uiState.selectedClipIds.size} clip(s). This cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteSelected()
                        showDeleteDialog = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = SafetyRed)
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun FilterChips(
    selectedFilter: ClipFilter,
    onFilterSelected: (ClipFilter) -> Unit
) {
    ScrollableTabRow(
        selectedTabIndex = selectedFilter.ordinal,
        containerColor = DeepCharcoal,
        contentColor = OnSurface,
        edgePadding = 16.dp,
        divider = {}
    ) {
        ClipFilter.entries.forEach { filter ->
            Tab(
                selected = filter == selectedFilter,
                onClick = { onFilterSelected(filter) },
                text = {
                    Text(
                        text = filter.displayName,
                        style = MaterialTheme.typography.labelMedium,
                        color = if (filter == selectedFilter) ElectricBlue else OnSurfaceVariant
                    )
                },
                selectedContentColor = ElectricBlue,
                unselectedContentColor = OnSurfaceVariant
            )
        }
    }
}

private val ClipFilter.displayName: String
    get() = when (this) {
        ClipFilter.ALL -> "All"
        ClipFilter.TODAY -> "Today"
        ClipFilter.THIS_WEEK -> "This Week"
        ClipFilter.LOCKED -> "Locked"
        ClipFilter.SYNCED -> "Synced"
        ClipFilter.UNSYNCED -> "Unsynced"
        ClipFilter.FRONT -> "Front"
        ClipFilter.BACK -> "Back"
        ClipFilter.DUAL -> "Dual"
    }
