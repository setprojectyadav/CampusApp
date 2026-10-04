package com.college.campusapp

import android.content.Intent

import android.os.Bundle

import android.widget.Toast

import androidx.activity.ComponentActivity

import androidx.activity.compose.setContent

import androidx.compose.animation.*

import androidx.compose.animation.core.*

import androidx.compose.foundation.BorderStroke

import androidx.compose.foundation.background

import androidx.compose.foundation.clickable

import androidx.compose.foundation.border

import androidx.compose.foundation.layout.*

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

import androidx.compose.ui.draw.clip

import androidx.compose.ui.draw.rotate

import androidx.compose.ui.graphics.Color

import androidx.compose.ui.graphics.Brush

import androidx.compose.ui.platform.LocalContext

import androidx.compose.ui.text.font.FontStyle

import androidx.compose.ui.text.font.FontWeight

import androidx.compose.ui.text.input.KeyboardType

import androidx.compose.ui.text.style.TextAlign

import androidx.compose.ui.unit.dp

import androidx.compose.ui.unit.sp

import com.college.campusapp.security.EncryptionManager

import com.college.campusapp.security.SecurityUtils

import kotlinx.coroutines.delay

import kotlinx.coroutines.launch

import androidx.compose.ui.platform.LocalLifecycleOwner

import androidx.lifecycle.Lifecycle

import androidx.lifecycle.LifecycleEventObserver

import androidx.lifecycle.lifecycleScope

import android.net.Uri

data class ActiveTransfer(

    val id: String,

    val otp: String,

    val itemName: String,

    val fee: Int,

    val pickPoint: String,

    val dropPoint: String

)

class TransferProductActivity : ComponentActivity() {

