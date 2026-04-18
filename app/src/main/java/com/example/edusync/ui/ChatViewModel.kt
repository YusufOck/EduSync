package com.example.edusync.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.edusync.data.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val userRepository: FirebaseUserRepository,
    private val teacherRepository: TeacherRepository
) : ViewModel() {

    private val _currentUserId = MutableStateFlow<String?>(null)
    val currentUserId = _currentUserId.asStateFlow()

    private val _targetUserId = MutableStateFlow<String?>(null)
    val targetUserId = _targetUserId.asStateFlow()

    fun initUser(userId: String) {
        _currentUserId.value = userId
    }

    fun setTargetUser(userId: String) {
        _targetUserId.value = userId
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val messages = combine(_currentUserId, _targetUserId) { current, target ->
        if (current == null || target == null) flowOf(emptyList<Message>())
        else chatRepository.getMessages(current, target)
    }.flatMapLatest { it }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val chatSummaries = _currentUserId.flatMapLatest { currentId ->
        if (currentId == null) return@flatMapLatest flowOf(emptyList<ChatSummary>())
        
        combine(
            chatRepository.getAllMessagesForAdmin(),
            teacherRepository.getAllTeachers(),
            userRepository.getAllUsersFlow()
        ) { allMessages, teachers, users ->
            allMessages.mapNotNull { (_, messages) ->
                val lastMsg = messages.lastOrNull() ?: return@mapNotNull null
                val otherId = if (lastMsg.senderId == "admin") lastMsg.receiverId else lastMsg.senderId
                
                if (otherId == "admin") return@mapNotNull null

                val user = users.find { it.username == otherId }
                val teacher = teachers.find { it.id == user?.teacherId }
                val name = if (teacher != null) "${teacher.title} ${teacher.name} ${teacher.surname}" else otherId

                ChatSummary(
                    otherUserId = otherId,
                    otherUserName = name,
                    lastMessage = lastMsg.content,
                    lastMessageTimestamp = lastMsg.timestamp,
                    unreadCount = messages.count { it.receiverId == currentId && !it.isRead }
                )
            }.sortedByDescending { it.lastMessageTimestamp }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val messageableTeachers = combine(
        teacherRepository.getAllTeachers(),
        userRepository.getAllUsersFlow()
    ) { teachers, users ->
        users.filter { it.role == UserRole.TEACHER }.mapNotNull { user ->
            val teacher = teachers.find { it.id == user.teacherId }
            if (teacher != null) {
                user.username to "${teacher.title} ${teacher.name} ${teacher.surname}"
            } else null
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun sendMessage(content: String) {
        val sender = _currentUserId.value ?: return
        val receiver = _targetUserId.value ?: return
        if (content.isBlank()) return

        viewModelScope.launch {
            val message = Message(
                senderId = sender,
                receiverId = receiver,
                content = content
            )
            chatRepository.sendMessage(message)
        }
    }

    fun markAsRead() {
        val current = _currentUserId.value ?: return
        val target = _targetUserId.value ?: return
        val chatId = if (current < target) "${current}_${target}" else "${target}_${current}"
        viewModelScope.launch {
            chatRepository.markAsRead(chatId, current)
        }
    }

    fun deleteChat(otherUserId: String) {
        val current = _currentUserId.value ?: return
        viewModelScope.launch {
            chatRepository.clearChatForUser(current, otherUserId)
        }
    }
}
