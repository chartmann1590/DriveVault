package com.drivevault.dashcam.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.drivevault.dashcam.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportBottomSheet(
    onDismiss: () -> Unit,
    onExportVideo: () -> Unit,
    onExportMetadata: () -> Unit,
    onExportGpx: () -> Unit,
    onShare: () -> Unit,
    onSaveToGallery: () -> Unit,
    onUploadToImmich: () -> Unit,
    modifier: Modifier = Modifier,
    immichEnabled: Boolean = false
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(bottom = 32.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier.padding(top = 12.dp, bottom = 16.dp),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(4.dp),
                color = OnSurfaceVariant.copy(alpha = 0.3f)
            ) {
                Box(modifier = Modifier.width(40.dp).height(4.dp))
            }
        }

        Text(
            text = "Export & Share",
            style = MaterialTheme.typography.headlineSmall,
            color = OnSurface
        )

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            item {
                ExportOptionItem(Icons.Filled.Videocam, "Export Video", "Original MP4 file", onExportVideo)
            }
            item {
                ExportOptionItem(Icons.Filled.Description, "Export Metadata", "JSON with all clip data", onExportMetadata)
            }
            item {
                ExportOptionItem(Icons.Filled.Map, "Export GPX Route", "GPS track file", onExportGpx)
            }
            item {
                ExportOptionItem(Icons.Filled.SaveAlt, "Save to Gallery", "Save to device", onSaveToGallery)
            }
            item {
                ExportOptionItem(Icons.Filled.Share, "Share", "Share via apps", onShare)
            }
            if (immichEnabled) {
                item {
                    ExportOptionItem(Icons.Filled.CloudUpload, "Upload to Immich", "Sync to server", onUploadToImmich)
                }
            }
        }
    }
}

@Composable
private fun ExportOptionItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
        color = SurfaceContainer
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = OnSurface)
            Column {
                Text(text = title, style = MaterialTheme.typography.bodyLarge, color = OnSurface)
                Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant)
            }
        }
    }
}
