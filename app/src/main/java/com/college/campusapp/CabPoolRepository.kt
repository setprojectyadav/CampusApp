package com.college.campusapp

import android.content.Context
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.mutableStateListOf
import com.college.campusapp.security.EncryptionManager
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.UUID
import kotlin.math.ceil

@Immutable
data class CabRider(
    val email: String,
    val name: String,
    val phone: String,
    val hostel: String,
    val gender: String = "Any",
    val joinedTimestamp: Long = System.currentTimeMillis(),
    val paid: Boolean = false,
    val paymentMethod: String = "WALLET", // "WALLET" or "UPI"
    val hideContactInPreview: Boolean = true
)

@Immutable
data class CabPoolChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val senderEmail: String,
    val senderName: String,
    val message: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Immutable
data class CabPool(
    val id: String,
    val hostEmail: String,
    val hostName: String,
    val hostPhone: String,
    val hostHostel: String,
    val hostBatch: String = "Batch 2026",
    val pickupPoint: String,
    val dropDestination: String,
    val routeCategory: String, // "Airport", "Station", "Mall", or Custom Name
    val departureDate: String, // "2026-08-27"
    val departureTime: String, // "07:30 PM"
    val departureTimestamp: Long = System.currentTimeMillis() + 3600000 * 4,
    val totalSeats: Int, // 4 or 7
    val bookedSeats: Int,
    val riders: List<CabRider> = emptyList(),
    val estimatedTotalFare: Int,
    val perPersonFare: Int = if (totalSeats > 0) ceil(estimatedTotalFare.toDouble() / totalSeats.toDouble()).toInt() else estimatedTotalFare,
    val cabType: String = "4-Seater", // "4-Seater" or "7-Seater"
    val femaleOnly: Boolean = false,
    val hideHostContactInPreview: Boolean = true,
    val notes: String = "",
    val status: String = "GATHERING", // "GATHERING", "LOCKED", "CAB_BOOKED", "COMPLETED", "CANCELLED"
    val cabDriverDetails: String? = null,
    val pendingHostTransferEmail: String? = null,
    val pendingHostTransferName: String? = null,
    val flightOrTrainNumber: String? = null,
    val isSuperhost: Boolean = false,
    val chatMessages: List<CabPoolChatMessage> = emptyList(),
    val createdTimestamp: Long = System.currentTimeMillis()
) {
    fun calculatePerPersonFare(): Int {
        val total = totalSeats.coerceAtLeast(1)
        return ceil(estimatedTotalFare.toDouble() / total.toDouble()).toInt()
    }

    fun isCancellationLocked(): Boolean {
        val oneHourBefore = departureTimestamp - (60 * 60 * 1000)
        return System.currentTimeMillis() >= oneHourBefore
    }

    fun isExpired(): Boolean {
        return System.currentTimeMillis() >= departureTimestamp
    }
}

object CategoryFilterValidator {
    private val blockedKeywords = listOf(
        "abuse", "hate", "kill", "porn", "sex", "nude", "drugs", "weed", "alcohol", "beer", "wine", "smoke",
        "casino", "gambling", "bet", "fraud", "scam", "cheat", "fake", "bomb", "terror", "weapon", "gun",
        "idiot", "stupid", "fuck", "shit", "bitch", "bastard", "ass", "dick", "pussy", "crap", "damn",
        "caste", "racist", "communal", "riot", "bribe", "smuggle"
    )

    fun validateCategoryName(name: String): Pair<Boolean, String> {
        val trimmed = name.trim()
        if (trimmed.length < 2) {
            return Pair(false, "Category name must be at least 2 characters.")
        }
        if (trimmed.length > 25) {
            return Pair(false, "Category name cannot exceed 25 characters.")
        }
        val validRegex = Regex("^[a-zA-Z0-9\\s/&\\-]+$")
        if (!validRegex.matches(trimmed)) {
            return Pair(false, "Only letters, numbers, spaces, and '/', '&', '-' are allowed.")
        }
        val lower = trimmed.lowercase()
        for (word in blockedKeywords) {
            if (lower.contains(word)) {
                return Pair(false, "Please choose a respectful, appropriate category name.")
            }
        }
        return Pair(true, "")
    }
}

object CabPoolRepository {
    const val PLATFORM_COMMISSION = 20

    private val gson = Gson()
    private val scope = CoroutineScope(Dispatchers.IO)
    private var isLoaded = false

    val activePools = mutableStateListOf<CabPool>()

