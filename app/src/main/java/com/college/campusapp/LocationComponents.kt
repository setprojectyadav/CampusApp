package com.college.campusapp
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.college.campusapp.security.EncryptionManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts

import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.path
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Delete

@Composable
fun LocationSelectionDialog(
    currentLocation: String,
    onLocationSelected: (String) -> Unit,
    onDismiss: () -> Unit,
    context: android.content.Context
) {
    var showSeeAll by remember { mutableStateOf(false) }
    val primaryColor = Color(0xFFE11D48)
    val textPrimary = Color(0xFF1E293B)
    val textSecondary = Color(0xFF64748B)

    var isManageMode by remember { mutableStateOf(false) }
    val selectedForDeletion = remember { mutableStateListOf<SavedAddress>() }
    var addressToDelete by remember { mutableStateOf<SavedAddress?>(null) }

    val savedAddresses = remember {
        val json = EncryptionManager.getString(context, "saved_address_list")
        val list: List<SavedAddress> = if (!json.isNullOrEmpty()) {
            try {
                val type = object : TypeToken<List<SavedAddress>>() {}.type
                Gson().fromJson(json, type)
            } catch (e: Exception) {
                emptyList()
            }
        } else {
            emptyList()
        }
        mutableStateListOf(*list.toTypedArray())
    }

    fun saveAddressesToDisk() {
        val json = Gson().toJson(savedAddresses.toList())
        EncryptionManager.saveString(context, "saved_address_list", json)
    }

    val mapLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val address = result.data?.getStringExtra("selected_address")
            if (address != null) {
                val newAddr = SavedAddress("Added Address", address, "", "📍")
                savedAddresses.add(0, newAddr)
                saveAddressesToDisk()
                onLocationSelected(address)
            }
        }
    }

    var isGpsOn by remember { mutableStateOf(false) }

    // Automatically check and refresh GPS status when app resumes
    val lifecycleOwner = androidx.compose.ui.platform.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                try {
                    val lm = context.getSystemService(Context.LOCATION_SERVICE) as? android.location.LocationManager
                    val gpsEnabled = lm?.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER) == true
                    isGpsOn = gpsEnabled
                    
                    val hasFineLoc = androidx.core.content.ContextCompat.checkSelfPermission(
                        context, android.Manifest.permission.ACCESS_FINE_LOCATION
                    ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                    val hasCoarseLoc = androidx.core.content.ContextCompat.checkSelfPermission(
                        context, android.Manifest.permission.ACCESS_COARSE_LOCATION
                    ) == android.content.pm.PackageManager.PERMISSION_GRANTED

                    if (gpsEnabled && (hasFineLoc || hasCoarseLoc) && lm != null) {
                        val provider = if (lm.isProviderEnabled(android.location.LocationManager.NETWORK_PROVIDER)) {
                            android.location.LocationManager.NETWORK_PROVIDER
                        } else {
                            android.location.LocationManager.GPS_PROVIDER
                        }
                        
                        val lastKnown = try { lm.getLastKnownLocation(provider) } catch (e: Exception) { null }
                        if (lastKnown != null) {
                            val localAddress = getShortLoc(context, lastKnown.latitude, lastKnown.longitude)
                            if (savedAddresses.none { it.address == localAddress }) {
                                savedAddresses.add(0, SavedAddress("Current Location", localAddress, "", "📍"))
                                saveAddressesToDisk()
                            }
                        }
                    }
                } catch (e: Exception) {
                    // Ignore location check exception
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    var searchQuery by remember { mutableStateOf("") }
    var voteCollegeText by remember { mutableStateOf("") }
    var hasVotedInDialog by remember { mutableStateOf(false) }

    // Hostel Picker Modal State
    var selectedCampusForHostel by remember { mutableStateOf<String?>(null) }
    var selectedHostelBlock by remember { mutableStateOf("Block Q") }
    var roomNumberInput by remember { mutableStateOf("") }

    val activeCampusesList = remember {
        listOf(
            Triple("VIT Vellore", "Katpadi, Vellore, Tamil Nadu", "🎓"),
            Triple("VIT Chennai", "Vandalur-Kelambakkam Rd, Chennai", "🏛️")
        )
    }

    val hostelBlockOptions = remember {
        listOf(
            "Block A", "Block B", "Block C", "Block D", "Block E",
            "Block F", "Block G", "Block H", "Block J", "Block K",
            "Block L", "Block M", "Block N", "Block P", "Block Q",
            "Block R", "Block S", "Block T", "Ladies Hostel A",
            "Ladies Hostel B", "Ladies Hostel C", "Ladies Hostel D", "Ladies Hostel E"
        )
    }

    val campusSuggestions = remember {
        listOf(
            SavedAddress("Hostel Block A", "Hostel Block A, Girls Residence Campus area", "", "🏢"),
            SavedAddress("Hostel Block B", "Hostel Block B, Boys Residence Hostel block", "", "🏢"),
            SavedAddress("Central Library", "Main University Library, Periyar Building ground floor", "", "📚"),
            SavedAddress("Main Cafeteria", "Student Food Court & Dining Hall, Block E", "", "🍔"),
            SavedAddress("Academic Block C", "Academic Block C, Department of Computer Science", "", "🏫"),
            SavedAddress("Sports Complex", "University Cricket Stadium and Indoor Gym", "", "🏀"),
            SavedAddress("Student Activity Center", "SAC Building, next to the Main Gate Ground", "", "🏛️")
        )
    }

    val displayAddresses = if (showSeeAll) {
        savedAddresses
    } else {
        savedAddresses.take(2)
    }

    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF8FAFC))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White)
            ) {
                // Header Bar: Back Arrow (Left), Title (Center), Close Button (Right)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFF8FAFC))
                            .border(1.dp, Color(0xFFF1F5F9), CircleShape)
                            .clickable {
                                if (showSeeAll) {
                                    showSeeAll = false
                                } else {
                                    onDismiss()
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        androidx.compose.foundation.Canvas(modifier = Modifier.size(16.dp)) {
                            val path = Path().apply {
                                moveTo(10.dp.toPx(), 2.dp.toPx())
                                lineTo(4.dp.toPx(), 8.dp.toPx())
                                lineTo(10.dp.toPx(), 14.dp.toPx())
                                moveTo(4.dp.toPx(), 8.dp.toPx())
                                lineTo(15.dp.toPx(), 8.dp.toPx())
                            }
                            drawPath(
                                path = path,
                                color = Color(0xFF0F172A),
                                style = Stroke(width = 2.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round, join = androidx.compose.ui.graphics.StrokeJoin.Round)
                            )
                        }
                    }

                    Text(
                        text = "Select Delivery Location",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )

                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFF8FAFC))
                            .border(1.dp, Color(0xFFF1F5F9), CircleShape)
                            .clickable { onDismiss() },
                        contentAlignment = Alignment.Center
                    ) {
                        androidx.compose.foundation.Canvas(modifier = Modifier.size(14.dp)) {
                            drawLine(color = Color(0xFF0F172A), start = androidx.compose.ui.geometry.Offset(2.dp.toPx(), 2.dp.toPx()), end = androidx.compose.ui.geometry.Offset(12.dp.toPx(), 12.dp.toPx()), strokeWidth = 2.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round)
                            drawLine(color = Color(0xFF0F172A), start = androidx.compose.ui.geometry.Offset(12.dp.toPx(), 2.dp.toPx()), end = androidx.compose.ui.geometry.Offset(2.dp.toPx(), 12.dp.toPx()), strokeWidth = 2.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round)
                        }
                    }
                }

                Divider(color = Color(0xFFF1F5F9), thickness = 1.dp)

                // 🔍 Search Bar (Zepto Style)
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search campus, hostel, or city...", fontSize = 13.5.sp, color = Color(0xFF94A3B8)) },
                        singleLine = true,
                        leadingIcon = {
                            Box(
                                modifier = Modifier
                                    .padding(start = 6.dp)
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFFFF1F2)),
                                contentAlignment = Alignment.Center
                            ) {
                                androidx.compose.foundation.Canvas(modifier = Modifier.size(15.dp)) {
                                    val r = 4.5.dp.toPx()
                                    val c = androidx.compose.ui.geometry.Offset(5.5.dp.toPx(), 5.5.dp.toPx())
                                    val iconColor = Color(0xFFFF0055)
                                    // Circle lens
                                    drawCircle(
                                        color = iconColor,
                                        radius = r,
                                        center = c,
                                        style = Stroke(width = 2.dp.toPx())
                                    )
                                    // Handle stem
                                    drawLine(
                                        color = iconColor,
                                        start = androidx.compose.ui.geometry.Offset(c.x + r * 0.7f, c.y + r * 0.7f),
                                        end = androidx.compose.ui.geometry.Offset(13.5.dp.toPx(), 13.5.dp.toPx()),
                                        strokeWidth = 2.dp.toPx(),
                                        cap = androidx.compose.ui.graphics.StrokeCap.Round
                                    )
                                }
                            }
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                Text(
                                    "✕",
                                    fontSize = 14.sp,
                                    color = Color(0xFF64748B),
                                    modifier = Modifier.clickable { searchQuery = "" }
                                )
                            }
                        },
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFFF8FAFC),
                            unfocusedContainerColor = Color(0xFFF8FAFC),
                            focusedBorderColor = Color(0xFFFF0055),
                            unfocusedBorderColor = Color(0xFFE2E8F0)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // 🎓 Sleek Horizontal Pill Chips for Active Campuses (Non-messy design)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Active:",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF64748B)
                        )
                        activeCampusesList.forEach { (cName, cAddr, iconStr) ->
                            val isSelected = currentLocation.contains(cName)
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(if (isSelected) Color(0xFFFF0055) else Color(0xFFF1F5F9))
                                    .clickable {
                                        selectedCampusForHostel = cName
                                    }
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(iconStr, fontSize = 12.sp)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = cName,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color.White else Color(0xFF0F172A)
                                )
                            }
                        }
                    }
                }

                Divider(color = Color(0xFFF1F5F9), thickness = 1.dp)

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                ) {
                    // Check if Search Query is active
                    if (searchQuery.isNotBlank()) {
                        val trimmedQuery = searchQuery.trim()
                        val isQueryActiveCampus = activeCampusesList.any { (name, addr, _) ->
                            name.contains(trimmedQuery, ignoreCase = true) || addr.contains(trimmedQuery, ignoreCase = true)
                        }

                        if (isQueryActiveCampus) {
                            Text(
                                text = "SEARCH RESULTS",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = Color(0xFF64748B),
                                letterSpacing = 0.8.sp
                            )
                            Spacer(modifier = Modifier.height(10.dp))

                            activeCampusesList.filter { (name, addr, _) ->
                                name.contains(trimmedQuery, ignoreCase = true) || addr.contains(trimmedQuery, ignoreCase = true)
                            }.forEach { (cName, cAddr, iconStr) ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            selectedCampusForHostel = cName
                                        },
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color.White),
                                    border = BorderStroke(1.dp, Color(0xFF10B981))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                            Text(iconStr, fontSize = 20.sp)
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Column {
                                                Text(cName, fontWeight = FontWeight.Bold, fontSize = 14.5.sp, color = Color(0xFF0F172A))
                                                Text(cAddr, fontSize = 11.5.sp, color = Color(0xFF64748B))
                                            }
                                        }
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(Color(0xFF10B981).copy(alpha = 0.1f))
                                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            Text("🟢 Deliverable", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF10B981))
                                        }
                                    }
                                }
                            }
                        } else {
                            // 🚫 Zepto-style Unserviceable Area Card + One-tap Vote Button
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2)),
                                border = BorderStroke(1.dp, Color(0xFFAEC26D0).copy(alpha = 0.3f))
                            ) {
                                Column(
                                    modifier = Modifier.padding(18.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text("😔", fontSize = 32.sp)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "We aren't delivering to '$trimmedQuery' yet!",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = Color(0xFF991B1B),
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "CampusApp currently delivers strictly inside launched university campuses.",
                                        fontSize = 12.sp,
                                        color = Color(0xFF7F1D1D),
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(modifier = Modifier.height(14.dp))

                                    Button(
                                        onClick = {
                                            CampusVoteRepository.recordVote(context, trimmedQuery)
                                            Toast.makeText(context, "🎉 Vote registered for '$trimmedQuery'! We'll launch here next!", Toast.LENGTH_SHORT).show()
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1)),
                                        shape = RoundedCornerShape(14.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("Vote to launch '$trimmedQuery' 🚀", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))
                    }

                    // Default View when query is empty: GPS & Saved Addresses & Sharing
                    if (searchQuery.isBlank()) {
                        // "Your device location is off" Banner Card when GPS is turned off
                        if (!isGpsOn) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                border = BorderStroke(1.dp, Color(0xFFF1F5F9))
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 22.dp, horizontal = 20.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Glossy3DLocationPin(
                                        modifier = Modifier
                                            .width(58.dp)
                                            .height(74.dp)
                                    )

                                    Spacer(modifier = Modifier.height(14.dp))

                                    Text(
                                        text = "Your device location is off",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        color = Color(0xFF0F172A)
                                    )

                                    Spacer(modifier = Modifier.height(4.dp))

                                    Text(
                                        text = "Turn on location for faster doorstep delivery across campus",
                                        fontSize = 12.sp,
                                        color = Color(0xFF64748B),
                                        textAlign = TextAlign.Center,
                                        lineHeight = 16.sp,
                                        modifier = Modifier.padding(horizontal = 8.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))
                        }

                        // "Use Current Location" Row Card
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = BorderStroke(1.dp, Color(0xFFF1F5F9))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        val lm = context.getSystemService(Context.LOCATION_SERVICE) as? android.location.LocationManager
                                        val gpsEnabled = lm?.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER) == true ||
                                                        lm?.isProviderEnabled(android.location.LocationManager.NETWORK_PROVIDER) == true
                                        if (lm != null && gpsEnabled) {
                                            try {
                                                val provider = if (lm.isProviderEnabled(android.location.LocationManager.NETWORK_PROVIDER)) {
                                                    android.location.LocationManager.NETWORK_PROVIDER
                                                } else android.location.LocationManager.GPS_PROVIDER
                                                val lastKnown = lm.getLastKnownLocation(provider)
                                                if (lastKnown != null) {
                                                    val localAddress = getShortLoc(context, lastKnown.latitude, lastKnown.longitude)
                                                    if (savedAddresses.none { it.address == localAddress }) {
                                                        savedAddresses.add(0, SavedAddress("Current Location", localAddress, "", "📍"))
                                                        saveAddressesToDisk()
                                                    }
                                                    onLocationSelected(localAddress)
                                                } else {
                                                    selectedCampusForHostel = "VIT Vellore"
                                                }
                                            } catch (e: Exception) {
                                                selectedCampusForHostel = "VIT Vellore"
                                            }
                                        } else {
                                            try {
                                                context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                                            } catch (e: Exception) {}
                                        }
                                    }
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(42.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(Color(0xFFFFF1F2))
                                            .border(1.dp, Color(0xFFFFE4E6), RoundedCornerShape(12.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        androidx.compose.foundation.Canvas(modifier = Modifier.size(20.dp)) {
                                            val c = center
                                            val r = 7.dp.toPx()
                                            val iconColor = Color(0xFFFF0055)
                                            drawCircle(
                                                color = iconColor,
                                                radius = r,
                                                center = c,
                                                style = Stroke(width = 2.dp.toPx())
                                            )
                                            drawCircle(color = iconColor, radius = 2.5.dp.toPx(), center = c)
                                            drawLine(color = iconColor, start = androidx.compose.ui.geometry.Offset(c.x, 0f), end = androidx.compose.ui.geometry.Offset(c.x, 3.5.dp.toPx()), strokeWidth = 2.dp.toPx())
                                            drawLine(color = iconColor, start = androidx.compose.ui.geometry.Offset(c.x, size.height - 3.5.dp.toPx()), end = androidx.compose.ui.geometry.Offset(c.x, size.height), strokeWidth = 2.dp.toPx())
                                            drawLine(color = iconColor, start = androidx.compose.ui.geometry.Offset(0f, c.y), end = androidx.compose.ui.geometry.Offset(3.5.dp.toPx(), c.y), strokeWidth = 2.dp.toPx())
                                            drawLine(color = iconColor, start = androidx.compose.ui.geometry.Offset(size.width - 3.5.dp.toPx(), c.y), end = androidx.compose.ui.geometry.Offset(size.width, c.y), strokeWidth = 2.dp.toPx())
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = "Use Current Location",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.5.sp,
                                        color = Color(0xFF0F172A)
                                    )
                                }

                                Button(
                                    onClick = {
                                        val lm = context.getSystemService(Context.LOCATION_SERVICE) as? android.location.LocationManager
                                        val gpsEnabled = lm?.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER) == true ||
                                                        lm?.isProviderEnabled(android.location.LocationManager.NETWORK_PROVIDER) == true
                                        if (lm != null && gpsEnabled) {
                                            try {
                                                val provider = if (lm.isProviderEnabled(android.location.LocationManager.NETWORK_PROVIDER)) {
                                                    android.location.LocationManager.NETWORK_PROVIDER
                                                } else android.location.LocationManager.GPS_PROVIDER
                                                val lastKnown = lm.getLastKnownLocation(provider)
                                                if (lastKnown != null) {
                                                    val localAddress = getShortLoc(context, lastKnown.latitude, lastKnown.longitude)
                                                    if (savedAddresses.none { it.address == localAddress }) {
                                                        savedAddresses.add(0, SavedAddress("Current Location", localAddress, "", "📍"))
                                                        saveAddressesToDisk()
                                                    }
                                                    onLocationSelected(localAddress)
                                                } else {
                                                    selectedCampusForHostel = "VIT Vellore"
                                                }
                                            } catch (e: Exception) {
                                                selectedCampusForHostel = "VIT Vellore"
                                            }
                                        } else {
                                            try {
                                                context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                                            } catch (e: Exception) {}
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF0055)),
                                    shape = RoundedCornerShape(12.dp),
                                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                                    modifier = Modifier.height(38.dp)
                                ) {
                                    Text(if (isGpsOn) "Use GPS" else "Enable GPS", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }



                        Spacer(modifier = Modifier.height(26.dp))

                        // SAVED ADDRESSES Header Label & Far-Right "See All >" Button
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "SAVED ADDRESSES",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = Color(0xFF64748B),
                                letterSpacing = 0.8.sp
                            )

                            if (savedAddresses.size > 2) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.clickable { showSeeAll = !showSeeAll }
                                ) {
                                    Text(
                                        text = if (showSeeAll) "Show Less" else "See All",
                                        fontSize = 13.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFFF0055)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    androidx.compose.foundation.Canvas(modifier = Modifier.size(12.dp)) {
                                        val path = Path().apply {
                                            if (showSeeAll) {
                                                moveTo(2.dp.toPx(), 8.dp.toPx())
                                                lineTo(6.dp.toPx(), 4.dp.toPx())
                                                lineTo(10.dp.toPx(), 8.dp.toPx())
                                            } else {
                                                moveTo(4.dp.toPx(), 2.dp.toPx())
                                                lineTo(8.dp.toPx(), 6.dp.toPx())
                                                lineTo(4.dp.toPx(), 10.dp.toPx())
                                            }
                                        }
                                        drawPath(
                                            path = path,
                                            color = Color(0xFFFF0055),
                                            style = Stroke(width = 2.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round, join = androidx.compose.ui.graphics.StrokeJoin.Round)
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        if (savedAddresses.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(Color.White)
                                    .drawWithContent {
                                        drawContent()
                                        val stroke = Stroke(
                                            width = 1.5f.dp.toPx(),
                                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 12f), 0f)
                                        )
                                        drawRoundRect(
                                            color = Color(0xFFE2E8F0),
                                            style = stroke,
                                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(20.dp.toPx(), 20.dp.toPx())
                                        )
                                    }
                                    .padding(vertical = 34.dp, horizontal = 20.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Box(
                                        modifier = Modifier
                                            .size(52.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFFF8FAFC))
                                            .border(1.dp, Color(0xFFF1F5F9), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        androidx.compose.foundation.Canvas(modifier = Modifier.size(22.dp)) {
                                            val path = Path().apply {
                                                moveTo(4.dp.toPx(), 2.dp.toPx())
                                                lineTo(18.dp.toPx(), 2.dp.toPx())
                                                lineTo(18.dp.toPx(), 20.dp.toPx())
                                                lineTo(11.dp.toPx(), 15.dp.toPx())
                                                lineTo(4.dp.toPx(), 20.dp.toPx())
                                                close()
                                            }
                                            drawPath(
                                                path = path,
                                                color = Color(0xFF64748B),
                                                style = Stroke(width = 1.8f.dp.toPx(), join = androidx.compose.ui.graphics.StrokeJoin.Round)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(14.dp))

                                    Text(
                                        text = "No saved addresses yet",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = Color(0xFF0F172A)
                                    )

                                    Spacer(modifier = Modifier.height(6.dp))

                                    Text(
                                        text = "Save your favorite campus spots for instant doorstep delivery.",
                                        fontSize = 12.sp,
                                        color = Color(0xFF64748B),
                                        textAlign = TextAlign.Center,
                                        lineHeight = 17.sp,
                                        modifier = Modifier.padding(horizontal = 12.dp)
                                    )
                                }
                            }
                        } else {
                            displayAddresses.forEach { item ->
                                val isSelected = currentLocation == item.address
                                SavedAddressCard(
                                    item = item,
                                    isSelected = isSelected,
                                    onSelectAddress = { onLocationSelected(item.address) },
                                    onEditClick = {
                                        val intent = Intent(context, SelectLocationActivity::class.java)
                                        mapLauncher.launch(intent)
                                    },
                                    onDeleteClick = {
                                        addressToDelete = item
                                    }
                                )
                            }
                        }
                    }

                    // Delete Confirmation Alert Dialog
                    if (addressToDelete != null) {
                        AlertDialog(
                            onDismissRequest = { addressToDelete = null },
                            title = {
                                Text(
                                    text = "Delete this address?",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = Color(0xFF0F172A)
                                )
                            },
                            text = {
                                Text(
                                    text = "Are you sure you want to delete '${addressToDelete?.label}'? This action cannot be undone.",
                                    fontSize = 13.sp,
                                    color = Color(0xFF64748B)
                                )
                            },
                            confirmButton = {
                                Button(
                                    onClick = {
                                        val target = addressToDelete
                                        if (target != null) {
                                            savedAddresses.remove(target)
                                            saveAddressesToDisk()
                                            Toast.makeText(context, "Address deleted", Toast.LENGTH_SHORT).show()
                                        }
                                        addressToDelete = null
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF0055)),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text("Delete", color = Color.White, fontWeight = FontWeight.Bold)
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { addressToDelete = null }) {
                                    Text("Cancel", color = Color(0xFF64748B), fontWeight = FontWeight.Medium)
                                }
                            },
                            containerColor = Color.White,
                            shape = RoundedCornerShape(16.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))
                }
            }

            // 🏢 Campus Location (Hostel or Academic Building) Selection Modal
            if (selectedCampusForHostel != null) {
                val campusTarget = selectedCampusForHostel ?: "VIT Vellore"
                var selectedLocationType by remember { mutableStateOf("Hostel") } // "Hostel" or "Academic"

                AlertDialog(
                    onDismissRequest = { selectedCampusForHostel = null },
                    title = {
                        Column {
                            // Drag Handle Indicator
                            Box(
                                modifier = Modifier
                                    .align(Alignment.CenterHorizontally)
                                    .width(40.dp)
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(Color(0xFFCBD5E1))
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(Color(0xFFF1F5F9)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(text = "🏫", fontSize = 18.sp)
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = campusTarget,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 17.sp,
                                        color = Color(0xFF0F172A)
                                    )
                                }

                                // Close Button
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFF1F5F9))
                                        .clickable { selectedCampusForHostel = null },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(text = "✕", fontSize = 13.sp, color = Color(0xFF64748B), fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    },
                    text = {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            // Segmented Capsule Tabs: [🏢 Hostel] [🎓 Academic]
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(Color(0xFFF1F5F9))
                                    .padding(4.dp)
                            ) {
                                val isHostel = selectedLocationType == "Hostel"
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(42.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (isHostel) Color(0xFFFF0055) else Color.Transparent)
                                        .clickable { selectedLocationType = "Hostel" },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(text = "🏢 ", fontSize = 13.sp)
                                        Text(
                                            text = "Hostel",
                                            fontWeight = if (isHostel) FontWeight.Bold else FontWeight.SemiBold,
                                            color = if (isHostel) Color.White else Color(0xFF0F172A),
                                            fontSize = 13.sp
                                        )
                                    }
                                }

                                val isAcademic = selectedLocationType == "Academic"
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(42.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (isAcademic) Color(0xFFFF0055) else Color.Transparent)
                                        .clickable { selectedLocationType = "Academic" },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(text = "🎓 ", fontSize = 13.sp)
                                        Text(
                                            text = "Academic",
                                            fontWeight = if (isAcademic) FontWeight.Bold else FontWeight.SemiBold,
                                            color = if (isAcademic) Color.White else Color(0xFF0F172A),
                                            fontSize = 13.sp
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(18.dp))

                            if (selectedLocationType == "Hostel") {
                                // Field 1 Header Row
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "HOSTEL BLOCK *",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        letterSpacing = 0.5.sp,
                                        color = Color(0xFF64748B)
                                    )
                                    Text(
                                        text = "e.g. Block D, Block Q, Ladies Hostel",
                                        fontSize = 11.sp,
                                        color = Color(0xFF94A3B8)
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))

                                OutlinedTextField(
                                    value = selectedHostelBlock,
                                    onValueChange = { selectedHostelBlock = it },
                                    placeholder = { Text("e.g. Block Q, LH-A", fontSize = 13.5.sp, color = Color(0xFF94A3B8)) },
                                    singleLine = true,
                                    leadingIcon = { Text("🏛️", fontSize = 15.sp) },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = Color.White,
                                        unfocusedContainerColor = Color.White,
                                        focusedBorderColor = Color(0xFFFF0055),
                                        unfocusedBorderColor = Color(0xFFE2E8F0)
                                    ),
                                    modifier = Modifier.fillMaxWidth().height(52.dp)
                                )

                                Spacer(modifier = Modifier.height(14.dp))

                                // Field 2 Header Row
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "ROOM NUMBER (OPTIONAL)",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        letterSpacing = 0.5.sp,
                                        color = Color(0xFF64748B)
                                    )
                                    Text(
                                        text = "Defaults to Main Entrance",
                                        fontSize = 11.sp,
                                        color = Color(0xFF94A3B8)
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))

                                OutlinedTextField(
                                    value = roomNumberInput,
                                    onValueChange = { roomNumberInput = it },
                                    placeholder = { Text("e.g. Room 412, 3rd Floor", fontSize = 13.sp, color = Color(0xFF94A3B8)) },
                                    singleLine = true,
                                    leadingIcon = { Text("📍", fontSize = 15.sp) },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = Color(0xFFF8FAFC),
                                        unfocusedContainerColor = Color(0xFFF8FAFC),
                                        focusedBorderColor = Color(0xFFFF0055),
                                        unfocusedBorderColor = Color(0xFFE2E8F0)
                                    ),
                                    modifier = Modifier.fillMaxWidth().height(52.dp)
                                )
                            } else {
                                // Field 1 Header Row
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "BUILDING OR DEPARTMENT *",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        letterSpacing = 0.5.sp,
                                        color = Color(0xFF64748B)
                                    )
                                    Text(
                                        text = "e.g. SJT, TT, Dept of CSE, MB",
                                        fontSize = 11.sp,
                                        color = Color(0xFF94A3B8)
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))

                                OutlinedTextField(
                                    value = selectedHostelBlock,
                                    onValueChange = { selectedHostelBlock = it },
                                    placeholder = { Text("e.g. Technology Tower (TT), SJT", fontSize = 13.5.sp, color = Color(0xFF94A3B8)) },
                                    singleLine = true,
                                    leadingIcon = { Text("🏛️", fontSize = 15.sp) },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = Color.White,
                                        unfocusedContainerColor = Color.White,
                                        focusedBorderColor = Color(0xFFFF0055),
                                        unfocusedBorderColor = Color(0xFFE2E8F0)
                                    ),
                                    modifier = Modifier.fillMaxWidth().height(52.dp)
                                )

                                Spacer(modifier = Modifier.height(14.dp))

                                // Field 2 Header Row
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "FLOOR / LAB / ROOM (OPTIONAL)",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        letterSpacing = 0.5.sp,
                                        color = Color(0xFF64748B)
                                    )
                                    Text(
                                        text = "Defaults to Main Entrance",
                                        fontSize = 11.sp,
                                        color = Color(0xFF94A3B8)
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))

                                OutlinedTextField(
                                    value = roomNumberInput,
                                    onValueChange = { roomNumberInput = it },
                                    placeholder = { Text("e.g. 4th Floor, Lab 412, Foyer", fontSize = 13.sp, color = Color(0xFF94A3B8)) },
                                    singleLine = true,
                                    leadingIcon = { Text("📍", fontSize = 15.sp) },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = Color(0xFFF8FAFC),
                                        unfocusedContainerColor = Color(0xFFF8FAFC),
                                        focusedBorderColor = Color(0xFFFF0055),
                                        unfocusedBorderColor = Color(0xFFE2E8F0)
                                    ),
                                    modifier = Modifier.fillMaxWidth().height(52.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // 📜 Delivery Policy Note Box
                            val policyNoteText = if (selectedLocationType == "Academic") {
                                "Delivery riders will meet you at your Academic Building / Department Entrance."
                            } else {
                                "Delivery riders will meet you at your Hostel Gate / Main Entrance."
                            }

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFFF8FAFC))
                                    .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(10.dp))
                                    .padding(horizontal = 14.dp, vertical = 10.dp)
                            ) {
                                Text(
                                    text = buildAnnotatedString {
                                        withStyle(style = SpanStyle(fontWeight = FontWeight.Bold, color = Color(0xFF334155))) {
                                            append("Note: ")
                                        }
                                        append(policyNoteText)
                                    },
                                    fontSize = 11.5.sp,
                                    lineHeight = 15.sp,
                                    color = Color(0xFF334155)
                                )
                            }
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                val blockOrBuildingInput = selectedHostelBlock.trim()
                                if (blockOrBuildingInput.isBlank()) {
                                    val fieldName = if (selectedLocationType == "Hostel") "Hostel Block" else "Building / Department"
                                    Toast.makeText(context, "Please enter your $fieldName!", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }

                                val primaryText = blockOrBuildingInput
                                val rawSecondary = roomNumberInput.trim()
                                val secondaryText = if (rawSecondary.isNotBlank()) rawSecondary else "Main Entrance"
                                
                                val formattedLabel = if (rawSecondary.isNotBlank()) {
                                    val prefix = if (selectedLocationType == "Hostel" && !secondaryText.contains("room", ignoreCase = true)) "Room $secondaryText" else secondaryText
                                    "$primaryText • $prefix"
                                } else {
                                    "$primaryText • Main Entrance"
                                }

                                val finalAddress = "$primaryText, $secondaryText • $campusTarget"
                                
                                val newSaved = SavedAddress(
                                    label = formattedLabel,
                                    address = finalAddress,
                                    status = if (selectedLocationType == "Hostel") "Hostel" else "Academic",
                                    icon = if (selectedLocationType == "Hostel") "🏢" else "🏫"
                                )
                                if (savedAddresses.none { it.address == finalAddress }) {
                                    savedAddresses.add(0, newSaved)
                                    saveAddressesToDisk()
                                }
                                onLocationSelected(finalAddress)
                                selectedCampusForHostel = null
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF0055)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().height(50.dp)
                        ) {
                            Text("Confirm & Set Location", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.5.sp)
                        }
                    },
                    dismissButton = null,
                    containerColor = Color.White,
                    shape = RoundedCornerShape(24.dp)
                )
            }
        }
    }
}

