package com.tdvorak.nothingmodes.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.tdvorak.nothingmodes.ui.theme.NothingShapes
import androidx.compose.material3.MaterialTheme
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.FillLayer
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.Feature
import org.maplibre.geojson.Point
import org.maplibre.geojson.Polygon
import kotlinx.coroutines.launch

/** OpenFreeMap vector styles — free, no API key, OpenMapTiles schema.
 *  `dark` derives from the CartoDB Dark Matter cartography; `positron` is the
 *  matching light style. */
private const val OFM_DARK_STYLE = "https://tiles.openfreemap.org/styles/dark"
private const val OFM_LIGHT_STYLE = "https://tiles.openfreemap.org/styles/positron"
private const val FENCE_FILL_LAYER = "nm-fence-fill"
private const val FENCE_LINE_LAYER = "nm-fence-line"
private const val FENCE_PIN_LAYER = "nm-fence-pin"
private const val FENCE_SOURCE = "nm-fence-src"
private const val PIN_SOURCE = "nm-pin-src"
private const val FENCE_RED = "#FF3C3C"

/** Interactive map for picking a location + radius. OpenFreeMap vector tiles,
 *  dark/light matched to the app theme; tap places the fence centre and calls
 *  [onPick]. Includes address search and a current-location shortcut so every
 *  map in the app behaves the same. The red outline/pin follow [radiusM]. */
