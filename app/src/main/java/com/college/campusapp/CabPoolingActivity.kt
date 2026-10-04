package com.college.campusapp

import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.platform.LocalLifecycleOwner
import com.college.campusapp.security.EncryptionManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.*

class CabPoolingActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        CabPoolRepository.load(this)

        setContent {
            CabPoolingScreen(onBack = { finish() })
        }
    }
}

fun triggerDeviceVibration(context: Context) {
    try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vibratorManager?.defaultVibrator?.vibrate(VibrationEffect.createOneShot(120, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            val v = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                v?.vibrate(VibrationEffect.createOneShot(120, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                v?.vibrate(120)
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

fun formatCountdownTime(targetTimestamp: Long, currentTimestamp: Long): String {
    val diff = targetTimestamp - currentTimestamp
    if (diff <= 0) return "Departed"
    val hours = diff / 3600000
    val minutes = (diff % 3600000) / 60000
    val seconds = (diff % 60000) / 1000
    return if (hours > 0) {
        String.format(Locale.US, "%dh %02dm %02ds", hours, minutes, seconds)
    } else {
        String.format(Locale.US, "%02dm %02ds", minutes, seconds)
    }
}

// Dual-Arrow Circular Refresh Button (Exact matching dual-arrow sync icon with smooth rotation)
@Composable
private fun CircularRefreshButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = Color(0xFF0284C7)
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

@Composable
fun LiveCabCountdownBadge(targetTimestamp: Long, modifier: Modifier = Modifier) {
    var now by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(targetTimestamp) {
        while (now < targetTimestamp) {
            delay(1000L)
            now = System.currentTimeMillis()
        }
    }
    val diff = targetTimestamp - now
    if (diff in 1..3600000) {
        val countdownStr = formatCountdownTime(targetTimestamp, now)
        Box(
            modifier = modifier
                .background(Color(0xFFFEF2F2), RoundedCornerShape(8.dp))
                .border(1.dp, Color(0xFFFECACA), RoundedCornerShape(8.dp))
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text(
                text = "Departs in: $countdownStr",
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFFDC2626)
            )
        }
    }
}

// Professional Animated Circular Clock Selector (Strict 1-Hour Forward Barrier)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CampusFutureTimePickerDialog(
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
    val primaryColor = AppTheme.Primary

    val now = remember { Calendar.getInstance() }
    val isToday = remember(selectedYear, selectedMonth, selectedDay) {
        selectedYear == now.get(Calendar.YEAR) &&
        selectedMonth == now.get(Calendar.MONTH) &&
        selectedDay == now.get(Calendar.DAY_OF_MONTH)
    }

    val minAllowedTimestamp = remember(now) { System.currentTimeMillis() + 60 * 60 * 1000 }

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
            val index = hour12 % 12
            index * 30f
        } else {
            selectedMinute * 6f
        }
    }

    val animatedAngle by animateFloatAsState(
        targetValue = targetAngle,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "clockHandAngle"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        containerColor = Color.White,
        title = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text("Select Departure Time", fontWeight = FontWeight.Bold, fontSize = 17.sp, color = textPrimary)
                Text(
                    text = if (isToday) "Min 1 hour ahead required for today" else "Select departure time for upcoming date",
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
                // Digital Time Display & Mode Switcher
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

                // Interactive Animated Circular Analog Clock Selector Dial
                Box(
                    modifier = Modifier
                        .size(240.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFF8FAFC))
                        .border(1.dp, Color(0xFFE2E8F0), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val centerOffset = Offset(size.width / 2f, size.height / 2f)
                        val radius = (size.minDimension / 2f) - 34.dp.toPx()
                        val rad = (animatedAngle - 90f) * (PI.toFloat() / 180f)
                        val handEnd = Offset(
                            x = centerOffset.x + radius * cos(rad),
                            y = centerOffset.y + radius * sin(rad)
                        )

                        drawLine(
                            color = Color(0xFF2563EB),
                            start = centerOffset,
                            end = handEnd,
                            strokeWidth = 3.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                        drawCircle(
                            color = Color(0xFF2563EB),
                            radius = 16.dp.toPx(),
                            center = handEnd
                        )
                        drawCircle(
                            color = Color(0xFF2563EB),
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
                        val distanceDp = 86.dp
                        val offsetX = distanceDp * cos(rad)
                        val offsetY = distanceDp * sin(rad)

                        Box(
                            modifier = Modifier
                                .offset(x = offsetX, y = offsetY)
                                .size(32.dp)
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
                        Text("1. Select Hour", fontWeight = if (isSelectingHours) FontWeight.ExtraBold else FontWeight.Medium, fontSize = 12.sp)
                    }
                    TextButton(
                        onClick = { isSelectingHours = false },
                        colors = ButtonDefaults.textButtonColors(contentColor = if (!isSelectingHours) primaryColor else textSecondary)
                    ) {
                        Text("2. Select Minute", fontWeight = if (!isSelectingHours) FontWeight.ExtraBold else FontWeight.Medium, fontSize = 12.sp)
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
                        Text("Departure Time:", fontSize = 11.sp, color = textSecondary)
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
                                text = "Must be at least 1 hour in advance",
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
                    containerColor = primaryColor,
                    disabledContainerColor = Color(0xFFE2E8F0),
                    disabledContentColor = Color(0xFF94A3B8)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Confirm Departure Time")
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
fun CabPoolingScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val primaryColor = AppTheme.Primary
    val primaryDark = AppTheme.PrimaryDark
    val textPrimary = AppTheme.InkPrimary
    val textSecondary = AppTheme.InkSecondary
    val successColor = AppTheme.Success

    val currentUserEmail = remember { EncryptionManager.getString(context, "user_email") ?: "student@college.edu" }
    val currentUserName = remember {
        currentUserEmail.substringBefore("@").replace(".", " ").replaceFirstChar {
            if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString()
        }
    }

    var selectedFilterCategory by remember { mutableStateOf("ALL") }
    var searchQuery by remember { mutableStateOf("") }
    var showCreatePoolDialog by remember { mutableStateOf(false) }
    var poolPendingHostPayment by remember { mutableStateOf<CabPool?>(null) }
    var selectedPoolId by remember { mutableStateOf<String?>(null) }
    var poolForCommissionPayment by remember { mutableStateOf<CabPool?>(null) }
    var poolForPrivacySelection by remember { mutableStateOf<Pair<CabPool, String>?>(null) }
    var poolForHostTransfer by remember { mutableStateOf<CabPool?>(null) }
    var refreshCounter by remember { mutableStateOf(0) }

    var isWaitingForHostUpiReturn by remember { mutableStateOf(false) }
    var pendingHostPoolByUpi by remember { mutableStateOf<CabPool?>(null) }

    var isWaitingForRiderUpiReturn by remember { mutableStateOf(false) }
    var pendingRiderPoolByUpi by remember { mutableStateOf<CabPool?>(null) }

    val hostUpiLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val pool = pendingHostPoolByUpi
        isWaitingForHostUpiReturn = false
        if (pool != null) {
            val isSuccess = isUpiPaymentSuccessful(result.resultCode, result.data)
            if (isSuccess) {
                CabPoolRepository.createPool(context, pool, paymentMethod = "UPI")
                refreshCounter++
                pendingHostPoolByUpi = null
                SoundEffectHelper.playPaymentSuccess(context)
                Toast.makeText(context, "Payment Verified: Platform Fee ₹20 received. Cab pool published!", Toast.LENGTH_SHORT).show()
            } else {
                pendingHostPoolByUpi = null
                Toast.makeText(context, "Payment Not Completed: Cab pool was not created. Please complete the ₹20 platform fee to host.", Toast.LENGTH_LONG).show()
            }
        }
    }

    val riderUpiLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val pool = pendingRiderPoolByUpi
        isWaitingForRiderUpiReturn = false
        if (pool != null) {
            val isSuccess = isUpiPaymentSuccessful(result.resultCode, result.data)
            if (isSuccess) {
                pendingRiderPoolByUpi = null
                poolForPrivacySelection = Pair(pool, "UPI")
                SoundEffectHelper.playPaymentSuccess(context)
                Toast.makeText(context, "Payment Verified: Convenience fee ₹20 received.", Toast.LENGTH_SHORT).show()
            } else {
                pendingRiderPoolByUpi = null
                Toast.makeText(context, "Payment Not Completed: You did not join the pool. Please complete the ₹20 convenience fee.", Toast.LENGTH_LONG).show()
            }
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                refreshCounter++
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val walletBalance = remember(CabPoolRepository.activePools.size, refreshCounter) {
        WalletManager.getBalance(context)
    }

    val dynamicCategories by remember(CabPoolRepository.activePools.size, refreshCounter) {
        derivedStateOf {
            val standard = setOf("Airport", "Station", "Mall")
            CabPoolRepository.activePools
                .map { it.routeCategory }
                .filter { it !in standard && it.isNotBlank() }
                .distinct()
        }
    }

    // High performance derivedStateOf - only recomputes when filter, search or count changes
    val filteredPools by remember(selectedFilterCategory, searchQuery, refreshCounter, CabPoolRepository.activePools.size) {
        derivedStateOf {
            val now = System.currentTimeMillis()
            val query = searchQuery.trim()
            CabPoolRepository.activePools.filter { pool ->
                val notExpired = pool.departureTimestamp > now
                val isActive = pool.status != "COMPLETED" && pool.status != "CANCELLED" && notExpired

                // Instant Direct ID search (e.g. CAB-XXXX or suffix): bypasses category restrictions
                val isDirectIdMatch = query.isNotEmpty() && (
                    pool.id.contains(query, ignoreCase = true) ||
                    pool.id.replace("CAB-", "").contains(query, ignoreCase = true)
                )
                if (isDirectIdMatch) {
                    return@filter true
                }

                val matchesCategory = when (selectedFilterCategory) {
                    "ALL" -> isActive
                    "Airport" -> isActive && pool.routeCategory.equals("Airport", ignoreCase = true)
                    "Station" -> isActive && pool.routeCategory.equals("Station", ignoreCase = true)
                    "Mall" -> isActive && pool.routeCategory.equals("Mall", ignoreCase = true)
                    "FEMALE_ONLY" -> isActive && pool.femaleOnly
                    "MY_RIDES" -> pool.hostEmail == currentUserEmail || pool.riders.any { it.email == currentUserEmail }
                    else -> isActive && pool.routeCategory.equals(selectedFilterCategory, ignoreCase = true)
                }

                val matchesSearch = query.isEmpty() ||
                        pool.id.contains(query, ignoreCase = true) ||
                        pool.dropDestination.contains(query, ignoreCase = true) ||
                        pool.pickupPoint.contains(query, ignoreCase = true) ||
                        pool.routeCategory.contains(query, ignoreCase = true) ||
                        pool.notes.contains(query, ignoreCase = true)

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
                            Text("Cab Pooling", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = textPrimary, maxLines = 1)
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .background(Color(0xFFE0F2FE), RoundedCornerShape(6.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text("LIVE", color = primaryDark, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold)
                            }
                        }
                        Text("Split fares & travel safely", fontSize = 11.sp, color = textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
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
                                SoundEffectHelper.playClick(context)
                                CabPoolRepository.refreshAndClean(context)
                                refreshCounter++
                                Toast.makeText(context, "Cab pools refreshed.", Toast.LENGTH_SHORT).show()
                            },
                            tint = primaryDark
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
                    SoundEffectHelper.playClick(context)
                    showCreatePoolDialog = true
                },
                containerColor = primaryDark,
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp),
                icon = { Text("+", fontSize = 20.sp, fontWeight = FontWeight.Bold) },
                text = { Text("Host a Cab Pool", fontWeight = FontWeight.Bold, fontSize = 14.sp) }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(AppTheme.Background)
        ) {
            val lifetimeSavings = remember(CabPoolRepository.activePools.size, refreshCounter) {
                CabPoolRepository.calculateLifetimeSavings(currentUserEmail)
            }

            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color(0xFFF0FDF4),
                border = BorderStroke(1.dp, Color(0xFFBBF7D0)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("💰", fontSize = 18.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "₹$lifetimeSavings Saved Pooling",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 13.sp,
                                color = Color(0xFF166534)
                            )
                            Text(
                                text = "vs Solo Cab Booking on Campus",
                                fontSize = 10.sp,
                                color = Color(0xFF15803D)
                            )
                        }
                    }
                    Box(
                        modifier = Modifier
                            .background(Color(0xFFDCFCE7), RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text("Top 10% Saver 🏆", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF166534))
                    }
                }
            }

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                shape = RoundedCornerShape(14.dp),
                color = Color.White,
                shadowElevation = 1.dp
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text("Search:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = textSecondary)
                    Spacer(modifier = Modifier.width(8.dp))
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Airport, Station, Mall, Fest...", fontSize = 13.sp, color = Color(0xFF94A3B8).copy(alpha = 0.7f)) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
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
                    CabFilterChip("All Pools", selected = selectedFilterCategory == "ALL") {
                        SoundEffectHelper.playClick(context)
                        selectedFilterCategory = "ALL"
                    }
                }
                item {
                    CabFilterChip("Airport", selected = selectedFilterCategory == "Airport") {
                        SoundEffectHelper.playClick(context)
                        selectedFilterCategory = "Airport"
                    }
                }
                item {
                    CabFilterChip("Station", selected = selectedFilterCategory == "Station") {
                        SoundEffectHelper.playClick(context)
                        selectedFilterCategory = "Station"
                    }
                }
                item {
                    CabFilterChip("Malls", selected = selectedFilterCategory == "Mall") {
                        SoundEffectHelper.playClick(context)
                        selectedFilterCategory = "Mall"
                    }
                }
                items(dynamicCategories) { cat ->
                    CabFilterChip(cat, selected = selectedFilterCategory == cat) {
                        SoundEffectHelper.playClick(context)
                        selectedFilterCategory = cat
                    }
                }
                item {
                    CabFilterChip("Female Only", selected = selectedFilterCategory == "FEMALE_ONLY") {
                        SoundEffectHelper.playClick(context)
                        selectedFilterCategory = "FEMALE_ONLY"
                    }
                }
                item {
                    CabFilterChip("My Rides", selected = selectedFilterCategory == "MY_RIDES") {
                        SoundEffectHelper.playClick(context)
                        selectedFilterCategory = "MY_RIDES"
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            if (filteredPools.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = if (selectedFilterCategory == "MY_RIDES") "No active cab pools in your rides." else "No active cab pools matching your search.",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = textPrimary,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Refresh to discover newly listed rides or create a new pool.",
                            fontSize = 12.sp,
                            color = textSecondary,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        OutlinedButton(
                            onClick = {
                                SystemNotificationHelper.showNotification(
                                    context,
                                    "🔔 Route Radar Active!",
                                    "We'll alert you the moment a new pool matching your destination is opened."
                                )
                                SoundEffectHelper.playSuccess(context)
                                Toast.makeText(context, "🔔 Route Radar Active: You'll be notified when a ride opens!", Toast.LENGTH_LONG).show()
                            },
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, primaryColor),
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp)
                        ) {
                            Text("🔔 Alert Me When a Ride Opens", fontSize = 12.sp, color = primaryColor, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = {
                                    CabPoolRepository.refreshAndClean(context)
                                    refreshCounter++
                                    Toast.makeText(context, "Refreshed.", Toast.LENGTH_SHORT).show()
                                },
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Refresh Feed")
                            }
                            Button(
                                onClick = { showCreatePoolDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Host a Cab Pool")
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 88.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredPools, key = { it.id }) { pool ->
                        CabPoolCard(
                            pool = pool,
                            currentUserEmail = currentUserEmail,
                            onJoinClick = {
                                if (pool.bookedSeats >= pool.totalSeats || pool.riders.size >= pool.totalSeats) {
                                    Toast.makeText(context, "This cab pool is completely full.", Toast.LENGTH_SHORT).show()
                                } else {
                                    poolForCommissionPayment = pool
                                }
                            },
                            onViewDetails = {
                                selectedPoolId = pool.id
                            }
                        )
                    }
                }
            }
        }
    }

    // Step 1: Rider Commission Payment Dialog
    poolForCommissionPayment?.let { pool ->
        CabPoolCommissionDialog(
            pool = pool,
            walletBalance = walletBalance,
            onDismiss = { poolForCommissionPayment = null },
            onPayWithWallet = {
                if (pool.bookedSeats >= pool.totalSeats || pool.riders.size >= pool.totalSeats) {
                    Toast.makeText(context, "This pool is already full. Payment was not deducted.", Toast.LENGTH_LONG).show()
                    poolForCommissionPayment = null
                } else if (walletBalance < CabPoolRepository.PLATFORM_COMMISSION) {
                    Toast.makeText(context, "Payment Failed: Insufficient wallet balance (Need ₹20). Please add money or use UPI.", Toast.LENGTH_LONG).show()
                } else {
                    val debited = WalletManager.debit(
                        context,
                        CabPoolRepository.PLATFORM_COMMISSION,
                        "Cab Pool Platform Convenience Fee (ID: " + pool.id + ") - ₹" + CabPoolRepository.PLATFORM_COMMISSION
                    )
                    if (debited) {
                        refreshCounter++
                        poolForCommissionPayment = null
                        poolForPrivacySelection = Pair(pool, "WALLET_PREPAID")
                    } else {
                        Toast.makeText(context, "Payment Failed: Wallet deduction failed. Please try again.", Toast.LENGTH_SHORT).show()
                    }
                }
            },
            onPayWithUpi = {
                if (pool.bookedSeats >= pool.totalSeats || pool.riders.size >= pool.totalSeats) {
                    Toast.makeText(context, "This pool is already full. Cannot proceed.", Toast.LENGTH_LONG).show()
                    poolForCommissionPayment = null
                } else {
                    val trId = "CAB" + System.currentTimeMillis()
                    val upiUriStr = "upi://pay?pa=campuspool@ybl&pn=CampusApp&tr=" + trId + "&am=20&cu=INR&tn=Cab%20Pool%20Commission%20" + pool.id
                    val upiIntent = Intent(Intent.ACTION_VIEW, Uri.parse(upiUriStr))
                    pendingRiderPoolByUpi = pool
                    isWaitingForRiderUpiReturn = true
                    poolForCommissionPayment = null
                    try {
                        riderUpiLauncher.launch(Intent.createChooser(upiIntent, "Pay ₹20 Convenience Fee via UPI"))
                    } catch (e: Exception) {
                        Toast.makeText(context, "No UPI apps found. Please install Google Pay, PhonePe, or Paytm, or pay via Wallet.", Toast.LENGTH_LONG).show()
                        pendingRiderPoolByUpi = null
                        isWaitingForRiderUpiReturn = false
                    }
                }
            }
        )
    }

    // Step 2: Rider Privacy Selection Dialog
    poolForPrivacySelection?.let { (pool, payMethod) ->
        CabPoolPrivacyChoiceDialog(
            onDismiss = { poolForPrivacySelection = null },
            onSelectPrivacy = { hideContact ->
                if (pool.bookedSeats >= pool.totalSeats || pool.riders.size >= pool.totalSeats) {
                    Toast.makeText(context, "Cannot join: Pool is full.", Toast.LENGTH_LONG).show()
                    poolForPrivacySelection = null
                } else {
                    val rider = CabRider(
                        email = currentUserEmail,
                        name = currentUserName,
                        phone = "+91 98765 00000",
                        hostel = "Campus Hostel",
                        gender = "Student",
                        paid = true,
                        hideContactInPreview = hideContact
                    )
                    val success = CabPoolRepository.joinPool(context, pool.id, rider, paymentMethod = if (payMethod == "WALLET_PREPAID") "PREPAID" else payMethod)
                    poolForPrivacySelection = null
                    if (success) {
                        refreshCounter++
                        SoundEffectHelper.playPaymentSuccess(context)
                        Toast.makeText(context, "Joined pool successfully. Trip and member details unlocked.", Toast.LENGTH_SHORT).show()
                        selectedPoolId = pool.id
                    } else {
                        Toast.makeText(context, "Unable to join pool.", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        )
    }

    // Host Creation Dialog
    if (showCreatePoolDialog) {
        CreateCabPoolDialog(
            currentUserEmail = currentUserEmail,
            currentUserName = currentUserName,
            onDismiss = { showCreatePoolDialog = false },
            onProceedToPayFee = { pool ->
                showCreatePoolDialog = false
                poolPendingHostPayment = pool
            }
        )
    }

    // Host Platform Fee Payment Dialog
    poolPendingHostPayment?.let { pool ->
        HostPlatformFeeDialog(
            pool = pool,
            walletBalance = walletBalance,
            onDismiss = { poolPendingHostPayment = null },
            onPayWithWallet = {
                if (walletBalance < CabPoolRepository.PLATFORM_COMMISSION) {
                    Toast.makeText(context, "Payment Failed: Insufficient wallet balance (Need ₹20). Please add money or use UPI.", Toast.LENGTH_LONG).show()
                } else {
                    val debited = WalletManager.debit(
                        context,
                        CabPoolRepository.PLATFORM_COMMISSION,
                        "Cab Pool Platform Convenience Fee (ID: " + pool.id + ") - ₹" + CabPoolRepository.PLATFORM_COMMISSION
                    )
                    if (debited) {
                        CabPoolRepository.createPool(context, pool, paymentMethod = "PREPAID")
                        refreshCounter++
                        poolPendingHostPayment = null
                        SoundEffectHelper.playPaymentSuccess(context)
                        Toast.makeText(context, "Payment Verified. Cab pool published!", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Payment Failed: Wallet deduction failed. Please try again.", Toast.LENGTH_SHORT).show()
                    }
                }
            },
            onPayWithUpi = {
                val trId = "CAB" + System.currentTimeMillis()
                val upiUriStr = "upi://pay?pa=campuspool@ybl&pn=CampusApp&tr=" + trId + "&am=20&cu=INR&tn=Host%20Cab%20Pool%20Fee%20" + pool.id
                val upiIntent = Intent(Intent.ACTION_VIEW, Uri.parse(upiUriStr))
                pendingHostPoolByUpi = pool
                isWaitingForHostUpiReturn = true
                poolPendingHostPayment = null
                try {
                    hostUpiLauncher.launch(Intent.createChooser(upiIntent, "Pay ₹20 Platform Convenience Fee via UPI"))
                } catch (e: Exception) {
                    Toast.makeText(context, "No UPI apps found. Please install Google Pay, PhonePe, or Paytm, or pay via Wallet.", Toast.LENGTH_LONG).show()
                    pendingHostPoolByUpi = null
                    isWaitingForHostUpiReturn = false
                }
            }
        )
    }

    // Host Transfer Selection Dialog
    poolForHostTransfer?.let { pool ->
        HostTransferDialog(
            pool = pool,
            currentUserEmail = currentUserEmail,
            onDismiss = { poolForHostTransfer = null },
            onSelectRiderForTransfer = { targetRider ->
                CabPoolRepository.requestHostTransfer(context, pool.id, targetRider.email, targetRider.name)
                refreshCounter++
                poolForHostTransfer = null
                selectedPoolId = null
                Toast.makeText(context, "Host transfer request sent to " + targetRider.name + ". Ownership will transfer once accepted.", Toast.LENGTH_LONG).show()
            },
            onCancelWholePool = {
                val (success, msg) = CabPoolRepository.cancelPool(context, pool.id)
                refreshCounter++
                poolForHostTransfer = null
                selectedPoolId = null
                Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
            }
        )
    }

    // Pool Details Dialog (Dynamically resolves latest pool state for real-time live chat & members)
    selectedPoolId?.let { poolId ->
        val pool = remember(poolId, refreshCounter, CabPoolRepository.activePools.toList()) {
            CabPoolRepository.activePools.find { it.id == poolId }
        } ?: CabPoolRepository.activePools.find { it.id == poolId }

        if (pool != null) {
            CabPoolDetailDialog(
                pool = pool,
                currentUserEmail = currentUserEmail,
                currentUserName = currentUserName,
                onDismiss = { selectedPoolId = null },
                onJoinClick = {
                    if (pool.bookedSeats >= pool.totalSeats || pool.riders.size >= pool.totalSeats) {
                        Toast.makeText(context, "This cab pool is completely full (${pool.totalSeats}/${pool.totalSeats} seats).", Toast.LENGTH_SHORT).show()
                    } else {
                        selectedPoolId = null
                        poolForCommissionPayment = pool
                    }
                },
                onRequestHostTransfer = {
                    poolForHostTransfer = pool
                },
                onAcceptHostTransfer = {
                    val success = CabPoolRepository.acceptHostTransfer(context, pool.id, currentUserEmail)
                    refreshCounter++
                    if (success) {
                        SoundEffectHelper.playActionComplete(context)
                        Toast.makeText(context, "You are now the official host of this pool.", Toast.LENGTH_SHORT).show()
                    }
                },
                onDeclineHostTransfer = {
                    SoundEffectHelper.playClick(context)
                    CabPoolRepository.declineHostTransfer(context, pool.id)
                    refreshCounter++
                    Toast.makeText(context, "Host transfer request declined.", Toast.LENGTH_SHORT).show()
                },
                onLeavePool = {
                    SoundEffectHelper.playClick(context)
                    val (success, msg) = CabPoolRepository.leavePool(context, pool.id, currentUserEmail)
                    refreshCounter++
                    if (success) {
                        selectedPoolId = null
                    }
                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                },
                onCancelPool = {
                    SoundEffectHelper.playClick(context)
                    val (success, msg) = CabPoolRepository.cancelPool(context, pool.id)
                    refreshCounter++
                    if (success) {
                        selectedPoolId = null
                    }
                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                },
                onCompletePool = {
                    SoundEffectHelper.playSuccess(context)
                    CabPoolRepository.completePool(context, pool.id)
                    refreshCounter++
                    selectedPoolId = null
                    Toast.makeText(context, "Journey completed.", Toast.LENGTH_SHORT).show()
                },
                onAddCabDetails = { details ->
                    SoundEffectHelper.playActionComplete(context)
                    CabPoolRepository.updateCabDetails(context, pool.id, details)
                    refreshCounter++
                    Toast.makeText(context, "Cab details updated and broadcasted to pool members.", Toast.LENGTH_SHORT).show()
                },
                onSendMessage = { msg ->
                    SoundEffectHelper.playClick(context)
                    val chat = CabPoolChatMessage(
                        senderEmail = currentUserEmail,
                        senderName = currentUserName,
                        message = msg
                    )
                    CabPoolRepository.addChatMessage(context, pool.id, chat)
                    refreshCounter++
                }
            )
        } else {
            selectedPoolId = null
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CabFilterChip(label: String, selected: Boolean, onClick: () -> Unit) {
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
            selectedContainerColor = AppTheme.Primary,
            selectedLabelColor = Color.White,
            containerColor = Color.White,
            labelColor = AppTheme.InkPrimary
        ),
        shape = RoundedCornerShape(16.dp),
        border = FilterChipDefaults.filterChipBorder(
            borderColor = Color(0xFFE2E8F0),
            selectedBorderColor = AppTheme.Primary,
            borderWidth = 1.dp,
            selectedBorderWidth = 1.dp
        )
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CabPoolCard(
    pool: CabPool,
    currentUserEmail: String,
    onJoinClick: () -> Unit,
    onViewDetails: () -> Unit
) {
    val textPrimary = AppTheme.InkPrimary
    val textSecondary = AppTheme.InkSecondary
    val primaryColor = AppTheme.Primary
    val successColor = AppTheme.Success

    val isUserInPool = pool.hostEmail == currentUserEmail || pool.riders.any { it.email == currentUserEmail }
    val isHost = pool.hostEmail == currentUserEmail
    val isFull = pool.bookedSeats >= pool.totalSeats || pool.riders.size >= pool.totalSeats
    val isUnderOneHour = pool.departureTimestamp - System.currentTimeMillis() in 1..3600000

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(2.dp, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, if (isUserInPool) primaryColor.copy(alpha = 0.5f) else Color(0xFFF1F5F9))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
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
                            .background(Color(0xFFEFF6FF), RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = pool.routeCategory,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = primaryColor
                        )
                    }
                    if (pool.femaleOnly) {
                        Box(
                            modifier = Modifier
                                .background(Color(0xFFFDF2F8), RoundedCornerShape(8.dp))
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
                    LiveCabCountdownBadge(targetTimestamp = pool.departureTimestamp)
                } else {
                    val (statusLabel, statusBg, statusColor) = when {
                        pool.status == "COMPLETED" -> Triple("Completed", Color(0xFFECFDF5), successColor)
                        pool.status == "CAB_BOOKED" -> Triple("Cab Booked", Color(0xFFEFF6FF), primaryColor)
                        isFull -> Triple("Pool Full", Color(0xFFFEF3C7), Color(0xFFD97706))
                        else -> Triple("" + pool.bookedSeats + "/" + pool.totalSeats + " Filled", Color(0xFFF1F5F9), Color(0xFF475569))
                    }

                    Box(
                        modifier = Modifier
                            .background(statusBg, RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(text = statusLabel, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = statusColor)
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Trip Route
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "From: " + pool.pickupPoint,
                        fontSize = 12.sp,
                        color = textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "To: " + pool.dropDestination,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 15.sp,
                        color = textPrimary,
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
                        Text(pool.departureDate, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFFB45309))
                        Text(pool.departureTime, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF92400E))
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TagPill(pool.cabType)
                if (pool.isSuperhost) {
                    TagPill("⭐ Superhost", bg = Color(0xFFFEF3C7), textColor = Color(0xFFB45309))
                }
                if (!pool.flightOrTrainNumber.isNullOrBlank()) {
                    TagPill(pool.flightOrTrainNumber, bg = Color(0xFFE0F2FE), textColor = Color(0xFF0369A1))
                }
                if (pool.pendingHostTransferEmail != null) {
                    TagPill("Host Transfer Pending", bg = Color(0xFFFEF3C7), textColor = Color(0xFFB45309))
                }
            }

            if (pool.notes.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "\"" + pool.notes + "\"",
                    fontSize = 11.sp,
                    color = Color(0xFF64748B),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(14.dp))
            Divider(color = Color(0xFFF1F5F9), thickness = 1.dp)
            Spacer(modifier = Modifier.height(12.dp))

            // Bottom Fare & Action
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val remaining = (pool.totalSeats - pool.bookedSeats).coerceAtLeast(0)
                    Text(
                        text = if (isFull) "No seats open" else "" + remaining + " seats open",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isFull) Color(0xFFE11D48) else textSecondary
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "Rs. " + pool.perPersonFare,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 16.sp,
                            color = successColor
                        )
                        Text(
                            text = "actual fare / seat",
                            fontSize = 9.sp,
                            color = textSecondary
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    if (isUserInPool) {
                        Button(
                            onClick = onViewDetails,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(if (isHost) "Manage" else "My Pass", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    } else if (isFull) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFF1F5F9),
                            modifier = Modifier.padding(4.dp)
                        ) {
                            Text(
                                "Pool Full",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF94A3B8),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    } else {
                        Button(
                            onClick = onViewDetails,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Text("Trip Preview", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TagPill(text: String, bg: Color = Color(0xFFF8FAFC), textColor: Color = Color(0xFF475569)) {
    Box(
        modifier = Modifier
            .background(bg, RoundedCornerShape(6.dp))
            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(6.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(text = text, fontSize = 10.sp, fontWeight = FontWeight.Medium, color = textColor)
    }
}

// Step 1: Rider Commission Payment Dialog ("Platform Convenience Fee" - Identical to Host)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CabPoolCommissionDialog(
    pool: CabPool,
    walletBalance: Int,
    onDismiss: () -> Unit,
    onPayWithWallet: () -> Unit,
    onPayWithUpi: () -> Unit
) {
    val textPrimary = AppTheme.InkPrimary
    val textSecondary = AppTheme.InkSecondary
    val primaryColor = AppTheme.Primary
    val successColor = AppTheme.Success

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        containerColor = Color.White,
        title = {
            Text("Join Cab Pool", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = textPrimary)
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFF8FAFC),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Destination: " + pool.dropDestination, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = textPrimary)
                        Text("Departure: " + pool.departureDate + " at " + pool.departureTime, fontSize = 11.sp, color = textSecondary)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Actual Ride Fare: Rs. " + pool.perPersonFare + " / person", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = textPrimary)
                    }
                }

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFFF0F9FF),
                    border = BorderStroke(1.dp, Color(0xFFBAE6FD)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Platform Convenience Fee", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0369A1))
                            Text("Rs. 20", fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, color = Color(0xFF0369A1))
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Platform Convenience Fee: To join this cab pool on the campus network, a standard fee of Rs. 20 is applicable for real-time ride coordination, identity verification, and 24/7 campus safety tools.",
                            fontSize = 11.sp,
                            color = Color(0xFF0C4A6E),
                            lineHeight = 15.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Divider(color = Color(0xFFBAE6FD).copy(alpha = 0.6f))
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "- Policy Notice: Platform fee is strictly non-refundable once activated.",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF991B1B)
                        )
                        Text(
                            text = "- Departure Lock: In-app seat releases close exactly 1 hour prior to scheduled departure. Riders can still join open seats within the hour.",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF991B1B)
                        )
                    }
                }

                Text("Select Secure Payment Method:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = textPrimary)

                // Option 1: Wallet
                Card(
                    onClick = onPayWithWallet,
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                    border = BorderStroke(1.dp, if (walletBalance >= 20) primaryColor.copy(alpha = 0.5f) else Color(0xFFE2E8F0)),
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
                            Text("Pay with App Wallet", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = textPrimary)
                            Text("Available Balance: Rs. " + walletBalance, fontSize = 11.sp, color = if (walletBalance >= 20) successColor else Color.Red)
                        }
                        Text("Pay Rs. 20", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = primaryColor)
                    }
                }

                // Option 2: UPI
                Card(
                    onClick = onPayWithUpi,
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                    border = BorderStroke(1.dp, Color(0xFF22C55E).copy(alpha = 0.5f)),
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
                            Text("Pay with UPI App", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = textPrimary)
                            Text("Google Pay, PhonePe, Paytm, BHIM", fontSize = 10.sp, color = textSecondary)
                        }
                        Text("Pay Rs. 20", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF15803D))
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color(0xFF64748B))
            }
        }
    )
}

// Host Platform Fee Dialog ("Platform Convenience Fee")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HostPlatformFeeDialog(
    pool: CabPool,
    walletBalance: Int,
    onDismiss: () -> Unit,
    onPayWithWallet: () -> Unit,
    onPayWithUpi: () -> Unit
) {
    val textPrimary = AppTheme.InkPrimary
    val textSecondary = AppTheme.InkSecondary
    val primaryColor = AppTheme.Primary
    val successColor = AppTheme.Success

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        containerColor = Color.White,
        title = {
            Text("Host Platform Confirmation", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = textPrimary)
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFF8FAFC),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Destination: " + pool.dropDestination, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = textPrimary)
                        Text("Capacity: " + pool.cabType + " (" + pool.totalSeats + " Seats)", fontSize = 11.sp, color = textSecondary)
                        Text("Total Estimated Fare: Rs. " + pool.estimatedTotalFare, fontSize = 11.sp, color = textSecondary)
                    }
                }

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFFF0F9FF),
                    border = BorderStroke(1.dp, Color(0xFFBAE6FD)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Platform Convenience Fee", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0369A1))
                            Text("Rs. 20", fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, color = Color(0xFF0369A1))
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Platform Convenience Fee: To publish your cab pool onto the campus network, a standard fee of Rs. 20 is applicable for real-time ride coordination, identity verification, and 24/7 campus safety tools.",
                            fontSize = 11.sp,
                            color = Color(0xFF0C4A6E),
                            lineHeight = 15.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Divider(color = Color(0xFFBAE6FD).copy(alpha = 0.6f))
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "- Policy Notice: Platform fee is strictly non-refundable once activated.",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF991B1B)
                        )
                        Text(
                            text = "- Departure Lock: In-app seat releases close exactly 1 hour prior to scheduled departure. Riders can still join open seats within the hour.",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF991B1B)
                        )
                    }
                }

                Text("Select Secure Payment Method:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = textPrimary)

                // Option 1: Wallet
                Card(
                    onClick = onPayWithWallet,
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                    border = BorderStroke(1.dp, if (walletBalance >= 20) primaryColor.copy(alpha = 0.5f) else Color(0xFFE2E8F0)),
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
                            Text("Pay with App Wallet", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = textPrimary)
                            Text("Available Balance: Rs. " + walletBalance, fontSize = 11.sp, color = if (walletBalance >= 20) successColor else Color.Red)
                        }
                        Text("Pay Rs. 20", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = primaryColor)
                    }
                }

                // Option 2: UPI
                Card(
                    onClick = onPayWithUpi,
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                    border = BorderStroke(1.dp, Color(0xFF22C55E).copy(alpha = 0.5f)),
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
                            Text("Pay with UPI App", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = textPrimary)
                            Text("Google Pay, PhonePe, Paytm, BHIM", fontSize = 10.sp, color = textSecondary)
                        }
                        Text("Pay Rs. 20", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF15803D))
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color(0xFF64748B))
            }
        }
    )
}

