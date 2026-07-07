package com.college.campusapp

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class WalletActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            WalletScreen(onBack = { finish() })
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WalletScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var balance by remember { mutableStateOf(WalletManager.getBalance(context)) }
    var transactions by remember { mutableStateOf(WalletManager.getTransactions(context)) }
    var showWithdrawDialog by remember { mutableStateOf(false) }

    val primaryColor = AppTheme.Primary
    val textPrimary = AppTheme.InkPrimary
    val textSecondary = AppTheme.InkSecondary

    if (showWithdrawDialog) {
        var amountText by remember { mutableStateOf("") }
        var upiIdText by remember { mutableStateOf("") }
        var withdrawError by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showWithdrawDialog = false },
            shape = RoundedCornerShape(24.dp),
            containerColor = Color.White,
            title = {
                Text(
                    text = "Withdraw to Bank",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = textPrimary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            text = {
                Column {
                    Text(
                        text = "Transfer cash securely to your account via UPI",
                        fontSize = 12.sp,
                        color = textSecondary,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    
                    OutlinedTextField(
                        value = amountText,
                        onValueChange = { amountText = it.filter { char -> char.isDigit() } },
                        label = { Text("Amount (₹)") },
                        placeholder = { Text("Min ₹50") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = AppTheme.FieldShape,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = primaryColor,
                            unfocusedBorderColor = AppTheme.DividerColor
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    OutlinedTextField(
                        value = upiIdText,
                        onValueChange = { upiIdText = it },
                        label = { Text("UPI ID") },
                        placeholder = { Text("student@upi") },
                        singleLine = true,
                        shape = AppTheme.FieldShape,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = primaryColor,
                            unfocusedBorderColor = AppTheme.DividerColor
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (withdrawError.isNotEmpty()) {
                        Text(
                            text = withdrawError,
                            color = Color.Red,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(top = 8.dp, start = 4.dp)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amount = amountText.toIntOrNull() ?: 0
                        if (amount < 50) {
                            withdrawError = "Minimum withdrawal amount is ₹50"
                            return@Button
                        }
                        if (amount > balance) {
                            withdrawError = "Insufficient wallet balance (Available: ₹$balance)"
                            return@Button
                        }
                        if (upiIdText.trim().isEmpty() || !upiIdText.contains("@")) {
                            withdrawError = "Please enter a valid UPI ID (e.g. name@upi)"
                            return@Button
                        }

                        val success = WalletManager.withdraw(context, amount, upiIdText.trim())
                        if (success) {
                            balance = WalletManager.getBalance(context)
                            transactions = WalletManager.getTransactions(context)
                            showWithdrawDialog = false
                            Toast.makeText(context, "Withdrawal request of ₹$amount submitted!", Toast.LENGTH_LONG).show()
                        } else {
                            withdrawError = "Failed to process withdrawal"
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Request Payout", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showWithdrawDialog = false }) {
                    Text("Cancel", color = textSecondary)
                }
            }
        )
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text("App Wallet", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = textPrimary)
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Text("←", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = textPrimary)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = AppTheme.Background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Large Premium Balance Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Brush.linearGradient(listOf(Color(0xFF0288D1), Color(0xFF03A9F4))))
                        .padding(24.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Available Balance", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("₹$balance.00", color = Color.White, fontSize = 36.sp, fontWeight = FontWeight.ExtraBold)
                        }
                        
                        Button(
                            onClick = { showWithdrawDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                            shape = AppTheme.ButtonShape,
                            modifier = Modifier
                                .align(Alignment.End)
                                .height(38.dp)
                        ) {
                            Text("Withdraw to Bank", color = Color(0xFF0288D1), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Recent Transactions Title
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "TRANSACTION HISTORY",
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    letterSpacing = 1.sp,
                    color = AppTheme.InkTertiary
                )
                Text(
                    text = "${transactions.size} records",
                    fontSize = 11.sp,
                    color = textSecondary
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Scrollable Transactions List
            if (transactions.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No transactions yet.", color = textSecondary, fontSize = 13.sp)
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White)
                        .border(1.5.dp, AppTheme.DividerColor, RoundedCornerShape(16.dp))
                        .padding(8.dp)
                ) {
                    items(transactions) { tx ->
                        TransactionRowItem(tx = tx)
                        Divider(color = AppTheme.DividerColor.copy(alpha = 0.5f))
                    }
                }
            }
        }
    }
}

@Composable
fun TransactionRowItem(tx: WalletTransaction) {
    val date = Date(tx.timestamp)
    val formatter = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
    val dateString = formatter.format(date)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = tx.description,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = AppTheme.InkPrimary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = dateString,
                    fontSize = 10.sp,
                    color = AppTheme.InkSecondary
                )
                if (tx.status == "PENDING") {
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Pending Payout",
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFD97706),
                        modifier = Modifier
                            .background(Color(0xFFFEF3C7), RoundedCornerShape(4.dp))
                            .padding(horizontal = 4.dp, vertical = 1.dp)
                    )
                }
            }
        }
        
        val amountSign = if (tx.amount > 0) "+" else ""
        val amountColor = if (tx.amount > 0) Color(0xFF10B981) else Color(0xFF0F172A)
        
        Text(
            text = "$amountSign₹${tx.amount}",
            fontWeight = FontWeight.ExtraBold,
            fontSize = 14.sp,
            color = amountColor
        )
    }
}
