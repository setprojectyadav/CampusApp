package com.college.campusapp

import android.content.Context
import com.college.campusapp.security.EncryptionManager
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.UUID

data class WalletTransaction(
    val id: String,
    val amount: Int,
    val type: String, // "REFUND", "PAYMENT", "WITHDRAWAL"
    val timestamp: Long,
    val description: String,
    val status: String // "PENDING", "COMPLETED"
)

object WalletManager {
    private val gson = Gson()

    fun getBalance(context: Context, isRider: Boolean = false): Int {
        if (isRider) {
            val isRiderFirstTime = EncryptionManager.getInt(context, "rider_wallet_initialized", 0) == 0
            if (isRiderFirstTime) {
                EncryptionManager.saveInt(context, "rider_wallet_initialized", 1)
                EncryptionManager.saveInt(context, "rider_wallet_balance", 0) // Rider starts at ₹0
            }
            return EncryptionManager.getInt(context, "rider_wallet_balance", 0)
        } else {
            // Initialize balance with a welcome credit if not set yet (v4 sets it to ₹2000)
            val isFirstTime = EncryptionManager.getInt(context, "wallet_initialized_v4", 0) == 0
            if (isFirstTime) {
                EncryptionManager.saveInt(context, "wallet_initialized_v4", 1)
                EncryptionManager.saveInt(context, "wallet_initialized_v3", 1)
                EncryptionManager.saveInt(context, "wallet_initialized_v2", 1)
                EncryptionManager.saveInt(context, "wallet_initialized", 1)
                EncryptionManager.saveInt(context, "wallet_test_credited_20k", 1)
                EncryptionManager.saveInt(context, "wallet_balance", 2000) // Set initial balance to exactly ₹2000
                val initialTx = WalletTransaction(
                    id = UUID.randomUUID().toString(),
                    amount = 2000,
                    type = "REFUND",
                    timestamp = System.currentTimeMillis(),
                    description = "Initial Wallet Balance",
                    status = "COMPLETED"
                )
                saveTransactions(context, listOf(initialTx), isRider = false)
                SystemNotificationHelper.showNotification(context, "💵 ₹2000 Wallet Balance Credited!", "Welcome to CampusApp! Your wallet balance has been initialized to ₹2000.")
            }
            return EncryptionManager.getInt(context, "wallet_balance", 0)
        }
    }

    fun credit(context: Context, amount: Int, description: String, isRider: Boolean = false) {
        val currentBalance = getBalance(context, isRider)
        val newBalance = currentBalance + amount
        val balanceKey = if (isRider) "rider_wallet_balance" else "wallet_balance"
        EncryptionManager.saveInt(context, balanceKey, newBalance)
        
        val tx = WalletTransaction(
            id = UUID.randomUUID().toString(),
            amount = amount,
            type = "REFUND",
            timestamp = System.currentTimeMillis(),
            description = description,
            status = "COMPLETED"
        )
        val list = getTransactions(context, isRider).toMutableList()
        list.add(0, tx) // Add to top of list
        saveTransactions(context, list, isRider)
        
        val orderId = if (description.contains("OrderNo-")) {
            "Order No: " + description.substringAfter("OrderNo-").substringBefore(" ").trim()
        } else if (description.contains("CSV-")) {
            "Order No: " + description.substringAfter("CSV-").substringBefore(" ").trim()
        } else if (description.contains("ORDER-")) {
            "ORDER-" + description.substringAfter("ORDER-").substringBefore(" ").trim()
        } else {
            ""
        }
        val uniqueTitle = if (orderId.isNotEmpty()) "💵 Wallet Credited ($orderId)" else "💵 Wallet Balance Credited!"
        SystemNotificationHelper.showNotification(context, uniqueTitle, "₹$amount has been credited: $description")
    }

