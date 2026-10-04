package com.college.campusapp

import android.widget.Toast

import androidx.compose.foundation.BorderStroke

import androidx.compose.foundation.background

import androidx.compose.foundation.border

import androidx.compose.foundation.clickable

import androidx.compose.foundation.layout.*

import androidx.compose.foundation.lazy.LazyColumn

import androidx.compose.foundation.lazy.items

import androidx.compose.foundation.shape.CircleShape

import androidx.compose.foundation.shape.RoundedCornerShape

import androidx.compose.foundation.text.KeyboardOptions

import androidx.compose.material3.*

import androidx.compose.runtime.*

import androidx.compose.ui.tooling.preview.Preview

import androidx.compose.ui.Alignment

import androidx.compose.ui.Modifier

import androidx.compose.ui.draw.clip

import androidx.compose.ui.graphics.Brush

import androidx.compose.ui.graphics.Color

import androidx.compose.ui.platform.LocalContext

import androidx.compose.ui.text.font.FontWeight

import androidx.compose.ui.text.input.KeyboardType

import androidx.compose.ui.text.style.TextAlign

import androidx.compose.ui.unit.dp

import androidx.compose.ui.unit.sp

import androidx.compose.ui.platform.LocalDensity

import androidx.compose.animation.core.animateFloatAsState

import androidx.compose.animation.core.spring

import androidx.compose.animation.core.Spring

import androidx.compose.ui.input.pointer.pointerInput

import androidx.compose.foundation.gestures.detectHorizontalDragGestures

import androidx.compose.ui.draw.rotate

import androidx.compose.ui.draw.scale

import androidx.compose.animation.core.rememberInfiniteTransition

import androidx.compose.animation.core.animateFloat

import androidx.compose.animation.core.infiniteRepeatable

import androidx.compose.animation.core.tween

import androidx.compose.animation.core.LinearEasing

import androidx.compose.animation.core.RepeatMode

import kotlinx.coroutines.launch

import java.util.Locale

import android.media.ToneGenerator

import android.media.AudioManager

import androidx.activity.compose.rememberLauncherForActivityResult

import androidx.activity.result.contract.ActivityResultContracts

import android.net.Uri

import androidx.compose.foundation.rememberScrollState

import androidx.compose.foundation.verticalScroll

import androidx.compose.ui.window.DialogProperties

@OptIn(ExperimentalMaterial3Api::class)

@Composable