    var lastUpiPaymentResult: Int? = null

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {

        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == 222) {

            lastUpiPaymentResult = resultCode

            if (resultCode == android.app.Activity.RESULT_OK && data != null) {

                val response = data.getStringExtra("response") ?: ""

                val isSuccess = response.lowercase().contains("status=success") || response.lowercase().contains("success")

                if (!isSuccess) {

                    lastUpiPaymentResult = android.app.Activity.RESULT_CANCELED

                }

            }

        }

    }

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        OrderRepository.load(this)

        setContent {

            val context = LocalContext.current

            val trackTransferId = remember { (context as? android.app.Activity)?.intent?.getStringExtra("TRACK_TRANSFER_ID") ?: "" }

            var activeTransfer by remember {

                mutableStateOf<ActiveTransfer?>(

                    if (trackTransferId.isNotEmpty()) {

                        val matchedOrder = OrderRepository.pendingOrders.find { it.id == trackTransferId }

                            ?: OrderRepository.completedOrders.find { it.id == trackTransferId }

                            ?: OrderRepository.cancelledOrders.find { it.id == trackTransferId }

                        if (matchedOrder != null) {

                            ActiveTransfer(

                                id = matchedOrder.id,

                                otp = matchedOrder.deliveryOtp,

                                itemName = matchedOrder.items.firstOrNull()?.first ?: "Package",

                                fee = matchedOrder.totalMin,

                                pickPoint = matchedOrder.pickPoint,

                                dropPoint = matchedOrder.dropPoint

                            )

                        } else {

                            null

                        }

                    } else {

                        null

                    }

                )

            }

            var deliveryStatus by remember(activeTransfer) {

                mutableStateOf(

                    if (activeTransfer != null) {

                        val matched = OrderRepository.pendingOrders.find { it.id == activeTransfer?.id }

                        when (matched?.status) {

                            "ACCEPTED" -> "accepted"

                            "DELIVERING" -> "in_transit"

                            "RIDER_ARRIVED" -> "arrived"

                            "DELIVERED" -> "completed"

                            else -> "searching"

                        }

                    } else {

                        "searching"

                    }

                )

            }

            androidx.activity.compose.BackHandler(enabled = activeTransfer != null) {

                (context as? android.app.Activity)?.intent?.removeExtra("TRACK_TRANSFER_ID")

                activeTransfer = null

                deliveryStatus = "searching"

            }

            // Track simulated runner name

            var runnerName by remember { mutableStateOf("") }

            val runnerNames = listOf("Rahul", "Priya", "Amit", "Sneha", "Karan")

            // UPI / Payment states

            var isWaitingForUpiReturn by remember { mutableStateOf(false) }

            var pendingTransferOrder by remember { mutableStateOf<SimulatedOrder?>(null) }

            var pendingActiveTransfer by remember { mutableStateOf<ActiveTransfer?>(null) }

            var pendingWalletDeduction by remember { mutableStateOf(0) }

            var isProcessingPayment by remember { mutableStateOf(false) }

            var paymentProcessingText by remember { mutableStateOf("") }

            var showUpiVerifyDialog by remember { mutableStateOf(false) }

            var showSuccessDialog by remember { mutableStateOf(false) }

            var successActiveTransfer by remember { mutableStateOf<ActiveTransfer?>(null) }

            var showMyOrdersPage by remember { mutableStateOf(false) }

            var shouldResetForm by remember { mutableStateOf(false) }

            var searchSeconds by remember { mutableStateOf(0) }

            var walletBalance by remember { mutableStateOf(WalletManager.getBalance(context)) }

            LaunchedEffect(activeTransfer, deliveryStatus) {

                if (activeTransfer != null && deliveryStatus == "searching") {

                    while (deliveryStatus == "searching") {

                        searchSeconds = OrderRepository.searchTimerMap[activeTransfer!!.id] ?: 0

                        delay(1000L)

                    }

                }

            }

            // Poll OrderRepository for status updates from delivery console

            LaunchedEffect(activeTransfer) {

                val currentActive = activeTransfer

                if (currentActive != null) {

                    val transferId = currentActive.id

                    var previousStatus = ""

                    while (true) {

                        delay(2000L)

                        OrderRepository.load(context, force = true)

                        val matchedOrder = OrderRepository.pendingOrders.find { it.id == transferId }

                            ?: OrderRepository.completedOrders.find { it.id == transferId }

                        val newStatus = matchedOrder?.status ?: "PENDING"

                        if (newStatus != previousStatus) {

                            previousStatus = newStatus

                            val orderNum = transferId.replace("OrderNo-", "").replace("CSV-", "")

                            when (newStatus) {

                                "ACCEPTED", "REACHED_SHOP", "ITEMS_PICKED" -> {

                                    if (matchedOrder?.runnerName?.isNotEmpty() == true) {

                                        runnerName = matchedOrder.runnerName ?: ""

                                    } else if (runnerName.isEmpty()) {

                                        runnerName = "Runner"

                                    }

                                    deliveryStatus = "accepted"

                                }

                                "DELIVERING" -> {

                                    if (matchedOrder?.runnerName?.isNotEmpty() == true) {

                                        runnerName = matchedOrder.runnerName ?: ""

                                    }

                                    deliveryStatus = "in_transit"

                                }

                                "RIDER_ARRIVED" -> {

                                    if (matchedOrder?.runnerName?.isNotEmpty() == true) {

                                        runnerName = matchedOrder.runnerName ?: ""

                                    }

                                    deliveryStatus = "arrived"

                                }

                                "DELIVERED" -> {

                                    if (matchedOrder?.runnerName?.isNotEmpty() == true) {

                                        runnerName = matchedOrder.runnerName ?: ""

                                    }

                                    deliveryStatus = "completed"

                                }

                                "CANCELLED" -> {

                                    deliveryStatus = "searching"

                                    activeTransfer = null

                                    break

                                }

                                else -> {

                                    deliveryStatus = "searching"

                                }

                            }

                        }

                        // Stop polling if completed or cancelled

                        if (deliveryStatus == "completed") break

                    }

                }

            }

            val lifecycleOwner = LocalLifecycleOwner.current

            DisposableEffect(lifecycleOwner) {

                val observer = LifecycleEventObserver { _, event ->

                    if (event == Lifecycle.Event.ON_RESUME) {

                        walletBalance = WalletManager.getBalance(context)

                        // Handle UPI Payment Return Redirect

                        if (isWaitingForUpiReturn && pendingTransferOrder != null) {

                            if (this@TransferProductActivity.lastUpiPaymentResult == android.app.Activity.RESULT_CANCELED) {

                                isWaitingForUpiReturn = false

                                pendingTransferOrder = null

                                pendingActiveTransfer = null

                                pendingWalletDeduction = 0

                                Toast.makeText(context, "Payment failed or cancelled in UPI app.", Toast.LENGTH_LONG).show()

                            } else {

                                showUpiVerifyDialog = true

                            }

                        }

                    }

                }

                lifecycleOwner.lifecycle.addObserver(observer)

                onDispose {

                    lifecycleOwner.lifecycle.removeObserver(observer)

                }

            }

            if (showUpiVerifyDialog && pendingTransferOrder != null) {

                val order = pendingTransferOrder

                val upiAmount = maxOf(0, (order?.totalMin ?: 0) - pendingWalletDeduction)

                AlertDialog(

                    onDismissRequest = {},

                    title = {

                        Text(

                            text = "UPI Payment Status",

                            fontWeight = FontWeight.Bold,

                            color = AppTheme.InkPrimary

                        )

                    },

                    text = {

                        Text(

                            text = "Did you complete the payment of ₹$upiAmount in the UPI app?",

                            color = AppTheme.InkSecondary

                        )

                    },

                    confirmButton = {

                        Button(

                            onClick = {

                                showUpiVerifyDialog = false

                                isWaitingForUpiReturn = false

                                lifecycleScope.launch {

                                    isProcessingPayment = true

                                    paymentProcessingText = "Verifying secure UPI payment status..."

                                    delay(2000L) // Professional delay

                                    val deduction = pendingWalletDeduction

                                    val active = pendingActiveTransfer

                                    val currentOrder = pendingTransferOrder

                                    if (deduction > 0) {

                                        WalletManager.debit(context, deduction, "Payment for Transfer ${currentOrder?.id}")

                                        walletBalance = WalletManager.getBalance(context)

                                    }

                                    if (currentOrder != null) {

                                        OrderRepository.pendingOrders.add(currentOrder)

                                        OrderRepository.save(context)

                                    }

                                    pendingTransferOrder = null

                                    pendingActiveTransfer = null

                                    pendingWalletDeduction = 0

                                    isProcessingPayment = false

                                    successActiveTransfer = active

                                    showSuccessDialog = true

                                }

                            },

                            colors = ButtonDefaults.buttonColors(containerColor = AppTheme.Primary)

                        ) {

                            Text("Yes, Paid", color = Color.White, fontWeight = FontWeight.Bold)

                        }

                    },

                    dismissButton = {

                        TextButton(

                            onClick = {

                                showUpiVerifyDialog = false

                                isWaitingForUpiReturn = false

                                pendingTransferOrder = null

                                pendingActiveTransfer = null

                                pendingWalletDeduction = 0

                                Toast.makeText(context, "Payment Cancelled: Transfer booking not completed.", Toast.LENGTH_LONG).show()

                            }

                        ) {

                            Text("No, Cancelled", color = AppTheme.Danger, fontWeight = FontWeight.Bold)

                        }

                    },

                    shape = AppTheme.CardShape,

                    containerColor = Color.White

                )

            }

            if (showSuccessDialog && successActiveTransfer != null) {

                val transfer = successActiveTransfer

                androidx.compose.ui.window.Dialog(

                    onDismissRequest = {

                        showSuccessDialog = false

                        successActiveTransfer = null

                    }

                ) {

                    Card(

                        modifier = Modifier.fillMaxWidth().padding(8.dp),

                        shape = RoundedCornerShape(28.dp),

                        colors = CardDefaults.cardColors(containerColor = Color.White),

                        elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)

                    ) {

                        Column(

                            modifier = Modifier.fillMaxWidth().padding(28.dp),

                            horizontalAlignment = Alignment.CenterHorizontally

                        ) {

                            Box(

                                modifier = Modifier

                                    .size(64.dp)

                                    .background(Color(0xFFECFDF5), CircleShape)

                                    .border(2.dp, Color(0xFF10B981), CircleShape),

                                contentAlignment = Alignment.Center

                            ) {

                                Text("✓", color = Color(0xFF10B981), fontWeight = FontWeight.Bold, fontSize = 28.sp)

                            }

                            Spacer(modifier = Modifier.height(20.dp))

                            Text(

                                text = "Order Booked Successfully!",

                                fontWeight = FontWeight.ExtraBold,

                                fontSize = 20.sp,

                                color = AppTheme.InkPrimary,

                                textAlign = TextAlign.Center

                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(

                                text = "Your package transfer request has been placed.",

                                fontSize = 13.sp,

                                color = AppTheme.InkSecondary,

                                textAlign = TextAlign.Center

                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            Divider(color = AppTheme.DividerColor)

                            Spacer(modifier = Modifier.height(16.dp))

                            // 1. Order No

                             Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {

                                 Text("Order No", color = AppTheme.InkSecondary, fontSize = 13.sp)

                                 Text(transfer?.id?.replace("OrderNo-", "")?.replace("CSV-", "") ?: "", fontWeight = FontWeight.Bold, color = AppTheme.InkPrimary, fontSize = 13.sp)

                             }

                             Spacer(modifier = Modifier.height(8.dp))

                             // 2. Item Name

                             Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {

                                 Text("Item Name", color = AppTheme.InkSecondary, fontSize = 13.sp)

                                 Text(

                                     text = if ((transfer?.itemName?.length ?: 0) > 18) {

                                         transfer?.itemName?.take(18) + "..."

                                     } else {

                                         transfer?.itemName ?: ""

                                     },

                                     fontWeight = FontWeight.Bold,

                                     color = AppTheme.InkPrimary,

                                     fontSize = 13.sp

                                 )

                             }

                             Spacer(modifier = Modifier.height(8.dp))

                             // 3. Pickup Location

                             Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {

                                 Text("Pickup Location", color = AppTheme.InkSecondary, fontSize = 13.sp)

                                 Text(

                                     text = if ((transfer?.pickPoint?.length ?: 0) > 18) {

                                         transfer?.pickPoint?.take(18) + "..."

                                     } else {

                                         transfer?.pickPoint ?: ""

                                     },

                                     fontWeight = FontWeight.Bold,

                                     color = AppTheme.InkPrimary,

                                     fontSize = 13.sp

                                 )

                             }

                             Spacer(modifier = Modifier.height(8.dp))

                             // 4. Drop Location

                             Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {

                                 Text("Drop Location", color = AppTheme.InkSecondary, fontSize = 13.sp)

                                 Text(

                                     text = if ((transfer?.dropPoint?.length ?: 0) > 18) {

                                         transfer?.dropPoint?.take(18) + "..."

                                     } else {

                                         transfer?.dropPoint ?: ""

                                     },

                                     fontWeight = FontWeight.Bold,

                                     color = AppTheme.InkPrimary,

                                     fontSize = 13.sp

                                 )

                             }

                             Spacer(modifier = Modifier.height(8.dp))

                             // 5. Amount Paid

                             Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {

                                 Text("Amount Paid", color = AppTheme.InkSecondary, fontSize = 13.sp)

                                 Text("₹${transfer?.fee}", fontWeight = FontWeight.Bold, color = AppTheme.Primary, fontSize = 13.sp)

                             }

                            Spacer(modifier = Modifier.height(24.dp))

                            Button(

                                onClick = {

                                    activeTransfer = transfer

                                    showSuccessDialog = false

                                    successActiveTransfer = null

                                    shouldResetForm = true

                                },

                                shape = AppTheme.ButtonShape,

                                colors = ButtonDefaults.buttonColors(containerColor = AppTheme.Primary),

                                modifier = Modifier.fillMaxWidth().height(48.dp)

                            ) {

                                Text("Track Order ➔", color = Color.White, fontWeight = FontWeight.Bold)

                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            TextButton(

                                onClick = {

                                    showSuccessDialog = false

                                    successActiveTransfer = null

                                    shouldResetForm = true

                                },

                                modifier = Modifier.fillMaxWidth()

                            ) {

                                Text("Close", color = AppTheme.InkSecondary, fontWeight = FontWeight.Medium)

                            }

                        }

                    }

                }

            }

            if (isProcessingPayment) {

                androidx.compose.ui.window.Dialog(onDismissRequest = {}) {

                    Card(

                        shape = AppTheme.CardShape,

                        colors = CardDefaults.cardColors(containerColor = Color.White),

                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),

                        modifier = Modifier.fillMaxWidth().padding(24.dp)

                    ) {

                        Column(

                            modifier = Modifier.padding(24.dp),

                            horizontalAlignment = Alignment.CenterHorizontally,

                            verticalArrangement = Arrangement.Center

                        ) {

                            CircularProgressIndicator(color = AppTheme.Primary)

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(

                                text = paymentProcessingText,

                                fontSize = 14.sp,

                                fontWeight = FontWeight.Bold,

                                color = AppTheme.InkPrimary,

                                textAlign = TextAlign.Center

                            )

                        }

                    }

                }

            }

            TransferProductScreen(

                activeTransfer = activeTransfer,

                deliveryStatus = deliveryStatus,

                runnerName = runnerName,

                isProcessingPayment = isProcessingPayment,

                paymentProcessingText = paymentProcessingText,

                walletBalance = walletBalance,

                searchSeconds = searchSeconds,

                onInitiateTransfer = { transfer ->

                    activeTransfer = transfer

                },

                onCompleteTransfer = {

                    // Update state inside OrderRepository to complete

                    val matchedOrder = OrderRepository.pendingOrders.find { it.id == activeTransfer?.id }

                    if (matchedOrder != null) {

                        matchedOrder.status = "DELIVERED"

                        OrderRepository.completedOrders.add(matchedOrder)

                        OrderRepository.pendingOrders.remove(matchedOrder)

                        OrderRepository.save(this@TransferProductActivity)

                    }

                    deliveryStatus = "completed"

                },

                onCancelTransfer = { orderId ->

                    val matchedOrder = OrderRepository.pendingOrders.find { it.id == orderId }

                    if (matchedOrder != null) {

                        val refundAmount = matchedOrder.totalMin

                        WalletManager.credit(this@TransferProductActivity, refundAmount, "Transfer Cancelled: ${matchedOrder.id}")

                        matchedOrder.status = "CANCELLED"

                        OrderRepository.cancelledOrders.add(matchedOrder)

                        OrderRepository.pendingOrders.remove(matchedOrder)

                        OrderRepository.save(this@TransferProductActivity)

                        walletBalance = WalletManager.getBalance(context)

                        Toast.makeText(this@TransferProductActivity, "Order cancelled. Refund of ₹$refundAmount credited.", Toast.LENGTH_LONG).show()

                    }

                    activeTransfer = null

                    deliveryStatus = "searching"

                },

                onBookOrder = { itemNameVal, pickPointVal, dropPointVal, itemCountVal, itemSizeVal, extraTipVal, isUrgentVal, pickupNameVal, pickupPhoneVal, dropNameVal, dropPhoneVal, totalPayoutVal, useWalletVal ->

                    val orderId = "OrderNo-" + (10000000..99999999).random().toString()

                    val otp = (1000..9999).random().toString()

                    val walletDeduction = if (useWalletVal) minOf(walletBalance, totalPayoutVal) else 0

                    val upiAmount = if (useWalletVal) maxOf(0, totalPayoutVal - walletBalance) else totalPayoutVal

                    val detailsList = mutableListOf<String>()

                    if (itemSizeVal != "Small") {

                        detailsList.add("Size: $itemSizeVal")

                    }

                    val finalItemName = (itemNameVal + (if (detailsList.isNotEmpty()) " (${detailsList.joinToString(", ")})" else "")).uppercase(java.util.Locale.ROOT)

                    val upperPickPoint = pickPointVal.uppercase(java.util.Locale.ROOT)

                    val upperDropPoint = dropPointVal.uppercase(java.util.Locale.ROOT)

                    val userCollegeName = EncryptionManager.getString(context, "user_verified_college", "Googleplex Campus") ?: "Googleplex Campus"

                    val lastLatStr = EncryptionManager.getString(context, "user_last_latitude", "0.0") ?: "0.0"
                    val lastLonStr = EncryptionManager.getString(context, "user_last_longitude", "0.0") ?: "0.0"
                    var orderLat = lastLatStr.toDoubleOrNull() ?: 0.0
                    var orderLon = lastLonStr.toDoubleOrNull() ?: 0.0
                    if (orderLat == 0.0 && orderLon == 0.0) {
                        val activeCamp = EncryptionManager.getString(context, "user_verified_college", "Googleplex Campus") ?: "Googleplex Campus"
                        if (activeCamp.contains("Stanford", ignoreCase = true)) {
                            orderLat = 37.4275
                            orderLon = -122.1697
                        } else if (activeCamp.contains("Delhi", ignoreCase = true) || activeCamp.contains("IIT", ignoreCase = true)) {
                            orderLat = 28.5450
                            orderLon = 77.1926
                        } else if (activeCamp.contains("Pilani", ignoreCase = true) || activeCamp.contains("BITS", ignoreCase = true)) {
                            orderLat = 28.3639
                            orderLon = 75.5866
                        } else if (activeCamp.contains("Technological", ignoreCase = true) || activeCamp.contains("DTU", ignoreCase = true)) {
                            orderLat = 28.7501
                            orderLon = 77.1177
                        } else {
                            orderLat = 37.4220
                            orderLon = -122.0841
                        }
                    }
                    val simulatedOrder = SimulatedOrder(
                        id = orderId,
                        customerId = EncryptionManager.getString(context, "user_email") ?: "student@college.edu",
                        deliveryOtp = otp,
                        items = listOf(finalItemName to 1),
                        pickPoint = upperPickPoint,
                        dropPoint = upperDropPoint,
                        totalMin = totalPayoutVal,
                        totalMax = totalPayoutVal,
                        deliveryFee = (20 + (itemCountVal - 1) * 5) + (if (itemSizeVal == "Medium") 15 else if (itemSizeVal == "Large") 40 else 0) + (if (isUrgentVal) 10 else 0) + extraTipVal,
                        gatePremium = 10,
                        status = "PENDING",
                        isEstimated = false,
                        pickupPhone = pickupPhoneVal,
                        dropPhone = dropPhoneVal,
                        collegeName = userCollegeName.replace(" (Simulated)", "").trim(),
                        pickupLat = orderLat,
                        pickupLon = orderLon
                    )

                    val targetActiveTransfer = ActiveTransfer(

                        id = orderId,

                        otp = otp,

                        itemName = finalItemName,

                        fee = totalPayoutVal,

                        pickPoint = upperPickPoint,

                        dropPoint = upperDropPoint

                    )

                    if (upiAmount > 0) {

                        pendingTransferOrder = simulatedOrder

                        pendingActiveTransfer = targetActiveTransfer

                        pendingWalletDeduction = walletDeduction

                        isWaitingForUpiReturn = true

                        val upiUriStr = "upi://pay?pa=campusrelay@ybl&pn=CampusApp&tr=$orderId&am=$upiAmount&cu=INR&tn=Payment%20for%20Transfer%20$orderId"

                        try {

                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(upiUriStr))

                            val chooser = Intent.createChooser(intent, "Pay with UPI app")

                            this@TransferProductActivity.lastUpiPaymentResult = null

                            (context as? android.app.Activity)?.startActivityForResult(chooser, 222)

                        } catch (e: Exception) {

                            this@TransferProductActivity.lastUpiPaymentResult = android.app.Activity.RESULT_OK

                            Toast.makeText(context, "No UPI apps found. Simulating payment...", Toast.LENGTH_SHORT).show()

                        }

                    } else {

                        // Wallet only

                        lifecycleScope.launch {

                            isProcessingPayment = true

                            paymentProcessingText = "Securing transaction with App Wallet..."

                            delay(1800L)

                            if (walletDeduction > 0) {

                                WalletManager.debit(context, walletDeduction, "Payment for Transfer $orderId")

                                walletBalance = WalletManager.getBalance(context)

                            }

                            OrderRepository.pendingOrders.add(simulatedOrder)

                            OrderRepository.save(context)

                            isProcessingPayment = false

                            successActiveTransfer = targetActiveTransfer

                            showSuccessDialog = true

                        }

                    }

                },

                onMyOrdersClick = {

                    OrderRepository.load(this@TransferProductActivity, force = true)

                    showMyOrdersPage = true

                },

                onClose = {

                    if (activeTransfer != null) {

                        (context as? android.app.Activity)?.intent?.removeExtra("TRACK_TRANSFER_ID")

                        activeTransfer = null

                        deliveryStatus = "searching"

                    } else {

                        finish()

                    }

                },

                shouldResetForm = shouldResetForm,

                onFormResetCompleted = { shouldResetForm = false }

            )

            if (showMyOrdersPage) {

                val currentUserEmail = remember { EncryptionManager.getString(context, "user_email") ?: "student@college.edu" }

                MyTransfersPage(

                    currentUserEmail = currentUserEmail,

                    primaryColor = AppTheme.Primary,

                    textPrimary = AppTheme.InkPrimary,

                    textSecondary = AppTheme.InkSecondary,

                    onTrackOrderClick = { order ->

                        val matchedActive = ActiveTransfer(

                            id = order.id,

                            otp = order.deliveryOtp,

                            itemName = order.items.firstOrNull()?.first ?: "Package",

                            fee = order.totalMin,

                            pickPoint = order.pickPoint,

                            dropPoint = order.dropPoint

                        )

                        activeTransfer = matchedActive

                        showMyOrdersPage = false

                    },

                    onBack = { showMyOrdersPage = false },

                    modifier = Modifier.fillMaxSize()

                )

            }

        }

    }

}