// Step 2: Privacy Preference Prompt
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CabPoolPrivacyChoiceDialog(
    onDismiss: () -> Unit,
    onSelectPrivacy: (Boolean) -> Unit
) {
    val textPrimary = AppTheme.InkPrimary
    val textSecondary = AppTheme.InkSecondary
    val primaryColor = AppTheme.Primary

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        containerColor = Color.White,
        title = {
            Text("Contact Privacy", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = textPrimary)
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Payment verified. Please choose how your contact credentials appear to other students in the pool:",
                    fontSize = 12.sp,
                    color = textSecondary
                )

                Card(
                    onClick = { onSelectPrivacy(true) },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                    border = BorderStroke(1.dp, primaryColor.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Hide My Contact (Recommended)", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = primaryColor)
                        Text("Name and phone number masked for other members.", fontSize = 10.sp, color = textSecondary)
                    }
                }

                Card(
                    onClick = { onSelectPrivacy(false) },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Show My Contact", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = textPrimary)
                        Text("Allow other pool members to view your verified student name and phone.", fontSize = 10.sp, color = textSecondary)
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {}
    )
}

// Host Transfer Nomination Dialog
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HostTransferDialog(
    pool: CabPool,
    currentUserEmail: String,
    onDismiss: () -> Unit,
    onSelectRiderForTransfer: (CabRider) -> Unit,
    onCancelWholePool: () -> Unit
) {
    val textPrimary = AppTheme.InkPrimary
    val textSecondary = AppTheme.InkSecondary
    val primaryColor = AppTheme.Primary

    val otherRiders = remember(pool) {
        pool.riders.filter { it.email != currentUserEmail }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        containerColor = Color.White,
        title = {
            Text("Transfer Host Ownership", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = textPrimary)
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "You are currently the host of this pool. To leave without cancelling the trip for everyone, select a co-rider to transfer host ownership to:",
                    fontSize = 12.sp,
                    color = textSecondary
                )

                if (otherRiders.isEmpty()) {
                    Text(
                        text = "No other riders have joined yet. You can cancel the pool directly.",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFB45309)
                    )
                } else {
                    Text("Select New Host Candidate:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = textPrimary)
                    otherRiders.forEach { rider ->
                        Card(
                            onClick = { onSelectRiderForTransfer(rider) },
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
                                    Text(rider.name, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = textPrimary)
                                    Text(rider.hostel + " - " + rider.phone, fontSize = 11.sp, color = textSecondary)
                                }
                                Text("Select", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = primaryColor)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                Divider(color = Color(0xFFF1F5F9))

                Text(
                    text = "Alternatively, you can cancel the whole pool for everyone:",
                    fontSize = 11.sp,
                    color = textSecondary
                )

                OutlinedButton(
                    onClick = onCancelWholePool,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Cancel Whole Pool For Everyone (No Refund)")
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

// Host a Cab Pool Dialog
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateCabPoolDialog(
    currentUserEmail: String,
    currentUserName: String,
    onDismiss: () -> Unit,
    onProceedToPayFee: (CabPool) -> Unit
) {
    val context = LocalContext.current
    val primaryColor = AppTheme.Primary
    val textPrimary = AppTheme.InkPrimary
    val textSecondary = AppTheme.InkSecondary
    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()

    val initialCal = remember {
        Calendar.getInstance().apply {
            add(Calendar.HOUR_OF_DAY, 2)
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

    var departureDate by remember { mutableStateOf(dateDisplayFormat.format(initialCal.time)) }
    var departureTime by remember { mutableStateOf(timeDisplayFormat.format(initialCal.time)) }

    var showTimePickerModal by remember { mutableStateOf(false) }

    var pickupPoint by remember { mutableStateOf("") }
    var dropDestination by remember { mutableStateOf("") }
    var selectedCategoryType by remember { mutableStateOf("Airport") }
    var customCategoryName by remember { mutableStateOf("") }
    var selectedCarCapacity by remember { mutableStateOf(4) }
    var estimatedFare by remember { mutableStateOf("") }
    var femaleOnly by remember { mutableStateOf(false) }
    var hideHostContact by remember { mutableStateOf(true) }
    var notes by remember { mutableStateOf("") }

    var errorField by remember { mutableStateOf<String?>(null) }
    var validationError by remember { mutableStateOf("") }

    val fareInt = estimatedFare.toIntOrNull() ?: 0
    val totalSeats = selectedCarCapacity
    val perPersonEstimate = if (totalSeats > 0 && fareInt > 0) ceil(fareInt.toDouble() / totalSeats.toDouble()).toInt() else 0

    val destinationPlaceholder = remember(selectedCategoryType) {
        when (selectedCategoryType) {
            "Airport" -> "e.g. Kempegowda International Airport (Terminal 1 and 2)"
            "Station" -> "e.g. City Central Railway Station (Platform 1)"
            "Mall" -> "e.g. Phoenix Marketcity Mall (Main Entry)"
            else -> "e.g. Tech Park, Hospital, Event Arena..."
        }
    }

    val farePlaceholder = if (selectedCarCapacity == 4) "e.g. 800" else "e.g. 1400"

    val currentDepartureCal = Calendar.getInstance().apply {
        set(selectedYear, selectedMonth, selectedDay, selectedHour, selectedMinute, 0)
    }
    val isDepartureInFuture = currentDepartureCal.timeInMillis >= System.currentTimeMillis() + 60 * 60 * 1000

    val isCategoryValid = if (selectedCategoryType == "Other") {
        CategoryFilterValidator.validateCategoryName(customCategoryName).first
    } else {
        true
    }

    val isFormCompleteAndValid = pickupPoint.isNotBlank() &&
            dropDestination.isNotBlank() &&
            !pickupPoint.trim().equals(dropDestination.trim(), ignoreCase = true) &&
            fareInt > 0 &&
            isCategoryValid &&
            isDepartureInFuture

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        containerColor = Color.White,
        title = {
            Text("Host a Campus Cab Pool", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = textPrimary)
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Select Car Capacity *", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = textPrimary)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (selectedCarCapacity == 4) primaryColor else Color(0xFFF1F5F9),
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                selectedCarCapacity = 4
                            }
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(vertical = 10.dp)
                        ) {
                            Text(
                                "4-Seater",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = if (selectedCarCapacity == 4) Color.White else textPrimary
                            )
                            Text(
                                "Sedan or Hatchback",
                                fontSize = 10.sp,
                                color = if (selectedCarCapacity == 4) Color.White.copy(alpha = 0.8f) else textSecondary
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (selectedCarCapacity == 7) primaryColor else Color(0xFFF1F5F9),
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                selectedCarCapacity = 7
                            }
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(vertical = 10.dp)
                        ) {
                            Text(
                                "7-Seater",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = if (selectedCarCapacity == 7) Color.White else textPrimary
                            )
                            Text(
                                "SUV or MUV",
                                fontSize = 10.sp,
                                color = if (selectedCarCapacity == 7) Color.White.copy(alpha = 0.8f) else textSecondary
                            )
                        }
                    }
                }

                Text("Route Category *", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = textPrimary)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("Airport" to "Airport", "Station" to "Station", "Mall" to "Mall", "Other" to "Other").forEach { (cat, label) ->
                        val sel = selectedCategoryType == cat
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (sel) primaryColor else Color(0xFFF1F5F9),
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    selectedCategoryType = cat
                                    errorField = null
                                    validationError = ""
                                }
                        ) {
                            Text(
                                text = label,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (sel) Color.White else Color(0xFF475569),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(vertical = 6.dp)
                            )
                        }
                    }
                }

                if (selectedCategoryType == "Other") {
                    val isCategoryError = errorField == "CATEGORY"
                    OutlinedTextField(
                        value = customCategoryName,
                        onValueChange = {
                            customCategoryName = it
                            errorField = null
                            validationError = ""
                        },
                        label = { Text("Specify Category Name *") },
                        placeholder = { Text("e.g. Fest, Concert, Exam Center, Hospital...", color = Color(0xFF94A3B8).copy(alpha = 0.6f)) },
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

                val isPickupError = errorField == "PICKUP"
                OutlinedTextField(
                    value = pickupPoint,
                    onValueChange = {
                        pickupPoint = it
                        errorField = null
                        validationError = ""
                    },
                    label = { Text("Campus Pickup Spot *") },
                    placeholder = { Text("e.g. Campus Main Gate, Girls Hostel Circle...", color = Color(0xFF94A3B8).copy(alpha = 0.6f)) },
                    singleLine = true,
                    isError = isPickupError,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = if (isPickupError) Color(0xFFE11D48) else primaryColor,
                        unfocusedBorderColor = if (isPickupError) Color(0xFFE11D48) else Color(0xFFCBD5E1)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                val isDestError = errorField == "DESTINATION"
                OutlinedTextField(
                    value = dropDestination,
                    onValueChange = {
                        dropDestination = it
                        errorField = null
                        validationError = ""
                    },
                    label = { Text("Drop Destination *") },
                    placeholder = { Text(destinationPlaceholder, color = Color(0xFF94A3B8).copy(alpha = 0.6f)) },
                    singleLine = true,
                    isError = isDestError,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = if (isDestError) Color(0xFFE11D48) else primaryColor,
                        unfocusedBorderColor = if (isDestError) Color(0xFFE11D48) else Color(0xFFCBD5E1)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                val isDateTimeError = errorField == "DATETIME" || !isDepartureInFuture
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
                                        departureDate = dateDisplayFormat.format(cal.time)

                                        val minCal = Calendar.getInstance().apply { add(Calendar.HOUR_OF_DAY, 1) }
                                        if (cal.timeInMillis < minCal.timeInMillis) {
                                            selectedHour = minCal.get(Calendar.HOUR_OF_DAY)
                                            selectedMinute = (minCal.get(Calendar.MINUTE) / 5) * 5
                                            val updatedCal = Calendar.getInstance().apply {
                                                set(y, m, d, selectedHour, selectedMinute, 0)
                                            }
                                            departureTime = timeDisplayFormat.format(updatedCal.time)
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
                            Text("Departure Date *", fontSize = 10.sp, color = textSecondary)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(departureDate, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = textPrimary)
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isDateTimeError) Color(0xFFFFF1F2) else Color(0xFFF8FAFC),
                        border = BorderStroke(1.dp, if (isDateTimeError) Color(0xFFE11D48) else Color(0xFFCBD5E1)),
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                showTimePickerModal = true
                            }
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
                            Text("Departure Time *", fontSize = 10.sp, color = textSecondary)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(departureTime, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = textPrimary)
                        }
                    }
                }

                val isFareError = errorField == "FARE"
                OutlinedTextField(
                    value = estimatedFare,
                    onValueChange = {
                        estimatedFare = it.filter { char -> char.isDigit() }
                        errorField = null
                        validationError = ""
                    },
                    label = { Text("Total Estimated Fare (Rs.) *") },
                    placeholder = { Text(farePlaceholder, color = Color(0xFF94A3B8).copy(alpha = 0.6f)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    isError = isFareError,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = if (isFareError) Color(0xFFE11D48) else primaryColor,
                        unfocusedBorderColor = if (isFareError) Color(0xFFE11D48) else Color(0xFFCBD5E1)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                if (fareInt > 0) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFF0FDF4),
                        border = BorderStroke(1.dp, Color(0xFFBBF7D0)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Calculated Split (" + totalSeats + " Seats):", fontSize = 11.sp, color = Color(0xFF166534))
                            Text("Rs. " + perPersonEstimate + " / person", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF15803D))
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Hide My Contact in Pool", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = textPrimary)
                        Text("Mask name and phone for other students", fontSize = 10.sp, color = textSecondary)
                    }
                    Switch(
                        checked = hideHostContact,
                        onCheckedChange = { hideHostContact = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = primaryColor)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Female-Only Pool", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = textPrimary)
                        Text("Restrict ride to female students", fontSize = 10.sp, color = textSecondary)
                    }
                    Switch(
                        checked = femaleOnly,
                        onCheckedChange = { femaleOnly = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = primaryColor)
                    )
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Trip Notes (Optional)") },
                    placeholder = { Text("Meeting instructions or trip advisory...", color = Color(0xFF94A3B8).copy(alpha = 0.6f)) },
                    maxLines = 2,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

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
                    if (pickupPoint.isBlank()) {
                        errorField = "PICKUP"
                        validationError = "Please enter Campus Pickup Spot."
                        triggerDeviceVibration(context)
                        coroutineScope.launch { scrollState.animateScrollTo(0) }
                        return@Button
                    }
                    if (dropDestination.isBlank()) {
                        errorField = "DESTINATION"
                        validationError = "Please enter Drop Destination."
                        triggerDeviceVibration(context)
                        coroutineScope.launch { scrollState.animateScrollTo(140) }
                        return@Button
                    }
                    if (pickupPoint.trim().equals(dropDestination.trim(), ignoreCase = true)) {
                        errorField = "DESTINATION"
                        validationError = "Pickup Spot and Drop Destination cannot be the same address."
                        triggerDeviceVibration(context)
                        coroutineScope.launch { scrollState.animateScrollTo(140) }
                        return@Button
                    }

                    val finalDepartureCal = Calendar.getInstance().apply {
                        set(selectedYear, selectedMonth, selectedDay, selectedHour, selectedMinute, 0)
                    }
                    if (finalDepartureCal.timeInMillis < System.currentTimeMillis() + 60 * 60 * 1000) {
                        errorField = "DATETIME"
                        validationError = "Departure time must be at least 1 hour in the future."
                        triggerDeviceVibration(context)
                        coroutineScope.launch { scrollState.animateScrollTo(260) }
                        return@Button
                    }

                    val fare = estimatedFare.toIntOrNull() ?: 0
                    if (fare <= 0) {
                        errorField = "FARE"
                        validationError = "Please enter a valid Total Estimated Fare."
                        triggerDeviceVibration(context)
                        coroutineScope.launch { scrollState.animateScrollTo(380) }
                        return@Button
                    }

                    val finalCategory = if (selectedCategoryType == "Other") {
                        val (valid, errorMsg) = CategoryFilterValidator.validateCategoryName(customCategoryName)
                        if (!valid) {
                            errorField = "CATEGORY"
                            validationError = errorMsg
                            triggerDeviceVibration(context)
                            coroutineScope.launch { scrollState.animateScrollTo(80) }
                            return@Button
                        }
                        customCategoryName.trim()
                    } else {
                        selectedCategoryType
                    }

                    val cabTypeName = if (selectedCarCapacity == 4) "4-Seater" else "7-Seater"

                    val pool = CabPool(
                        id = "CAB-POOL-" + (100..999).random(),
                        hostEmail = currentUserEmail,
                        hostName = currentUserName,
                        hostPhone = "+91 98765 00000",
                        hostHostel = "Campus Resident",
                        hostBatch = "Batch 2026",
                        pickupPoint = pickupPoint.trim(),
                        dropDestination = dropDestination.trim(),
                        routeCategory = finalCategory,
                        departureDate = departureDate.trim(),
                        departureTime = departureTime.trim(),
                        departureTimestamp = finalDepartureCal.timeInMillis,
                        totalSeats = selectedCarCapacity,
                        bookedSeats = 1,
                        riders = listOf(
                            CabRider(currentUserEmail, currentUserName, "+91 98765 00000", "Hostel", paid = true, hideContactInPreview = hideHostContact)
                        ),
                        estimatedTotalFare = fare,
                        perPersonFare = perPersonEstimate,
                        cabType = cabTypeName,
                        femaleOnly = femaleOnly,
                        hideHostContactInPreview = hideHostContact,
                        notes = notes.trim(),
                        cabDriverDetails = null,
                        status = "GATHERING"
                    )
                    onProceedToPayFee(pool)
                },
                enabled = isFormCompleteAndValid,
                colors = ButtonDefaults.buttonColors(
                    containerColor = primaryColor,
                    disabledContainerColor = Color(0xFFE2E8F0),
                    disabledContentColor = Color(0xFF94A3B8)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Proceed to Pay Platform Convenience Fee (Rs. 20)")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color(0xFF64748B))
            }
        }
    )

    if (showTimePickerModal) {
        CampusFutureTimePickerDialog(
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
                departureTime = timeDisplayFormat.format(cal.time)
                showTimePickerModal = false
                errorField = null
                validationError = ""
            }
        )
    }
}