fun RiderDashboard(

    onSwitchBack: () -> Unit,

    modifier: Modifier = Modifier

) {

    val context = LocalContext.current

    remember {

        OrderRepository.load(context)

        true

    }

    var isOnline by remember { mutableStateOf(com.college.campusapp.security.EncryptionManager.getBoolean(context, "rider_online_status", false)) }

    val currentUserEmail = remember { com.college.campusapp.security.EncryptionManager.getString(context, "user_email") ?: "student@college.edu" }

    var userCollege by remember { mutableStateOf("Googleplex Campus") }

    var riderLatitude by remember { mutableStateOf(com.college.campusapp.security.EncryptionManager.getString(context, "user_last_latitude", "0.0")?.toDoubleOrNull() ?: 0.0) }

    var riderLongitude by remember { mutableStateOf(com.college.campusapp.security.EncryptionManager.getString(context, "user_last_longitude", "0.0")?.toDoubleOrNull() ?: 0.0) }

    var isDetectingCollege by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[android.Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarseGranted = permissions[android.Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (fineGranted || coarseGranted) {
            isDetectingCollege = true
            detectCurrentCampus(
                context = context,
                onResult = { campus ->
                    isDetectingCollege = false
                    val lastLatStr = com.college.campusapp.security.EncryptionManager.getString(context, "user_last_latitude", "0.0") ?: "0.0"
                    val lastLonStr = com.college.campusapp.security.EncryptionManager.getString(context, "user_last_longitude", "0.0") ?: "0.0"
                    riderLatitude = lastLatStr.toDoubleOrNull() ?: 0.0
                    riderLongitude = lastLonStr.toDoubleOrNull() ?: 0.0
                    if (campus != null) {
                        userCollege = campus.name
                        com.college.campusapp.security.EncryptionManager.saveString(context, "user_verified_college", campus.name)
                    } else {
                        userCollege = "City Mode"
                        com.college.campusapp.security.EncryptionManager.saveString(context, "user_verified_college", "City Mode")
                    }
                },
                onGpsDisabled = {
                    isDetectingCollege = false
                    Toast.makeText(context, "Please turn on GPS/Location in settings", Toast.LENGTH_LONG).show()
                    try {
                        context.startActivity(android.content.Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                    } catch (e: Exception) {}
                }
            )
        }
    }

    val pendingJobs = OrderRepository.pendingOrders

    var refreshTrigger by remember { mutableStateOf(0) }

    val completedJobs = remember(OrderRepository.completedOrders.size, refreshTrigger) {

        OrderRepository.completedOrders.filter { it.runnerName == currentUserEmail || it.runnerName.isNullOrEmpty() }

    }

    val todayJobs = remember(completedJobs, refreshTrigger) {

        completedJobs.filter { it.completionTimestamp > 0L && android.text.format.DateUtils.isToday(it.completionTimestamp) }

    }

    val pastJobs = remember(completedJobs, refreshTrigger) {

        completedJobs.filter { it.completionTimestamp == 0L || !android.text.format.DateUtils.isToday(it.completionTimestamp) }

    }

    var isRefreshing by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()

    val infiniteTransition = rememberInfiniteTransition(label = "RefreshRotation")

    val angle by infiniteTransition.animateFloat(

        initialValue = 0f,

        targetValue = 360f,

        animationSpec = infiniteRepeatable(

            animation = tween(1000, easing = LinearEasing),

            repeatMode = RepeatMode.Restart

        ),

        label = "Angle"

    )

    var walletBalance by remember { mutableStateOf(WalletManager.getBalance(context, isRider = true)) }

    var selectedJob by remember { mutableStateOf<SimulatedOrder?>(null) }

    val storedActiveJobId = remember { com.college.campusapp.security.EncryptionManager.getString(context, "active_rider_job_id") }

    var activeJob by remember {

        mutableStateOf(

            if (storedActiveJobId != null) {

                OrderRepository.pendingOrders.firstOrNull { it.id == storedActiveJobId }

                    ?: OrderRepository.completedOrders.firstOrNull { it.id == storedActiveJobId }

            } else {

                null

            }

        )

    }

    LaunchedEffect(activeJob) {

        if (activeJob != null) {

            com.college.campusapp.security.EncryptionManager.saveString(context, "active_rider_job_id", activeJob!!.id)

        } else {

            com.college.campusapp.security.EncryptionManager.saveString(context, "active_rider_job_id", null)

        }

    }

    var dashboardTab by remember { mutableStateOf("Current") }

    // Read refreshTrigger to force recomposition on manual refresh

    // Bypassed self-order filter (it.customerId != currentUserEmail) for single-device testing

    val visibleJobs = remember(pendingJobs.size, refreshTrigger, userCollege) {

        val cleanRiderCollege = userCollege.replace(" (Simulated)", "").trim()

        pendingJobs.filter { 

            it.status == "PENDING" && 

            it.collegeName.replace(" (Simulated)", "").trim().equals(cleanRiderCollege, ignoreCase = true)

        }

    }

    val rawPendingCount = remember(pendingJobs.size, userCollege) {

        val cleanRiderCollege = userCollege.replace(" (Simulated)", "").trim()

        pendingJobs.count { 

            it.status == "PENDING" && 

            it.collegeName.replace(" (Simulated)", "").trim().equals(cleanRiderCollege, ignoreCase = true)

        }

    }

    LaunchedEffect(rawPendingCount) {

        val soundEnabled = com.college.campusapp.security.EncryptionManager.getBoolean(context, "sound_effects_enabled", true)

        if (isOnline && rawPendingCount > 0 && soundEnabled) {

            try {

                val toneG = ToneGenerator(AudioManager.STREAM_ALARM, 100)

                toneG.startTone(ToneGenerator.TONE_DTMF_1, 2000)

            } catch (e: Exception) {

                e.printStackTrace()

            }

        }

    }

    val primaryColor = Color(0xFF0EA5E9) // Sky Blue (Speed / Delivery)

    val textPrimary = Color(0xFF0F172A)

    val textSecondary = Color(0xFF475569)

    // Job Details Dialog / Action Center

    if (selectedJob != null) {

        val job = selectedJob!!

        var showEtaSelection by remember { mutableStateOf(false) }

        val isPackageTransfer = job.id.startsWith("OrderNo-") || job.id.startsWith("CSV-")

        AlertDialog(

            onDismissRequest = { 

                selectedJob = null 

                showEtaSelection = false

            },

            shape = RoundedCornerShape(24.dp),

            containerColor = Color.White,

            title = {

                Text(

                    text = if (showEtaSelection) (if (isPackageTransfer) "Select ETA to Pickup" else "Select ETA to Shop") else "Accept Delivery Job",

                    fontWeight = FontWeight.Bold,

                    fontSize = 18.sp,

                    color = textPrimary,

                    textAlign = TextAlign.Center,

                    modifier = Modifier.fillMaxWidth()

                )

            },

            text = {

                Column {

                    if (!showEtaSelection) {

                        Text(

                            text = if (isPackageTransfer) "Review package transfer details before accepting this job." else "Review purchase details before accepting this job.",

                            fontSize = 12.sp,

                            color = textSecondary,

                            modifier = Modifier.padding(bottom = 12.dp)

                        )

                        Divider(color = Color(0xFFF1F5F9))

                        Spacer(modifier = Modifier.height(8.dp))

                        InfoRow(label = "Pickup From:", value = job.pickPoint, valueColor = primaryColor)

                        InfoRow(label = "Deliver To:", value = job.dropPoint)

                        if (!isPackageTransfer) {

                            val payAtShop = if (job.itemsSubtotal > 0) job.itemsSubtotal else (job.totalMin - job.deliveryFee - job.gatePremium - 2).coerceAtLeast(0)

                            InfoRow(

                                label = "Amount Rider Will Pay at Shop:",

                                value = "₹$payAtShop",

                                valueColor = textPrimary

                            )

                        }

                        val earningsText = if (job.isEstimated) {

                            "₹${job.deliveryFee} - ₹${job.deliveryFee + 5}"

                        } else {

                            "₹${job.deliveryFee}"

                        }

                        InfoRow(

                            label = "Payout Earnings:", 

                            value = earningsText,

                            valueColor = Color(0xFF10B981)

                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(if (isPackageTransfer) "Items to Transfer:" else "Items to Purchase:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = textPrimary)

                        job.items.forEach { (name, qty) ->

                            Text("• $name (x$qty)", fontSize = 12.sp, color = textSecondary, modifier = Modifier.padding(start = 4.dp, top = 2.dp, bottom = 2.dp))

                        }

                    } else {

                        Text(

                            text = if (isPackageTransfer) "Choose your estimated travel time to reach pickup location:" else "Choose your estimated travel time to reach shop:",

                            fontSize = 13.sp,

                            color = textSecondary,

                            modifier = Modifier.padding(bottom = 20.dp)

                        )

                        Row(

                            modifier = Modifier.fillMaxWidth(),

                            horizontalArrangement = Arrangement.spacedBy(16.dp)

                        ) {

                            // 7 mins button

                            Button(

                                onClick = {

                                    job.etaSeconds = 7 * 60 // 7 minutes

                                    job.etaTimestamp = System.currentTimeMillis() + 7 * 60 * 1000

                                    job.status = "ACCEPTED"

                                    job.runnerName = currentUserEmail

                                    activeJob = job

                                    selectedJob = null

                                    showEtaSelection = false

                                    OrderRepository.save(context)

                                    val formattedId = if (isPackageTransfer) job.id.replace("OrderNo-", "").replace("CSV-", "") else job.id

                                    SystemNotificationHelper.showNotification(

                                        context,

                                        if (isPackageTransfer) "Transfer Accepted 📦" else "Order Accepted 🚴",

                                        if (isPackageTransfer) "Rider Rahul accepted your transfer request $formattedId and is heading to the pickup spot!" else "Rider Rahul accepted your order $formattedId and is heading to the store!"

                                    )

                                    Toast.makeText(context, if (isPackageTransfer) "Transfer Accepted! Heading to pickup spot." else "Job Accepted! ETA set to 7 mins.", Toast.LENGTH_LONG).show()

                                },

                                modifier = Modifier.weight(1f).height(48.dp),

                                colors = ButtonDefaults.buttonColors(containerColor = primaryColor),

                                shape = RoundedCornerShape(12.dp)

                            ) {

                                Text("7 Mins", fontWeight = FontWeight.Bold, color = Color.White)

                            }

                            // 10 mins button

                            Button(

                                onClick = {

                                    job.etaSeconds = 10 * 60 // 10 minutes

                                    job.etaTimestamp = System.currentTimeMillis() + 10 * 60 * 1000

                                    job.status = "ACCEPTED"

                                    job.runnerName = currentUserEmail

                                    activeJob = job

                                    selectedJob = null

                                    showEtaSelection = false

                                    OrderRepository.save(context)

                                    val formattedId = if (isPackageTransfer) job.id.replace("OrderNo-", "").replace("CSV-", "") else job.id

                                    SystemNotificationHelper.showNotification(

                                        context,

                                        if (isPackageTransfer) "Transfer Accepted 📦" else "Order Accepted 🚴",

                                        if (isPackageTransfer) "Rider Rahul accepted your transfer request $formattedId and is heading to the pickup spot!" else "Rider Rahul accepted your order $formattedId and is heading to the store!"

                                    )

                                    Toast.makeText(context, if (isPackageTransfer) "Transfer Accepted! Heading to pickup spot." else "Job Accepted! ETA set to 10 mins.", Toast.LENGTH_LONG).show()

                                },

                                modifier = Modifier.weight(1f).height(48.dp),

                                colors = ButtonDefaults.buttonColors(containerColor = primaryColor),

                                shape = RoundedCornerShape(12.dp)

                            ) {

                                Text("10 Mins", fontWeight = FontWeight.Bold, color = Color.White)

                            }

                        }

                    }

                }

            },

            confirmButton = {

                if (!showEtaSelection) {

                    Button(

                        onClick = {

                            showEtaSelection = true

                        },

                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),

                        shape = RoundedCornerShape(12.dp)

                    ) {

                        Text("Accept Job", color = Color.White)

                    }

                }

            },

            dismissButton = {

                if (!showEtaSelection) {

                    TextButton(onClick = { selectedJob = null }) {

                        Text("Decline", color = textSecondary)

                    }

                } else {

                    TextButton(onClick = { showEtaSelection = false }) {

                        Text("Back", color = textSecondary)

                    }

                }

            }

        )

    }

    Scaffold(

        topBar = {

            CenterAlignedTopAppBar(

                title = {

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {

                        Text("Rider Console", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = textPrimary)

                        if (userCollege.isNotEmpty()) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.clickable {
                                    isDetectingCollege = true
                                    detectCurrentCampus(
                                        context = context,
                                        onResult = { campus ->
                                            isDetectingCollege = false
                                            val lastLatStr = com.college.campusapp.security.EncryptionManager.getString(context, "user_last_latitude", "0.0") ?: "0.0"
                                            val lastLonStr = com.college.campusapp.security.EncryptionManager.getString(context, "user_last_longitude", "0.0") ?: "0.0"
                                            riderLatitude = lastLatStr.toDoubleOrNull() ?: 0.0
                                            riderLongitude = lastLonStr.toDoubleOrNull() ?: 0.0
                                            if (campus != null) {
                                                userCollege = campus.name
                                                com.college.campusapp.security.EncryptionManager.saveString(context, "user_verified_college", campus.name)
                                                Toast.makeText(context, "🎉 GPS Verified: ${campus.name}!", Toast.LENGTH_LONG).show()
                                            } else {
                                                val latStr = com.college.campusapp.security.EncryptionManager.getString(context, "user_last_latitude", "0.0") ?: "0.0"
                                                val lonStr = com.college.campusapp.security.EncryptionManager.getString(context, "user_last_longitude", "0.0") ?: "0.0"
                                                val shortLoc = getShortLoc(context, latStr.toDoubleOrNull() ?: 0.0, lonStr.toDoubleOrNull() ?: 0.0)
                                                userCollege = shortLoc
                                                com.college.campusapp.security.EncryptionManager.saveString(context, "user_verified_college", shortLoc)
                                                Toast.makeText(context, "🎉 GPS Verified: $shortLoc!", Toast.LENGTH_LONG).show()
                                            }
                                        },
                                        onGpsDisabled = {
                                            isDetectingCollege = false
                                            Toast.makeText(context, "Please turn on GPS/Location in settings", Toast.LENGTH_LONG).show()
                                            try {
                                                context.startActivity(android.content.Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                                            } catch (e: Exception) {}
                                        }
                                    )
                                }
                            ) {
                                Text("📍 $userCollege", fontSize = 10.sp, color = Color(0xFF0288D1), fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.width(4.dp))
                                if (isDetectingCollege) {
                                    CircularProgressIndicator(color = Color(0xFF0288D1), strokeWidth = 1.dp, modifier = Modifier.size(10.dp))
                                } else {
                                    Text("🔄", fontSize = 9.sp, color = Color(0xFF0288D1))
                                }
                            }
                        }

                        Text(

                            text = if (isOnline) "🟢 ACTIVE & RECEIVING JOBS" else "🔴 OFFLINE",

                            fontSize = 9.sp,

                            fontWeight = FontWeight.Bold,

                            color = if (isOnline) Color(0xFF10B981) else Color.Red

                        )

                    }

                },

                navigationIcon = {

                    Button(

                        onClick = onSwitchBack,

                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF1F5F9)),

                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),

                        modifier = Modifier.padding(start = 12.dp).height(32.dp)

                    ) {

                        Text("← Student", color = textSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)

                    }

                },

                actions = {

                    IconButton(

                        onClick = {

                            val intent = android.content.Intent(context, WalletActivity::class.java).apply {

                                putExtra("IS_RIDER", true)

                            }

                            context.startActivity(intent)

                        },

                        modifier = Modifier.padding(end = 8.dp)

                    ) {

                        Text("💳", fontSize = 20.sp)

                    }

                    Switch(

                        checked = isOnline,

                        onCheckedChange = { 

                            if (it && userCollege.isEmpty()) {

                                    Toast.makeText(context, "📍 Please verify your location to receive local jobs!", Toast.LENGTH_LONG).show()

                                } else {

                                isOnline = it 

                                com.college.campusapp.security.EncryptionManager.saveBoolean(context, "rider_online_status", it)

                            }

                        },

                        colors = SwitchDefaults.colors(

                            checkedThumbColor = Color.White,

                            checkedTrackColor = Color(0xFF10B981)

                        ),

                        modifier = Modifier.padding(end = 12.dp)

                    )

                },

                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.White)

            )

        },

        containerColor = Color(0xFFF8FAFC)

    ) { paddingValues ->

        // GPS Verification Overlays for Rider

        Column(

            modifier = Modifier

                .fillMaxSize()

                .padding(paddingValues)

                .padding(16.dp)

                .verticalScroll(rememberScrollState())

        ) {

            // Earnings Overview Card

            Card(

                onClick = {

                    val intent = android.content.Intent(context, WalletActivity::class.java).apply {

                        putExtra("IS_RIDER", true)

                    }

                    context.startActivity(intent)

                },

                modifier = Modifier

                    .fillMaxWidth()

                    .height(130.dp),

                shape = RoundedCornerShape(20.dp),

                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)

            ) {

                Box(

                    modifier = Modifier

                        .fillMaxSize()

                        .background(Brush.linearGradient(listOf(Color(0xFF0288D1), Color(0xFF03A9F4))))

                        .padding(20.dp)

                ) {

                    Row(

                        modifier = Modifier.fillMaxSize(),

                        verticalAlignment = Alignment.CenterVertically

                    ) {

                        // Today's Stats

                        Column(modifier = Modifier.weight(1f)) {

                            Text("TODAY'S WORK 📅", color = Color.White.copy(alpha = 0.8f), fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)

                            Spacer(modifier = Modifier.height(4.dp))

                            Text("₹${todayJobs.sumOf { it.deliveryFee }}", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)

                            Spacer(modifier = Modifier.height(4.dp))

                            Text("Completed: ${todayJobs.size}", color = Color.White.copy(alpha = 0.9f), fontSize = 12.sp)

                        }

                        // Divider

                        Box(

                            modifier = Modifier

                                .fillMaxHeight()

                                .width(1.dp)

                                .background(Color.White.copy(alpha = 0.2f))

                                .padding(vertical = 10.dp)

                        )

                        Spacer(modifier = Modifier.width(16.dp))

                        // Past Stats

                        Column(modifier = Modifier.weight(1f)) {

                            Text("PAST HISTORY 📜", color = Color.White.copy(alpha = 0.8f), fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)

                            Spacer(modifier = Modifier.height(4.dp))

                            Text("₹${pastJobs.sumOf { it.deliveryFee }}", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)

                            Spacer(modifier = Modifier.height(4.dp))

                            Text("Total Past: ${pastJobs.size}", color = Color.White.copy(alpha = 0.9f), fontSize = 12.sp)

                        }

                    }
}

            }

            Spacer(modifier = Modifier.height(24.dp))

            // Segmented Top Switcher Tabs (Current vs Completed)

            Row(

                modifier = Modifier

                    .fillMaxWidth()

                    .clip(RoundedCornerShape(12.dp))

                    .background(Color(0xFFF1F5F9))

                    .padding(4.dp)

            ) {

                listOf("Current", "Completed").forEach { tab ->

                    val isSelected = (dashboardTab == tab)

                    Box(

                        modifier = Modifier

                            .weight(1f)

                            .clip(RoundedCornerShape(8.dp))

                            .background(if (isSelected) primaryColor else Color.Transparent)

                            .clickable { dashboardTab = tab }

                            .padding(vertical = 10.dp),

                        contentAlignment = Alignment.Center

                    ) {

                        Text(

                            text = if (tab == "Current") "⚡ Current Orders" else "✅ Completed Orders",

                            color = if (isSelected) Color.White else textSecondary,

                            fontWeight = FontWeight.Bold,

                            fontSize = 13.sp

                        )

                    }

                }

            }

            Spacer(modifier = Modifier.height(20.dp))

            if (dashboardTab == "Current") {

                // active order or job list

                if (activeJob != null) {

                val job = activeJob!!

                var jobStatus by remember(job.id, job.status) { mutableStateOf(job.status) }

                Text(

                    text = "ACTIVE DELIVERY JOB 🚴",

                    fontWeight = FontWeight.Bold,

                    fontSize = 11.sp,

                    letterSpacing = 1.sp,

                    color = primaryColor,

                    modifier = Modifier.padding(bottom = 10.dp)

                )

                Card(

                    modifier = Modifier.fillMaxWidth(),

                    shape = RoundedCornerShape(20.dp),

                    border = BorderStroke(1.5.dp, primaryColor),

                    colors = CardDefaults.cardColors(containerColor = Color.White)

                ) {

                    var actualBillText by remember { mutableStateOf("") }

                    var enteredOtp by remember { mutableStateOf("") }

                    val actualPrices = remember { mutableStateMapOf<String, String>() }

                    var screenshotState by remember(job.screenshotUri) { mutableStateOf(job.screenshotUri) }

                    val imagePickerLauncher = rememberLauncherForActivityResult(

                        contract = ActivityResultContracts.GetContent()

                    ) { uri: Uri? ->

                        if (uri != null) {

                            job.screenshotUri = uri.toString()

                            screenshotState = uri.toString()

                            Toast.makeText(context, "📸 Screenshot uploaded successfully!", Toast.LENGTH_SHORT).show()

                        }

                    }

                    val cameraLauncher = rememberLauncherForActivityResult(

                        contract = ActivityResultContracts.TakePicturePreview()

                    ) { bitmap: android.graphics.Bitmap? ->

                        if (bitmap != null) {

                            val mockPhotoUri = "content://media/external/images/clicked_package_photo_${System.currentTimeMillis()}.png"

                            job.screenshotUri = mockPhotoUri

                            screenshotState = mockPhotoUri

                            Toast.makeText(context, "📸 Photo clicked and attached!", Toast.LENGTH_SHORT).show()

                        }

                    }

                    Column(modifier = Modifier.padding(16.dp)) {

                        Row(

                            horizontalArrangement = Arrangement.SpaceBetween,

                            verticalAlignment = Alignment.CenterVertically,

                            modifier = Modifier.fillMaxWidth()

                        ) {

                            Text(job.id, fontWeight = FontWeight.ExtraBold, color = primaryColor, fontSize = 15.sp)

                            val earningsText = if (job.isEstimated) {

                                "Earnings: ₹${job.deliveryFee} - ₹${job.deliveryFee + 5}"

                            } else {

                                "Earnings: ₹${job.deliveryFee}"

                            }

                            Text(

                                text = earningsText,

                                fontWeight = FontWeight.Bold,

                                color = Color(0xFF10B981),

                                fontSize = 13.sp

                            )

                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text("Pick point: ${job.pickPoint}", fontSize = 12.sp, color = textSecondary)

                        Text("Drop spot: ${job.dropPoint}", fontSize = 12.sp, color = textSecondary)

                        val isPackageTransfer = job.id.startsWith("OrderNo-") || job.id.startsWith("CSV-")

                        if (isPackageTransfer) {

                            if (jobStatus == "ACCEPTED" || jobStatus == "REACHED_SHOP") {

                                val phone = job.pickupPhone

                                if (!phone.isNullOrBlank()) {

                                    Spacer(modifier = Modifier.height(4.dp))

                                    Text("📞 Contact Pickup: $phone", fontSize = 12.sp, color = primaryColor, fontWeight = FontWeight.Bold)

                                }

                            } else if (jobStatus == "ITEMS_PICKED" || jobStatus == "DELIVERING" || jobStatus == "RIDER_ARRIVED") {

                                val phone = job.dropPhone

                                if (!phone.isNullOrBlank()) {

                                    Spacer(modifier = Modifier.height(4.dp))

                                    Text("📞 Contact Drop-off: $phone", fontSize = 12.sp, color = primaryColor, fontWeight = FontWeight.Bold)

                                }

                            }

                        }

                        if (isPackageTransfer) {

                            Spacer(modifier = Modifier.height(12.dp))

                            Button(

                                onClick = {

                                    job.status = "CANCELLED"

                                    // Credit compensation payout to Runner

                                    WalletManager.credit(context, 20, "Violating Order Cancelled (Food Surcharge Compensation): ${job.id}", isRider = true)

                                    walletBalance = WalletManager.getBalance(context, isRider = true)

                                    OrderRepository.cancelledOrders.add(job)

                                    OrderRepository.pendingOrders.remove(job)

                                    activeJob = null

                                    OrderRepository.save(context)

                                    SystemNotificationHelper.showNotification(

                                        context,

                                        "Order Violating Cancelled ❌",

                                        "Order ${job.id.replace("OrderNo-", "").replace("CSV-", "")} has been cancelled by the runner due to food policy violation."

                                    )

                                    Toast.makeText(context, "🚫 Food transfer violation reported. Job cancelled, ₹20 compensation credited.", Toast.LENGTH_LONG).show()

                                },

                                colors = ButtonDefaults.buttonColors(containerColor = AppTheme.Danger),

                                shape = RoundedCornerShape(12.dp),

                                modifier = Modifier.fillMaxWidth().height(42.dp)

                            ) {

                                Text(

                                    text = "Report Food Item & Cancel Violation",

                                    color = Color.White,

                                    fontWeight = FontWeight.Bold,

                                    fontSize = 12.sp

                                )

                            }

                        }

                        val payAtShop = if (job.itemsSubtotal > 0) job.itemsSubtotal else (job.totalMin - job.deliveryFee - job.gatePremium - 2).coerceAtLeast(0)

                        if (jobStatus == "ACCEPTED") {

                            // Countdown timer logic based on system timestamp

                            LaunchedEffect(jobStatus, job.etaTimestamp) {

                                while (jobStatus == "ACCEPTED" && job.etaTimestamp > System.currentTimeMillis()) {

                                    job.etaSeconds = ((job.etaTimestamp - System.currentTimeMillis()) / 1000).coerceAtLeast(0).toInt()

                                    kotlinx.coroutines.delay(500L)

                                }

                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Timer display

                            val minutes = job.etaSeconds / 60

                            val secs = job.etaSeconds % 60

                            val timerString = String.format(Locale.US, "%02d:%02d", minutes, secs)

                            Column(

                                modifier = Modifier

                                    .fillMaxWidth()

                                    .background(primaryColor.copy(alpha = 0.05f), RoundedCornerShape(12.dp))

                                    .border(1.dp, primaryColor.copy(alpha = 0.15f), RoundedCornerShape(12.dp))

                                    .padding(16.dp),

                                horizontalAlignment = Alignment.CenterHorizontally

                            ) {

                                Text(

                                    text = if (isPackageTransfer) "Estimated Time to Pickup Location" else "Estimated Time to Shop",

                                    fontSize = 12.sp,

                                    fontWeight = FontWeight.Medium,

                                    color = textSecondary

                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                Text(

                                    text = timerString,

                                    fontSize = 32.sp,

                                    fontWeight = FontWeight.ExtraBold,

                                    color = primaryColor

                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                // Bouncing travel animation with breeze lines

                                val infiniteTransition = rememberInfiniteTransition(label = "transit")

                                val bounceX by infiniteTransition.animateFloat(

                                    initialValue = 0f,

                                    targetValue = 18f,

                                    animationSpec = infiniteRepeatable(

                                        animation = tween(800, easing = LinearEasing),

                                        repeatMode = RepeatMode.Reverse

                                    ),

                                    label = "bounceX"

                                )

                                val windOffset1 by infiniteTransition.animateFloat(

                                    initialValue = 15f,

                                    targetValue = -25f,

                                    animationSpec = infiniteRepeatable(

                                        animation = tween(500, easing = LinearEasing),

                                        repeatMode = RepeatMode.Restart

                                    ),

                                    label = "wind1"

                                )

                                val windOffset2 by infiniteTransition.animateFloat(

                                    initialValue = 25f,

                                    targetValue = -15f,

                                    animationSpec = infiniteRepeatable(

                                        animation = tween(700, easing = LinearEasing),

                                        repeatMode = RepeatMode.Restart

                                    ),

                                    label = "wind2"

                                )

                                Box(

                                    modifier = Modifier.fillMaxWidth().height(80.dp),

                                    contentAlignment = Alignment.Center

                                ) {

                                    Row(verticalAlignment = Alignment.CenterVertically) {

                                        // Animated Breeze Trails (blown backwards behind rider)

                                        Box(modifier = Modifier.width(50.dp).height(30.dp)) {

                                            Text("—", color = primaryColor.copy(alpha = 0.25f), fontSize = 16.sp, modifier = Modifier.offset(x = windOffset1.dp, y = 2.dp))

                                            Text("~~", color = primaryColor.copy(alpha = 0.15f), fontSize = 12.sp, modifier = Modifier.offset(x = windOffset2.dp, y = 12.dp))

                                            Text("—", color = primaryColor.copy(alpha = 0.25f), fontSize = 16.sp, modifier = Modifier.offset(x = (windOffset1 - 8f).dp, y = 22.dp))

                                        }

                                        Spacer(modifier = Modifier.width(6.dp))

                                        // Rider bobbing up/down & flipped to face right

                                        Text(

                                            text = "🚴", 

                                            fontSize = 34.sp, 

                                            modifier = Modifier

                                                .scale(scaleX = -1f, scaleY = 1f) // Face right

                                                .offset(y = (if (bounceX.toInt() % 2 == 0) 2 else 0).dp)

                                        )

                                        Spacer(modifier = Modifier.width(16.dp))

                                        Text(if (isPackageTransfer) "📍" else "🏪", fontSize = 36.sp)

                                    }

                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                // Red Warning Penalization Note

                                Text(

                                    text = if (isPackageTransfer) "⚠️ Note: If you delay reaching the pickup location, you might be penalised." else "⚠️ Note: If you delay reaching the shop, you might be penalised.",

                                    color = Color.Red,

                                    fontWeight = FontWeight.SemiBold,

                                    fontSize = 11.sp,

                                    textAlign = TextAlign.Center,

                                    modifier = Modifier.fillMaxWidth()

                                )

                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Reached Shop / Pickup Location Button

                            Button(

                                onClick = {

                                    job.status = "REACHED_SHOP"

                                    jobStatus = "REACHED_SHOP"

                                    OrderRepository.save(context)

                                    if (isPackageTransfer) {

                                        SystemNotificationHelper.showNotification(

                                            context,

                                            "Rider Arrived at Pickup 📦",

                                            "Rider Rahul arrived at the pickup spot to collect your package!"

                                        )

                                        Toast.makeText(context, "Arrived at Pickup! Please upload package photo.", Toast.LENGTH_SHORT).show()

                                    } else {

                                        SystemNotificationHelper.showNotification(

                                            context,

                                            "Rider Reached Shop 🏪",

                                            "Rider Rahul reached the store and is purchasing your items!"

                                        )

                                        Toast.makeText(context, "Shop Reached! Check off checklist items.", Toast.LENGTH_SHORT).show()

                                    }

                                },

                                colors = ButtonDefaults.buttonColors(containerColor = primaryColor),

                                shape = RoundedCornerShape(12.dp),

                                modifier = Modifier.fillMaxWidth().height(48.dp)

                            ) {

                                Text(if (isPackageTransfer) "Arrived at Pickup Location" else "Reached Shop", color = Color.White, fontWeight = FontWeight.Bold)

                            }

                        } else if (jobStatus == "REACHED_SHOP") {

                            if (isPackageTransfer) {

                                // Package Collection Photo Upload Screen

                                Column(modifier = Modifier.fillMaxWidth().padding(top = 10.dp)) {

                                    Text(

                                        text = "PACKAGE COLLECTION 📦",

                                        fontWeight = FontWeight.Bold,

                                        fontSize = 12.sp,

                                        color = primaryColor,

                                        modifier = Modifier.padding(bottom = 6.dp)

                                    )

                                    Text(

                                        text = "Please take a photo of the package/product to verify collection before proceeding.",

                                        fontSize = 11.sp,

                                        color = textSecondary,

                                        modifier = Modifier.padding(bottom = 12.dp)

                                    )

                                    if (screenshotState == null) {

                                        Button(

                                            onClick = {

                                                try {

                                                    cameraLauncher.launch(null)

                                                } catch (e: Exception) {

                                                    job.screenshotUri = "content://media/external/images/mock_package_photo.png"

                                                    screenshotState = "content://media/external/images/mock_package_photo.png"

                                                    Toast.makeText(context, "📸 Photo clicked!", Toast.LENGTH_SHORT).show()

                                                }

                                            },

                                            colors = ButtonDefaults.buttonColors(containerColor = primaryColor.copy(alpha = 0.1f)),

                                            shape = RoundedCornerShape(10.dp),

                                            modifier = Modifier.fillMaxWidth().height(44.dp)

                                        ) {

                                            Text("Click Package Photo 📷", color = primaryColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)

                                        }

                                    } else {

                                        Row(

                                            modifier = Modifier

                                                .fillMaxWidth()

                                                .background(Color(0xFFECFDF5), RoundedCornerShape(10.dp))

                                                .border(1.dp, Color(0xFFECFDF5), RoundedCornerShape(10.dp))

                                                .padding(10.dp),

                                            horizontalArrangement = Arrangement.SpaceBetween,

                                            verticalAlignment = Alignment.CenterVertically

                                        ) {

                                            Row(verticalAlignment = Alignment.CenterVertically) {

                                                Text("🖼️", fontSize = 16.sp)

                                                Spacer(modifier = Modifier.width(8.dp))

                                                Column {

                                                    val uriName = screenshotState!!.split("/").lastOrNull() ?: "package_photo.jpg"

                                                    val truncatedName = if (uriName.length > 25) uriName.take(12) + "..." + uriName.takeLast(8) else uriName

                                                    Text(truncatedName, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = textPrimary)

                                                    Text("Package Photo Captured • Ready", fontSize = 10.sp, color = textSecondary)

                                                }

                                            }

                                            TextButton(onClick = { 

                                                job.screenshotUri = null

                                                screenshotState = null 

                                            }) {

                                                Text("Remove", color = Color.Red, fontSize = 11.sp, fontWeight = FontWeight.Bold)

                                            }

                                        }

                                    }

                                    Spacer(modifier = Modifier.height(16.dp))

                                    Button(

                                        onClick = {

                                            job.status = "ITEMS_PICKED"

                                            jobStatus = "ITEMS_PICKED"

                                            OrderRepository.save(context)

                                            val formattedId = job.id.replace("OrderNo-", "").replace("CSV-", "")

                                            SystemNotificationHelper.showNotification(

                                                context,

                                                "Package Picked Up 📦",

                                                "Rider Rahul has picked up your package for Transfer $formattedId and is preparing to deliver!"

                                            )

                                            Toast.makeText(context, "Package picked up! Choose delivery ETA.", Toast.LENGTH_SHORT).show()

                                        },

                                        enabled = (screenshotState != null),

                                        colors = ButtonDefaults.buttonColors(containerColor = primaryColor),

                                        shape = RoundedCornerShape(12.dp),

                                        modifier = Modifier.fillMaxWidth().height(48.dp)

                                    ) {

                                        Text("Confirm Pickup & Proceed ➔", color = Color.White, fontWeight = FontWeight.Bold)

                                    }

                                }

                            } else {

                                // Checklist Purchase Screen

                                Spacer(modifier = Modifier.height(10.dp))

                                Row(

                                    modifier = Modifier

                                        .fillMaxWidth()

                                        .background(Color(0xFFFEF2F2), RoundedCornerShape(8.dp))

                                        .border(1.dp, Color(0xFFFEE2E2), RoundedCornerShape(8.dp))

                                        .padding(horizontal = 12.dp, vertical = 8.dp),

                                    horizontalArrangement = Arrangement.SpaceBetween,

                                    verticalAlignment = Alignment.CenterVertically

                                ) {

                                    Text("Total to Pay Canteen:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFFEF4444))

                                    Text("₹$payAtShop", fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, color = Color(0xFFEF4444))

                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                Text("Purchase Checklist:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = textPrimary)

                                job.items.forEach { (name, qty) ->

                                    val isItemCancelled = job.cancelledItems.contains(name)

                                    var checked by remember { mutableStateOf(false) }

                                    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {

                                        Row(

                                            verticalAlignment = Alignment.CenterVertically,

                                            modifier = Modifier.fillMaxWidth()

                                        ) {

                                            Checkbox(

                                                checked = checked && !isItemCancelled,

                                                onCheckedChange = { if (!isItemCancelled) checked = it },

                                                enabled = !isItemCancelled,

                                                colors = CheckboxDefaults.colors(checkedColor = primaryColor)

                                            )

                                            Column(modifier = Modifier.weight(1f)) {

                                                Text(

                                                    text = "$name (x$qty)",

                                                    fontSize = 12.sp,

                                                    fontWeight = FontWeight.SemiBold,

                                                    color = if (isItemCancelled) Color.Gray else textPrimary,

                                                    style = androidx.compose.ui.text.TextStyle(

                                                        textDecoration = if (isItemCancelled) androidx.compose.ui.text.style.TextDecoration.LineThrough else androidx.compose.ui.text.style.TextDecoration.None

                                                    )

                                                )

                                                val estPrice = job.itemPrices[name] ?: 0

                                                Text(

                                                    text = "Paid: ₹$estPrice/unit • Total: ₹${estPrice * qty}",

                                                    fontSize = 10.sp,

                                                    color = Color.Gray

                                                )

                                            }

                                            if (!isItemCancelled) {

                                                var actualPriceText by remember(name) { mutableStateOf("") }

                                                OutlinedTextField(

                                                    value = actualPriceText,

                                                    onValueChange = { 

                                                        actualPriceText = it.filter { c -> c.isDigit() }

                                                        actualPrices[name] = actualPriceText

                                                    },

                                                    label = { Text("Actual Item Price", fontSize = 8.sp) },

                                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),

                                                    singleLine = true,

                                                    shape = RoundedCornerShape(8.dp),

                                                    modifier = Modifier.width(135.dp),

                                                    colors = OutlinedTextFieldDefaults.colors(

                                                        focusedContainerColor = Color.White,

                                                        unfocusedContainerColor = Color.White,

                                                        focusedLabelColor = primaryColor,

                                                        unfocusedLabelColor = Color.Gray

                                                    )

                                                )

                                            } else {

                                                Text(

                                                    text = "Cancelled",

                                                    color = Color(0xFFEF4444),

                                                    fontSize = 10.sp,

                                                    fontWeight = FontWeight.Bold,

                                                    modifier = Modifier.padding(end = 4.dp)

                                                )

                                            }

                                        }

                                    }

                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                Text("Payment Verification Photo:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = textPrimary)

                                Spacer(modifier = Modifier.height(6.dp))

                                if (screenshotState == null) {

                                    Button(

                                        onClick = {

                                            try {

                                                cameraLauncher.launch(null)

                                            } catch (e: Exception) {

                                                job.screenshotUri = "content://media/external/images/mock_payment_screenshot.png"

                                                screenshotState = "content://media/external/images/mock_payment_screenshot.png"

                                                Toast.makeText(context, "📸 Photo clicked and attached!", Toast.LENGTH_SHORT).show()

                                            }

                                        },

                                        colors = ButtonDefaults.buttonColors(containerColor = primaryColor.copy(alpha = 0.1f)),

                                        shape = RoundedCornerShape(10.dp),

                                        modifier = Modifier.fillMaxWidth().height(40.dp)

                                    ) {

                                        Text("Click Payment Photo 📷", color = primaryColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)

                                    }

                                } else {

                                    Row(

                                        modifier = Modifier

                                            .fillMaxWidth()

                                            .background(Color(0xFFECFDF5), RoundedCornerShape(10.dp))

                                            .border(1.dp, Color(0xFFECFDF5), RoundedCornerShape(10.dp))

                                            .padding(10.dp),

                                        horizontalArrangement = Arrangement.SpaceBetween,

                                        verticalAlignment = Alignment.CenterVertically

                                    ) {

                                        Row(verticalAlignment = Alignment.CenterVertically) {

                                            Text("🖼️", fontSize = 16.sp)

                                            Spacer(modifier = Modifier.width(8.dp))

                                            Column {

                                                val uriName = screenshotState!!.split("/").lastOrNull() ?: "screenshot.jpg"

                                                val truncatedName = if (uriName.length > 25) uriName.take(12) + "..." + uriName.takeLast(8) else uriName

                                                Text(truncatedName, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = textPrimary)

                                                Text("Payment Photo Captured • Ready", fontSize = 10.sp, color = textSecondary)

                                            }

                                        }

                                        TextButton(onClick = { 

                                            job.screenshotUri = null

                                            screenshotState = null 

                                        }) {

                                            Text("Remove", color = Color.Red, fontSize = 11.sp, fontWeight = FontWeight.Bold)

                                        }

                                    }

                                }

                                if (job.isEstimated) {

                                    Spacer(modifier = Modifier.height(12.dp))

                                    OutlinedTextField(

                                        value = actualBillText,

                                        onValueChange = { actualBillText = it.filter { c -> c.isDigit() } },

                                        label = { Text("Enter Counter Bill Amount (₹)") },

                                        placeholder = { Text("e.g. 145") },

                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),

                                        shape = RoundedCornerShape(12.dp),

                                        colors = OutlinedTextFieldDefaults.colors(

                                            focusedBorderColor = primaryColor,

                                            unfocusedBorderColor = Color(0xFFE2E8F0)

                                        ),

                                        modifier = Modifier.fillMaxWidth()

                                    )

                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                val totalExtraAmount = remember(actualPrices.size, refreshTrigger, actualPrices.values.toList()) {

                                    var extra = 0

                                    job.items.forEach { (name, qty) ->

                                        if (!job.cancelledItems.contains(name)) {

                                            val estPrice = job.itemPrices[name] ?: 0

                                            val actPrice = actualPrices[name]?.toIntOrNull() ?: estPrice

                                            if (actPrice > estPrice) {

                                                extra += (actPrice - estPrice) * qty

                                            }

                                        }

                                    }

                                    extra

                                }

                                if (totalExtraAmount > 0) {

                                    Card(

                                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),

                                        shape = RoundedCornerShape(8.dp),

                                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFBEB)),

                                        border = BorderStroke(1.dp, Color(0xFFF59E0B))

                                    ) {

                                        Text(

                                            text = "⚠️ Actual store prices exceed the user's estimated price by a total of ₹$totalExtraAmount. You must request extra payment from the student.",

                                            fontSize = 11.sp,

                                            color = Color(0xFF92400E),

                                            modifier = Modifier.padding(10.dp)

                                        )

                                    }

                                    Button(

                                        onClick = {

                                            job.proposedItemsSubtotal = job.itemsSubtotal + totalExtraAmount

                                            job.proposedTotalMax = job.totalMax + totalExtraAmount

                                            job.proposedItemPrices = job.items.associate { (name, qty) ->

                                                val estPrice = job.itemPrices[name] ?: 0

                                                val actPrice = actualPrices[name]?.toIntOrNull() ?: estPrice

                                                name to actPrice

                                            }

                                            job.priceCorrectionExpiryTimestamp = System.currentTimeMillis() + 5 * 60 * 1000L

                                            job.status = "PRICE_CORRECTION_PENDING"

                                            jobStatus = "PRICE_CORRECTION_PENDING"

                                            OrderRepository.save(context)

                                            SystemNotificationHelper.showNotification(

                                                context,

                                                "Price Correction Required ⚠️",

                                                "The rider requested a price correction for your order ${job.id}. Original: ₹${job.totalMax}, New: ₹${job.proposedTotalMax}."

                                            )

                                            Toast.makeText(context, "Correction Request Submitted! Awaiting student approval.", Toast.LENGTH_LONG).show()

                                        },

                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706)),

                                        shape = RoundedCornerShape(12.dp),

                                        modifier = Modifier.fillMaxWidth().height(48.dp)

                                    ) {

                                        Text("Request Extra Payment (₹$totalExtraAmount)", color = Color.White, fontWeight = FontWeight.Bold)

                                    }

                                } else {

                                    Button(

                                        onClick = {

                                            job.status = "ITEMS_PICKED"

                                            jobStatus = "ITEMS_PICKED"

                                            OrderRepository.save(context)

                                            SystemNotificationHelper.showNotification(

                                                context,

                                                "Items Picked Up 🛍️",

                                                "Rider Rahul picked up your items and is preparing to dispatch!"

                                            )

                                            Toast.makeText(context, "Items Picked! Choose delivery ETA.", Toast.LENGTH_SHORT).show()

                                        },

                                        enabled = (screenshotState != null),

                                        colors = ButtonDefaults.buttonColors(containerColor = primaryColor),

                                        shape = RoundedCornerShape(12.dp),

                                        modifier = Modifier.fillMaxWidth().height(48.dp)

                                    ) {

                                        Text("Items Picked", color = Color.White, fontWeight = FontWeight.Bold)

                                    }

                                }

                            }

                        } else if (jobStatus == "PRICE_CORRECTION_PENDING") {

                            var secondsRemaining by remember(job.priceCorrectionExpiryTimestamp) {

                                mutableStateOf(((job.priceCorrectionExpiryTimestamp - System.currentTimeMillis()) / 1000).coerceAtLeast(0).toInt())

                            }

                            LaunchedEffect(job.priceCorrectionExpiryTimestamp) {

                                while (true) {

                                    val diff = ((job.priceCorrectionExpiryTimestamp - System.currentTimeMillis()) / 1000).toInt()

                                    secondsRemaining = diff.coerceAtLeast(0)

                                    if (diff <= 0) {

                                        job.status = "CANCELLED"

                                        jobStatus = "CANCELLED"

                                        val refundVal = (job.totalMax - 15).coerceAtLeast(0)

                                        if (refundVal > 0) {

                                            WalletManager.credit(context, refundVal, "Refund for Cancelled Order (Minus ₹15 Penalty): ${job.id}", isRider = false)

                                        }

                                        WalletManager.credit(context, 15, "Cancellation Compensation: ${job.id}", isRider = true)

                                        OrderRepository.cancelledOrders.add(job)

                                        OrderRepository.pendingOrders.remove(job)

                                        activeJob = null

                                        OrderRepository.save(context)

                                        SystemNotificationHelper.showNotification(

                                            context,

                                            "Order Cancelled ❌",

                                            "Order ${job.id} was automatically cancelled due to price correction timeout."

                                        )

                                        break

                                    }

                                    kotlinx.coroutines.delay(500L)

                                }

                            }

                            val mins = secondsRemaining / 60

                            val secs = secondsRemaining % 60

                            val timerString = String.format(java.util.Locale.US, "%02d:%02d", mins, secs)

                            Spacer(modifier = Modifier.height(16.dp))

                            Card(

                                modifier = Modifier

                                    .fillMaxWidth()

                                    .padding(vertical = 8.dp),

                                shape = RoundedCornerShape(16.dp),

                                border = BorderStroke(1.5.dp, Color(0xFF3B82F6)),

                                colors = CardDefaults.cardColors(containerColor = Color(0xFFEFF6FF))

                            ) {

                                Column(

                                    modifier = Modifier.padding(16.dp),

                                    horizontalAlignment = Alignment.CenterHorizontally

                                ) {

                                    CircularProgressIndicator(color = Color(0xFF3B82F6), modifier = Modifier.size(36.dp))

                                    Spacer(modifier = Modifier.height(16.dp))

                                    Text(

                                        text = "Price Correction Requested",

                                        fontWeight = FontWeight.Bold,

                                        fontSize = 15.sp,

                                        color = Color(0xFF1E3A8A),

                                        textAlign = TextAlign.Center

                                    )

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Text(

                                        text = "Waiting for the student to approve and pay the difference of ₹${job.proposedTotalMax - job.totalMax} (Actual: ₹${job.proposedItemsSubtotal} vs Est: ₹${job.itemsSubtotal}).\n\nTimeout countdown: $timerString\n\nIf the timer runs out without payment, the order will cancel automatically.",

                                        fontSize = 12.sp,

                                        color = Color(0xFF1D4ED8),

                                        textAlign = TextAlign.Center

                                    )

                                }

                            }

                        } else if (jobStatus == "ITEMS_PICKED") {

                            // ETA Selection for Drop Location

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(

                                text = "Choose travel time to reach drop location:",

                                fontWeight = FontWeight.Bold,

                                fontSize = 14.sp,

                                color = textPrimary

                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            Row(

                                modifier = Modifier.fillMaxWidth(),

                                horizontalArrangement = Arrangement.spacedBy(16.dp)

                            ) {

                                // 7 mins button

                                Button(

                                    onClick = {

                                        job.etaSeconds = 7 * 60

                                        job.etaTimestamp = System.currentTimeMillis() + 7 * 60 * 1000

                                        job.status = "DELIVERING"

                                        jobStatus = "DELIVERING"

                                        OrderRepository.save(context)

                                        SystemNotificationHelper.showNotification(

                                            context,

                                            "Order Out for Delivery 🛵",

                                            "Rider is on the way! Estimated to arrive at your spot in 7 minutes."

                                        )

                                        Toast.makeText(context, "ETA set to 7 Mins. Heading to drop spot!", Toast.LENGTH_SHORT).show()

                                    },

                                    modifier = Modifier.weight(1f).height(48.dp),

                                    colors = ButtonDefaults.buttonColors(containerColor = primaryColor),

                                    shape = RoundedCornerShape(12.dp)

                                ) {

                                    Text("7 Mins", fontWeight = FontWeight.Bold, color = Color.White)

                                }

                                // 15 mins button

                                Button(

                                    onClick = {

                                        job.etaSeconds = 15 * 60

                                        job.etaTimestamp = System.currentTimeMillis() + 15 * 60 * 1000

                                        job.status = "DELIVERING"

                                        jobStatus = "DELIVERING"

                                        OrderRepository.save(context)

                                        SystemNotificationHelper.showNotification(

                                            context,

                                            "Order Out for Delivery 🛵",

                                            "Rider is on the way! Estimated to arrive at your spot in 15 minutes."

                                        )

                                        Toast.makeText(context, "ETA set to 15 Mins. Heading to drop spot!", Toast.LENGTH_SHORT).show()

                                    },

                                    modifier = Modifier.weight(1f).height(48.dp),

                                    colors = ButtonDefaults.buttonColors(containerColor = primaryColor),

                                    shape = RoundedCornerShape(12.dp)

                                ) {

                                    Text("15 Mins", fontWeight = FontWeight.Bold, color = Color.White)

                                }

                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Delay Warning Note

                            Text(

                                text = "⚠️ Note: If you delay reaching the drop location, you might be penalised.",

                                color = Color.Red,

                                fontWeight = FontWeight.SemiBold,

                                fontSize = 11.sp,

                                textAlign = TextAlign.Center,

                                modifier = Modifier.fillMaxWidth()

                            )

                        } else if (jobStatus == "DELIVERING") {

                            // Stage 1: Transit to Drop Location Page

                            LaunchedEffect(jobStatus, job.etaTimestamp) {

                                while (jobStatus == "DELIVERING" && job.etaTimestamp > System.currentTimeMillis()) {

                                    job.etaSeconds = ((job.etaTimestamp - System.currentTimeMillis()) / 1000).coerceAtLeast(0).toInt()

                                    kotlinx.coroutines.delay(500L)

                                }

                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Timer display

                            val minutes = job.etaSeconds / 60

                            val secs = job.etaSeconds % 60

                            val timerString = String.format(Locale.US, "%02d:%02d", minutes, secs)

                            Column(

                                modifier = Modifier

                                    .fillMaxWidth()

                                    .background(primaryColor.copy(alpha = 0.05f), RoundedCornerShape(12.dp))

                                    .border(1.dp, primaryColor.copy(alpha = 0.15f), RoundedCornerShape(12.dp))

                                    .padding(16.dp),

                                horizontalAlignment = Alignment.CenterHorizontally

                            ) {

                                Text(

                                    text = "Estimated Time to Reach Drop Location",

                                    fontSize = 12.sp,

                                    fontWeight = FontWeight.Medium,

                                    color = textSecondary

                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                Text(

                                    text = timerString,

                                    fontSize = 32.sp,

                                    fontWeight = FontWeight.ExtraBold,

                                    color = primaryColor

                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                // Bouncing travel animation with breeze lines heading towards customer spot (house icon)

                                val infiniteTransition = rememberInfiniteTransition(label = "transit")

                                val bounceX by infiniteTransition.animateFloat(

                                    initialValue = 0f,

                                    targetValue = 18f,

                                    animationSpec = infiniteRepeatable(

                                        animation = tween(800, easing = LinearEasing),

                                        repeatMode = RepeatMode.Reverse

                                    ),

                                    label = "bounceX"

                                )

                                val windOffset1 by infiniteTransition.animateFloat(

                                    initialValue = 15f,

                                    targetValue = -25f,

                                    animationSpec = infiniteRepeatable(

                                        animation = tween(500, easing = LinearEasing),

                                        repeatMode = RepeatMode.Restart

                                    ),

                                    label = "wind1"

                                )

                                val windOffset2 by infiniteTransition.animateFloat(

                                    initialValue = 25f,

                                    targetValue = -15f,

                                    animationSpec = infiniteRepeatable(

                                        animation = tween(700, easing = LinearEasing),

                                        repeatMode = RepeatMode.Restart

                                    ),

                                    label = "wind2"

                                )

                                Box(

                                    modifier = Modifier.fillMaxWidth().height(80.dp),

                                    contentAlignment = Alignment.Center

                                ) {

                                    Row(verticalAlignment = Alignment.CenterVertically) {

                                        // Animated Breeze Trails

                                        Box(modifier = Modifier.width(50.dp).height(30.dp)) {

                                            Text("—", color = primaryColor.copy(alpha = 0.25f), fontSize = 16.sp, modifier = Modifier.offset(x = windOffset1.dp, y = 2.dp))

                                            Text("~~", color = primaryColor.copy(alpha = 0.15f), fontSize = 12.sp, modifier = Modifier.offset(x = windOffset2.dp, y = 12.dp))

                                            Text("—", color = primaryColor.copy(alpha = 0.25f), fontSize = 16.sp, modifier = Modifier.offset(x = (windOffset1 - 8f).dp, y = 22.dp))

                                        }

                                        Spacer(modifier = Modifier.width(6.dp))

                                        // Rider

                                        Text(

                                            text = "🚴", 

                                            fontSize = 34.sp, 

                                            modifier = Modifier

                                                .scale(scaleX = -1f, scaleY = 1f) // Face right

                                                .offset(y = (if (bounceX.toInt() % 2 == 0) 2 else 0).dp)

                                        )

                                        Spacer(modifier = Modifier.width(16.dp))

                                        Text("🏠", fontSize = 36.sp)

                                    }

                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                // Red Warning Note

                                Text(

                                    text = "⚠️ Note: If you delay reaching the drop location, you might be penalised.",

                                    color = Color.Red,

                                    fontWeight = FontWeight.SemiBold,

                                    fontSize = 11.sp,

                                    textAlign = TextAlign.Center,

                                    modifier = Modifier.fillMaxWidth()

                                )

                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // "Reached Location" / "Arrived at Drop Location" Button

                            Button(

                                onClick = {

                                    job.status = "RIDER_ARRIVED"

                                    jobStatus = "RIDER_ARRIVED"

                                    OrderRepository.save(context)

                                    SystemNotificationHelper.showNotification(

                                        context,

                                        "Rider Arrived 🚪",

                                        if (isPackageTransfer) {

                                            "Rider Rahul arrived at your drop location! Share OTP ${job.deliveryOtp} to complete package delivery."

                                        } else {

                                            "Rider Rahul arrived at your spot! Share OTP ${job.deliveryOtp} to collect your package."

                                        }

                                    )

                                    Toast.makeText(context, if (isPackageTransfer) "Drop Location Reached! Enter Verification OTP." else "Location Reached! Enter Verification OTP.", Toast.LENGTH_SHORT).show()

                                },

                                colors = ButtonDefaults.buttonColors(containerColor = primaryColor),

                                shape = RoundedCornerShape(12.dp),

                                modifier = Modifier.fillMaxWidth().height(48.dp)

                            ) {

                                Text(if (isPackageTransfer) "Arrived at Drop Location" else "Reached Location", color = Color.White, fontWeight = FontWeight.Bold)

                            }

                        } else {

                            // job.status == "RIDER_ARRIVED"

                            // Stage 2: OTP Verification & Swipe to Deliver Screen

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(

                                text = "Verify Customer OTP to Complete Delivery:",

                                fontWeight = FontWeight.Bold,

                                fontSize = 13.sp,

                                color = textPrimary,

                                modifier = Modifier.fillMaxWidth()

                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            OutlinedTextField(

                                value = enteredOtp,

                                onValueChange = { enteredOtp = it.filter { c -> c.isDigit() } },

                                label = { Text("Enter Delivery Verification OTP") },

                                placeholder = { Text("6-digit code from customer") },

                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),

                                shape = RoundedCornerShape(12.dp),

                                colors = OutlinedTextFieldDefaults.colors(

                                    focusedBorderColor = primaryColor,

                                    unfocusedBorderColor = Color(0xFFE2E8F0)

                                ),

                                modifier = Modifier.fillMaxWidth()

                            )

                            Spacer(modifier = Modifier.height(20.dp))

                            // Swipe to Deliver Button

                            val payout = job.deliveryFee

                            SwipeToPlaceOrderButton(

                                enabled = true,

                                onSubmit = {

                                    if (enteredOtp.trim() != job.deliveryOtp) {

                                        Toast.makeText(context, "❌ Invalid OTP! Please request correct code from customer.", Toast.LENGTH_LONG).show()

                                        return@SwipeToPlaceOrderButton

                                    }

                                    val bill = actualBillText.toIntOrNull() ?: job.totalMin

                                    job.actualBill = bill

                                    job.status = "DELIVERED"

                                    job.completionTimestamp = System.currentTimeMillis()

                                    // Credit Rider Payout

                                    WalletManager.credit(context, payout, "Delivery Payout: ${job.id}", isRider = true)

                                    walletBalance = WalletManager.getBalance(context, isRider = true)

                                    OrderRepository.completedOrders.add(job)

                                    pendingJobs.remove(job)

                                    activeJob = null

                                    OrderRepository.save(context)

                                    SystemNotificationHelper.showNotification(

                                        context,

                                        "Order Delivered 🎉",

                                        "Your order ${job.id} has been successfully delivered. Thank you!"

                                    )

                                    // Notify success

                                    Toast.makeText(context, "🎉 Delivery Completed! Payout of ₹$payout credited to wallet.", Toast.LENGTH_LONG).show()

                                },

                                primaryColor = Color(0xFF10B981),

                                modifier = Modifier.fillMaxWidth(),

                                text = "Swipe to Deliver ➔"

                            )

                        }

                    }

                }

            } else {

                // Job Feed List

                Row(

                    modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),

                    horizontalArrangement = Arrangement.SpaceBetween,

                    verticalAlignment = Alignment.CenterVertically

                ) {

                    Text(

                        text = "AVAILABLE JOBS POOL",

                        fontWeight = FontWeight.Bold,

                        fontSize = 11.sp,

                        letterSpacing = 1.sp,

                        color = Color(0xFF64748B)

                    )

                    Button(

                        onClick = {

                            if (!isRefreshing) {

                                isRefreshing = true

                                scope.launch {

                                    kotlinx.coroutines.delay(1000L)

                                    refreshTrigger++

                                    isRefreshing = false

                                }

                            }

                        },

                        colors = ButtonDefaults.buttonColors(

                            containerColor = Color(0xFFE0F2FE)

                        ),

                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),

                        shape = RoundedCornerShape(20.dp),

                        modifier = Modifier.height(30.dp)

                    ) {

                        Text(

                            text = "🔄", 

                            fontSize = 13.sp,

                            modifier = Modifier.rotate(if (isRefreshing) angle else 0f)

                        )

                        Spacer(modifier = Modifier.width(4.dp))

                        Text(

                            text = if (isRefreshing) "Refreshing..." else "Refresh", 

                            fontSize = 11.sp, 

                            fontWeight = FontWeight.Bold, 

                            color = Color(0xFF0284C7)

                        )

                    }

                }

                if (!isOnline) {

                    Box(

                        modifier = Modifier

                            .fillMaxWidth()

                            .padding(vertical = 40.dp),

                        contentAlignment = Alignment.Center

                    ) {

                        Text("You are offline. Go online to view delivery requests.", color = textSecondary, fontSize = 13.sp)

                    }

                } else if (visibleJobs.isEmpty()) {

                    Box(

                        modifier = Modifier

                            .fillMaxWidth()

                            .padding(vertical = 40.dp),

                        contentAlignment = Alignment.Center

                    ) {

                        Text("No active delivery requests on campus right now.", color = textSecondary, fontSize = 13.sp)

                    }

                } else {

                    Column(

                        verticalArrangement = Arrangement.spacedBy(12.dp),

                        modifier = Modifier.fillMaxWidth()

                    ) {

                        visibleJobs.forEach { job ->

                            JobFeedItem(

                                job = job,

                                onClick = { selectedJob = job },

                                primaryColor = primaryColor

                            )

                        }

                    }

                }

            }

        } else {

            // dashboardTab == "Completed"

            Text(

                text = "DELIVERY HISTORY ✅",

                fontWeight = FontWeight.Bold,

                fontSize = 11.sp,

                letterSpacing = 1.sp,

                color = Color(0xFF0F172A),

                modifier = Modifier.padding(bottom = 10.dp)

            )

            if (completedJobs.isEmpty()) {

                Box(

                    modifier = Modifier

                        .fillMaxWidth()

                        .padding(vertical = 40.dp),

                    contentAlignment = Alignment.Center

                ) {

                    Text("You haven't completed any deliveries yet.", color = textSecondary, fontSize = 13.sp)

                }

            } else {

                val renderCompletedJobCard: @Composable (SimulatedOrder) -> Unit = { completedJob ->

                    Card(

                        modifier = Modifier

                            .fillMaxWidth()

                            .padding(vertical = 6.dp),

                        shape = RoundedCornerShape(12.dp),

                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),

                        colors = CardDefaults.cardColors(containerColor = Color.White)

                    ) {

                        Column(modifier = Modifier.padding(14.dp)) {

                            val isPackageTransfer = completedJob.id.startsWith("OrderNo-") || completedJob.id.startsWith("CSV-")

                            val orderText = if (isPackageTransfer) {

                                "Order No: ${completedJob.id.replace("OrderNo-", "").replace("CSV-", "")}"

                            } else {

                                "Order #${completedJob.id.substringAfter("ORDER-")}"

                            }

                            Row(

                                modifier = Modifier.fillMaxWidth(),

                                horizontalArrangement = Arrangement.SpaceBetween,

                                verticalAlignment = Alignment.CenterVertically

                            ) {

                                Row(verticalAlignment = Alignment.CenterVertically) {

                                    Text(

                                        text = orderText,

                                        fontWeight = FontWeight.Bold,

                                        fontSize = 13.sp,

                                        color = Color(0xFF0F172A)

                                    )

                                    Spacer(modifier = Modifier.width(6.dp))

                                    Box(

                                        modifier = Modifier

                                            .clip(RoundedCornerShape(6.dp))

                                            .background(if (isPackageTransfer) Color(0xFFE0F2FE) else Color(0xFFFFF3DC))

                                            .padding(horizontal = 6.dp, vertical = 2.dp)

                                    ) {

                                        Text(

                                            text = if (isPackageTransfer) "📦 TRANSFER" else "🍔 FOOD",

                                            color = if (isPackageTransfer) Color(0xFF0369A1) else Color(0xFFD97706),

                                            fontSize = 8.sp,

                                            fontWeight = FontWeight.Bold

                                        )

                                    }

                                }

                                Text(

                                    text = "Payout: ₹${completedJob.deliveryFee}",

                                    fontWeight = FontWeight.Bold,

                                    fontSize = 13.sp,

                                    color = Color(0xFF10B981)

                                )

                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            Column {

                                completedJob.items.forEach { (name, qty) ->

                                    val isItemCancelled = completedJob.cancelledItems.contains(name)

                                    Row(

                                        verticalAlignment = Alignment.CenterVertically,

                                        modifier = Modifier.padding(vertical = 1.dp)

                                    ) {

                                        Text("•", fontSize = 11.sp, color = if (isItemCancelled) Color.LightGray else Color.Gray, modifier = Modifier.padding(end = 4.dp))

                                        Text(

                                            text = "$name (x$qty)",

                                            fontSize = 11.sp,

                                            color = if (isItemCancelled) Color.LightGray else Color.Gray,

                                            style = androidx.compose.ui.text.TextStyle(

                                                textDecoration = if (isItemCancelled) androidx.compose.ui.text.style.TextDecoration.LineThrough else androidx.compose.ui.text.style.TextDecoration.None

                                            )

                                        )

                                    }

                                }

                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(

                                text = "Delivered to: ${completedJob.dropPoint}",

                                fontSize = 10.sp,

                                color = Color.Gray

                            )

                        }

                    }

                }

                if (todayJobs.isNotEmpty()) {

                    Text(

                        text = "TODAY'S DELIVERIES (${todayJobs.size}) 📅",

                        fontWeight = FontWeight.Bold,

                        fontSize = 12.sp,

                        color = primaryColor,

                        modifier = Modifier.padding(top = 10.dp, bottom = 6.dp)

                    )

                    todayJobs.forEach { job ->

                        renderCompletedJobCard(job)

                    }

                }

                if (pastJobs.isNotEmpty()) {

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(

                        text = "PAST DELIVERIES (${pastJobs.size}) 📜",

                        fontWeight = FontWeight.Bold,

                        fontSize = 12.sp,

                        color = Color(0xFF64748B),

                        modifier = Modifier.padding(top = 10.dp, bottom = 6.dp)

                    )

                    pastJobs.forEach { job ->

                        renderCompletedJobCard(job)

                    }

                }

            }

        }

    }

}

}

@OptIn(ExperimentalMaterial3Api::class)

@Composable

fun JobFeedItem(

    job: SimulatedOrder,

    onClick: () -> Unit,

    primaryColor: Color

) {

    Card(

        onClick = onClick,

        shape = RoundedCornerShape(16.dp),

        colors = CardDefaults.cardColors(containerColor = Color.White),

        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp),

        modifier = Modifier.fillMaxWidth()

    ) {

        Row(

            modifier = Modifier.padding(16.dp),

            verticalAlignment = Alignment.CenterVertically,

            horizontalArrangement = Arrangement.SpaceBetween

        ) {

            Column(modifier = Modifier.weight(1f)) {

                Row(verticalAlignment = Alignment.CenterVertically) {

                    val isPackageTransfer = job.id.startsWith("OrderNo-") || job.id.startsWith("CSV-")

                    Text(

                        text = if (isPackageTransfer) job.id.replace("OrderNo-", "Order No: ").replace("CSV-", "Order No: ") else job.id,

                        fontWeight = FontWeight.ExtraBold,

                        color = primaryColor,

                        fontSize = 14.sp

                    )

                    Spacer(modifier = Modifier.width(6.dp))

                    Box(

                        modifier = Modifier

                            .background(

                                if (isPackageTransfer) Color(0xFFE0F2FE) else Color(0xFFFFF3DC),

                                RoundedCornerShape(6.dp)

                            )

                            .padding(horizontal = 6.dp, vertical = 2.dp)

                    ) {

                        Text(

                            text = if (isPackageTransfer) "📦 TRANSFER" else "🍔 FOOD",

                            color = if (isPackageTransfer) Color(0xFF0369A1) else Color(0xFFD97706),

                            fontSize = 8.sp,

                            fontWeight = FontWeight.Bold

                        )

                    }

                    if (job.isEstimated) {

                        Spacer(modifier = Modifier.width(6.dp))

                        Text(

                            text = "Estimated Price",

                            fontSize = 8.sp,

                            fontWeight = FontWeight.Bold,

                            color = Color(0xFFD97706),

                            modifier = Modifier

                                .background(Color(0xFFFEF3C7), RoundedCornerShape(4.dp))

                                .padding(horizontal = 4.dp, vertical = 1.dp)

                        )

                    }

                }

                Spacer(modifier = Modifier.height(4.dp))

                Text("Pick: ${job.pickPoint}", fontSize = 12.sp, color = Color(0xFF334155), fontWeight = FontWeight.Medium)

                Text("Drop: ${job.dropPoint}", fontSize = 11.sp, color = Color(0xFF64748B))

            }

            Column(horizontalAlignment = Alignment.End) {

                val earningsText = if (job.isEstimated) {

                    "₹${job.deliveryFee} - ₹${job.deliveryFee + 5}"

                } else {

                    "₹${job.deliveryFee}"

                }

                Text(

                    text = earningsText,

                    fontWeight = FontWeight.ExtraBold,

                    fontSize = 16.sp,

                    color = Color(0xFF10B981)

                )

                Text("Earning", fontSize = 10.sp, color = Color(0xFF94A3B8))

            }

        }

    }

}

@Composable

private fun InfoRow(label: String, value: String, valueColor: Color = Color(0xFF0F172A)) {

    Row(

        modifier = Modifier

            .fillMaxWidth()

            .padding(vertical = 4.dp),

        horizontalArrangement = Arrangement.SpaceBetween

    ) {

        Text(text = label, fontSize = 12.sp, color = Color(0xFF64748B))

        Text(text = value, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = valueColor)

    }

}

@Preview(showBackground = true)

@Composable

fun RiderDashboardPreview() {

    RiderDashboard(onSwitchBack = {})

}

fun detectCurrentCampus(
    context: android.content.Context,
    onResult: (CollegeCampus?) -> Unit,
    onGpsDisabled: () -> Unit
) {
    val locationManager = context.getSystemService(android.content.Context.LOCATION_SERVICE) as android.location.LocationManager
    val isGpsEnabled = locationManager.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER)
    val isNetworkEnabled = locationManager.isProviderEnabled(android.location.LocationManager.NETWORK_PROVIDER)
    if (!isGpsEnabled && !isNetworkEnabled) { onGpsDisabled(); return }
    val provider = if (isNetworkEnabled) android.location.LocationManager.NETWORK_PROVIDER else android.location.LocationManager.GPS_PROVIDER
    try {
        val lastKnown = locationManager.getLastKnownLocation(provider)
            ?: locationManager.getLastKnownLocation(android.location.LocationManager.GPS_PROVIDER)
            ?: locationManager.getLastKnownLocation(android.location.LocationManager.NETWORK_PROVIDER)
        if (lastKnown != null) {
            com.college.campusapp.security.EncryptionManager.saveString(context, "user_last_latitude", lastKnown.latitude.toString())
            com.college.campusapp.security.EncryptionManager.saveString(context, "user_last_longitude", lastKnown.longitude.toString())
            onResult(matchCampus(lastKnown.latitude, lastKnown.longitude)); return
        }
        val handler = android.os.Handler(android.os.Looper.getMainLooper())
        var listener: android.location.LocationListener? = null
        val timeoutRunnable = Runnable {
            listener?.let { locationManager.removeUpdates(it) }
            onResult(matchCampus(37.4220, -122.0841))
        }
        listener = object : android.location.LocationListener {
            override fun onLocationChanged(loc: android.location.Location) {
                handler.removeCallbacks(timeoutRunnable)
                com.college.campusapp.security.EncryptionManager.saveString(context, "user_last_latitude", loc.latitude.toString())
                com.college.campusapp.security.EncryptionManager.saveString(context, "user_last_longitude", loc.longitude.toString())
                onResult(matchCampus(loc.latitude, loc.longitude))
                locationManager.removeUpdates(this)
            }
            override fun onStatusChanged(p: String?, s: Int, e: android.os.Bundle?) {}
            override fun onProviderEnabled(p: String) {}
            override fun onProviderDisabled(p: String) {}
        }
        handler.postDelayed(timeoutRunnable, 2500L)
        locationManager.requestLocationUpdates(provider, 0L, 0f, listener, android.os.Looper.getMainLooper())
    } catch (e: SecurityException) { onResult(null) } catch (e: Exception) { onResult(matchCampus(37.4220, -122.0841)) }
}

data class CollegeCampus(val id: String, val name: String, val latitude: Double, val longitude: Double, val radiusKm: Double)

val CAMPUSES = listOf(
    CollegeCampus("1", "Googleplex Campus", 37.4220, -122.0841, 2.0),
    CollegeCampus("2", "Stanford University", 37.4275, -122.1697, 2.5),
    CollegeCampus("3", "IIT Delhi", 28.5450, 77.1926, 1.5),
    CollegeCampus("4", "BITS Pilani", 28.3639, 75.5866, 1.5),
    CollegeCampus("5", "Delhi Technological University", 28.7501, 77.1177, 1.5)
)

fun calculateDistanceKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val theta = lon1 - lon2
    var dist = Math.sin(Math.toRadians(lat1)) * Math.sin(Math.toRadians(lat2)) +
            Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) * Math.cos(Math.toRadians(theta))
    dist = Math.acos(dist); dist = Math.toDegrees(dist); dist = dist * 60 * 1.1515 * 1.609344
    return if (dist.isNaN()) 0.0 else dist
}

fun matchCampus(lat: Double, lon: Double): CollegeCampus? {
    for (campus in CAMPUSES) { if (calculateDistanceKm(lat, lon, campus.latitude, campus.longitude) <= campus.radiusKm) return campus }
    return null
}
