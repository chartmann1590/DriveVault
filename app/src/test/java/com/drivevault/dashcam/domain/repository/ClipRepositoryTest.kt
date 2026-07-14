package com.drivevault.dashcam.domain.repository

import com.drivevault.dashcam.data.local.dao.ClipDao
import com.drivevault.dashcam.data.local.dao.HeadingSampleDao
import com.drivevault.dashcam.data.local.dao.LocationSampleDao
import com.drivevault.dashcam.data.local.dao.SnapshotDao
import com.drivevault.dashcam.data.local.entity.ClipEntity
import com.drivevault.dashcam.domain.model.CameraMode
import com.drivevault.dashcam.domain.model.ImmichStatus
import com.drivevault.dashcam.domain.model.toDomain
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.*

class ClipRepositoryTest {

    private val clipDao = mock<ClipDao>()
    private val locationDao = mock<LocationSampleDao>()
    private val headingDao = mock<HeadingSampleDao>()
    private val snapshotDao = mock<SnapshotDao>()
    private lateinit var repo: ClipRepository

    private val sampleEntity = ClipEntity(
        id = 1,
        fileUri = "/data/clips/test.mp4",
        startTimeMillis = 1000,
        endTimeMillis = 61000,
        durationMillis = 60000,
        cameraMode = "BACK",
        fileSizeBytes = 5_000_000,
        startLatitude = 40.7128,
        startLongitude = -74.006,
        endLatitude = 40.7138,
        endLongitude = -74.007,
        averageSpeedMps = 15.0,
        maxSpeedMps = 25.0,
        startHeadingDegrees = 90f,
        endHeadingDegrees = 180f,
        audioEnabled = true,
        locked = false,
        immichStatus = "NOT_CONFIGURED"
    )

    @Before
    fun setup() {
        repo = ClipRepository(clipDao, locationDao, headingDao, snapshotDao)
    }

    @Test
    fun observeAllClips_mapsEntitiesToDomain() = runTest {
        whenever(clipDao.observeAll()).thenReturn(flowOf(listOf(sampleEntity)))

        var result: List<com.drivevault.dashcam.domain.model.Clip>? = null
        repo.observeAllClips().collect { result = it }

        assertNotNull(result)
        assertEquals(1, result!!.size)
        assertEquals(1L, result!![0].id)
        assertEquals(CameraMode.BACK, result!![0].cameraMode)
        assertEquals(60000L, result!![0].durationMillis)
        assertEquals(15.0, result!![0].averageSpeedMps, 0.001)
    }

    @Test
    fun observeClipById_returnsNullWhenNotFound() = runTest {
        whenever(clipDao.observeById(999)).thenReturn(flowOf(null))

        var result: com.drivevault.dashcam.domain.model.Clip? = null
        repo.observeClipById(999).collect { result = it }

        assertNull(result)
    }

    @Test
    fun observeClipById_returnsDomainClip() = runTest {
        whenever(clipDao.observeById(1)).thenReturn(flowOf(sampleEntity))

        var result: com.drivevault.dashcam.domain.model.Clip? = null
        repo.observeClipById(1).collect { result = it }

        assertNotNull(result)
        assertEquals(1L, result!!.id)
    }

    @Test
    fun observeLockedClips_filtersCorrectly() = runTest {
        whenever(clipDao.observeLocked()).thenReturn(flowOf(emptyList()))
        var result: List<com.drivevault.dashcam.domain.model.Clip>? = null
        repo.observeLockedClips().collect { result = it }
        assertTrue(result!!.isEmpty())
    }

    @Test
    fun insertClip_delegatesToDao() = runTest {
        val clip = sampleEntity.toDomain()
        whenever(clipDao.insert(any())).thenReturn(1)

        val id = repo.insertClip(clip)

        assertEquals(1, id)
        verify(clipDao).insert(any())
    }

    @Test
    fun deleteClip_deletesEntityAndSamples() = runTest {
        val clip = sampleEntity.toDomain()
        repo.deleteClip(clip)

        verify(clipDao).delete(any())
        verify(locationDao).deleteByClipId(clip.id)
        verify(headingDao).deleteByClipId(clip.id)
    }

    @Test
    fun setClipLocked_delegatesToDao() = runTest {
        repo.setClipLocked(1, true)
        verify(clipDao).setLocked(1, true)
    }

    @Test
    fun getClipCount_delegatesToDao() = runTest {
        whenever(clipDao.getClipCount()).thenReturn(42)
        assertEquals(42, repo.getClipCount())
    }

    @Test
    fun getTotalStorageUsed_returnsZeroWhenNull() = runTest {
        whenever(clipDao.getTotalStorageUsed()).thenReturn(null)
        assertEquals(0L, repo.getTotalStorageUsed())
    }

    @Test
    fun updateImmichStatus_delegatesWithCorrectParams() = runTest {
        repo.updateImmichStatus(1, "UPLOADED", "asset-123")
        verify(clipDao).updateImmichStatus(eq(1L), eq("UPLOADED"), eq("asset-123"), any())
    }

    @Test
    fun markPendingForUpload_updatesBatch() = runTest {
        repo.markPendingForUpload(listOf(1, 2, 3))
        verify(clipDao).updateImmichStatusBatch(listOf(1L, 2L, 3L), "PENDING")
    }

    @Test
    fun getFailedUploads_returnsDomainClips() = runTest {
        whenever(clipDao.getFailedUploads()).thenReturn(listOf(sampleEntity))
        val clips = repo.getFailedUploads()
        assertEquals(1, clips.size)
        assertEquals(ImmichStatus.NOT_CONFIGURED, clips[0].immichStatus)
    }

    @Test
    fun observeTotalStorageUsed_handlesNullGracefully() = runTest {
        whenever(clipDao.observeTotalStorageUsed()).thenReturn(flowOf(null))
        var result: Long? = null
        repo.observeTotalStorageUsed().collect { result = it }
        assertEquals(0L, result)
    }

    @Test
    fun deleteClipsByIds_deletesSamplesToo() = runTest {
        repo.deleteClipsByIds(listOf(1, 2))
        verify(clipDao).deleteByIds(listOf(1L, 2L))
        verify(locationDao).deleteByClipIds(listOf(1L, 2L))
        verify(headingDao).deleteByClipIds(listOf(1L, 2L))
    }
}
