package com.drivevault.dashcam.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.drivevault.dashcam.domain.model.TelemetryState
import com.drivevault.dashcam.domain.util.UnitConverter
import com.drivevault.dashcam.ui.theme.*
import java.util.Locale

@Composable
fun SpeedOverlayCard(
    telemetry: TelemetryState,
    speedUnit: String,
    modifier: Modifier = Modifier
) {
    GlassPanel(
        modifier = modifier,
        cornerRadius = 12.dp
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = UnitConverter.formatSpeed(telemetry.displaySpeed, speedUnit),
                style = MaterialTheme.typography.displayLarge,
                color = OnSurface,
                maxLines = 1
            )
        }
    }
}

@Composable
fun GpsOverlayCard(
    telemetry: TelemetryState,
    showCoordinates: Boolean,
    modifier: Modifier = Modifier
) {
    GlassPanel(
        modifier = modifier,
        cornerRadius = 12.dp
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val statusColor = if (telemetry.gpsFixed) ElectricBlue else AmberWarning
                Canvas(modifier = Modifier.size(8.dp)) {
                    drawCircle(statusColor)
                }
                Text(
                    text = if (telemetry.gpsFixed) "GPS FIXED" else "NO GPS",
                    style = MaterialTheme.typography.labelMedium,
                    color = statusColor
                )
            }
            val loc = telemetry.location
            if (showCoordinates && loc != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = String.format(Locale.US, "%.5f, %.5f", loc.latitude, loc.longitude),
                    style = MaterialTheme.typography.bodySmall,
                    color = OnSurfaceVariant,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
fun HeadingOverlayCard(
    telemetry: TelemetryState,
    modifier: Modifier = Modifier
) {
    val heading = telemetry.displayHeading
    val direction = telemetry.compassDirection

    GlassPanel(
        modifier = modifier,
        cornerRadius = 12.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (heading != null && direction != null) {
                Text(
                    text = direction.abbreviation,
                    style = MaterialTheme.typography.headlineMedium,
                    color = OnSurface
                )
                Text(
                    text = String.format(Locale.US, "%03.0f°", heading),
                    style = MaterialTheme.typography.titleMedium,
                    color = OnSurfaceVariant
                )
            } else {
                Text(
                    text = "-- --°",
                    style = MaterialTheme.typography.titleMedium,
                    color = OnSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun TimestampOverlayCard(
    timestamp: Long,
    modifier: Modifier = Modifier
) {
    GlassPanel(
        modifier = modifier,
        cornerRadius = 12.dp
    ) {
        val date = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date(timestamp))
        val time = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.US).format(java.util.Date(timestamp))
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Text(text = date, style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant)
            Text(text = time, style = MaterialTheme.typography.titleMedium, color = OnSurface)
        }
    }
}
