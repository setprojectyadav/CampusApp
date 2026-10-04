package com.college.campusapp

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.college.campusapp.security.EncryptionManager
import androidx.compose.foundation.clickable
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Brush

import androidx.activity.compose.BackHandler
import java.util.Locale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.draw.clip
import androidx.compose.animation.core.*

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        OrderRepository.load(this)
        
        val action: String? = intent?.action
        val data: android.net.Uri? = intent?.data
        val showShareAddressInit = (android.content.Intent.ACTION_VIEW == action && data != null && data.toString().contains("request-address"))
        setContent {
            var showShareAddressScreen by remember { mutableStateOf(showShareAddressInit) }
            val savedLocation = remember { EncryptionManager.getString(this, "user_verified_college") }
            var showLocationDialog by remember { mutableStateOf(true) }
            var userCollege by remember { mutableStateOf(savedLocation ?: "Select Campus Location") }

            var selectedTab by remember { mutableStateOf(0) }
            var showSettingsDialog by remember { mutableStateOf(false) }
            var currentConsole by remember { mutableStateOf<String?>("student") }

            BackHandler(enabled = currentConsole != null && currentConsole != "student") {
                currentConsole = "student"
            }

            if (showShareAddressScreen) {
                ShareAddressScreen(
                    onBack = { showShareAddressScreen = false },
                    context = this
                )
            }

            if (showLocationDialog) {
                LocationSelectionDialog(
                    currentLocation = userCollege,
                    onLocationSelected = { selected ->
                        userCollege = selected
                        EncryptionManager.saveString(this, "user_verified_college", selected)
                        showLocationDialog = false
                    },
                    onDismiss = {
                        if (userCollege.isNotEmpty() && userCollege != "Select Campus Location") {
                            showLocationDialog = false
                        }
                    },
                    context = this
                )
            }

            when (currentConsole) {
                "student" -> {
                    if (showSettingsDialog) {
                        val email = EncryptionManager.getString(this, "user_email") ?: "N/A"
                        AlertDialog(
                            onDismissRequest = { showSettingsDialog = false },
                            title = {
                                Text(
                                    text = "CampusApp Security Center",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp
                                )
                            },
                            text = {
                                Column {
                                    Text("Active Session Details:\n", fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                                    Text("• Current Student: $email", color = Color(0xFF475569), fontSize = 13.sp)
                                    Text("• Session Storage: EncryptedSharedPreferences (AES-256)", color = Color(0xFF475569), fontSize = 13.sp)
                                    Text("• SSL Status: HTTPS Only (No Cleartext)", color = Color(0xFF475569), fontSize = 13.sp)
                                    Text("• Code Shield: Proguard Obfuscation Active", color = Color(0xFF475569), fontSize = 13.sp)
                                    Text("• Screenshots: Blocked in Sensitive Contexts", color = Color(0xFF475569), fontSize = 13.sp)
                                }
                            },
                            confirmButton = {
                                Button(
                                    onClick = { showSettingsDialog = false },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF5C1D8D))
                                ) {
                                    Text("OK")
                                }
                            },
                            dismissButton = {
                                TextButton(
                                    onClick = {
                                        showSettingsDialog = false
                                        EncryptionManager.clear(this@MainActivity)
                                        val intent = Intent(this@MainActivity, AuthActivity::class.java).apply {
                                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                        }
                                        startActivity(intent)
                                        finish()
                                    }
                                ) {
                                    Text("Sign Out", color = Color.Red)
                                }
                            }
                        )
                    }

                    MainScreenView(
                        selectedTabIndex = selectedTab,
                        onTabSelect = { index ->
                            if (index == 1) {
                                showSettingsDialog = true
                            } else {
                                selectedTab = index
                            }
                        },
                        onCategoryClick = { title ->
                            when (title) {
                                "Campus Mart", "Order Product", "Order a Product" -> {
                                    startActivity(Intent(this, OrderProductActivity::class.java))
                                }
                                "Send and bring items", "Send and bring Items", "Transfer Product", "Transfer a Product" -> {
                                    startActivity(Intent(this, TransferProductActivity::class.java))
                                }
                                "Rent & Host Items", "Rent Product", "Rent a Product" -> {
                                    startActivity(Intent(this, RentProductActivity::class.java))
                                }
                                "Ride Pooling", "Cab & Ride Pooling", "Cab Pooling", "People Gathering for Cab" -> {
                                    startActivity(Intent(this, CabPoolingActivity::class.java))
                                }
                                "Campus Food Club", "Meal Gathering" -> {
                                    startActivity(Intent(this, MealGatheringActivity::class.java))
                                }
                                else -> {
                                    Toast.makeText(this, "$title is coming soon!", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        onBackClick = { currentConsole = null },
                        onLocationClick = { showLocationDialog = true }
                    )
                }
                "rider" -> {
                    RiderDashboard(
                        onSwitchBack = { currentConsole = null },
                        modifier = Modifier.fillMaxSize()
                    )
                }
                "admin" -> {
                    AdminDashboard(
                        onSwitchBack = { currentConsole = null },
                        modifier = Modifier.fillMaxSize()
                    )
                }
                else -> {
                    ConsoleGatewayScreen(
                        onSelectStudent = { currentConsole = "student" },
                        onSelectRider = { currentConsole = "rider" },
                        onSelectAdmin = { currentConsole = "admin" },
                        onBack = { finish() }
                    )
                }
            }
        }
    }
}

data class DashboardCategory(
    val title: String,
    val iconRes: Int,
    val description: String
)

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun MainScreenView(
    selectedTabIndex: Int,
    onTabSelect: (Int) -> Unit,
    onCategoryClick: (String) -> Unit,
    onBackClick: () -> Unit,
    onLocationClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val primaryColor = Color(0xFF2563EB) // Minimalist Cobalt Blue
    val textPrimary = Color(0xFF0F172A)
    val textSecondary = Color(0xFF64748B)

    val currentUserEmail = remember { EncryptionManager.getString(context, "user_email") ?: "student@college.edu" }
    val currentUserName = remember {
        currentUserEmail.substringBefore("@").replace(".", " ").replaceFirstChar {
            if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString()
        }
    }
    var userCollege by remember { mutableStateOf(EncryptionManager.getString(context, "user_verified_college") ?: "Googleplex Campus") }
    var walletBalance by remember { mutableStateOf(WalletManager.getBalance(context, isRider = false)) }

    val bentoServices = remember {
        listOf(
            BentoServiceItem(
                title = "Campus Mart",
                description = "Order food, snacks & daily essentials",
                icon = Icons.Outlined.ShoppingBag,
                badgeText = "15m Drop",
                accentColor = Color(0xFF0066FF),
                cardType = BentoCardType.HERO_WIDE
            ),
            BentoServiceItem(
                title = "Rent & Host Items",
                description = "Rent gear or host your own items & earn",
                icon = Icons.Outlined.Handshake,
                badgeText = "EARN",
                accentColor = Color(0xFF0066FF),
                cardType = BentoCardType.STANDARD_SQUARE
            ),
            BentoServiceItem(
                title = "Send and bring Items",
                description = "Send or request any item brought across campus",
                icon = Icons.Outlined.Inventory2,
                badgeText = "PEER",
                accentColor = Color(0xFF0066FF),
                cardType = BentoCardType.STANDARD_SQUARE
            ),
            BentoServiceItem(
                title = "Ride Pooling",
                description = "Share rides to airport & city with peers",
                icon = Icons.Outlined.DirectionsCar,
                badgeText = "SPLIT",
                accentColor = Color(0xFF0066FF),
                cardType = BentoCardType.STANDARD_SQUARE
            ),
            BentoServiceItem(
                title = "Campus Food Club",
                description = "Group up, order bulk meals together & split delivery",
                icon = Icons.Outlined.Groups,
                badgeText = "0% FEE",
                accentColor = Color(0xFF0066FF),
                cardType = BentoCardType.COMPACT_WIDE
            )
        )
    }

    var showQuickServicesSheet by remember { mutableStateOf(false) }

    if (showQuickServicesSheet) {
        ModalBottomSheet(
            onDismissRequest = { showQuickServicesSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = Color.White,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 8.dp)
            ) {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("All Campus Services", fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF0F172A))
                        Text("Select a service to start", fontSize = 11.sp, color = Color(0xFF64748B))
                    }
                    IconButton(onClick = { showQuickServicesSheet = false }) {
                        Icon(Icons.Outlined.Close, contentDescription = "Close", tint = Color(0xFF64748B))
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Compact 2-Column Grid of All 6 Services (Fits 100% on screen with zero scrolling)
                val allServicesList = listOf(
                    Triple("Campus Mart", "Snacks & Essentials", Icons.Outlined.ShoppingBag to Color(0xFFF97316)),
                    Triple("Campus Rental", "Gear & Tech Rentals", Icons.Outlined.Handshake to Color(0xFF6366F1)),
                    Triple("Campus Store", "Official Merch", Icons.Outlined.Storefront to Color(0xFF0066FF)),
                    Triple("Campus Courier", "Send & Bring Items", Icons.Outlined.Inventory2 to Color(0xFF10B981)),
                    Triple("Campus Cabs", "Cab & Ride Pooling", Icons.Outlined.DirectionsCar to Color(0xFF0284C7)),
                    Triple("Campus Food Club", "Group Meal Orders", Icons.Outlined.Groups to Color(0xFFF43F5E))
                )

                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    allServicesList.chunked(2).forEach { rowItems ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            rowItems.forEach { (title, desc, iconWithColor) ->
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = Color(0xFFF8FAFC),
                                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable {
                                            showQuickServicesSheet = false
                                            onCategoryClick(title)
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(iconWithColor.second),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(iconWithColor.first, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                        }
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(title, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                            Spacer(modifier = Modifier.height(1.dp))
                                            Text(desc, fontSize = 10.sp, color = Color(0xFF64748B), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    Scaffold(
        bottomBar = {
            Surface(
                color = Color.White.copy(alpha = 0.92f),
                shadowElevation = 8.dp,
                shape = RoundedCornerShape(50),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0).copy(alpha = 0.8f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val navItems = listOf(
                        Triple("Home", Icons.Outlined.Home, 0),
                        Triple("Services", Icons.Outlined.GridView, 1),
                        Triple("Activity", Icons.Outlined.Assignment, 2),
                        Triple("Profile", Icons.Outlined.Person, 3)
                    )

                    navItems.forEach { (label, icon, index) ->
                        val isSelected = selectedTabIndex == index
                        val activeContent = Color(0xFF0066FF)
                        val inactiveContent = Color(0xFF64748B)

                        if (isSelected) {
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = Color(0xFFDBEAFE).copy(alpha = 0.65f),
                                border = BorderStroke(1.dp, Color(0xFF93C5FD)),
                                shadowElevation = 2.dp,
                                modifier = Modifier.clickable { onTabSelect(index) }
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = label,
                                        tint = activeContent,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = label,
                                        color = activeContent,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }
                            }
                        } else {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .clickable { onTabSelect(index) }
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = label,
                                    tint = inactiveContent,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = label,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = inactiveContent
                                )
                            }
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (selectedTabIndex == 0) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.White)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 16.dp)
                ) {
                    // Stitch Header Section
                    CampusHeaderSection(
                        userName = currentUserName,
                        college = userCollege,
                        walletBalance = walletBalance.toDouble(),
                        onWalletClick = {
                            context.startActivity(Intent(context, WalletActivity::class.java).apply {
                                putExtra("IS_RIDER", false)
                            })
                        },
                        onLocationClick = onLocationClick
                    )

                    // Active Orders Section Header Row with Right-Aligned Wallet Pill
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "MY ACTIVE ORDERS",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF475569),
                            letterSpacing = 0.5.sp
                        )

                        Surface(
                            shape = RoundedCornerShape(50),
                            color = Color(0xFFDBEAFE).copy(alpha = 0.65f),
                            border = BorderStroke(1.dp, Color(0xFF93C5FD)),
                            shadowElevation = 3.dp,
                            modifier = Modifier.clickable {
                                context.startActivity(Intent(context, WalletActivity::class.java).apply {
                                    putExtra("IS_RIDER", false)
                                })
                            }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.AccountBalanceWallet,
                                    contentDescription = "Wallet Balance",
                                    tint = Color(0xFF0066FF),
                                    modifier = Modifier.size(17.dp)
                                )
                                Text(
                                    text = "₹${walletBalance}0",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFF0066FF)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Unified Data Class for Active Service Items across all features
                    data class CombinedActiveItem(
                        val id: String,
                        val title: String,
                        val category: String,
                        val dueTime: String,
                        val pin: String,
                        val status: String,
                        val serviceType: String
                    )

                    // Read Active Orders across ALL Services continuously with a 2-second real-time ticker
                    var refreshTicker by remember { mutableStateOf(0L) }
                    LaunchedEffect(Unit) {
                        while (true) {
                            OrderRepository.load(context)
                            CabPoolRepository.load(context)
                            RentalRepository.load(context)
                            MealGatheringRepository.load(context)
                            refreshTicker = System.currentTimeMillis()
                            kotlinx.coroutines.delay(2000)
                        }
                    }

                    val userEmail = remember { EncryptionManager.getString(context, "user_email") ?: "" }

                    val activeOrdersList = remember(
                        refreshTicker,
                        OrderRepository.pendingOrders.size,
                        CabPoolRepository.activePools.size,
                        RentalRepository.rentalsList.size,
                        MealGatheringRepository.activeCircles.size,
                        OrderRepository.pendingOrders.map { "${it.id}:${it.status}" },
                        CabPoolRepository.activePools.map { "${it.id}:${it.status}:${it.riders.size}" },
                        RentalRepository.rentalsList.map { "${it.id}:${it.status}" },
                        MealGatheringRepository.activeCircles.map { "${it.id}:${it.status}:${it.members.size}" }
                    ) {
                        val items = mutableListOf<CombinedActiveItem>()

                        // 1. Campus Mart / Food Orders & Peer Package Transfers
                        OrderRepository.pendingOrders.filter {
                            !it.status.equals("DELIVERED", true) &&
                            !it.status.equals("CANCELLED", true) &&
                            !it.status.equals("COMPLETED", true) &&
                            (userEmail.isEmpty() || it.customerId.equals(userEmail, ignoreCase = true))
                        }.forEach { order ->
                            val isTransfer = (order.pickPoint.isNotBlank() && order.dropPoint.isNotBlank()) || order.items.any { it.first.contains("Transfer", ignoreCase = true) || it.first.contains("Peer", ignoreCase = true) || it.first.contains("Courier", ignoreCase = true) || it.first.contains("Package", ignoreCase = true) || it.first.contains("Send", ignoreCase = true) || it.first.contains("Bring", ignoreCase = true) }
                            val rawTitle = order.items.firstOrNull()?.first ?: "Campus Order"
                            val cleanTitle = if (order.items.size > 1) "$rawTitle + ${order.items.size - 1} more" else rawTitle
                            val fullAddressText = if (isTransfer) "Pickup: ${order.pickPoint}\nDrop: ${order.dropPoint}" else "Delivering to: ${order.dropPoint}"

                            val estTimeStr = if (order.totalMin == order.totalMax) "Est. ${order.totalMin} mins" else "Est. ${order.totalMin}-${order.totalMax} mins"
                            items.add(
                                CombinedActiveItem(
                                    id = order.id,
                                    title = cleanTitle,
                                    category = fullAddressText,
                                    dueTime = estTimeStr,
                                    pin = order.deliveryOtp,
                                    status = order.status,
                                    serviceType = if (isTransfer) "TRANSFER" else "ORDER"
                                )
                            )
                        }

                        // 2. Cab Pooling Rides & Cab Hosts
                        CabPoolRepository.activePools.filter { pool ->
                            !pool.status.equals("COMPLETED", true) &&
                            !pool.status.equals("CANCELLED", true) &&
                            !pool.status.equals("DELIVERED", true) &&
                            !pool.isExpired() &&
                            (userEmail.isEmpty() || pool.hostEmail.equals(userEmail, ignoreCase = true) || pool.riders.any { it.email.equals(userEmail, ignoreCase = true) })
                        }.forEach { cab ->
                            // Count unique participants (Host + Riders without double counting)
                            val uniqueParticipants = mutableSetOf<String>()
                            if (cab.hostEmail.isNotBlank()) uniqueParticipants.add(cab.hostEmail)
                            cab.riders.forEach { rider -> if (rider.email.isNotBlank()) uniqueParticipants.add(rider.email) }
                            val totalJoined = if (uniqueParticipants.isNotEmpty()) uniqueParticipants.size else (1 + cab.riders.size).coerceAtMost(cab.totalSeats)
                            val seatLabel = "$totalJoined/${cab.totalSeats} Joined"
                            val fullCabRoute = "Pickup: ${cab.pickupPoint}\nDrop: ${cab.dropDestination} (${cab.routeCategory} • $seatLabel)"
                            items.add(
                                CombinedActiveItem(
                                    id = cab.id,
                                    title = "Cab to ${cab.dropDestination}",
                                    category = fullCabRoute,
                                    dueTime = "Departs ${cab.departureTime}",
                                    pin = cab.id.takeLast(4),
                                    status = if (cab.status == "GATHERING") "ACCEPTED" else cab.status,
                                    serviceType = "CAB"
                                )
                            )
                        }

                        // 3. Rent & Host Items
                        RentalRepository.rentalsList.filter { rental ->
                            !rental.status.equals("RETURNED", true) &&
                            !rental.status.equals("COMPLETED", true) &&
                            !rental.status.equals("CANCELLED", true) &&
                            (userEmail.isEmpty() || rental.renterEmail.equals(userEmail, ignoreCase = true))
                        }.forEach { rental ->
                            val fullRentalLocation = "Rental Category: ${rental.category}"
                            items.add(
                                CombinedActiveItem(
                                    id = rental.id,
                                    title = rental.productName,
                                    category = fullRentalLocation,
                                    dueTime = "Due ${rental.durationDays} Days",
                                    pin = rental.handoverOtp,
                                    status = if (rental.status == "PENDING_COLLECTION") "ACCEPTED" else "ON_THE_WAY",
                                    serviceType = "RENT"
                                )
                            )
                        }

                        // 4. Campus Food Club / Meal Gathering Circles
                        MealGatheringRepository.activeCircles.filter { circle ->
                            !circle.status.equals("COMPLETED", true) &&
                            !circle.status.equals("CANCELLED", true) &&
                            !circle.isExpired() &&
                            (userEmail.isEmpty() || circle.hostEmail.equals(userEmail, ignoreCase = true) || circle.members.any { it.email.equals(userEmail, ignoreCase = true) })
                        }.forEach { circle ->
                            val memberCount = circle.members.size.coerceAtLeast(circle.joinedBuddies)
                            val details = "Meeting: ${circle.meetingPoint}\nSpot: ${circle.restaurantSpot} ($memberCount/${circle.totalBuddies} Joined)"
                            items.add(
                                CombinedActiveItem(
                                    id = circle.id,
                                    title = "Food Club @ ${circle.restaurantSpot.take(20)}",
                                    category = details,
                                    dueTime = "Target ${circle.targetTime}",
                                    pin = circle.id.takeLast(4),
                                    status = if (circle.status == "GATHERING") "ACCEPTED" else circle.status,
                                    serviceType = "MEAL"
                                )
                            )
                        }

                        items
                    }

                    val hasActiveOrders = activeOrdersList.isNotEmpty()

                    if (!hasActiveOrders) {
                        // Display Idle Banner when no active orders
                        LiveActivityTrackerCard(
                            hasActiveOrder = false,
                            onClick = { showQuickServicesSheet = true }
                        )
                    } else {
                        // Multi-Order Horizontal Slider / Carousel using HorizontalPager with 4-second auto-scroll
                        Column {
                            val pagerState = androidx.compose.foundation.pager.rememberPagerState(pageCount = { activeOrdersList.size })

                            // Automatic 4-second auto-slide animation loop when multiple active orders exist
                            LaunchedEffect(activeOrdersList.size) {
                                if (activeOrdersList.size > 1) {
                                    while (true) {
                                        kotlinx.coroutines.delay(4000)
                                        val nextPage = (pagerState.currentPage + 1) % activeOrdersList.size
                                        pagerState.animateScrollToPage(nextPage)
                                    }
                                }
                            }

                            androidx.compose.foundation.pager.HorizontalPager(
                                state = pagerState,
                                pageSpacing = 14.dp,
                                modifier = Modifier.fillMaxWidth()
                            ) { page ->
                                val item = activeOrdersList[page]

                                LiveActivityTrackerCard(
                                    hasActiveOrder = true,
                                    itemName = item.title,
                                    category = item.category,
                                    dueTime = item.dueTime,
                                    pin = item.pin,
                                    status = item.status,
                                    serviceType = item.serviceType,
                                    onClick = {
                                        val targetIntent = when (item.serviceType) {
                                            "CAB" -> Intent(context, CabPoolingActivity::class.java)
                                            "RENT" -> Intent(context, RentProductActivity::class.java)
                                            "MEAL" -> Intent(context, MealGatheringActivity::class.java)
                                            "TRANSFER" -> Intent(context, TransferProductActivity::class.java).apply {
                                                putExtra("TRACK_TRANSFER_ID", item.id)
                                            }
                                            else -> Intent(context, OrderProductActivity::class.java)
                                        }
                                        context.startActivity(targetIntent)
                                    }
                                )
                            }

                            // Show Carousel Page Indicator Dots if user has 2+ orders
                            if (activeOrdersList.size > 1) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    activeOrdersList.indices.forEach { index ->
                                        val isSelected = pagerState.currentPage == index
                                        Box(
                                            modifier = Modifier
                                                .padding(horizontal = 3.dp)
                                                .height(6.dp)
                                                .width(if (isSelected) 18.dp else 6.dp)
                                                .clip(CircleShape)
                                                .background(if (isSelected) Color(0xFF0066FF) else Color(0xFFCBD5E1))
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF0066FF))
                            )
                            Text(
                                text = "CAMPUS SERVICES",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF475569),
                                letterSpacing = 0.5.sp
                            )
                        }
                        Text(
                            text = "View More",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0066FF),
                            modifier = Modifier.clickable {
                                showQuickServicesSheet = true
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // 🍱 Exact Bento Grid Layout from Screenshot
                    Column(
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)
                    ) {
                        // 1. HERO WIDE CARD (Campus Mart - Orange Accent)
                        Surface(
                            shape = RoundedCornerShape(22.dp),
                            color = Color(0xFFFFFBEB),
                            border = BorderStroke(1.dp, Color(0xFFFDE68A)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onCategoryClick("Campus Mart") }
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(50.dp)
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(Color(0xFFF97316)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.ShoppingBag,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                    Column {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Text("Campus Mart", fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF0F172A))
                                            Surface(shape = RoundedCornerShape(50), color = Color(0xFFFEF3C7)) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                                ) {
                                                    Box(modifier = Modifier.size(5.dp).clip(CircleShape).background(Color(0xFFD97706)))
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text("HOT 🔥", color = Color(0xFF92400E), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text("Order fresh food, snacks & daily dorm essentials", fontSize = 12.sp, color = Color(0xFF64748B))
                                    }
                                }
                                Icon(Icons.Outlined.ArrowForward, contentDescription = null, tint = Color(0xFF94A3B8), modifier = Modifier.size(18.dp))
                            }
                        }

                        // 2. 2-COLUMN BENTO GRID (Rent & Host + Campus Store, Send & Bring + Ride Pooling)
                        // Row 1: Rent & Host (Purple) + Campus Store (Blue)
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // Rent & Host Card
                            Surface(
                                shape = RoundedCornerShape(22.dp),
                                color = Color(0xFFF5F3FF),
                                border = BorderStroke(1.dp, Color(0xFFDDD6FE)),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(150.dp)
                                    .clickable { onCategoryClick("Rent & Host Items") }
                            ) {
                                Box(modifier = Modifier.padding(14.dp)) {
                                    Surface(
                                        shape = RoundedCornerShape(50),
                                        color = Color(0xFFEDE9FE),
                                        modifier = Modifier.align(Alignment.TopEnd)
                                    ) {
                                        Text("EARN", color = Color(0xFF6D28D9), fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
                                    }
                                    Column(
                                        modifier = Modifier.fillMaxSize(),
                                        verticalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(44.dp)
                                                .clip(RoundedCornerShape(14.dp))
                                                .background(Color(0xFF6366F1)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(Icons.Outlined.Handshake, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
                                        }
                                        Column {
                                            Text("Campus Rental", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text("Borrow tech gear or host your own items", fontSize = 11.sp, color = Color(0xFF64748B), maxLines = 2)
                                        }
                                    }
                                }
                            }

                            // Campus Store Card
                            Surface(
                                shape = RoundedCornerShape(22.dp),
                                color = Color(0xFFEFF6FF),
                                border = BorderStroke(1.dp, Color(0xFFBFDBFE)),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(150.dp)
                                    .clickable { onCategoryClick("Campus Store") }
                            ) {
                                Box(modifier = Modifier.padding(14.dp)) {
                                    Surface(
                                        shape = RoundedCornerShape(50),
                                        color = Color(0xFFDBEAFE),
                                        modifier = Modifier.align(Alignment.TopEnd)
                                    ) {
                                        Text("OFFICIAL", color = Color(0xFF1D4ED8), fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
                                    }
                                    Column(
                                        modifier = Modifier.fillMaxSize(),
                                        verticalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(44.dp)
                                                .clip(RoundedCornerShape(14.dp))
                                                .background(Color(0xFF0066FF)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(Icons.Outlined.Storefront, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
                                        }
                                        Column {
                                            Text("Campus Store", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0066FF))
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text("Official university merch, books & gear", fontSize = 11.sp, color = Color(0xFF64748B), maxLines = 2)
                                        }
                                    }
                                }
                            }
                        }

                        // Row 2: Send & Bring (Green) + Ride Pooling (Cyan Blue)
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // Send & Bring Card
                            Surface(
                                shape = RoundedCornerShape(22.dp),
                                color = Color(0xFFECFDF5),
                                border = BorderStroke(1.dp, Color(0xFFA7F3D0)),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(150.dp)
                                    .clickable { onCategoryClick("Send and bring items") }
                            ) {
                                Box(modifier = Modifier.padding(14.dp)) {
                                    Surface(
                                        shape = RoundedCornerShape(50),
                                        color = Color(0xFFD1FAE5),
                                        modifier = Modifier.align(Alignment.TopEnd)
                                    ) {
                                        Text("PARCEL", color = Color(0xFF047857), fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
                                    }
                                    Column(
                                        modifier = Modifier.fillMaxSize(),
                                        verticalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(44.dp)
                                                .clip(RoundedCornerShape(14.dp))
                                                .background(Color(0xFF10B981)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(Icons.Outlined.Inventory2, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
                                        }
                                        Column {
                                            Text("Campus Courier", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text("Send and bring items across campus", fontSize = 11.sp, color = Color(0xFF64748B), maxLines = 2)
                                        }
                                    }
                                }
                            }

                            // Ride Pooling Card (Campus Cabs)
                            Surface(
                                shape = RoundedCornerShape(22.dp),
                                color = Color(0xFFF0F9FF),
                                border = BorderStroke(1.dp, Color(0xFFBAE6FD)),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(150.dp)
                                    .clickable { onCategoryClick("Cab & Ride Pooling") }
                            ) {
                                Box(modifier = Modifier.padding(14.dp)) {
                                    Surface(
                                        shape = RoundedCornerShape(50),
                                        color = Color(0xFFE0F2FE),
                                        modifier = Modifier.align(Alignment.TopEnd)
                                    ) {
                                        Text("SPLIT", color = Color(0xFF0369A1), fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
                                    }
                                    Column(
                                        modifier = Modifier.fillMaxSize(),
                                        verticalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(44.dp)
                                                .clip(RoundedCornerShape(14.dp))
                                                .background(Color(0xFF0284C7)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(Icons.Outlined.DirectionsCar, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
                                        }
                                        Column {
                                            Text("Campus Cabs", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text("Airport & city cab shares with students", fontSize = 11.sp, color = Color(0xFF64748B), maxLines = 2)
                                        }
                                    }
                                }
                            }
                        }

                        // 3. COMPACT WIDE BOTTOM CARD (Campus Food Club - Pink/Red Accent)
                        Surface(
                            shape = RoundedCornerShape(22.dp),
                            color = Color(0xFFFFF1F2),
                            border = BorderStroke(1.dp, Color(0xFFFECDD3)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onCategoryClick("Campus Food Club") }
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(46.dp)
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(Color(0xFFF43F5E)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Outlined.Groups, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
                                    }
                                    Column {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Text("Campus Food Club", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                                            Surface(shape = RoundedCornerShape(50), color = Color(0xFFFFE4E6)) {
                                                Text("0% FEE", color = Color(0xFFBE123C), fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text("Group up, order bulk meals together & split delivery", fontSize = 11.sp, color = Color(0xFF64748B))
                                    }
                                }
                                Icon(Icons.Outlined.ChevronRight, contentDescription = null, tint = Color(0xFF94A3B8), modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            } else if (selectedTabIndex == 2) {
                // Activity Tab View
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.White)
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Outlined.Assignment, contentDescription = null, tint = Color(0xFF0066FF), modifier = Modifier.size(56.dp))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Activity & Order History", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("Your completed orders, rentals and ride logs will appear here.", fontSize = 13.sp, color = Color(0xFF64748B), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                }
            } else if (selectedTabIndex == 3) {
                ProfileScreenView(modifier = Modifier.fillMaxSize())
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryCardView(category: DashboardCategory, onClick: () -> Unit) {
    val textPrimary = Color(0xFF0F172A)
    val textSecondary = Color(0xFF64748B)

    val badgeText = when (category.title) {
        "Rent Product", "Rent a Product" -> "POPULAR"
        "Cab Pooling", "People Gathering for Cab" -> "LIVE"
        "Order Product", "Order a Product" -> "FAST"
        "Transfer Product", "Transfer a Product" -> "FREE"
        else -> null
    }

    val badgeBgColor = when (category.title) {
        "Rent Product", "Rent a Product" -> Color(0xFFDC2626)
        "Cab Pooling", "People Gathering for Cab" -> Color(0xFF059669)
        "Order Product", "Order a Product" -> Color(0xFFD97706)
        "Transfer Product", "Transfer a Product" -> Color(0xFF2563EB)
        else -> Color.Transparent
    }

    val iconDrawable = when (category.title) {
        "Order Product", "Order a Product" -> R.drawable.ic_order
        "Transfer Product", "Transfer a Product" -> R.drawable.ic_transfer
        "Rent Product", "Rent a Product" -> R.drawable.ic_rent
        "Cab Pooling", "People Gathering for Cab" -> R.drawable.ic_cab
        "Meal Gathering", "People Gathering for Meal" -> R.drawable.ic_meal
        "Assignments", "Assignment Completion" -> R.drawable.ic_assignment
        else -> R.drawable.ic_order
    }

    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(150.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp)
        ) {
            // Pill Badge at Top Right
            if (badgeText != null) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = badgeBgColor,
                    modifier = Modifier.align(Alignment.TopEnd)
                ) {
                    Text(
                        text = badgeText,
                        color = Color.White,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                    )
                }
            }

            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Vector Line Icon Box (No Emoji / Cartoon)
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(44.dp)
                        .background(Color(0xFFF1F5F9), RoundedCornerShape(12.dp))
                ) {
                    Icon(
                        painter = painterResource(id = iconDrawable),
                        contentDescription = category.title,
                        tint = Color(0xFF1E293B),
                        modifier = Modifier.size(24.dp)
                    )
                }

                Column {
                    Text(
                        text = category.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = category.description,
                        fontSize = 11.sp,
                        color = textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
fun ProfileScreenView(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val email = remember { EncryptionManager.getString(context, "user_email") ?: "student@college.edu" }
    val college = remember { EncryptionManager.getString(context, "user_verified_college") ?: "Googleplex Campus" }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(80.dp)
                .background(Color(0xFFEFF6FF), CircleShape)
        ) {
            Text("👤", fontSize = 40.sp)
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = email.substringBefore("@").replace(".", " ").capitalize(Locale.ROOT),
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF0F172A)
        )
        Text(
            text = email,
            fontSize = 14.sp,
            color = Color(0xFF64748B)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFFE0F2FE)
        ) {
            Text(
                text = "📍 $college",
                color = Color(0xFF0284C7),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
            )
        }
    }
}

data class ServiceItem(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val tagText: String? = null,
    val tagBg: Color = Color(0xFFEFF6FF),
    val tagTextColor: Color = Color(0xFF0066FF)
)

@Composable
fun CampusHeaderSection(
    userName: String = "Alex",
    college: String = "GOOGLEPLEX CAMPUS",
    walletBalance: Double = 450.0,
    onWalletClick: () -> Unit = {},
    onLocationClick: () -> Unit = {}
) {
    val initial = if (userName.isNotEmpty()) userName.take(2).uppercase(Locale.ROOT) else "AL"

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp)
    ) {
        // Top Location Header Row (College Name)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.clickable { onLocationClick() }
                ) {
                    Text(
                        text = college.uppercase(Locale.ROOT),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF94A3B8),
                        letterSpacing = 0.5.sp
                    )
                    Icon(
                        imageVector = Icons.Outlined.KeyboardArrowDown,
                        contentDescription = "Change Location",
                        tint = Color(0xFF94A3B8),
                        modifier = Modifier.size(14.dp)
                    )
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF10B981))
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Hey, $userName",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF0F172A)
                    )
                    Text("✨", fontSize = 18.sp)
                }
            }
        }
    }
}

@Composable
fun FullWidthServiceCard(
    service: ServiceItem,
    onClick: () -> Unit
) {
    val electricBlue = Color(0xFF0066FF)
    val ultraLightSlate = Color(0xFFF8FAFC)
    val slateBorder = Color(0xFFE2E8F0)
    val deepCharcoal = Color(0xFF111827)
    val mutedGray = Color(0xFF6B7280)

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = ultraLightSlate,
        border = BorderStroke(1.dp, slateBorder),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .border(1.5.dp, electricBlue.copy(alpha = 0.3f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = service.icon,
                        contentDescription = service.title,
                        tint = electricBlue,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Column {
                    Text(text = service.title, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = deepCharcoal)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(text = service.description, fontSize = 12.sp, color = mutedGray)
                }
            }
            Icon(imageVector = Icons.Outlined.ChevronRight, contentDescription = null, tint = Color(0xFF94A3B8))
        }
    }
}

