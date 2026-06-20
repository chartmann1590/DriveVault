package com.drivevault.dashcam.firebase

import org.junit.Assert.*
import org.junit.Test

class ClipShareStateTest {

    @Test
    fun idle_isIdle() {
        assertTrue(ClipShareState.Idle is ClipShareState)
    }

    @Test
    fun uploading_hasCorrectProgress() {
        val state = ClipShareState.Uploading(50)
        assertEquals(50, state.progressPercent)
    }

    @Test
    fun uploading_progressZero() {
        val state = ClipShareState.Uploading(0)
        assertEquals(0, state.progressPercent)
    }

    @Test
    fun uploading_progressHundred() {
        val state = ClipShareState.Uploading(100)
        assertEquals(100, state.progressPercent)
    }

    @Test
    fun success_hasShareUrlAndId() {
        val state = ClipShareState.Success(
            shareUrl = "https://example.com/view?id=abc",
            shareId = "abc"
        )
        assertEquals("https://example.com/view?id=abc", state.shareUrl)
        assertEquals("abc", state.shareId)
    }

    @Test
    fun error_hasMessage() {
        val state = ClipShareState.Error("Network error")
        assertEquals("Network error", state.message)
    }

    @Test
    fun error_statesAreDistinct() {
        val e1 = ClipShareState.Error("msg1")
        val e2 = ClipShareState.Error("msg2")
        assertEquals("msg1", e1.message)
        assertEquals("msg2", e2.message)
        assertNotEquals(e1.message, e2.message)
    }
}
