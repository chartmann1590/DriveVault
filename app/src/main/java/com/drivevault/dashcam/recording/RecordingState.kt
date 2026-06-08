package com.drivevault.dashcam.recording

data class RecordingState(
    val isRecording: Boolean = false,
    val currentClipStartTime: Long = 0L,
    val currentClipElapsed: Long = 0L,
    val clipDuration: Long = 60_000L,
    val cameraMode: String = "BACK",
    val audioEnabled: Boolean = true,
    val overlayEnabled: Boolean = false,
    val miniMapEnabled: Boolean = false,
    val locked: Boolean = false,
    val storageRemaining: Long = 0L,
    val error: String? = null
) {
    val progressFraction: Float get() = if (clipDuration > 0) (currentClipElapsed.toFloat() / clipDuration.toFloat()).coerceIn(0f, 1f) else 0f
    val elapsedSeconds: Int get() = (currentClipElapsed / 1000).toInt()
}
