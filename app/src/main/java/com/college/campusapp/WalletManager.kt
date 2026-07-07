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

    fun getBalance(context: Context): Int {
        // Initialize balance with a welcome credit if not set yet
        val isFirstTime = EncryptionManager.getInt(context, "wallet_initialized", 0) == 0
        if (isFirstTime) {
            EncryptionManager.saveInt(context, "wallet_initialized", 1)
            EncryptionManager.saveInt(context, "wallet_balance", 200) // ₹200 welcome bonus!
            val initialTx = WalletTransaction(
                id = UUID.randomUUID().toString(),
                amount = 200,
                type = "REFUND",
                timestamp = System.currentTimeMillis(),
                description = "Welcome Balance Credited",
                status = "COMPLETED"
            )
            saveTransactions(context, listOf(initialTx))
        }

        // Add ₹20,000 test balance if not credited yet
        val isTestCredited = EncryptionManager.getInt(context, "wallet_test_credited_20k", 0) == 1
        if (!isTestCredited) {
            EncryptionManager.saveInt(context, "wallet_test_credited_20k", 1)
            // Directly credit balance & save transaction
            val currentBalance = EncryptionManager.getInt(context, "wallet_balance", 0)
            val newBalance = currentBalance + 20000
            EncryptionManager.saveInt(context, "wallet_balance", newBalance)
            
            val tx = WalletTransaction(
                id = UUID.randomUUID().toString(),
                amount = 20000,
                type = "REFUND",
                timestamp = System.currentTimeMillis(),
                description = "Testing Credit Added",
                status = "COMPLETED"
            )
            val list = getTransactions(context).toMutableList()
            list.add(0, tx)
            saveTransactions(context, list)
        }

        return EncryptionManager.getInt(context, "wallet_balance", 0)
    }

    fun credit(context: Context, amount: Int, description: String) {
        val currentBalance = getBalance(context)
        val newBalance = currentBalance + amount
        EncryptionManager.saveInt(context, "wallet_balance", newBalance)
        
        val tx = WalletTransaction(
            id = UUID.randomUUID().toString(),
            amount = amount,
            type = "REFUND",
            timestamp = System.currentTimeMillis(),
            description = description,
            status = "COMPLETED"
        )
        val list = getTransactions(context).toMutableList()
        list.add(0, tx) // Add to top of list
        saveTransactions(context, list)
    }

    fun debit(context: Context, amount: Int, description: String): Boolean {
        val currentBalance = getBalance(context)
        if (currentBalance < amount) return false
        val newBalance = currentBalance - amount
        EncryptionManager.saveInt(context, "wallet_balance", newBalance)
        
        val tx = WalletTransaction(
            id = UUID.randomUUID().toString(),
            amount = -amount,
            type = "PAYMENT",
            timestamp = System.currentTimeMillis(),
            description = description,
            status = "COMPLETED"
        )
        val list = getTransactions(context).toMutableList()
        list.add(0, tx)
        saveTransactions(context, list)
        return true
    }

    fun withdraw(context: Context, amount: Int, methodDetails: String): Boolean {
        val currentBalance = getBalance(context)
        if (currentBalance < amount) return false
        val newBalance = currentBalance - amount
        EncryptionManager.saveInt(context, "wallet_balance", newBalance)
        
        val tx = WalletTransaction(
            id = UUID.randomUUID().toString(),
            amount = -amount,
            type = "WITHDRAWAL",
            timestamp = System.currentTimeMillis(),
            description = "Withdrawal to: $methodDetails",
            status = "PENDING"
        )
        val list = getTransactions(context).toMutableList()
        list.add(0, tx)
        saveTransactions(context, list)
        return true
    }

    fun getTransactions(context: Context): List<WalletTransaction> {
        val json = EncryptionManager.getString(context, "wallet_transactions", "")
        if (json.isNullOrEmpty()) return emptyList()
        return try {
            val type = object : TypeToken<List<WalletTransaction>>() {}.type
            gson.fromJson(json, type)
        } catch (e: java.lang.Exception) {
            emptyList()
        }
    }

    private fun saveTransactions(context: Context, list: List<WalletTransaction>) {
        val json = gson.toJson(list)
        EncryptionManager.saveString(context, "wallet_transactions", json)
    }
}