@Composable
fun LocationMapPicker(
    lat: Double,
    lng: Double,
    radiusM: Double,
    onPick: (lat: Double, lng: Double) -> Unit,
    modifier: Modifier = Modifier,
    heightDp: Int = 220,
    showSearch: Boolean = true,
    showCurrentLocation: Boolean = true,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val isDark = isSystemInDarkTheme()
    val focusManager = LocalFocusManager.current
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    var map by remember { mutableStateOf<MapLibreMap?>(null) }
    var styleReady by remember { mutableStateOf(false) }
    var addressQuery by remember { mutableStateOf("") }
    var searching by remember { mutableStateOf(false) }

    // MapLibre must be initialised before a MapView can be constructed.
    val mapView =
        remember {
            MapLibre.getInstance(context.applicationContext)
            MapView(context)
        }

    fun searchAddress() {
        val q = addressQuery.trim()
        if (q.isEmpty() || searching) return
        scope.launch {
            searching = true
            val address = geocodeAddress(context, q)
            searching = false
            if (address != null) {
                onPick(address.latitude, address.longitude)
            } else {
                android.widget.Toast.makeText(context, "Address not found", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }

    DisposableEffect(lifecycleOwner) {
        mapView.onCreate(null)
        mapView.getMapAsync { m ->
            map = m
            m.uiSettings.apply {
                isLogoEnabled = false
                isCompassEnabled = false
                isAttributionEnabled = true
                attributionGravity = android.view.Gravity.BOTTOM or android.view.Gravity.END
                setAttributionMargins(0, 0, 12, 12)
            }
            m.cameraPosition =
                CameraPosition.Builder()
                    .target(LatLng(if (lat == 0.0 && lng == 0.0) 50.0755 else lat, if (lat == 0.0 && lng == 0.0) 14.4378 else lng))
                    .zoom(15.0)
                    .build()
            m.addOnMapClickListener { point ->
                focusManager.clearFocus()
                onPick(point.latitude, point.longitude)
                true
            }
        }
        val observer =
            LifecycleEventObserver { _, event ->
                when (event) {
                    Lifecycle.Event.ON_START -> mapView.onStart()
                    Lifecycle.Event.ON_RESUME -> mapView.onResume()
                    Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                    Lifecycle.Event.ON_STOP -> mapView.onStop()
                    Lifecycle.Event.ON_DESTROY -> mapView.onDestroy()
                    else -> Unit
                }
            }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            mapView.onStop()
            mapView.onDestroy()
        }
    }

    // Restyle when the theme flips between light and dark.
    LaunchedEffect(isDark, map) {
        val m = map ?: return@LaunchedEffect
        styleReady = false
        m.setStyle(Style.Builder().fromUri(if (isDark) OFM_DARK_STYLE else OFM_LIGHT_STYLE)) { style ->
            if (isDark) applyNothingDarkTint(style)
            styleReady = true
        }
    }

    // Redraw the fence circle and red pin whenever the centre or radius changes.
    LaunchedEffect(lat, lng, radiusM, styleReady, map) {
        val m = map ?: return@LaunchedEffect
        if (!styleReady) return@LaunchedEffect
        if (lat != 0.0 || lng != 0.0) {
            val center = LatLng(lat, lng)
            m.style?.let { style -> applyFence(style, center, radiusM) }
            m.animateCamera(CameraUpdateFactory.newLatLng(center))
        }
    }

    androidx.compose.foundation.layout.Column(modifier = modifier) {
        if (showSearch) {
            com.tdvorak.nothingmodes.ui.theme.NothingInput(
                value = addressQuery,
                onValueChange = { addressQuery = it },
                label = "Search address",
                placeholder = "Street, city, place...",
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = androidx.compose.ui.text.input.ImeAction.Search),
                keyboardActions = androidx.compose.foundation.text.KeyboardActions(onSearch = { searchAddress() }),
                infoText = "Type a place and press search — works worldwide.",
            )
            if (searching) {
                androidx.compose.material3.Text(
                    text = "Searching...",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontFamily = com.tdvorak.nothingmodes.ui.theme.NothingFonts.mono(),
                    modifier = Modifier.padding(top = com.tdvorak.nothingmodes.ui.theme.NothingSpacing.xs),
                )
            }
            androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(com.tdvorak.nothingmodes.ui.theme.NothingSpacing.sm))
        }
        androidx.compose.material3.Text(
            text = "Tap the map to place the point.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontFamily = com.tdvorak.nothingmodes.ui.theme.NothingFonts.mono(),
            modifier = Modifier.fillMaxWidth(),
        )
        androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(com.tdvorak.nothingmodes.ui.theme.NothingSpacing.sm))
        AndroidView(
            factory = { mapView },
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(heightDp.dp)
                    .clip(NothingShapes.input)
                    .border(1.dp, MaterialTheme.colorScheme.outline, NothingShapes.input),
        )
        if (showCurrentLocation) {
            androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(com.tdvorak.nothingmodes.ui.theme.NothingSpacing.sm))
            com.tdvorak.nothingmodes.ui.theme.NothingPillButton(
                text = "Use current location",
                onClick = {
                    val fine =
                        context.checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION) ==
                            android.content.pm.PackageManager.PERMISSION_GRANTED
                    val coarse =
                        context.checkSelfPermission(android.Manifest.permission.ACCESS_COARSE_LOCATION) ==
                            android.content.pm.PackageManager.PERMISSION_GRANTED
                    if (!fine && !coarse) return@NothingPillButton
                    runCatching {
                        com.google.android.gms.location.LocationServices
                            .getFusedLocationProviderClient(context)
                            .lastLocation
                            .addOnSuccessListener { location ->
                                if (location != null) onPick(location.latitude, location.longitude)
                            }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/** Device-geocodes an address string — resolves worldwide. */
private suspend fun geocodeAddress(
    context: android.content.Context,
    query: String,
): android.location.Address? =
    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        runCatching {
            val geocoder = android.location.Geocoder(context, java.util.Locale.getDefault())
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                kotlinx.coroutines.suspendCancellableCoroutine { cont ->
                    geocoder.getFromLocationName(query, 1) { results ->
                        cont.resume(results.firstOrNull()) {}
                    }
                }
            } else {
                @Suppress("DEPRECATION")
                geocoder.getFromLocationName(query, 1)?.firstOrNull()
            }
        }.getOrNull()
    }

/** Draw (or redraw) the fence fill + outline + centre pin on a loaded style. */
private fun applyFence(
    style: Style,
    center: LatLng,
    radiusM: Double,
) {
    val circle = Feature.fromGeometry(Polygon.fromLngLats(listOf(circleRing(center, radiusM))))
    val pin = Feature.fromGeometry(Point.fromLngLat(center.longitude, center.latitude))

    val fenceSrc = style.getSourceAs<GeoJsonSource>(FENCE_SOURCE)
    if (fenceSrc != null) {
        fenceSrc.setGeoJson(circle)
        style.getSourceAs<GeoJsonSource>(PIN_SOURCE)?.setGeoJson(pin)
        return
    }

    style.addSource(GeoJsonSource(FENCE_SOURCE, circle))
    style.addSource(GeoJsonSource(PIN_SOURCE, pin))
    style.addLayer(
        FillLayer(FENCE_FILL_LAYER, FENCE_SOURCE).withProperties(
            PropertyFactory.fillColor(FENCE_RED),
            PropertyFactory.fillOpacity(0.06f),
        ),
    )
    style.addLayer(
        LineLayer(FENCE_LINE_LAYER, FENCE_SOURCE).withProperties(
            PropertyFactory.lineColor(FENCE_RED),
            PropertyFactory.lineWidth(2f),
        ),
    )
    style.addLayer(
        CircleLayer(FENCE_PIN_LAYER, PIN_SOURCE).withProperties(
            PropertyFactory.circleColor(FENCE_RED),
            PropertyFactory.circleRadius(6f),
            PropertyFactory.circleStrokeWidth(2f),
            PropertyFactory.circleStrokeColor("#FFFFFF"),
        ),
    )
}

/** Recolors the loaded OpenFreeMap "dark" style toward the Nothing palette:
 *  near-black canvas and land, muted gray streets, and a restrained red only
 *  on major roads — matching the fence pin's accent instead of the style's
 *  maroon default. */
private fun applyNothingDarkTint(style: Style) {
    style.layers.forEach { layer ->
        when (layer) {
            is org.maplibre.android.style.layers.BackgroundLayer ->
                layer.setProperties(PropertyFactory.backgroundColor("#000000"))
            is FillLayer -> {
                val fill =
                    when (layer.sourceLayer) {
                        "water", "ocean" -> "#050505"
                        "landcover", "landuse", "park", "aeroway" -> "#0A0A0A"
                        "building", "building-3d" -> "#161616"
                        else -> "#0A0A0A"
                    }
                layer.setProperties(PropertyFactory.fillColor(fill))
            }
            is LineLayer -> {
                when (layer.sourceLayer) {
                    "transportation" -> {
                        // Major roads get the muted red accent; the rest fade to gray.
                        val id = layer.id.lowercase()
                        if (id.contains("motorway") || id.contains("trunk") || id.contains("major")) {
                            layer.setProperties(
                                PropertyFactory.lineColor("#B3282D"),
                                PropertyFactory.lineOpacity(0.85f),
                            )
                        } else {
                            layer.setProperties(PropertyFactory.lineColor("#2E2E2E"))
                        }
                    }
                    "waterway", "water" -> layer.setProperties(PropertyFactory.lineColor("#141414"))
                    "boundary" -> layer.setProperties(PropertyFactory.lineColor("#2A2A2A"))
                }
            }
            is org.maplibre.android.style.layers.SymbolLayer ->
                layer.setProperties(
                    PropertyFactory.textColor("#8A8A8A"),
                    PropertyFactory.textHaloColor("#000000"),
                    PropertyFactory.textHaloWidth(1f),
                )
            else -> Unit
        }
    }
}

/** A geodesic-ish circle ring around [center], [steps]+1 points, closed. */
private fun circleRing(
    center: LatLng,
    radiusM: Double,
    steps: Int = 72,
): List<Point> {
    val latDegPerM = 1.0 / 111320.0
    val lngDegPerM = 1.0 / (111320.0 * kotlin.math.cos(Math.toRadians(center.latitude)).coerceAtLeast(0.01))
    return (0..steps).map { i ->
        val a = 2.0 * Math.PI * i / steps
        Point.fromLngLat(
            center.longitude + radiusM * kotlin.math.sin(a) * lngDegPerM,
            center.latitude + radiusM * kotlin.math.cos(a) * latDegPerM,
        )
    }
}
