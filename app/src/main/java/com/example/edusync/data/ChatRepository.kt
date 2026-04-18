package com.example.edusync.data

import com.example.edusync.util.SecurityUtils
import com.google.firebase.database.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatRepository @Inject constructor(
    private val database: FirebaseDatabase
) {
    private val messagesRef = database.getReference("messages")

    suspend fun sendMessage(message: Message) {
        val chatId = getChatId(message.senderId, message.receiverId)
        val ref = messagesRef.child(chatId).push()
        val encryptedMessage = message.copy(
            id = ref.key ?: "",
            content = SecurityUtils.encrypt(message.content)
        )
        ref.setValue(encryptedMessage).await()
    }

    fun getMessages(currentUserId: String, targetUserId: String): Flow<List<Message>> = callbackFlow {
        val chatId = getChatId(currentUserId, targetUserId)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = snapshot.children.mapNotNull { it.getValue(Message::class.java)?.let { msg ->
                    msg.copy(content = SecurityUtils.decrypt(msg.content))
                }}.filter { msg ->
                    // Only show messages NOT deleted by the current user
                    if (msg.senderId == currentUserId) !msg.deletedBySender
                    else !msg.deletedByReceiver
                }
                trySend(list)
            }
            override fun onCancelled(error: DatabaseError) { close(error.toException()) }
        }
        messagesRef.child(chatId).addValueEventListener(listener)
        awaitClose { messagesRef.child(chatId).removeEventListener(listener) }
    }

    suspend fun markAsRead(chatId: String, currentUserId: String) {
        val snapshot = messagesRef.child(chatId).get().await()
        snapshot.children.forEach { child ->
            val msg = child.getValue(Message::class.java)
            if (msg != null && msg.receiverId == currentUserId && !msg.isRead) {
                child.ref.child("read").setValue(true)
            }
        }
    }

    /**
     * Logical deletion for the current user only.
     */
    suspend fun clearChatForUser(currentUserId: String, targetUserId: String) {
        val chatId = getChatId(currentUserId, targetUserId)
        val snapshot = messagesRef.child(chatId).get().await()
        snapshot.children.forEach { child ->
            val msg = child.getValue(Message::class.java) ?: return@forEach
            val updates = mutableMapOf<String, Any>()
            if (msg.senderId == currentUserId) {
                updates["deletedBySender"] = true
            } else {
                updates["deletedByReceiver"] = true
            }
            child.ref.updateChildren(updates).await()
        }
    }

    private fun getChatId(u1: String, u2: String): String {
        return if (u1 < u2) "${u1}_${u2}" else "${u2}_${u1}"
    }

    fun getAllMessagesForAdmin(): Flow<Map<String, List<Message>>> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val allChats = mutableMapOf<String, List<Message>>()
                snapshot.children.forEach { chatSnapshot ->
                    val messages = chatSnapshot.children.mapNotNull { msgSnap ->
                        msgSnap.getValue(Message::class.java)?.let { msg ->
                            // Admin only sees what hasn't been deleted by "admin"
                            val isDeletedByAdmin = (msg.senderId == "admin" && msg.deletedBySender) || 
                                                 (msg.receiverId == "admin" && msg.deletedByReceiver)
                            
                            if (!isDeletedByAdmin) {
                                msg.copy(content = SecurityUtils.decrypt(msg.content))
                            } else null
                        }
                    }
                    if (messages.isNotEmpty()) {
                        allChats[chatSnapshot.key ?: ""] = messages
                    }
                }
                trySend(allChats)
            }
            override fun onCancelled(error: DatabaseError) { close(error.toException()) }
        }
        messagesRef.addValueEventListener(listener)
        awaitClose { messagesRef.removeEventListener(listener) }
    }
}
