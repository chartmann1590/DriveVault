package com.drivevault.dashcam.domain.model

enum class CameraMode(val displayName: String) {
    BACK("Back"),
    FRONT("Front"),
    DUAL("Dual")
}

enum class VideoQuality(val displayName: String, val width: Int, val height: Int) {
    Q720("720p", 1280, 720),
    Q1080("1080p", 1920, 1080),
    Q4K("4K", 3840, 2160)
}

enum class FpsOption(val value: Int) {
    FPS_30(30),
    FPS_60(60)
}

enum class BitratePreset(val displayName: String) {
    LOW("Low"),
    BALANCED("Balanced"),
    HIGH("High")
}

enum class SpeedUnit(val displayName: String) {
    MPH("MPH"),
    KPH("KPH")
}

enum class ImmichStatus {
    NOT_CONFIGURED,
    PENDING,
    UPLOADING,
    UPLOADED,
    FAILED
}

enum class ImmichSyncMode(val displayName: String) {
    MANUAL("Manual only"),
    WIFI_ONLY("Wi-Fi only"),
    WIFI_CHARGING("Wi-Fi + Charging"),
    ALWAYS("Always")
}

enum class PipSize(val displayName: String) {
    SMALL("Small"),
    MEDIUM("Medium"),
    LARGE("Large")
}

enum class OverlayPosition(val displayName: String) {
    TOP_LEFT("Top Left"),
    TOP_RIGHT("Top Right"),
    BOTTOM_LEFT("Bottom Left"),
    BOTTOM_RIGHT("Bottom Right")
}

enum class CompassDirection(val abbreviation: String, val minDegrees: Float, val maxDegrees: Float) {
    N("N", 337.5f, 360f),
    NE("NE", 22.5f, 67.5f),
    E("E", 67.5f, 112.5f),
    SE("SE", 112.5f, 157.5f),
    S("S", 157.5f, 202.5f),
    SW("SW", 202.5f, 247.5f),
    W("W", 247.5f, 292.5f),
    NW("NW", 292.5f, 337.5f);

    companion object {
        fun fromDegrees(degrees: Float): CompassDirection {
            val normalized = ((degrees % 360) + 360) % 360
            return when {
                normalized >= 337.5f || normalized < 22.5f -> N
                normalized < 67.5f -> NE
                normalized < 112.5f -> E
                normalized < 157.5f -> SE
                normalized < 202.5f -> S
                normalized < 247.5f -> SW
                normalized < 292.5f -> W
                else -> NW
            }
        }
    }
}
