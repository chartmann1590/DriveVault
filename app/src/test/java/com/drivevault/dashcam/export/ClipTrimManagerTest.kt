package com.drivevault.dashcam.export

import org.junit.Assert.*
import org.junit.Test

class ClipTrimManagerTest {

    private val shareLimitBytes = 50L * 1024 * 1024

    @Test
    fun needsTrim_underLimit_returnsFalse() {
        assertFalse(ClipTrimManager.needsTrimForSharing(40L * 1024 * 1024))
    }

    @Test
    fun needsTrim_atLimit_returnsFalse() {
        assertFalse(ClipTrimManager.needsTrimForSharing(shareLimitBytes))
    }

    @Test
    fun needsTrim_aboveLimit_returnsTrue() {
        assertTrue(ClipTrimManager.needsTrimForSharing(51L * 1024 * 1024))
    }

    @Test
    fun needsTrim_veryLarge_returnsTrue() {
        assertTrue(ClipTrimManager.needsTrimForSharing(200L * 1024 * 1024))
    }

    @Test
    fun estimateSize_proportionalScaling() {
        val estimate = ClipTrimManager.estimateSize(
            fileSizeBytes = 100L * 1024 * 1024,
            durationMs = 60000L,
            trimDurationMs = 30000L
        )
        assertEquals(50L * 1024 * 1024, estimate)
    }

    @Test
    fun estimateSize_fullDuration_returnsOriginalSize() {
        val fileSize = 100L * 1024 * 1024
        val estimate = ClipTrimManager.estimateSize(
            fileSizeBytes = fileSize,
            durationMs = 60000L,
            trimDurationMs = 60000L
        )
        assertEquals(fileSize, estimate)
    }

    @Test
    fun estimateSize_zeroDuration_returnsZero() {
        val estimate = ClipTrimManager.estimateSize(
            fileSizeBytes = 100L * 1024 * 1024,
            durationMs = 60000L,
            trimDurationMs = 0L
        )
        assertEquals(0L, estimate)
    }

    @Test
    fun maxShareableDuration_underLimit_returnsFullDuration() {
        val result = ClipTrimManager.maxShareableDurationMs(
            fileSizeBytes = 40L * 1024 * 1024,
            durationMs = 60000L
        )
        assertEquals(60000L, result)
    }

    @Test
    fun maxShareableDuration_aboveLimit_calculatesCorrectly() {
        val result = ClipTrimManager.maxShareableDurationMs(
            fileSizeBytes = 100L * 1024 * 1024,
            durationMs = 60000L
        )
        assertEquals(30000L, result)
    }

    @Test
    fun maxShareableDuration_veryLarge_returnsShortClip() {
        val result = ClipTrimManager.maxShareableDurationMs(
            fileSizeBytes = 500L * 1024 * 1024,
            durationMs = 60000L
        )
        assertEquals(6000L, result)
    }

    @Test
    fun estimateSize_withDurationZero_avoidsDivisionByZero() {
        val estimate = ClipTrimManager.estimateSize(
            fileSizeBytes = 50L * 1024 * 1024,
            durationMs = 0L,
            trimDurationMs = 0L
        )
        // When durationMs <= 0, returns fileSizeBytes unchanged
        assertEquals(50L * 1024 * 1024, estimate)
    }
}
