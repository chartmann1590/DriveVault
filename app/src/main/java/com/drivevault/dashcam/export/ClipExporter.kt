package com.drivevault.dashcam.`export`

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.webkit.MimeTypeMap
import androidx.core.content.FileProvider
import com.drivevault.dashcam.data.local.DriveVaultDatabase
import com.drivevault.dashcam.data.local.entity.LocationSampleEntity
import com.drivevault.dashcam.data.local.entity.HeadingSampleEntity
import com.drivevault.dashcam.data.local.entity.ClipEntity
import com.drivevault.dashcam.domain.model.Clip
import com.drivevault.dashcam.domain.model.toDomain
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*

data class ExportOptions(
    val includeVideo: Boolean = true,
    val includeMetadata: Boolean = true,
    val includeGpx: Boolean = true,
    val redactLocation: Boolean = false,
    val bakeOverlay: Boolean = false
)

data class ClipExportResult(
    val videoFile: File? = null,
    val secondaryVideoFile: File? = null,
    val metadataFile: File? = null,
    val gpxFile: File? = null
)

class ClipExporter(private val context: Context) {

    private val db by lazy { DriveVaultDatabase.getInstance(context) }
    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()

    suspend fun exportClip(clipId: Long, options: ExportOptions): ClipExportResult = withContext(Dispatchers.IO) {
        val clip = db.clipDao().getById(clipId) ?: return@withContext ClipExportResult()
        val locations = db.locationSampleDao().getByClipId(clipId)
        val headings = db.headingSampleDao().getByClipId(clipId)

        val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val dateFormat = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US)
        val baseName = "DriveVault_${dateFormat.format(Date(clip.startTimeMillis))}"

        var videoFile: File? = null
        var secondaryVideoFile: File? = null
        var metadataFile: File? = null
        var gpxFile: File? = null

        if (options.includeVideo) {
            val source = File(clip.fileUri)
            if (source.exists()) {
                videoFile = File(exportDir, "$baseName.mp4")
                source.copyTo(videoFile, overwrite = true)
            }
            val secondarySource = clip.secondaryFileUri?.let { File(it) }
            if (secondarySource?.exists() == true) {
                secondaryVideoFile = File(exportDir, "${baseName}_front.mp4")
                secondarySource.copyTo(secondaryVideoFile, overwrite = true)
            }
        }

        if (options.includeMetadata) {
            metadataFile = File(exportDir, "$baseName.json")
            writeMetadataJson(clip, locations, headings, options.redactLocation, metadataFile)
        }

        if (options.includeGpx && locations.isNotEmpty()) {
            gpxFile = File(exportDir, "$baseName.gpx")
            writeGpxFile(locations, options.redactLocation, gpxFile)
        }

