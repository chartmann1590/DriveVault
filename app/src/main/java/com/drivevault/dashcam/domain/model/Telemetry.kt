package com.drivevault.dashcam.domain.model

data class LocationData(
    val latitude: Double,
    val longitude: Double,
    val altitude: Double = 0.0,
    val accuracy: Float = 0f,
    val speedMps: Float = 0f,
    val bearing: Float = 0f,
    val hasBearing: Boolean = false,
    val timestampMillis: Long = System.currentTimeMillis()
)

data class HeadingData(
    val headingDegrees: Float,
    val source: HeadingSource,
    val timestampMillis: Long = System.currentTimeMillis()
)

enum class HeadingSource {
    GPS_BEARING,
    ROTATION_VECTOR,
    ACCELEROMETER_MAGNETOMETER
}

data class TelemetryState(
    val location: LocationData? = null,
    val heading: HeadingData? = null,
    val gpsFixed: Boolean = false,
    val speedMps: Float? = null
) {
    val displaySpeed: Float? get() = location?.speedMps?.takeIf { it > 0f }
    val displayHeading: Float? get() = heading?.headingDegrees
    val compassDirection: CompassDirection? get() = heading?.headingDegrees?.let { CompassDirection.fromDegrees(it) }
}
