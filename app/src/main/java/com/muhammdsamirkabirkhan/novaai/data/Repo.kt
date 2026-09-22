package com.muhammdsamirkabirkhan.novaai.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.concurrent.TimeUnit

/**
 * All server access. The app NEVER talks to an AI provider or Google Play API directly:
 * it only calls your Firebase backend (which holds every secret) and reads its own Firestore data.
 */
object Repo {
    private val auth get() = FirebaseAuth.getInstance()
    private val db get() = FirebaseFirestore.getInstance()
    private val fns get() = FirebaseFunctions.getInstance()
    private val me get() = auth.currentUser?.uid ?: error("Not signed in")

    fun profile(uid: String): Flow<UserProfile?> = callbackFlow {
        val reg = db.collection("users").document(uid).addSnapshotListener { s, e ->
            trySend(if (e != null) null else s?.toObject(UserProfile::class.java))
        }
        awaitClose { reg.remove() }
    }

    fun chats(): Flow<List<ChatSummary>> = callbackFlow {
        val reg = db.collection("users/$me/chats")
            .orderBy("updatedAt", Query.Direction.DESCENDING).limit(200)
            .addSnapshotListener { s, e ->
                trySend(if (e != null || s == null) emptyList() else s.toObjects(ChatSummary::class.java))
            }
        awaitClose { reg.remove() }
    }

    fun messages(chatId: String): Flow<List<Message>> = callbackFlow {
        val reg = db.collection("users/$me/chats/$chatId/messages")
            .orderBy("createdAt", Query.Direction.ASCENDING)
            .addSnapshotListener { s, e ->
                trySend(if (e != null || s == null) emptyList() else s.toObjects(Message::class.java))
            }
        awaitClose { reg.remove() }
    }

    private suspend fun call(name: String, data: Map<String, Any?> = emptyMap()): Map<String, Any?> {
        val c = fns.getHttpsCallable(name).apply { setTimeout(90, TimeUnit.SECONDS) }
        @Suppress("UNCHECKED_CAST")
        return (c.call(data.filterValues { it != null }).await().data as? Map<String, Any?>) ?: emptyMap()
    }

    suspend fun bootstrap(name: String? = null) = runCatching { call("bootstrap", mapOf("name" to name)) }

    suspend fun chat(chatId: String?, message: String) = runCatching {
        val r = call("chat", mapOf("chatId" to chatId, "message" to message))
        ChatReply(r["chatId"] as String, r["reply"] as String)
    }

    suspend fun runTool(tool: String, text: String, option: String) = runCatching {
        call("runTool", mapOf("tool" to tool, "text" to text, "option" to option))["result"] as String
    }

    suspend fun deleteChat(id: String) = runCatching { call("deleteChat", mapOf("chatId" to id)); Unit }

    suspend fun report(chatId: String, messageId: String) =
        runCatching { call("reportContent", mapOf("chatId" to chatId, "messageId" to messageId, "reason" to "harmful_or_offensive")); Unit }

    suspend fun verifyPurchase(token: String, productId: String) =
        runCatching { call("verifyPurchase", mapOf("purchaseToken" to token, "productId" to productId)); Unit }

    suspend fun syncSubscription() = runCatching { call("syncSubscription"); Unit }

    suspend fun rename(name: String) =
        runCatching { db.document("users/$me").update("displayName", name).await(); Unit }

    suspend fun deleteAccount() = runCatching { call("deleteAccount"); Unit }
}