    fun debit(context: Context, amount: Int, description: String, isRider: Boolean = false): Boolean {
        val currentBalance = getBalance(context, isRider)
        if (currentBalance < amount) return false
        val newBalance = currentBalance - amount
        val balanceKey = if (isRider) "rider_wallet_balance" else "wallet_balance"
        EncryptionManager.saveInt(context, balanceKey, newBalance)
        
        val tx = WalletTransaction(
            id = UUID.randomUUID().toString(),
            amount = -amount,
            type = "PAYMENT",
            timestamp = System.currentTimeMillis(),
            description = description,
            status = "COMPLETED"
        )
        val list = getTransactions(context, isRider).toMutableList()
        list.add(0, tx)
        saveTransactions(context, list, isRider)
        
        val orderId = if (description.contains("OrderNo-")) {
            "Order No: " + description.substringAfter("OrderNo-").substringBefore(" ").trim()
        } else if (description.contains("CSV-")) {
            "Order No: " + description.substringAfter("CSV-").substringBefore(" ").trim()
        } else if (description.contains("ORDER-")) {
            "ORDER-" + description.substringAfter("ORDER-").substringBefore(" ").trim()
        } else {
            ""
        }
        val uniqueTitle = if (orderId.isNotEmpty()) "💳 Wallet Debited ($orderId)" else "💳 Wallet Balance Debited"
        SystemNotificationHelper.showNotification(context, uniqueTitle, "₹$amount has been debited: $description")
        return true
    }

    fun withdraw(context: Context, amount: Int, methodDetails: String, isRider: Boolean = false): Boolean {
        if (!isRider) return false // Block customer withdrawals programmatically
        val currentBalance = getBalance(context, isRider)
        if (currentBalance < amount) return false
        val newBalance = currentBalance - amount
        val balanceKey = if (isRider) "rider_wallet_balance" else "wallet_balance"
        EncryptionManager.saveInt(context, balanceKey, newBalance)
        
        val tax = (amount * 0.05).toInt()
        val credited = amount - tax
        
        val tx = WalletTransaction(
            id = "WITHDRAW-" + (10000000..99999999).random().toString(),
            amount = -amount,
            type = "WITHDRAWAL",
            timestamp = System.currentTimeMillis(),
            description = "Withdrawal to: $methodDetails (5% Tax: ₹$tax, Credited: ₹$credited)",
            status = "COMPLETED"
        )
        val list = getTransactions(context, isRider).toMutableList()
        list.add(0, tx)
        saveTransactions(context, list, isRider)
        return true
    }

    fun getLockedBalance(context: Context, isRider: Boolean = false): Int {
        val txs = getTransactions(context, isRider)
        val cal = java.util.Calendar.getInstance()
        val currentWeek = cal.get(java.util.Calendar.WEEK_OF_YEAR)
        val currentYear = cal.get(java.util.Calendar.YEAR)

        var locked = 0
        for (tx in txs) {
            if (tx.amount > 0 && tx.type == "REFUND") {
                val desc = tx.description.lowercase()
                if (desc.contains("payout") || desc.contains("compensation") || desc.contains("violation")) {
                    val txCal = java.util.Calendar.getInstance()
                    txCal.timeInMillis = tx.timestamp
                    val txWeek = txCal.get(java.util.Calendar.WEEK_OF_YEAR)
                    val txYear = txCal.get(java.util.Calendar.YEAR)
                    if (currentWeek == txWeek && currentYear == txYear) {
                        locked += tx.amount
                    }
                }
            }
        }
        return locked
    }

    fun getTransactions(context: Context, isRider: Boolean = false): List<WalletTransaction> {
        val key = if (isRider) "rider_wallet_transactions" else "wallet_transactions"
        val json = EncryptionManager.getString(context, key, "")
        if (json.isNullOrEmpty()) return emptyList()
        return try {
            val type = object : TypeToken<List<WalletTransaction>>() {}.type
            gson.fromJson(json, type)
        } catch (e: java.lang.Exception) {
            emptyList()
        }
    }

    private fun saveTransactions(context: Context, list: List<WalletTransaction>, isRider: Boolean = false) {
        val key = if (isRider) "rider_wallet_transactions" else "wallet_transactions"
        val json = gson.toJson(list)
        EncryptionManager.saveString(context, key, json)
    }
}
