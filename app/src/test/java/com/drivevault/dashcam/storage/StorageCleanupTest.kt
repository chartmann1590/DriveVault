package com.drivevault.dashcam.storage

import org.junit.Assert.*
import org.junit.Test

class StorageInfoTest {

    @Test
    fun storageInfo_lowAt80Percent() {
        val info = StorageInfo(
            totalUsedBytes = 81L * 1024 * 1024,
            maxAllowedBytes = 100L * 1024 * 1024,
            clipCount = 10,
            lockedClipCount = 2,
            availableBytes = 19L * 1024 * 1024,
            usagePercent = 81f
        )
        assertTrue(info.isLow)
        assertFalse(info.isCritical)
    }

    @Test
    fun storageInfo_criticalAt95Percent() {
        val info = StorageInfo(
            totalUsedBytes = 96L * 1024 * 1024,
            maxAllowedBytes = 100L * 1024 * 1024,
            clipCount = 50,
            lockedClipCount = 5,
            availableBytes = 4L * 1024 * 1024,
            usagePercent = 96f
        )
        assertTrue(info.isCritical)
        assertTrue(info.isLow)
    }

    @Test
    fun storageInfo_normalBelow80() {
        val info = StorageInfo(
            totalUsedBytes = 50L * 1024 * 1024,
            maxAllowedBytes = 100L * 1024 * 1024,
            clipCount = 5,
            lockedClipCount = 0,
            availableBytes = 50L * 1024 * 1024,
            usagePercent = 50f
        )
        assertFalse(info.isLow)
        assertFalse(info.isCritical)
    }

    @Test
    fun storageInfo_usedMbCalculation() {
        val info = StorageInfo(
            totalUsedBytes = 50L * 1024 * 1024,
            maxAllowedBytes = 100L * 1024 * 1024,
            clipCount = 5,
            lockedClipCount = 0,
            availableBytes = 50L * 1024 * 1024,
            usagePercent = 50f
        )
        assertEquals(50L, info.usedMb)
        assertEquals(100L, info.maxMb)
    }
}
