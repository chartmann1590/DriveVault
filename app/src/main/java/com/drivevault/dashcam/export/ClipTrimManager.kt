package com.drivevault.dashcam.export

import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.nio.ByteBuffer

object ClipTrimManager {

    private const val SHARE_LIMIT_BYTES = 50L * 1024 * 1024

    /** Returns true if the file needs trimming/compression before it can be shared. */
    fun needsTrimForSharing(fileSizeBytes: Long) = fileSizeBytes > SHARE_LIMIT_BYTES

    /**
     * Estimates the size of a trimmed clip proportionally to the original.
     * Accurate enough for dashcam footage at roughly constant bitrate.
     */
    fun estimateSize(fileSizeBytes: Long, durationMs: Long, trimDurationMs: Long): Long {
        if (durationMs <= 0 || trimDurationMs >= durationMs) return fileSizeBytes
        return fileSizeBytes * trimDurationMs / durationMs
    }

    /**
     * Max trim duration (ms) such that the estimated output stays under [SHARE_LIMIT_BYTES].
     * Returns the full duration if the file is already under the limit.
     */
    fun maxShareableDurationMs(fileSizeBytes: Long, durationMs: Long): Long {
        if (fileSizeBytes <= SHARE_LIMIT_BYTES) return durationMs
        return SHARE_LIMIT_BYTES * durationMs / fileSizeBytes
    }

    /**
     * Extracts [startMs]..[endMs] from [inputPath] into a temp file using MediaMuxer.
     * No re-encoding: start point snaps to the nearest preceding keyframe.
     * Returns the trimmed [File] on success; caller is responsible for deleting it.
     */
    suspend fun trim(
        inputPath: String,
        cacheDir: File,
        startMs: Long,
        endMs: Long
    ): File = withContext(Dispatchers.IO) {
        val output = File(cacheDir, "share_trim_${System.currentTimeMillis()}.mp4")
        output.parentFile?.mkdirs()

        val extractor = MediaExtractor()
        val muxer = MediaMuxer(output.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)

        try {
            extractor.setDataSource(inputPath)

            val trackMap = mutableMapOf<Int, Int>()
            for (i in 0 until extractor.trackCount) {
                val fmt = extractor.getTrackFormat(i)
                val mime = fmt.getString(MediaFormat.KEY_MIME) ?: continue
                if (mime.startsWith("video/") || mime.startsWith("audio/")) {
                    trackMap[i] = muxer.addTrack(fmt)
                    extractor.selectTrack(i)
                }
            }

            if (trackMap.isEmpty()) throw IOException("No audio/video tracks in $inputPath")

            val startUs = startMs * 1000L
            val endUs = endMs * 1000L

            // Seek to preceding keyframe so we have a valid starting point
            extractor.seekTo(startUs, MediaExtractor.SEEK_TO_PREVIOUS_SYNC)
            val actualStartUs = extractor.sampleTime.coerceAtLeast(0L)

            muxer.start()

            val buf = ByteBuffer.allocate(1 * 1024 * 1024)
            val info = MediaCodec.BufferInfo()

            while (true) {
                val srcTrack = extractor.sampleTrackIndex
                if (srcTrack < 0) break
                val sampleUs = extractor.sampleTime
                if (sampleUs > endUs) break

                val dstTrack = trackMap[srcTrack]
                if (dstTrack == null) { extractor.advance(); continue }

                info.size = extractor.readSampleData(buf, 0)
                if (info.size < 0) break

                info.presentationTimeUs = (sampleUs - actualStartUs).coerceAtLeast(0L)
                info.flags = if (extractor.sampleFlags and MediaExtractor.SAMPLE_FLAG_SYNC != 0)
                    MediaCodec.BUFFER_FLAG_KEY_FRAME else 0

                muxer.writeSampleData(dstTrack, buf, info)
                extractor.advance()
            }

            muxer.stop()
            output
        } catch (e: Exception) {
            output.delete()
            throw IOException("Trim failed: ${e.message}", e)
        } finally {
            extractor.release()
            runCatching { muxer.release() }
        }
    }
}
