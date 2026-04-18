package com.example.edusync.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.edusync.data.ChatSummary
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminMessagesScreen(
    onChatClick: (String) -> Unit,
    viewModel: ChatViewModel = hiltViewModel()
) {
    val chatSummaries by viewModel.chatSummaries.collectAsState()
    val messageableTeachers by viewModel.messageableTeachers.collectAsState()
    var showNewChatDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.initUser("admin")
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Sohbetler") })
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showNewChatDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Yeni Sohbet")
            }
        }
    ) { padding ->
        if (chatSummaries.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("Henüz mesaj yok.", color = Color.Gray)
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
                items(chatSummaries) { summary ->
                    ChatSummaryItem(
                        summary = summary,
                        onClick = { onChatClick(summary.otherUserId) },
                        onDelete = { viewModel.deleteChat(summary.otherUserId) }
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp)
                }
            }
        }

        if (showNewChatDialog) {
            NewChatDialog(
                teachers = messageableTeachers,
                onDismiss = { showNewChatDialog = false },
                onTeacherSelected = { username ->
                    showNewChatDialog = false
                    onChatClick(username)
                }
            )
        }
    }
}

@Composable
fun ChatSummaryItem(
    summary: ChatSummary,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier.size(50.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Person, null, modifier = Modifier.size(30.dp))
            }
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = summary.otherUserName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = timeFormat.format(Date(summary.lastMessageTimestamp)),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = summary.lastMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    color = if (summary.unreadCount > 0) MaterialTheme.colorScheme.onSurface else Color.Gray,
                    fontWeight = if (summary.unreadCount > 0) FontWeight.Bold else FontWeight.Normal,
                    modifier = Modifier.weight(1f)
                )
                if (summary.unreadCount > 0) {
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .background(MaterialTheme.colorScheme.primary, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = summary.unreadCount.toString(),
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, null, tint = Color.LightGray, modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}

@Composable
fun NewChatDialog(
    teachers: List<Pair<String, String>>,
    onDismiss: () -> Unit,
    onTeacherSelected: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Mesaj Gönderilecek Hoca") },
        text = {
            LazyColumn(modifier = Modifier.heightIn(max = 400.dp)) {
                items(teachers) { (username, fullName) ->
                    TextButton(
                        onClick = { onTeacherSelected(username) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = fullName,
                            modifier = Modifier.fillMaxWidth(),
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Start
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("İptal") }
        }
    )
}