data class SavedAddress(
    val label: String,
    val address: String,
    val status: String,
    val icon: String
)

@Composable
fun ShareAddressScreen(
    onBack: () -> Unit,
    context: android.content.Context
) {
    val primaryColor = Color(0xFFE11D48)
    val textPrimary = Color(0xFF1E293B)
    val textSecondary = Color(0xFF64748B)

    val savedAddresses = remember {
        val json = EncryptionManager.getString(context, "saved_address_list")
        val list: List<SavedAddress> = if (!json.isNullOrEmpty()) {
            try {
                val type = object : TypeToken<List<SavedAddress>>() {}.type
                Gson().fromJson(json, type)
            } catch (e: Exception) {
                emptyList()
            }
        } else {
            emptyList()
        }
        mutableStateListOf(*list.toTypedArray())
    }

    fun saveAddressesToDisk() {
        val json = Gson().toJson(savedAddresses.toList())
        EncryptionManager.saveString(context, "saved_address_list", json)
    }

    val mapLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val address = result.data?.getStringExtra("selected_address")
            if (address != null) {
                val newAddr = SavedAddress("Added Address", address, "", "📍")
                savedAddresses.add(0, newAddr)
                saveAddressesToDisk()
            }
        }
    }

    var selectedIndex by remember { mutableStateOf(-1) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC))
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(horizontal = 16.dp, vertical = 16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFF1F5F9))
                            .clickable { onBack() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("←", fontSize = 20.sp, color = textPrimary, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Share Address",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = textPrimary
                    )
                }
            }

            Divider(color = Color(0xFFE2E8F0), thickness = 1.dp)

            // Scrollable Content
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
            ) {
                // Cloud pin illustration banner
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color(0xFFE0F2FE), Color(0xFFF0F9FF))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("📍", fontSize = 64.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Share your address",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = textPrimary
                        )
                        Text(
                            text = "with your friends or loved ones",
                            fontSize = 13.sp,
                            color = textSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Add New Address Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFFF1F5F9))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                try {
                                    val intent = Intent(context, SelectLocationActivity::class.java)
                                    mapLauncher.launch(intent)
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Unable to launch map screen.", Toast.LENGTH_SHORT).show()
                                }
                            }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("+", fontSize = 22.sp, color = primaryColor, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Add New Address",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = primaryColor,
                            modifier = Modifier.weight(1f)
                        )
                        Text("➔", fontSize = 14.sp, color = textSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // List of addresses
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    if (savedAddresses.isEmpty()) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                            border = BorderStroke(1.5.dp, Color(0xFFE2E8F0))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 32.dp, horizontal = 24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                    Box(
                                        modifier = Modifier.size(56.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                                            val w = size.width
                                            val h = size.height
                                            val lensCenter = androidx.compose.ui.geometry.Offset(w * 0.42f, h * 0.42f)
                                            val lensRadius = 26.dp.toPx()
                                            val strokeW = 5.dp.toPx()
                                            val lensColor = Color(0xFFCBD5E1)

                                            // 1. Outer lens circle (filled soft grey)
                                            drawCircle(
                                                color = Color(0xFFEFF6FF),
                                                radius = lensRadius,
                                                center = lensCenter
                                            )

                                            // 2. Lens border (thick ring)
                                            drawCircle(
                                                color = lensColor,
                                                radius = lensRadius,
                                                center = lensCenter,
                                                style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeW)
                                            )

                                            // 3. Inner detail ring
                                            drawCircle(
                                                color = Color(0xFFDDE4EE),
                                                radius = lensRadius * 0.55f,
                                                center = lensCenter,
                                                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.5.dp.toPx())
                                            )

                                            // 4. Handle (thick rounded line bottom-right)
                                            val handleStart = androidx.compose.ui.geometry.Offset(
                                                lensCenter.x + lensRadius * 0.68f,
                                                lensCenter.y + lensRadius * 0.68f
                                            )
                                            val handleEnd = androidx.compose.ui.geometry.Offset(
                                                lensCenter.x + lensRadius * 1.55f,
                                                lensCenter.y + lensRadius * 1.55f
                                            )
                                            drawLine(
                                                color = lensColor,
                                                start = handleStart,
                                                end = handleEnd,
                                                strokeWidth = 7.dp.toPx(),
                                                cap = androidx.compose.ui.graphics.StrokeCap.Round
                                            )

                                            // 5. Glossy 3D highlight top-left
                                            drawCircle(
                                                color = Color.White.copy(alpha = 0.75f),
                                                radius = 7.dp.toPx(),
                                                center = androidx.compose.ui.geometry.Offset(
                                                    lensCenter.x - 11.dp.toPx(),
                                                    lensCenter.y - 11.dp.toPx()
                                                )
                                            )
                                        }
                                    }
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(text = "No saved address yet", fontWeight = FontWeight.ExtraBold, fontSize = 17.sp, color = Color(0xFF1E293B))
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(text = "Start by detecting your current location", fontSize = 12.sp, color = Color(0xFF94A3B8), textAlign = TextAlign.Center)
                            }
                        }
                    }
                    savedAddresses.forEachIndexed { index, item ->
                        val isSelected = selectedIndex == index
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = BorderStroke(1.dp, if (isSelected) primaryColor else Color(0xFFF1F5F9))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedIndex = index
                                    }
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(item.icon, fontSize = 20.sp)
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = item.label,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = textPrimary
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = item.address,
                                        fontSize = 12.sp,
                                        color = textSecondary,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                RadioButton(
                                    selected = isSelected,
                                    onClick = { selectedIndex = index },
                                    colors = RadioButtonDefaults.colors(selectedColor = primaryColor)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }

            // Bottom-pinned Share Address Button
            Divider(color = Color(0xFFE2E8F0), thickness = 1.dp)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Button(
                    onClick = {
                        if (selectedIndex != -1) {
                            val selectedAddress = savedAddresses[selectedIndex].address
                            try {
                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, "Here is my delivery address: $selectedAddress")
                                }
                                context.startActivity(Intent.createChooser(shareIntent, "Send Address via"))
                            } catch (e: Exception) {
                                Toast.makeText(context, "Unable to share address.", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    enabled = selectedIndex != -1,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = primaryColor,
                        disabledContainerColor = Color(0xFFE2E8F0)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Text(
                        text = "Share Address",
                        color = if (selectedIndex != -1) Color.White else textSecondary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

@Composable
fun Glossy3DLocationPin(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.size(100.dp, 120.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            val centerX = width / 2f
            val topRadius = width * 0.42f
            val groundY = height * 0.88f

            // 1. Soft Magenta Ambient Glow behind pin
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFFFF0055).copy(alpha = 0.22f), Color.Transparent),
                    center = androidx.compose.ui.geometry.Offset(centerX, height * 0.45f),
                    radius = width * 0.55f
                )
            )

            // 2. Ground Contact Shadow
            drawOval(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0x401E293B), Color(0x001E293B)),
                    center = androidx.compose.ui.geometry.Offset(centerX, groundY + 4.dp.toPx()),
                    radius = width * 0.35f
                ),
                topLeft = androidx.compose.ui.geometry.Offset(centerX - width * 0.30f, groundY - 2.dp.toPx()),
                size = androidx.compose.ui.geometry.Size(width * 0.60f, 12.dp.toPx())
            )

            // 3. Volumetric 3D Teardrop Pin Path
            val pinPath = Path().apply {
                moveTo(centerX - topRadius, 4.dp.toPx() + topRadius)
                arcTo(
                    rect = androidx.compose.ui.geometry.Rect(
                        left = centerX - topRadius,
                        top = 4.dp.toPx(),
                        right = centerX + topRadius,
                        bottom = 4.dp.toPx() + (topRadius * 2)
                    ),
                    startAngleDegrees = 180f,
                    sweepAngleDegrees = 180f,
                    forceMoveTo = false
                )
                cubicTo(centerX + topRadius, 4.dp.toPx() + topRadius * 1.55f, centerX + (topRadius * 0.40f), groundY - 4.dp.toPx(), centerX, groundY)
                cubicTo(centerX - (topRadius * 0.40f), groundY - 4.dp.toPx(), centerX - topRadius, 4.dp.toPx() + topRadius * 1.55f, centerX - topRadius, 4.dp.toPx() + topRadius)
                close()
            }

            // Vibrant 3D Gradient Fill
            drawPath(
                path = pinPath,
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFFFF3377), Color(0xFFFF0055), Color(0xFFC70042)),
                    startY = 0f,
                    endY = groundY
                )
            )

            // 4. Glossy Crescent Arc Highlight along top-right edge
            val highlightPath = Path().apply {
                arcTo(
                    rect = androidx.compose.ui.geometry.Rect(
                        left = centerX - topRadius + 3.dp.toPx(),
                        top = 7.dp.toPx(),
                        right = centerX + topRadius - 3.dp.toPx(),
                        bottom = 1.dp.toPx() + (topRadius * 2)
                    ),
                    startAngleDegrees = 300f,
                    sweepAngleDegrees = 90f,
                    forceMoveTo = false
                )
            }
            drawPath(
                path = highlightPath,
                color = Color.White.copy(alpha = 0.55f),
                style = Stroke(width = 3.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round)
            )

            // 5. White Doughnut Hole & Vibrant Pink Pupil Center
            val holeCenterY = 4.dp.toPx() + topRadius
            val holeRadius = topRadius * 0.40f
            drawCircle(color = Color.White, radius = holeRadius, center = androidx.compose.ui.geometry.Offset(centerX, holeCenterY))
            drawCircle(color = Color(0xFFFF0055), radius = holeRadius * 0.48f, center = androidx.compose.ui.geometry.Offset(centerX, holeCenterY))
        }
    }
}