@OptIn(ExperimentalMaterial3Api::class)

@Composable

fun TransferProductScreen(

    activeTransfer: ActiveTransfer?,

    deliveryStatus: String,

    runnerName: String,

    isProcessingPayment: Boolean,

    paymentProcessingText: String,

    walletBalance: Int,

    searchSeconds: Int,

    onInitiateTransfer: (ActiveTransfer) -> Unit,

    onCompleteTransfer: () -> Unit,

    onCancelTransfer: (String) -> Unit,

    onBookOrder: (

        itemName: String,

        pickPoint: String,

        dropPoint: String,

        itemCount: Int,

        itemSize: String,

        extraTip: Int,

        isUrgent: Boolean,

        pickupName: String,

        pickupPhone: String,

        dropName: String,

        dropPhone: String,

        totalPayout: Int,

        useWallet: Boolean

    ) -> Unit,

    onMyOrdersClick: () -> Unit,

    onClose: () -> Unit,

    shouldResetForm: Boolean,

    onFormResetCompleted: () -> Unit

) {

    val context = LocalContext.current

    val currentUserEmail = remember { EncryptionManager.getString(context, "user_email") ?: "student@college.edu" }

    val primaryColor = AppTheme.Primary

    val textPrimary = AppTheme.InkPrimary

    val textSecondary = AppTheme.InkSecondary

    var itemName by remember { mutableStateOf("") }

    var pickPoint by remember { mutableStateOf("") }

    var dropPoint by remember { mutableStateOf("") }

    var itemCount by remember { mutableStateOf(1) }

    var itemSize by remember { mutableStateOf("Small") }

    var extraTip by remember { mutableStateOf(0) }

    var customTipText by remember { mutableStateOf("") }

    var isUrgent by remember { mutableStateOf(false) }

    var transferMode by remember { mutableStateOf("Send") } // "Send" or "Fetch"

    var confirmNoFood by remember { mutableStateOf(false) }

    var pickupName by remember { mutableStateOf("") }

    var pickupPhone by remember { mutableStateOf("") }

    var dropName by remember { mutableStateOf("") }

    var dropPhone by remember { mutableStateOf("") }

    var currentStep by remember { mutableStateOf(1) }

    var useWallet by remember { mutableStateOf(true) }

    LaunchedEffect(activeTransfer) {

        if (activeTransfer == null) {

            currentStep = 1

            useWallet = true

        }

    }

    // Validation errors

    var itemNameError by remember { mutableStateOf("") }

    var pickPointError by remember { mutableStateOf("") }

    var dropPointError by remember { mutableStateOf("") }

    var pickupPhoneError by remember { mutableStateOf("") }

    var dropPhoneError by remember { mutableStateOf("") }

    LaunchedEffect(shouldResetForm) {

        if (shouldResetForm) {

            itemName = ""

            pickPoint = ""

            dropPoint = ""

            itemCount = 1

            itemSize = "Small"

            extraTip = 0

            customTipText = ""

            isUrgent = false

            pickupName = ""

            pickupPhone = ""

            dropName = ""

            dropPhone = ""

            itemNameError = ""

            pickPointError = ""

            dropPointError = ""

            pickupPhoneError = ""

            dropPhoneError = ""

            currentStep = 1

            useWallet = true

            confirmNoFood = false

            onFormResetCompleted()

        }

    }

    var showBalanceDialog by remember { mutableStateOf(false) }

    var showSuccessDialog by remember { mutableStateOf(false) }

    val baseFare = 20

    val itemSurcharge = (itemCount - 1) * 5

    val sizeSurcharge = when (itemSize) {

        "Medium" -> 15

        "Large" -> 40

        else -> 0

    }

    val deliveryFee = baseFare + itemSurcharge

    val appServiceFee = 10

    val urgencySurcharge = if (isUrgent) 10 else 0

    val totalPayout = deliveryFee + sizeSurcharge + appServiceFee + urgencySurcharge + extraTip

    Scaffold(

        topBar = {

            TopAppBar(

                title = {

                    Text(

                        text = "Transfer a Product",

                        fontWeight = FontWeight.Bold,

                        color = textPrimary,

                        fontSize = 18.sp

                    )

                },

                navigationIcon = {

                    IconButton(onClick = onClose) {

                        Icon(

                            imageVector = Icons.Default.ArrowBack,

                            contentDescription = "Back",

                            tint = textPrimary

                        )

                    }

                },

                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)

            )

        },

        containerColor = AppTheme.Background

    ) { paddingValues ->

        Box(

            modifier = Modifier

                .fillMaxSize()

                .padding(paddingValues)

        ) {

            if (activeTransfer == null) {

                // Request Form View

                Column(

                    modifier = Modifier

                        .fillMaxSize()

                        .verticalScroll(rememberScrollState())

                        .padding(16.dp)

                ) {

                    // Balance Badge Header

                    Row(

                        modifier = Modifier.fillMaxWidth(),

                        horizontalArrangement = Arrangement.SpaceBetween,

                        verticalAlignment = Alignment.CenterVertically

                    ) {

                        Text(

                            text = if (transferMode == "Send") "Send items around campus instantly" else "Ask someone to bring your item",

                            fontSize = 13.sp,

                            color = textSecondary,

                            fontWeight = FontWeight.Medium,

                            modifier = Modifier.weight(1f)

                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        WalletBadge(balance = walletBalance) {

                            context.startActivity(Intent(context, WalletActivity::class.java))

                        }

                    }

                     Spacer(modifier = Modifier.height(12.dp))

                    if (currentStep == 1) {

                        // Light Blue "My Orders" Button Card

                        Card(

                            onClick = onMyOrdersClick,

                            modifier = Modifier

                                .fillMaxWidth()

                                .padding(bottom = 16.dp),

                            shape = RoundedCornerShape(12.dp),

                            border = BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.4f)),

                            colors = CardDefaults.cardColors(containerColor = Color(0xFFE0F2FE))

                        ) {

                            Row(

                                modifier = Modifier.padding(12.dp).fillMaxWidth(),

                                horizontalArrangement = Arrangement.SpaceBetween,

                                verticalAlignment = Alignment.CenterVertically

                            ) {

                                Row(verticalAlignment = Alignment.CenterVertically) {

                                    Text("📦", fontSize = 18.sp)

                                    Spacer(modifier = Modifier.width(10.dp))

                                    Column {

                                        Text(

                                            text = "My Orders",

                                            fontWeight = FontWeight.Bold,

                                            fontSize = 13.sp,

                                            color = Color(0xFF0369A1)

                                        )

                                        Text(

                                            text = "Track active, past, and cancelled transfers",

                                            fontSize = 11.sp,

                                            color = Color(0xFF0369A1).copy(alpha = 0.8f)

                                        )

                                    }

                                }

                                Text(

                                    text = "View ➔",

                                    color = Color(0xFF0369A1),

                                    fontSize = 12.sp,

                                    fontWeight = FontWeight.Bold

                                )

                            }

                        }

                    }

                    FormStepIndicator(currentStep = currentStep)

                    Spacer(modifier = Modifier.height(16.dp))

                    if (currentStep == 1) {

                        // Mode Switcher Tabs

                        Row(

                            modifier = Modifier

                                .fillMaxWidth()

                                .clip(AppTheme.CardShape)

                                .background(Color(0xFFF1F5F9))

                                .padding(4.dp)

                        ) {

                            val modes = listOf("Send Item", "Bring Item")

                            modes.forEach { mode ->

                                val isSelected = (mode == "Send Item" && transferMode == "Send") || (mode == "Bring Item" && transferMode == "Fetch")

                                Box(

                                    modifier = Modifier

                                        .weight(1f)

                                        .clip(AppTheme.CardShape)

                                        .background(if (isSelected) primaryColor else Color.Transparent)

                                        .clickable {

                                            val newMode = if (mode == "Send Item") "Send" else "Fetch"

                                            if (transferMode != newMode) {

                                                transferMode = newMode

                                                itemName = ""

                                                pickPoint = ""

                                                dropPoint = ""

                                                pickupName = ""

                                                pickupPhone = ""

                                                dropName = ""

                                                dropPhone = ""

                                                itemCount = 1

                                                itemSize = "Small"

                                                extraTip = 0

                                                customTipText = ""

                                                isUrgent = false

                                                itemNameError = ""

                                                pickPointError = ""

                                                dropPointError = ""

                                                pickupPhoneError = ""

                                                dropPhoneError = ""

                                            }

                                        }

                                        .padding(vertical = 10.dp),

                                    contentAlignment = Alignment.Center

                                ) {

                                    Text(

                                        text = mode,

                                        color = if (isSelected) Color.White else textSecondary,

                                        fontWeight = FontWeight.Bold,

                                        fontSize = 13.sp

                                    )

                                }

                            }

                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Card(

                            shape = AppTheme.CardShape,

                            colors = CardDefaults.cardColors(containerColor = Color.White),

                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),

                            modifier = Modifier.fillMaxWidth()

                        ) {

                            Column(modifier = Modifier.padding(16.dp)) {

                                // Section: Item details

                                Text(

                                    text = "ITEM DETAILS",

                                    fontSize = 11.sp,

                                    fontWeight = FontWeight.Bold,

                                    color = textSecondary,

                                    letterSpacing = 1.sp

                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                OutlinedTextField(

                                    value = itemName,

                                    onValueChange = {

                                        itemName = it

                                        itemNameError = ""

                                    },

                                    label = { Text(if (transferMode == "Send") "What are you sending?" else "What item should the runner bring?") },

                                    placeholder = { Text(if (transferMode == "Send") "e.g. Charger, Lab Coat, Calculator" else "e.g. Swiggy Box, Amazon Packet, Zomato Food") },

                                    isError = itemNameError.isNotEmpty(),

                                    supportingText = {

                                        if (itemNameError.isNotEmpty()) {

                                            Text(itemNameError, color = AppTheme.Danger)

                                        }

                                    },

                                    shape = AppTheme.FieldShape,

                                    colors = OutlinedTextFieldDefaults.colors(

                                        focusedBorderColor = primaryColor,

                                        unfocusedBorderColor = AppTheme.DividerColor

                                    ),

                                    modifier = Modifier.fillMaxWidth()

                                )

                                Spacer(modifier = Modifier.height(14.dp))

                                Divider(color = AppTheme.DividerColor)

                                Spacer(modifier = Modifier.height(14.dp))

                                Row(

                                    modifier = Modifier.fillMaxWidth(),

                                    horizontalArrangement = Arrangement.SpaceBetween,

                                    verticalAlignment = Alignment.CenterVertically

                                ) {

                                    Column(modifier = Modifier.weight(1f)) {

                                        Text(

                                            text = "Number of Items / Bags",

                                            fontWeight = FontWeight.Bold,

                                            fontSize = 14.sp,

                                            color = textPrimary

                                        )

                                        Text(

                                            text = "Surcharge of +₹5 per extra item",

                                            fontSize = 11.sp,

                                            color = textSecondary

                                        )

                                    }

                                    Row(verticalAlignment = Alignment.CenterVertically) {

                                        IconButton(

                                            onClick = { if (itemCount > 1) itemCount-- },

                                            modifier = Modifier.size(32.dp).background(Color(0xFFE2E8F0), CircleShape)

                                        ) {

                                            Text("-", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = textPrimary)

                                        }

                                        Text(

                                            text = itemCount.toString(),

                                            fontWeight = FontWeight.Bold,

                                            fontSize = 16.sp,

                                            modifier = Modifier.padding(horizontal = 16.dp),

                                            color = textPrimary

                                        )

                                        IconButton(

                                            onClick = { itemCount++ },

                                            modifier = Modifier.size(32.dp).background(Color(0xFFE2E8F0), CircleShape)

                                        ) {

                                            Text("+", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = textPrimary)

                                        }

                                    }

                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                Divider(color = AppTheme.DividerColor)

                                Spacer(modifier = Modifier.height(14.dp))

                                Text(

                                    text = "Item Size",

                                    fontWeight = FontWeight.Bold,

                                    fontSize = 12.sp,

                                    color = textPrimary

                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                Row(

                                    modifier = Modifier.fillMaxWidth(),

                                    horizontalArrangement = Arrangement.spacedBy(8.dp)

                                ) {

                                    val sizes = listOf("Small", "Medium", "Large")

                                    sizes.forEach { size ->

                                        val isSelected = itemSize == size

                                        Box(

                                            modifier = Modifier

                                                .weight(1f)

                                                .clip(AppTheme.ChipShape)

                                                .background(if (isSelected) primaryColor else Color(0xFFF1F5F9))

                                                .clickable { itemSize = size }

                                                .padding(vertical = 10.dp),

                                            contentAlignment = Alignment.Center

                                        ) {

                                            Text(

                                                text = size,

                                                color = if (isSelected) Color.White else textPrimary,

                                                fontWeight = FontWeight.Bold,

                                                fontSize = 13.sp

                                            )

                                        }

                                    }

                                }

                            }

                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // ─── Policy Checkbox Card (Solution 1) ─────────────────

                        Card(

                            shape = AppTheme.CardShape,

                            border = BorderStroke(1.dp, if (confirmNoFood) AppTheme.Success.copy(alpha = 0.5f) else AppTheme.Danger.copy(alpha = 0.3f)),

                            colors = CardDefaults.cardColors(containerColor = if (confirmNoFood) Color(0xFFF0FDF4) else Color(0xFFFEF2F2)),

                            modifier = Modifier

                                .fillMaxWidth()

                                .clickable { confirmNoFood = !confirmNoFood }

                        ) {

                            Row(

                                modifier = Modifier.padding(14.dp),

                                verticalAlignment = Alignment.CenterVertically

                            ) {

                                Checkbox(

                                    checked = confirmNoFood,

                                    onCheckedChange = { confirmNoFood = it },

                                    colors = CheckboxDefaults.colors(

                                        checkedColor = AppTheme.Success,

                                        uncheckedColor = AppTheme.Danger

                                    )

                                )

                                Spacer(modifier = Modifier.width(8.dp))

                                Text(

                                    text = "I confirm that this package does NOT contain food, snacks, or drinks. I understand that if the runner cancels due to a food item violation, a non-refundable ₹20 penalty will be charged.",

                                    fontSize = 11.sp,

                                    color = if (confirmNoFood) AppTheme.InkPrimary else AppTheme.Danger,

                                    fontWeight = FontWeight.Medium,

                                    lineHeight = 15.sp

                                )

                            }

                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        Button(

                            onClick = {

                                val trimmed = itemName.trim()

                                val lowerTrimmed = trimmed.lowercase(java.util.Locale.ROOT)

                                val foodKeywords = listOf(

                                    "samosa", "burger", "pizza", "sandwich", "momo", "fries", "chips", "snack",

                                    "tea", "coffee", "chai", "cold drink", "coke", "pepsi", "sprite", "fanta",

                                    "juice", "shake", "biscuit", "cookie", "chocolate", "maggi", "noodles", "pasta",

                                    "roll", "roti", "rice", "curry", "paneer", "chicken", "biryani", "food", "eat",

                                    "drink", "soda", "lays", "kurkure"

                                )

                                val hasFood = foodKeywords.any { keyword ->

                                    lowerTrimmed.contains(keyword) || lowerTrimmed.split("\\s+".toRegex()).contains(keyword)

                                }

                                if (trimmed.isEmpty()) {

                                    itemNameError = "Item name is required"

                                } else if (!trimmed.any { it.isLetter() }) {

                                    itemNameError = "Item name must contain at least one letter"

                                } else if (hasFood) {

                                    itemNameError = "Food / snacks are not allowed in product transfer! Please use Canteen module."

                                } else {

                                    currentStep = 2

                                }

                            },

                            enabled = confirmNoFood,

                            shape = AppTheme.ButtonShape,

                            colors = ButtonDefaults.buttonColors(

                                containerColor = primaryColor,

                                disabledContainerColor = Color(0xFFCBD5E1)

                            ),

                            modifier = Modifier.fillMaxWidth().height(50.dp)

                        ) {

                            Text("Next: Delivery Route", color = if (confirmNoFood) Color.White else Color(0xFF94A3B8), fontWeight = FontWeight.Bold)

                        }

                    } else if (currentStep == 2) {

                        Card(

                            shape = AppTheme.CardShape,

                            colors = CardDefaults.cardColors(containerColor = Color.White),

                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),

                            modifier = Modifier.fillMaxWidth()

                        ) {

                            Column(modifier = Modifier.padding(16.dp)) {

                                // Section: Pickup and Dropoff Locations

                                Text(

                                    text = "DELIVERY ROUTE",

                                    fontSize = 11.sp,

                                    fontWeight = FontWeight.Bold,

                                    color = textSecondary,

                                    letterSpacing = 1.sp

                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                OutlinedTextField(

                                    value = pickPoint,

                                    onValueChange = {

                                        pickPoint = it

                                        pickPointError = ""

                                    },

                                    label = { Text(if (transferMode == "Send") "Pickup Location" else "Pickup Location (e.g. College Gate)") },

                                    placeholder = { Text(if (transferMode == "Send") "e.g. Hostel Block A, Room 304" else "e.g. Main Gate Security Post") },

                                    isError = pickPointError.isNotEmpty(),

                                    supportingText = {

                                        if (pickPointError.isNotEmpty()) {

                                            Text(pickPointError, color = AppTheme.Danger)

                                        }

                                    },

                                    shape = AppTheme.FieldShape,

                                    colors = OutlinedTextFieldDefaults.colors(

                                        focusedBorderColor = primaryColor,

                                        unfocusedBorderColor = AppTheme.DividerColor

                                    ),

                                    modifier = Modifier.fillMaxWidth()

                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                OutlinedTextField(

                                    value = dropPoint,

                                    onValueChange = {

                                        dropPoint = it

                                        dropPointError = ""

                                    },

                                    label = { Text(if (transferMode == "Send") "Drop-off Location" else "Deliver to (e.g. Hostel Room)") },

                                    placeholder = { Text(if (transferMode == "Send") "e.g. Library Desk 4, LT-3 Hall" else "e.g. Hostel Block A, Room 304") },

                                    isError = dropPointError.isNotEmpty(),

                                    supportingText = {

                                        if (dropPointError.isNotEmpty()) {

                                            Text(dropPointError, color = AppTheme.Danger)

                                        }

                                    },

                                    shape = AppTheme.FieldShape,

                                    colors = OutlinedTextFieldDefaults.colors(

                                        focusedBorderColor = primaryColor,

                                        unfocusedBorderColor = AppTheme.DividerColor

                                    ),

                                    modifier = Modifier.fillMaxWidth()

                                )

                            }

                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        Row(

                            modifier = Modifier.fillMaxWidth(),

                            horizontalArrangement = Arrangement.spacedBy(12.dp)

                        ) {

                            OutlinedButton(

                                onClick = { currentStep = 1 },

                                shape = AppTheme.ButtonShape,

                                modifier = Modifier.weight(1f).height(50.dp),

                                border = BorderStroke(1.5.dp, primaryColor)

                            ) {

                                Text("Back", color = primaryColor, fontWeight = FontWeight.Bold)

                            }

                            Button(

                                onClick = {

                                    var hasError = false

                                    if (pickPoint.trim().isEmpty()) {

                                        pickPointError = "Pickup location is required"

                                        hasError = true

                                    }

                                    if (dropPoint.trim().isEmpty()) {

                                        dropPointError = "Drop-off location is required"

                                        hasError = true

                                    }

                                    if (!hasError) {

                                        currentStep = 3

                                    }

                                },

                                shape = AppTheme.ButtonShape,

                                colors = ButtonDefaults.buttonColors(containerColor = primaryColor),

                                modifier = Modifier.weight(1.5f).height(50.dp)

                            ) {

                                Text("Next: Contacts & Payout", color = Color.White, fontWeight = FontWeight.Bold)

                            }

                        }

                    } else if (currentStep == 3) {

                        Card(

                            shape = AppTheme.CardShape,

                            colors = CardDefaults.cardColors(containerColor = Color.White),

                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),

                            modifier = Modifier.fillMaxWidth()

                        ) {

                            Column(modifier = Modifier.padding(16.dp)) {

                                Text(

                                    text = "CONTACT DETAILS",

                                    fontSize = 11.sp,

                                    fontWeight = FontWeight.Bold,

                                    color = textSecondary,

                                    letterSpacing = 1.sp

                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                // PICKUP CONTACT

                                Text(

                                    text = "Pickup Point Contact",

                                    fontWeight = FontWeight.Bold,

                                    fontSize = 13.sp,

                                    color = textPrimary

                                )

                                Spacer(modifier = Modifier.height(6.dp))

                                OutlinedTextField(

                                     value = pickupName,

                                     onValueChange = { input ->

                                         pickupName = input.filter { c -> c.isLetter() || c.isWhitespace() }

                                     },

                                     label = { Text("Name (optional)") },

                                     placeholder = { Text("e.g. Yash Patel (or Self)") },

                                     shape = AppTheme.FieldShape,

                                     colors = OutlinedTextFieldDefaults.colors(

                                         focusedBorderColor = primaryColor,

                                         unfocusedBorderColor = AppTheme.DividerColor

                                     ),

                                     modifier = Modifier.fillMaxWidth()

                                 )

                                 Spacer(modifier = Modifier.height(8.dp))

                                 OutlinedTextField(

                                     value = pickupPhone,

                                     onValueChange = { input ->

                                         val digits = input.filter { c -> c.isDigit() }

                                         if (digits.length <= 10) {

                                             pickupPhone = digits

                                             pickupPhoneError = ""

                                         }

                                     },

                                     label = { Text("Phone Number") },

                                     isError = pickupPhoneError.isNotEmpty(),

                                     supportingText = {

                                         if (pickupPhoneError.isNotEmpty()) {

                                             Text(pickupPhoneError, color = AppTheme.Danger)

                                         } else {

                                             Text("Who should the runner call at the pickup point?", color = textSecondary, fontSize = 11.sp)

                                         }

                                     },

                                     keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),

                                     shape = AppTheme.FieldShape,

                                     colors = OutlinedTextFieldDefaults.colors(

                                         focusedBorderColor = primaryColor,

                                         unfocusedBorderColor = AppTheme.DividerColor

                                     ),

                                     modifier = Modifier.fillMaxWidth()

                                 )

                                 Spacer(modifier = Modifier.height(16.dp))

                                 Divider(color = AppTheme.DividerColor)

                                 Spacer(modifier = Modifier.height(16.dp))

                                 // DROP-OFF CONTACT

                                 Text(

                                     text = "Drop-off Point Contact",

                                     fontWeight = FontWeight.Bold,

                                     fontSize = 13.sp,

                                     color = textPrimary

                                 )

                                 Spacer(modifier = Modifier.height(6.dp))

                                 OutlinedTextField(

                                     value = dropName,

                                     onValueChange = { input ->

                                         dropName = input.filter { c -> c.isLetter() || c.isWhitespace() }

                                     },

                                     label = { Text("Name (optional)") },

                                     placeholder = { Text("e.g. Hosteller Roommate") },

                                     shape = AppTheme.FieldShape,

                                     colors = OutlinedTextFieldDefaults.colors(

                                         focusedBorderColor = primaryColor,

                                         unfocusedBorderColor = AppTheme.DividerColor

                                     ),

                                     modifier = Modifier.fillMaxWidth()

                                 )

                                 Spacer(modifier = Modifier.height(8.dp))

                                 OutlinedTextField(

                                     value = dropPhone,

                                     onValueChange = { input ->

                                         val digits = input.filter { c -> c.isDigit() }

                                         if (digits.length <= 10) {

                                             dropPhone = digits

                                             dropPhoneError = ""

                                         }

                                     },

                                     label = { Text("Phone Number") },

                                     isError = dropPhoneError.isNotEmpty(),

                                     supportingText = {

                                         if (dropPhoneError.isNotEmpty()) {

                                             Text(dropPhoneError, color = AppTheme.Danger)

                                         } else {

                                             Text("Who should the runner call at the drop-off point?", color = textSecondary, fontSize = 11.sp)

                                         }

                                     },

                                     keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),

                                     shape = AppTheme.FieldShape,

                                     colors = OutlinedTextFieldDefaults.colors(

                                         focusedBorderColor = primaryColor,

                                         unfocusedBorderColor = AppTheme.DividerColor

                                     ),

                                     modifier = Modifier.fillMaxWidth()

                                )

                            }

                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // CARD 3.5: App Wallet / UPI payment choice Card

                        Card(

                            modifier = Modifier

                                .fillMaxWidth()

                                .clickable(enabled = walletBalance > 0) {

                                    useWallet = !useWallet

                                },

                            shape = AppTheme.CardShape,

                            border = BorderStroke(1.dp, AppTheme.DividerColor),

                            colors = CardDefaults.cardColors(containerColor = Color.White)

                        ) {

                            Column(modifier = Modifier.padding(16.dp)) {

                                Text(

                                    text = "PAYMENT METHOD",

                                    fontSize = 11.sp,

                                    fontWeight = FontWeight.Bold,

                                    color = textSecondary,

                                    letterSpacing = 1.sp

                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                Row(

                                    verticalAlignment = Alignment.CenterVertically,

                                    modifier = Modifier.fillMaxWidth()

                                ) {

                                    Text("💵", fontSize = 20.sp)

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Column(modifier = Modifier.weight(1f)) {

                                        Text("Use Wallet Balance", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = textPrimary)

                                        Text("Available: ₹$walletBalance", fontSize = 11.sp, color = AppTheme.Success, fontWeight = FontWeight.Bold)

                                    }

                                    Switch(

                                        checked = useWallet,

                                        onCheckedChange = { useWallet = it },

                                        enabled = walletBalance > 0,

                                        colors = SwitchDefaults.colors(

                                            checkedThumbColor = Color.White,

                                            checkedTrackColor = AppTheme.Success

                                        )

                                    )

                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                Divider(color = AppTheme.DividerColor)

                                Spacer(modifier = Modifier.height(12.dp))

                                val upiAmount = if (useWallet) maxOf(0, totalPayout - walletBalance) else totalPayout

                                val walletDeduction = if (useWallet) minOf(walletBalance, totalPayout) else 0

                                Text(

                                    text = if (upiAmount > 0) {

                                        if (walletDeduction > 0) {

                                            "Pay ₹$walletDeduction via Wallet + ₹$upiAmount via Google Pay/UPI 📱"

                                        } else {

                                            "Pay ₹$upiAmount via Google Pay/UPI 📱"

                                        }

                                    } else {

                                        "Pay ₹$walletDeduction entirely via Wallet ✅"

                                    },

                                    fontSize = 12.sp,

                                    fontWeight = FontWeight.Bold,

                                    color = primaryColor

                                )

                            }

                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Card(

                            shape = AppTheme.CardShape,

                            colors = CardDefaults.cardColors(containerColor = Color.White),

                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),

                            modifier = Modifier.fillMaxWidth()

                        ) {

                            Column(modifier = Modifier.padding(16.dp)) {

                                // Section: Pricing Breakdown

                                Text(

                                    text = "FARE BREAKDOWN",

                                    fontSize = 11.sp,

                                    fontWeight = FontWeight.Bold,

                                    color = textSecondary,

                                    letterSpacing = 1.sp

                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                // Breakdown items

                                Row(

                                    modifier = Modifier.fillMaxWidth(),

                                    horizontalArrangement = Arrangement.SpaceBetween

                                ) {

                                    Text(

                                        text = if (itemCount > 1) "Delivery Fee (with ${itemCount - 1} extra items)" else "Delivery Fee",

                                        color = textSecondary,

                                        fontSize = 13.sp

                                    )

                                    Text("₹$deliveryFee", color = textPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)

                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Row(

                                    modifier = Modifier.fillMaxWidth(),

                                    horizontalArrangement = Arrangement.SpaceBetween

                                ) {

                                    Text("App Service Fee", color = textSecondary, fontSize = 13.sp)

                                    Text("₹$appServiceFee", color = textPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)

                                }

                                if (sizeSurcharge > 0) {

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Row(

                                        modifier = Modifier.fillMaxWidth(),

                                        horizontalArrangement = Arrangement.SpaceBetween

                                    ) {

                                        Text("Bulk Size Surcharge ($itemSize)", color = textSecondary, fontSize = 13.sp)

                                        Text("+₹$sizeSurcharge", color = textPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)

                                    }

                                }

                                if (extraTip > 0) {

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Row(

                                        modifier = Modifier.fillMaxWidth(),

                                        horizontalArrangement = Arrangement.SpaceBetween

                                    ) {

                                        Text("Runner Tip", color = textSecondary, fontSize = 13.sp)

                                        Text("+₹$extraTip", color = AppTheme.Success, fontWeight = FontWeight.Bold, fontSize = 13.sp)

                                    }

                                }

                                if (isUrgent) {

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Row(

                                        modifier = Modifier.fillMaxWidth(),

                                        horizontalArrangement = Arrangement.SpaceBetween

                                    ) {

                                        Text("Urgent Surcharge", color = textSecondary, fontSize = 13.sp)

                                        Text("+₹10", color = AppTheme.Danger, fontWeight = FontWeight.Bold, fontSize = 13.sp)

                                    }

                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                Divider(color = AppTheme.DividerColor)

                                Spacer(modifier = Modifier.height(10.dp))

                                Row(

                                    modifier = Modifier.fillMaxWidth(),

                                    horizontalArrangement = Arrangement.SpaceBetween,

                                    verticalAlignment = Alignment.CenterVertically

                                ) {

                                    Text("Total Payout to Runner", fontWeight = FontWeight.Bold, color = textPrimary, fontSize = 14.sp)

                                    Text("₹$totalPayout", fontWeight = FontWeight.ExtraBold, color = primaryColor, fontSize = 18.sp)

                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                Divider(color = AppTheme.DividerColor)

                                Spacer(modifier = Modifier.height(16.dp))

                                // Tipping Row

                                Text(

                                    text = "Add Runner Tip (optional)",

                                    fontWeight = FontWeight.Bold,

                                    fontSize = 12.sp,

                                    color = textPrimary

                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                Row(

                                    modifier = Modifier.fillMaxWidth(),

                                    horizontalArrangement = Arrangement.spacedBy(8.dp)

                                ) {

                                    val tips = listOf(0, 10, 20, -1)

                                    tips.forEach { tip ->

                                        val isSelected = (tip >= 0 && extraTip == tip && customTipText.isEmpty()) || (tip == -1 && customTipText.isNotEmpty())

                                        val label = when (tip) {

                                            0 -> "None"

                                            -1 -> "Custom"

                                            else -> "+₹$tip"

                                        }

                                        Box(

                                            modifier = Modifier

                                                .weight(1f)

                                                .clip(AppTheme.ChipShape)

                                                .background(if (isSelected) AppTheme.Success else Color(0xFFF1F5F9))

                                                .clickable {

                                                    if (tip >= 0) {

                                                        extraTip = tip

                                                        customTipText = ""

                                                    } else {

                                                        extraTip = 0

                                                        customTipText = "15"

                                                        extraTip = 15

                                                    }

                                                }

                                                .padding(vertical = 8.dp),

                                            contentAlignment = Alignment.Center

                                        ) {

                                            Text(

                                                text = label,

                                                color = if (isSelected) Color.White else textSecondary,

                                                fontWeight = FontWeight.Bold,

                                                fontSize = 12.sp

                                            )

                                        }

                                    }

                                }

                                if (customTipText.isNotEmpty()) {

                                    Spacer(modifier = Modifier.height(12.dp))

                                    OutlinedTextField(

                                        value = customTipText,

                                        onValueChange = { input ->

                                            val filtered = input.filter { it.isDigit() }

                                            customTipText = filtered

                                            extraTip = filtered.toIntOrNull() ?: 0

                                        },

                                        label = { Text("Enter Custom Tip Amount (₹)") },

                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),

                                        shape = AppTheme.FieldShape,

                                        colors = OutlinedTextFieldDefaults.colors(

                                            focusedBorderColor = primaryColor,

                                            unfocusedBorderColor = AppTheme.DividerColor

                                        ),

                                        modifier = Modifier.fillMaxWidth()

                                    )

                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                // Urgency Row

                                Row(

                                    modifier = Modifier.fillMaxWidth(),

                                    horizontalArrangement = Arrangement.SpaceBetween,

                                    verticalAlignment = Alignment.CenterVertically

                                ) {

                                    Column {

                                        Text(

                                            text = "Urgent Delivery",

                                            fontWeight = FontWeight.Bold,

                                            fontSize = 14.sp,

                                            color = textPrimary

                                        )

                                        Text(

                                            text = "Flashing alert tag for faster pickup",

                                            fontSize = 11.sp,

                                            color = textSecondary

                                        )

                                    }

                                    Switch(

                                        checked = isUrgent,

                                        onCheckedChange = { isUrgent = it },

                                        colors = SwitchDefaults.colors(

                                            checkedThumbColor = Color.White,

                                            checkedTrackColor = AppTheme.Accent,

                                            uncheckedThumbColor = Color.White,

                                        )

                                    )

                                }

                            }

                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Spacer(modifier = Modifier.height(24.dp))

                        Row(

                            modifier = Modifier.fillMaxWidth(),

                            horizontalArrangement = Arrangement.spacedBy(12.dp)

                        ) {

                            OutlinedButton(

                                onClick = { currentStep = 2 },

                                shape = AppTheme.ButtonShape,

                                modifier = Modifier.weight(1f).height(52.dp),

                                border = BorderStroke(1.5.dp, primaryColor)

                            ) {

                                Text("Back", color = primaryColor, fontWeight = FontWeight.Bold)

                            }

                            Button(

                                onClick = {

                                    var hasError = false

                                    if (pickupPhone.trim().isEmpty()) {

                                        pickupPhoneError = "Pickup phone number is required"

                                        hasError = true

                                    } else if (pickupPhone.trim().length != 10) {

                                        pickupPhoneError = "Pickup phone must be exactly 10 digits"

                                        hasError = true

                                    }

                                    if (dropPhone.trim().isEmpty()) {

                                        dropPhoneError = "Drop-off phone number is required"

                                        hasError = true

                                    } else if (dropPhone.trim().length != 10) {

                                        dropPhoneError = "Drop-off phone must be exactly 10 digits"

                                        hasError = true

                                    }

                                    if (!hasError) {

                                        val sItem = SecurityUtils.sanitizeInput(itemName.trim())

                                        val sPick = SecurityUtils.sanitizeInput(pickPoint.trim())

                                        val sDrop = SecurityUtils.sanitizeInput(dropPoint.trim())

                                        if (SecurityUtils.containsInjectionPatterns(sItem) ||

                                            SecurityUtils.containsInjectionPatterns(sPick) ||

                                            SecurityUtils.containsInjectionPatterns(sDrop)) {

                                             Toast.makeText(context, "Security warning: Malicious script detected!", Toast.LENGTH_LONG).show()

                                        } else {

                                            onBookOrder(

                                                if (transferMode == "Fetch") "Bring: $sItem" else "Send: $sItem",

                                                sPick,

                                                sDrop,

                                                itemCount,

                                                itemSize,

                                                extraTip,

                                                isUrgent,

                                                pickupName,

                                                pickupPhone,

                                                dropName,

                                                dropPhone,

                                                totalPayout,

                                                useWallet

                                            )

                                        }

                                    }

                                },

                                shape = AppTheme.ButtonShape,

                                colors = ButtonDefaults.buttonColors(containerColor = primaryColor),

                                modifier = Modifier.weight(2f).height(52.dp)

                            ) {

                                Text("Book Order", color = Color.White, fontWeight = FontWeight.Bold)

                            }

                        }

                    }

                    Spacer(modifier = Modifier.height(20.dp))

                }

            } else {

                // Live Tracker View — Premium Redesigned UI

                var enteredOtp by remember { mutableStateOf("") }

                var otpError by remember { mutableStateOf(false) }

                val orderNumber = activeTransfer.id.replace("OrderNo-", "").replace("CSV-", "")

                // Infinite pulse animation for active step

                val infiniteTransition = rememberInfiniteTransition(label = "pulse")

                val pulseAlpha by infiniteTransition.animateFloat(

                    initialValue = 0.4f,

                    targetValue = 1f,

                    animationSpec = infiniteRepeatable(

                        animation = tween(800, easing = FastOutSlowInEasing),

                        repeatMode = RepeatMode.Reverse

                    ),

                    label = "pulseAlpha"

                )

                Column(

                    modifier = Modifier

                        .fillMaxSize()

                        .verticalScroll(rememberScrollState())

                ) {

                    // ─── Gradient Header Card ────────────────────────────────

                    Box(

                        modifier = Modifier

                            .fillMaxWidth()

                            .background(

                                brush = Brush.verticalGradient(

                                    colors = listOf(

                                        primaryColor,

                                        primaryColor.copy(alpha = 0.85f)

                                    )

                                )

                            )

                            .padding(horizontal = 20.dp, vertical = 24.dp)

                    ) {

                        Column {

                            Row(

                                modifier = Modifier.fillMaxWidth(),

                                horizontalArrangement = Arrangement.SpaceBetween,

                                verticalAlignment = Alignment.CenterVertically

                            ) {

                                Column {

                                    Text(

                                        text = "📦 Live Tracking",

                                        fontWeight = FontWeight.Bold,

                                        fontSize = 20.sp,

                                        color = Color.White

                                    )

                                    Spacer(modifier = Modifier.height(4.dp))

                                    Text(

                                        text = "Order No: $orderNumber",

                                        fontSize = 13.sp,

                                        color = Color.White.copy(alpha = 0.8f),

                                        fontWeight = FontWeight.Medium

                                    )

                                }

                                // Status badge

                                val statusText = when (deliveryStatus) {

                                    "searching" -> "Searching"

                                    "accepted" -> "Accepted"

                                    "in_transit" -> "In Transit"

                                    "arrived" -> "Arrived"

                                    "completed" -> "Delivered"

                                    else -> "Pending"

                                }

                                val statusEmoji = when (deliveryStatus) {

                                    "searching" -> "🔍"

                                    "accepted" -> "✅"

                                    "in_transit" -> "🚀"

                                    "arrived" -> "📍"

                                    "completed" -> "🎉"

                                    else -> "⏳"

                                }

                                Box(

                                    modifier = Modifier

                                        .clip(RoundedCornerShape(20.dp))

                                        .background(Color.White.copy(alpha = 0.2f))

                                        .padding(horizontal = 14.dp, vertical = 6.dp)

                                ) {

                                    Text(

                                        text = "$statusEmoji $statusText",

                                        color = Color.White,

                                        fontWeight = FontWeight.Bold,

                                        fontSize = 12.sp

                                    )

                                }

                            }

                            Spacer(modifier = Modifier.height(20.dp))

                            // Order summary row chips

                            Row(

                                modifier = Modifier

                                    .fillMaxWidth()

                                    .clip(RoundedCornerShape(12.dp))

                                    .background(Color.White.copy(alpha = 0.15f))

                                    .padding(12.dp),

                                horizontalArrangement = Arrangement.SpaceBetween

                            ) {

                                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {

                                    Text("📦", fontSize = 16.sp)

                                    Spacer(modifier = Modifier.height(4.dp))

                                    Text(

                                        text = if (activeTransfer.itemName.length > 12) activeTransfer.itemName.take(12) + "…" else activeTransfer.itemName,

                                        color = Color.White,

                                        fontSize = 11.sp,

                                        fontWeight = FontWeight.SemiBold,

                                        textAlign = TextAlign.Center,

                                        maxLines = 1

                                    )

                                    Text("Item", color = Color.White.copy(alpha = 0.6f), fontSize = 9.sp)

                                }

                                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {

                                    Text("💰", fontSize = 16.sp)

                                    Spacer(modifier = Modifier.height(4.dp))

                                    Text("₹${activeTransfer.fee}", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)

                                    Text("Price", color = Color.White.copy(alpha = 0.6f), fontSize = 9.sp)

                                }

                                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {

                                    Text("📍", fontSize = 16.sp)

                                    Spacer(modifier = Modifier.height(4.dp))

                                    Text(

                                        text = if (activeTransfer.pickPoint.length > 12) activeTransfer.pickPoint.take(12) + "…" else activeTransfer.pickPoint,

                                        color = Color.White,

                                        fontSize = 11.sp,

                                        fontWeight = FontWeight.SemiBold,

                                        textAlign = TextAlign.Center,

                                        maxLines = 1

                                    )

                                    Text("From", color = Color.White.copy(alpha = 0.6f), fontSize = 9.sp)

                                }

                                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {

                                    Text("🏁", fontSize = 16.sp)

                                    Spacer(modifier = Modifier.height(4.dp))

                                    Text(

                                        text = if (activeTransfer.dropPoint.length > 12) activeTransfer.dropPoint.take(12) + "…" else activeTransfer.dropPoint,

                                        color = Color.White,

                                        fontSize = 11.sp,

                                        fontWeight = FontWeight.SemiBold,

                                        textAlign = TextAlign.Center,

                                        maxLines = 1

                                    )

                                    Text("To", color = Color.White.copy(alpha = 0.6f), fontSize = 9.sp)

                                }

                            }

                        }

                    }

                    // ─── Content Area ────────────────────────────────────────

                    Column(

                        modifier = Modifier

                            .fillMaxWidth()

                            .padding(16.dp)

                    ) {

                        // ─── Transfer Details Card ───────────────────────────

                        Card(

                            shape = RoundedCornerShape(16.dp),

                            colors = CardDefaults.cardColors(containerColor = Color.White),

                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),

                            modifier = Modifier.fillMaxWidth()

                        ) {

                            Column(modifier = Modifier.padding(20.dp)) {

                                Text(

                                    text = "Transfer Details",

                                    fontWeight = FontWeight.Bold,

                                    fontSize = 16.sp,

                                    color = textPrimary

                                )

                                Spacer(modifier = Modifier.height(16.dp))

                                val detailItems = listOf(

                                    Triple("Order No", orderNumber, textPrimary),

                                    Triple("Item Name", activeTransfer.itemName, textPrimary),

                                    Triple("Pickup Location", activeTransfer.pickPoint, textPrimary),

                                    Triple("Drop-off Location", activeTransfer.dropPoint, textPrimary),

                                    Triple("Price", "₹${activeTransfer.fee}", AppTheme.Success)

                                )

                                detailItems.forEachIndexed { index, (label, value, color) ->

                                    Row(

                                        modifier = Modifier.fillMaxWidth(),

                                        horizontalArrangement = Arrangement.SpaceBetween,

                                        verticalAlignment = Alignment.CenterVertically

                                    ) {

                                        Text(

                                            text = label,

                                            fontSize = 13.sp,

                                            color = textSecondary,

                                            fontWeight = FontWeight.Medium

                                        )

                                        Text(

                                            text = value,

                                            fontSize = 13.sp,

                                            color = color,

                                            fontWeight = FontWeight.Bold,

                                            textAlign = TextAlign.End,

                                            modifier = Modifier.weight(1f).padding(start = 16.dp)

                                        )

                                    }

                                    if (index < detailItems.lastIndex) {

                                        Spacer(modifier = Modifier.height(10.dp))

                                        Divider(color = AppTheme.DividerColor.copy(alpha = 0.5f))

                                        Spacer(modifier = Modifier.height(10.dp))

                                    }

                                }

                            }

                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // ─── Vertical Timeline Card ──────────────────────────

                        Card(

                            shape = RoundedCornerShape(16.dp),

                            colors = CardDefaults.cardColors(containerColor = Color.White),

                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),

                            modifier = Modifier.fillMaxWidth()

                        ) {

                            Column(modifier = Modifier.padding(20.dp)) {

                                Text(

                                    text = "Delivery Progress",

                                    fontWeight = FontWeight.Bold,

                                    fontSize = 16.sp,

                                    color = textPrimary

                                )

                                Spacer(modifier = Modifier.height(20.dp))

                                // Timeline steps data

                                data class TimelineStep(val emoji: String, val title: String, val subtitle: String)

                                val steps = listOf(

                                    TimelineStep("🔍", "Searching for runner", "Broadcasting to campus runners…"),

                                    TimelineStep("🏃", if (runnerName.isNotEmpty()) "Accepted by $runnerName" else "Runner accepts job", "Runner is heading to the pickup spot"),

                                    TimelineStep("🚀", "Item in transit", if (runnerName.isNotEmpty()) "$runnerName is carrying your item" else "Runner is on the way"),

                                    TimelineStep("📍", "Runner arrived", "Runner is at the drop-off point")

                                )

                                val statusIndex = when (deliveryStatus) {

                                    "searching" -> 0

                                    "accepted" -> 1

                                    "in_transit" -> 2

                                    "arrived" -> 3

                                    "completed" -> 4

                                    else -> 0

                                }

                                steps.forEachIndexed { index, step ->

                                    val isStepCompleted = index < statusIndex

                                    val isStepActive = index == statusIndex && deliveryStatus != "completed"

                                    val isStepPending = index > statusIndex

                                    val isLast = index == steps.lastIndex

                                    Row(modifier = Modifier.fillMaxWidth()) {

                                        // ── Timeline column (circle + line) ──

                                        Column(

                                            horizontalAlignment = Alignment.CenterHorizontally,

                                            modifier = Modifier.width(32.dp)

                                        ) {

                                            // Circle

                                            Box(

                                                modifier = Modifier

                                                    .size(28.dp)

                                                    .clip(CircleShape)

                                                    .background(

                                                        when {

                                                            isStepCompleted -> AppTheme.Success

                                                            isStepActive -> primaryColor.copy(alpha = pulseAlpha)

                                                            else -> Color(0xFFE2E8F0)

                                                        }

                                                    ),

                                                contentAlignment = Alignment.Center

                                            ) {

                                                if (isStepCompleted) {

                                                    Text("✓", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)

                                                } else {

                                                    Text(step.emoji, fontSize = 12.sp)

                                                }

                                            }

                                            // Connecting line

                                            if (!isLast) {

                                                Box(

                                                    modifier = Modifier

                                                        .width(2.5.dp)

                                                        .height(48.dp)

                                                        .background(

                                                            if (isStepCompleted) AppTheme.Success.copy(alpha = 0.5f) 

                                                            else Color(0xFFE2E8F0)

                                                        )

                                                )

                                            }

                                        }

                                        Spacer(modifier = Modifier.width(14.dp))

                                        // ── Content column ──

                                        Column(

                                            modifier = Modifier

                                                .weight(1f)

                                                .padding(bottom = if (!isLast) 24.dp else 0.dp)

                                        ) {

                                            Text(

                                                text = step.title,

                                                fontSize = 14.sp,

                                                fontWeight = if (isStepActive || isStepCompleted) FontWeight.Bold else FontWeight.Normal,

                                                color = when {

                                                    isStepCompleted -> AppTheme.Success

                                                    isStepActive -> textPrimary

                                                    else -> textSecondary.copy(alpha = 0.5f)

                                                }

                                            )

                                            if (isStepActive) {

                                                Spacer(modifier = Modifier.height(2.dp))

                                                Text(

                                                    text = step.subtitle,

                                                    fontSize = 11.sp,

                                                    color = textSecondary

                                                )

                                            }

                                            if (isStepCompleted) {

                                                Spacer(modifier = Modifier.height(2.dp))

                                                Text(

                                                    text = "Completed ✓",

                                                    fontSize = 10.sp,

                                                    color = AppTheme.Success,

                                                    fontWeight = FontWeight.Medium

                                                )

                                            }

                                        }

                                    }

                                }

                                // Completed state – final row

                                if (deliveryStatus == "completed") {

                                    Spacer(modifier = Modifier.height(12.dp))

                                    Box(

                                        modifier = Modifier

                                            .fillMaxWidth()

                                            .clip(RoundedCornerShape(12.dp))

                                            .background(AppTheme.SuccessSoft)

                                            .padding(12.dp),

                                        contentAlignment = Alignment.Center

                                    ) {

                                        Text(

                                            text = "🎉 All steps completed! Package delivered.",

                                            fontWeight = FontWeight.Bold,

                                            fontSize = 13.sp,

                                            color = AppTheme.Success

                                        )

                                    }

                                }

                            }

                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // ─── OTP Card (when runner arrived) ──────────────────

                        if (deliveryStatus == "arrived") {

                            Card(

                                shape = RoundedCornerShape(16.dp),

                                colors = CardDefaults.cardColors(containerColor = Color.White),

                                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),

                                modifier = Modifier.fillMaxWidth()

                            ) {

                                Column(

                                    modifier = Modifier

                                        .fillMaxWidth()

                                        .padding(24.dp),

                                    horizontalAlignment = Alignment.CenterHorizontally

                                ) {

                                    Box(

                                        modifier = Modifier

                                            .size(56.dp)

                                            .clip(CircleShape)

                                            .background(primaryColor.copy(alpha = 0.1f)),

                                        contentAlignment = Alignment.Center

                                    ) {

                                        Text("🔐", fontSize = 28.sp)

                                    }

                                    Spacer(modifier = Modifier.height(16.dp))

                                    Text(

                                        text = "Verification OTP",

                                        fontWeight = FontWeight.Bold,

                                        fontSize = 16.sp,

                                        color = textPrimary

                                    )

                                    Spacer(modifier = Modifier.height(4.dp))

                                    Text(

                                        text = "Share this code with $runnerName",

                                        fontSize = 12.sp,

                                        color = textSecondary,

                                        textAlign = TextAlign.Center

                                    )

                                    Spacer(modifier = Modifier.height(16.dp))

                                    // OTP digits in separate boxes

                                    Row(

                                        horizontalArrangement = Arrangement.spacedBy(10.dp),

                                        verticalAlignment = Alignment.CenterVertically

                                    ) {

                                        activeTransfer.otp.forEach { digit ->

                                            Box(

                                                modifier = Modifier

                                                    .size(52.dp)

                                                    .clip(RoundedCornerShape(12.dp))

                                                    .background(

                                                        Brush.verticalGradient(

                                                            listOf(primaryColor.copy(alpha = 0.08f), primaryColor.copy(alpha = 0.15f))

                                                        )

                                                    )

                                                    .border(1.5.dp, primaryColor.copy(alpha = 0.3f), RoundedCornerShape(12.dp)),

                                                contentAlignment = Alignment.Center

                                            ) {

                                                Text(

                                                    text = digit.toString(),

                                                    fontSize = 22.sp,

                                                    fontWeight = FontWeight.ExtraBold,

                                                    color = primaryColor

                                                )

                                            }

                                        }

                                    }

                                    Spacer(modifier = Modifier.height(16.dp))

                                    Text(

                                        text = "This OTP ensures secure delivery verification.",

                                        fontSize = 10.sp,

                                        color = textSecondary,

                                        textAlign = TextAlign.Center

                                    )

                                }

                            }

                        } else if (deliveryStatus == "completed") {

                            // ─── Completion Card ─────────────────────────────

                            Card(

                                shape = RoundedCornerShape(16.dp),

                                colors = CardDefaults.cardColors(containerColor = Color.White),

                                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),

                                modifier = Modifier.fillMaxWidth()

                            ) {

                                Column(

                                    modifier = Modifier

                                        .fillMaxWidth()

                                        .padding(24.dp),

                                    horizontalAlignment = Alignment.CenterHorizontally

                                ) {

                                    Box(

                                        modifier = Modifier

                                            .size(64.dp)

                                            .clip(CircleShape)

                                            .background(AppTheme.Success.copy(alpha = 0.12f)),

                                        contentAlignment = Alignment.Center

                                    ) {

                                        Text("🎉", fontSize = 32.sp)

                                    }

                                    Spacer(modifier = Modifier.height(16.dp))

                                    Text(

                                        text = "Transfer Completed!",

                                        fontWeight = FontWeight.Bold,

                                        fontSize = 18.sp,

                                        color = AppTheme.Success

                                    )

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Text(

                                        text = "$runnerName has successfully delivered your package and received the ₹${activeTransfer.fee} payout.",

                                        fontSize = 13.sp,

                                        color = textSecondary,

                                        textAlign = TextAlign.Center,

                                        lineHeight = 18.sp

                                    )

                                    Spacer(modifier = Modifier.height(20.dp))

                                    Button(

                                        onClick = onClose,

                                        shape = RoundedCornerShape(12.dp),

                                        colors = ButtonDefaults.buttonColors(containerColor = primaryColor),

                                        modifier = Modifier.fillMaxWidth().height(50.dp)

                                    ) {

                                        Text("Return to Dashboard", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)

                                    }

                                }

                            }

                        } else {

                            // ─── Runner Info Card ────────────────────────────

                            if (deliveryStatus == "accepted" || deliveryStatus == "in_transit") {

                                Card(

                                    shape = RoundedCornerShape(16.dp),

                                    colors = CardDefaults.cardColors(containerColor = Color.White),

                                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),

                                    modifier = Modifier.fillMaxWidth()

                                ) {

                                    Row(

                                        modifier = Modifier

                                            .fillMaxWidth()

                                            .padding(16.dp),

                                        verticalAlignment = Alignment.CenterVertically

                                    ) {

                                        Box(

                                            modifier = Modifier

                                                .size(44.dp)

                                                .clip(CircleShape)

                                                .background(primaryColor.copy(alpha = 0.1f)),

                                            contentAlignment = Alignment.Center

                                        ) {

                                            Text("🏃", fontSize = 22.sp)

                                        }

                                        Spacer(modifier = Modifier.width(14.dp))

                                        Column(modifier = Modifier.weight(1f)) {

                                            Text(

                                                text = runnerName,

                                                fontWeight = FontWeight.Bold,

                                                fontSize = 15.sp,

                                                color = textPrimary

                                            )

                                            Text(

                                                text = if (deliveryStatus == "accepted") "Heading to pickup spot" else "Carrying your item",

                                                fontSize = 12.sp,

                                                color = textSecondary

                                            )

                                        }

                                        Box(

                                            modifier = Modifier

                                                .clip(RoundedCornerShape(20.dp))

                                                .background(AppTheme.PrimarySoft)

                                                .padding(horizontal = 12.dp, vertical = 6.dp)

                                        ) {

                                            Text(

                                                text = "Runner",

                                                color = primaryColor,

                                                fontWeight = FontWeight.Bold,

                                                fontSize = 11.sp

                                            )

                                        }

                                    }

                                }

                                Spacer(modifier = Modifier.height(16.dp))

                            }

                            // ─── Status Activity Card ────────────────────────

                            Card(

                                shape = RoundedCornerShape(16.dp),

                                colors = CardDefaults.cardColors(containerColor = Color.White),

                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),

                                modifier = Modifier.fillMaxWidth()

                            ) {

                                Column(

                                    modifier = Modifier.padding(24.dp),

                                    horizontalAlignment = Alignment.CenterHorizontally

                                ) {

                                    if (deliveryStatus == "searching") {

                                        val infiniteTransition = rememberInfiniteTransition(label = "pulse")

                                        val pulseScale1 by infiniteTransition.animateFloat(

                                            initialValue = 0.6f,

                                            targetValue = 1.4f,

                                            animationSpec = infiniteRepeatable(

                                                animation = tween(1200, easing = LinearEasing),

                                                repeatMode = RepeatMode.Restart

                                            ),

                                            label = "scale1"

                                        )

                                        val pulseAlpha1 by infiniteTransition.animateFloat(

                                            initialValue = 0.8f,

                                            targetValue = 0f,

                                            animationSpec = infiniteRepeatable(

                                                animation = tween(1200, easing = LinearEasing),

                                                repeatMode = RepeatMode.Restart

                                            ),

                                            label = "alpha1"

                                        )

                                        Box(

                                            modifier = Modifier.size(200.dp),

                                            contentAlignment = Alignment.Center

                                        ) {

                                            Box(

                                                modifier = Modifier

                                                    .size(150.dp)

                                                    .rotate(pulseScale1 * 360f)

                                                    .border(2.dp, Color(0xFF0EA5E9).copy(alpha = pulseAlpha1), CircleShape)

                                            )

                                            Box(

                                                modifier = Modifier

                                                    .size(70.dp)

                                                    .background(Color(0xFF0EA5E9).copy(alpha = 0.2f), CircleShape)

                                                    .border(1.5.dp, Color(0xFF0EA5E9), CircleShape),

                                                contentAlignment = Alignment.Center

                                            ) {

                                                Text("🔍", fontSize = 28.sp)

                                            }

                                        }

                                        Spacer(modifier = Modifier.height(20.dp))

                                        Text(

                                            text = "Finding nearest rider...",

                                            fontSize = 16.sp,

                                            fontWeight = FontWeight.Bold,

                                            color = textPrimary,

                                            textAlign = TextAlign.Center

                                        )

                                        Spacer(modifier = Modifier.height(8.dp))

                                        val min = searchSeconds / 60

                                        val sec = searchSeconds % 60

                                        val timerString = String.format(java.util.Locale.US, "%02d:%02d", min, sec)

                                        Text(

                                            text = "Search active for $timerString",

                                            color = textSecondary,

                                            fontSize = 13.sp,

                                            textAlign = TextAlign.Center

                                        )

                                    } else {

                                        CircularProgressIndicator(

                                            color = primaryColor,

                                            strokeWidth = 3.dp,

                                            modifier = Modifier.size(40.dp)

                                        )

                                        Spacer(modifier = Modifier.height(16.dp))

                                        val progressText = when (deliveryStatus) {

                                            "accepted" -> "$runnerName is heading to pick up your item…"

                                            "in_transit" -> "$runnerName is carrying your item to destination…"

                                            else -> "Processing…"

                                        }

                                        Text(

                                            text = progressText,

                                            fontSize = 13.sp,

                                            fontWeight = FontWeight.Medium,

                                            color = textSecondary,

                                            textAlign = TextAlign.Center

                                        )

                                    }

                                }

                            }

                            val matchedOrder = remember(activeTransfer?.id, OrderRepository.pendingOrders.size, OrderRepository.completedOrders.size) {

                                if (activeTransfer != null) {

                                    OrderRepository.pendingOrders.find { it.id == activeTransfer!!.id }

                                        ?: OrderRepository.completedOrders.find { it.id == activeTransfer!!.id }

                                } else null

                            }

                            if (activeTransfer != null && matchedOrder?.screenshotUri != null) {

                                Spacer(modifier = Modifier.height(16.dp))

                                Card(

                                    shape = RoundedCornerShape(16.dp),

                                    colors = CardDefaults.cardColors(containerColor = Color.White),

                                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),

                                    modifier = Modifier.fillMaxWidth()

                                ) {

                                    Column(

                                        modifier = Modifier.padding(16.dp)

                                    ) {

                                        Row(

                                            verticalAlignment = Alignment.CenterVertically,

                                            modifier = Modifier.fillMaxWidth()

                                        ) {

                                            Text("📸", fontSize = 18.sp)

                                            Spacer(modifier = Modifier.width(10.dp))

                                            Text(

                                                text = "Package Photo Verified",

                                                fontWeight = FontWeight.Bold,

                                                fontSize = 14.sp,

                                                color = textPrimary

                                            )

                                        }

                                        Spacer(modifier = Modifier.height(12.dp))

                                        Box(

                                            modifier = Modifier

                                                .fillMaxWidth()

                                                .height(130.dp)

                                                .background(Color(0xFFF8FAFC), RoundedCornerShape(12.dp))

                                                .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(12.dp)),

                                            contentAlignment = Alignment.Center

                                        ) {

                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {

                                                Text("📦", fontSize = 34.sp)

                                                Spacer(modifier = Modifier.height(6.dp))

                                                Text(

                                                    text = "Package Photo Preview",

                                                    fontSize = 12.sp,

                                                    color = textSecondary,

                                                    fontWeight = FontWeight.SemiBold

                                                )

                                                Text(

                                                    text = "package_photo_verified.png",

                                                    fontSize = 10.sp,

                                                    color = Color.Gray

                                                )

                                            }

                                        }

                                    }

                                }

                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // ─── Cancel Button ──────────────────────────────

                            val isCancelable = deliveryStatus == "searching"

                            Button(

                                onClick = {

                                    if (isCancelable && activeTransfer != null) {

                                        onCancelTransfer(activeTransfer.id)

                                    }

                                },

                                enabled = isCancelable,

                                shape = RoundedCornerShape(12.dp),

                                colors = ButtonDefaults.buttonColors(

                                    containerColor = AppTheme.Danger,

                                    disabledContainerColor = Color(0xFFE2E8F0)

                                ),

                                modifier = Modifier.fillMaxWidth().height(50.dp)

                            ) {

                                Text(

                                    text = if (isCancelable) "Cancel Request" else "Cancellation Disabled",

                                    color = if (isCancelable) Color.White else textSecondary,

                                    fontWeight = FontWeight.Bold,

                                    fontSize = 14.sp

                                )

                            }

                            if (!isCancelable) {

                                Spacer(modifier = Modifier.height(8.dp))

                                Text(

                                    text = "Once a runner accepts, cancellation is disabled.",

                                    fontSize = 11.sp,

                                    color = AppTheme.Danger,

                                    fontWeight = FontWeight.Medium,

                                    textAlign = TextAlign.Center,

                                    modifier = Modifier.fillMaxWidth()

                                )

                            }

                        }

                        Spacer(modifier = Modifier.height(24.dp))

                    }

                }

            }

        }

    }

    // Insufficient Balance Dialog

    if (showBalanceDialog) {

        AlertDialog(

            onDismissRequest = { showBalanceDialog = false },

            title = {

                Text(

                    text = "Insufficient Wallet Balance",

                    fontWeight = FontWeight.Bold,

                    fontSize = 18.sp,

                    color = textPrimary

                )

            },

            text = {

                Text(

                    text = "You do not have enough funds in your App Wallet to cover the reward fee (₹$totalPayout). Please top up your wallet to continue.",

                    color = textSecondary,

                    fontSize = 13.sp

                )

            },

            confirmButton = {

                Button(

                    onClick = {

                        showBalanceDialog = false

                        val intent = Intent(context, WalletActivity::class.java)

                        context.startActivity(intent)

                    },

                    colors = ButtonDefaults.buttonColors(containerColor = primaryColor),

                    shape = AppTheme.ButtonShape

                ) {

                    Text("Go to Wallet", color = Color.White)

                }

            },

            dismissButton = {

                TextButton(

                    onClick = { showBalanceDialog = false }

                ) {

                    Text("Cancel", color = textSecondary)

                }

            },

            shape = AppTheme.CardShape,

            containerColor = Color.White

        )

    }

    // Handover Completed Dialog

    if (showSuccessDialog) {

        AlertDialog(

            onDismissRequest = { showSuccessDialog = false },

            title = {

                Text(

                    text = "Handover Verified!",

                    fontWeight = FontWeight.Bold,

                    fontSize = 18.sp,

                    color = AppTheme.Success

                )

            },

            text = {

                Column {

                    Text("Your secure delivery has been successfully verified via OTP.", color = textPrimary, fontSize = 13.sp)

                    Spacer(modifier = Modifier.height(6.dp))

                    Text("• Item: ${activeTransfer?.itemName}", color = textSecondary, fontSize = 12.sp)

                    Text("• Route: ${activeTransfer?.pickPoint} ➔ ${activeTransfer?.dropPoint}", color = textSecondary, fontSize = 12.sp)

                    Text("• Paid Runner: ₹${activeTransfer?.fee}", color = textSecondary, fontSize = 12.sp)

                }

            },

            confirmButton = {

                Button(

                    onClick = {

                        showSuccessDialog = false

                    },

                    colors = ButtonDefaults.buttonColors(containerColor = AppTheme.Success),

                    shape = AppTheme.ButtonShape

                ) {

                    Text("Awesome", color = Color.White)

                }

            },

            shape = AppTheme.CardShape,

            containerColor = Color.White

        )

    }

}

@Composable

fun StatusStepRow(

    stepNumber: Int,

    label: String,

    isActive: Boolean,

    isCompleted: Boolean

) {

    val stepColor = if (isCompleted) {

        AppTheme.Success

    } else if (isActive) {

        AppTheme.Primary

    } else {

        Color(0xFFCBD5E1)

    }

    Row(

        modifier = Modifier

            .fillMaxWidth()

            .padding(vertical = 10.dp),

        verticalAlignment = Alignment.CenterVertically

    ) {

        Box(

            modifier = Modifier

                .size(24.dp)

                .clip(CircleShape)

                .background(stepColor),

            contentAlignment = Alignment.Center

        ) {

            if (isCompleted) {

                Text("✓", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)

            } else {

                Text(

                    text = stepNumber.toString(),

                    color = if (isActive) Color.White else AppTheme.InkSecondary,

                    fontWeight = FontWeight.Bold,

                    fontSize = 11.sp

                )

            }

        }

        Spacer(modifier = Modifier.width(12.dp))

        Text(

            text = label,

            fontSize = 13.sp,

            fontWeight = if (isActive || isCompleted) FontWeight.SemiBold else FontWeight.Normal,

            color = if (isActive || isCompleted) AppTheme.InkPrimary else AppTheme.InkSecondary

        )

    }

}

@Composable

fun FormStepIndicator(currentStep: Int) {

    Row(

        modifier = Modifier

            .fillMaxWidth()

            .padding(vertical = 12.dp),

        horizontalArrangement = Arrangement.SpaceBetween,

        verticalAlignment = Alignment.CenterVertically

    ) {

        val steps = listOf("Details", "Route", "Booking")

        steps.forEachIndexed { index, title ->

            val stepNum = index + 1

            val isCurrent = stepNum == currentStep

            val isPassed = stepNum < currentStep

            val circleBg = if (isCurrent) AppTheme.Primary else if (isPassed) AppTheme.Success else Color(0xFFCBD5E1)

            val textColor = if (isCurrent) AppTheme.Primary else if (isPassed) AppTheme.Success else AppTheme.InkSecondary

            Row(verticalAlignment = Alignment.CenterVertically) {

                Box(

                    modifier = Modifier

                        .size(24.dp)

                        .clip(CircleShape)

                        .background(circleBg),

                    contentAlignment = Alignment.Center

                ) {

                    Text(

                        text = if (isPassed) "✓" else stepNum.toString(),

                        color = Color.White,

                        fontSize = 12.sp,

                        fontWeight = FontWeight.Bold

                    )

                }

                Spacer(modifier = Modifier.width(6.dp))

                Text(

                    text = title,

                    color = textColor,

                    fontSize = 12.sp,

                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal

                )

            }

            if (index < 2) {

                Divider(

                    color = if (stepNum < currentStep) AppTheme.Success else Color(0xFFE2E8F0),

                    modifier = Modifier

                        .weight(1f)

                        .padding(horizontal = 8.dp)

                        .height(1.dp)

                )

            }

        }

    }

}

@OptIn(ExperimentalMaterial3Api::class)

@Composable

fun MyTransfersPage(

    currentUserEmail: String,

    primaryColor: Color,

    textPrimary: Color,

    textSecondary: Color,

    onTrackOrderClick: (SimulatedOrder) -> Unit,

    onBack: () -> Unit,

    modifier: Modifier = Modifier

) {

    val context = LocalContext.current

    LaunchedEffect(Unit) {

        OrderRepository.load(context, force = true)

    }

    val myPending = remember {

        derivedStateOf {

            OrderRepository.pendingOrders.filter { 

                (it.id.startsWith("OrderNo-") || it.id.startsWith("CSV-")) && 

                it.customerId == currentUserEmail && 

                it.status != "DELIVERED" && 

                it.status != "CANCELLED" 

            }

        }

    }

    val myCompleted = remember {

        derivedStateOf {

            OrderRepository.completedOrders.filter { 

                (it.id.startsWith("OrderNo-") || it.id.startsWith("CSV-")) && 

                it.customerId == currentUserEmail && 

                it.status == "DELIVERED" 

            }

        }

    }

    val myCancelled = remember {

        derivedStateOf {

            OrderRepository.cancelledOrders.filter { 

                (it.id.startsWith("OrderNo-") || it.id.startsWith("CSV-")) && 

                it.customerId == currentUserEmail && 

                it.status == "CANCELLED" 

            }

        }

    }

    var selectedTab by remember { mutableStateOf(0) }

    val tabTitles = listOf("Current", "Past", "Cancelled")

    Scaffold(

        topBar = {

            CenterAlignedTopAppBar(

                title = { Text("My Transfers", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = textPrimary) },

                navigationIcon = {

                    IconButton(onClick = onBack) {

                        Text("←", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = textPrimary)

                    }

                },

                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.White)

            )

        },

        containerColor = AppTheme.Background,

        modifier = modifier

    ) { paddingValues ->

        Column(modifier = Modifier.padding(paddingValues).fillMaxSize()) {

            TabRow(

                selectedTabIndex = selectedTab,

                containerColor = Color.White,

                contentColor = primaryColor

            ) {

                tabTitles.forEachIndexed { index, title ->

                    Tab(

                        selected = selectedTab == index,

                        onClick = { selectedTab = index },

                        text = { Text(title, fontWeight = FontWeight.SemiBold, fontSize = 13.sp) }

                    )

                }

            }

            val currentList = when (selectedTab) {

                0 -> myPending.value

                1 -> myCompleted.value

                else -> myCancelled.value

            }

            if (currentList.isEmpty()) {

                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {

                        Text("📦", fontSize = 48.sp)

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(

                            text = "No transfers found",

                            color = textSecondary,

                            fontSize = 14.sp,

                            fontWeight = FontWeight.Medium

                        )

                    }

                }

            } else {

                Column(

                    modifier = Modifier

                        .weight(1f)

                        .fillMaxSize()

                        .verticalScroll(rememberScrollState())

                        .padding(16.dp),

                    verticalArrangement = Arrangement.spacedBy(12.dp)

                ) {

                    currentList.forEach { order ->

                        Card(

                            modifier = Modifier.fillMaxWidth(),

                            shape = AppTheme.CardShape,

                            colors = CardDefaults.cardColors(containerColor = Color.White),

                            border = BorderStroke(1.dp, AppTheme.DividerColor)

                        ) {

                            Column(modifier = Modifier.padding(16.dp)) {

                                Row(

                                    modifier = Modifier.fillMaxWidth(),

                                    horizontalArrangement = Arrangement.SpaceBetween,

                                    verticalAlignment = Alignment.CenterVertically

                                ) {

                                    Row(verticalAlignment = Alignment.CenterVertically) {

                                        Text("📦", fontSize = 18.sp)

                                        Spacer(modifier = Modifier.width(8.dp))

                                        Text(

                                            text = "Order No: " + order.id.replace("OrderNo-", "").replace("CSV-", ""),

                                            fontWeight = FontWeight.Bold,

                                            fontSize = 14.sp,

                                            color = textPrimary

                                        )

                                    }

                                    Text(

                                        text = order.status,

                                        fontWeight = FontWeight.Bold,

                                        fontSize = 12.sp,

                                        color = if (order.status == "DELIVERED") AppTheme.Success else if (order.status == "CANCELLED") AppTheme.Danger else primaryColor

                                    )

                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                Divider(color = AppTheme.DividerColor)

                                Spacer(modifier = Modifier.height(12.dp))

                                Text(

                                    text = "Item: ${order.items.firstOrNull()?.first ?: "Package"}",

                                    fontSize = 13.sp,

                                    fontWeight = FontWeight.Medium,

                                    color = textPrimary

                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                Text(

                                    text = "Route: ${order.pickPoint} → ${order.dropPoint}",

                                    fontSize = 12.sp,

                                    color = textSecondary

                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                Text(

                                    text = "Total paid: ₹${order.totalMin}",

                                    fontSize = 12.sp,

                                    fontWeight = FontWeight.SemiBold,

                                    color = textPrimary

                                )

                                if (selectedTab == 0) {

                                    Spacer(modifier = Modifier.height(12.dp))

                                    Button(

                                        onClick = { onTrackOrderClick(order) },

                                        shape = AppTheme.ButtonShape,

                                        colors = ButtonDefaults.buttonColors(containerColor = primaryColor),

                                        modifier = Modifier.fillMaxWidth(),

                                        contentPadding = PaddingValues(vertical = 8.dp)

                                    ) {

                                        Text("Track Order ➔", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)

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
