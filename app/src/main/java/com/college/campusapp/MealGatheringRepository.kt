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
data class MealMember(
    val email: String,
    val name: String,
    val phone: String,
    val hostel: String,
    val joinedTimestamp: Long = System.currentTimeMillis(),
    val hideContactInPreview: Boolean = true,
    val karmaBadge: String = "⭐ Quick UPI Payer"
)

@Immutable
data class MealChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val senderEmail: String,
    val senderName: String,
    val message: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Immutable
data class MealCircle(
    val id: String,
    val hostEmail: String,
    val hostName: String,
    val hostPhone: String,
    val hostHostel: String,
    val hostBatch: String = "Batch 2026",
    val restaurantSpot: String,
    val mealCategory: String,
    val meetingPoint: String,
    val targetDate: String,
    val targetTime: String,
    val targetTimestamp: Long = System.currentTimeMillis() + 3600000 * 2,
    val totalBuddies: Int,
    val joinedBuddies: Int,
    val members: List<MealMember> = emptyList(),
    val estimatedTotalBill: Int = 0,
    val perPersonEstimate: Int = if (totalBuddies > 0 && estimatedTotalBill > 0) ceil(estimatedTotalBill.toDouble() / totalBuddies.toDouble()).toInt() else 0,
    val dietaryVibeTags: List<String> = emptyList(),
    val femaleOnly: Boolean = false,
    val hideHostContactInPreview: Boolean = true,
    val isNightOwl: Boolean = false,
    val hostKarmaBadge: String = "⭐ Instant UPI Payer",
    val notes: String = "",
    val status: String = "GATHERING",
    val orderTrackingDetails: String? = null,
    val chatMessages: List<MealChatMessage> = emptyList(),
    val pendingHostTransferEmail: String? = null,
    val pendingHostTransferName: String? = null,
    val createdTimestamp: Long = System.currentTimeMillis()
) {
    fun calculatePerPersonShare(): Int {
        if (estimatedTotalBill <= 0) return 0
        val total = totalBuddies.coerceAtLeast(1)
        return ceil(estimatedTotalBill.toDouble() / total.toDouble()).toInt()
    }

    fun isExpired(): Boolean {
        return System.currentTimeMillis() >= targetTimestamp
    }
}

object MealGatheringRepository {
    private val gson = Gson()
    private val scope = CoroutineScope(Dispatchers.IO)
    private var isLoaded = false

    val activeCircles = mutableStateListOf<MealCircle>()

