package com.drivevault.dashcam.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import kotlinx.coroutines.launch
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.FileProvider
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import java.util.Locale
import com.drivevault.dashcam.domain.model.Clip
import com.drivevault.dashcam.export.ClipTrimManager
import com.drivevault.dashcam.firebase.ClipShareState
import com.drivevault.dashcam.ui.components.ExportBottomSheet
import com.drivevault.dashcam.ui.components.SyncStatusBadge
import com.drivevault.dashcam.ui.theme.*
import com.drivevault.dashcam.ui.viewmodel.ClipDetailViewModel
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClipDetailScreen(
    clipId: Long,
    viewModel: ClipDetailViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val clip = uiState.clip
    val scope = rememberCoroutineScope()

    LaunchedEffect(clipId) { viewModel.loadClip(clipId) }
    LaunchedEffect(Unit) { viewModel.resetShareState() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Clip Detail",
                        style = MaterialTheme.typography.headlineSmall,
                        color = OnSurface
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Filled.ArrowBack, "Back", tint = OnSurface)
                    }
                },
                actions = {
                    if (clip != null) {
                        IconButton(onClick = viewModel::toggleLock) {
                            Icon(
                                if (clip.locked) Icons.Filled.Lock else Icons.Filled.LockOpen,
                                if (clip.locked) "Unlock" else "Lock",
                                tint = if (clip.locked) AmberWarning else OnSurfaceVariant
                            )
                        }
                        IconButton(onClick = viewModel::showExportSheet) {
                            Icon(Icons.Filled.MoreVert, "More", tint = OnSurface)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DeepCharcoal)
            )
        },
        containerColor = DeepCharcoal
    ) { padding ->
        if (clip != null) {
            Column(
                modifier = modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
            ) {
                VideoPlayer(
                    fileUri = clip.fileUri,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                )
                if (clip.secondaryFileUri != null) {
                    Text(
                        text = "Front camera",
                        modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 8.dp),
                        style = MaterialTheme.typography.titleSmall,
                        color = OnSurfaceVariant
                    )
                    VideoPlayer(
                        fileUri = clip.secondaryFileUri,
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(16f / 9f)
                    )
                }

                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            val date = Date(clip.startTimeMillis)
                            Text(
                                text = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.US).format(date),
                                style = MaterialTheme.typography.titleMedium,
                                color = OnSurface
                            )
                            Text(
                                text = "${clip.durationMillis / 1000}s | ${clip.cameraMode.displayName}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = OnSurfaceVariant
                            )
                        }
                        SyncStatusBadge(status = clip.immichStatus.name)
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilledTonalButton(
                            onClick = {
                                scope.launch {
                                    val shareIntent = viewModel.shareClipAndGetIntent()
                                    if (shareIntent != null) {
                                        context.startActivity(Intent.createChooser(shareIntent, "Share clip"))
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Filled.Share, null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Share")
                        }
                        FilledTonalButton(
                            onClick = {
                                viewModel.saveToGallery()
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Filled.SaveAlt, null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Gallery")
                        }
                    }

                    if (uiState.clipSharingEnabled && clip != null) {
                        val fileMb = clip.fileSizeBytes / (1024f * 1024f)
                        val overLimit = ClipTrimManager.needsTrimForSharing(clip.fileSizeBytes)
                        Spacer(modifier = Modifier.height(8.dp))
                        FilledTonalButton(
                            onClick = {
                                if (overLimit) viewModel.openTrimSheet()
                                else viewModel.cloudShareClip()
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = uiState.shareState is ClipShareState.Idle
                        ) {
                            Icon(
                                if (overLimit) Icons.Filled.ContentCut else Icons.Filled.CloudUpload,
                                null, modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (overLimit) "Trim & Share (24h link)" else "Cloud Share (24h link)")
                        }
                        // File size hint + limit note
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "%.1f MB".format(fileMb),
                                style = MaterialTheme.typography.labelSmall,
                                color = if (overLimit) SafetyRed else OnSurfaceVariant
                            )
                            Text(
                                text = if (overLimit) "⚠ Over 50MB limit — trim required" else "Limit: 50MB",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (overLimit) SafetyRed else OnSurfaceVariant
                            )
                        }
                    }

                    // Trim & Share bottom sheet
                    if (uiState.showTrimSheet && clip != null) {
                        TrimShareSheet(
                            clip = clip,
                            trimStartMs = uiState.trimStartMs,
                            trimEndMs = uiState.trimEndMs,
                            estimatedBytes = uiState.trimEstimatedBytes,
                            onDismiss = viewModel::closeTrimSheet,
                            onTrimChange = viewModel::updateTrimRange,
                            onUpload = viewModel::cloudShareTrimmed
                        )
                    }

                    when (val state = uiState.shareState) {
                        is ClipShareState.Uploading -> {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "Uploading… ${state.progressPercent}%",
                                style = MaterialTheme.typography.bodySmall,
                                color = OnSurfaceVariant
                            )
                            LinearProgressIndicator(
                                progress = { state.progressPercent / 100f },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        is ClipShareState.Success -> {
                            Spacer(modifier = Modifier.height(8.dp))
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = SurfaceContainer,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        "Link ready — expires in 24 hours",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = ElectricBlue
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        state.shareUrl,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = OnSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        OutlinedButton(
                                            onClick = { clipboard.setText(AnnotatedString(state.shareUrl)) },
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(Icons.Filled.ContentCopy, null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Copy")
                                        }
                                        OutlinedButton(
                                            onClick = {
                                                val intent = Intent(Intent.ACTION_SEND).apply {
                                                    type = "text/plain"
                                                    putExtra(Intent.EXTRA_TEXT, state.shareUrl)
                                                }
                                                context.startActivity(Intent.createChooser(intent, "Share link"))
                                            },
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(Icons.Filled.Share, null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Share")
                                        }
                                    }
                                }
                            }
                        }
                        is ClipShareState.Error -> {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "Upload failed: ${state.message}",
                                style = MaterialTheme.typography.bodySmall,
                                color = SafetyRed
                            )
                            TextButton(onClick = viewModel::cloudShareClip) { Text("Retry") }
                        }
                        else -> {}
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text("Details", style = MaterialTheme.typography.titleMedium, color = OnSurface)
                    Spacer(modifier = Modifier.height(8.dp))

                    DetailRow("Duration", "${clip.durationMillis / 1000} seconds")
                    DetailRow("Camera", clip.cameraMode.displayName)
                    if (clip.secondaryFileUri != null) {
                        DetailRow("Dual camera", "Rear + front streams recorded")
                    }
                    DetailRow("Resolution", "${clip.width}x${clip.height}")
                    DetailRow("Audio", if (clip.audioEnabled) "Yes" else "No")
                    DetailRow("Locked", if (clip.locked) "Yes" else "No")
                    if (clip.startLatitude != 0.0 && clip.startLongitude != 0.0) {
                        DetailRow("Start Location", String.format(Locale.US, "%.5f, %.5f", clip.startLatitude, clip.startLongitude))
                    }
                    if (clip.maxSpeedMps > 0) {
                        DetailRow("Max Speed", "${(clip.maxSpeedMps * 2.2394).toInt()} mph")
                        DetailRow("Avg Speed", "${(clip.averageSpeedMps * 2.23694).toInt()} mph")
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = viewModel::exportVideo,
                            modifier = Modifier.weight(1f)
                        ) { Text("Video") }
                        OutlinedButton(
                            onClick = viewModel::exportMetadata,
                            modifier = Modifier.weight(1f)
                        ) { Text("JSON") }
                        OutlinedButton(
                            onClick = viewModel::exportGpx,
                            modifier = Modifier.weight(1f)
                        ) { Text("GPX") }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedButton(
                        onClick = viewModel::uploadToImmich,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = ElectricBlue)
                    ) {
Icon(Icons.Filled.CloudUpload, null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Upload to Immich")
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedButton(
                        onClick = {
                            viewModel.deleteClip()
                            onNavigateBack()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = SafetyRed)
                    ) {
                        Icon(Icons.Filled.Delete, null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Delete Clip")
                    }
                }
            }
        }
    }

    if (uiState.showExportSheet) {
        ModalBottomSheet(
            onDismissRequest = viewModel::hideExportSheet,
            containerColor = SurfaceContainer
        ) {
            ExportBottomSheet(
                onDismiss = viewModel::hideExportSheet,
                onExportVideo = viewModel::exportVideo,
                onExportMetadata = viewModel::exportMetadata,
                onExportGpx = viewModel::exportGpx,
                onShare = {
                    scope.launch {
                        val intent = viewModel.shareClipAndGetIntent()
                        if (intent != null) {
                            context.startActivity(Intent.createChooser(intent, "Share clip"))
                        }
                    }
                },
                onSaveToGallery = viewModel::saveToGallery,
                onUploadToImmich = viewModel::uploadToImmich,
                immichEnabled = true
            )
        }
    }
}

