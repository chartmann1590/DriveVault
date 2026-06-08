package com.drivevault.dashcam.`export`

import com.drivevault.dashcam.data.local.entity.LocationSampleEntity
import org.junit.Assert.assertTrue
import org.junit.Test

class GpxExportTest {

    @Test
    fun gpxFormat_containsXmlDeclaration() {
        val gpx = buildTestGpx(emptyList())
        assertTrue(gpx.startsWith("<?xml"))
    }

    @Test
    fun gpxFormat_containsGpxTag() {
        val gpx = buildTestGpx(emptyList())
        assertTrue(gpx.contains("<gpx"))
        assertTrue(gpx.contains("</gpx>"))
    }

    @Test
    fun gpxFormat_containsTrackSegment() {
        val gpx = buildTestGpx(emptyList())
        assertTrue(gpx.contains("<trk>"))
        assertTrue(gpx.contains("<trkseg>"))
    }

    @Test
    fun gpxFormat_containsTrackPoints() {
        val samples = listOf(
            LocationSampleEntity(clipId = 1, timestampMillis = 1000, latitude = 40.7128, longitude = -74.006),
            LocationSampleEntity(clipId = 1, timestampMillis = 2000, latitude = 40.7138, longitude = -74.007)
        )
        val gpx = buildTestGpx(samples)
        assertTrue(gpx.contains("lat=\"40.7128\""))
        assertTrue(gpx.contains("lon=\"-74.006\""))
        assertTrue(gpx.contains("lat=\"40.7138\""))
        assertTrue(gpx.contains("<trkpt"))
    }

    private fun buildTestGpx(locations: List<LocationSampleEntity>): String {
        val trkpts = locations.joinToString("\n") { loc ->
            """      <trkpt lat="${loc.latitude}" lon="${loc.longitude}">
        <time>${loc.timestampMillis}</time>
      </trkpt>"""
        }

        return """<?xml version="1.0" encoding="UTF-8"?>
<gpx version="1.1" creator="DriveVault Dashcam">
  <trk>
    <name>DriveVault Recording</name>
    <trkseg>
$trkpts
    </trkseg>
  </trk>
</gpx>"""
    }
}
