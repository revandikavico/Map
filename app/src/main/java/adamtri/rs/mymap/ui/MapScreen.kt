package adamtri.rs.mymap.ui

import android.location.Geocoder
import android.util.Log
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    var transportMode by remember { mutableStateOf("Mobil") }
    var selectedMode by remember { mutableStateOf("driving") }
    
    // Target Distance States
    var targetDistance by remember { mutableStateOf(5f) }
    var actualDistanceValue by remember { mutableStateOf(0) }
    var showRadius by remember { mutableStateOf(true) }
    var isLoading by remember { mutableStateOf(false) }
    
    // Place Details State
    var destinationDetails by remember { mutableStateOf<PlaceDetails?>(null) }
    
    val scaffoldState = rememberBottomSheetScaffoldState()
    
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(originLatLng!!, 14f)
    }

    val customDarkMapStyle = """
        [
          { "elementType": "geometry", "stylers": [ { "color": "#1d2c4d" } ] },
          { "elementType": "labels.text.fill", "stylers": [ { "color": "#8ec3b9" } ] },
          { "elementType": "labels.text.stroke", "stylers": [ { "color": "#1a3646" } ] },
          { "featureType": "administrative.country", "elementType": "geometry.stroke", "stylers": [ { "color": "#4b6878" } ] },
          { "featureType": "poi", "elementType": "geometry", "stylers": [ { "color": "#283d6a" } ] },
          { "featureType": "poi", "elementType": "labels.text.fill", "stylers": [ { "color": "#6f9ba5" } ] },
          { "featureType": "road", "elementType": "geometry", "stylers": [ { "color": "#304a7d" } ] },
          { "featureType": "road", "elementType": "labels.text.fill", "stylers": [ { "color": "#98a5be" } ] },
          { "featureType": "water", "elementType": "geometry", "stylers": [ { "color": "#0e1626" } ] }
        ]
    """.trimIndent()

    val apiKey = "AIzaSyAMxzfxQjAg9Jr-WE5EtBpAE7xCXwz2B1Q"

    suspend fun resolveLocation(query: String): LatLng? = withContext(Dispatchers.IO) {
        try {
            val geocoder = Geocoder(context)
            @Suppress("DEPRECATION")
            val addresses = geocoder.getFromLocationName(query, 1)
            if (!addresses.isNullOrEmpty()) {
                val address = addresses[0]
                LatLng(address.latitude, address.longitude)
            } else null
        } catch (e: Exception) { null }
    }

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
            Log.e("RouteGo", "Details fetch error", e)
        }
    }

    fun calculateRoute() {
        scope.launch {
            isLoading = true
            destinationDetails = null
            try {
                val oLatLng = if (originText.contains(",")) {
                    val parts = originText.split(",")
                    if (parts.size == 2) LatLng(parts[0].trim().toDouble(), parts[1].trim().toDouble())
                    else resolveLocation(originText)
                } else resolveLocation(originText)

                var dLatLng: LatLng? = null
                if (destinationText.contains(",")) {
                    val parts = destinationText.split(",")
                    if (parts.size == 2) {
                        dLatLng = LatLng(parts[0].trim().toDouble(), parts[1].trim().toDouble())
                        fetchPlaceDetailsForLatLng(dLatLng)
                    }
                }

                if (dLatLng == null) {
                    val searchResponse = RetrofitClient.getPlacesApiService(context).searchPlace(destinationText, apiKey)
                    if (searchResponse.results.isNotEmpty()) {
                        val result = searchResponse.results[0]
                        dLatLng = LatLng(result.geometry.location.lat, result.geometry.location.lng)
                        val detailResponse = RetrofitClient.getPlacesApiService(context).getPlaceDetails(result.placeId, apiKey = apiKey)
                        if (detailResponse.status == "OK") destinationDetails = detailResponse.result
                    } else dLatLng = resolveLocation(destinationText)
                }

                if (oLatLng == null || dLatLng == null) {
                    snackbarHostState.showSnackbar("Location not found")
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
                    routePoints = PolyUtil.decode(route.overviewPolyline.points)
                    
                    if (route.legs.isNotEmpty()) {
                        distance = route.legs[0].distance.text
                        duration = route.legs[0].duration.text
                        actualDistanceValue = route.legs[0].distance.value
                    }

                    val bounds = LatLngBounds.builder().include(oLatLng).include(dLatLng).build()
                    cameraPositionState.animate(CameraUpdateFactory.newLatLngBounds(bounds, 200), 800)
                    
                    scaffoldState.bottomSheetState.partialExpand()
                } else {
                    snackbarHostState.showSnackbar("No route found")
                }
            } catch (e: Exception) {
                snackbarHostState.showSnackbar("Error: ${e.message}")
            } finally {
                isLoading = false
            }
        }
    }

    fun clearSearch() {
        originText = ""
        destinationText = ""
        routePoints = emptyList()
        distance = ""
        duration = ""
        actualDistanceValue = 0
        destinationDetails = null
    }

    LaunchedEffect(selectedMode) {
        if (originText.isNotEmpty() && destinationText.isNotEmpty()) calculateRoute()
    }

    BottomSheetScaffold(
        scaffoldState = scaffoldState,
        sheetPeekHeight = 120.dp,
        sheetShape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        sheetContent = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp)
            ) {
                if (distance.isNotEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "$duration",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "$distance • $transportMode",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        
                        StatusBadge(
                            isInTarget = actualDistanceValue <= targetDistance * 1000
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    destinationDetails?.let { details ->
                        PlaceInfoCard(details)
                    }
                } else {
                    Text(
                        "Pilih rute untuk melihat detail",
                        modifier = Modifier.padding(vertical = 16.dp),
                        style = MaterialTheme.typography.bodyLarge
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
                
                // Radius Controls
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Target Radius: ${targetDistance.toInt()} km", style = MaterialTheme.typography.labelLarge)
                        Slider(
                            value = targetDistance,
                            onValueChange = { targetDistance = it },
                            valueRange = 1f..20f,
                            steps = 18
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = showRadius, onCheckedChange = { showRadius = it })
                            Text("Tampilkan Visualisasi Radius", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(32.dp))
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
                    mapStyleOptions = if (isDarkMode) MapStyleOptions(customDarkMapStyle) else null,
                    isMyLocationEnabled = false
                ),
                uiSettings = MapUiSettings(zoomControlsEnabled = false),
                onMapClick = { latLng ->
                    destinationText = "${latLng.latitude},${latLng.longitude}"
                    calculateRoute()
                },
                onMapLongClick = { latLng ->
                    originText = "${latLng.latitude},${latLng.longitude}"
                    calculateRoute()
                }
            ) {
                originLatLng?.let {
                    Marker(
                        state = remember(it) { MarkerState(position = it) },
                        icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)
                    )
                    Circle(
                        center = it,
                        radius = (targetDistance * 1000).toDouble(),
                        visible = showRadius,
                        fillColor = if (actualDistanceValue <= targetDistance * 1000) Color(0x154CAF50) else Color(0x15F44336),
                        strokeColor = if (actualDistanceValue <= targetDistance * 1000) Color(0x804CAF50) else Color(0x80F44336),
                        strokeWidth = 2f
                    )
                }
                destinationLatLng?.let {
                    Marker(state = remember(it) { MarkerState(position = it) })
                }
                if (routePoints.isNotEmpty()) {
                    Polyline(points = routePoints, color = MaterialTheme.colorScheme.primary, width = 12f)
                }
            }

            // Floating Navigation Header
            FloatingSearchCard(
                originText = originText,
                destinationText = destinationText,
                onOriginChange = { originText = it },
                onDestinationChange = { destinationText = it },
                onSearch = { calculateRoute() },
                onClear = { clearSearch() },
                isLoading = isLoading,
                selectedMode = selectedMode,
                onModeChange = { mode, label -> 
                    selectedMode = mode
                    transportMode = label
                }
            )

            // Overlays
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 140.dp)
            )

            Column(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FloatingActionButton(
                    onClick = onToggleDarkMode,
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.primary,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(if (isDarkMode) Icons.Default.LightMode else Icons.Default.DarkMode, null)
                }
            }
        }
    }
}