@Composable
fun VideoPlayer(
    fileUri: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            val mediaItem = MediaItem.fromUri(fileUri)
            setMediaItem(mediaItem)
            prepare()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.release()
        }
    }

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            PlayerView(ctx).apply {
                player = exoPlayer
                useController = true
            }
        }
    )
}

@Composable
fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium, color = OnSurfaceVariant)
        Text(text = value, style = MaterialTheme.typography.bodyMedium, color = OnSurface)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrimShareSheet(
    clip: Clip,
    trimStartMs: Long,
    trimEndMs: Long,
    estimatedBytes: Long,
    onDismiss: () -> Unit,
    onTrimChange: (Long, Long) -> Unit,
    onUpload: () -> Unit
) {
    val durationMs = clip.durationMillis
    val maxShareableMs = ClipTrimManager.maxShareableDurationMs(clip.fileSizeBytes, durationMs)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = SurfaceContainer
    ) {
        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
            Text(
                "Trim before sharing",
                style = MaterialTheme.typography.titleMedium,
                color = OnSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "This clip is %.1fMB (limit 50MB). Select the portion to share.".format(
                    clip.fileSizeBytes / (1024f * 1024f)
                ),
                style = MaterialTheme.typography.bodySmall,
                color = OnSurfaceVariant
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Start slider
            val startFraction = (trimStartMs / durationMs.toFloat()).coerceIn(0f, 1f)
            Text(
                "Start: ${formatMs(trimStartMs)}",
                style = MaterialTheme.typography.labelMedium,
                color = OnSurfaceVariant
            )
            Slider(
                value = startFraction,
                onValueChange = { f ->
                    val newStart = (f * durationMs).toLong()
                    val newEnd = (newStart + maxShareableMs).coerceAtMost(durationMs)
                    onTrimChange(newStart, newEnd)
                },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Duration slider (how many seconds of the clip to include)
            val trimDuration = (trimEndMs - trimStartMs).coerceAtLeast(1000L)
            val durationFraction = (trimDuration / maxShareableMs.toFloat()).coerceIn(0f, 1f)
            Text(
                "Duration: ${formatMs(trimDuration)} / max ${formatMs(maxShareableMs)}",
                style = MaterialTheme.typography.labelMedium,
                color = OnSurfaceVariant
            )
            Slider(
                value = durationFraction,
                onValueChange = { f ->
                    val newDur = (f * maxShareableMs).toLong().coerceAtLeast(1000L)
                    val newEnd = (trimStartMs + newDur).coerceAtMost(durationMs)
                    onTrimChange(trimStartMs, newEnd)
                },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Estimate + range summary
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = DeepCharcoal,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "${formatMs(trimStartMs)} → ${formatMs(trimEndMs)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = OnSurfaceVariant
                    )
                    Text(
                        "~%.1fMB".format(estimatedBytes / (1024f * 1024f)),
                        style = MaterialTheme.typography.bodySmall,
                        color = if (estimatedBytes > 50L * 1024 * 1024) SafetyRed else ElectricBlue
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            val overLimit = estimatedBytes > 50L * 1024 * 1024
            Button(
                onClick = onUpload,
                modifier = Modifier.fillMaxWidth(),
                enabled = !overLimit
            ) {
                Icon(Icons.Filled.CloudUpload, null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Upload trimmed clip (${formatMs(trimEndMs - trimStartMs)}, ~%.1fMB)".format(
                    estimatedBytes / (1024f * 1024f)
                ))
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

private fun formatMs(ms: Long): String {
    val s = ms / 1000
    return if (s >= 60) "%d:%02d".format(s / 60, s % 60) else "${s}s"
}