@Composable
fun LiveActivityTrackerCard(
    hasActiveOrder: Boolean = false,
    itemName: String = "",
    category: String = "",
    dueTime: String = "",
    pin: String = "",
    status: String = "",
    serviceType: String = "",
    onClick: () -> Unit
) {
    if (!hasActiveOrder) {
        // Clean Minimal Empty State Card (Shown when user has no active order)
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color(0xFFF8FAFC),
            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
            shadowElevation = 0.dp,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onClick() }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFF1F5F9)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Inventory2,
                            contentDescription = null,
                            tint = Color(0xFF64748B),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Column {
                        Text(
                            text = "No Active Orders",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Order food, rent gear or pool cabs to start live tracking",
                            fontSize = 11.sp,
                            color = Color(0xFF64748B)
                        )
                    }
                }
            }
        }
        return
    }

    // Smooth infinite blinking animation for green live dot
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    val normalizedStatus = status.uppercase(Locale.ROOT).replace(" ", "_")

    val serviceColor = when (serviceType) {
        "CAB" -> Color(0xFF0284C7)
        "RENT" -> Color(0xFF6366F1)
        "TRANSFER" -> Color(0xFF10B981)
        "MEAL" -> Color(0xFFF43F5E)
        else -> Color(0xFF0066FF)
    }

    val stepIndex = when (normalizedStatus) {
        "PENDING", "CONFIRMED", "GATHERING", "RESERVED" -> 1
        "ACCEPTED", "RUNNER_ASSIGNED", "PICKED_UP", "ARRIVED", "DELIVERING", "ON_THE_WAY", "IN_TRANSIT", "LOCKED", "CAB_BOOKED", "ACTIVE" -> 2
        "DELIVERED", "COMPLETED", "RETURNED" -> 3
        else -> 1
    }

    val statusLabel = when {
        serviceType == "CAB" -> when (normalizedStatus) {
            "GATHERING" -> "Seats Available"
            "LOCKED", "ACCEPTED", "CAB_BOOKED" -> "Cab Confirmed"
            "ON_THE_WAY", "IN_TRANSIT", "PICKED_UP" -> "En Route"
            "COMPLETED" -> "Completed"
            else -> "Cab Confirmed"
        }
        serviceType == "RENT" -> when (normalizedStatus) {
            "PENDING_COLLECTION", "PENDING" -> "Ready for Pickup"
            "ACTIVE", "ACCEPTED", "ON_THE_WAY", "PICKED_UP" -> "Item Rented"
            "PENDING_RETURN" -> "Return Pending"
            "RETURNED" -> "Returned"
            else -> "Active Rental"
        }
        serviceType == "TRANSFER" -> when (normalizedStatus) {
            "PENDING" -> "Courier Requested"
            "ACCEPTED" -> "Runner Assigned"
            "PICKED_UP", "ARRIVED" -> "Package Picked Up"
            "ON_THE_WAY", "IN_TRANSIT", "DELIVERING" -> "Out for Delivery"
            "DELIVERED" -> "Delivered"
            else -> "In Transit"
        }
        serviceType == "MEAL" -> when (normalizedStatus) {
            "GATHERING" -> "Gathering Buddies"
            "ORDER_PLACED" -> "Meal Ordered"
            "COMPLETED" -> "Completed"
            else -> "Food Circle Active"
        }
        else -> when (normalizedStatus) {
            "PENDING" -> "Order Placed"
            "CONFIRMED" -> "Preparing"
            "ACCEPTED" -> "Runner Assigned"
            "PICKED_UP", "ARRIVED" -> "Order Picked Up"
            "ON_THE_WAY", "IN_TRANSIT", "DELIVERING" -> "Out for Delivery"
            "DELIVERED" -> "Delivered"
            else -> "In Progress"
        }
    }

    Surface(
        shape = RoundedCornerShape(24.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        shadowElevation = 0.dp,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Top Row: LIVE TRACKING badge + Status Pill + Due time pill
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = Color(0xFFDCFCE7)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF10B981).copy(alpha = alpha))
                            )
                            Text(
                                text = "LIVE TRACKING",
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF15803D),
                                letterSpacing = 0.4.sp
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(50),
                        color = Color(0xFFEFF6FF),
                        border = BorderStroke(1.dp, Color(0xFFBFDBFE))
                    ) {
                        Text(
                            text = statusLabel,
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0066FF),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.width(6.dp))

                Surface(
                    shape = RoundedCornerShape(50),
                    color = Color(0xFFF1F5F9)
                ) {
                    Text(
                        text = dueTime,
                        fontSize = 10.5.sp,
                        color = Color(0xFF64748B),
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        maxLines = 1
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Dynamic Icon & Accent Color based on Service Type
            val (serviceIcon, serviceColor) = when {
                itemName.contains("Cab", ignoreCase = true) || category.contains("Cab", ignoreCase = true) -> Icons.Outlined.DirectionsCar to Color(0xFF0284C7)
                itemName.contains("Rent", ignoreCase = true) || category.contains("Rental", ignoreCase = true) || category.contains("Media Lab", ignoreCase = true) -> Icons.Outlined.Handshake to Color(0xFF0066FF)
                itemName.contains("Transfer", ignoreCase = true) || category.contains("Courier", ignoreCase = true) || category.contains("Send", ignoreCase = true) -> Icons.Outlined.Inventory2 to Color(0xFF10B981)
                else -> Icons.Outlined.ShoppingBag to Color(0xFFF97316)
            }

            // Middle Section: Item Title + Track Button Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(serviceColor.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = serviceIcon,
                            contentDescription = null,
                            tint = serviceColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Text(
                        text = itemName,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF0F172A),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Surface(
                    shape = RoundedCornerShape(50),
                    color = Color(0xFF0066FF),
                    modifier = Modifier.clickable { onClick() }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Text(
                            text = "Track",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Icon(
                            imageVector = Icons.Outlined.ChevronRight,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Premium Structured Location & OTP Section
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color(0xFFF8FAFC),
                border = BorderStroke(1.dp, Color(0xFFF1F5F9)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
                    val lines = category.split("\n")
                    lines.forEachIndexed { index, line ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(if (index == 0 && lines.size > 1) Color(0xFF10B981) else Color(0xFFEF4444))
                            )
                            Text(
                                text = line,
                                fontSize = 11.5.sp,
                                color = Color(0xFF334155),
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        if (index < lines.size - 1) {
                            Spacer(modifier = Modifier.height(4.dp))
                        }
                    }

                    if (pin.isNotEmpty() && (status.equals("ACCEPTED", true) || status.equals("PICKED UP", true) || status.equals("ON_THE_WAY", true) || status.equals("IN_TRANSIT", true) || stepIndex >= 2)) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFFFEF3C7),
                                border = BorderStroke(1.dp, Color(0xFFFDE68A))
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = "VERIFICATION OTP",
                                        fontSize = 8.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFB45309)
                                    )
                                    Text(
                                        text = pin,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color(0xFFD97706),
                                        letterSpacing = 1.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Divider(color = Color(0xFFF1F5F9), thickness = 1.dp)

            Spacer(modifier = Modifier.height(12.dp))

            // Bottom Row: Vibrant Step Timeline matching Service Accent Color & Progress
            val (step1Label, step2Label, step3Label) = when (serviceType) {
                "CAB" -> Triple("Gathering", "Confirmed", "En Route")
                "RENT" -> Triple("Reserved", "Rented", "Returned")
                "TRANSFER" -> Triple("Requested", "Picked Up", "Delivered")
                "MEAL" -> Triple("Gathering", "Joined", "Meal Ready")
                else -> Triple("Order Placed", "Preparing", "Delivered")
            }

            Column(modifier = Modifier.fillMaxWidth()) {
                // Step Labels & Progress Bar Columns (Each label centered exactly over its progress segment)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Step 1 Segment + Centered Label
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = step1Label,
                            fontSize = 10.sp,
                            fontWeight = if (stepIndex >= 1) FontWeight.Bold else FontWeight.Medium,
                            color = if (stepIndex >= 1) serviceColor else Color(0xFF94A3B8),
                            textAlign = TextAlign.Center,
                            maxLines = 1
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(5.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(
                                    if (stepIndex >= 1) {
                                        Brush.horizontalGradient(listOf(serviceColor, serviceColor.copy(alpha = 0.85f)))
                                    } else {
                                        Brush.horizontalGradient(listOf(Color(0xFFE2E8F0), Color(0xFFE2E8F0)))
                                    }
                                )
                        )
                    }

                    // Step 2 Segment + Centered Label
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = step2Label,
                            fontSize = 10.sp,
                            fontWeight = if (stepIndex >= 2) FontWeight.Bold else FontWeight.Medium,
                            color = if (stepIndex >= 2) serviceColor else Color(0xFF94A3B8),
                            textAlign = TextAlign.Center,
                            maxLines = 1
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(5.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(
                                    if (stepIndex >= 2) {
                                        Brush.horizontalGradient(listOf(serviceColor, serviceColor.copy(alpha = 0.85f)))
                                    } else {
                                        Brush.horizontalGradient(listOf(Color(0xFFE2E8F0), Color(0xFFE2E8F0)))
                                    }
                                )
                        )
                    }

                    // Step 3 Segment + Centered Label
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = step3Label,
                            fontSize = 10.sp,
                            fontWeight = if (stepIndex >= 3) FontWeight.Bold else FontWeight.Medium,
                            color = if (stepIndex >= 3) serviceColor else Color(0xFF94A3B8),
                            textAlign = TextAlign.Center,
                            maxLines = 1
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(5.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(
                                    if (stepIndex >= 3) {
                                        Brush.horizontalGradient(listOf(Color(0xFF10B981), Color(0xFF059669)))
                                    } else {
                                        Brush.horizontalGradient(listOf(Color(0xFFE2E8F0), Color(0xFFE2E8F0)))
                                    }
                                )
                        )
                    }
                }
            }
        }
    }
}

