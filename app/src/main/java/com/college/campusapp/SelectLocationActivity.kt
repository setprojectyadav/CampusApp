package com.college.campusapp

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.events.MapListener
import org.osmdroid.events.ScrollEvent
import org.osmdroid.events.ZoomEvent
import kotlinx.coroutines.launch

class SelectLocationActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize OSM configuration with valid User-Agent to avoid server 403 blocks
        val config = Configuration.getInstance()
        config.userAgentValue = packageName
        config.tileDownloadThreads = 8
        config.tileFileSystemThreads = 8
        config.tileDownloadMaxQueueSize = 60
        config.tileFileSystemMaxQueueSize = 60
        config.cacheMapTileCount = 40
        config.osmdroidBasePath = filesDir
        config.osmdroidTileCache = java.io.File(filesDir, "osmdroid/tiles")
        config.load(this, getSharedPreferences("osm_pref", MODE_PRIVATE))

        setContent {
            SelectLocationScreen(
                onBack = { finish() },
                onLocationConfirmed = { address ->
                    val data = Intent().apply {
                        putExtra("selected_address", address)
                    }
                    setResult(RESULT_OK, data)
                    finish()
                }
            )
        }
    }
}

@Composable
fun SelectLocationScreen(
    onBack: () -> Unit,
    onLocationConfirmed: (String) -> Unit
) {
    // Theme Colors
    val primaryMagenta = Color(0xFFFF0055)
    val textPrimary = Color(0xFF0F172A)
    val textSecondary = Color(0xFF64748B)

    // Location State (Default to New Delhi, India)
    var centerLat by remember { mutableStateOf(28.6139) }
    var centerLon by remember { mutableStateOf(77.2090) }
    var baseBuildingName by remember { mutableStateOf("Hostel Block C, North Wing") }
    var baseLandmark by remember { mutableStateOf("Main Academic Campus • Tech Corridor") }
    var isServiceable by remember { mutableStateOf(true) }

    // Quick Add Chip Input States
    var selectedRoomNo by remember { mutableStateOf("") }
    var selectedFloor by remember { mutableStateOf("") }
    var selectedLandmark by remember { mutableStateOf("") }

    // Dialog state for entering room/floor/landmark details
    var activeChipType by remember { mutableStateOf<String?>(null) }
    var tempInputValue by remember { mutableStateOf("") }

    val context = androidx.compose.ui.platform.LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var mapViewRef by remember { mutableStateOf<org.osmdroid.views.MapView?>(null) }
    var geocodeJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var isSearching by remember { mutableStateOf(false) }

    val localCampusIndex = remember {
        mapOf(
            "vit chennai" to Pair(12.8406, 80.1534),
            "vit vellore" to Pair(12.9692, 79.1559),
            "vit" to Pair(12.9692, 79.1559),
            "srm chennai" to Pair(12.8231, 80.0442),
            "srm ktr" to Pair(12.8231, 80.0442),
            "iit delhi" to Pair(28.5450, 77.1926),
            "iit bombay" to Pair(19.1334, 72.9133),
            "iit madras" to Pair(12.9915, 80.2337),
            "iit kharagpur" to Pair(22.3193, 87.3099),
            "iit kanpur" to Pair(26.5123, 80.2329),
            "iit roorkee" to Pair(29.8649, 77.8965),
            "iit guwahati" to Pair(26.1901, 91.6917),
            "iit hyderabad" to Pair(17.5947, 78.1234),
            "iisc bangalore" to Pair(13.0184, 77.5684),
            "bits pilani" to Pair(28.3639, 75.5870),
            "bits hyderabad" to Pair(17.5449, 78.5718),
            "bits goa" to Pair(15.3911, 73.8781),
            "amity noida" to Pair(28.5439, 77.3330),
            "manipal" to Pair(13.3525, 74.7928),
            "thapar" to Pair(30.3564, 76.3647),
            "dtu" to Pair(28.7501, 77.1177),
            "nsut" to Pair(28.6080, 77.0371),
            "iiit hyderabad" to Pair(17.4451, 78.3489),
            "anna university" to Pair(13.0102, 80.2354),
            "delhi" to Pair(28.6139, 77.2090),
            "new delhi" to Pair(28.6139, 77.2090),
            "mumbai" to Pair(19.0760, 72.8777),
            "bangalore" to Pair(12.9716, 77.5946),
            "bengaluru" to Pair(12.9716, 77.5946),
            "pune" to Pair(18.5204, 73.8567),
            "hyderabad" to Pair(17.3850, 78.4867),
            "chennai" to Pair(13.0827, 80.2707),
            "chandigarh" to Pair(30.7333, 76.7794),
            "jaipur" to Pair(26.9124, 75.7873),
            "kolkata" to Pair(22.5726, 88.3639),
            "hostel block a" to Pair(28.6139, 77.2090),
            "hostel block b" to Pair(28.6145, 77.2095),
            "central library" to Pair(28.6132, 77.2085),
            "main cafeteria" to Pair(28.6148, 77.2080)
        )
    }

    var activeCampusName by remember { mutableStateOf("VIT Vellore") }

    val activeCampuses = remember {
        listOf(
            Pair("VIT Vellore", Pair(12.9692, 79.1559)),
            Pair("VIT Chennai", Pair(12.8406, 80.1534))
        )
    }

    fun checkServiceability(lat: Double, lon: Double) {
        var minDistance = Double.MAX_VALUE
        var nearestCampus = "Campus Zone"

        for ((cName, coords) in activeCampuses) {
            val cLat = coords.first
            val cLon = coords.second
            val theta = lon - cLon
            var dist = Math.sin(Math.toRadians(lat)) * Math.sin(Math.toRadians(cLat)) +
                    Math.cos(Math.toRadians(lat)) * Math.cos(Math.toRadians(cLat)) *
                    Math.cos(Math.toRadians(theta))
            dist = Math.acos(dist)
            dist = Math.toDegrees(dist)
            dist = dist * 60 * 1.1515 * 1.609344 // km
            if (!dist.isNaN() && dist < minDistance) {
                minDistance = dist
                nearestCampus = cName
            }
        }
        
        isServiceable = minDistance <= 2.5
        activeCampusName = nearestCampus
        
        // Debounce reverse geocoding request so it runs cleanly when dragging ends or slows
        geocodeJob?.cancel()
        geocodeJob = coroutineScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            kotlinx.coroutines.delay(250) // 250ms debounce
            try {
                val geocoder = android.location.Geocoder(context, java.util.Locale.getDefault())
                val addresses = geocoder.getFromLocation(lat, lon, 1)
                if (!addresses.isNullOrEmpty()) {
                    val addr = addresses[0]
                    val feature = addr.featureName ?: addr.thoroughfare ?: addr.subLocality ?: ""
                    val subLoc = addr.subLocality ?: addr.locality ?: addr.adminArea ?: ""
                    val line = addr.getAddressLine(0) ?: ""

                    val titleStr = if (feature.isNotEmpty() && !feature.contains(subLoc, ignoreCase = true)) {
                        "$feature, $subLoc"
                    } else if (subLoc.isNotEmpty()) {
                        subLoc
                    } else if (line.isNotEmpty()) {
                        line.take(35)
                    } else {
                        nearestCampus
                    }

                    val landmarkStr = if (line.isNotEmpty()) {
                        line
                    } else {
                        "$nearestCampus • Main Tech Zone"
                    }

                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                        if (isServiceable) {
                            baseBuildingName = titleStr
                            baseLandmark = landmarkStr
                        } else {
                            baseBuildingName = "Off-Campus Area ($titleStr)"
                            baseLandmark = "Delivery distance exceeds 2.5km limit"
                        }
                    }
                } else {
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                        if (isServiceable) {
                            baseBuildingName = "$nearestCampus Zone"
                            baseLandmark = "$nearestCampus • Tech Corridor"
                        } else {
                            baseBuildingName = "Off-Campus Area (Outside Zone)"
                            baseLandmark = "Delivery distance exceeds 2.5km limit"
                        }
                    }
                }
            } catch (e: Exception) {
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    if (isServiceable) {
                        baseBuildingName = "$nearestCampus Zone"
                        baseLandmark = "$nearestCampus • Main Hub"
                    } else {
                        baseBuildingName = "Off-Campus Area"
                        baseLandmark = "Delivery distance exceeds 2.5km limit"
                    }
                }
            }
        }
    }

    fun performLocationSearch(query: String) {
        val cleanQuery = query.trim().lowercase(java.util.Locale.ROOT)
        if (cleanQuery.isBlank()) return

        // 1. Instant 0ms Local Dictionary Check (longest key match first)
        val localHit = localCampusIndex.entries
            .filter { entry ->
                val key = entry.key
                if (cleanQuery.length <= 3) {
                    cleanQuery == key || cleanQuery.split(" ").contains(key) || key.split(" ").contains(cleanQuery)
                } else {
                    cleanQuery.contains(key) || key.contains(cleanQuery)
                }
            }
            .maxByOrNull { it.key.length }?.value

        if (localHit != null) {
            centerLat = localHit.first
            centerLon = localHit.second
            val targetGeoPoint = org.osmdroid.util.GeoPoint(localHit.first, localHit.second)
            mapViewRef?.controller?.setCenter(targetGeoPoint)
            mapViewRef?.controller?.animateTo(targetGeoPoint)
            mapViewRef?.controller?.setZoom(16.0)
            mapViewRef?.invalidate()
            checkServiceability(localHit.first, localHit.second)
            return
        }

        isSearching = true
        coroutineScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val resultChannel = kotlinx.coroutines.channels.Channel<Pair<Double, Double>>(1)
            
            // Parallel Job A: Fast Photon Autocomplete API (< 50ms)
            launch {
                try {
                    val encodedQuery = java.net.URLEncoder.encode(query, "UTF-8")
                    val urlStr = "https://photon.komoot.io/api/?q=$encodedQuery&limit=1&bbox=68.1,6.4,97.4,37.1"
                    val url = java.net.URL(urlStr)
                    val conn = url.openConnection() as java.net.HttpURLConnection
                    conn.requestMethod = "GET"
                    conn.setRequestProperty("User-Agent", "CampusApp/1.0 (Android; Mobile)")
                    conn.connectTimeout = 1200
                    conn.readTimeout = 1200
                    if (conn.responseCode == 200) {
                        val stream = conn.inputStream.bufferedReader().use { it.readText() }
                        val jsonObj = com.google.gson.JsonParser.parseString(stream).asJsonObject
                        val features = jsonObj.getAsJsonArray("features")
                        if (features.size() > 0) {
                            val coords = features.get(0).asJsonObject.getAsJsonObject("geometry").getAsJsonArray("coordinates")
                            val lon = coords.get(0).asDouble
                            val lat = coords.get(1).asDouble
                            if (lat in 6.4..37.1 && lon in 68.1..97.4) {
                                resultChannel.trySend(Pair(lat, lon))
                            }
                        }
                    }
                } catch (e: Exception) {}
            }

            // Parallel Job B: Nominatim OpenStreetMap API (< 150ms)
            launch {
                try {
                    val encodedQuery = java.net.URLEncoder.encode(query, "UTF-8")
                    val urlStr = "https://nominatim.openstreetmap.org/search?format=json&q=$encodedQuery&limit=1&countrycodes=in"
                    val url = java.net.URL(urlStr)
                    val conn = url.openConnection() as java.net.HttpURLConnection
                    conn.requestMethod = "GET"
                    conn.setRequestProperty("User-Agent", "CampusApp/1.0 (Android; Mobile)")
                    conn.connectTimeout = 1500
                    conn.readTimeout = 1500
                    if (conn.responseCode == 200) {
                        val stream = conn.inputStream.bufferedReader().use { it.readText() }
                        val jsonArray = com.google.gson.JsonParser.parseString(stream).asJsonArray
                        if (jsonArray.size() > 0) {
                            val obj = jsonArray.get(0).asJsonObject
                            val lat = obj.get("lat").asString.toDouble()
                            val lon = obj.get("lon").asString.toDouble()
                            if (lat in 6.4..37.1 && lon in 68.1..97.4) {
                                resultChannel.trySend(Pair(lat, lon))
                            }
                        }
                    }
                } catch (e: Exception) {}
            }

            // Parallel Job C: Native Geocoder Fallback
            launch {
                try {
                    val geocoder = android.location.Geocoder(context, java.util.Locale.getDefault())
                    val indiaQuery = if (!query.lowercase(java.util.Locale.ROOT).contains("india")) "$query, India" else query
                    val results = geocoder.getFromLocationName(indiaQuery, 1)
                    if (!results.isNullOrEmpty()) {
                        val res = results[0]
                        if ((res.latitude in 6.4..37.1 && res.longitude in 68.1..97.4) || res.countryCode.equals("in", ignoreCase = true)) {
                            resultChannel.trySend(Pair(res.latitude, res.longitude))
                        }
                    }
                } catch (e: Exception) {}
            }

            // Await the winner of the 3 parallel jobs
            val winner = kotlinx.coroutines.withTimeoutOrNull(2000) {
                resultChannel.receiveCatching().getOrNull()
            }

            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                isSearching = false
                if (winner != null) {
                    centerLat = winner.first
                    centerLon = winner.second
                    val targetGeoPoint = org.osmdroid.util.GeoPoint(winner.first, winner.second)
                    mapViewRef?.controller?.setCenter(targetGeoPoint)
                    mapViewRef?.controller?.animateTo(targetGeoPoint)
                    mapViewRef?.controller?.setZoom(16.0)
                    mapViewRef?.invalidate()
                    checkServiceability(winner.first, winner.second)
                } else {
                    Toast.makeText(context, "Location not found in India. Try entering campus or city name.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // Live Instant Autocomplete Search as user types (300ms debounce)
    LaunchedEffect(searchQuery) {
        val q = searchQuery.trim()
        if (q.length >= 2) {
            kotlinx.coroutines.delay(300)
            performLocationSearch(q)
        }
    }





    // Build final full address
    val fullAddress = remember(baseBuildingName, selectedRoomNo, selectedFloor, selectedLandmark) {
        val parts = mutableListOf<String>()
        if (selectedRoomNo.isNotEmpty()) parts.add("Room $selectedRoomNo")
        if (selectedFloor.isNotEmpty()) parts.add(selectedFloor)
        parts.add(baseBuildingName)
        if (selectedLandmark.isNotEmpty()) parts.add("Near $selectedLandmark")
        parts.joinToString(", ")
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF1F5F9))
    ) {
        // Layer A: Official Google Maps View with 3D Vector Buildings & Smooth Vector Engine

        // Layer A: Campus OpenStreetMap HD View (100% Stable, Fastly Multi-CDN Engine)
        androidx.compose.ui.viewinterop.AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                org.osmdroid.views.MapView(ctx).apply {
                    mapViewRef = this
                    setTileSource(TileSourceFactory.MAPNIK)
                    setLayerType(android.view.View.LAYER_TYPE_HARDWARE, null)
                    setBuiltInZoomControls(false)
                    setMultiTouchControls(true)
                    isTilesScaledToDpi = true
                    // GTA-style Dynamic Tile Streaming: loads only tiles visible on screen
                    setHorizontalMapRepetitionEnabled(false)
                    setVerticalMapRepetitionEnabled(false)
                    setMinZoomLevel(4.0)
                    setMaxZoomLevel(19.0)
                    controller.setZoom(16.0)
                    
                    // Restrict Map Viewport strictly to India Bounding Box
                    val indiaBox = org.osmdroid.util.BoundingBox(37.5, 97.4, 6.4, 68.1)
                    setScrollableAreaLimitDouble(indiaBox)
                    
                    // Auto-locate user's actual GPS position if available inside India
                    val locationManager = context.getSystemService(android.content.Context.LOCATION_SERVICE) as? android.location.LocationManager
                    var userGeoPoint: org.osmdroid.util.GeoPoint? = null
                    try {
                        if (locationManager != null) {
                            val provider = if (locationManager.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER)) {
                                android.location.LocationManager.GPS_PROVIDER
                            } else if (locationManager.isProviderEnabled(android.location.LocationManager.NETWORK_PROVIDER)) {
                                android.location.LocationManager.NETWORK_PROVIDER
                            } else null

                            if (provider != null) {
                                val lastKnown = locationManager.getLastKnownLocation(provider)
                                if (lastKnown != null && lastKnown.latitude in 6.4..37.5 && lastKnown.longitude in 68.1..97.4) {
                                    userGeoPoint = org.osmdroid.util.GeoPoint(lastKnown.latitude, lastKnown.longitude)
                                }
                            }
                        }
                    } catch (e: Exception) {}

                    val initialPoint = userGeoPoint ?: org.osmdroid.util.GeoPoint(28.6139, 77.2090)
                    controller.setCenter(initialPoint)
                    checkServiceability(initialPoint.latitude, initialPoint.longitude)
                    
                    addMapListener(object : org.osmdroid.events.MapListener {
                        override fun onScroll(event: org.osmdroid.events.ScrollEvent?): Boolean {
                            val center = mapCenter
                            centerLat = center.latitude
                            centerLon = center.longitude
                            checkServiceability(center.latitude, center.longitude)
                            return true
                        }
                        override fun onZoom(event: org.osmdroid.events.ZoomEvent?): Boolean = true
                    })
                    
                    // Trigger tile fetching engine
                    onResume()
                }
            },
            update = { map ->
                map.onResume()
            }
        )

        // Floating "Locate Me / Recenter GPS" Crosshair Target Button (Floating right side above bottom sheet)
        FloatingActionButton(
            onClick = {
                val lm = context.getSystemService(android.content.Context.LOCATION_SERVICE) as? android.location.LocationManager
                val gpsEnabled = lm?.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER) == true ||
                                lm?.isProviderEnabled(android.location.LocationManager.NETWORK_PROVIDER) == true
                if (lm != null && gpsEnabled) {
                    try {
                        val provider = if (lm.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER)) {
                            android.location.LocationManager.GPS_PROVIDER
                        } else android.location.LocationManager.NETWORK_PROVIDER
                        val loc = lm.getLastKnownLocation(provider)
                        if (loc != null) {
                            val userGeoPoint = org.osmdroid.util.GeoPoint(loc.latitude, loc.longitude)
                            mapViewRef?.controller?.animateTo(userGeoPoint)
                            checkServiceability(loc.latitude, loc.longitude)
                        } else {
                            Toast.makeText(context, "Detecting GPS location...", Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {
                        Toast.makeText(context, "GPS permission required", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(context, "Please turn on device GPS", Toast.LENGTH_SHORT).show()
                    try {
                        context.startActivity(Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                    } catch (e: Exception) {}
                }
            },
            shape = CircleShape,
            containerColor = Color.White,
            contentColor = Color(0xFF0F172A),
            elevation = FloatingActionButtonDefaults.elevation(6.dp),
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 16.dp)
                .offset(y = 120.dp)
                .size(46.dp)
        ) {
            Canvas(modifier = Modifier.size(20.dp)) {
                val c = center
                val r = 7.dp.toPx()
                drawCircle(color = Color(0xFF0F172A), radius = r, center = c, style = Stroke(width = 1.8f.dp.toPx()))
                drawCircle(color = Color(0xFFFF0055), radius = 2.5.dp.toPx(), center = c)
                drawLine(color = Color(0xFF0F172A), start = Offset(c.x, 0f), end = Offset(c.x, 3.5.dp.toPx()), strokeWidth = 1.8f.dp.toPx())
                drawLine(color = Color(0xFF0F172A), start = Offset(c.x, size.height - 3.5.dp.toPx()), end = Offset(c.x, size.height), strokeWidth = 1.8f.dp.toPx())
                drawLine(color = Color(0xFF0F172A), start = Offset(0f, c.y), end = Offset(3.5.dp.toPx(), c.y), strokeWidth = 1.8f.dp.toPx())
                drawLine(color = Color(0xFF0F172A), start = Offset(size.width - 3.5.dp.toPx(), c.y), end = Offset(size.width, c.y), strokeWidth = 1.8f.dp.toPx())
            }
        }

        // Layer B: Top Navigation & Delivery Zone Pill Header
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .align(Alignment.TopCenter),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Circular Back Button Arrow
                Surface(
                    onClick = onBack,
                    shape = CircleShape,
                    color = Color.White,
                    shadowElevation = 4.dp,
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Canvas(modifier = Modifier.size(16.dp)) {
                            val path = Path().apply {
                                moveTo(10.dp.toPx(), 2.dp.toPx())
                                lineTo(4.dp.toPx(), 8.dp.toPx())
                                lineTo(10.dp.toPx(), 14.dp.toPx())
                            }
                            drawPath(
                                path = path,
                                color = Color(0xFF0F172A),
                                style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Interactive Live Search Bar
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color.White,
                    shadowElevation = 3.dp,
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isSearching) "⏳" else "🔍",
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        androidx.compose.foundation.text.BasicTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            singleLine = true,
                            textStyle = androidx.compose.ui.text.TextStyle(
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = textPrimary
                            ),
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                imeAction = androidx.compose.ui.text.input.ImeAction.Search
                            ),
                            keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                                onSearch = {
                                    performLocationSearch(searchQuery)
                                }
                            ),
                            modifier = Modifier.weight(1f),
                            decorationBox = { innerTextField ->
                                if (searchQuery.isEmpty()) {
                                    Text(
                                        text = baseBuildingName,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = textPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                innerTextField()
                            }
                        )
                        if (searchQuery.isNotEmpty()) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Search",
                                    fontSize = 11.sp,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(primaryMagenta)
                                        .clickable { performLocationSearch(searchQuery) }
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "✕",
                                    fontSize = 14.sp,
                                    color = textSecondary,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.clickable { searchQuery = "" }
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Active Campus Quick Selector Pills
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                activeCampuses.forEach { (cName, coords) ->
                    val isSelected = isServiceable && activeCampusName == cName
                    Surface(
                        onClick = {
                            centerLat = coords.first
                            centerLon = coords.second
                            val pt = org.osmdroid.util.GeoPoint(coords.first, coords.second)
                            mapViewRef?.controller?.setCenter(pt)
                            mapViewRef?.controller?.animateTo(pt)
                            mapViewRef?.controller?.setZoom(16.0)
                            mapViewRef?.invalidate()
                            checkServiceability(coords.first, coords.second)
                        },
                        shape = RoundedCornerShape(14.dp),
                        color = if (isSelected) primaryMagenta else Color.White,
                        shadowElevation = 2.dp,
                        border = if (isSelected) null else BorderStroke(1.dp, Color(0xFFE2E8F0))
                    ) {
                        Text(
                            text = "🎓 $cName",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) Color.White else textPrimary,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Delivery Zone Pill Floating Below Header
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color(0xFF1E293B),
                shadowElevation = 4.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .background(
                                color = if (isServiceable) Color(0xFF10B981) else Color(0xFFEF4444),
                                shape = CircleShape
                            )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isServiceable) "🟢 Inside $activeCampusName Zone • Instant Delivery Active" else "🔴 Outside Launch Zone • Delivery Unavailable",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        // Layer C: Center Delivery Marker (Precision Needle Pin)
        CenterMapPrecisionPin(
            modifier = Modifier
                .align(Alignment.Center)
                .offset(y = (-36).dp)
        )

        // Layer D: Bottom Sheet Delivery Details Card
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter),
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            color = Color.White,
            shadowElevation = 16.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(top = 10.dp, bottom = 20.dp)
            ) {
                // Draggable Handle Pill
                Box(
                    modifier = Modifier
                        .width(40.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color(0xFFCBD5E1))
                        .align(Alignment.CenterHorizontally)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Header Row with Mini Pink Pin & Subtitle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFFCE7F3)),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.size(18.dp)) {
                            val path = Path().apply {
                                moveTo(size.width / 2, size.height)
                                cubicTo(
                                    size.width * 0.1f, size.height * 0.6f,
                                    0f, size.height * 0.4f,
                                    size.width / 2, 0f
                                )
                                cubicTo(
                                    size.width, size.height * 0.4f,
                                    size.width * 0.9f, size.height * 0.6f,
                                    size.width / 2, size.height
                                )
                                close()
                            }
                            drawPath(path, color = primaryMagenta)
                            drawCircle(color = Color.White, radius = 2.5.dp.toPx(), center = Offset(size.width / 2, size.height * 0.35f))
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = baseBuildingName,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = baseLandmark,
                            fontSize = 12.sp,
                            color = textSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Info Alert Card
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFFFEF3C7),
                    border = BorderStroke(1.dp, Color(0xFFFDE68A))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(18.dp)
                                .background(Color(0xFF92400E), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "i",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Add hostel room number for hassle-free doorstep delivery",
                            fontSize = 12.sp,
                            color = Color(0xFF92400E),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Horizontal Quick Add Chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    QuickAddChip(
                        label = if (selectedRoomNo.isEmpty()) "+ Room No." else "Room: $selectedRoomNo",
                        isSelected = selectedRoomNo.isNotEmpty(),
                        onClick = {
                            tempInputValue = selectedRoomNo
                            activeChipType = "Room No."
                        }
                    )
                    QuickAddChip(
                        label = if (selectedFloor.isEmpty()) "+ Floor / Wing" else selectedFloor,
                        isSelected = selectedFloor.isNotEmpty(),
                        onClick = {
                            tempInputValue = selectedFloor
                            activeChipType = "Floor / Wing"
                        }
                    )
                    QuickAddChip(
                        label = if (selectedLandmark.isEmpty()) "+ Landmark" else "Near: $selectedLandmark",
                        isSelected = selectedLandmark.isNotEmpty(),
                        onClick = {
                            tempInputValue = selectedLandmark
                            activeChipType = "Landmark"
                        }
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Primary CTA Button / Vote for College Button
                if (isServiceable) {
                    Button(
                        onClick = {
                            onLocationConfirmed(fullAddress)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = primaryMagenta),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .shadow(8.dp, RoundedCornerShape(16.dp), ambientColor = primaryMagenta, spotColor = primaryMagenta)
                    ) {
                        Text(
                            text = "Confirm Location & Proceed →",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else {
                    var hasVoted by remember { mutableStateOf(false) }
                    Button(
                        onClick = {
                            CampusVoteRepository.recordVote(context, baseBuildingName)
                            hasVoted = true
                            Toast.makeText(context, "🎉 Vote recorded! We will launch CampusApp at $baseBuildingName soon.", Toast.LENGTH_LONG).show()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (hasVoted) Color(0xFF10B981) else Color(0xFF6366F1)
                        ),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                    ) {
                        Text(
                            text = if (hasVoted) "✓ Vote Registered for $baseBuildingName" else "🚀 Vote to Launch CampusApp at My College",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }

    // Quick Add Input Dialog
    if (activeChipType != null) {
        AlertDialog(
            onDismissRequest = { activeChipType = null },
            title = {
                Text(
                    text = "Add ${activeChipType}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = textPrimary
                )
            },
            text = {
                OutlinedTextField(
                    value = tempInputValue,
                    onValueChange = { tempInputValue = it },
                    placeholder = { Text("e.g. ${if (activeChipType == "Room No.") "302" else if (activeChipType == "Floor / Wing") "3rd Floor" else "Near Mess Gate"}") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = primaryMagenta,
                        unfocusedBorderColor = Color(0xFFE2E8F0)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        when (activeChipType) {
                            "Room No." -> selectedRoomNo = tempInputValue.trim()
                            "Floor / Wing" -> selectedFloor = tempInputValue.trim()
                            "Landmark" -> selectedLandmark = tempInputValue.trim()
                        }
                        activeChipType = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = primaryMagenta),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Save", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { activeChipType = null }) {
                    Text("Cancel", color = textSecondary)
                }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(16.dp)
        )
    }
}

// Quick Add Pill Chip Composable
@Composable
fun QuickAddChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = if (isSelected) Color(0xFFFFF1F2) else Color.White,
        border = BorderStroke(1.dp, if (isSelected) Color(0xFFFF0055) else Color(0xFFE2E8F0))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) Color(0xFFFF0055) else Color(0xFF334155)
            )
        }
    }
}

// Center Uber-Style Precision Pin Composable
@Composable
fun CenterMapPrecisionPin(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 1. Floating Delivery Tooltip
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = Color(0xFF0F172A),
            shadowElevation = 6.dp
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .background(Color(0xFFFF0055), CircleShape)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Delivery point",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Downward Triangle Pointer
        Canvas(modifier = Modifier.size(8.dp, 4.dp)) {
            val path = Path().apply {
                moveTo(0f, 0f)
                lineTo(size.width / 2, size.height)
                lineTo(size.width, 0f)
                close()
            }
            drawPath(path, color = Color(0xFF0F172A))
        }

        Spacer(modifier = Modifier.height(2.dp))

        // 2. Circular Pin Head + Precision Needle + Shadow
        Canvas(modifier = Modifier.size(24.dp, 42.dp)) {
            val centerX = size.width / 2f
            val headRadius = 11.dp.toPx()
            val headCenterY = headRadius

            // Ground Contact Shadow
            drawOval(
                color = Color(0x33000000),
                topLeft = Offset(centerX - 7.dp.toPx(), size.height - 3.dp.toPx()),
                size = Size(14.dp.toPx(), 3.dp.toPx())
            )

            // Slender Needle Stem
            drawLine(
                color = Color(0xFF334155),
                start = Offset(centerX, headCenterY + headRadius - 2.dp.toPx()),
                end = Offset(centerX, size.height - 2.dp.toPx()),
                strokeWidth = 2.dp.toPx(),
                cap = StrokeCap.Round
            )

            // Outer Magenta Ring
            drawCircle(
                color = Color(0xFFFF0055),
                radius = headRadius,
                center = Offset(centerX, headCenterY)
            )

            // Inner White Border Ring
            drawCircle(
                color = Color.White,
                radius = headRadius * 0.72f,
                center = Offset(centerX, headCenterY),
                style = Stroke(width = 2.dp.toPx())
            )

            // Center White Hole
            drawCircle(
                color = Color.White,
                radius = headRadius * 0.38f,
                center = Offset(centerX, headCenterY)
            )
        }
    }
}

