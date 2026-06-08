package com.drivevault.dashcam.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.drivevault.dashcam.ui.theme.*

@Composable
fun GlassPanel(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 16.dp,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(cornerRadius),
        color = GlassOverlayColor,
        border = BorderStroke(1.dp, GlassBorder),
        content = content
    )
}

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 24.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(cornerRadius),
        color = GlassOverlayColor,
        border = BorderStroke(1.dp, GlassBorder),
        content = { Column(content = content) }
    )
}

@Composable
fun StatusChip(
    text: String,
    isActive: Boolean,
    modifier: Modifier = Modifier,
    activeColor: Color = ElectricBlue,
    inactiveColor: Color = Color.White.copy(alpha = 0.3f)
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = if (isActive) activeColor.copy(alpha = 0.2f) else inactiveColor.copy(alpha = 0.1f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isActive) {
                Canvas(modifier = Modifier.size(6.dp)) {
                    drawCircle(activeColor)
                }
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                color = if (isActive) activeColor else Color.White.copy(alpha = 0.5f)
            )
        }
    }
}

@Composable
fun RecordingIndicator(
    isRecording: Boolean,
    modifier: Modifier = Modifier
) {
    if (!isRecording) return
    val infiniteTransition = rememberInfiniteTransition()
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(800),
            repeatMode = RepeatMode.Reverse
        )
    )
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = SafetyRedDeep.copy(alpha = 0.7f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Canvas(modifier = Modifier.size(10.dp)) {
                drawCircle(SafetyRed.copy(alpha = pulseAlpha))
            }
            Text(
                text = "REC",
                style = MaterialTheme.typography.labelLarge,
                color = SafetyRed
            )
        }
    }
}
