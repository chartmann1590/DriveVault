package com.drivevault.dashcam.recording

import android.graphics.RectF
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.objects.ObjectDetection
import com.google.mlkit.vision.objects.ObjectDetector
import com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class DetectedObjectResult(
    val boundingBox: RectF,
    val label: String,
    val confidence: Float,
    val imageWidth: Int,
    val imageHeight: Int,
    val imageRotationDegrees: Int
)

class ObjectDetectionManager {

    private val detector: ObjectDetector = ObjectDetection.getClient(
        ObjectDetectorOptions.Builder()
            .setDetectorMode(ObjectDetectorOptions.STREAM_MODE)
            .enableClassification()
            .enableMultipleObjects()
            .build()
    )

    private val _detectedObjects = MutableStateFlow<List<DetectedObjectResult>>(emptyList())
    val detectedObjects: StateFlow<List<DetectedObjectResult>> = _detectedObjects.asStateFlow()

    @ExperimentalGetImage
    fun processImage(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage == null) {
            imageProxy.close()
            return
        }
        val rotationDegrees = imageProxy.imageInfo.rotationDegrees
        val imageWidth = imageProxy.width
        val imageHeight = imageProxy.height
        val inputImage = InputImage.fromMediaImage(mediaImage, rotationDegrees)

        detector.process(inputImage)
            .addOnSuccessListener { objects ->
                val results = objects.mapNotNull { obj ->
                    val best = obj.labels
                        .filter { it.text == "Vehicle" || it.text == "Person" }
                        .maxByOrNull { it.confidence }
                    best?.let {
                        DetectedObjectResult(
                            boundingBox = RectF(obj.boundingBox),
                            label = it.text,
                            confidence = it.confidence,
                            imageWidth = imageWidth,
                            imageHeight = imageHeight,
                            imageRotationDegrees = rotationDegrees
                        )
                    }
                }
                _detectedObjects.value = results
            }
            .addOnFailureListener { _detectedObjects.value = emptyList() }
            .addOnCompleteListener { imageProxy.close() }
    }

    fun close() {
        detector.close()
        _detectedObjects.value = emptyList()
    }
}
