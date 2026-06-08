package com.drivevault.dashcam.domain.model

import org.junit.Assert.*
import org.junit.Test

class CompassDirectionTest {

    @Test
    fun fromDegrees_0_isNorth() {
        assertEquals(CompassDirection.N, CompassDirection.fromDegrees(0f))
    }

    @Test
    fun fromDegrees_360_isNorth() {
        assertEquals(CompassDirection.N, CompassDirection.fromDegrees(360f))
    }

    @Test
    fun fromDegrees_45_isNE() {
        assertEquals(CompassDirection.NE, CompassDirection.fromDegrees(45f))
    }

    @Test
    fun fromDegrees_90_isE() {
        assertEquals(CompassDirection.E, CompassDirection.fromDegrees(90f))
    }

    @Test
    fun fromDegrees_135_isSE() {
        assertEquals(CompassDirection.SE, CompassDirection.fromDegrees(135f))
    }

    @Test
    fun fromDegrees_180_isS() {
        assertEquals(CompassDirection.S, CompassDirection.fromDegrees(180f))
    }

    @Test
    fun fromDegrees_225_isSW() {
        assertEquals(CompassDirection.SW, CompassDirection.fromDegrees(225f))
    }

    @Test
    fun fromDegrees_270_isW() {
        assertEquals(CompassDirection.W, CompassDirection.fromDegrees(270f))
    }

    @Test
    fun fromDegrees_315_isNW() {
        assertEquals(CompassDirection.NW, CompassDirection.fromDegrees(315f))
    }

    @Test
    fun fromDegrees_negativeValue_wrapsCorrectly() {
        assertEquals(CompassDirection.NW, CompassDirection.fromDegrees(-45f))
        assertEquals(CompassDirection.W, CompassDirection.fromDegrees(-90f))
    }

    @Test
    fun fromDegrees_boundaryNNE() {
        assertEquals(CompassDirection.N, CompassDirection.fromDegrees(22.4f))
        assertEquals(CompassDirection.NE, CompassDirection.fromDegrees(22.5f))
    }

    @Test
    fun fromDegrees_350_isN() {
        assertEquals(CompassDirection.N, CompassDirection.fromDegrees(350f))
    }

    @Test
    fun allDirectionsHaveAbbreviations() {
        CompassDirection.entries.forEach { dir ->
            assertTrue(dir.abbreviation.isNotBlank())
        }
    }
}
