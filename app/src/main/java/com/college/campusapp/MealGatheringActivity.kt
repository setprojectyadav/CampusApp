package com.college.campusapp

import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.college.campusapp.security.EncryptionManager
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID
import kotlin.math.*

class MealGatheringActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        MealGatheringRepository.load(this)

        setContent {
            MealGatheringScreen(onBack = { finish() })
        }
    }
}

fun formatMealCountdownTime(targetTimestamp: Long, currentTimestamp: Long): String {
    val diff = targetTimestamp - currentTimestamp
    if (diff <= 0) return "Time Passed"
    val hours = diff / 3600000
    val minutes = (diff % 3600000) / 60000
    val seconds = (diff % 60000) / 1000
    return if (hours > 0) {
        String.format(Locale.US, "%dh %02dm %02ds", hours, minutes, seconds)
    } else {
        String.format(Locale.US, "%02dm %02ds", minutes, seconds)
    }
}

// Ultra-lightweight isolated meal countdown badge (Only this tiny composable recomposes every second)
@Composable
fun LiveMealCountdownBadge(targetTimestamp: Long, modifier: Modifier = Modifier) {
    var now by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(targetTimestamp) {
        while (now < targetTimestamp) {
            delay(1000L)
            now = System.currentTimeMillis()
        }
    }
    val diff = targetTimestamp - now
    if (diff in 1..3600000) {
        val countdownStr = formatMealCountdownTime(targetTimestamp, now)
        Box(
            modifier = modifier
                .background(Color(0xFFFEF2F2), RoundedCornerShape(8.dp))
                .border(1.dp, Color(0xFFFECACA), RoundedCornerShape(8.dp))
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text(
                text = "Starts in: $countdownStr",
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFFDC2626)
            )
        }
    }
}

// Dual-Arrow Circular Refresh Button (Exact matching dual-arrow sync icon with smooth rotation)
@Composable
private fun CircularRefreshButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = Color(0xFFE11D48)
) {
    var isRotating by remember { mutableStateOf(false) }
    val rotation by animateFloatAsState(
        targetValue = if (isRotating) 360f else 0f,
        animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing),
        finishedListener = { isRotating = false },
        label = "dualArrowRefreshRotation"
    )

    Box(
        modifier = modifier
            .size(28.dp)
            .clip(CircleShape)
            .background(tint.copy(alpha = 0.08f))
            .border(1.dp, tint.copy(alpha = 0.22f), CircleShape)
            .clickable {
                isRotating = true
                onClick()
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier
                .size(13.dp)
                .rotate(rotation)
        ) {
            val strokeWidth = 1.7.dp.toPx()
            val radius = (size.minDimension - strokeWidth * 2.2f) / 2
            val center = Offset(size.width / 2, size.height / 2)
            val arrowLength = 3.0.dp.toPx()
            val arrowHalfWidth = 2.2.dp.toPx()

            // ─── Top-Right Arc ───────────────────────────
            drawArc(
                color = tint,
                startAngle = 155f,
                sweepAngle = 140f,
                useCenter = false,
                style = androidx.compose.ui.graphics.drawscope.Stroke(
                    width = strokeWidth,
                    cap = StrokeCap.Round
                )
            )

            // ─── Arrowhead 1 (Clockwise at ~320°) ────────────────
            val rad1 = Math.toRadians(320.0)
            val tip1X = center.x + radius * kotlin.math.cos(rad1).toFloat()
            val tip1Y = center.y + radius * kotlin.math.sin(rad1).toFloat()
            val vx1 = -kotlin.math.sin(rad1).toFloat()
            val vy1 = kotlin.math.cos(rad1).toFloat()
            val nx1 = kotlin.math.cos(rad1).toFloat()
            val ny1 = kotlin.math.sin(rad1).toFloat()

            val base1X = tip1X - vx1 * arrowLength
            val base1Y = tip1Y - vy1 * arrowLength
            val p1AX = base1X + nx1 * arrowHalfWidth
            val p1AY = base1Y + ny1 * arrowHalfWidth
            val p1BX = base1X - nx1 * arrowHalfWidth
            val p1BY = base1Y - ny1 * arrowHalfWidth

            val arrow1Path = androidx.compose.ui.graphics.Path().apply {
                moveTo(tip1X, tip1Y)
                lineTo(p1AX, p1AY)
                lineTo(p1BX, p1BY)
                close()
            }
            drawPath(path = arrow1Path, color = tint)

            // ─── Bottom-Left Arc ────────────────────────
            drawArc(
                color = tint,
                startAngle = 335f,
                sweepAngle = 140f,
                useCenter = false,
                style = androidx.compose.ui.graphics.drawscope.Stroke(
                    width = strokeWidth,
                    cap = StrokeCap.Round
                )
            )

            // ─── Arrowhead 2 (Clockwise at ~140°) ────────────────
            val rad2 = Math.toRadians(140.0)
            val tip2X = center.x + radius * kotlin.math.cos(rad2).toFloat()
            val tip2Y = center.y + radius * kotlin.math.sin(rad2).toFloat()
            val vx2 = -kotlin.math.sin(rad2).toFloat()
            val vy2 = kotlin.math.cos(rad2).toFloat()
            val nx2 = kotlin.math.cos(rad2).toFloat()
            val ny2 = kotlin.math.sin(rad2).toFloat()

            val base2X = tip2X - vx2 * arrowLength
            val base2Y = tip2Y - vy2 * arrowLength
            val p2AX = base2X + nx2 * arrowHalfWidth
            val p2AY = base2Y + ny2 * arrowHalfWidth
            val p2BX = base2X - nx2 * arrowHalfWidth
            val p2BY = base2Y - ny2 * arrowHalfWidth

            val arrow2Path = androidx.compose.ui.graphics.Path().apply {
                moveTo(tip2X, tip2Y)
                lineTo(p2AX, p2AY)
                lineTo(p2BX, p2BY)
                close()
            }
            drawPath(path = arrow2Path, color = tint)
        }
    }
}

// Unified Wallet Badge across all sections
@Composable
private fun UnifiedWalletBadge(balance: Int, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(50),
        color = Color(0xFFF0FDF4),
        border = BorderStroke(1.dp, Color(0xFF86EFAC)),
        modifier = modifier.clickable(onClick = onClick)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(18.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF22C55E)),
                contentAlignment = Alignment.Center
            ) {
                Text("\u20B9", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 10.sp)
            }
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = "\u20B9$balance",
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = Color(0xFF15803D)
            )
        }
    }
}