@Composable
fun FloatingSearchCard(
    originText: String,
    destinationText: String,
    onOriginChange: (String) -> Unit,
    onDestinationChange: (String) -> Unit,
    onSearch: () -> Unit,
    onClear: () -> Unit,
    isLoading: Boolean,
    selectedMode: String,
    onModeChange: (String, String) -> Unit
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .statusBarsPadding(),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 8.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    SearchInputField(
                        value = originText,
                        onValueChange = onOriginChange,
                        placeholder = "Lokasi Awal",
                        icon = Icons.Outlined.MyLocation,
                        iconColor = Color(0xFF2196F3)
                    )
                    Spacer(Modifier.height(8.dp))
                    SearchInputField(
                        value = destinationText,
                        onValueChange = onDestinationChange,
                        placeholder = "Tujuan",
                        icon = Icons.Outlined.Place,
                        iconColor = Color(0xFFF44336)
                    )
                }
                
                Spacer(Modifier.width(8.dp))
                
                IconButton(
                    onClick = onSearch,
                    modifier = Modifier
                        .size(48.dp)
                        .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp)),
                    enabled = !isLoading
                ) {
                    if (isLoading) CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(24.dp))
                    else Icon(Icons.Default.Search, null, tint = Color.White)
                }
            }
            
            Spacer(Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TransportChip(
                        selected = selectedMode == "driving",
                        onClick = { onModeChange("driving", "Mobil") },
                        icon = Icons.Default.DirectionsCar,
                        label = "Mobil"
                    )
                    TransportChip(
                        selected = selectedMode == "two_wheeler",
                        onClick = { onModeChange("two_wheeler", "Motor") },
                        icon = Icons.Default.TwoWheeler,
                        label = "Motor"
                    )
                }
                
                TextButton(onClick = onClear) {
                    Text("Hapus", style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

@Composable
fun SearchInputField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconColor: Color
) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(placeholder, fontSize = 14.sp) },
        leadingIcon = { Icon(icon, null, tint = iconColor, modifier = Modifier.size(20.dp)) },
        modifier = Modifier.fillMaxWidth(),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent,
            disabledContainerColor = Color.Transparent,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent
        ),
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyMedium
    )
}

