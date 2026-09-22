package com.muhammdsamirkabirkhan.novaai.data

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.Exclude
import java.time.LocalDate
import java.time.ZoneOffset

/** Mirror of users/{uid}. Plan/usage fields are written ONLY by the backend. */
data class UserProfile(
    val displayName: String = "",
    val email: String = "",
    val plan: String = "free",
    val premiumUntil: Timestamp? = null,
    val productId: String? = null,
    val basePlanId: String? = null,
    val autoRenewing: Boolean = false,
    val dailyLimit: Long = 0,
    val usedToday: Long = 0,
    val usageDate: String = "",
    val bonusCredits: Long = 0,
) {
    @get:Exclude
    val premium: Boolean
        get() = plan == "premium" && (premiumUntil?.toDate()?.time ?: 0L) > System.currentTimeMillis()

    @get:Exclude
    val remainingToday: Long
        get() = if (usageDate == LocalDate.now(ZoneOffset.UTC).toString())
            (dailyLimit - usedToday).coerceAtLeast(0) else dailyLimit
}

data class ChatSummary(
    @DocumentId val id: String = "",
    val title: String = "",
    val updatedAt: Timestamp? = null,
)

data class Message(
    @DocumentId val id: String = "",
    val role: String = "user",
    val content: String = "",
    val createdAt: Timestamp? = null,
)

data class ChatReply(val chatId: String, val reply: String)