// Dialog 3: Pool Details Page (with live countdown badge)
fun generateProfessionalCabShareText(pool: CabPool): String {
    val openSeats = (pool.totalSeats - pool.bookedSeats).coerceAtLeast(0)
    val sb = StringBuilder()
    sb.appendLine("\uD83D\uDE97 *CAMPUS CAB POOL INVITATION*")
    sb.appendLine("\u2022 *Ride ID:* `${pool.id}`")
    sb.appendLine("\u2022 *Organized via:* CampusApp Verified Student Network")
    sb.appendLine()
    sb.appendLine("\uD83D\uDCCD *ROUTE & DESTINATION*")
    sb.appendLine("\u2022 *Pickup:* ${pool.pickupPoint}")
    sb.appendLine("\u2022 *Destination:* ${pool.dropDestination}")
    sb.appendLine("\u2022 *Category:* ${pool.routeCategory}")
    sb.appendLine()
    sb.appendLine("\uD83D\uDDD3\uFE0F *SCHEDULE*")
    sb.appendLine("\u2022 *Date:* ${pool.departureDate}")
    sb.appendLine("\u2022 *Departure:* ${pool.departureTime}")
    sb.appendLine()
    sb.appendLine("\uD83D\uDC65 *SEATS & FARE SPLIT*")
    sb.appendLine("\u2022 *Vehicle Type:* ${pool.cabType}")
    sb.appendLine("\u2022 *Open Seats:* $openSeats of ${pool.totalSeats} seats available")
    sb.appendLine("\u2022 *Per Rider:* Rs. ${pool.perPersonFare} (Total Est: Rs. ${pool.estimatedTotalFare})")
    if (pool.femaleOnly) {
        sb.appendLine("\u2022 *Policy:* \uD83D\uDD12 Female-Only Cab Pool")
    }
    if (pool.notes.isNotBlank()) {
        sb.appendLine()
        sb.appendLine("\uD83D\uDCDD *HOST ADVISORY*")
        sb.appendLine("\"${pool.notes.trim()}\"")
    }
    if (!pool.cabDriverDetails.isNullOrBlank()) {
        sb.appendLine()
        sb.appendLine("\uD83D\uDE96 *DRIVER & VEHICLE DETAILS*")
        sb.appendLine("${pool.cabDriverDetails.trim()}")
    }
    sb.appendLine()
    sb.appendLine("\uD83D\uDCF1 *HOW TO JOIN:*")
    sb.appendLine("1. Open *CampusApp*")
    sb.appendLine("2. Select *Cab Pooling*")
    sb.appendLine("3. Search Ride ID: *${pool.id}*")
    sb.appendLine()
    sb.appendLine("\uD83D\uDCF2 *DOWNLOAD CAMPUSAPP:*")
    sb.appendLine("Get the app on Google Play Store to join this pool & split fares:")
    sb.appendLine("https://play.google.com/store/apps/details?id=com.college.campusapp")
    sb.appendLine()
    sb.appendLine("_Split fares, save travel costs, and ride safely with campus peers!_")
    return sb.toString().trim()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CabPoolDetailDialog(
    pool: CabPool,
    currentUserEmail: String,
    currentUserName: String,
    onDismiss: () -> Unit,
    onJoinClick: () -> Unit,
    onRequestHostTransfer: () -> Unit,
    onAcceptHostTransfer: () -> Unit,
    onDeclineHostTransfer: () -> Unit,
    onLeavePool: () -> Unit,
    onCancelPool: () -> Unit,
    onCompletePool: () -> Unit,
    onAddCabDetails: (String) -> Unit,
    onSendMessage: (String) -> Unit
) {
    val context = LocalContext.current
    val primaryColor = AppTheme.Primary
    val textPrimary = AppTheme.InkPrimary
    val textSecondary = AppTheme.InkSecondary
    val isUserInPool = pool.hostEmail == currentUserEmail || pool.riders.any { it.email == currentUserEmail }
    val isHost = pool.hostEmail == currentUserEmail
    val isFull = pool.bookedSeats >= pool.totalSeats || pool.riders.size >= pool.totalSeats
    val isLocked = pool.isCancellationLocked()
    val isUnderOneHour = pool.departureTimestamp - System.currentTimeMillis() in 1..3600000
    val isPendingTransferForMe = pool.pendingHostTransferEmail == currentUserEmail

    var chatText by remember { mutableStateOf("") }
    var cabDetailsInput by remember { mutableStateOf(pool.cabDriverDetails ?: "") }
    var showCabEdit by remember { mutableStateOf(false) }

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
                        text = if (isUserInPool) "Cab Pool Details" else "Trip Preview",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = textPrimary
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Pool ID: " + pool.id, fontSize = 11.sp, color = textSecondary)
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
                                    val clip = android.content.ClipData.newPlainText("Pool ID", pool.id)
                                    clipboard?.setPrimaryClip(clip)
                                    Toast.makeText(context, "Pool ID copied to clipboard!", Toast.LENGTH_SHORT).show()
                                }
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .background(if (isUserInPool) Color(0xFFE0F2FE) else Color(0xFFFEF3C7), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = if (isUserInPool) pool.status else "TRIP PREVIEW",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isUserInPool) primaryColor else Color(0xFFB45309)
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
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (isUnderOneHour) {
                    LiveCabCountdownBadge(targetTimestamp = pool.departureTimestamp, modifier = Modifier.fillMaxWidth())
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
                                text = "${pool.hostName} wants to transfer host ownership of this pool to you. Do you accept?",
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

                if (isLocked && isUserInPool) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFFEE2E2),
                        border = BorderStroke(1.dp, Color(0xFFFECACA)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text("Departure Within 1 Hour", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color(0xFF991B1B))
                            Text("Cancellations and seat releases are locked within 1 hour of scheduled departure.", fontSize = 10.sp, color = Color(0xFF7F1D1D))
                        }
                    }
                }

                // Trip Details Card
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFFF8FAFC),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("From: " + pool.pickupPoint, fontSize = 12.sp, color = textSecondary)
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("To: " + pool.dropDestination, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = textPrimary, modifier = Modifier.weight(1f))
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
                                            val mapUri = Uri.parse("geo:0,0?q=" + Uri.encode(pool.dropDestination))
                                            val mapIntent = Intent(Intent.ACTION_VIEW, mapUri)
                                            context.startActivity(mapIntent)
                                        } catch (e: Exception) {
                                            Toast.makeText(context, "Unable to launch Maps", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                    .padding(horizontal = 6.dp, vertical = 3.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("Departure: " + pool.departureDate + " at " + pool.departureTime, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF0284C7))
                        Text("Seats: " + pool.bookedSeats + "/" + pool.totalSeats + " Filled (" + (pool.totalSeats - pool.bookedSeats).coerceAtLeast(0) + " open)", fontSize = 11.sp, color = textSecondary)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Actual Fare: Rs. " + pool.perPersonFare + " / person (Total: Rs. " + pool.estimatedTotalFare + ")",
                            fontSize = 12.sp,
                            color = Color(0xFF16A34A),
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }

                // Trip tags
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TagPill(pool.cabType)
                    TagPill(pool.routeCategory)
                    if (pool.femaleOnly) {
                        TagPill("Female Only", bg = Color(0xFFFDF2F8), textColor = Color(0xFFDB2777))
                    }
                }

                if (pool.notes.isNotEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFFFAFAFA),
                        border = BorderStroke(1.dp, Color(0xFFF1F5F9)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text("Host Trip Advisory:", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = textSecondary)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(text = pool.notes, fontSize = 11.sp, color = textPrimary)
                        }
                    }
                }

                // If JOINED
                if (isUserInPool) {
                    if (!pool.cabDriverDetails.isNullOrEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFEFF6FF),
                            border = BorderStroke(1.dp, Color(0xFFBFDBFE)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text("Vehicle and Driver Assigned", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color(0xFF1E40AF))
                                Text(pool.cabDriverDetails ?: "", fontSize = 11.sp, color = Color(0xFF1E3A8A))
                            }
                        }
                    } else if (isHost) {
                        if (showCabEdit) {
                            OutlinedTextField(
                                value = cabDetailsInput,
                                onValueChange = { cabDetailsInput = it },
                                label = { Text("Driver Name, Cab No & Phone") },
                                placeholder = { Text("e.g. Innova KA 01 AB 1234 (Ramesh 9876543210)", color = Color(0xFF94A3B8).copy(alpha = 0.6f)) },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            )
                            Button(
                                onClick = {
                                    onAddCabDetails(cabDetailsInput)
                                    showCabEdit = false
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Save Driver Info", fontSize = 11.sp)
                            }
                        } else {
                            OutlinedButton(
                                onClick = { showCabEdit = true },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Add or Update Driver Info", fontSize = 11.sp)
                            }
                        }
                    }

                    Text("Pool Members (" + pool.riders.size + "/" + pool.totalSeats + "):", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = textPrimary)
                    pool.riders.forEachIndexed { idx, rider ->
                        val isMe = rider.email == currentUserEmail
                        val isRiderHost = rider.email == pool.hostEmail
                        val isHidden = rider.hideContactInPreview && !isMe

                        val memberName = when {
                            isMe -> rider.name + " (You)"
                            isHidden -> if (isRiderHost) "Host (Contact Hidden)" else "Student " + (idx + 1) + " (Contact Hidden)"
                            else -> if (isRiderHost) rider.name + " (Host)" else rider.name
                        }
                        val memberSub = when {
                            isMe -> rider.hostel + " - " + rider.phone
                            isHidden -> "Verified Student (Private)"
                            else -> rider.hostel + " - " + rider.phone
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
                                if (!isMe && !isHidden && rider.phone.isNotBlank()) {
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
                                                    val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + rider.phone))
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
                            val shareText = generateProfessionalCabShareText(pool)
                            val sendIntent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TEXT, shareText)
                                type = "text/plain"
                            }
                            context.startActivity(Intent.createChooser(sendIntent, "Share Ride Pool"))
                        },
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color(0xFF22C55E)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Share via WhatsApp or SMS", fontSize = 12.sp, color = Color(0xFF15803D), fontWeight = FontWeight.Bold)
                    }

                    Divider(color = Color(0xFFF1F5F9), thickness = 1.dp)
                    Text("In-Pool Chat:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = textPrimary)

                    if (pool.chatMessages.isEmpty()) {
                        Text("No messages yet. Coordinate pickup timing here.", fontSize = 11.sp, color = textSecondary)
                    } else {
                        pool.chatMessages.forEach { chat ->
                            val isMe = chat.senderEmail == currentUserEmail
                            val timeFormatted = remember(chat.timestamp) {
                                val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
                                sdf.format(Date(chat.timestamp))
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
                        OutlinedTextField(
                            value = chatText,
                            onValueChange = { chatText = it },
                            placeholder = { Text("Message...", fontSize = 11.sp, color = Color(0xFF94A3B8).copy(alpha = 0.6f)) },
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Button(
                            onClick = {
                                if (chatText.isNotBlank()) {
                                    onSendMessage(chatText)
                                    chatText = ""
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Send", fontSize = 11.sp)
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (!isUserInPool) {
                if (!isFull) {
                    Button(
                        onClick = onJoinClick,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Join Pool (Pay Rs. 20 Platform Fee)")
                    }
                }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (isHost && pool.status != "COMPLETED") {
                        if (pool.riders.size > 1) {
                            OutlinedButton(
                                onClick = onRequestHostTransfer,
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Transfer Host and Leave")
                            }
                        }
                        if (!isLocked) {
                            OutlinedButton(
                                onClick = onCancelPool,
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Cancel Pool")
                            }
                        }
                        Button(
                            onClick = onCompletePool,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Finish Ride")
                        }
                    } else if (!isHost && pool.status != "COMPLETED") {
                        if (!isLocked) {
                            OutlinedButton(
                                onClick = onLeavePool,
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Leave Pool (No Refund)")
                            }
                        } else {
                            Text("Seat release locked (<1h)", color = Color.Red, fontSize = 11.sp, fontWeight = FontWeight.Bold)
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

private fun isUpiPaymentSuccessful(resultCode: Int, data: Intent?): Boolean {
    if (data != null) {
        val response = data.getStringExtra("response")
            ?: data.extras?.keySet()?.joinToString("&") { "$it=${data.extras?.get(it)}" }
            ?: ""
        val lower = response.lowercase()
        if (lower.contains("status=success") || lower.contains("status=00") || lower.contains("txnstatus=success") || lower.contains("success")) {
            return true
        }
        if (lower.contains("status=failure") || lower.contains("status=failed") || lower.contains("status=cancelled") || lower.contains("status=cancel") || lower.contains("cancelled") || lower.contains("failure")) {
            return false
        }
    }
    return resultCode == android.app.Activity.RESULT_OK
}