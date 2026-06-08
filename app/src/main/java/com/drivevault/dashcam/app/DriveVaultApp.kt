package com.drivevault.dashcam.app

import android.app.Application
import com.drivevault.dashcam.data.local.DriveVaultDatabase
import com.drivevault.dashcam.data.repository.SettingsRepository
import com.drivevault.dashcam.firebase.DriveVaultFirebase
import com.drivevault.dashcam.firebase.FirebaseUserSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class DriveVaultApp : Application() {

    val database by lazy { DriveVaultDatabase.getInstance(this) }
    val settings by lazy { SettingsRepository(this) }
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        instance = this
        DriveVaultFirebase.initialize(this)
        observeFirebaseSettings()
    }

    private fun observeFirebaseSettings() {
        appScope.launch {
            val firebaseFlows = listOf(
                settings.firebaseCrashlyticsEnabled,
                settings.firebaseAnalyticsEnabled,
                settings.firebasePerformanceEnabled,
                settings.firebaseRemoteConfigEnabled,
                settings.firebaseMessagingEnabled,
                settings.firebaseFirestoreEnabled,
                settings.firebaseStorageEnabled,
                settings.firebaseAllowClipUpload,
                settings.firebaseAllowLocationUpload
            )
            combine(firebaseFlows) { values ->
                FirebaseUserSettings(
                    crashlyticsEnabled = values[0],
                    analyticsEnabled = values[1],
                    performanceEnabled = values[2],
                    remoteConfigEnabled = values[3],
                    messagingEnabled = values[4],
                    firestoreEnabled = values[5],
                    storageEnabled = values[6],
                    clipUploadAllowed = values[7],
                    locationUploadAllowed = values[8]
                )
            }.collect { firebaseSettings ->
                DriveVaultFirebase.applySettings(firebaseSettings)
            }
        }
    }

    companion object {
        lateinit var instance: DriveVaultApp
            private set
    }
}
