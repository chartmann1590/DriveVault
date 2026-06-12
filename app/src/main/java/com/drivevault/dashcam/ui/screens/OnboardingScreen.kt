package com.drivevault.dashcam.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.drivevault.dashcam.ui.theme.*

data class OnboardingPage(
    val icon: ImageVector,
    val title: String,
    val description: String
)

@Composable
fun OnboardingScreen(
    onComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val pages = listOf(
        OnboardingPage(
            icon = Icons.Filled.Videocam,
            title = "Welcome to DriveVault",
            description = "A professional-grade dashcam app that records your drives with GPS, speed, and heading overlays."
        ),
        OnboardingPage(
            icon = Icons.Filled.Security,
            title = "Safety First",
            description = "This app is designed to be a set-and-forget dashcam. Always drive safely and obey local traffic laws. Never interact with the app while driving."
        ),
        OnboardingPage(
            icon = Icons.Filled.LocationOn,
            title = "Permissions Required",
            description = "DriveVault needs camera, microphone, and location permissions to record videos with GPS overlays. Notifications are needed for the recording service."
        ),
        OnboardingPage(
            icon = Icons.Filled.Cloud,
            title = "Optional Immich Sync",
            description = "Optionally sync clips to your self-hosted Immich server. No data is sent anywhere unless you explicitly enable and configure it."
        ),
        OnboardingPage(
            icon = Icons.Filled.PlayArrow,
            title = "Ready to Record",
            description = "You're all set! Tap the record button to start capturing your drives. Clips are saved as 60-second segments."
        )
    )

    var currentPage by remember { mutableIntStateOf(0) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DeepCharcoal)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = pages[currentPage].icon,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = SafetyRed
            )
            Spacer(modifier = Modifier.height(32.dp))
            Text(
                text = pages[currentPage].title,
                style = MaterialTheme.typography.headlineMedium,
                color = OnSurface,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = pages[currentPage].description,
                style = MaterialTheme.typography.bodyLarge,
                color = OnSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(pages.size) { index ->
                Surface(
                    modifier = Modifier
                        .padding(4.dp)
                        .size(if (index == currentPage) 10.dp else 8.dp),
                    shape = RoundedCornerShape(5.dp),
                    color = if (index == currentPage) SafetyRed else OnSurfaceVariant.copy(alpha = 0.3f)
                ) {}
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            if (currentPage > 0) {
                OutlinedButton(onClick = { currentPage-- }) {
                    Text("Back")
                }
            } else {
                Spacer(modifier = Modifier.width(1.dp))
            }

            Button(
                onClick = {
                    if (currentPage < pages.size - 1) {
                        currentPage++
                    } else {
                        onComplete()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = SafetyRed)
            ) {
                Text(if (currentPage < pages.size - 1) "Next" else "Get Started")
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}