package com.drivevault.dashcam.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.drivevault.dashcam.domain.model.Clip
import com.drivevault.dashcam.ui.theme.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ClipCard(
    clip: Clip,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: () -> Unit = {},
    isSelected: Boolean = false,
    isSelectMode: Boolean = false
) {
    GlassCard(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .clickable(onClick = onClick),
        cornerRadius = 24.dp
    ) {
        Box {
            Column {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = SurfaceContainerHigh
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            val thumbFile = clip.thumbnailUri?.let { File(it) }
                            if (thumbFile != null && thumbFile.exists()) {
                                Icon(
                                    imageVector = Icons.Filled.Videocam,
                                    contentDescription = null,
                                    tint = OnSurfaceVariant,
                                    modifier = Modifier.size(48.dp)
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Filled.Videocam,
                                    contentDescription = null,
                                    tint = OnSurfaceVariant,
                                    modifier = Modifier.size(48.dp)
                                )
                            }
                        }
                    }
                    Row(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (clip.locked) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = AmberWarning.copy(alpha = 0.2f)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Lock,
                                    contentDescription = "Locked",
                                    tint = AmberWarning,
                                    modifier = Modifier.size(16.dp).padding(2.dp)
                                )
                            }
                        }
                        SyncStatusBadge(status = clip.immichStatus.name)
                    }
                    Surface(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp),
                        shape = RoundedCornerShape(4.dp),
                        color = SurfaceContainer.copy(alpha = 0.7f)
                    ) {
                        Text(
                            text = clip.cameraMode.displayName,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = OnSurfaceVariant
                        )
                    }
                }
                Column(modifier = Modifier.padding(12.dp)) {
                    val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.US)
                    val timeFormat = SimpleDateFormat("HH:mm", Locale.US)
                    val date = Date(clip.startTimeMillis)
                    Text(
                        text = "${dateFormat.format(date)} at ${timeFormat.format(date)}",
                        style = MaterialTheme.typography.titleSmall,
                        color = OnSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "${clip.durationMillis / 1000}s",
                            style = MaterialTheme.typography.bodySmall,
                            color = OnSurfaceVariant
                        )
                        if (clip.maxSpeedMps > 0) {
                            Text(
                                text = "${(clip.maxSpeedMps * 2.23694).toInt()} mph",
                                style = MaterialTheme.typography.bodySmall,
                                color = OnSurfaceVariant
                            )
                        }
                    }
                }
            }
            if (isSelectMode) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = null,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp),
                    colors = CheckboxDefaults.colors(checkedColor = ElectricBlue)
                )
            }
        }
    }
}

@Composable
fun PermissionCard(
    permissionName: String,
    description: String,
    isGranted: Boolean,
    onRequest: () -> Unit,
    modifier: Modifier = Modifier
) {
    GlassCard(modifier = modifier.fillMaxWidth(), cornerRadius = 16.dp) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = permissionName,
                    style = MaterialTheme.typography.titleSmall,
                    color = OnSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = OnSurfaceVariant
                )
            }
            if (isGranted) {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = "Granted",
                    tint = SuccessGreen,
                    modifier = Modifier.size(24.dp)
                )
            } else {
                FilledTonalButton(onClick = onRequest) {
                    Text("Grant")
                }
            }
        }
    }
}

@Composable
fun ImmichConnectionCard(
    serverUrl: String,
    isConnected: Boolean,
    errorMessage: String?,
    onTestConnection: () -> Unit,
    modifier: Modifier = Modifier
) {
    GlassCard(modifier = modifier.fillMaxWidth(), cornerRadius = 16.dp) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.CloudDone,
                    contentDescription = null,
                    tint = if (isConnected) SuccessGreen else OnSurfaceVariant
                )
                Text(
                    text = if (isConnected) "Connected" else "Not connected",
                    style = MaterialTheme.typography.titleSmall,
                    color = if (isConnected) SuccessGreen else OnSurfaceVariant
                )
            }
            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = errorMessage,
                    style = MaterialTheme.typography.bodySmall,
                    color = SafetyRed
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(onClick = onTestConnection) {
                Text("Test Connection")
            }
        }
    }
}
