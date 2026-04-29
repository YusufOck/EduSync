package com.example.edusync.data

import com.example.edusync.util.SecurityUtils
import com.google.firebase.database.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatRepository @Inject constructor(
    private val database: FirebaseDatabase
) {
    private val messagesRef = database.getReference("messages")

    suspend fun sendMessage(message: Message) = withContext(Dispatchers.IO) {
        val chatId = getChatId(message.senderId, message.receiverId)
        val ref = messagesRef.child(chatId).push()
        
        val encryptedContent = withContext(Dispatchers.Default) {
            SecurityUtils.encrypt(message.content)
        }
        
        val encryptedMessage = message.copy(
            id = ref.key ?: "",
            content = encryptedContent
        )
        ref.setValue(encryptedMessage).await()
    }

    fun getMessages(currentUserId: String, targetUserId: String): Flow<List<Message>> = callbackFlow {
        val chatId = getChatId(currentUserId, targetUserId)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                launch(Dispatchers.Default) {
                    val messages = snapshot.children.mapNotNull { it.getValue(Message::class.java)?.let { msg ->
                        msg.copy(content = SecurityUtils.decrypt(msg.content))
                    }}.filter { msg ->
                        if (msg.senderId == currentUserId) !msg.deletedBySender
                        else !msg.deletedByReceiver
                    }
                    trySend(messages)
                }
            }
            override fun onCancelled(error: DatabaseError) { close(error.toException()) }
        }
        messagesRef.child(chatId).addValueEventListener(listener)
        awaitClose { messagesRef.child(chatId).removeEventListener(listener) }
    }.distinctUntilChanged().flowOn(Dispatchers.IO)

    suspend fun markAsRead(chatId: String, currentUserId: String) = withContext(Dispatchers.IO) {
        val snapshot = messagesRef.child(chatId).get().await()
        val updates = mutableMapOf<String, Any>()
        
        snapshot.children.forEach { child ->
            val msg = child.getValue(Message::class.java)
            if (msg != null && msg.receiverId == currentUserId && !msg.isRead) {
                updates["${child.key}/read"] = true
            }
        }
        
        if (updates.isNotEmpty()) {
            messagesRef.child(chatId).updateChildren(updates).await()
        }
    }

    suspend fun clearChatForUser(currentUserId: String, targetUserId: String) = withContext(Dispatchers.IO) {
        val chatId = getChatId(currentUserId, targetUserId)
        val snapshot = messagesRef.child(chatId).get().await()
        val updates = mutableMapOf<String, Any>()
        
        snapshot.children.forEach { child ->
            val msg = child.getValue(Message::class.java) ?: return@forEach
            val key = child.key ?: return@forEach
            if (msg.senderId == currentUserId) {
                updates["$key/deletedBySender"] = true
            } else {
                updates["$key/deletedByReceiver"] = true
            }
        }
        
        if (updates.isNotEmpty()) {
            messagesRef.child(chatId).updateChildren(updates).await()
        }
    }

    private fun getChatId(u1: String, u2: String): String {
        return if (u1 < u2) "${u1}_${u2}" else "${u2}_${u1}"
    }

    fun getAllMessagesForAdmin(): Flow<Map<String, List<Message>>> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                launch(Dispatchers.Default) {
                    val allChats = mutableMapOf<String, List<Message>>()
                    snapshot.children.forEach { chatSnapshot ->
                        val messages = chatSnapshot.children.mapNotNull { msgSnap ->
                            msgSnap.getValue(Message::class.java)?.let { msg ->
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
            }
            override fun onCancelled(error: DatabaseError) { close(error.toException()) }
        }
        messagesRef.addValueEventListener(listener)
        awaitClose { messagesRef.removeEventListener(listener) }
    }.distinctUntilChanged().flowOn(Dispatchers.IO)

    fun getTotalUnreadCount(userId: String, isAdmin: Boolean): Flow<Int> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                launch(Dispatchers.Default) {
                    var count = 0
                    if (isAdmin) {
                        snapshot.children.forEach { chatSnapshot ->
                            val hasUnread = chatSnapshot.children.any { msgSnapshot ->
                                val msg = msgSnapshot.getValue(Message::class.java)
                                msg != null && msg.receiverId == userId && !msg.isRead && !msg.deletedByReceiver
                            }
                            if (hasUnread) count++
                        }
                    } else {
                        snapshot.children.forEach { chatSnapshot ->
                            chatSnapshot.children.forEach { msgSnapshot ->
                                val msg = msgSnapshot.getValue(Message::class.java)
                                if (msg != null && msg.receiverId == userId && !msg.isRead && !msg.deletedByReceiver) {
                                    count++
                                }
                            }
                        }
                    }
                    trySend(count)
                }
            }
            override fun onCancelled(error: DatabaseError) { close(error.toException()) }
        }
        messagesRef.addValueEventListener(listener)
        awaitClose { messagesRef.removeEventListener(listener) }
    }.distinctUntilChanged().flowOn(Dispatchers.IO)
}
