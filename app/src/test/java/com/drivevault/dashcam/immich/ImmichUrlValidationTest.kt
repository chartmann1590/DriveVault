package com.drivevault.dashcam.immich

import org.junit.Assert.*
import org.junit.Test

class ImmichUrlValidationTest {

    @Test
    fun validHttpsUrl_passes() {
        assertTrue(ImmichClient.isValidServerUrl("https://immich.example.com"))
    }

    @Test
    fun validHttpUrl_passes() {
        assertTrue(ImmichClient.isValidServerUrl("http://192.168.1.100:2283"))
    }

    @Test
    fun validUrlWithPath_passes() {
        assertTrue(ImmichClient.isValidServerUrl("https://example.com/immich"))
    }

    @Test
    fun blankUrl_fails() {
        assertFalse(ImmichClient.isValidServerUrl(""))
    }

    @Test
    fun noProtocol_fails() {
        assertFalse(ImmichClient.isValidServerUrl("immich.example.com"))
    }

    @Test
    fun ftpProtocol_fails() {
        assertFalse(ImmichClient.isValidServerUrl("ftp://example.com"))
    }

    @Test
    fun invalidUrl_fails() {
        assertFalse(ImmichClient.isValidServerUrl("not a url"))
    }
}