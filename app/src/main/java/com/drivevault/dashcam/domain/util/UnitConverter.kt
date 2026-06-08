package com.drivevault.dashcam.domain.util

object UnitConverter {
    fun mpsToMph(mps: Float): Float = mps * 2.23694f
    fun mpsToKph(mps: Float): Float = mps * 3.6f
    fun mpsToUnit(mps: Float, unit: String): Float = if (unit == "KPH") mpsToKph(mps) else mpsToMph(mps)
    fun formatSpeed(mps: Float?, unit: String): String {
        if (mps == null || mps <= 0f) return if (unit == "KPH") "-- KPH" else "-- MPH"
        val converted = mpsToUnit(mps, unit)
        return "${converted.toInt()} $unit"
    }
}
