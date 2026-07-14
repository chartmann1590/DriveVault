package com.drivevault.dashcam.permissions

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

@SuppressLint("InlinedApi")
object PermissionManager {

    val REQUIRED_PERMISSIONS: List<String> = buildList {
        add(Manifest.permission.CAMERA)
        add(Manifest.permission.RECORD_AUDIO)
        add(Manifest.permission.ACCESS_FINE_LOCATION)
        add(Manifest.permission.ACCESS_COARSE_LOCATION)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            add(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    val BACKGROUND_LOCATION_PERMISSION: String = Manifest.permission.ACCESS_BACKGROUND_LOCATION

    val RECORDING_START_PERMISSIONS: List<String> = listOf(Manifest.permission.CAMERA)

    val OPTIONAL_PERMISSIONS: List<String> = buildList {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            add(Manifest.permission.READ_MEDIA_VIDEO)
        } else {
            add(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }

    val REQUESTABLE_PERMISSIONS: List<String> = REQUIRED_PERMISSIONS + OPTIONAL_PERMISSIONS

    fun isBackgroundLocationGranted(context: Context): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.Q ||
            isPermissionGranted(context, BACKGROUND_LOCATION_PERMISSION)
    }

    fun shouldShowBackgroundLocationRationale(activity: Activity): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
            activity.shouldShowRequestPermissionRationale(BACKGROUND_LOCATION_PERMISSION)
    }

    fun isPermissionGranted(context: Context, permission: String): Boolean {
        return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    }

    fun areAllRequiredGranted(context: Context): Boolean {
        return REQUIRED_PERMISSIONS.all { isPermissionGranted(context, it) }
    }

    fun getMissingPermissions(context: Context): List<String> {
        return REQUIRED_PERMISSIONS.filter { !isPermissionGranted(context, it) }
    }

    fun getMissingRecordingStartPermissions(context: Context): List<String> {
        return RECORDING_START_PERMISSIONS.filter { !isPermissionGranted(context, it) }
    }

    fun getMissingRequestablePermissions(context: Context): List<String> {
        return REQUESTABLE_PERMISSIONS.filter { !isPermissionGranted(context, it) }
    }

    fun getPermissionDescription(permission: String): String = when (permission) {
        Manifest.permission.CAMERA -> "Required for dashcam video recording"
        Manifest.permission.RECORD_AUDIO -> "Required for audio recording in clips"
        Manifest.permission.ACCESS_FINE_LOCATION -> "Required for GPS overlay with speed and coordinates"
        Manifest.permission.ACCESS_COARSE_LOCATION -> "Required for approximate location"
        Manifest.permission.ACCESS_BACKGROUND_LOCATION -> "Required to keep GPS recording when the app is in the background"
        Manifest.permission.POST_NOTIFICATIONS -> "Required for recording service notification"
        Manifest.permission.READ_MEDIA_VIDEO -> "Required for saving clips to gallery"
        Manifest.permission.READ_EXTERNAL_STORAGE -> "Required for accessing saved clips"
        else -> "Required for full app functionality"
    }

    fun getPermissionIcon(permission: String): String = when (permission) {
        Manifest.permission.CAMERA -> "camera"
        Manifest.permission.RECORD_AUDIO -> "mic"
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.ACCESS_BACKGROUND_LOCATION -> "location"
        Manifest.permission.POST_NOTIFICATIONS -> "notification"
        else -> "storage"
    }
}
