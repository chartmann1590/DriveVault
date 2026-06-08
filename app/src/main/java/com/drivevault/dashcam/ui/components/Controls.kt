package com.drivevault.dashcam.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.drivevault.dashcam.ui.theme.*

@Composable
fun RecordButton(
    isRecording: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    FilledIconButton(
        onClick = onClick,
        modifier = modifier
            .size(88.dp)
            .semantics { contentDescription = if (isRecording) "Stop recording" else "Start recording" },
        shape = CircleShape,
        colors = IconButtonDefaults.filledIconButtonColors(
            containerColor = if (isRecording) OnSurface.copy(alpha = 0.3f) else SafetyRed,
            contentColor = if (isRecording) SafetyRed else SafetyRedContainer
        )
    ) {
        if (isRecording) {
            Surface(
                modifier = Modifier.size(28.dp),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(4.dp),
                color = SafetyRed
            ) {}
        } else {
            Surface(
                modifier = Modifier.size(38.dp),
                shape = CircleShape,
                color = SafetyRedContainer
            ) {}
        }
    }
}

@Composable
fun LockClipButton(
    isLocked: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    FilledTonalIconButton(
        onClick = onClick,
        modifier = modifier.size(48.dp),
        shape = CircleShape,
        colors = IconButtonDefaults.filledTonalIconButtonColors(
            containerColor = if (isLocked) AmberWarning.copy(alpha = 0.2f) else SurfaceContainerHigh,
            contentColor = if (isLocked) AmberWarning else OnSurfaceVariant
        )
    ) {
        Icon(
            imageVector = if (isLocked) Icons.Filled.Lock else Icons.Filled.LockOpen,
            contentDescription = if (isLocked) "Unlock clip" else "Lock clip"
        )
    }
}

@Composable
fun SnapshotButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    FilledTonalIconButton(
        onClick = onClick,
        modifier = modifier.size(48.dp),
        shape = CircleShape,
        colors = IconButtonDefaults.filledTonalIconButtonColors(
            containerColor = SurfaceContainerHigh,
            contentColor = OnSurface
        )
    ) {
        Icon(imageVector = Icons.Filled.CameraAlt, contentDescription = "Snapshot")
    }
}

@Composable
fun AudioToggle(
    isEnabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    FilledTonalIconButton(
        onClick = onClick,
        modifier = modifier.size(48.dp),
        shape = CircleShape,
        colors = IconButtonDefaults.filledTonalIconButtonColors(
            containerColor = SurfaceContainerHigh,
            contentColor = if (isEnabled) ElectricBlue else OnSurfaceVariant.copy(alpha = 0.4f)
        )
    ) {
        Icon(
            imageVector = if (isEnabled) Icons.Filled.Mic else Icons.Filled.MicOff,
            contentDescription = if (isEnabled) "Mute audio" else "Unmute audio"
        )
    }
}

@Composable
fun OverlayToggle(
    isEnabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    FilledTonalIconButton(
        onClick = onClick,
        modifier = modifier.size(48.dp),
        shape = CircleShape,
        colors = IconButtonDefaults.filledTonalIconButtonColors(
            containerColor = SurfaceContainerHigh,
            contentColor = if (isEnabled) ElectricBlue else OnSurfaceVariant.copy(alpha = 0.4f)
        )
    ) {
        Icon(imageVector = Icons.Filled.Layers, contentDescription = "Toggle overlay")
    }
}

@Composable
fun MiniMapToggle(
    isEnabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    FilledTonalIconButton(
        onClick = onClick,
        modifier = modifier.size(48.dp),
        shape = CircleShape,
        colors = IconButtonDefaults.filledTonalIconButtonColors(
            containerColor = SurfaceContainerHigh,
            contentColor = if (isEnabled) ElectricBlue else OnSurfaceVariant.copy(alpha = 0.4f)
        )
    ) {
        Icon(imageVector = Icons.Filled.Map, contentDescription = "Toggle mini map")
    }
}

@Composable
fun CameraModeSelector(
    selectedMode: String,
    onModeSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    dualSupported: Boolean = true
) {
    Surface(
        modifier = modifier,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
        color = SurfaceContainerHigh.copy(alpha = 0.7f)
    ) {
        Row(
            modifier = Modifier.padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CameraModeChip("BACK", selectedMode == "BACK", { onModeSelected("BACK") })
            CameraModeChip("FRONT", selectedMode == "FRONT", { onModeSelected("FRONT") })
            if (dualSupported) {
                CameraModeChip("DUAL", selectedMode == "DUAL", { onModeSelected("DUAL") })
            }
        }
    }
}

@Composable
private fun CameraModeChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
        color = if (selected) ElectricBlue.copy(alpha = 0.2f) else Color.Transparent,
        contentColor = if (selected) ElectricBlue else OnSurfaceVariant
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelMedium
        )
    }
}

@Composable
fun SyncStatusBadge(
    status: String,
    modifier: Modifier = Modifier
) {
    val (color, text) = when (status) {
        "UPLOADED" -> SuccessGreen to "Synced"
        "UPLOADING" -> ElectricBlue to "Uploading"
        "PENDING" -> AmberWarning to "Pending"
        "FAILED" -> SafetyRed to "Failed"
        else -> OnSurfaceVariant.copy(alpha = 0.4f) to "Not synced"
    }
    Surface(
        modifier = modifier,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(4.dp),
        color = color.copy(alpha = 0.15f)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall,
            color = color
        )
    }
}

@Composable
fun StorageUsageCard(
    usedMb: Long,
    maxMb: Long,
    clipCount: Int,
    modifier: Modifier = Modifier
) {
    GlassPanel(modifier = modifier, cornerRadius = 12.dp) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = "Storage",
                style = MaterialTheme.typography.labelMedium,
                color = OnSurfaceVariant
            )
            Spacer(modifier = Modifier.size(4.dp))
            Text(
                text = "$usedMb MB / $maxMb MB",
                style = MaterialTheme.typography.bodyMedium,
                color = OnSurface
            )
            val fraction = if (maxMb > 0) (usedMb.toFloat() / maxMb.toFloat()).coerceIn(0f, 1f) else 0f
            LinearProgressIndicator(
                progress = fraction,
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                color = when {
                    fraction > 0.95f -> SafetyRed
                    fraction > 0.8f -> AmberWarning
                    else -> ElectricBlue
                },
                trackColor = SurfaceContainerHighest
            )
            Text(
                text = "$clipCount clips",
                style = MaterialTheme.typography.bodySmall,
                color = OnSurfaceVariant
            )
        }
    }
}
