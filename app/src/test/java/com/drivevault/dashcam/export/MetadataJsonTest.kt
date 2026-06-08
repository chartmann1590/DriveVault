package com.drivevault.dashcam.`export`

import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MetadataJsonTest {

    private val gson = Gson()

    @Test
    fun metadata_hasAppName() {
        val metadata = buildTestMetadata()
        assertEquals("DriveVault Dashcam", metadata["appName"])
    }

    @Test
    fun metadata_hasClipId() {
        val metadata = buildTestMetadata()
        assertTrue(metadata.containsKey("clipId"))
    }

    @Test
    fun metadata_hasDuration() {
        val metadata = buildTestMetadata()
        assertTrue(metadata.containsKey("durationMillis"))
    }

    @Test
    fun metadata_hasCameraMode() {
        val metadata = buildTestMetadata()
        assertEquals("BACK", metadata["cameraMode"])
    }

    @Test
    fun metadata_redacted_doesNotHaveLocation() {
        val metadata = buildTestMetadata(redacted = true)
        assertFalse(metadata.containsKey("locationSamples"))
        assertFalse(metadata.containsKey("startLocation"))
        assertEquals("LOCATION_REDACTED", metadata["privacyRedactionStatus"])
    }

    @Test
    fun metadata_notRedacted_hasLocation() {
        val metadata = buildTestMetadata(redacted = false)
        assertEquals("FULL", metadata["privacyRedactionStatus"])
        assertTrue(metadata.containsKey("locationSamples"))
        assertTrue(metadata.containsKey("startLocation"))
    }

    @Test
    fun metadata_serializesToJson() {
        val metadata = buildTestMetadata()
        val json = gson.toJson(metadata)
        assertTrue(json.contains("\"appName\""))
        assertTrue(json.contains("DriveVault Dashcam"))
    }

    private fun buildTestMetadata(redacted: Boolean = false): Map<String, Any?> {
        val metadata = mutableMapOf<String, Any?>(
            "appName" to "DriveVault Dashcam",
            "clipId" to 1L,
            "recordingStart" to 1000000L,
            "recordingEnd" to 1060000L,
            "durationMillis" to 60000L,
            "cameraMode" to "BACK",
            "averageSpeedMps" to 15.0,
            "maxSpeedMps" to 25.0,
            "privacyRedactionStatus" to if (redacted) "LOCATION_REDACTED" else "FULL"
        )

        if (!redacted) {
            metadata["startLocation"] = mapOf("latitude" to 40.7128, "longitude" to -74.006)
            metadata["endLocation"] = mapOf("latitude" to 40.7138, "longitude" to -74.007)
            metadata["locationSamples"] = emptyList<Map<String, Any?>>()
            metadata["headingSamples"] = emptyList<Map<String, Any?>>()
        }

        return metadata
    }
}
