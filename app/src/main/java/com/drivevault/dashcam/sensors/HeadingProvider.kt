package com.drivevault.dashcam.sensors

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import com.drivevault.dashcam.domain.model.HeadingData
import com.drivevault.dashcam.domain.model.HeadingSource
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

class HeadingProvider(private val context: Context) {

    private val sensorManager by lazy { context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager }

    private var rotationVectorSensor: Sensor? = null
    private var accelerometer: Sensor? = null
    private var magnetometer: Sensor? = null

    private val hasRotationVector: Boolean get() = sensorManager?.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR) != null
    private val hasAccelMag: Boolean get() = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null && sensorManager?.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD) != null

    val isAvailable: Boolean get() = hasRotationVector || hasAccelMag

    fun observeHeading(): Flow<HeadingData?> = callbackFlow {
        val sm = sensorManager
        if (sm == null) {
            trySend(null)
            close()
            return@callbackFlow
        }

        if (hasRotationVector) {
            rotationVectorSensor = sm.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
            val listener = object : SensorEventListener {
                private val rotationMatrix = FloatArray(9)
                private val orientationValues = FloatArray(3)

                override fun onSensorChanged(event: SensorEvent) {
                    SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
                    SensorManager.getOrientation(rotationMatrix, orientationValues)
                    val heading = Math.toDegrees(orientationValues[0].toDouble()).toFloat()
                    val normalized = ((heading % 360) + 360) % 360
                    trySend(HeadingData(normalized, HeadingSource.ROTATION_VECTOR, System.currentTimeMillis()))
                }
                override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {}
            }
            sm.registerListener(listener, rotationVectorSensor, SensorManager.SENSOR_DELAY_UI)
            awaitClose { sm.unregisterListener(listener) }
        } else if (hasAccelMag) {
            accelerometer = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
            magnetometer = sm.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
            val listener = object : SensorEventListener {
                private val gravity = FloatArray(3)
                private val geomagnetic = FloatArray(3)
                private val rotationMatrix = FloatArray(9)
                private val orientationValues = FloatArray(3)
                private var hasGravity = false
                private var hasGeomagnetic = false

                override fun onSensorChanged(event: SensorEvent) {
                    when (event.sensor.type) {
                        Sensor.TYPE_ACCELEROMETER -> {
                            System.arraycopy(event.values, 0, gravity, 0, 3)
                            hasGravity = true
                        }
                        Sensor.TYPE_MAGNETIC_FIELD -> {
                            System.arraycopy(event.values, 0, geomagnetic, 0, 3)
                            hasGeomagnetic = true
                        }
                    }
                    if (hasGravity && hasGeomagnetic) {
                        if (SensorManager.getRotationMatrix(rotationMatrix, null, gravity, geomagnetic)) {
                            SensorManager.getOrientation(rotationMatrix, orientationValues)
                            val heading = Math.toDegrees(orientationValues[0].toDouble()).toFloat()
                            val normalized = ((heading % 360) + 360) % 360
                            trySend(HeadingData(normalized, HeadingSource.ACCELEROMETER_MAGNETOMETER, System.currentTimeMillis()))
                        }
                    }
                }
                override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {}
            }
            sm.registerListener(listener, accelerometer, SensorManager.SENSOR_DELAY_UI)
            sm.registerListener(listener, magnetometer, SensorManager.SENSOR_DELAY_UI)
            awaitClose { sm.unregisterListener(listener) }
        } else {
            trySend(null)
            close()
            return@callbackFlow
        }
    }.distinctUntilChanged()

    companion object {
        fun bearingFromLocations(startLat: Double, startLon: Double, endLat: Double, endLon: Double): Float {
            val lat1 = Math.toRadians(startLat)
            val lon1 = Math.toRadians(startLon)
            val lat2 = Math.toRadians(endLat)
            val lon2 = Math.toRadians(endLon)
            val dLon = lon2 - lon1
            val y = sin(dLon) * cos(lat2)
            val x = cos(lat1) * sin(lat2) - sin(lat1) * cos(lat2) * cos(dLon)
            val bearing = Math.toDegrees(atan2(y, x)).toFloat()
            return ((bearing % 360) + 360) % 360
        }
    }
}