    fun load(context: Context, force: Boolean = false) {
        if (isLoaded && !force) return
        isLoaded = true

        try {
            val json = EncryptionManager.getString(context, "db_cab_pools_v11", "")
            if (!json.isNullOrEmpty()) {
                val type = object : TypeToken<List<CabPool>>() {}.type
                val loaded: List<CabPool> = gson.fromJson(json, type)
                activePools.clear()
                activePools.addAll(loaded.filter { !it.isExpired() && it.status != "COMPLETED" && it.status != "CANCELLED" })
            } else {
                initDefaultPools(context)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            initDefaultPools(context)
        }
    }

    private fun initDefaultPools(context: Context) {
        activePools.clear()
        val now = System.currentTimeMillis()
        activePools.addAll(
            listOf(
                CabPool(
                    id = "CAB-POOL-101",
                    hostEmail = "aarav.sharma@college.edu",
                    hostName = "Aarav Sharma",
                    hostPhone = "+91 98765 43210",
                    hostHostel = "Hostel 3 (Block A)",
                    hostBatch = "CS 2026",
                    pickupPoint = "Campus Main Gate",
                    dropDestination = "Kempegowda International Airport (Terminal 1 and 2)",
                    routeCategory = "Airport",
                    departureDate = "Today",
                    departureTime = "07:30 PM",
                    departureTimestamp = now + (90 * 60 * 1000),
                    totalSeats = 4,
                    bookedSeats = 2,
                    riders = listOf(
                        CabRider("aarav.sharma@college.edu", "Aarav Sharma", "+91 98765 43210", "Hostel 3", paid = true, hideContactInPreview = false),
                        CabRider("rohit.v@college.edu", "Rohit Verma", "+91 98765 11111", "Hostel 3", paid = true, hideContactInPreview = true)
                    ),
                    estimatedTotalFare = 1400,
                    perPersonFare = 350,
                    cabType = "4-Seater",
                    femaleOnly = false,
                    hideHostContactInPreview = false,
                    flightOrTrainNumber = "✈️ Flight 6E-2134",
                    isSuperhost = true,
                    notes = "Uber Premier booked. Large boot space available. Meeting near main security gate.",
                    status = "GATHERING"
                ),
                CabPool(
                    id = "CAB-POOL-102",
                    hostEmail = "priya.nair@college.edu",
                    hostName = "Priya Nair",
                    hostPhone = "+91 98765 22222",
                    hostHostel = "Girls Hostel 2",
                    hostBatch = "EC 2025",
                    pickupPoint = "Girls Hostel Block A Circle",
                    dropDestination = "Phoenix Marketcity Mall (Main Entry)",
                    routeCategory = "Mall",
                    departureDate = "Today",
                    departureTime = "04:30 PM",
                    departureTimestamp = now + (45 * 60 * 1000),
                    totalSeats = 4,
                    bookedSeats = 3,
                    riders = listOf(
                        CabRider("priya.nair@college.edu", "Priya Nair", "+91 98765 22222", "Girls Hostel 2", paid = true, hideContactInPreview = true),
                        CabRider("sneha.k@college.edu", "Sneha K", "+91 98765 33333", "Girls Hostel 1", paid = true, hideContactInPreview = true),
                        CabRider("ananya.g@college.edu", "Ananya Gupta", "+91 98765 44444", "Girls Hostel 2", paid = true, hideContactInPreview = true)
                    ),
                    estimatedTotalFare = 600,
                    perPersonFare = 150,
                    cabType = "4-Seater",
                    femaleOnly = true,
                    hideHostContactInPreview = true,
                    isSuperhost = true,
                    notes = "Weekend shopping trip. Only 1 seat left for female students.",
                    status = "GATHERING"
                ),
                CabPool(
                    id = "CAB-POOL-103",
                    hostEmail = "vikram.patel@college.edu",
                    hostName = "Vikram Patel",
                    hostPhone = "+91 98765 55555",
                    hostHostel = "Hostel 1",
                    hostBatch = "ME 2026",
                    pickupPoint = "Campus Sports Complex",
                    dropDestination = "City Central Railway Station (Platform 1)",
                    routeCategory = "Station",
                    departureDate = "Today",
                    departureTime = "09:15 PM",
                    departureTimestamp = now + (180 * 60 * 1000),
                    totalSeats = 7,
                    bookedSeats = 4,
                    riders = listOf(
                        CabRider("vikram.patel@college.edu", "Vikram Patel", "+91 98765 55555", "Hostel 1", paid = true, hideContactInPreview = false),
                        CabRider("karthik.r@college.edu", "Karthik R", "+91 98765 66666", "Hostel 4", paid = true, hideContactInPreview = true),
                        CabRider("manish.t@college.edu", "Manish Tiwari", "+91 98765 77777", "Hostel 1", paid = true, hideContactInPreview = true),
                        CabRider("deepak.s@college.edu", "Deepak S", "+91 98765 88888", "Hostel 2", paid = true, hideContactInPreview = true)
                    ),
                    estimatedTotalFare = 1050,
                    perPersonFare = 150,
                    cabType = "7-Seater",
                    femaleOnly = false,
                    hideHostContactInPreview = false,
                    flightOrTrainNumber = "🚆 Shatabdi Exp #12007",
                    isSuperhost = false,
                    notes = "Heading home for long weekend via night train.",
                    status = "GATHERING"
                )
            )
        )
        save(context)
    }

    fun calculateLifetimeSavings(userEmail: String): Int {
        var savings = 0
        activePools.forEach { pool ->
            val isParticipant = pool.hostEmail == userEmail || pool.riders.any { it.email == userEmail }
            if (isParticipant) {
                val soloCost = pool.estimatedTotalFare
                val myCost = pool.perPersonFare
                val saved = (soloCost - myCost).coerceAtLeast(0)
                savings += saved
            }
        }
        return if (savings > 0) savings else 1450
    }

    fun save(context: Context) {
        scope.launch {
            try {
                val json = gson.toJson(activePools.toList())
                EncryptionManager.saveString(context, "db_cab_pools_v11", json)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun createPool(context: Context, pool: CabPool, paymentMethod: String = "WALLET"): Boolean {
        activePools.add(0, pool)
        save(context)
        notifyAllMembers(context, pool.id, "Cab pool created for ${pool.dropDestination}.")
        return true
    }

    fun joinPool(context: Context, poolId: String, rider: CabRider, paymentMethod: String = "WALLET"): Boolean {
        val index = activePools.indexOfFirst { it.id == poolId }
        if (index != -1) {
            val pool = activePools[index]
            if (pool.bookedSeats >= pool.totalSeats || pool.riders.size >= pool.totalSeats) {
                return false
            }
            if (pool.riders.none { it.email == rider.email }) {
                val updatedRiders = pool.riders.toMutableList().apply { add(rider.copy(paid = true, paymentMethod = paymentMethod)) }
                val updatedPool = pool.copy(
                    riders = updatedRiders,
                    bookedSeats = updatedRiders.size
                )
                activePools[index] = updatedPool
                save(context)
                notifyAllMembers(context, poolId, "${rider.name} joined the cab pool.")
                return true
            }
        }
        return false
    }

    fun leavePool(context: Context, poolId: String, userEmail: String): Pair<Boolean, String> {
        val index = activePools.indexOfFirst { it.id == poolId }
        if (index != -1) {
            val pool = activePools[index]
            if (pool.isCancellationLocked()) {
                return Pair(false, "Cancellations are locked within 1 hour of scheduled departure.")
            }
            if (pool.hostEmail == userEmail) {
                return Pair(false, "Host cannot leave directly. Please transfer hostmanship to another rider or cancel the pool.")
            }
            val rider = pool.riders.find { it.email == userEmail }
            if (rider != null) {
                val updatedRiders = pool.riders.toMutableList().apply { remove(rider) }
                val updatedPool = pool.copy(
                    riders = updatedRiders,
                    bookedSeats = updatedRiders.size
                )
                activePools[index] = updatedPool
                save(context)
                notifyAllMembers(context, poolId, "${rider.name} left the pool. 1 seat opened.")
                return Pair(true, "You left the pool. Platform Convenience Fee is non-refundable.")
            }
        }
        return Pair(false, "Pool not found.")
    }

    fun cancelPool(context: Context, poolId: String): Pair<Boolean, String> {
        val index = activePools.indexOfFirst { it.id == poolId }
        if (index != -1) {
            val pool = activePools[index]
            if (pool.isCancellationLocked()) {
                return Pair(false, "Cannot cancel within 1 hour of scheduled departure.")
            }
            val cancelled = pool.copy(status = "CANCELLED")
            notifyAllMembers(context, poolId, "This cab pool was cancelled by the host.")
            activePools.removeAt(index)
            save(context)
            return Pair(true, "Cab pool cancelled.")
        }
        return Pair(false, "Pool not found.")
    }

    fun completePool(context: Context, poolId: String) {
        val index = activePools.indexOfFirst { it.id == poolId }
        if (index != -1) {
            val pool = activePools[index]
            activePools.removeAt(index)
            save(context)
            notifyAllMembers(context, poolId, "Journey completed. Thank you for using Campus Cab Pooling.")
        }
    }

    fun requestHostTransfer(context: Context, poolId: String, targetEmail: String, targetName: String) {
        val index = activePools.indexOfFirst { it.id == poolId }
        if (index != -1) {
            val pool = activePools[index]
            activePools[index] = pool.copy(
                pendingHostTransferEmail = targetEmail,
                pendingHostTransferName = targetName
            )
            save(context)
            notifyAllMembers(context, poolId, "Host transfer requested to $targetName. Awaiting acceptance.")
        }
    }

    fun acceptHostTransfer(context: Context, poolId: String, acceptedEmail: String): Boolean {
        val index = activePools.indexOfFirst { it.id == poolId }
        if (index != -1) {
            val pool = activePools[index]
            if (pool.pendingHostTransferEmail == acceptedEmail) {
                val newHostRider = pool.riders.find { it.email == acceptedEmail }
                if (newHostRider != null) {
                    val oldHostEmail = pool.hostEmail
                    val updatedRiders = pool.riders.filter { it.email != oldHostEmail }
                    activePools[index] = pool.copy(
                        hostEmail = newHostRider.email,
                        hostName = newHostRider.name,
                        hostPhone = newHostRider.phone,
                        hostHostel = newHostRider.hostel,
                        pendingHostTransferEmail = null,
                        pendingHostTransferName = null,
                        riders = updatedRiders,
                        bookedSeats = updatedRiders.size
                    )
                    save(context)
                    notifyAllMembers(context, poolId, "${newHostRider.name} accepted the host transfer and is now the official host.")
                    return true
                }
            }
        }
        return false
    }

    fun declineHostTransfer(context: Context, poolId: String) {
        val index = activePools.indexOfFirst { it.id == poolId }
        if (index != -1) {
            val pool = activePools[index]
            activePools[index] = pool.copy(
                pendingHostTransferEmail = null,
                pendingHostTransferName = null
            )
            save(context)
            notifyAllMembers(context, poolId, "Host transfer request was declined.")
        }
    }

    fun updateCabDetails(context: Context, poolId: String, details: String) {
        val index = activePools.indexOfFirst { it.id == poolId }
        if (index != -1) {
            val pool = activePools[index]
            activePools[index] = pool.copy(
                cabDriverDetails = details.trim(),
                status = "CAB_BOOKED"
            )
            save(context)
            notifyAllMembers(context, poolId, "Vehicle and driver info updated: $details")
        }
    }

    fun addChatMessage(context: Context, poolId: String, chat: CabPoolChatMessage) {
        val index = activePools.indexOfFirst { it.id == poolId }
        if (index != -1) {
            val pool = activePools[index]
            val updatedChats = pool.chatMessages.toMutableList().apply { add(chat) }
            activePools[index] = pool.copy(chatMessages = updatedChats)
            save(context)
        }
    }

    fun notifyAllMembers(context: Context, poolId: String, messageText: String) {
        val pool = activePools.find { it.id == poolId } ?: return
        val systemChat = CabPoolChatMessage(
            senderEmail = "system@campusapp",
            senderName = "Campus Safety Bot",
            message = messageText
        )
        val updatedChats = pool.chatMessages.toMutableList().apply { add(systemChat) }
        val index = activePools.indexOfFirst { it.id == poolId }
        if (index != -1) {
            activePools[index] = pool.copy(chatMessages = updatedChats)
            save(context)
        }

        // Only notify if the active device user is a joined rider or the host of this pool
        val currentUserEmail = EncryptionManager.getString(context, "user_email") ?: ""
        val isMember = currentUserEmail.isNotBlank() && (
            pool.hostEmail.equals(currentUserEmail, ignoreCase = true) ||
            pool.riders.any { it.email.equals(currentUserEmail, ignoreCase = true) }
        )

        if (isMember) {
            try {
                SystemNotificationHelper.showNotification(
                    context = context,
                    title = "Cab Pool Update: ${pool.dropDestination}",
                    message = messageText
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun refreshAndClean(context: Context) {
        load(context, force = true)
        val now = System.currentTimeMillis()
        val expired = activePools.filter { it.departureTimestamp <= now || it.status == "COMPLETED" || it.status == "CANCELLED" }
        if (expired.isNotEmpty()) {
            activePools.removeAll(expired)
            save(context)
        }
    }
}