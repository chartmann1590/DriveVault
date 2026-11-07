package com.drivevault.dashcam.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.location.Location
import android.os.Looper
import androidx.core.content.ContextCompat
import com.drivevault.dashcam.domain.model.LocationData
import com.google.android.gms.location.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

class LocationProvider(private val fusedClient: FusedLocationProviderClient, private val context: android.content.Context) {

    private val hasLocationPermission: Boolean
        get() = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

    @SuppressLint("MissingPermission")
    fun observeLocation(): Flow<LocationData?> = callbackFlow {
        if (!hasLocationPermission) {
            trySend(null)
            close()
            return@callbackFlow
        }

        // Amazon Appstore (Fire) build: no Google Play services there, so the fused provider
        // can't run; use the platform LocationManager (GPS when the device has one, else network).
        if (com.drivevault.dashcam.BuildConfig.STORE == "amazon") {
            val lm = context.getSystemService(android.content.Context.LOCATION_SERVICE) as android.location.LocationManager
            val providers = runCatching { lm.getProviders(true) }.getOrDefault(emptyList())
                .filter { it != android.location.LocationManager.PASSIVE_PROVIDER }
            val listener = android.location.LocationListener { loc -> trySend(platformData(loc)) }
            if (providers.isEmpty()) trySend(null)
            providers.forEach { p -> runCatching { lm.requestLocationUpdates(p, 500L, 0f, listener, Looper.getMainLooper()) } }
            awaitClose { runCatching { lm.removeUpdates(listener) } }
            return@callbackFlow
        }

        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 500L)
            .setMinUpdateIntervalMillis(200L)
            .setWaitForAccurateLocation(false)
            .build()

        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val loc = result.lastLocation ?: return
                trySend(
                    LocationData(
                        latitude = loc.latitude,
                        longitude = loc.longitude,
                        altitude = loc.altitude,
                        accuracy = loc.accuracy,
                        speedMps = if (loc.hasSpeed()) loc.speed else 0f,
                        bearing = if (loc.hasBearing()) loc.bearing else 0f,
                        hasBearing = loc.hasBearing(),
                        timestampMillis = loc.time
                    )
                )
            }
        }

        fusedClient.requestLocationUpdates(request, callback, Looper.getMainLooper())
            .addOnFailureListener { trySend(null) }

        awaitClose { fusedClient.removeLocationUpdates(callback) }
    }.distinctUntilChanged()

    @SuppressLint("MissingPermission")
    suspend fun getLastKnownLocation(): LocationData? {
        if (!hasLocationPermission) return null
        if (com.drivevault.dashcam.BuildConfig.STORE == "amazon") {
            val lm = context.getSystemService(android.content.Context.LOCATION_SERVICE) as android.location.LocationManager
            return runCatching {
                lm.getProviders(true).mapNotNull { p -> runCatching { lm.getLastKnownLocation(p) }.getOrNull() }
                    .maxByOrNull { it.time }?.let { platformData(it) }
            }.getOrNull()
        }
        return try {
            val loc = fusedClient.getLastLocation().await() ?: return null
            LocationData(
                latitude = loc.latitude,
                longitude = loc.longitude,
                altitude = loc.altitude,
                accuracy = loc.accuracy,
                speedMps = if (loc.hasSpeed()) loc.speed else 0f,
                bearing = if (loc.hasBearing()) loc.bearing else 0f,
                timestampMillis = loc.time
            )
        } catch (_: Exception) { null }
    }

    private fun platformData(loc: Location) = LocationData(
        latitude = loc.latitude,
        longitude = loc.longitude,
        altitude = loc.altitude,
        accuracy = loc.accuracy,
        speedMps = if (loc.hasSpeed()) loc.speed else 0f,
        bearing = if (loc.hasBearing()) loc.bearing else 0f,
        hasBearing = loc.hasBearing(),
        timestampMillis = loc.time
    )

    private suspend fun <T> com.google.android.gms.tasks.Task<T>.await(): T = kotlinx.coroutines.suspendCancellableCoroutine { cont ->
        addOnSuccessListener { cont.resumeWith(Result.success(it)) }
        addOnFailureListener { cont.resumeWith(Result.failure(it)) }
    }
}
