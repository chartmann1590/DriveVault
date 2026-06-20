package com.drivevault.dashcam.storage

import com.drivevault.dashcam.data.local.dao.ClipDao
import com.drivevault.dashcam.data.local.dao.HeadingSampleDao
import com.drivevault.dashcam.data.local.dao.LocationSampleDao
import com.drivevault.dashcam.data.local.entity.ClipEntity
import com.drivevault.dashcam.data.repository.SettingsRepository
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
import org.mockito.kotlin.*

class StorageManagerTest {

    @Test
    fun storageInfo_isLowAt80Percent() {
        val info = StorageInfo(
            totalUsedBytes = 80L * 1024 * 1024,
            maxAllowedBytes = 100L * 1024 * 1024,
            clipCount = 10,
            lockedClipCount = 2,
            availableBytes = 20L * 1024 * 1024,
            usagePercent = 80f
        )
        assertFalse(info.isLow)
        assertFalse(info.isCritical)
    }

    @Test
    fun storageInfo_isLowAbove80Percent() {
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
    fun storageInfo_isCriticalAbove95Percent() {
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
    fun storageInfo_usedMb_correctCalculation() {
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

    @Test
    fun storageInfo_usagePercent_atZero() {
        val info = StorageInfo(
            totalUsedBytes = 0L,
            maxAllowedBytes = 100L * 1024 * 1024,
            clipCount = 0,
            lockedClipCount = 0,
            availableBytes = 100L * 1024 * 1024,
            usagePercent = 0f
        )
        assertEquals(0L, info.usedMb)
        assertFalse(info.isLow)
    }

    @Test
    fun storageInfo_usagePercent_at100() {
        val info = StorageInfo(
            totalUsedBytes = 100L * 1024 * 1024,
            maxAllowedBytes = 100L * 1024 * 1024,
            clipCount = 100,
            lockedClipCount = 10,
            availableBytes = 0L,
            usagePercent = 100f
        )
        assertTrue(info.isCritical)
        assertEquals(100L, info.usedMb)
    }
}