enum class BentoCardType { HERO_WIDE, STANDARD_SQUARE, COMPACT_WIDE }

data class BentoServiceItem(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val badgeText: String,
    val accentColor: Color,
    val cardType: BentoCardType
)

@Composable
fun LiveActivityHeroCard(
    itemName: String,
    location: String,
    dueTime: String,
    pin: String,
    onTrackClick: () -> Unit
) {
    val electricBlue = Color(0xFF0066FF)
    val darkCharcoal = Color(0xFF0F172A)
    val slate900 = Color(0xFF1E293B)
    val emeraldLive = Color(0xFF10B981)
    val pureWhite = Color(0xFFFFFFFF)

    Surface(
        shape = RoundedCornerShape(24.dp),
        color = darkCharcoal,
        border = BorderStroke(1.dp, slate900),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = electricBlue.copy(alpha = 0.2f),
                    border = BorderStroke(1.dp, electricBlue.copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(emeraldLive)
                        )
                        Text("LIVE ACTIVITY", color = Color(0xFF93C5FD), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Surface(shape = RoundedCornerShape(50), color = pureWhite.copy(alpha = 0.1f)) {
                    Text(dueTime, color = pureWhite, fontSize = 11.sp, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Main Item Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(pureWhite.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Outlined.Camera, contentDescription = null, tint = Color(0xFF93C5FD))
                    }
                    Column {
                        Text(itemName, color = pureWhite, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Text(location, color = Color(0xFFCBD5E1), fontSize = 11.sp)
                    }
                }
                Button(
                    onClick = onTrackClick,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = electricBlue),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text("Track", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(Icons.Outlined.ArrowForward, contentDescription = null, modifier = Modifier.size(14.dp))
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Stepper & PIN Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(modifier = Modifier.width(140.dp).height(5.dp).clip(CircleShape).background(pureWhite.copy(alpha = 0.15f))) {
                        Box(modifier = Modifier.fillMaxHeight().fillMaxWidth(0.66f).clip(CircleShape).background(electricBlue))
                    }
                    Text("Picked Up", color = Color(0xFF94A3B8), fontSize = 10.sp)
                }
                Surface(shape = RoundedCornerShape(6.dp), color = pureWhite.copy(alpha = 0.1f)) {
                    Text("PIN $pin", color = pureWhite, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp))
                }
            }
        }
    }
}
