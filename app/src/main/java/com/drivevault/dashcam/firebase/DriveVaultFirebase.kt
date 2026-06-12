package com.drivevault.dashcam.firebase

import android.content.Context
import android.os.Bundle
import com.drivevault.dashcam.BuildConfig
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.perf.FirebasePerformance
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings

data class FirebaseUserSettings(
    val crashlyticsEnabled: Boolean = false,
    val analyticsEnabled: Boolean = false,
    val performanceEnabled: Boolean = false,
    val remoteConfigEnabled: Boolean = false,
    val messagingEnabled: Boolean = false,
    val firestoreEnabled: Boolean = false,
    val locationUploadAllowed: Boolean = false
)

object DriveVaultFirebase {
    private const val KEY_PRIVACY_MODE = "privacy_mode"
    private var appContext: Context? = null
    @Volatile private var currentSettings = FirebaseUserSettings()

    fun initialize(context: Context) {
        runCatching {
            appContext = context.applicationContext
            if (FirebaseApp.getApps(context).isEmpty()) {
                val options = FirebaseOptions.Builder()
                    .setApplicationId(BuildConfig.FIREBASE_APP_ID)
                    .setApiKey(BuildConfig.FIREBASE_API_KEY)
                    .setProjectId(BuildConfig.FIREBASE_PROJECT_ID)
                    .setGcmSenderId(BuildConfig.FIREBASE_PROJECT_NUMBER)
                    .build()
                FirebaseApp.initializeApp(context, options)
            }

            FirebaseCrashlytics.getInstance().apply {
                setCrashlyticsCollectionEnabled(false)
                setCustomKey("package_name", context.packageName)
                setCustomKey("firebase_project", BuildConfig.FIREBASE_PROJECT_ID)
                setCustomKey(KEY_PRIVACY_MODE, "no_clip_or_gps_upload")
            }

            FirebaseAnalytics.getInstance(context).apply {
                setAnalyticsCollectionEnabled(false)
                setDefaultEventParameters(
                    Bundle().apply {
                        putString("app_mode", "dashcam")
                        putString(KEY_PRIVACY_MODE, "local_media")
                    }
                )
            }

            FirebasePerformance.getInstance().isPerformanceCollectionEnabled = false

            FirebaseRemoteConfig.getInstance().apply {
                setConfigSettingsAsync(
                    FirebaseRemoteConfigSettings.Builder()
                        .setMinimumFetchIntervalInSeconds(if (BuildConfig.DEBUG) 60 else 3600)
                        .build()
                )
                setDefaultsAsync(
                    mapOf(
                        "dual_recording_enabled" to true,
                        "firebase_clip_upload_enabled" to false,
                        "firebase_location_upload_enabled" to false,
                        "recording_segment_seconds" to 60L
                    )
                )
            }
        }.onFailure { error ->
            android.util.Log.w("DriveVaultFirebase", "Firebase initialization failed", error)
        }
    }

    fun applySettings(settings: FirebaseUserSettings) {
        currentSettings = settings.copy(
            locationUploadAllowed = settings.firestoreEnabled && settings.locationUploadAllowed
        )
        val context = appContext ?: return

        runCatching {
            FirebaseCrashlytics.getInstance().apply {
                setCrashlyticsCollectionEnabled(currentSettings.crashlyticsEnabled)
                setCustomKey("firebase_analytics_enabled", currentSettings.analyticsEnabled)
                setCustomKey("firebase_performance_enabled", currentSettings.performanceEnabled)
                setCustomKey("firebase_remote_config_enabled", currentSettings.remoteConfigEnabled)
                setCustomKey("firebase_messaging_enabled", currentSettings.messagingEnabled)
                setCustomKey("firebase_firestore_enabled", currentSettings.firestoreEnabled)
                setCustomKey("firebase_location_upload_allowed", currentSettings.locationUploadAllowed)
                setCustomKey(
                    KEY_PRIVACY_MODE,
                    if (currentSettings.locationUploadAllowed) "user_enabled_cloud_features" else "local_only"
                )
            }

            FirebaseAnalytics.getInstance(context).setAnalyticsCollectionEnabled(currentSettings.analyticsEnabled)
            FirebasePerformance.getInstance().isPerformanceCollectionEnabled = currentSettings.performanceEnabled

            if (currentSettings.remoteConfigEnabled) {
                FirebaseRemoteConfig.getInstance().fetchAndActivate()
            }

            if (currentSettings.messagingEnabled) {
                FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
                    if (currentSettings.crashlyticsEnabled) {
                        FirebaseCrashlytics.getInstance().setCustomKey("fcm_token_available", token.isNotBlank())
                    }
                }
            }
        }.onFailure { error ->
            android.util.Log.w("DriveVaultFirebase", "Firebase settings apply failed", error)
        }
    }

    fun logRecordingStarted(cameraMode: String, dualStreamRequested: Boolean) {
        val context = appContext ?: return
        if (!currentSettings.analyticsEnabled) return
        runCatching {
            FirebaseAnalytics.getInstance(context).logEvent(
                "recording_started",
                Bundle().apply {
                    putString("camera_mode", cameraMode)
                    putBoolean("dual_stream_requested", dualStreamRequested)
                }
            )
            if (currentSettings.crashlyticsEnabled) {
                FirebaseCrashlytics.getInstance().setCustomKey("last_camera_mode", cameraMode)
            }
        }
    }

    fun logRecordingStopped(cameraMode: String, durationMillis: Long, hasSecondaryStream: Boolean) {
        val context = appContext ?: return
        if (!currentSettings.analyticsEnabled) return
        runCatching {
            FirebaseAnalytics.getInstance(context).logEvent(
                "recording_stopped",
                Bundle().apply {
                    putString("camera_mode", cameraMode)
                    putLong("duration_seconds", durationMillis / 1000L)
                    putBoolean("has_secondary_stream", hasSecondaryStream)
                }
            )
        }
    }

    fun recordNonFatal(message: String, throwable: Throwable? = null) {
        android.util.Log.w("DriveVaultFirebase", message, throwable)
        if (!currentSettings.crashlyticsEnabled) return
        runCatching {
            FirebaseCrashlytics.getInstance().log(message)
            if (throwable != null) {
                FirebaseCrashlytics.getInstance().recordException(throwable)
            }
        }
    }
}
