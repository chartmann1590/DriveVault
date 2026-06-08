package com.drivevault.dashcam.sensors

import android.content.Context
import com.drivevault.dashcam.domain.model.*
import com.drivevault.dashcam.location.LocationProvider
import com.google.android.gms.location.FusedLocationProviderClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class TelemetryManager(
    context: Context,
    fusedLocationClient: FusedLocationProviderClient
) {
    private val locationProvider = LocationProvider(fusedLocationClient, context)
    private val headingProvider = HeadingProvider(context)

    private val _telemetryState = MutableStateFlow(TelemetryState())
    val telemetryState: StateFlow<TelemetryState> = _telemetryState.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.Default)

    fun start() {
        scope.launch {
            locationProvider.observeLocation().collect { loc ->
                _telemetryState.update { state ->
                    val heading = if (loc != null && loc.bearing > 0f) {
                        HeadingData(loc.bearing, HeadingSource.GPS_BEARING, loc.timestampMillis)
                    } else state.heading
                    state.copy(
                        location = loc,
                        gpsFixed = loc != null && loc.accuracy < 30f,
                        speedMps = loc?.speedMps?.takeIf { it > 0f },
                        heading = heading ?: state.heading
                    )
                }
            }
        }
        scope.launch {
            headingProvider.observeHeading().collect { heading ->
                _telemetryState.update { state ->
                    val currentSource = state.heading?.source
                    if (currentSource == HeadingSource.GPS_BEARING && state.speedMps != null && state.speedMps!! > 2f) {
                        state
                    } else {
                        state.copy(heading = heading ?: state.heading)
                    }
                }
            }
        }
    }

    fun stop() {
        _telemetryState.value = TelemetryState()
    }

    val isHeadingAvailable: Boolean get() = headingProvider.isAvailable
}