val WhatsAppGreenColor = Color(0xFF25D366)
val WhatsAppGreenBgColor = Color(0xFFECFDF5)
val WhatsAppBorderColor = Color(0xFFD1FAE5)

@Composable
fun WhatsAppIconBadge(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(42.dp)
            .background(WhatsAppGreenBgColor, RoundedCornerShape(12.dp))
            .border(1.dp, WhatsAppBorderColor, RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(22.dp)) {
            val w = size.width
            val h = size.height
            val scaleX = w / 24f
            val scaleY = h / 24f
            val path = Path().apply {
                moveTo(12f * scaleX, 2f * scaleY)
                cubicTo(6.48f * scaleX, 2f * scaleY, 2f * scaleX, 6.48f * scaleY, 2f * scaleX, 12f * scaleY)
                cubicTo(2f * scaleX, 13.85f * scaleY, 2.5f * scaleX, 15.58f * scaleY, 3.38f * scaleX, 17.08f * scaleY)
                lineTo(2.25f * scaleX, 21.25f * scaleY)
                lineTo(6.57f * scaleX, 20.14f * scaleY)
                cubicTo(8.18f * scaleX, 20.71f * scaleY, 10f * scaleX, 21.31f * scaleY, 12f * scaleX, 22f * scaleY)
                cubicTo(17.52f * scaleX, 22f * scaleY, 22f * scaleX, 17.52f * scaleY, 22f * scaleX, 12f * scaleY)
                cubicTo(22f * scaleX, 6.48f * scaleY, 17.52f * scaleX, 2f * scaleY, 12f * scaleX, 2f * scaleY)
                close()

                moveTo(16.8f * scaleX, 15.1f * scaleY)
                cubicTo(16.6f * scaleX, 15.65f * scaleY, 15.8f * scaleX, 16.15f * scaleY, 15.2f * scaleX, 16.25f * scaleY)
                cubicTo(14.8f * scaleX, 16.3f * scaleY, 14.3f * scaleX, 16.35f * scaleY, 12.5f * scaleX, 15.6f * scaleY)
                cubicTo(10.2f * scaleX, 14.65f * scaleY, 8.7f * scaleX, 12.3f * scaleY, 8.6f * scaleX, 12.15f * scaleY)
                cubicTo(8.5f * scaleX, 12.0f * scaleY, 7.7f * scaleX, 10.95f * scaleY, 7.7f * scaleX, 9.85f * scaleY)
                cubicTo(7.7f * scaleX, 8.75f * scaleY, 8.25f * scaleX, 8.2f * scaleY, 8.5f * scaleX, 7.95f * scaleY)
                cubicTo(8.7f * scaleX, 7.75f * scaleY, 8.95f * scaleX, 7.7f * scaleY, 9.2f * scaleX, 7.7f * scaleY)
                lineTo(9.7f * scaleX, 7.7f * scaleY)
                cubicTo(9.9f * scaleX, 7.7f * scaleY, 10.1f * scaleX, 7.75f * scaleY, 10.2f * scaleX, 8.0f * scaleY)
                lineTo(10.9f * scaleX, 9.7f * scaleY)
                cubicTo(11.0f * scaleX, 9.9f * scaleY, 11.0f * scaleX, 10.05f * scaleY, 10.85f * scaleX, 10.25f * scaleY)
                lineTo(10.45f * scaleX, 10.75f * scaleY)
                cubicTo(10.3f * scaleX, 10.9f * scaleY, 10.15f * scaleX, 11.05f * scaleY, 10.3f * scaleX, 11.3f * scaleY)
                cubicTo(10.45f * scaleX, 11.55f * scaleY, 11.0f * scaleX, 12.45f * scaleY, 11.8f * scaleX, 13.15f * scaleY)
                cubicTo(12.85f * scaleX, 14.05f * scaleY, 13.7f * scaleX, 14.35f * scaleY, 13.95f * scaleX, 14.5f * scaleY)
                cubicTo(14.2f * scaleX, 14.6f * scaleY, 14.35f * scaleX, 14.55f * scaleY, 14.5f * scaleX, 14.4f * scaleY)
                lineTo(15.0f * scaleX, 13.7f * scaleY)
                cubicTo(15.2f * scaleX, 13.45f * scaleY, 15.4f * scaleX, 13.4f * scaleY, 15.65f * scaleX, 13.5f * scaleY)
                lineTo(17.25f * scaleX, 14.3f * scaleY)
                cubicTo(17.5f * scaleX, 14.45f * scaleY, 17.65f * scaleX, 14.55f * scaleY, 17.65f * scaleX, 14.7f * scaleY)
                cubicTo(17.65f * scaleX, 14.85f * scaleY, 17.0f * scaleX, 14.55f * scaleY, 16.8f * scaleX, 15.1f * scaleY)
                close()
            }
            drawPath(path, color = WhatsAppGreenColor)
        }
    }
}

