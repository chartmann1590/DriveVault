package com.drivevault.dashcam.map

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.compass.CompassOverlay
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay

@Composable
fun MiniMapOverlay(
    latitude: Double,
    longitude: Double,
    heading: Float?,
    modifier: Modifier = Modifier,
    routePoints: List<GeoPoint> = emptyList()
) {
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        Configuration.getInstance().load(context, context.getSharedPreferences("osmdroid", 0))
    }

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            Configuration.getInstance().userAgentValue = "DriveVault-Dashcam"
            MapView(ctx).apply {
                setTileSource(TileSourceFactory.MAPNIK)
                setMultiTouchControls(false)
                isTilesScaledToDpi = true
                controller.setZoom(16.0)
                val startPoint = GeoPoint(latitude, longitude)
                controller.setCenter(startPoint)

                val locationOverlay = MyLocationNewOverlay(GpsMyLocationProvider(ctx), this)
                locationOverlay.enableMyLocation()
                overlays.add(locationOverlay)

                val compassOverlay = CompassOverlay(ctx, this)
                compassOverlay.enableCompass()
                overlays.add(compassOverlay)
            }
        },
        update = { mapView ->
            val point = GeoPoint(latitude, longitude)
            mapView.controller.animateTo(point)
        }
    )
}