// Animated Circular Clock Time Selector for Meal Gathering
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CampusMealTimePickerDialog(
    selectedYear: Int,
    selectedMonth: Int,
    selectedDay: Int,
    initialHour: Int,
    initialMinute: Int,
    onDismiss: () -> Unit,
    onTimeSelected: (Int, Int) -> Unit
) {
    val textPrimary = AppTheme.InkPrimary
    val textSecondary = AppTheme.InkSecondary
    val primaryColor = Color(0xFFE11D48)

    val now = remember { Calendar.getInstance() }
    val isToday = remember(selectedYear, selectedMonth, selectedDay) {
        selectedYear == now.get(Calendar.YEAR) &&
        selectedMonth == now.get(Calendar.MONTH) &&
        selectedDay == now.get(Calendar.DAY_OF_MONTH)
    }

    val minAllowedTimestamp = remember(now) { System.currentTimeMillis() + 15 * 60 * 1000 }

    var isSelectingHours by remember { mutableStateOf(true) }
    var isPm by remember { mutableStateOf(initialHour >= 12) }
    var hour12 by remember {
        val h = if (initialHour % 12 == 0) 12 else initialHour % 12
        mutableStateOf(h)
    }
    var selectedMinute by remember { mutableStateOf((initialMinute / 5) * 5) }

    fun to24Hour(h12: Int, pm: Boolean): Int {
        return if (pm) {
            if (h12 == 12) 12 else h12 + 12
        } else {
            if (h12 == 12) 0 else h12
        }
    }

    fun isHourSelectable(h12: Int, pm: Boolean): Boolean {
        if (!isToday) return true
        val h24 = to24Hour(h12, pm)
        val testCal = Calendar.getInstance().apply {
            set(selectedYear, selectedMonth, selectedDay, h24, 59, 59)
        }
        return testCal.timeInMillis >= minAllowedTimestamp
    }

    fun isMinuteSelectable(minVal: Int): Boolean {
        if (!isToday) return true
        val h24 = to24Hour(hour12, isPm)
        val testCal = Calendar.getInstance().apply {
            set(selectedYear, selectedMonth, selectedDay, h24, minVal, 0)
        }
        return testCal.timeInMillis >= minAllowedTimestamp
    }

    fun isAmSelectable(): Boolean {
        if (!isToday) return true
        val testCal = Calendar.getInstance().apply {
            set(selectedYear, selectedMonth, selectedDay, 11, 59, 59)
        }
        return testCal.timeInMillis >= minAllowedTimestamp
    }

    LaunchedEffect(isPm, hour12) {
        if (isToday && !isAmSelectable() && !isPm) {
            isPm = true
        }
        if (!isHourSelectable(hour12, isPm)) {
            val candidateHours = (1..12).filter { isHourSelectable(it, isPm) }
            if (candidateHours.isNotEmpty()) {
                hour12 = candidateHours.first()
            } else if (!isPm) {
                isPm = true
                val pmCandidates = (1..12).filter { isHourSelectable(it, true) }
                if (pmCandidates.isNotEmpty()) hour12 = pmCandidates.first()
            }
        }
        if (!isMinuteSelectable(selectedMinute)) {
            val validMin = listOf(0, 5, 10, 15, 20, 25, 30, 35, 40, 45, 50, 55).firstOrNull { isMinuteSelectable(it) } ?: 0
            selectedMinute = validMin
        }
    }

    val selectedCal = remember(selectedYear, selectedMonth, selectedDay, hour12, isPm, selectedMinute) {
        Calendar.getInstance().apply {
            set(selectedYear, selectedMonth, selectedDay, to24Hour(hour12, isPm), selectedMinute, 0)
        }
    }

    val isFutureValid = selectedCal.timeInMillis >= minAllowedTimestamp
    val timeFormatted = String.format(Locale.US, "%02d:%02d %s", hour12, selectedMinute, if (isPm) "PM" else "AM")

    val targetAngle = remember(isSelectingHours, hour12, selectedMinute) {
        if (isSelectingHours) {
            (hour12 % 12) * 30f
        } else {
            selectedMinute * 6f
        }
    }

    val animatedAngle by animateFloatAsState(
        targetValue = targetAngle,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "mealClockHandAngle"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        containerColor = Color.White,
        title = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text("Select Meal or Order Time", fontWeight = FontWeight.Bold, fontSize = 17.sp, color = textPrimary)
                Text(
                    text = if (isToday) "Only upcoming times for today are selectable" else "Select time for upcoming date",
                    fontSize = 11.sp,
                    color = textSecondary
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelectingHours) primaryColor.copy(alpha = 0.12f) else Color(0xFFF1F5F9),
                        border = BorderStroke(2.dp, if (isSelectingHours) primaryColor else Color.Transparent),
                        modifier = Modifier
                            .clickable { isSelectingHours = true }
                            .padding(4.dp)
                    ) {
                        Text(
                            text = String.format(Locale.US, "%02d", hour12),
                            fontSize = 32.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isSelectingHours) primaryColor else textPrimary,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                        )
                    }

                    Text(":", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = textSecondary, modifier = Modifier.padding(horizontal = 2.dp))

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (!isSelectingHours) primaryColor.copy(alpha = 0.12f) else Color(0xFFF1F5F9),
                        border = BorderStroke(2.dp, if (!isSelectingHours) primaryColor else Color.Transparent),
                        modifier = Modifier
                            .clickable { isSelectingHours = false }
                            .padding(4.dp)
                    ) {
                        Text(
                            text = String.format(Locale.US, "%02d", selectedMinute),
                            fontSize = 32.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (!isSelectingHours) primaryColor else textPrimary,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        val amAllowed = isAmSelectable()
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (!isPm) primaryColor else if (amAllowed) Color(0xFFF1F5F9) else Color(0xFFF8FAFC),
                            border = BorderStroke(1.dp, if (!isPm) primaryColor else Color(0xFFE2E8F0)),
                            modifier = Modifier.clickable(enabled = amAllowed) {
                                if (amAllowed) isPm = false
                            }
                        ) {
                            Text(
                                text = "AM",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (!isPm) Color.White else if (amAllowed) textPrimary else Color(0xFFCBD5E1),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isPm) primaryColor else Color(0xFFF1F5F9),
                            border = BorderStroke(1.dp, if (isPm) primaryColor else Color(0xFFE2E8F0)),
                            modifier = Modifier.clickable {
                                isPm = true
                            }
                        ) {
                            Text(
                                text = "PM",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isPm) Color.White else textPrimary,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .size(230.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFF8FAFC))
                        .border(1.dp, Color(0xFFE2E8F0), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val centerOffset = Offset(size.width / 2f, size.height / 2f)
                        val radius = (size.minDimension / 2f) - 32.dp.toPx()
                        val rad = (animatedAngle - 90f) * (PI.toFloat() / 180f)
                        val handEnd = Offset(
                            x = centerOffset.x + radius * cos(rad),
                            y = centerOffset.y + radius * sin(rad)
                        )

                        drawLine(
                            color = Color(0xFFE11D48),
                            start = centerOffset,
                            end = handEnd,
                            strokeWidth = 3.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                        drawCircle(
                            color = Color(0xFFE11D48),
                            radius = 15.dp.toPx(),
                            center = handEnd
                        )
                        drawCircle(
                            color = Color(0xFFE11D48),
                            radius = 5.dp.toPx(),
                            center = centerOffset
                        )
                    }

                    val itemsList = if (isSelectingHours) (1..12).toList() else listOf(0, 5, 10, 15, 20, 25, 30, 35, 40, 45, 50, 55)

                    itemsList.forEach { value ->
                        val itemAngle = if (isSelectingHours) (value % 12) * 30f else value * 6f
                        val isSelected = if (isSelectingHours) hour12 == value else selectedMinute == value
                        val isSelectable = if (isSelectingHours) isHourSelectable(value, isPm) else isMinuteSelectable(value)

                        val rad = (itemAngle - 90f) * (PI.toFloat() / 180f)
                        val distanceDp = 82.dp
                        val offsetX = distanceDp * cos(rad)
                        val offsetY = distanceDp * sin(rad)

                        Box(
                            modifier = Modifier
                                .offset(x = offsetX, y = offsetY)
                                .size(30.dp)
                                .clip(CircleShape)
                                .clickable(enabled = isSelectable) {
                                    if (isSelectingHours) {
                                        hour12 = value
                                        isSelectingHours = false
                                    } else {
                                        selectedMinute = value
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (isSelectingHours) "$value" else String.format(Locale.US, "%02d", value),
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Bold,
                                color = when {
                                    isSelected -> Color.White
                                    !isSelectable -> Color(0xFFCBD5E1)
                                    else -> textPrimary
                                }
                            )
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(
                        onClick = { isSelectingHours = true },
                        colors = ButtonDefaults.textButtonColors(contentColor = if (isSelectingHours) primaryColor else textSecondary)
                    ) {
                        Text("1. Pick Hour", fontWeight = if (isSelectingHours) FontWeight.ExtraBold else FontWeight.Medium, fontSize = 12.sp)
                    }
                    TextButton(
                        onClick = { isSelectingHours = false },
                        colors = ButtonDefaults.textButtonColors(contentColor = if (!isSelectingHours) primaryColor else textSecondary)
                    ) {
                        Text("2. Pick Minute", fontWeight = if (!isSelectingHours) FontWeight.ExtraBold else FontWeight.Medium, fontSize = 12.sp)
                    }
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isFutureValid) Color(0xFFF0FDF4) else Color(0xFFFFF1F2),
                    border = BorderStroke(1.dp, if (isFutureValid) Color(0xFFBBF7D0) else Color(0xFFFECDD3)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Meal Time:", fontSize = 11.sp, color = textSecondary)
                        if (isFutureValid) {
                            val diffMins = ((selectedCal.timeInMillis - System.currentTimeMillis()) / 60000).toInt()
                            val hrs = diffMins / 60
                            val mins = diffMins % 60
                            Text(
                                text = "$timeFormatted (in ${hrs}h ${mins}m)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF15803D)
                            )
                        } else {
                            Text(
                                text = "Please pick upcoming future time",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFE11D48)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val finalH24 = to24Hour(hour12, isPm)
                    onTimeSelected(finalH24, selectedMinute)
                },
                enabled = isFutureValid,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFE11D48),
                    disabledContainerColor = Color(0xFFE2E8F0),
                    disabledContentColor = Color(0xFF94A3B8)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Confirm Meal Time")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color(0xFF64748B))
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MealGatheringScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val primaryColor = Color(0xFFE11D48)
    val textPrimary = AppTheme.InkPrimary
    val textSecondary = AppTheme.InkSecondary

    val currentUserEmail = remember { EncryptionManager.getString(context, "user_email") ?: "student@college.edu" }
    val currentUserName = remember {
        currentUserEmail.substringBefore("@").replace(".", " ").replaceFirstChar {
            if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString()
        }
    }

    var selectedFilterCategory by remember { mutableStateOf("ALL") }
    var searchQuery by remember { mutableStateOf("") }
    var showCreateCircleDialog by remember { mutableStateOf(false) }
    var showFoodWheelDialog by remember { mutableStateOf(false) }
    var prefilledSpotForCreate by remember { mutableStateOf("") }
    var selectedCircleId by remember { mutableStateOf<String?>(null) }
    var circleForHostTransfer by remember { mutableStateOf<MealCircle?>(null) }
    var refreshCounter by remember { mutableStateOf(0) }

    val walletBalance = remember(MealGatheringRepository.activeCircles.size, refreshCounter) {
        WalletManager.getBalance(context)
    }

    val dynamicCategories by remember(MealGatheringRepository.activeCircles.size, refreshCounter) {
        derivedStateOf {
            val standard = setOf("Delivery Split", "Campus Canteen", "Dine-Out", "Late-Night Bite")
            MealGatheringRepository.activeCircles
                .map { it.mealCategory }
                .filter { it !in standard && it.isNotBlank() }
                .distinct()
        }
    }

    val filteredCircles by remember(selectedFilterCategory, searchQuery, refreshCounter, MealGatheringRepository.activeCircles.size) {
        derivedStateOf {
            val now = System.currentTimeMillis()
            val query = searchQuery.trim()
            MealGatheringRepository.activeCircles.filter { circle ->
                val notExpired = circle.targetTimestamp > now
                val isActive = circle.status != "COMPLETED" && circle.status != "CANCELLED" && notExpired

                // Instant Direct ID search (e.g. MEAL-XXXX or suffix): bypasses category restrictions
                val isDirectIdMatch = query.isNotEmpty() && (
                    circle.id.contains(query, ignoreCase = true) ||
                    circle.id.replace("MEAL-", "").contains(query, ignoreCase = true)
                )
                if (isDirectIdMatch) {
                    return@filter true
                }

                val matchesCategory = when (selectedFilterCategory) {
                    "ALL" -> isActive
                    "Delivery Split" -> isActive && circle.mealCategory.equals("Delivery Split", ignoreCase = true)
                    "Campus Canteen" -> isActive && circle.mealCategory.equals("Campus Canteen", ignoreCase = true)
                    "Dine-Out" -> isActive && circle.mealCategory.equals("Dine-Out", ignoreCase = true)
                    "Late-Night Bite" -> isActive && circle.mealCategory.equals("Late-Night Bite", ignoreCase = true)
                    "PURE_VEG" -> isActive && circle.dietaryVibeTags.any { it.equals("Pure Veg", ignoreCase = true) || it.equals("Jain Food", ignoreCase = true) }
                    "FEMALE_ONLY" -> isActive && circle.femaleOnly
                    "MY_CIRCLES" -> circle.hostEmail == currentUserEmail || circle.members.any { it.email == currentUserEmail }
                    else -> isActive && circle.mealCategory.equals(selectedFilterCategory, ignoreCase = true)
                }

                val matchesSearch = query.isEmpty() ||
                        circle.id.contains(query, ignoreCase = true) ||
                        circle.restaurantSpot.contains(query, ignoreCase = true) ||
                        circle.meetingPoint.contains(query, ignoreCase = true) ||
                        circle.notes.contains(query, ignoreCase = true) ||
                        circle.mealCategory.contains(query, ignoreCase = true) ||
                        circle.dietaryVibeTags.any { it.contains(query, ignoreCase = true) }

                matchesCategory && matchesSearch
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column(modifier = Modifier.padding(end = 4.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Food Circles", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = textPrimary, maxLines = 1)
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .background(Color(0xFFFEF2F2), RoundedCornerShape(6.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text("FREE", color = Color(0xFFDC2626), fontSize = 10.sp, fontWeight = FontWeight.ExtraBold)
                            }
                        }
                        Text("Split food orders & save together", fontSize = 11.sp, color = textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = textPrimary
                        )
                    }
                },
                actions = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        CircularRefreshButton(
                            onClick = {
                                try { (context.getSystemService(Context.VIBRATOR_SERVICE) as? android.os.Vibrator)?.vibrate(30) } catch (e: Exception) {}
                                MealGatheringRepository.refreshAndClean(context)
                                refreshCounter++
                                Toast.makeText(context, "Meal circles refreshed.", Toast.LENGTH_SHORT).show()
                            },
                            tint = Color(0xFFE11D48)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        UnifiedWalletBadge(
                            balance = walletBalance,
                            onClick = { context.startActivity(Intent(context, WalletActivity::class.java)) }
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    try { (context.getSystemService(Context.VIBRATOR_SERVICE) as? android.os.Vibrator)?.vibrate(30) } catch (e: Exception) {}
                    showCreateCircleDialog = true
                },
                containerColor = primaryColor,
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp),
                icon = { Text("+", fontSize = 20.sp, fontWeight = FontWeight.Bold) },
                text = { Text("Host a Meal Circle", fontWeight = FontWeight.Bold, fontSize = 14.sp) }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(AppTheme.Background)
        ) {
            val buddiesMet = remember(MealGatheringRepository.activeCircles.size, refreshCounter) {
                MealGatheringRepository.calculateBuddiesMet(currentUserEmail)
            }

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 5.dp)
                    .clickable {
                        try { (context.getSystemService(Context.VIBRATOR_SERVICE) as? android.os.Vibrator)?.vibrate(30) } catch (e: Exception) {}
                        showFoodWheelDialog = true
                    },
                shape = RoundedCornerShape(14.dp),
                color = Color(0xFFFFF1F2),
                border = BorderStroke(1.dp, Color(0xFFFECDD3))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Text("🍕", fontSize = 18.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "$buddiesMet Campus Food Buddies Met",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 13.sp,
                                color = Color(0xFF9F1239)
                            )
                            Text(
                                text = "Splitting late-night orders & hostel meals",
                                fontSize = 10.sp,
                                color = Color(0xFFBE123C)
                            )
                        }
                    }
                    Button(
                        onClick = {
                            try { (context.getSystemService(Context.VIBRATOR_SERVICE) as? android.os.Vibrator)?.vibrate(30) } catch (e: Exception) {}
                            showFoodWheelDialog = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE11D48)),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.height(30.dp)
                    ) {
                        Text("🎲 Spin Wheel", fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                    }
                }
            }

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFFF0FDF4),
                border = BorderStroke(1.dp, Color(0xFFBBF7D0))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "100% Free Campus Community Service - Rs. 0 Platform Fee",
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = Color(0xFF166534)
                        )
                        Text(
                            text = "Free to host and join. Settle food bills directly with buddies or delivery agent.",
                            fontSize = 10.sp,
                            color = Color(0xFF15803D)
                        )
                    }
                }
            }

            // Compact Search Bar with reduced vertical height and instant clear
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 3.dp),
                shape = RoundedCornerShape(12.dp),
                color = Color.White,
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                shadowElevation = 0.5.dp
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text("🔍", fontSize = 13.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(modifier = Modifier.weight(1f)) {
                        if (searchQuery.isEmpty()) {
                            Text(
                                text = "Search by Spot or Circle ID (e.g. MEAL-XXXX)...",
                                fontSize = 12.sp,
                                color = Color(0xFF94A3B8)
                            )
                        }
                        BasicTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            singleLine = true,
                            textStyle = androidx.compose.ui.text.TextStyle(
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = textPrimary
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    if (searchQuery.isNotEmpty()) {
                        Text(
                            text = "✕",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF94A3B8),
                            modifier = Modifier
                                .clip(CircleShape)
                                .clickable { searchQuery = "" }
                                .padding(4.dp)
                        )
                    }
                }
            }

            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    MealFilterChip("All Meals", selected = selectedFilterCategory == "ALL") {
                        try { (context.getSystemService(Context.VIBRATOR_SERVICE) as? android.os.Vibrator)?.vibrate(30) } catch (e: Exception) {}
                        selectedFilterCategory = "ALL"
                    }
                }
                item {
                    MealFilterChip("Delivery Split", selected = selectedFilterCategory == "Delivery Split") {
                        try { (context.getSystemService(Context.VIBRATOR_SERVICE) as? android.os.Vibrator)?.vibrate(30) } catch (e: Exception) {}
                        selectedFilterCategory = "Delivery Split"
                    }
                }
                item {
                    MealFilterChip("Campus Canteen", selected = selectedFilterCategory == "Campus Canteen") {
                        try { (context.getSystemService(Context.VIBRATOR_SERVICE) as? android.os.Vibrator)?.vibrate(30) } catch (e: Exception) {}
                        selectedFilterCategory = "Campus Canteen"
                    }
                }
                item {
                    MealFilterChip("Dine-Out", selected = selectedFilterCategory == "Dine-Out") {
                        try { (context.getSystemService(Context.VIBRATOR_SERVICE) as? android.os.Vibrator)?.vibrate(30) } catch (e: Exception) {}
                        selectedFilterCategory = "Dine-Out"
                    }
                }
                item {
                    MealFilterChip("Late-Night Bites", selected = selectedFilterCategory == "Late-Night Bite") {
                        try { (context.getSystemService(Context.VIBRATOR_SERVICE) as? android.os.Vibrator)?.vibrate(30) } catch (e: Exception) {}
                        selectedFilterCategory = "Late-Night Bite"
                    }
                }
                items(dynamicCategories) { cat ->
                    MealFilterChip(cat, selected = selectedFilterCategory == cat) {
                        try { (context.getSystemService(Context.VIBRATOR_SERVICE) as? android.os.Vibrator)?.vibrate(30) } catch (e: Exception) {}
                        selectedFilterCategory = cat
                    }
                }
                item {
                    MealFilterChip("Pure Veg", selected = selectedFilterCategory == "PURE_VEG") {
                        selectedFilterCategory = "PURE_VEG"
                    }
                }
                item {
                    MealFilterChip("Female Only", selected = selectedFilterCategory == "FEMALE_ONLY") {
                        selectedFilterCategory = "FEMALE_ONLY"
                    }
                }
                item {
                    MealFilterChip("My Circles", selected = selectedFilterCategory == "MY_CIRCLES") {
                        selectedFilterCategory = "MY_CIRCLES"
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            if (filteredCircles.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = if (selectedFilterCategory == "MY_CIRCLES") "No active meal circles in your history." else "No meal circles matching your search.",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = textPrimary,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Host a group order or gathering to split costs with fellow students.",
                            fontSize = 12.sp,
                            color = textSecondary,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        OutlinedButton(
                            onClick = {
                                SystemNotificationHelper.showNotification(
                                    context,
                                    "🔔 Food Circle Radar Active!",
                                    "We'll alert you the moment a new meal circle or late-night order is hosted."
                                )
                                SoundEffectHelper.playSuccess(context)
                                Toast.makeText(context, "🔔 Food Radar Active: You'll be notified when a meal circle opens!", Toast.LENGTH_LONG).show()
                            },
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, primaryColor),
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp)
                        ) {
                            Text("🔔 Alert Me When a Meal Circle Opens", fontSize = 12.sp, color = primaryColor, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = {
                                    MealGatheringRepository.refreshAndClean(context)
                                    refreshCounter++
                                    Toast.makeText(context, "Refreshed.", Toast.LENGTH_SHORT).show()
                                },
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Refresh Feed")
                            }
                            Button(
                                onClick = { showCreateCircleDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Host a Meal Circle")
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 88.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredCircles, key = { it.id }) { circle ->
                        MealCircleCard(
                            circle = circle,
                            currentUserEmail = currentUserEmail,
                            onJoinClick = {
                                if (circle.joinedBuddies >= circle.totalBuddies || circle.members.size >= circle.totalBuddies) {
                                    Toast.makeText(context, "This meal circle is completely full.", Toast.LENGTH_SHORT).show()
                                } else {
                                    val member = MealMember(
                                        email = currentUserEmail,
                                        name = currentUserName,
                                        phone = "+91 98765 00000",
                                        hostel = "Campus Hostel",
                                        hideContactInPreview = true
                                    )
                                    val joined = MealGatheringRepository.joinCircle(context, circle.id, member)
                                    if (joined) {
                                        refreshCounter++
                                        SoundEffectHelper.playActionComplete(context)
                                        Toast.makeText(context, "Joined meal circle for ${circle.restaurantSpot}.", Toast.LENGTH_SHORT).show()
                                        selectedCircleId = circle.id
                                    } else {
                                        Toast.makeText(context, "Unable to join circle.", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            onViewDetails = {
                                selectedCircleId = circle.id
                            }
                        )
                    }
                }
            }
        }
    }

    if (showCreateCircleDialog) {
        CreateMealCircleDialog(
            currentUserEmail = currentUserEmail,
            currentUserName = currentUserName,
            initialSpot = prefilledSpotForCreate,
            onDismiss = {
                showCreateCircleDialog = false
                prefilledSpotForCreate = ""
            },
            onCircleCreated = { newCircle ->
                MealGatheringRepository.createCircle(context, newCircle)
                refreshCounter++
                showCreateCircleDialog = false
                prefilledSpotForCreate = ""
                SoundEffectHelper.playSuccess(context)
                Toast.makeText(context, "Meal Circle Published Free on Campus Network.", Toast.LENGTH_LONG).show()
            }
        )
    }

    circleForHostTransfer?.let { circle ->
        MealHostTransferDialog(
            circle = circle,
            currentUserEmail = currentUserEmail,
            onDismiss = { circleForHostTransfer = null },
            onSelectMemberForTransfer = { targetMember ->
                MealGatheringRepository.requestHostTransfer(context, circle.id, targetMember.email, targetMember.name)
                refreshCounter++
                circleForHostTransfer = null
                selectedCircleId = null
                SoundEffectHelper.playActionComplete(context)
                Toast.makeText(context, "Host transfer request sent to " + targetMember.name + ". Ownership will transfer once accepted.", Toast.LENGTH_LONG).show()
            },
            onCancelWholeCircle = {
                val (success, msg) = MealGatheringRepository.cancelCircle(context, circle.id)
                refreshCounter++
                circleForHostTransfer = null
                selectedCircleId = null
                Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
            }
        )
    }

    selectedCircleId?.let { cid ->
        val activeCircle = MealGatheringRepository.activeCircles.find { it.id == cid }
        if (activeCircle != null) {
            MealCircleDetailDialog(
                circle = activeCircle,
                currentUserEmail = currentUserEmail,
                currentUserName = currentUserName,
                onDismiss = { selectedCircleId = null },
                onJoinClick = {
                    if (activeCircle.joinedBuddies >= activeCircle.totalBuddies || activeCircle.members.size >= activeCircle.totalBuddies) {
                        Toast.makeText(context, "This meal circle is completely full.", Toast.LENGTH_SHORT).show()
                    } else {
                        val member = MealMember(
                            email = currentUserEmail,
                            name = currentUserName,
                            phone = "+91 98765 00000",
                            hostel = "Campus Hostel",
                            hideContactInPreview = true
                        )
                        val joined = MealGatheringRepository.joinCircle(context, activeCircle.id, member)
                        if (joined) {
                            refreshCounter++
                            SoundEffectHelper.playActionComplete(context)
                            Toast.makeText(context, "Joined meal circle.", Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                onRequestHostTransfer = {
                    circleForHostTransfer = activeCircle
                },
                onAcceptHostTransfer = {
                    val accepted = MealGatheringRepository.acceptHostTransfer(context, activeCircle.id, currentUserEmail)
                    refreshCounter++
                    if (accepted) {
                        SoundEffectHelper.playActionComplete(context)
                        Toast.makeText(context, "Host transfer accepted. You are now the host.", Toast.LENGTH_SHORT).show()
                    }
                },
                onDeclineHostTransfer = {
                    try { (context.getSystemService(Context.VIBRATOR_SERVICE) as? android.os.Vibrator)?.vibrate(30) } catch (e: Exception) {}
                    MealGatheringRepository.declineHostTransfer(context, activeCircle.id)
                    refreshCounter++
                    Toast.makeText(context, "Host transfer declined.", Toast.LENGTH_SHORT).show()
                },
                onLeaveCircle = {
                    try { (context.getSystemService(Context.VIBRATOR_SERVICE) as? android.os.Vibrator)?.vibrate(30) } catch (e: Exception) {}
                    val (success, msg) = MealGatheringRepository.leaveCircle(context, activeCircle.id, currentUserEmail)
                    refreshCounter++
                    if (success) selectedCircleId = null
                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                },
                onCancelCircle = {
                    try { (context.getSystemService(Context.VIBRATOR_SERVICE) as? android.os.Vibrator)?.vibrate(30) } catch (e: Exception) {}
                    val (success, msg) = MealGatheringRepository.cancelCircle(context, activeCircle.id)
                    refreshCounter++
                    if (success) selectedCircleId = null
                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                },
                onCompleteCircle = {
                    SoundEffectHelper.playSuccess(context)
                    MealGatheringRepository.completeCircle(context, activeCircle.id)
                    refreshCounter++
                    selectedCircleId = null
                    Toast.makeText(context, "Meal Circle Completed.", Toast.LENGTH_SHORT).show()
                },
                onUpdateOrderTracking = { trackingText ->
                    SoundEffectHelper.playActionComplete(context)
                    MealGatheringRepository.updateOrderTracking(context, activeCircle.id, trackingText)
                    refreshCounter++
                    Toast.makeText(context, "Order tracking note updated.", Toast.LENGTH_SHORT).show()
                },
                onSendMessage = { msg ->
                    try { (context.getSystemService(Context.VIBRATOR_SERVICE) as? android.os.Vibrator)?.vibrate(30) } catch (e: Exception) {}
                    val chat = MealChatMessage(
                        senderEmail = currentUserEmail,
                        senderName = currentUserName,
                        message = msg
                    )
                    MealGatheringRepository.addChatMessage(context, activeCircle.id, chat)
                    refreshCounter++
                }
            )
        } else {
            selectedCircleId = null
        }
    }

    if (showFoodWheelDialog) {
        CampusFoodWheelDialog(
            onDismiss = { showFoodWheelDialog = false },
            onSelectSpot = { spot ->
                searchQuery = spot
                selectedFilterCategory = "ALL"
            },
            onHostSpot = { spot ->
                prefilledSpotForCreate = spot
                showCreateCircleDialog = true
            }
        )
    }
}

@Composable
fun CampusFoodWheelDialog(
    onDismiss: () -> Unit,
    onSelectSpot: (String) -> Unit,
    onHostSpot: (String) -> Unit
) {
    val context = LocalContext.current
    val defaultSpots = remember {
        listOf(
            "Campus Main Canteen 🍛",
            "Maggi Hotspot 🍜",
            "Dominos Pizza 🍕",
            "Roll Corner 🌯",
            "Meghana Biryani 🍗",
            "Chai Tapri ☕",
            "South Indian Thali 🍱",
            "Burger Point 🍔"
        )
    }

    val spots = remember {
        val savedJson = EncryptionManager.getString(context, "custom_food_wheel_spots")
        val initialList = if (!savedJson.isNullOrEmpty()) {
            try {
                val type = object : TypeToken<List<String>>() {}.type
                val parsed: List<String> = Gson().fromJson(savedJson, type)
                if (parsed.size >= 5) parsed else defaultSpots
            } catch (e: Exception) {
                defaultSpots
            }
        } else {
            defaultSpots
        }
        mutableStateListOf<String>().apply { addAll(initialList) }
    }

    fun saveSpots() {
        try {
            val json = Gson().toJson(spots.toList())
            EncryptionManager.saveString(context, "custom_food_wheel_spots", json)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    val colors = listOf(
        Color(0xFFF43F5E), // Pink / Rose
        Color(0xFF8B5CF6), // Purple / Violet
        Color(0xFFFF7A00), // Electric Orange
        Color(0xFFE11D48), // Bright Red
        Color(0xFFD946EF), // Vivid Magenta
        Color(0xFF22C55E), // Electric Green
        Color(0xFFF59E0B), // Golden Yellow
        Color(0xFF0EA5E9), // Vivid Sky Blue
        Color(0xFF14B8A6), // Teal
        Color(0xFF3B82F6), // Royal Blue
        Color(0xFFF97316), // Vivid Amber
        Color(0xFF6366F1)  // Indigo
    )

    var isEditMode by remember { mutableStateOf(false) }
    var newSpotName by remember { mutableStateOf("") }
    val rotation = remember { Animatable(0f) }
    var selectedIndex by remember { mutableStateOf(0) }
    var isSpinning by remember { mutableStateOf(false) }
    var hasSpun by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    fun spinWheel() {
        if (!isSpinning && spots.size >= 5) {
            scope.launch {
                isSpinning = true
                try { (context.getSystemService(Context.VIBRATOR_SERVICE) as? android.os.Vibrator)?.vibrate(30) } catch (e: Exception) {}
                val randomSlice = (0 until spots.size).random()
                val sliceDeg = 360f / spots.size.toFloat()
                val sliceMidAngle = randomSlice * sliceDeg + (sliceDeg / 2f)
                val targetOffset = ((270f - sliceMidAngle) % 360f + 360f) % 360f
                val currentRotation = rotation.value
                val currentModulo = ((currentRotation % 360f) + 360f) % 360f
                val deltaToTarget = ((targetOffset - currentModulo) % 360f + 360f) % 360f
                val extraRounds = 5 * 360f
                val finalTarget = currentRotation + extraRounds + deltaToTarget

                rotation.animateTo(
                    targetValue = finalTarget,
                    animationSpec = tween(
                        durationMillis = 2800,
                        easing = FastOutSlowInEasing
                    )
                )
                selectedIndex = randomSlice
                isSpinning = false
                hasSpun = true
                SoundEffectHelper.playSuccess(context)
            }
        }
    }

    LaunchedEffect(Unit) {
        if (!isEditMode) {
            spinWheel()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        containerColor = Color.White,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (isEditMode) "⚙️ Customize Wheel" else "🎲 Campus Food Decider",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 17.sp,
                            color = Color(0xFFE11D48)
                        )
                    }
                    Text(
                        text = "${spots.size} Spots (Min 5, Max Unlimited)",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF94A3B8)
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isEditMode) Color(0xFFFFF1F2) else Color(0xFFF1F5F9),
                        border = BorderStroke(1.dp, if (isEditMode) Color(0xFFFECDD3) else Color(0xFFE2E8F0)),
                        modifier = Modifier.clickable {
                            try { (context.getSystemService(Context.VIBRATOR_SERVICE) as? android.os.Vibrator)?.vibrate(30) } catch (e: Exception) {}
                            isEditMode = !isEditMode
                        }
                    ) {
                        Text(
                            text = if (isEditMode) "🎯 Wheel" else "✏️ Edit",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isEditMode) Color(0xFFE11D48) else Color(0xFF475569),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    Text(
                        text = "✕",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF94A3B8),
                        modifier = Modifier
                            .clip(CircleShape)
                            .clickable { onDismiss() }
                            .padding(6.dp)
                    )
                }
            }
        },
        text = {
            if (isEditMode) {
                // EDIT MODE: Add, delete, customize spots
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 400.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Add your favorite hostel food joints or remove options. Minimum 5 spots required for the spin wheel.",
                        fontSize = 11.sp,
                        color = Color(0xFF64748B)
                    )

                    // Add new spot input
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFFF8FAFC),
                            border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
                                if (newSpotName.isEmpty()) {
                                    Text("Add spot name (e.g. KFC, Subway)...", fontSize = 11.sp, color = Color(0xFF94A3B8))
                                }
                                BasicTextField(
                                    value = newSpotName,
                                    onValueChange = { newSpotName = it },
                                    singleLine = true,
                                    textStyle = androidx.compose.ui.text.TextStyle(
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = AppTheme.InkPrimary
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Button(
                            onClick = {
                                val trimmed = newSpotName.trim()
                                if (trimmed.isNotEmpty()) {
                                    if (spots.any { it.equals(trimmed, ignoreCase = true) }) {
                                        Toast.makeText(context, "Spot already in wheel!", Toast.LENGTH_SHORT).show()
                                    } else {
                                        spots.add(trimmed)
                                        saveSpots()
                                        newSpotName = ""
                                        SoundEffectHelper.playActionComplete(context)
                                        Toast.makeText(context, "Added $trimmed!", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE11D48)),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.height(36.dp)
                        ) {
                            Text("+ Add", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Quick suggestion pills
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val suggestions = listOf("KFC 🍗", "Subway 🥪", "Momos Point 🥟", "Dosa Corner 🥞", "Shawarma Hub 🌯", "Tea Post ☕", "Juice Bar 🥤")
                        items(suggestions) { sug ->
                            val alreadyAdded = spots.any { it.contains(sug.substringBefore(" "), ignoreCase = true) }
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (alreadyAdded) Color(0xFFF1F5F9) else Color(0xFFFFF1F2),
                                border = BorderStroke(0.8.dp, if (alreadyAdded) Color(0xFFE2E8F0) else Color(0xFFFECDD3)),
                                modifier = Modifier.clickable(enabled = !alreadyAdded) {
                                    spots.add(sug)
                                    saveSpots()
                                    SoundEffectHelper.playActionComplete(context)
                                }
                            ) {
                                Text(
                                    text = if (alreadyAdded) "✓ $sug" else "+ $sug",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (alreadyAdded) Color(0xFF94A3B8) else Color(0xFFE11D48),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    // Scrollable list of current spots
                    Text(
                        text = "Current Wheel Capacity: ${spots.size} options (Min: 5, Max: Unlimited)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF334155)
                    )

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false)
                            .heightIn(max = 180.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        itemsIndexed(spots) { idx, spot ->
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFFF8FAFC),
                                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 10.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .clip(CircleShape)
                                                .background(colors[idx % colors.size])
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = spot,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = AppTheme.InkPrimary
                                        )
                                    }

                                    val canDelete = spots.size > 5
                                    Text(
                                        text = "🗑️",
                                        fontSize = 13.sp,
                                        modifier = Modifier
                                            .alpha(if (canDelete) 1f else 0.25f)
                                            .clip(CircleShape)
                                            .clickable(enabled = canDelete) {
                                                if (spots.size > 5) {
                                                    spots.removeAt(idx)
                                                    saveSpots()
                                                    try { (context.getSystemService(Context.VIBRATOR_SERVICE) as? android.os.Vibrator)?.vibrate(30) } catch (e: Exception) {}
                                                } else {
                                                    Toast.makeText(context, "Wheel requires minimum 5 options!", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                            .padding(4.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Bottom controls in edit mode
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                spots.clear()
                                spots.addAll(defaultSpots)
                                saveSpots()
                                try { (context.getSystemService(Context.VIBRATOR_SERVICE) as? android.os.Vibrator)?.vibrate(30) } catch (e: Exception) {}
                                Toast.makeText(context, "Reset to 8 default campus spots.", Toast.LENGTH_SHORT).show()
                            },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f).height(36.dp),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Text("🔄 Reset Defaults", fontSize = 10.sp)
                        }

                        Button(
                            onClick = {
                                isEditMode = false
                                spinWheel()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE11D48)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f).height(36.dp),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Text("✓ Done & Spin", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else {
                // WHEEL SPINNER MODE
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Can't decide what to eat? Spin your custom wheel!",
                        fontSize = 11.sp,
                        color = Color(0xFF64748B),
                        textAlign = TextAlign.Center
                    )

                    // The Recreated Spin Wheel UI - Matching Reference Design
                    Box(
                        modifier = Modifier
                            .size(230.dp)
                            .padding(vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        // 1. Inner Rotating Multi-Colored Slices (184.dp)
                        Canvas(
                            modifier = Modifier
                                .size(184.dp)
                                .clip(CircleShape)
                                .rotate(rotation.value)
                        ) {
                            val count = spots.size.coerceAtLeast(5)
                            val sliceAngle = 360f / count.toFloat()
                            val radius = size.minDimension / 2f
                            val center = Offset(size.width / 2f, size.height / 2f)

                            // Draw colorful pie slices
                            for (i in 0 until count) {
                                drawArc(
                                    color = colors[i % colors.size],
                                    startAngle = i * sliceAngle,
                                    sweepAngle = sliceAngle,
                                    useCenter = true
                                )
                            }

                            // Subtle white slice dividing lines
                            for (i in 0 until count) {
                                val rad = Math.toRadians((i * sliceAngle).toDouble())
                                val lineX = center.x + radius * cos(rad).toFloat()
                                val lineY = center.y + radius * sin(rad).toFloat()
                                drawLine(
                                    color = Color.White.copy(alpha = 0.25f),
                                    start = center,
                                    end = Offset(lineX, lineY),
                                    strokeWidth = 1.5.dp.toPx()
                                )
                            }
                        }

                        // 2. Static Outer Navy Blue Bezel Ring with 12 Glowing Bulb Lights (220.dp)
                        Canvas(
                            modifier = Modifier.size(220.dp)
                        ) {
                            val center = Offset(size.width / 2f, size.height / 2f)
                            val outerRadius = size.minDimension / 2f
                            val bezelThickness = 18.dp.toPx()

                            // Outer navy ring body
                            drawCircle(
                                color = Color(0xFF133266),
                                radius = outerRadius - (bezelThickness / 2f),
                                center = center,
                                style = androidx.compose.ui.graphics.drawscope.Stroke(width = bezelThickness)
                            )

                            // Outer dark bezel rim
                            drawCircle(
                                color = Color(0xFF0A1C3E),
                                radius = outerRadius - 1.dp.toPx(),
                                center = center,
                                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx())
                            )

                            // Inner shadow border adjacent to rotating wheel
                            drawCircle(
                                color = Color(0x66000000),
                                radius = outerRadius - bezelThickness,
                                center = center,
                                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.5.dp.toPx())
                            )

                            // 12 Glowing light bulb studs around the navy ring
                            val bulbTrackRadius = outerRadius - (bezelThickness / 2f)
                            for (i in 0 until 12) {
                                val angleRad = Math.toRadians(i * 30.0)
                                val bx = center.x + bulbTrackRadius * cos(angleRad).toFloat()
                                val by = center.y + bulbTrackRadius * sin(angleRad).toFloat()

                                // Soft golden glow
                                drawCircle(
                                    color = Color(0x99FFF59D),
                                    radius = 5.dp.toPx(),
                                    center = Offset(bx, by)
                                )
                                // Bright white-yellow bulb
                                drawCircle(
                                    color = Color(0xFFFFFDE7),
                                    radius = 3.2.dp.toPx(),
                                    center = Offset(bx, by)
                                )
                                // Center hot core
                                drawCircle(
                                    color = Color(0xFFFFEE58),
                                    radius = 1.8.dp.toPx(),
                                    center = Offset(bx, by)
                                )
                            }
                        }

                        // 3. Center "SPIN" Hub Button
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF0A1C3E))
                                .border(2.5.dp, Color(0xFF1E3A8A), CircleShape)
                                .clickable(enabled = !isSpinning) { spinWheel() },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "SPIN",
                                color = Color.White,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 13.sp,
                                letterSpacing = 1.sp
                            )
                        }

                        // 4. Golden Top Indicator Pointer (Arrowhead needle)
                        Canvas(
                            modifier = Modifier
                                .size(width = 34.dp, height = 46.dp)
                                .align(Alignment.TopCenter)
                                .offset(y = (-4).dp)
                        ) {
                            val w = size.width
                            val h = size.height

                            // Top anchor tab
                            val tabPath = androidx.compose.ui.graphics.Path().apply {
                                moveTo(w * 0.15f, 0f)
                                lineTo(w * 0.85f, 0f)
                                lineTo(w * 0.80f, h * 0.22f)
                                lineTo(w * 0.20f, h * 0.22f)
                                close()
                            }
                            drawPath(tabPath, color = Color(0xFFEAB308))

                            // Main golden triangular needle pointing straight down
                            val needlePath = androidx.compose.ui.graphics.Path().apply {
                                moveTo(w * 0.12f, h * 0.16f)
                                lineTo(w * 0.88f, h * 0.16f)
                                lineTo(w * 0.50f, h)
                                close()
                            }
                            drawPath(needlePath, color = Color(0xFFFBBF24))

                            // 3D light reflection highlight on left facet
                            val highlightPath = androidx.compose.ui.graphics.Path().apply {
                                moveTo(w * 0.12f, h * 0.16f)
                                lineTo(w * 0.50f, h * 0.16f)
                                lineTo(w * 0.50f, h)
                                close()
                            }
                            drawPath(highlightPath, color = Color(0xFFFEF08A).copy(alpha = 0.5f))

                            // Crisp amber needle border
                            drawPath(
                                needlePath,
                                color = Color(0xFFD97706),
                                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.5.dp.toPx())
                            )
                        }
                    }

                    // Winner Card
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = if (hasSpun && !isSpinning) Color(0xFFFFF1F2) else Color(0xFFF8FAFC),
                        border = BorderStroke(1.5.dp, if (hasSpun && !isSpinning) Color(0xFFE11D48) else Color(0xFFE2E8F0)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = if (isSpinning) "🌀 Spinning ${spots.size} spots..." else if (hasSpun) "🎉 TODAY'S CAMPUS PICK 🎉" else "Ready to Spin (${spots.size} Spots)",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (hasSpun && !isSpinning) Color(0xFFE11D48) else Color(0xFF64748B)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            val winnerText = if (isSpinning) "Picking the best spot..." else if (selectedIndex in spots.indices) spots[selectedIndex] else "Ready"
                            Text(
                                text = winnerText,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (hasSpun && !isSpinning) Color(0xFF9F1239) else Color(0xFF0F172A),
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    // Action Buttons
                    Button(
                        onClick = { spinWheel() },
                        enabled = !isSpinning,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE11D48)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().height(40.dp)
                    ) {
                        Text(
                            text = if (isSpinning) "Spinning..." else if (hasSpun) "🎲 Spin Again" else "🎯 Spin the Wheel!",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }

                    if (hasSpun && !isSpinning && selectedIndex in spots.indices) {
                        val chosenSpot = spots[selectedIndex]
                        // Strip trailing emoji symbols, preserving ALL textual words of the option
                        val cleanedSpot = chosenSpot.replace(Regex("[\\p{So}\\p{Cn}\\p{Sk}\\uD83C-\\uDBFF\\uDC00-\\uDFFF]+"), "").trim().ifEmpty { chosenSpot.trim() }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    onSelectSpot(cleanedSpot)
                                    onDismiss()
                                },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f).height(36.dp),
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text("🔍 Find Circles", fontSize = 11.sp, color = Color(0xFFE11D48), fontWeight = FontWeight.Bold)
                            }
                            Button(
                                onClick = {
                                    onHostSpot(cleanedSpot)
                                    onDismiss()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f).height(36.dp),
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text("➕ Host Circle", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {}
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MealFilterChip(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = {
            Text(
                text = label,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                fontSize = 12.sp
            )
        },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = Color(0xFFE11D48),
            selectedLabelColor = Color.White,
            containerColor = Color.White,
            labelColor = AppTheme.InkPrimary
        ),
        shape = RoundedCornerShape(16.dp),
        border = FilterChipDefaults.filterChipBorder(
            borderColor = Color(0xFFE2E8F0),
            selectedBorderColor = Color(0xFFE11D48),
            borderWidth = 1.dp,
            selectedBorderWidth = 1.dp
        )
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MealCircleCard(
    circle: MealCircle,
    currentUserEmail: String,
    onJoinClick: () -> Unit,
    onViewDetails: () -> Unit
) {
    val textPrimary = AppTheme.InkPrimary
    val textSecondary = AppTheme.InkSecondary
    val primaryColor = Color(0xFFE11D48)

    val isUserInCircle = circle.hostEmail == currentUserEmail || circle.members.any { it.email == currentUserEmail }
    val isHost = circle.hostEmail == currentUserEmail
    val isFull = circle.joinedBuddies >= circle.totalBuddies || circle.members.size >= circle.totalBuddies
    val isUnderOneHour = circle.targetTimestamp - System.currentTimeMillis() in 1..3600000
    val isPendingTransferForMe = circle.pendingHostTransferEmail == currentUserEmail

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(2.dp, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, if (isUserInCircle) primaryColor.copy(alpha = 0.5f) else Color(0xFFF1F5F9))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row: Category Badge & Status / Countdown
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .background(Color(0xFFFFF1F2), RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = circle.mealCategory,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = primaryColor
                        )
                    }

                    if (circle.isNightOwl) {
                        Box(
                            modifier = Modifier
                                .background(Color(0xFFEDE9FE), RoundedCornerShape(8.dp))
                                .border(1.dp, Color(0xFFDDD6FE), RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "🌙 Night Owl",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF7C3AED)
                            )
                        }
                    }

                    if (circle.femaleOnly) {
                        Box(
                            modifier = Modifier
                                .background(Color(0xFFFDF2F8), RoundedCornerShape(8.dp))
                                .border(1.dp, Color(0xFFFBCFE8), RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "Female Only",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFDB2777)
                            )
                        }
                    }
                }

                if (isUnderOneHour) {
                    LiveMealCountdownBadge(targetTimestamp = circle.targetTimestamp)
                } else {
                    val statusLabel = if (isFull) "Circle Full" else "${circle.joinedBuddies}/${circle.totalBuddies} Buddies"
                    val statusBg = if (isFull) Color(0xFFFEF3C7) else Color(0xFFF1F5F9)
                    val statusColor = if (isFull) Color(0xFFD97706) else Color(0xFF475569)

                    Box(
                        modifier = Modifier
                            .background(statusBg, RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(text = statusLabel, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = statusColor)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Restaurant & Meeting Spot
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = circle.restaurantSpot,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 16.sp,
                        color = textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Meeting at: " + circle.meetingPoint,
                        fontSize = 12.sp,
                        color = textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFFFFBEB),
                    border = BorderStroke(1.dp, Color(0xFFFDE68A)),
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(circle.targetDate, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFFB45309))
                        Text(circle.targetTime, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF92400E))
                    }
                }
            }

            // ONLY Selected Dietary and Circle Preferences in VERTICAL list
            if (circle.dietaryVibeTags.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    circle.dietaryVibeTags.forEach { tag ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFF8FAFC), RoundedCornerShape(6.dp))
                                .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(6.dp))
                                .padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(primaryColor)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = tag,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF334155)
                            )
                        }
                    }
                }
            }

            if (circle.notes.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "\"" + circle.notes + "\"",
                    fontSize = 11.sp,
                    color = Color(0xFF64748B),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(14.dp))
            Divider(color = Color(0xFFF1F5F9), thickness = 1.dp)
            Spacer(modifier = Modifier.height(12.dp))

            // Per Buddy Split Section - Beautifully Aligned with Generous Spacing
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFFF0FDF4),
                border = BorderStroke(1.dp, Color(0xFFBBF7D0)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        if (circle.estimatedTotalBill > 0 && circle.perPersonEstimate > 0) {
                            Text(
                                text = "Per Buddy Split",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF166534)
                            )
                            Text(
                                text = "Total Bill: Rs. ${circle.estimatedTotalBill} (${circle.totalBuddies} buddies)",
                                fontSize = 10.sp,
                                color = Color(0xFF15803D)
                            )
                        } else {
                            Text(
                                text = "Bill Settlement",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF166534)
                            )
                            Text(
                                text = "Split directly on actuals",
                                fontSize = 10.sp,
                                color = Color(0xFF15803D)
                            )
                        }
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        if (circle.estimatedTotalBill > 0 && circle.perPersonEstimate > 0) {
                            Text(
                                text = "approx. Rs. ${circle.perPersonEstimate}",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 16.sp,
                                color = Color(0xFF15803D)
                            )
                            Text(
                                text = "/ buddy",
                                fontSize = 10.sp,
                                color = Color(0xFF166534)
                            )
                        } else {
                            Text(
                                text = "Direct Split",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 13.sp,
                                color = Color(0xFF15803D)
                            )
                            Text(
                                text = "Pay on order",
                                fontSize = 10.sp,
                                color = Color(0xFF166534)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isUserInCircle) {
                    Button(
                        onClick = onViewDetails,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(if (isHost) "Manage Circle" else "My Circle", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                } else if (isFull) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFF1F5F9),
                        modifier = Modifier.padding(4.dp)
                    ) {
                        Text(
                            "Group Full",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF94A3B8),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }
                } else {
                    Button(
                        onClick = onViewDetails,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text("Join Free (Rs. 0 Fee)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// Host a Meal Circle Dialog (with Dropdown List, Close Button, Clear All, and Unique Unpredictable ID)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateMealCircleDialog(
    currentUserEmail: String,
    currentUserName: String,
    initialSpot: String = "",
    onDismiss: () -> Unit,
    onCircleCreated: (MealCircle) -> Unit
) {
    val context = LocalContext.current
    val primaryColor = Color(0xFFE11D48)
    val textPrimary = AppTheme.InkPrimary
    val textSecondary = AppTheme.InkSecondary
    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()

    val initialCal = remember {
        Calendar.getInstance().apply {
            add(Calendar.HOUR_OF_DAY, 1)
            set(Calendar.MINUTE, 0)
        }
    }

    var selectedYear by remember { mutableStateOf(initialCal.get(Calendar.YEAR)) }
    var selectedMonth by remember { mutableStateOf(initialCal.get(Calendar.MONTH)) }
    var selectedDay by remember { mutableStateOf(initialCal.get(Calendar.DAY_OF_MONTH)) }
    var selectedHour by remember { mutableStateOf(initialCal.get(Calendar.HOUR_OF_DAY)) }
    var selectedMinute by remember { mutableStateOf(initialCal.get(Calendar.MINUTE)) }

    val dateDisplayFormat = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
    val timeDisplayFormat = remember { SimpleDateFormat("hh:mm a", Locale.getDefault()) }

    var targetDate by remember { mutableStateOf(dateDisplayFormat.format(initialCal.time)) }
    var targetTime by remember { mutableStateOf(timeDisplayFormat.format(initialCal.time)) }

    var showTimePickerModal by remember { mutableStateOf(false) }

    var selectedCategory by remember { mutableStateOf("Delivery Split") }
    var customCategoryName by remember { mutableStateOf("") }
    var restaurantSpot by remember { mutableStateOf(initialSpot) }
    var meetingPoint by remember { mutableStateOf("") }
    var totalBuddies by remember { mutableStateOf(4) }
    var estimatedTotalBill by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var femaleOnly by remember { mutableStateOf(false) }
    var hideHostContact by remember { mutableStateOf(true) }

    // Indian Campus Dietary & Circle Preferences List
    val availablePreferences = listOf(
        "Pure Veg",
        "Jain Food",
        "Non Veg",
        "Eggitarian",
        "Halal Friendly",
        "South Indian",
        "North Indian",
        "Street Food",
        "Late Night Cravings",
        "Budget Deal",
        "Birthday Treat",
        "Exam Study Break",
        "Healthy / Diet"
    )

    val selectedPreferences = remember { mutableStateListOf<String>("Budget Deal") }
    var isDietaryDropdownExpanded by remember { mutableStateOf(false) }

    var errorField by remember { mutableStateOf<String?>(null) }
    var validationError by remember { mutableStateOf("") }

    val billInt = estimatedTotalBill.toIntOrNull() ?: 0
    val perPersonEstimate = if (totalBuddies > 0 && billInt > 0) ceil(billInt.toDouble() / totalBuddies.toDouble()).toInt() else 0

    val spotPlaceholder = remember(selectedCategory) {
        when (selectedCategory) {
            "Delivery Split" -> "e.g. Dominos Pizza, Subway, Burger King, Meghana Foods..."
            "Campus Canteen" -> "e.g. Main Canteen Block A, South Mess, Nescafe Hub..."
            "Dine-Out" -> "e.g. BBQ Nation, City Center Cafe, Street Momo Point..."
            "Late-Night Bite" -> "e.g. Night Canteen Maggi Stall, Campus Tea Point..."
            else -> "e.g. Food Truck, Hostel Lawn, Bakery..."
        }
    }

    val currentTargetCal = Calendar.getInstance().apply {
        set(selectedYear, selectedMonth, selectedDay, selectedHour, selectedMinute, 0)
    }
    val isTargetInFuture = currentTargetCal.timeInMillis >= System.currentTimeMillis() + 15 * 60 * 1000

    val isCategoryValid = if (selectedCategory == "Other") {
        CategoryFilterValidator.validateCategoryName(customCategoryName).first
    } else {
        true
    }

    val isFormValid = restaurantSpot.isNotBlank() && meetingPoint.isNotBlank() && isCategoryValid && isTargetInFuture

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        containerColor = Color.White,
        title = {
            Column {
                Text("Host a Campus Meal Circle", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = textPrimary)
                Text("100% Free - Rs. 0 Platform Fee", fontSize = 11.sp, color = Color(0xFF16A34A), fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Category Selector
                Text("Meal Category *", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = textPrimary)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    listOf("Delivery Split" to "Delivery", "Campus Canteen" to "Canteen", "Dine-Out" to "Dine-Out", "Late-Night Bite" to "Late-Night", "Other" to "Other").forEach { (cat, label) ->
                        val sel = selectedCategory == cat
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (sel) primaryColor else Color(0xFFF1F5F9),
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    selectedCategory = cat
                                    errorField = null
                                    validationError = ""
                                }
                        ) {
                            Text(
                                text = label,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (sel) Color.White else Color(0xFF475569),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(vertical = 6.dp)
                            )
                        }
                    }
                }

                if (selectedCategory == "Other") {
                    val isCategoryError = errorField == "CATEGORY"
                    OutlinedTextField(
                        value = customCategoryName,
                        onValueChange = {
                            customCategoryName = it
                            errorField = null
                            validationError = ""
                        },
                        label = { Text("Specify Custom Meal Category *") },
                        placeholder = { Text("e.g. Hostel Party, Exam Treat, Food Fest, Chai Break...", color = Color(0xFF94A3B8).copy(alpha = 0.6f)) },
                        singleLine = true,
                        isError = isCategoryError,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = if (isCategoryError) Color(0xFFE11D48) else primaryColor,
                            unfocusedBorderColor = if (isCategoryError) Color(0xFFE11D48) else Color(0xFFCBD5E1)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                val isSpotError = errorField == "SPOT"
                OutlinedTextField(
                    value = restaurantSpot,
                    onValueChange = {
                        restaurantSpot = it
                        errorField = null
                        validationError = ""
                    },
                    label = { Text("Food Spot or Restaurant Name *") },
                    placeholder = { Text(spotPlaceholder, color = Color(0xFF94A3B8).copy(alpha = 0.6f)) },
                    singleLine = true,
                    isError = isSpotError,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = if (isSpotError) Color(0xFFE11D48) else primaryColor,
                        unfocusedBorderColor = if (isSpotError) Color(0xFFE11D48) else Color(0xFFCBD5E1)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                val isMeetingError = errorField == "MEETING"
                OutlinedTextField(
                    value = meetingPoint,
                    onValueChange = {
                        meetingPoint = it
                        errorField = null
                        validationError = ""
                    },
                    label = { Text("Meeting or Delivery Pickup Location *") },
                    placeholder = { Text("e.g. Hostel 2 Common Room, Canteen Center Table...", color = Color(0xFF94A3B8).copy(alpha = 0.6f)) },
                    singleLine = true,
                    isError = isMeetingError,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = if (isMeetingError) Color(0xFFE11D48) else primaryColor,
                        unfocusedBorderColor = if (isMeetingError) Color(0xFFE11D48) else Color(0xFFCBD5E1)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                val isDateTimeError = errorField == "DATETIME" || !isTargetInFuture
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isDateTimeError) Color(0xFFFFF1F2) else Color(0xFFF8FAFC),
                        border = BorderStroke(1.dp, if (isDateTimeError) Color(0xFFE11D48) else Color(0xFFCBD5E1)),
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                val dp = DatePickerDialog(
                                    context,
                                    { _, y, m, d ->
                                        selectedYear = y
                                        selectedMonth = m
                                        selectedDay = d
                                        val cal = Calendar.getInstance().apply {
                                            set(y, m, d, selectedHour, selectedMinute, 0)
                                        }
                                        targetDate = dateDisplayFormat.format(cal.time)

                                        val minCal = Calendar.getInstance().apply { add(Calendar.MINUTE, 30) }
                                        if (cal.timeInMillis < minCal.timeInMillis) {
                                            selectedHour = minCal.get(Calendar.HOUR_OF_DAY)
                                            selectedMinute = (minCal.get(Calendar.MINUTE) / 5) * 5
                                            val updatedCal = Calendar.getInstance().apply {
                                                set(y, m, d, selectedHour, selectedMinute, 0)
                                            }
                                            targetTime = timeDisplayFormat.format(updatedCal.time)
                                        }
                                        errorField = null
                                        validationError = ""
                                    },
                                    selectedYear,
                                    selectedMonth,
                                    selectedDay
                                )
                                dp.datePicker.minDate = System.currentTimeMillis() - 1000
                                dp.show()
                            }
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
                            Text("Target Date *", fontSize = 10.sp, color = textSecondary)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(targetDate, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = textPrimary)
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isDateTimeError) Color(0xFFFFF1F2) else Color(0xFFF8FAFC),
                        border = BorderStroke(1.dp, if (isDateTimeError) Color(0xFFE11D48) else Color(0xFFCBD5E1)),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { showTimePickerModal = true }
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
                            Text("Order or Meeting Time *", fontSize = 10.sp, color = textSecondary)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(targetTime, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = textPrimary)
                        }
                    }
                }

                Text("Group Capacity: $totalBuddies Buddies", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = textPrimary)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    (2..8).forEach { count ->
                        val sel = totalBuddies == count
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (sel) primaryColor else Color(0xFFF1F5F9),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { totalBuddies = count }
                        ) {
                            Text(
                                text = "$count",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (sel) Color.White else textPrimary,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(vertical = 6.dp)
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = estimatedTotalBill,
                    onValueChange = {
                        estimatedTotalBill = it.filter { char -> char.isDigit() }
                        errorField = null
                        validationError = ""
                    },
                    label = { Text("Estimated Total Bill in Rs. (Optional)") },
                    placeholder = { Text("e.g. 600 (or leave blank if paying on actuals)", color = Color(0xFF94A3B8).copy(alpha = 0.6f)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                // Per Buddy Split with Generous Spacing & Proper Alignment
                if (billInt > 0) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFFF0FDF4),
                        border = BorderStroke(1.dp, Color(0xFFBBF7D0)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFDCFCE7)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("₹", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF15803D))
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text("Per Buddy Split", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF166534))
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text("Total Rs. $billInt ($totalBuddies buddies)", fontSize = 11.sp, fontWeight = FontWeight.Medium, color = Color(0xFF15803D))
                                }
                            }
                            Column(
                                horizontalAlignment = Alignment.End,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text("approx. Rs. $perPersonEstimate", fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, color = Color(0xFF15803D))
                                Text("per buddy", fontSize = 10.sp, fontWeight = FontWeight.Medium, color = Color(0xFF166534))
                            }
                        }
                    }
                }

                // Dietary Preferences Dropdown with Dedicated Close & Done buttons
                Text("Dietary & Circle Preferences", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = textPrimary)
                Box(modifier = Modifier.fillMaxWidth()) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFF8FAFC),
                        border = BorderStroke(1.dp, if (isDietaryDropdownExpanded) primaryColor else Color(0xFFCBD5E1)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isDietaryDropdownExpanded = true }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val summaryText = if (selectedPreferences.isEmpty()) {
                                "Tap to select preferences..."
                            } else {
                                "${selectedPreferences.size} selected (${selectedPreferences.take(2).joinToString(", ")}${if (selectedPreferences.size > 2) "..." else ""})"
                            }
                            Text(
                                text = summaryText,
                                fontSize = 12.sp,
                                fontWeight = if (selectedPreferences.isNotEmpty()) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (selectedPreferences.isNotEmpty()) textPrimary else textSecondary
                            )
                            Text(
                                text = if (isDietaryDropdownExpanded) "^" else "v",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = textSecondary
                            )
                        }
                    }

                    DropdownMenu(
                        expanded = isDietaryDropdownExpanded,
                        onDismissRequest = { isDietaryDropdownExpanded = false },
                        modifier = Modifier
                            .fillMaxWidth(0.88f)
                            .heightIn(max = 320.dp)
                            .background(Color.White)
                    ) {
                        // Header with Close button (closes only dropdown, returns to host dialog)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFF8FAFC))
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Select Preferences", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = textPrimary)
                            TextButton(
                                onClick = { isDietaryDropdownExpanded = false },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text("Close", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = primaryColor)
                            }
                        }

                        Divider(color = Color(0xFFE2E8F0))

                        availablePreferences.forEach { pref ->
                            val isSelected = selectedPreferences.contains(pref)
                            DropdownMenuItem(
                                text = {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Checkbox(
                                            checked = isSelected,
                                            onCheckedChange = {
                                                if (isSelected) selectedPreferences.remove(pref) else selectedPreferences.add(pref)
                                            },
                                            colors = CheckboxDefaults.colors(checkedColor = primaryColor)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = pref,
                                            fontSize = 13.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = textPrimary
                                        )
                                    }
                                },
                                onClick = {
                                    if (isSelected) selectedPreferences.remove(pref) else selectedPreferences.add(pref)
                                }
                            )
                        }

                        Divider(color = Color(0xFFE2E8F0))

                        // Bottom Done Button
                        DropdownMenuItem(
                            text = {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Done (${selectedPreferences.size} selected)", fontWeight = FontWeight.Bold, color = primaryColor, fontSize = 13.sp)
                                }
                            },
                            onClick = { isDietaryDropdownExpanded = false }
                        )
                    }
                }

                // Selected Preferences List with Clear All button and Cross Icons
                if (selectedPreferences.isNotEmpty()) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Selected Preferences (${selectedPreferences.size})",
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = textSecondary
                            )
                            TextButton(
                                onClick = { selectedPreferences.clear() },
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text("Clear All", fontSize = 11.sp, color = primaryColor, fontWeight = FontWeight.Bold)
                            }
                        }

                        selectedPreferences.forEach { pref ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFFF1F5F9), RoundedCornerShape(8.dp))
                                    .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(pref, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = textPrimary)
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFE2E8F0))
                                        .clickable { selectedPreferences.remove(pref) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        painter = painterResource(id = android.R.drawable.ic_menu_close_clear_cancel),
                                        contentDescription = "Remove",
                                        tint = Color(0xFF64748B),
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Female-Only Circle Switch
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Female-Only Meal Circle", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = textPrimary)
                        Text("Restrict meal circle to female students only", fontSize = 10.sp, color = textSecondary)
                    }
                    Switch(
                        checked = femaleOnly,
                        onCheckedChange = { femaleOnly = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFFDB2777))
                    )
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Order or Meal Notes (Optional)") },
                    placeholder = { Text("e.g. Mention item choices, spice levels, or meeting spot details...", color = Color(0xFF94A3B8).copy(alpha = 0.6f)) },
                    maxLines = 2,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Hide My Contact in Circle", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = textPrimary)
                        Text("Mask phone for non-host members", fontSize = 10.sp, color = textSecondary)
                    }
                    Switch(
                        checked = hideHostContact,
                        onCheckedChange = { hideHostContact = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = primaryColor)
                    )
                }

                if (validationError.isNotEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFFFF1F2),
                        border = BorderStroke(1.dp, Color(0xFFFECDD3)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = validationError,
                            color = Color(0xFFE11D48),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (restaurantSpot.isBlank()) {
                        errorField = "SPOT"
                        validationError = "Please enter Food Spot or Restaurant Name."
                        triggerDeviceVibration(context)
                        coroutineScope.launch { scrollState.animateScrollTo(0) }
                        return@Button
                    }
                    if (meetingPoint.isBlank()) {
                        errorField = "MEETING"
                        validationError = "Please enter Meeting or Pickup Location."
                        triggerDeviceVibration(context)
                        coroutineScope.launch { scrollState.animateScrollTo(100) }
                        return@Button
                    }
                    if (restaurantSpot.trim().equals(meetingPoint.trim(), ignoreCase = true)) {
                        errorField = "MEETING"
                        validationError = "Food Spot (destination) and Meeting / Pickup location cannot be the same address."
                        triggerDeviceVibration(context)
                        coroutineScope.launch { scrollState.animateScrollTo(100) }
                        return@Button
                    }

                    if (selectedCategory == "Other") {
                        val (valid, errorMsg) = CategoryFilterValidator.validateCategoryName(customCategoryName)
                        if (!valid) {
                            errorField = "CATEGORY"
                            validationError = errorMsg
                            triggerDeviceVibration(context)
                            coroutineScope.launch { scrollState.animateScrollTo(0) }
                            return@Button
                        }
                    }

                    val finalCal = Calendar.getInstance().apply {
                        set(selectedYear, selectedMonth, selectedDay, selectedHour, selectedMinute, 0)
                    }
                    if (finalCal.timeInMillis < System.currentTimeMillis() + 15 * 60 * 1000) {
                        errorField = "DATETIME"
                        validationError = "Meal time must be at least 15 minutes in the future."
                        triggerDeviceVibration(context)
                        coroutineScope.launch { scrollState.animateScrollTo(200) }
                        return@Button
                    }

                    val bill = estimatedTotalBill.toIntOrNull() ?: 0
                    val finalCategory = if (selectedCategory == "Other") customCategoryName.trim() else selectedCategory

                    // Requirement 3: Cryptographically Unique and Unpredictable ID Generation
                    val secureIdPart = UUID.randomUUID().toString().replace("-", "").take(8).uppercase(Locale.US)
                    val generatedCircleId = "MEAL-$secureIdPart"

                    val newCircle = MealCircle(
                        id = generatedCircleId,
                        hostEmail = currentUserEmail,
                        hostName = currentUserName,
                        hostPhone = "+91 98765 00000",
                        hostHostel = "Campus Resident",
                        restaurantSpot = restaurantSpot.trim(),
                        mealCategory = finalCategory,
                        meetingPoint = meetingPoint.trim(),
                        targetDate = targetDate.trim(),
                        targetTime = targetTime.trim(),
                        targetTimestamp = finalCal.timeInMillis,
                        totalBuddies = totalBuddies,
                        joinedBuddies = 1,
                        members = listOf(
                            MealMember(currentUserEmail, currentUserName, "+91 98765 00000", "Hostel", hideContactInPreview = hideHostContact)
                        ),
                        estimatedTotalBill = bill,
                        perPersonEstimate = perPersonEstimate,
                        dietaryVibeTags = selectedPreferences.toList(),
                        femaleOnly = femaleOnly,
                        hideHostContactInPreview = hideHostContact,
                        notes = notes.trim(),
                        status = "GATHERING"
                    )
                    onCircleCreated(newCircle)
                },
                enabled = isFormValid,
                colors = ButtonDefaults.buttonColors(
                    containerColor = primaryColor,
                    disabledContainerColor = Color(0xFFE2E8F0),
                    disabledContentColor = Color(0xFF94A3B8)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Publish Free Meal Circle (Rs. 0 Fee)")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color(0xFF64748B))
            }
        }
    )

    if (showTimePickerModal) {
        CampusMealTimePickerDialog(
            selectedYear = selectedYear,
            selectedMonth = selectedMonth,
            selectedDay = selectedDay,
            initialHour = selectedHour,
            initialMinute = selectedMinute,
            onDismiss = { showTimePickerModal = false },
            onTimeSelected = { h, m ->
                selectedHour = h
                selectedMinute = m
                val cal = Calendar.getInstance().apply {
                    set(selectedYear, selectedMonth, selectedDay, h, m, 0)
                }
                targetTime = timeDisplayFormat.format(cal.time)
                showTimePickerModal = false
                errorField = null
                validationError = ""
            }
        )
    }
}

