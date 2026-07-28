package adamtri.rs.mymap.ui

import android.location.Geocoder
import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.*
import com.google.maps.android.PolyUtil
import com.google.maps.android.compose.*
import com.google.maps.android.compose.Circle
import adamtri.rs.mymap.data.api.RetrofitClient
import adamtri.rs.mymap.data.model.PlaceDetails
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    modifier: Modifier = Modifier,
    isDarkMode: Boolean = false,
    onToggleDarkMode: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    
    // UI State for Text Fields
    var originText by remember { mutableStateOf("-7.9731565,112.609915") }
    var destinationText by remember { mutableStateOf("-7.9826092,112.6282364") }
    
    // UI State for Markers and Route
    var originLatLng by remember { mutableStateOf<LatLng?>(LatLng(-7.9731565, 112.609915)) }
    var destinationLatLng by remember { mutableStateOf<LatLng?>(LatLng(-7.9826092, 112.6282364)) }
    var routePoints by remember { mutableStateOf<List<LatLng>>(emptyList()) }
    
    // UI State for Route Details
    var distance by remember { mutableStateOf("") }
    var duration by remember { mutableStateOf("") }
    var transportMode by remember { mutableStateOf("Roda 4") }
    var selectedMode by remember { mutableStateOf("driving") } // driving or two_wheeler
    
    // Target Distance States
    var targetDistance by remember { mutableStateOf(5f) } // Target distance in km
    var actualDistanceValue by remember { mutableStateOf(0) } // Actual distance from API in meters
    var showRadius by remember { mutableStateOf(true) } // Toggle for circle visibility
    var isLoading by remember { mutableStateOf(false) }
    
    // Place Details State
    var destinationDetails by remember { mutableStateOf<PlaceDetails?>(null) }
    
    val scaffoldState = rememberBottomSheetScaffoldState()
    
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(originLatLng!!, 14f)
    }

    val darkMapStyle = """
        [
          { "elementType": "geometry", "stylers": [ { "color": "#242f3e" } ] },
          { "elementType": "labels.text.stroke", "stylers": [ { "color": "#242f3e" } ] },
          { "elementType": "labels.text.fill", "stylers": [ { "color": "#746855" } ] },
          { "featureType": "administrative.locality", "elementType": "labels.text.fill", "stylers": [ { "color": "#d59563" } ] },
          { "featureType": "poi", "elementType": "labels.text.fill", "stylers": [ { "color": "#d59563" } ] },
          { "featureType": "poi.park", "elementType": "geometry", "stylers": [ { "color": "#263c3f" } ] },
          { "featureType": "poi.park", "elementType": "labels.text.fill", "stylers": [ { "color": "#6b9a76" } ] },
          { "featureType": "road", "elementType": "geometry", "stylers": [ { "color": "#38414e" } ] },
          { "featureType": "road", "elementType": "geometry.stroke", "stylers": [ { "color": "#212a37" } ] },
          { "featureType": "road", "elementType": "labels.text.fill", "stylers": [ { "color": "#9ca5b3" } ] },
          { "featureType": "road.highway", "elementType": "geometry", "stylers": [ { "color": "#746855" } ] },
          { "featureType": "road.highway", "elementType": "geometry.stroke", "stylers": [ { "color": "#1f2835" } ] },
          { "featureType": "road.highway", "elementType": "labels.text.fill", "stylers": [ { "color": "#f3d19c" } ] },
          { "featureType": "transit", "elementType": "geometry", "stylers": [ { "color": "#2f3948" } ] },
          { "featureType": "transit.station", "elementType": "labels.text.fill", "stylers": [ { "color": "#d59563" } ] },
          { "featureType": "water", "elementType": "geometry", "stylers": [ { "color": "#17263c" } ] },
          { "featureType": "water", "elementType": "labels.text.fill", "stylers": [ { "color": "#515c6d" } ] },
          { "featureType": "water", "elementType": "labels.text.stroke", "stylers": [ { "color": "#17263c" } ] }
        ]
    """.trimIndent()

    val apiKey = "AIzaSyAMxzfxQjAg9Jr-WE5EtBpAE7xCXwz2B1Q"

    // Helper function to resolve location from query string
    suspend fun resolveLocation(query: String): LatLng? = withContext(Dispatchers.IO) {
        try {
            val geocoder = Geocoder(context)
            @Suppress("DEPRECATION")
            val addresses = geocoder.getFromLocationName(query, 1)
            if (!addresses.isNullOrEmpty()) {
                val address = addresses[0]
                LatLng(address.latitude, address.longitude)
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e("MapScreen", "Error resolving location: $query", e)
            null
        }
    }

    // Helper function to fetch place details from coordinates
    suspend fun fetchPlaceDetailsForLatLng(latLng: LatLng) {
        try {
            val response = RetrofitClient.getPlacesApiService(context).searchNearby(
                location = "${latLng.latitude},${latLng.longitude}",
                apiKey = apiKey
            )
            if (response.results.isNotEmpty()) {
                val placeId = response.results[0].placeId
                val detailResponse = RetrofitClient.getPlacesApiService(context).getPlaceDetails(placeId, apiKey = apiKey)
                if (detailResponse.status == "OK") {
                    destinationDetails = detailResponse.result
                }
            }
        } catch (e: Exception) {
            Log.e("MapScreen", "Error fetching nearby place details", e)
        }
    }

    // Helper function to fetch route
    fun fetchRoute() {
        scope.launch {
            isLoading = true
            destinationDetails = null
            try {
                // Parse or resolve origin
                val oLatLng = if (originText.contains(",")) {
                    val parts = originText.split(",")
                    if (parts.size == 2) {
                        try {
                            LatLng(parts[0].trim().toDouble(), parts[1].trim().toDouble())
                        } catch (e: Exception) {
                            resolveLocation(originText)
                        }
                    } else resolveLocation(originText)
                } else resolveLocation(originText)

                // Parse or resolve destination
                var dLatLng: LatLng? = null
                if (destinationText.contains(",")) {
                    val parts = destinationText.split(",")
                    if (parts.size == 2) {
                        try {
                            dLatLng = LatLng(parts[0].trim().toDouble(), parts[1].trim().toDouble())
                            
                            // If we have coordinates but no details, try to fetch them from nearby
                            if (destinationDetails == null) {
                                fetchPlaceDetailsForLatLng(dLatLng)
                            }
                        } catch (e: Exception) {
                            // Fallback to Places Search
                        }
                    }
                }

                // If not coordinates, search using Places API for details
                if (dLatLng == null) {
                    val searchResponse = RetrofitClient.getPlacesApiService(context).searchPlace(destinationText, apiKey)
                    if (searchResponse.results.isNotEmpty()) {
                        val result = searchResponse.results[0]
                        dLatLng = LatLng(result.geometry.location.lat, result.geometry.location.lng)
                        
                        // Fetch Full Details (Rating)
                        val detailResponse = RetrofitClient.getPlacesApiService(context).getPlaceDetails(result.placeId, apiKey = apiKey)
                        if (detailResponse.status == "OK") {
                            destinationDetails = detailResponse.result
                        }
                    } else {
                        // Fallback to Geocoder if Places API fails
                        dLatLng = resolveLocation(destinationText)
                    }
                }

                if (oLatLng == null) {
                    snackbarHostState.showSnackbar("Lokasi tidak ditemukan: $originText")
                    return@launch
                }
                if (dLatLng == null) {
                    snackbarHostState.showSnackbar("Lokasi tidak ditemukan: $destinationText")
                    return@launch
                }

                originLatLng = oLatLng
                destinationLatLng = dLatLng

                val response = RetrofitClient.getDirectionsApiService(context).getDirections(
                    origin = "${oLatLng.latitude},${oLatLng.longitude}",
                    destination = "${dLatLng.latitude},${dLatLng.longitude}",
                    mode = selectedMode,
                    apiKey = apiKey
                )
                if (response.routes.isNotEmpty()) {
                    val route = response.routes[0]
                    val encodedPolyline = route.overviewPolyline.points
                    routePoints = PolyUtil.decode(encodedPolyline)
                    
                    // Extract Distance and Duration
                    if (route.legs.isNotEmpty()) {
                        distance = route.legs[0].distance.text
                        duration = route.legs[0].duration.text
                        actualDistanceValue = route.legs[0].distance.value
                    }

                    // Automatic Camera Focus on Route
                    val bounds = LatLngBounds.builder()
                        .include(oLatLng)
                        .include(dLatLng)
                        .build()
                    cameraPositionState.animate(CameraUpdateFactory.newLatLngBounds(bounds, 150), 1000)
                    
                    // Auto-expand sheet to show details
                    scope.launch {
                        scaffoldState.bottomSheetState.expand()
                    }
                } else {
                    snackbarHostState.showSnackbar("Rute tidak ditemukan")
                }
            } catch (e: Exception) {
                Log.e("MapScreen", "Error fetching directions", e)
                snackbarHostState.showSnackbar("Gagal mengambil rute: ${e.message}")
            } finally {
                isLoading = false
            }
        }
    }

    // Reset function
    fun resetFields() {
        originText = ""
        destinationText = ""
        routePoints = emptyList()
        distance = ""
        duration = ""
        actualDistanceValue = 0
        targetDistance = 5f
        showRadius = true
        transportMode = "Roda 4"
        selectedMode = "driving"
        destinationDetails = null

        // Tetap tampilkan marker awal
        originLatLng = LatLng(-7.9731565, 112.609915)
        destinationLatLng = LatLng(-7.9826092, 112.6282364)
    }

    // Auto refresh when mode changes
    LaunchedEffect(selectedMode) {
        if (originText.isNotEmpty() && destinationText.isNotEmpty()) {
            fetchRoute()
        }
    }

    // Initial load
    LaunchedEffect(Unit) {
        fetchRoute()
    }

    BottomSheetScaffold(
        scaffoldState = scaffoldState,
        sheetPeekHeight = 140.dp,
        sheetDragHandle = { BottomSheetDefaults.DragHandle() },
        sheetContent = {
            if (isLoading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                // Compact Summary (Always visible in Peek Height)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        if (distance.isNotEmpty() || duration.isNotEmpty()) {
                            Text(
                                text = "$duration ($distance)",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color(0xFF00796B)
                            )
                            Text(
                                text = "Moda: $transportMode",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                text = if (actualDistanceValue <= targetDistance * 1000) "Dalam Target" else "Diluar Target",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (actualDistanceValue <= targetDistance * 1000) Color(0xFF4CAF50) else Color(0xFFF44336)
                            )
                            
                            // Rating visible in Peek Area
                            destinationDetails?.let { details ->
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = details.name,
                                        style = MaterialTheme.typography.bodySmall,
                                        maxLines = 1
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "⭐ ${details.rating ?: ""}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFFFBC02D)
                                    )
                                }
                            }
                        } else {
                            Text(
                                text = "Cari Rute Perjalanan",
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    }
                    Button(
                        onClick = { 
                            fetchRoute()
                            scope.launch { scaffoldState.bottomSheetState.partialExpand() }
                        },
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Icon(Icons.Default.Place, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Cari Rute")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Expanded Controls
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Tips: Tap peta untuk tujuan, Long-press untuk asal.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    OutlinedTextField(
                        value = originText,
                        onValueChange = { originText = it },
                        label = { Text("Origin (Lat,Lng)") },
                        modifier = Modifier.fillMaxWidth(),
                        leadingIcon = { Icon(Icons.Default.Place, contentDescription = null, tint = Color.Blue) }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = destinationText,
                        onValueChange = { destinationText = it },
                        label = { Text("Destination (Lat,Lng)") },
                        modifier = Modifier.fillMaxWidth(),
                        leadingIcon = { Icon(Icons.Default.Place, contentDescription = null, tint = Color.Red) }
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text("Pilih Moda Transportasi:", style = MaterialTheme.typography.labelLarge)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = selectedMode == "driving",
                            onClick = { 
                                selectedMode = "driving"
                                transportMode = "Roda 4"
                            },
                            label = { Text("Mobil") },
                            leadingIcon = { Icon(Icons.Default.DirectionsCar, contentDescription = null, modifier = Modifier.size(18.dp)) }
                        )
                        FilterChip(
                            selected = selectedMode == "two_wheeler",
                            onClick = { 
                                selectedMode = "two_wheeler"
                                transportMode = "Roda 2"
                            },
                            label = { Text("Motor") },
                            leadingIcon = { Icon(Icons.Default.TwoWheeler, contentDescription = null, modifier = Modifier.size(18.dp)) }
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text("Target Jarak: ${targetDistance.toInt()} km", style = MaterialTheme.typography.labelLarge)
                    Slider(
                        value = targetDistance,
                        onValueChange = { targetDistance = it },
                        valueRange = 0f..20f,
                        steps = 19
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Tampilkan Radius", style = MaterialTheme.typography.bodyMedium)
                        Switch(
                            checked = showRadius,
                            onCheckedChange = { showRadius = it }
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = { 
                                resetFields()
                                scope.launch { scaffoldState.bottomSheetState.partialExpand() }
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Gray)
                        ) {
                            Text("Reset / Clear")
                        }
                    }
                    
                    // Detailed Info Card (Visible when expanded)
                    if (distance.isNotEmpty() || duration.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(16.dp))
                        ElevatedCard(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Place, contentDescription = null, tint = Color.Blue)
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(text = "Jarak: $distance", style = MaterialTheme.typography.bodyLarge)
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.AccessTime, contentDescription = null, tint = Color.Red)
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(text = "Waktu Tempuh: $duration", style = MaterialTheme.typography.bodyLarge)
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = if (actualDistanceValue <= targetDistance * 1000) "Status: Dalam Target" else "Status: Diluar Target",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (actualDistanceValue <= targetDistance * 1000) Color(0xFF4CAF50) else Color(0xFFF44336)
                                )

                                destinationDetails?.let { details ->
                                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                                    Text(text = "Detail Tujuan:", style = MaterialTheme.typography.titleSmall)
                                    Text(text = details.name, style = MaterialTheme.typography.bodyLarge)
                                    details.formattedAddress?.let { 
                                        Text(text = it, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "⭐ ${details.rating ?: "No rating"}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = Color(0xFFFBC02D) // Gold color
                                        )
                                        details.userRatingsTotal?.let { count ->
                                            Text(
                                                text = " ($count reviews)",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = Color.Gray
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(32.dp)) // Extra padding for the bottom
                }
            }
        }
    ) { innerPadding ->
        Box(modifier = modifier
            .fillMaxSize()
            .padding(innerPadding)) {
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                properties = MapProperties(
                    mapStyleOptions = if (isDarkMode) MapStyleOptions(darkMapStyle) else null
                ),
                onMapClick = { latLng ->
                    destinationLatLng = latLng
                    destinationText = "${latLng.latitude},${latLng.longitude}"
                    destinationDetails = null // Clear old details
                    fetchRoute() // fetchRoute will now handle fetching details for these coordinates
                },
                onMapLongClick = { latLng ->
                    originLatLng = latLng
                    originText = "${latLng.latitude},${latLng.longitude}"
                    fetchRoute()
                }
            ) {
                originLatLng?.let {
                    Marker(
                        state = remember(it) { MarkerState(position = it) },
                        title = "Origin",
                        snippet = "Titik Keberangkatan",
                        icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE),
                        onClick = { _ ->
                            scope.launch {
                                snackbarHostState.showSnackbar("Marker Origin diklik: ${it.latitude}, ${it.longitude}")
                            }
                            false
                        }
                    )
                }
                
                destinationLatLng?.let {
                    Marker(
                        state = remember(it) { MarkerState(position = it) },
                        title = "Destination",
                        snippet = "Titik Tujuan",
                        icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE),
                        onClick = { _ ->
                            scope.launch {
                                snackbarHostState.showSnackbar("Marker Destination diklik: ${it.latitude}, ${it.longitude}")
                            }
                            false
                        }
                    )
                }

                originLatLng?.let {
                    Circle(
                        center = it,
                        radius = (targetDistance * 1000).toDouble(),
                        visible = showRadius,
                        fillColor = if (actualDistanceValue <= targetDistance * 1000) Color(0x224CAF50) else Color(0x22F44336),
                        strokeColor = if (actualDistanceValue <= targetDistance * 1000) Color(0xFF4CAF50) else Color(0xFFF44336),
                        strokeWidth = 2f
                    )
                }

                if (routePoints.isNotEmpty()) {
                    Polyline(
                        points = routePoints,
                        color = Color(0xFF00796B), // Teal color
                        width = 12f
                    )
                }
            }
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.align(Alignment.BottomCenter)
            )

            // Dark Mode Toggle Button
            SmallFloatingActionButton(
                onClick = onToggleDarkMode,
                modifier = Modifier
                    .padding(16.dp)
                    .align(Alignment.TopEnd),
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(
                    imageVector = if (isDarkMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                    contentDescription = "Toggle Dark Mode"
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MapScreenPreview() {
    MapScreen()
}