/**
 * Pixel-perfect "Pick Location on Map" Bottom Button with outlined map pin.
 */
@Composable
fun PickLocationOnMapButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    val brandMagenta = Color(0xFFFF0055)

    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(54.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = brandMagenta,
            contentColor = Color.White
        ),
        shape = RoundedCornerShape(16.dp),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 2.dp,
            pressedElevation = 4.dp
        ),
        contentPadding = PaddingValues(horizontal = 20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Map Pin Icon (Clean White Teardrop Location Marker)
            Canvas(modifier = Modifier.size(20.dp)) {
                val w = size.width
                val h = size.height
                val scaleX = w / 24f
                val scaleY = h / 24f
                val strokeW = 2.dp.toPx()

                // Teardrop outline
                val path = Path().apply {
                    moveTo(12f * scaleX, 2f * scaleY)
                    cubicTo(7.58f * scaleX, 2f * scaleY, 4f * scaleX, 5.58f * scaleY, 4f * scaleX, 10f * scaleY)
                    cubicTo(4f * scaleX, 15.25f * scaleY, 12f * scaleX, 22f * scaleY, 12f * scaleX, 22f * scaleY)
                    cubicTo(12f * scaleX, 22f * scaleY, 20f * scaleX, 15.25f * scaleY, 20f * scaleX, 10f * scaleY)
                    cubicTo(20f * scaleX, 5.58f * scaleY, 16.42f * scaleX, 2f * scaleY, 12f * scaleX, 2f * scaleY)
                    close()
                }
                drawPath(
                    path = path,
                    color = Color.White,
                    style = Stroke(width = strokeW, cap = androidx.compose.ui.graphics.StrokeCap.Round, join = androidx.compose.ui.graphics.StrokeJoin.Round)
                )

                // Inner hollow circular core
                drawCircle(
                    color = Color.White,
                    radius = 3.2f * scaleX,
                    center = androidx.compose.ui.geometry.Offset(12f * scaleX, 10f * scaleY),
                    style = Stroke(width = strokeW)
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Text(
                text = "Pick Location on Map",
                fontSize = 15.5.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}

/**
 * SavedAddressCard with 3-dot overflow menu (⋮), Tag Badge Pill, Edit, and Delete actions.
 */
@Composable
fun SavedAddressCard(
    item: SavedAddress,
    isSelected: Boolean,
    onSelectAddress: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showMenu by remember { mutableStateOf(false) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, if (isSelected) Color(0xFFFF0055) else Color(0xFFE2E8F0))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onSelectAddress() }
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Leading Icon Container (40.dp x 40.dp Box with Map Pin Outline Vector)
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFF8FAFC))
                    .border(1.dp, Color(0xFFF1F5F9), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.size(20.dp)) {
                    val w = size.width
                    val h = size.height
                    val scaleX = w / 24f
                    val scaleY = h / 24f
                    val strokeW = 1.8f.dp.toPx()
                    val path = Path().apply {
                        moveTo(12f * scaleX, 2f * scaleY)
                        cubicTo(7.58f * scaleX, 2f * scaleY, 4f * scaleX, 5.58f * scaleY, 4f * scaleX, 10f * scaleY)
                        cubicTo(4f * scaleX, 15.25f * scaleY, 12f * scaleX, 22f * scaleY, 12f * scaleX, 22f * scaleY)
                        cubicTo(12f * scaleX, 22f * scaleY, 20f * scaleX, 15.25f * scaleY, 20f * scaleX, 10f * scaleY)
                        cubicTo(20f * scaleX, 5.58f * scaleY, 16.42f * scaleX, 2f * scaleY, 12f * scaleX, 2f * scaleY)
                        close()
                    }
                    drawPath(
                        path = path,
                        color = Color(0xFF0F172A),
                        style = Stroke(width = strokeW, cap = androidx.compose.ui.graphics.StrokeCap.Round, join = androidx.compose.ui.graphics.StrokeJoin.Round)
                    )
                    drawCircle(
                        color = Color(0xFF0F172A),
                        radius = 3.2f * scaleX,
                        center = androidx.compose.ui.geometry.Offset(12f * scaleX, 10f * scaleY),
                        style = Stroke(width = strokeW)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Content Column (weight = 1f)
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = item.label,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Color(0xFF0F172A)
                    )

                    if (item.status.isNotEmpty()) {
                        Spacer(modifier = Modifier.width(8.dp))
                        // Tag Badge Pill
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFFF1F5F9))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = item.status.uppercase(),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF475569)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = item.address,
                    fontSize = 11.5.sp,
                    color = Color(0xFF64748B),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // 3-Dot Overflow Menu (⋮)
            Box {
                IconButton(
                    onClick = { showMenu = true },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "More options",
                        tint = Color(0xFF64748B),
                        modifier = Modifier.size(18.dp)
                    )
                }

                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false },
                    modifier = Modifier.background(Color.White, RoundedCornerShape(12.dp))
                ) {
                    DropdownMenuItem(
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Edit Address",
                                    tint = Color(0xFF334155),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "Edit Address",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF0F172A)
                                )
                            }
                        },
                        onClick = {
                            showMenu = false
                            onEditClick()
                        }
                    )

                    Divider(thickness = 0.5.dp, color = Color(0xFFF1F5F9))

                    DropdownMenuItem(
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete Address",
                                    tint = Color(0xFFFF0055),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "Delete Address",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFFFF0055)
                                )
                            }
                        },
                        onClick = {
                            showMenu = false
                            onDeleteClick()
                        }
                    )
                }
            }
        }
    }
}
