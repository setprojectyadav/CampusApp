package com.college.campusapp

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class AppNotification(
    val id: String,
    val icon: String,
    val title: String,
    val message: String,
    val timestamp: Long,
    val category: String // "ORDER", "WALLET", "SECURITY", "PROMO"
)

object NotificationManager {
    fun getPreviewNotifications(): List<AppNotification> {
        val now = System.currentTimeMillis()
        return listOf(
            AppNotification(
                id = "1",
                icon = "⚡",
                title = "Order Status Update",
                message = "Rider Aarav is picking up your order from Campus Store.",
                timestamp = now - 45000,
                category = "ORDER"
            ),
            AppNotification(
                id = "2",
                icon = "💵",
                title = "Wallet Refund Credited",
                message = "₹25.00 refunded back to your wallet from price estimate diff.",
                timestamp = now - 360000,
                category = "WALLET"
            ),
            AppNotification(
                id = "3",
                icon = "🛡️",
                title = "Session Secured",
                message = "EncryptedSharedPreferences initialized using MasterKey AES-256.",
                timestamp = now - 720000,
                category = "SECURITY"
            ),
            AppNotification(
                id = "4",
                icon = "🎉",
                title = "Hostel Gate Offer",
                message = "Order above ₹150 and split gate premium fees with hostel mates!",
                timestamp = now - 1200000,
                category = "PROMO"
            )
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationBar(modifier: Modifier = Modifier) {
    var isVisible by remember { mutableStateOf(true) }
    var showSheet by remember { mutableStateOf(false) }
    val list = remember { NotificationManager.getPreviewNotifications() }
    var currentIndex by remember { mutableStateOf(0) }

    // Rotate messages every 4 seconds
    LaunchedEffect(isVisible) {
        if (isVisible) {
            while (true) {
                delay(4000)
                currentIndex = (currentIndex + 1) % list.size
            }
        }
    }

    if (!isVisible) return

    val currentNotification = list[currentIndex]

    // Style colors according to type
    val (barBg, textCol, iconCol) = when (currentNotification.category) {
        "ORDER" -> Triple(Color(0xFFEFF6FF), Color(0xFF1E40AF), Color(0xFF3B82F6))     // Soft blue
        "WALLET" -> Triple(Color(0xFFECFDF5), Color(0xFF065F46), Color(0xFF10B981))    // Soft emerald
        "SECURITY" -> Triple(Color(0xFFF8FAF9), Color(0xFF1E293B), Color(0xFF64748B))  // Soft gray/slate
        else -> Triple(Color(0xFFFFF7ED), Color(0xFF9A3412), Color(0xFFF97316))        // Soft orange (Promo)
    }

    Column(modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(barBg)
                .clickable { showSheet = true }
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Animated ticker transition
                AnimatedContent(
                    targetState = currentNotification,
                    transitionSpec = {
                        (slideInVertically { height -> height } + fadeIn() togetherWith
                                slideOutVertically { height -> -height } + fadeOut())
                    },
                    label = "TickerTransition",
                    modifier = Modifier.weight(1f)
                ) { notif ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = notif.icon,
                            fontSize = 15.sp,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Column {
                            Text(
                                text = notif.title,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = textCol
                            )
                            Text(
                                text = notif.message,
                                fontSize = 11.sp,
                                color = textCol.copy(alpha = 0.85f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Close Button
                Box(
                    modifier = Modifier
                        .size(18.dp)
                        .clip(CircleShape)
                        .clickable { isVisible = false }
                        .background(textCol.copy(alpha = 0.08f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "✕",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = textCol
                    )
                }
            }
        }
    }

    if (showSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSheet = false },
            containerColor = Color.White,
            dragHandle = {
                Box(
                    modifier = Modifier
                        .padding(top = 10.dp, bottom = 10.dp)
                        .width(36.dp)
                        .height(4.dp)
                        .background(Color(0xFFE2E8F0))
                        .clip(CircleShape)
                )
            }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 10.dp)
            ) {
                Text(
                    text = "Notification Center",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp,
                    color = Color(0xFF0F172A),
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp)
                ) {
                    items(list) { item ->
                        val date = Date(item.timestamp)
                        val formatter = SimpleDateFormat("hh:mm a", Locale.getDefault())
                        val timeString = formatter.format(date)

                        val itemBg = when (item.category) {
                            "ORDER" -> Color(0xFFEFF6FF)
                            "WALLET" -> Color(0xFFECFDF5)
                            "SECURITY" -> Color(0xFFF8FAF9)
                            else -> Color(0xFFFFF7ED)
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(itemBg)
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.8f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(item.icon, fontSize = 16.sp)
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = item.title,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = Color(0xFF0F172A)
                                    )
                                    Text(
                                        text = timeString,
                                        fontSize = 10.sp,
                                        color = Color(0xFF64748B)
                                    )
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = item.message,
                                    fontSize = 11.sp,
                                    color = Color(0xFF334155),
                                    lineHeight = 15.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