        ClipExportResult(videoFile, secondaryVideoFile, metadataFile, gpxFile)
    }

    private fun writeMetadataJson(
        clip: ClipEntity,
        locations: List<LocationSampleEntity>,
        headings: List<HeadingSampleEntity>,
        redactLocation: Boolean,
        outputFile: File
    ) {
        val metadata = mutableMapOf<String, Any?>(
            "appName" to "DriveVault Dashcam",
            "clipId" to clip.id,
            "recordingStart" to clip.startTimeMillis,
            "recordingEnd" to clip.endTimeMillis,
            "durationMillis" to clip.durationMillis,
            "cameraMode" to clip.cameraMode,
            "videoInfo" to mapOf(
                "width" to clip.width,
                "height" to clip.height,
                "fps" to clip.fps,
                "bitrate" to clip.bitrate,
                "fileSizeBytes" to clip.fileSizeBytes,
                "audioEnabled" to clip.audioEnabled,
                "primaryFileUri" to clip.fileUri,
                "secondaryFileUri" to clip.secondaryFileUri
            ),
            "averageSpeedMps" to clip.averageSpeedMps,
            "maxSpeedMps" to clip.maxSpeedMps,
            "privacyRedactionStatus" to if (redactLocation) "LOCATION_REDACTED" else "FULL"
        )

        if (!redactLocation) {
            metadata["locationSamples"] = locations.map { loc ->
                mapOf(
                    "timestamp" to loc.timestampMillis,
                    "latitude" to loc.latitude,
                    "longitude" to loc.longitude,
                    "altitude" to loc.altitude,
                    "accuracy" to loc.accuracy,
                    "speedMps" to loc.speedMps,
                    "bearing" to loc.bearingDegrees
                )
            }
            metadata["headingSamples"] = headings.map { h ->
                mapOf(
                    "timestamp" to h.timestampMillis,
                    "headingDegrees" to h.headingDegrees,
                    "source" to h.source
                )
            }
            metadata["startLocation"] = mapOf(
                "latitude" to clip.startLatitude,
                "longitude" to clip.startLongitude
            )
            metadata["endLocation"] = mapOf(
                "latitude" to clip.endLatitude,
                "longitude" to clip.endLongitude
            )
        }

        val writer = FileWriter(outputFile)
        gson.toJson(metadata, writer)
        writer.close()
    }

    private fun writeGpxFile(
        locations: List<LocationSampleEntity>,
        redactLocation: Boolean,
        outputFile: File
    ) {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }

        val trkpts = if (redactLocation) "" else locations.joinToString("\n") { loc ->
            """      <trkpt lat="${loc.latitude}" lon="${loc.longitude}">
        <ele>${loc.altitude}</ele>
        <time>${dateFormat.format(Date(loc.timestampMillis))}</time>
        <speed>${loc.speedMps}</speed>
        <course>${loc.bearingDegrees}</course>
      </trkpt>"""
        }

        val gpx = """<?xml version="1.0" encoding="UTF-8"?>
<gpx version="1.1" creator="DriveVault Dashcam"
  xmlns="http://www.topografix.com/GPX/1/1"
  xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
  xsi:schemaLocation="http://www.topografix.com/GPX/1/1 http://www.topografix.com/GPX/1/1/gpx.xsd">
  <trk>
    <name>DriveVault Recording</name>
    <trkseg>
$trkpts
    </trkseg>
  </trk>
</gpx>"""

        outputFile.writeText(gpx)
    }

    fun createShareIntent(clipId: Long, result: ClipExportResult): Intent? {
        val files = listOfNotNull(result.videoFile, result.secondaryVideoFile, result.metadataFile, result.gpxFile)
        if (files.isEmpty()) return null

        val uris = files.mapNotNull { file ->
            try {
                FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            } catch (_: Exception) { null }
        }
        if (uris.isEmpty()) return null

        val intent = if (uris.size == 1) {
            Intent(Intent.ACTION_SEND).apply {
                type = getMimeType(uris.first().toString())
                putExtra(Intent.EXTRA_STREAM, uris.first())
            }
        } else {
            Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                type = "*/*"
                putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uris))
            }
        }

        return intent.apply {
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    suspend fun saveToGallery(clipId: Long): Boolean = withContext(Dispatchers.IO) {
        val clip = db.clipDao().getById(clipId) ?: return@withContext false
        val file = File(clip.fileUri)
        if (!file.exists()) return@withContext false

        val dateFormat = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US)
        val fileName = "DriveVault_${dateFormat.format(Date(clip.startTimeMillis))}.mp4"

        val values = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, fileName)
            put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
            put(MediaStore.Video.Media.DATE_ADDED, System.currentTimeMillis() / 1000)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Video.Media.RELATIVE_PATH, Environment.DIRECTORY_MOVIES + "/DriveVault")
                put(MediaStore.Video.Media.IS_PENDING, 1)
            }
        }

        val resolver = context.contentResolver
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        } else {
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        }

        val uri = resolver.insert(collection, values) ?: return@withContext false

        try {
            resolver.openOutputStream(uri)?.use { out ->
                file.inputStream().use { it.copyTo(out) }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                values.clear()
                values.put(MediaStore.Video.Media.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
            }
            true
        } catch (_: Exception) {
                resolver.delete(uri, null, null)
                false
            }
    }

    private fun getMimeType(path: String): String {
        val ext = path.substringAfterLast('.', "")
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext) ?: "*/*"
    }
}
