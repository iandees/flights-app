package com.iandees.flights.feature.map

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.iandees.flights.core.model.Flight
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapView
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.PropertyFactory.lineColor
import org.maplibre.android.style.layers.PropertyFactory.lineOpacity
import org.maplibre.android.style.layers.PropertyFactory.lineWidth
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.LineString
import org.maplibre.geojson.Point

private const val STYLE_URL  = "https://demotiles.maplibre.org/style.json"
private const val SOURCE_ID  = "flight-arcs"
private const val LAYER_ID   = "flight-arcs-layer"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(viewModel: MapViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(topBar = { TopAppBar(title = { Text("Map") }) }) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (uiState.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else {
                FlightMapView(
                    flights = uiState.flights,
                    coords  = uiState.coords,
                    modifier = Modifier.fillMaxSize(),
                )
                Surface(
                    modifier = Modifier.align(Alignment.TopEnd).padding(12.dp),
                    shape = MaterialTheme.shapes.medium,
                    tonalElevation = 4.dp,
                ) {
                    Text(
                        text = "${uiState.flights.size} flights",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }
        }
    }
}

@Composable
private fun FlightMapView(
    flights: List<Flight>,
    coords: Map<String, Pair<Double, Double>>,
    modifier: Modifier = Modifier,
) {
    val featureCollection = remember(flights, coords) {
        val features = flights.mapNotNull { flight ->
            val from = coords[flight.departureAirport] ?: return@mapNotNull null
            val to   = coords[flight.arrivalAirport]   ?: return@mapNotNull null
            val pts  = greatCirclePoints(from.first, from.second, to.first, to.second)
                .map { (lat, lon) -> Point.fromLngLat(lon, lat) }
            Feature.fromGeometry(LineString.fromLngLats(pts))
        }
        FeatureCollection.fromFeatures(features)
    }

    AndroidView(
        modifier = modifier,
        factory = { context ->
            MapLibre.getInstance(context)
            MapView(context).apply {
                getMapAsync { map ->
                    map.setStyle(STYLE_URL) { style ->
                        // Add source + layer once the style loads
                        style.addSource(GeoJsonSource(SOURCE_ID, featureCollection))
                        style.addLayer(
                            LineLayer(LAYER_ID, SOURCE_ID).withProperties(
                                lineColor("#3B82F6"),
                                lineWidth(1.5f),
                                lineOpacity(0.7f),
                            )
                        )
                    }
                    map.cameraPosition = CameraPosition.Builder()
                        .target(LatLng(20.0, 0.0))
                        .zoom(1.0)
                        .build()
                }
            }
        },
        update = { mapView ->
            // Update GeoJSON source when flights change
            mapView.getMapAsync { map ->
                map.getStyle { style ->
                    style.getSourceAs<GeoJsonSource>(SOURCE_ID)?.setGeoJson(featureCollection)
                }
            }
        },
        onRelease = { mapView -> mapView.onDestroy() },
    )
}
