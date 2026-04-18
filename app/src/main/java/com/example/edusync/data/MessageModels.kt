package com.example.edusync.data

import com.google.firebase.database.PropertyName

data class Message(
    var id: String = "",
    var senderId: String = "", 
    var receiverId: String = "", 
    var content: String = "",
    var timestamp: Long = System.currentTimeMillis(),
    @get:PropertyName("read")
    @set:PropertyName("read")
    @PropertyName("read")
    var isRead: Boolean = false,
    // Add flags for local deletion (won't affect other party)
    var deletedBySender: Boolean = false,
    var deletedByReceiver: Boolean = false
)

data class ChatSummary(
    val otherUserId: String, 
    val otherUserName: String,
    val lastMessage: String,
    val lastMessageTimestamp: Long,
    val unreadCount: Int = 0
)
