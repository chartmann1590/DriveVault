package com.drivevault.dashcam.ui.screens

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import kotlinx.coroutines.delay
import android.Manifest
import android.os.Build
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.drivevault.dashcam.permissions.PermissionManager
import com.drivevault.dashcam.ui.components.PermissionCard
import com.drivevault.dashcam.ui.theme.*

@Composable
fun PermissionsScreen(
    onAllGranted: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var requestStarted by remember { mutableStateOf(false) }
    var missingPermissions by remember {
        mutableStateOf(PermissionManager.getMissingRequestablePermissions(context))
    }
    var backgroundLocationRequested by remember { mutableStateOf(false) }

    val launcher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        missingPermissions = PermissionManager.getMissingRequestablePermissions(context)
        if (PermissionManager.areAllRequiredGranted(context) &&
            PermissionManager.isBackgroundLocationGranted(context)
        ) {
            onAllGranted()
        }
    }

    val backgroundLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { _ ->
        backgroundLocationRequested = true
        if (PermissionManager.areAllRequiredGranted(context)) {
            onAllGranted()
        }
    }

    LaunchedEffect(Unit) {
        if (PermissionManager.areAllRequiredGranted(context) &&
            PermissionManager.isBackgroundLocationGranted(context)
        ) {
            onAllGranted()
        } else if (!requestStarted) {
            requestStarted = true
            delay(350)
            launcher.launch(PermissionManager.getMissingRequestablePermissions(context).toTypedArray())
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                missingPermissions = PermissionManager.getMissingRequestablePermissions(context)
                if (PermissionManager.areAllRequiredGranted(context) &&
                    PermissionManager.isBackgroundLocationGranted(context)
                ) {
                    onAllGranted()
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Permissions Required",
            style = MaterialTheme.typography.headlineMedium,
            color = OnSurface
        )
        Text(
            text = if (missingPermissions.isEmpty()) {
                "DriveVault has the permissions it needs to function as a dashcam."
            } else {
                "Missing: ${missingPermissions.joinToString { it.substringAfterLast('.') }}."
            },
            style = MaterialTheme.typography.bodyLarge,
            color = OnSurfaceVariant
        )

        PermissionManager.REQUESTABLE_PERMISSIONS.forEach { permission ->
            val isGranted = PermissionManager.isPermissionGranted(context, permission)
            PermissionCard(
                permissionName = permission.substringAfterLast("."),
                description = PermissionManager.getPermissionDescription(permission),
                isGranted = isGranted,
                onRequest = {
                    launcher.launch(arrayOf(permission))
                }
            )
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val bgGranted = PermissionManager.isBackgroundLocationGranted(context)
            PermissionCard(
                permissionName = Manifest.permission.ACCESS_BACKGROUND_LOCATION.substringAfterLast("."),
                description = PermissionManager.getPermissionDescription(Manifest.permission.ACCESS_BACKGROUND_LOCATION),
                isGranted = bgGranted,
                onRequest = {
                    backgroundLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                }
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = {
                val missing = PermissionManager.getMissingRequestablePermissions(context)
                if (missing.isEmpty() && PermissionManager.isBackgroundLocationGranted(context)) {
                    onAllGranted()
                } else if (missing.isNotEmpty()) {
                    launcher.launch(missing.toTypedArray())
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    backgroundLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                }
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = SafetyRed)
        ) {
            Text(
                if (PermissionManager.areAllRequiredGranted(context) &&
                    PermissionManager.isBackgroundLocationGranted(context)
                ) "Continue" else "Grant Permissions"
            )
        }

        OutlinedButton(
            onClick = {
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", context.packageName, null)
                }
                context.startActivity(intent)
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Open app settings")
        }
    }
}