    fun load(context: Context, force: Boolean = false) {
        if (isLoaded && !force) return
        isLoaded = true

        try {
            val json = EncryptionManager.getString(context, "db_meal_circles_v2", "")
            if (!json.isNullOrEmpty()) {
                val type = object : TypeToken<List<MealCircle>>() {}.type
                val loaded: List<MealCircle> = gson.fromJson(json, type)
                activeCircles.clear()
                activeCircles.addAll(loaded.filter { !it.isExpired() && it.status != "COMPLETED" && it.status != "CANCELLED" })
            } else {
                initDefaultCircles(context)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            initDefaultCircles(context)
        }
    }

    private fun initDefaultCircles(context: Context) {
        activeCircles.clear()
        val now = System.currentTimeMillis()
        activeCircles.addAll(
            listOf(
                MealCircle(
                    id = "MEAL-CIRCLE-101",
                    hostEmail = "aarav.sharma@college.edu",
                    hostName = "Aarav Sharma",
                    hostPhone = "+91 98765 43210",
                    hostHostel = "Hostel 3 (Block A)",
                    restaurantSpot = "Dominos Pizza (Hostel Delivery)",
                    mealCategory = "Delivery Split",
                    meetingPoint = "Hostel 3 Common Room",
                    targetDate = "Today",
                    targetTime = "08:45 PM",
                    targetTimestamp = now + (45 * 60 * 1000),
                    totalBuddies = 4,
                    joinedBuddies = 2,
                    members = listOf(
                        MealMember("aarav.sharma@college.edu", "Aarav Sharma", "+91 98765 43210", "Hostel 3", hideContactInPreview = false),
                        MealMember("rohit.v@college.edu", "Rohit Verma", "+91 98765 11111", "Hostel 3", hideContactInPreview = true)
                    ),
                    estimatedTotalBill = 640,
                    perPersonEstimate = 160,
                    dietaryVibeTags = listOf("Budget Deal", "Pure Veg"),
                    femaleOnly = false,
                    notes = "Ordering 2 Medium Farmhouse and Garlic Bread combo with 40 percent student discount. 2 seats open.",
                    orderTrackingDetails = null,
                    status = "GATHERING",
                    chatMessages = listOf(
                        MealChatMessage(
                            senderEmail = "aarav.sharma@college.edu",
                            senderName = "Aarav Sharma",
                            message = "Hey everyone! Let me know if anyone wants cheese burst crust before placing at 8:45 PM."
                        )
                    )
                ),
                MealCircle(
                    id = "MEAL-CIRCLE-102",
                    hostEmail = "priya.nair@college.edu",
                    hostName = "Priya Nair",
                    hostPhone = "+91 98765 22222",
                    hostHostel = "Girls Hostel 2",
                    restaurantSpot = "Campus Main Canteen (Thali and Dosa)",
                    mealCategory = "Campus Canteen",
                    meetingPoint = "Canteen Center Table 6",
                    targetDate = "Today",
                    targetTime = "01:15 PM",
                    targetTimestamp = now + (90 * 60 * 1000),
                    totalBuddies = 3,
                    joinedBuddies = 1,
                    members = listOf(
                        MealMember("priya.nair@college.edu", "Priya Nair", "+91 98765 22222", "Girls Hostel 2", hideContactInPreview = true)
                    ),
                    estimatedTotalBill = 240,
                    perPersonEstimate = 80,
                    dietaryVibeTags = listOf("South Indian", "Pure Veg"),
                    femaleOnly = true,
                    notes = "Heading for quick South Indian lunch between lab sessions. Join to sit and eat together.",
                    status = "GATHERING"
                ),
                MealCircle(
                    id = "MEAL-CIRCLE-103",
                    hostEmail = "vikram.patel@college.edu",
                    hostName = "Vikram Patel",
                    hostPhone = "+91 98765 33333",
                    hostHostel = "Hostel 1",
                    restaurantSpot = "Meghana Biryani and Kebabs",
                    mealCategory = "Dine-Out",
                    meetingPoint = "Campus Main Gate",
                    targetDate = "Today",
                    targetTime = "08:00 PM",
                    targetTimestamp = now + (180 * 60 * 1000),
                    totalBuddies = 5,
                    joinedBuddies = 3,
                    members = listOf(
                        MealMember("vikram.patel@college.edu", "Vikram Patel", "+91 98765 33333", "Hostel 1", hideContactInPreview = false),
                        MealMember("deepak.s@college.edu", "Deepak S", "+91 98765 44444", "Hostel 4", hideContactInPreview = true),
                        MealMember("karan.m@college.edu", "Karan Malhotra", "+91 98765 55555", "Hostel 1", hideContactInPreview = true)
                    ),
                    estimatedTotalBill = 1250,
                    perPersonEstimate = 250,
                    dietaryVibeTags = listOf("Non Veg", "Halal Friendly", "Budget Deal"),
                    femaleOnly = false,
                    isNightOwl = true,
                    notes = "Biryani jumbo family pack outing for dinner. Auto rickshaw split from main gate.",
                    status = "GATHERING"
                )
            )
        )
        save(context)
    }

    fun calculateBuddiesMet(userEmail: String): Int {
        var count = 0
        activeCircles.forEach { circle ->
            val inCircle = circle.hostEmail == userEmail || circle.members.any { it.email == userEmail }
            if (inCircle) {
                count += circle.members.size
            }
        }
        return if (count > 0) count else 8
    }

    fun save(context: Context) {
        scope.launch {
            try {
                val json = gson.toJson(activeCircles.toList())
                EncryptionManager.saveString(context, "db_meal_circles_v2", json)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun createCircle(context: Context, circle: MealCircle): Boolean {
        activeCircles.add(0, circle)
        save(context)
        notifyAllMembers(context, circle.id, "Meal circle created for ${circle.restaurantSpot}. Buddies can now join.")
        return true
    }

    fun joinCircle(context: Context, circleId: String, member: MealMember): Boolean {
        val index = activeCircles.indexOfFirst { it.id == circleId }
        if (index != -1) {
            val circle = activeCircles[index]
            if (circle.joinedBuddies >= circle.totalBuddies || circle.members.size >= circle.totalBuddies) {
                return false
            }
            if (circle.members.none { it.email == member.email }) {
                val updatedMembers = circle.members.toMutableList().apply { add(member) }
                val updatedCircle = circle.copy(
                    members = updatedMembers,
                    joinedBuddies = updatedMembers.size,
                    perPersonEstimate = if (circle.totalBuddies > 0 && circle.estimatedTotalBill > 0) ceil(circle.estimatedTotalBill.toDouble() / circle.totalBuddies.toDouble()).toInt() else 0
                )
                activeCircles[index] = updatedCircle
                save(context)
                notifyAllMembers(context, circleId, "${member.name} joined the meal circle.")
                return true
            }
        }
        return false
    }

    fun leaveCircle(context: Context, circleId: String, userEmail: String): Pair<Boolean, String> {
        val index = activeCircles.indexOfFirst { it.id == circleId }
        if (index != -1) {
            val circle = activeCircles[index]
            if (circle.hostEmail == userEmail) {
                return Pair(false, "Host cannot leave directly. Please cancel the circle.")
            }
            val member = circle.members.find { it.email == userEmail }
            if (member != null) {
                val updatedMembers = circle.members.toMutableList().apply { remove(member) }
                val updatedCircle = circle.copy(
                    members = updatedMembers,
                    joinedBuddies = updatedMembers.size,
                    perPersonEstimate = if (circle.totalBuddies > 0 && circle.estimatedTotalBill > 0) ceil(circle.estimatedTotalBill.toDouble() / circle.totalBuddies.toDouble()).toInt() else 0
                )
                activeCircles[index] = updatedCircle
                save(context)
                notifyAllMembers(context, circleId, "${member.name} left the meal circle.")
                return Pair(true, "You left the meal circle.")
            }
        }
        return Pair(false, "Meal circle not found.")
    }

    fun cancelCircle(context: Context, circleId: String): Pair<Boolean, String> {
        val index = activeCircles.indexOfFirst { it.id == circleId }
        if (index != -1) {
            val circle = activeCircles[index]
            notifyAllMembers(context, circleId, "This meal circle was cancelled by the host.")
            activeCircles.removeAt(index)
            save(context)
            return Pair(true, "Meal circle cancelled.")
        }
        return Pair(false, "Circle not found.")
    }

    fun completeCircle(context: Context, circleId: String) {
        val index = activeCircles.indexOfFirst { it.id == circleId }
        if (index != -1) {
            notifyAllMembers(context, circleId, "Meal completed successfully.")
            activeCircles.removeAt(index)
            save(context)
        }
    }

    fun requestHostTransfer(context: Context, circleId: String, targetEmail: String, targetName: String) {
        val index = activeCircles.indexOfFirst { it.id == circleId }
        if (index != -1) {
            val circle = activeCircles[index]
            activeCircles[index] = circle.copy(
                pendingHostTransferEmail = targetEmail,
                pendingHostTransferName = targetName
            )
            save(context)
            notifyAllMembers(context, circleId, "Host transfer requested to $targetName. Awaiting acceptance.")
        }
    }

    fun acceptHostTransfer(context: Context, circleId: String, acceptedEmail: String): Boolean {
        val index = activeCircles.indexOfFirst { it.id == circleId }
        if (index != -1) {
            val circle = activeCircles[index]
            if (circle.pendingHostTransferEmail == acceptedEmail) {
                val newHostMember = circle.members.find { it.email == acceptedEmail }
                if (newHostMember != null) {
                    val oldHostEmail = circle.hostEmail
                    val updatedMembers = circle.members.filter { it.email != oldHostEmail }
                    activeCircles[index] = circle.copy(
                        hostEmail = newHostMember.email,
                        hostName = newHostMember.name,
                        hostPhone = newHostMember.phone,
                        hostHostel = newHostMember.hostel,
                        pendingHostTransferEmail = null,
                        pendingHostTransferName = null,
                        members = updatedMembers,
                        joinedBuddies = updatedMembers.size
                    )
                    save(context)
                    notifyAllMembers(context, circleId, "${newHostMember.name} accepted the host transfer and is now the official host.")
                    return true
                }
            }
        }
        return false
    }

    fun declineHostTransfer(context: Context, circleId: String) {
        val index = activeCircles.indexOfFirst { it.id == circleId }
        if (index != -1) {
            val circle = activeCircles[index]
            activeCircles[index] = circle.copy(
                pendingHostTransferEmail = null,
                pendingHostTransferName = null
            )
            save(context)
            notifyAllMembers(context, circleId, "Host transfer request was declined.")
        }
    }

    fun updateOrderTracking(context: Context, circleId: String, trackingText: String) {
        val index = activeCircles.indexOfFirst { it.id == circleId }
        if (index != -1) {
            val circle = activeCircles[index]
            activeCircles[index] = circle.copy(
                orderTrackingDetails = trackingText.trim(),
                status = "ORDER_PLACED"
            )
            save(context)
            notifyAllMembers(context, circleId, "Order update: $trackingText")
        }
    }

    fun addChatMessage(context: Context, circleId: String, chat: MealChatMessage) {
        val index = activeCircles.indexOfFirst { it.id == circleId }
        if (index != -1) {
            val circle = activeCircles[index]
            val updatedChats = circle.chatMessages.toMutableList().apply { add(chat) }
            activeCircles[index] = circle.copy(chatMessages = updatedChats)
            save(context)
        }
    }

    fun notifyAllMembers(context: Context, circleId: String, messageText: String) {
        val circle = activeCircles.find { it.id == circleId } ?: return
        val systemChat = MealChatMessage(
            senderEmail = "system@campusapp",
            senderName = "Campus Meal Bot",
            message = messageText
        )
        val updatedChats = circle.chatMessages.toMutableList().apply { add(systemChat) }
        val index = activeCircles.indexOfFirst { it.id == circleId }
        if (index != -1) {
            activeCircles[index] = circle.copy(chatMessages = updatedChats)
            save(context)
        }

        // Only notify if the active device user is a joined member or the host of this meal circle
        val currentUserEmail = EncryptionManager.getString(context, "user_email") ?: ""
        val isMember = currentUserEmail.isNotBlank() && (
            circle.hostEmail.equals(currentUserEmail, ignoreCase = true) ||
            circle.members.any { it.email.equals(currentUserEmail, ignoreCase = true) }
        )

        if (isMember) {
            try {
                SystemNotificationHelper.showNotification(
                    context = context,
                    title = "Meal Circle: ${circle.restaurantSpot}",
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
        val expired = activeCircles.filter { it.targetTimestamp <= now || it.status == "COMPLETED" || it.status == "CANCELLED" }
        if (expired.isNotEmpty()) {
            activeCircles.removeAll(expired)
            save(context)
        }
    }
}