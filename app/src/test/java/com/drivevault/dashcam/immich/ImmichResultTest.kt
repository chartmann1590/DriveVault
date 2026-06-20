package com.drivevault.dashcam.immich

import org.junit.Assert.*
import org.junit.Test

class ImmichResultTest {

    @Test
    fun success_holdsData() {
        val result = ImmichResult.Success("hello")
        assertTrue(result is ImmichResult.Success)
        assertEquals("hello", result.data)
    }

    @Test
    fun error_holdsMessageAndCode() {
        val result = ImmichResult.Error("Not found", 404)
        assertTrue(result is ImmichResult.Error)
        assertEquals("Not found", result.message)
        assertEquals(404, result.code)
    }

    @Test
    fun error_defaultCodeIsZero() {
        val result = ImmichResult.Error("Something went wrong")
        assertEquals(0, result.code)
    }

    @Test
    fun networkError_holdsMessage() {
        val result = ImmichResult.NetworkError("Timeout")
        assertTrue(result is ImmichResult.NetworkError)
        assertEquals("Timeout", result.message)
    }

    @Test
    fun authError_holdsMessage() {
        val result = ImmichResult.AuthError("Invalid API key")
        assertTrue(result is ImmichResult.AuthError)
        assertEquals("Invalid API key", result.message)
    }

    @Test
    fun success_withNull_works() {
        val result = ImmichResult.Success(null)
        assertNull(result.data)
    }

    @Test
    fun success_withComplexType() {
        data class Foo(val id: String, val count: Int)
        val foo = Foo("abc", 5)
        val result = ImmichResult.Success(foo)
        assertEquals("abc", result.data.id)
        assertEquals(5, result.data.count)
    }
}
