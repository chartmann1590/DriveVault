package com.drivevault.dashcam.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.drivevault.dashcam.ui.components.ImmichConnectionCard
import com.drivevault.dashcam.ui.components.SupportFeedbackSection
import com.drivevault.dashcam.ui.theme.*
import com.drivevault.dashcam.ui.viewmodel.FeedbackViewModel
import com.drivevault.dashcam.ui.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    feedbackViewModel: FeedbackViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", style = MaterialTheme.typography.headlineSmall, color = OnSurface) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Filled.ArrowBack, "Back", tint = OnSurface)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DeepCharcoal)
            )
        },
        containerColor = DeepCharcoal
    ) { padding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            SettingsSectionHeader("Recording")
            SettingsToggle("Audio Recording", uiState.audioEnabled) { viewModel.setAudioEnabled(it) }
            SettingsDropdown("Video Quality", uiState.videoQuality, listOf("720p", "1080p", "4K")) { viewModel.setVideoQuality(it) }
            SettingsDropdown("FPS", uiState.fps.toString(), listOf("30", "60")) { viewModel.setFps(it.toIntOrNull() ?: 30) }
            SettingsDropdown("Bitrate", uiState.bitratePreset, listOf("LOW", "BALANCED", "HIGH")) { viewModel.setBitratePreset(it) }
            SettingsToggle("Loop Recording", uiState.loopRecording) { viewModel.setLoopRecording(it) }
            SettingsToggle("Auto-Delete Oldest", uiState.autoDeleteOldest) { viewModel.setAutoDeleteOldest(it) }

            SettingsSectionHeader("Camera")
            SettingsDropdown("Default Camera", uiState.defaultCameraMode, listOf("BACK", "FRONT", "DUAL")) { viewModel.setDefaultCameraMode(it) }
            SettingsToggle("Stabilization", uiState.stabilization) { viewModel.setStabilization(it) }
            SettingsToggle("Mirror Front Camera", uiState.mirrorFrontCamera) { viewModel.setMirrorFrontCamera(it) }

            SettingsSectionHeader("Overlay")
            SettingsToggle("Show Speed", uiState.showSpeed) { viewModel.setShowSpeed(it) }
            SettingsDropdown("Speed Unit", uiState.speedUnit, listOf("MPH", "KPH")) { viewModel.setSpeedUnit(it) }
            SettingsToggle("Show GPS Coordinates", uiState.showGpsCoordinates) { viewModel.setShowGpsCoordinates(it) }
            SettingsToggle("Show Heading", uiState.showHeading) { viewModel.setShowHeading(it) }
            SettingsToggle("Show Timestamp", uiState.showTimestamp) { viewModel.setShowTimestamp(it) }
            SettingsToggle("Show Mini Map", uiState.showMiniMap) { viewModel.setShowMiniMap(it) }

            SettingsSectionHeader("Map")
            SettingsToggle("Route Trail", uiState.mapRouteTrail) { viewModel.setMapRouteTrail(it) }

            SettingsSectionHeader("Privacy")
            SettingsToggle("Blur Location on Share", uiState.blurLocationOnShare) { viewModel.setBlurLocationOnShare(it) }
            SettingsToggle("Hide Exact GPS", uiState.hideExactGps) { viewModel.setHideExactGps(it) }
            SettingsToggle("Confirm GPS Share", uiState.confirmGpsShare) { viewModel.setConfirmGpsShare(it) }
            SettingsToggle("Local Only Mode", uiState.localOnlyMode) { viewModel.setLocalOnlyMode(it) }

            SettingsSectionHeader("Firebase")
            Text(
                text = "Firebase is optional. Crash reports, analytics, messaging, cloud metadata, clip uploads, and GPS uploads stay off unless you enable them here.",
                style = MaterialTheme.typography.bodySmall,
                color = OnSurfaceVariant
            )
            SettingsToggle("Crash Reports", uiState.firebaseCrashlyticsEnabled) { viewModel.setFirebaseCrashlyticsEnabled(it) }
            SettingsToggle("Analytics", uiState.firebaseAnalyticsEnabled) { viewModel.setFirebaseAnalyticsEnabled(it) }
            SettingsToggle("Performance Monitoring", uiState.firebasePerformanceEnabled) { viewModel.setFirebasePerformanceEnabled(it) }
            SettingsToggle("Remote Config", uiState.firebaseRemoteConfigEnabled) { viewModel.setFirebaseRemoteConfigEnabled(it) }
            SettingsToggle("Cloud Messaging", uiState.firebaseMessagingEnabled) { viewModel.setFirebaseMessagingEnabled(it) }
            SettingsToggle("Cloud Metadata", uiState.firebaseFirestoreEnabled) { viewModel.setFirebaseFirestoreEnabled(it) }
            SettingsToggle("Firebase Storage", uiState.firebaseStorageEnabled) { viewModel.setFirebaseStorageEnabled(it) }
            SettingsToggle(
                label = "Allow Clip Uploads",
                checked = uiState.firebaseAllowClipUpload && uiState.firebaseStorageEnabled,
                enabled = uiState.firebaseStorageEnabled
            ) { viewModel.setFirebaseAllowClipUpload(it) }
            SettingsToggle(
                label = "Allow GPS Uploads",
                checked = uiState.firebaseAllowLocationUpload && (uiState.firebaseFirestoreEnabled || uiState.firebaseStorageEnabled),
                enabled = uiState.firebaseFirestoreEnabled || uiState.firebaseStorageEnabled
            ) { viewModel.setFirebaseAllowLocationUpload(it) }

            SettingsSectionHeader("Immich Sync")
            SettingsToggle("Enable Immich Sync", uiState.immichEnabled) { viewModel.setImmichEnabled(it) }

            if (uiState.immichEnabled) {
                OutlinedTextField(
                    value = uiState.immichServerUrl,
                    onValueChange = viewModel::setImmichServerUrl,
                    label = { Text("Server URL") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = OnSurface,
                        unfocusedTextColor = OnSurface,
                        focusedBorderColor = ElectricBlue,
                        unfocusedBorderColor = OutlineVariant
                    )
                )
                OutlinedTextField(
                    value = uiState.immichApiKey,
                    onValueChange = viewModel::setImmichApiKey,
                    label = { Text("API Key") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = OnSurface,
                        unfocusedTextColor = OnSurface,
                        focusedBorderColor = ElectricBlue,
                        unfocusedBorderColor = OutlineVariant
                    )
                )

                ImmichConnectionCard(
                    serverUrl = uiState.immichServerUrl,
                    isConnected = uiState.immichConnectionStatus?.startsWith("Connected") == true,
                    errorMessage = if (uiState.immichConnectionStatus?.startsWith("Error") == true ||
                        uiState.immichConnectionStatus?.startsWith("Auth") == true ||
                        uiState.immichConnectionStatus?.startsWith("Network") == true) uiState.immichConnectionStatus else null,
                    onTestConnection = viewModel::testConnection
                )

                if (uiState.isTestingConnection) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }

                if (uiState.immichConnectionStatus != null && !uiState.isTestingConnection) {
                    Text(
                        text = uiState.immichConnectionStatus!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (uiState.immichConnectionStatus!!.startsWith("Connected")) SuccessGreen else SafetyRed
                    )
                }

                SettingsDropdown("Sync Mode", uiState.immichSyncMode, listOf("MANUAL", "WIFI_ONLY", "WIFI_CHARGING", "ALWAYS")) { viewModel.setImmichSyncMode(it) }
                SettingsToggle("Upload Videos", uiState.immichUploadVideos) { viewModel.setImmichUploadVideos(it) }
                SettingsToggle("Upload Metadata", uiState.immichUploadMetadata) { viewModel.setImmichUploadMetadata(it) }
                SettingsToggle("Upload Locked Only", uiState.immichUploadLockedOnly) { viewModel.setImmichUploadLockedOnly(it) }
                SettingsToggle("Auto-Create Album", uiState.immichAutoAlbum) { viewModel.setImmichAutoAlbum(it) }
            }

            SettingsSectionHeader("Support & Feedback")
            SupportFeedbackSection(viewModel = feedbackViewModel)

            SettingsSectionHeader("About")
            Text("DriveVault Dashcam v1.0.0", style = MaterialTheme.typography.bodyMedium, color = OnSurfaceVariant)
            Text("A privacy-first dashcam application.", style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant)
            Text("SAFETY DISCLAIMER: This app is not a substitute for safe driving practices. Always obey local traffic laws.", style = MaterialTheme.typography.bodySmall, color = AmberWarning)

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = ElectricBlue,
        modifier = Modifier.padding(top = 8.dp)
    )
}

@Composable
fun SettingsToggle(
    label: String,
    checked: Boolean,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyLarge,
            color = if (enabled) OnSurface else OnSurfaceVariant
        )
        Switch(
            checked = checked,
            enabled = enabled,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(checkedTrackColor = ElectricBlue)
        )
    }
}

@Composable
fun SettingsDropdown(
    label: String,
    value: String,
    options: List<String>,
    onSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge, color = OnSurface)
        Box {
            TextButton(onClick = { expanded = true }) {
                Text(value, color = ElectricBlue)
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option) },
                        onClick = {
                            onSelected(option)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}
