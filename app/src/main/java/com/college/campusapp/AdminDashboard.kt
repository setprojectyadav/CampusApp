package com.college.campusapp

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboard(
    onSwitchBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val primaryColor = AppTheme.Primary
    val successColor = AppTheme.Success
    val dangerColor = AppTheme.Danger
    val textPrimary = AppTheme.InkPrimary
    val textSecondary = AppTheme.InkSecondary

    var selectedTab by remember { mutableStateOf(0) } // 0 = Pending Reviews, 1 = All Listings
    var refreshTrigger by remember { mutableStateOf(0) }

    // Load listings from RentalRepository
    LaunchedEffect(refreshTrigger) {
        RentalRepository.load(context, force = true)
    }

    val pendingList = remember(RentalRepository.userItemsList.size, selectedTab, refreshTrigger) {
        RentalRepository.userItemsList.filter { it.status == "PENDING_APPROVAL" }
    }
    
    val allListings = remember(RentalRepository.userItemsList.size, selectedTab, refreshTrigger) {
        RentalRepository.userItemsList.toList()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF64748B).copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("🛡️", fontSize = 18.sp)
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Admin Panel",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = textPrimary
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onSwitchBack) {
                        Text("◀", fontSize = 16.sp, color = textPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White),
                modifier = Modifier.shadow(4.dp)
            )
        },
        containerColor = AppTheme.Background,
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Stat Cards Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val totalItems = allListings.size
                val totalPending = pendingList.size
                val totalApproved = allListings.count { it.status == "APPROVED" }

                StatCard(
                    title = "Pending",
                    value = totalPending.toString(),
                    color = Color(0xFFF59E0B),
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    title = "Approved",
                    value = totalApproved.toString(),
                    color = successColor,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    title = "Total Hosted",
                    value = totalItems.toString(),
                    color = Color(0xFF64748B),
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Tab Selector
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.White,
                contentColor = primaryColor,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(12.dp))
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Pending (${pendingList.size})", fontWeight = FontWeight.Bold, fontSize = 11.sp) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("All (${allListings.size})", fontWeight = FontWeight.Bold, fontSize = 11.sp) }
                )
                val campusVotes = remember(selectedTab, refreshTrigger) { CampusVoteRepository.getVotes(context) }
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("🚀 Votes (${campusVotes.sumOf { it.second }})", fontWeight = FontWeight.Bold, fontSize = 11.sp) }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Main Content Feed
            if (selectedTab == 0) {
                // Pending Reviews Feed
                if (pendingList.isEmpty()) {
                    EmptyStateView(
                        emoji = "✨",
                        title = "All caught up!",
                        description = "No products are waiting for approval right now."
                    )
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(pendingList, key = { it.id }) { item ->
                            PendingItemCard(
                                item = item,
                                onApprove = {
                                    item.status = "APPROVED"
                                    RentalRepository.save(context)
                                    // Trigger notification simulation
                                    SystemNotificationHelper.showNotification(
                                        context,
                                        "Product Hosting Approved 🎉",
                                        "Your item '${item.name}' has been approved and is now live in the rental marketplace!"
                                    )
                                    Toast.makeText(context, "Listing approved and published!", Toast.LENGTH_SHORT).show()
                                    refreshTrigger++
                                },
                                onReject = {
                                    item.status = "REJECTED"
                                    RentalRepository.save(context)
                                    Toast.makeText(context, "Listing rejected.", Toast.LENGTH_SHORT).show()
                                    refreshTrigger++
                                }
                            )
                        }
                    }
                }
            } else if (selectedTab == 1) {
                // All Listings Feed
                if (allListings.isEmpty()) {
                    EmptyStateView(
                        emoji = "📦",
                        title = "No hosted items",
                        description = "No items have been hosted on the platform yet."
                    )
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(allListings, key = { it.id }) { item ->
                            AllListingsCard(
                                item = item,
                                onDelete = {
                                    RentalRepository.userItemsList.remove(item)
                                    RentalRepository.save(context)
                                    Toast.makeText(context, "Item deleted permanently.", Toast.LENGTH_SHORT).show()
                                    refreshTrigger++
                                },
                                onToggleStatus = {
                                    item.status = if (item.status == "APPROVED") "REJECTED" else "APPROVED"
                                    RentalRepository.save(context)
                                    Toast.makeText(context, "Status updated to ${item.status}", Toast.LENGTH_SHORT).show()
                                    refreshTrigger++
                                }
                            )
                        }
                    }
                }
            } else {
                // Campus Launch Votes Leaderboard Tab
                val campusVotes = remember(selectedTab, refreshTrigger) { CampusVoteRepository.getVotes(context) }
                Column(modifier = Modifier.fillMaxSize()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Target Launch Campus Leaderboard",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = textPrimary
                        )
                        TextButton(onClick = {
                            CampusVoteRepository.clearVotes(context)
                            refreshTrigger++
                            Toast.makeText(context, "Votes reset", Toast.LENGTH_SHORT).show()
                        }) {
                            Text("Reset Votes", fontSize = 11.sp, color = AppTheme.Danger)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(campusVotes.indices.toList()) { index ->
                            val (campusName, voteCount) = campusVotes[index]
                            val medalEmoji = when (index) {
                                0 -> "🥇"
                                1 -> "🥈"
                                2 -> "🥉"
                                else -> "🎓"
                            }
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                        Text(medalEmoji, fontSize = 22.sp)
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column {
                                            Text(
                                                text = campusName,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp,
                                                color = textPrimary
                                            )
                                            Text(
                                                text = "Rank #${index + 1} Requested Location",
                                                fontSize = 11.sp,
                                                color = textSecondary
                                            )
                                        }
                                    }

                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(20.dp))
                                            .background(Color(0xFFEEF2FF))
                                            .padding(horizontal = 12.dp, vertical = 6.dp)
                                    ) {
                                        Text(
                                            text = "🔥 $voteCount votes",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF4F46E5)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StatCard(
    title: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.shadow(2.dp, RoundedCornerShape(14.dp)),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, color.copy(alpha = 0.15f))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(title, fontSize = 11.sp, color = AppTheme.InkSecondary, fontWeight = FontWeight.Medium)
            Spacer(modifier = Modifier.height(4.dp))
            Text(value, fontSize = 20.sp, color = color, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
fun EmptyStateView(
    emoji: String,
    title: String,
    description: String
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 60.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(24.dp)
        ) {
            Text(emoji, fontSize = 48.sp)
            Spacer(modifier = Modifier.height(12.dp))
            Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = AppTheme.InkPrimary)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = description,
                fontSize = 12.sp,
                color = AppTheme.InkSecondary,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun PendingItemCard(
    item: RentalCatalogItem,
    onApprove: () -> Unit,
    onReject: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().shadow(2.dp, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header: Emoji + Title + Category
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(AppTheme.Primary.copy(alpha = 0.08f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(item.emoji, fontSize = 22.sp)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = AppTheme.InkPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Box(
                        modifier = Modifier
                            .background(Color(0xFFF1F5F9), RoundedCornerShape(6.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = item.category.uppercase(),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF475569)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Divider(color = Color(0xFFF1F5F9))
            Spacer(modifier = Modifier.height(12.dp))

            // Pricing Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("DAILY RENT", fontSize = 9.sp, color = AppTheme.InkSecondary)
                    Text("₹${item.dailyRate}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = AppTheme.Primary)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("SECURITY DEPOSIT", fontSize = 9.sp, color = AppTheme.InkSecondary)
                    Text("₹${item.securityDeposit}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = AppTheme.InkPrimary)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Description
            Text("DESCRIPTION", fontSize = 9.sp, color = AppTheme.InkSecondary)
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = item.description,
                fontSize = 12.sp,
                color = AppTheme.InkPrimary,
                lineHeight = 16.sp
            )

            Spacer(modifier = Modifier.height(14.dp))
            Divider(color = Color(0xFFF1F5F9))
            Spacer(modifier = Modifier.height(14.dp))

            // Approve & Reject Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onReject,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = AppTheme.Danger),
                    border = BorderStroke(1.dp, AppTheme.Danger),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f).height(40.dp)
                ) {
                    Text("Reject", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }

                Button(
                    onClick = onApprove,
                    colors = ButtonDefaults.buttonColors(containerColor = AppTheme.Success),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1.2f).height(40.dp)
                ) {
                    Text("Approve Listing", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
fun AllListingsCard(
    item: RentalCatalogItem,
    onDelete: () -> Unit,
    onToggleStatus: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().shadow(1.dp, RoundedCornerShape(14.dp)),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header Row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Text(item.emoji, fontSize = 20.sp)
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = item.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = AppTheme.InkPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Rate: ₹${item.dailyRate}/day | Deposit: ₹${item.securityDeposit}",
                            fontSize = 11.sp,
                            color = AppTheme.InkSecondary
                        )
                    }
                }

                // Status Badge
                val badgeColor = when (item.status) {
                    "APPROVED" -> AppTheme.Success.copy(alpha = 0.1f)
                    "PENDING_APPROVAL" -> Color(0xFFFEF3C7)
                    else -> AppTheme.Danger.copy(alpha = 0.1f)
                }
                val badgeTextColor = when (item.status) {
                    "APPROVED" -> AppTheme.Success
                    "PENDING_APPROVAL" -> Color(0xFFD97706)
                    else -> AppTheme.Danger
                }
                Box(
                    modifier = Modifier
                        .background(badgeColor, RoundedCornerShape(6.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = item.status,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = badgeTextColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Actions row
            Row(
                horizontalArrangement = Arrangement.End,
                modifier = Modifier.fillMaxWidth()
            ) {
                TextButton(
                    onClick = onToggleStatus,
                    colors = ButtonDefaults.textButtonColors(contentColor = AppTheme.Primary)
                ) {
                    Text(
                        text = if (item.status == "APPROVED") "Deactivate" else "Activate",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                TextButton(
                    onClick = onDelete,
                    colors = ButtonDefaults.textButtonColors(contentColor = AppTheme.Danger)
                ) {
                    Text("Delete", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                }
            }
        }
    }
}