// Detail Dialog with Vertical list of ONLY Selected Preferences & Live State Syncing
fun generateProfessionalMealShareText(circle: MealCircle): String {
    val openSpots = (circle.totalBuddies - circle.joinedBuddies).coerceAtLeast(0)
    val sb = StringBuilder()
    sb.appendLine("\uD83C\uDF7D\uFE0F *CAMPUS FOOD & MEAL CIRCLE*")
    sb.appendLine("\u2022 *Circle ID:* `${circle.id}`")
    sb.appendLine("\u2022 *Platform Fee:* Rs. 0 (100% Free Campus Service)")
    sb.appendLine()
    sb.appendLine("\uD83D\uDCCD *SPOT & MEETING POINT*")
    sb.appendLine("\u2022 *Food Spot:* ${circle.restaurantSpot}")
    sb.appendLine("\u2022 *Meeting Point:* ${circle.meetingPoint}")
    sb.appendLine("\u2022 *Category:* ${circle.mealCategory}")
    sb.appendLine()
    sb.appendLine("\uD83D\uDDD3\uFE0F *SCHEDULE*")
    sb.appendLine("\u2022 *Date:* ${circle.targetDate}")
    sb.appendLine("\u2022 *Target Time:* ${circle.targetTime}")
    sb.appendLine()
    sb.appendLine("\uD83D\uDC65 *GROUP & BILL SPLIT*")
    sb.appendLine("\u2022 *Open Spots:* $openSpots of ${circle.totalBuddies} buddies available")
    if (circle.estimatedTotalBill > 0 && circle.perPersonEstimate > 0) {
        sb.appendLine("\u2022 *Per Buddy:* approx. Rs. ${circle.perPersonEstimate} (Total: Rs. ${circle.estimatedTotalBill})")
    } else {
        sb.appendLine("\u2022 *Bill Split:* Direct Split on Actuals (Pay on delivery)")
    }
    if (circle.femaleOnly) {
        sb.appendLine("\u2022 *Policy:* \uD83D\uDD12 Female-Only Meal Circle")
    }
    if (circle.dietaryVibeTags.isNotEmpty()) {
        sb.appendLine("\u2022 *Preferences:* ${circle.dietaryVibeTags.joinToString(", ")}")
    }
    if (circle.notes.isNotBlank()) {
        sb.appendLine()
        sb.appendLine("\uD83D\uDCDD *HOST NOTES*")
        sb.appendLine("\"${circle.notes.trim()}\"")
    }
    if (!circle.orderTrackingDetails.isNullOrBlank()) {
        sb.appendLine()
        sb.appendLine("\uD83D\uDCE6 *LIVE ORDER STATUS*")
        sb.appendLine("${circle.orderTrackingDetails.trim()}")
    }
    sb.appendLine()
    sb.appendLine("\uD83D\uDCF1 *HOW TO JOIN (100% FREE):*")
    sb.appendLine("1. Open *CampusApp*")
    sb.appendLine("2. Select *People Gathering for Food*")
    sb.appendLine("3. Search Circle ID: *${circle.id}*")
    sb.appendLine()
    sb.appendLine("\uD83D\uDCF2 *DOWNLOAD CAMPUSAPP:*")
    sb.appendLine("Get the app on Google Play Store to join this circle & save on food:")
    sb.appendLine("https://play.google.com/store/apps/details?id=com.college.campusapp")
    sb.appendLine()
    sb.appendLine("_Enjoy delicious meals, meet campus peers, and split bills easily!_")
    return sb.toString().trim()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MealHostTransferDialog(
    circle: MealCircle,
    currentUserEmail: String,
    onDismiss: () -> Unit,
    onSelectMemberForTransfer: (MealMember) -> Unit,
    onCancelWholeCircle: () -> Unit
) {
    val primaryColor = Color(0xFFE11D48)
    val textPrimary = AppTheme.InkPrimary
    val textSecondary = AppTheme.InkSecondary
    val otherMembers = circle.members.filter { it.email != currentUserEmail }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(20.dp),
        containerColor = Color.White,
        title = {
            Text("Transfer Host Ownership", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = textPrimary)
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "As the host, transferring ownership lets another buddy manage this meal circle, coordinates, and order updates.",
                    fontSize = 11.sp,
                    color = textSecondary
                )

                if (otherMembers.isEmpty()) {
                    Text("No other buddies joined yet. You can cancel the circle instead.", fontSize = 12.sp, color = Color.Red)
                } else {
                    Text("Select New Host Candidate:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = textPrimary)
                    otherMembers.forEach { member ->
                        Card(
                            onClick = { onSelectMemberForTransfer(member) },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                            border = BorderStroke(1.dp, primaryColor.copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(member.name, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = textPrimary)
                                    Text(member.hostel + " - " + member.phone, fontSize = 11.sp, color = textSecondary)
                                }
                                Text("Select", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = primaryColor)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                Divider(color = Color(0xFFF1F5F9))

                Text(
                    text = "Alternatively, you can cancel the meal circle for everyone:",
                    fontSize = 11.sp,
                    color = textSecondary
                )

                OutlinedButton(
                    onClick = onCancelWholeCircle,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Cancel Circle For Everyone")
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = Color(0xFF64748B))
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MealCircleDetailDialog(
    circle: MealCircle,
    currentUserEmail: String,
    currentUserName: String,
    onDismiss: () -> Unit,
    onJoinClick: () -> Unit,
    onRequestHostTransfer: () -> Unit,
    onAcceptHostTransfer: () -> Unit,
    onDeclineHostTransfer: () -> Unit,
    onLeaveCircle: () -> Unit,
    onCancelCircle: () -> Unit,
    onCompleteCircle: () -> Unit,
    onUpdateOrderTracking: (String) -> Unit,
    onSendMessage: (String) -> Unit
) {
    val context = LocalContext.current
    val primaryColor = Color(0xFFE11D48)
    val textPrimary = AppTheme.InkPrimary
    val textSecondary = AppTheme.InkSecondary
    val isUserInCircle = circle.hostEmail == currentUserEmail || circle.members.any { it.email == currentUserEmail }
    val isHost = circle.hostEmail == currentUserEmail
    val isFull = circle.joinedBuddies >= circle.totalBuddies || circle.members.size >= circle.totalBuddies
    val isUnderOneHour = circle.targetTimestamp - System.currentTimeMillis() in 1..3600000
    val isPendingTransferForMe = circle.pendingHostTransferEmail == currentUserEmail

    var chatText by remember { mutableStateOf("") }
    var trackingInput by remember { mutableStateOf(circle.orderTrackingDetails ?: "") }
    var showTrackingEdit by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        containerColor = Color.White,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = if (isUserInCircle) "Meal Circle Details" else "Meal Circle Preview",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = textPrimary
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("ID: " + circle.id, fontSize = 11.sp, color = textSecondary)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "📋 Copy",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = primaryColor,
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .clickable {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? android.content.ClipboardManager
                                    val clip = android.content.ClipData.newPlainText("Meal Circle ID", circle.id)
                                    clipboard?.setPrimaryClip(clip)
                                    Toast.makeText(context, "Circle ID copied to clipboard!", Toast.LENGTH_SHORT).show()
                                }
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .background(Color(0xFFFFF1F2), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = if (isUserInCircle) circle.status else "FREE PREVIEW",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = primaryColor
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 480.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                if (isUnderOneHour) {
                    LiveMealCountdownBadge(targetTimestamp = circle.targetTimestamp, modifier = Modifier.fillMaxWidth())
                }

                if (isPendingTransferForMe) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFFEF3C7),
                        border = BorderStroke(1.dp, Color(0xFFFDE68A)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("Host Transfer Request", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF92400E))
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "${circle.hostName} wants to transfer host ownership of this meal circle to you. Do you accept?",
                                fontSize = 11.sp,
                                color = Color(0xFF78350F)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = onAcceptHostTransfer,
                                    colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("Accept Host Role", fontSize = 11.sp)
                                }
                                OutlinedButton(
                                    onClick = onDeclineHostTransfer,
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("Decline", fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFFF8FAFC),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(circle.restaurantSpot, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = textPrimary, modifier = Modifier.weight(1f))
                            Text(
                                text = "🗺️ Maps",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = primaryColor,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(primaryColor.copy(alpha = 0.08f))
                                    .clickable {
                                        try {
                                            val mapUri = Uri.parse("geo:0,0?q=" + Uri.encode(circle.restaurantSpot))
                                            val mapIntent = Intent(Intent.ACTION_VIEW, mapUri)
                                            context.startActivity(mapIntent)
                                        } catch (e: Exception) {
                                            Toast.makeText(context, "Unable to launch Maps", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                    .padding(horizontal = 6.dp, vertical = 3.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(3.dp))
                        Text("Meeting Spot: " + circle.meetingPoint, fontSize = 12.sp, color = textSecondary)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("Time: " + circle.targetDate + " at " + circle.targetTime, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF0284C7))
                        Text("Buddies: " + circle.joinedBuddies + "/" + circle.totalBuddies + " Joined (" + (circle.totalBuddies - circle.joinedBuddies).coerceAtLeast(0) + " open)", fontSize = 11.sp, color = textSecondary)
                    }
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFEFF6FF),
                    border = BorderStroke(1.dp, Color(0xFFDBEAFE)),
                    modifier = Modifier.fillMaxWidth().clickable {
                        SystemNotificationHelper.showNotification(
                            context,
                            "🔔 Meal Reminder Set!",
                            "Reminder active for ${circle.restaurantSpot} gathering on ${circle.targetDate} at ${circle.targetTime}."
                        )
                        SoundEffectHelper.playSuccess(context)
                        Toast.makeText(context, "🔔 Meal Alert set for ${circle.restaurantSpot}!", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("🔔", fontSize = 14.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("Set Meal Schedule Alert", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1D4ED8))
                            Text("Tap to receive reminders for ${circle.targetTime} gathering", fontSize = 10.sp, color = Color(0xFF2563EB))
                        }
                    }
                }

                // Per Buddy Split with Generous Space & Proper Alignment
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFFF0FDF4),
                    border = BorderStroke(1.dp, Color(0xFFBBF7D0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFDCFCE7)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("₹", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF15803D))
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("Per Buddy Split", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF166534))
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = if (circle.estimatedTotalBill > 0) "Total: Rs. ${circle.estimatedTotalBill} (${circle.totalBuddies} buddies)" else "Split on actuals directly",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF15803D)
                                )
                            }
                        }
                        Column(
                            horizontalAlignment = Alignment.End,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = if (circle.perPersonEstimate > 0) "approx. Rs. ${circle.perPersonEstimate}" else "Direct Split",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 15.sp,
                                color = Color(0xFF15803D)
                            )
                            Text(
                                text = if (circle.perPersonEstimate > 0) "per buddy" else "on delivery",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF166534)
                            )
                        }
                    }
                }

                // VERTICAL display of ONLY selected options
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text("Selected Circle Attributes:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = textPrimary)

                    // Category Item
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFFFF1F2), RoundedCornerShape(8.dp))
                            .border(1.dp, Color(0xFFFECDD3), RoundedCornerShape(8.dp))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Category: ", fontSize = 11.sp, color = textSecondary)
                        Text(circle.mealCategory, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = primaryColor)
                    }

                    if (circle.femaleOnly) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFFDF2F8), RoundedCornerShape(8.dp))
                                .border(1.dp, Color(0xFFFBCFE8), RoundedCornerShape(8.dp))
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Preference: ", fontSize = 11.sp, color = textSecondary)
                            Text("Female Only Circle", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFDB2777))
                        }
                    }

                    if (circle.dietaryVibeTags.isNotEmpty()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFF8FAFC), RoundedCornerShape(8.dp))
                                .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(8.dp))
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Vibes: ", fontSize = 11.sp, color = textSecondary)
                            Text(circle.dietaryVibeTags.joinToString(", "), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = textPrimary)
                        }
                    }
                }

                if (circle.notes.isNotEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFFFAFAFA),
                        border = BorderStroke(1.dp, Color(0xFFF1F5F9)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text("Host Meal Notes:", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = textSecondary)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(text = circle.notes, fontSize = 11.sp, color = textPrimary)
                        }
                    }
                }

                if (isUserInCircle) {
                    if (!circle.orderTrackingDetails.isNullOrEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFEFF6FF),
                            border = BorderStroke(1.dp, Color(0xFFBFDBFE)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text("Live Order Delivery Tracking", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color(0xFF1E40AF))
                                Text(circle.orderTrackingDetails ?: "", fontSize = 11.sp, color = Color(0xFF1E3A8A))
                            }
                        }
                    } else if (isHost) {
                        if (showTrackingEdit) {
                            OutlinedTextField(
                                value = trackingInput,
                                onValueChange = { trackingInput = it },
                                label = { Text("Order ID / ETA / Live Tracking Link") },
                                placeholder = { Text("e.g. Swiggy Order 4123 placed. ETA: 20 mins", color = Color(0xFF94A3B8).copy(alpha = 0.6f)) },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            )
                            Button(
                                onClick = {
                                    onUpdateOrderTracking(trackingInput)
                                    showTrackingEdit = false
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Save Order Update", fontSize = 11.sp)
                            }
                        } else {
                            OutlinedButton(
                                onClick = { showTrackingEdit = true },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Post Order ID or Tracking Status", fontSize = 11.sp)
                            }
                        }
                    }

                    Text("Joined Buddies (" + circle.members.size + "/" + circle.totalBuddies + "):", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = textPrimary)
                    circle.members.forEachIndexed { idx, member ->
                        val isMe = member.email == currentUserEmail
                        val isMemberHost = member.email == circle.hostEmail
                        val isHidden = member.hideContactInPreview && !isMe

                        val memberName = when {
                            isMe -> member.name + " (You)"
                            isHidden -> if (isMemberHost) "Host (Contact Masked)" else "Student " + (idx + 1) + " (Private)"
                            else -> if (isMemberHost) member.name + " (Host)" else member.name
                        }
                        val memberSub = when {
                            isMe -> member.hostel + " - " + member.phone
                            isHidden -> "Verified Student"
                            else -> member.hostel + " - " + member.phone
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFF1F5F9), RoundedCornerShape(10.dp))
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(memberName, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = textPrimary)
                                Text(memberSub, fontSize = 10.sp, color = textSecondary)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                if (!isMe && !isHidden && member.phone.isNotBlank()) {
                                    Text(
                                        text = "📞 Call",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF16A34A),
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(Color(0xFFDCFCE7))
                                            .clickable {
                                                try {
                                                    val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + member.phone))
                                                    context.startActivity(dialIntent)
                                                } catch (e: Exception) {
                                                    Toast.makeText(context, "Could not open dialer", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                            .padding(horizontal = 6.dp, vertical = 3.dp)
                                    )
                                }
                                Text("Joined", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF16A34A))
                            }
                        }
                    }

                    OutlinedButton(
                        onClick = {
                            val shareText = generateProfessionalMealShareText(circle)
                            val sendIntent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TEXT, shareText)
                                type = "text/plain"
                            }
                            context.startActivity(Intent.createChooser(sendIntent, "Invite Buddies to Meal Circle"))
                        },
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color(0xFF22C55E)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Share via WhatsApp or SMS", fontSize = 12.sp, color = Color(0xFF15803D), fontWeight = FontWeight.Bold)
                    }

                    Divider(color = Color(0xFFF1F5F9), thickness = 1.dp)

                    Text("In-Circle Coordination Chat:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = textPrimary)
                    if (circle.chatMessages.isEmpty()) {
                        Text("No messages yet. Discuss items, toppings, and share UPI QR here.", fontSize = 11.sp, color = textSecondary)
                    } else {
                        circle.chatMessages.forEach { chat ->
                            val isMe = chat.senderEmail == currentUserEmail
                            val timeFormatted = remember(chat.timestamp) {
                                val sdf = java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault())
                                sdf.format(java.util.Date(chat.timestamp))
                            }
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp),
                                horizontalAlignment = if (isMe) Alignment.End else Alignment.Start
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    if (isMe) {
                                        Text(timeFormatted, fontSize = 8.sp, color = textSecondary)
                                        Text(chat.senderName, fontSize = 9.sp, color = textSecondary)
                                    } else {
                                        Text(chat.senderName, fontSize = 9.sp, color = textSecondary)
                                        Text(timeFormatted, fontSize = 8.sp, color = textSecondary)
                                    }
                                }
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isMe) primaryColor else Color(0xFFF1F5F9),
                                    modifier = Modifier.widthIn(max = 240.dp)
                                ) {
                                    Text(
                                        text = chat.message,
                                        fontSize = 11.sp,
                                        color = if (isMe) Color.White else textPrimary,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFFF8FAFC),
                            border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                            ) {
                                if (chatText.isEmpty()) {
                                    Text(
                                        "Item choices, spice, UPI QR...",
                                        fontSize = 11.sp,
                                        color = Color(0xFF94A3B8)
                                    )
                                }
                                BasicTextField(
                                    value = chatText,
                                    onValueChange = { chatText = it },
                                    singleLine = true,
                                    textStyle = androidx.compose.ui.text.TextStyle(
                                        fontSize = 11.sp,
                                        color = textPrimary
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Button(
                            onClick = {
                                if (chatText.isNotBlank()) {
                                    onSendMessage(chatText)
                                    chatText = ""
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Text("Send", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (!isUserInCircle) {
                if (!isFull) {
                    Button(
                        onClick = onJoinClick,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Join Meal Circle (100% Free)")
                    }
                }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (isHost) {
                        if (circle.members.size > 1) {
                            OutlinedButton(
                                onClick = onRequestHostTransfer,
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Transfer Host")
                            }
                        }
                        OutlinedButton(
                            onClick = onCancelCircle,
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Cancel Circle")
                        }
                        Button(
                            onClick = onCompleteCircle,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Finish Meal")
                        }
                    } else {
                        OutlinedButton(
                            onClick = onLeaveCircle,
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Leave Circle")
                        }
                    }
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = Color(0xFF64748B))
            }
        }
    )
}