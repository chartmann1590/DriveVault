package com.drivevault.dashcam.recording

import org.junit.Assert.*
import org.junit.Test

class ClipSegmentationTest {

    @Test
    fun recordingState_defaultClipDurationIs60Seconds() {
        val state = RecordingState()
        assertEquals(60_000L, state.clipDuration)
    }

    @Test
    fun recordingState_progressZeroAtStart() {
        val state = RecordingState(currentClipElapsed = 0L)
        assertEquals(0f, state.progressFraction, 0.01f)
    }

    @Test
    fun recordingState_progressHalfAt30Seconds() {
        val state = RecordingState(currentClipElapsed = 30_000L)
        assertEquals(0.5f, state.progressFraction, 0.01f)
    }

    @Test
    fun recordingState_progressFullAt60Seconds() {
        val state = RecordingState(currentClipElapsed = 60_000L)
        assertEquals(1.0f, state.progressFraction, 0.01f)
    }

    @Test
    fun recordingState_progressClampedAbove60() {
        val state = RecordingState(currentClipElapsed = 90_000L)
        assertEquals(1.0f, state.progressFraction, 0.01f)
    }

    @Test
    fun recordingState_elapsedSecondsCalculation() {
        val state = RecordingState(currentClipElapsed = 45_230L)
        assertEquals(45, state.elapsedSeconds)
    }

    @Test
    fun recordingState_notRecordingByDefault() {
        val state = RecordingState()
        assertFalse(state.isRecording)
    }

    @Test
    fun recordingState_notLockedByDefault() {
        val state = RecordingState()
        assertFalse(state.locked)
    }
}
