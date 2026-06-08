package com.drivevault.dashcam.domain.util

import org.junit.Assert.*
import org.junit.Test

class UnitConverterTest {

    @Test
    fun mpsToMph_zeroReturnsZero() {
        assertEquals(0f, UnitConverter.mpsToMph(0f), 0.01f)
    }

    @Test
    fun mpsToMph_convertsCorrectly() {
        assertEquals(22.369f, UnitConverter.mpsToMph(10f), 0.01f)
        assertEquals(44.739f, UnitConverter.mpsToMph(20f), 0.01f)
    }

    @Test
    fun mpsToKph_convertsCorrectly() {
        assertEquals(36f, UnitConverter.mpsToKph(10f), 0.01f)
        assertEquals(72f, UnitConverter.mpsToKph(20f), 0.01f)
    }

    @Test
    fun mpsToUnit_mphMode_returnsMph() {
        assertEquals(22.369f, UnitConverter.mpsToUnit(10f, "MPH"), 0.01f)
    }

    @Test
    fun mpsToUnit_kphMode_returnsKph() {
        assertEquals(36f, UnitConverter.mpsToUnit(10f, "KPH"), 0.01f)
    }

    @Test
    fun formatSpeed_nullSpeed_showsUnavailable() {
        assertEquals("-- MPH", UnitConverter.formatSpeed(null, "MPH"))
        assertEquals("-- KPH", UnitConverter.formatSpeed(null, "KPH"))
    }

    @Test
    fun formatSpeed_zeroSpeed_showsUnavailable() {
        assertEquals("-- MPH", UnitConverter.formatSpeed(0f, "MPH"))
        assertEquals("-- KPH", UnitConverter.formatSpeed(0f, "KPH"))
    }

    @Test
    fun formatSpeed_validSpeed_formatsCorrectly() {
        val result = UnitConverter.formatSpeed(10f, "MPH")
        assertTrue(result.contains("MPH"))
        assertFalse(result.contains("--"))
    }
}