@Composable
fun TransportChip(
    selected: Boolean,
    onClick: () -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        leadingIcon = { Icon(icon, null, modifier = Modifier.size(16.dp)) },
        shape = RoundedCornerShape(8.dp)
    )
}

@Composable
fun StatusBadge(isInTarget: Boolean) {
    Surface(
        color = if (isInTarget) Color(0xFFE8F5E9) else Color(0xFFFFEBEE),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (isInTarget) Icons.Default.CheckCircle else Icons.Default.Warning,
                contentDescription = null,
                tint = if (isInTarget) Color(0xFF2E7D32) else Color(0xFFC62828),
                modifier = Modifier.size(14.dp)
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text = if (isInTarget) "Dalam Target" else "Diluar Target",
                style = MaterialTheme.typography.labelSmall,
                color = if (isInTarget) Color(0xFF2E7D32) else Color(0xFFC62828),
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun PlaceInfoCard(details: PlaceDetails) {
    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = details.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            details.formattedAddress?.let {
                Text(text = it, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Star, null, tint = Color(0xFFFFB300), modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text(
                    text = "${details.rating ?: "N/A"} (${details.userRatingsTotal ?: 0} ulasan)",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MapScreenPreview() {
    adamtri.rs.mymap.ui.theme.MyMapTheme {
        MapScreen()
    }
}
