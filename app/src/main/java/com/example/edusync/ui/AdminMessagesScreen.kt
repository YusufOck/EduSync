package com.example.edusync.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.example.edusync.ui.theme.*
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
            TopAppBar(
                title = { Text("Mesaj Kutusu", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = PrimaryBlue
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showNewChatDialog = true },
                containerColor = PrimaryBlue,
                contentColor = Color.White,
                shape = CircleShape
            ) {
                Icon(Icons.Default.Add, contentDescription = "Yeni Sohbet")
            }
        }
    ) { padding ->
        if (chatSummaries.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding).background(BackgroundLight), contentAlignment = Alignment.Center) {
                Text("Henüz mesaj yok.", color = Color.Gray)
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(padding).background(BackgroundLight)) {
                items(chatSummaries) { summary ->
                    ChatSummaryItem(
                        summary = summary,
                        onClick = { onChatClick(summary.otherUserId) },
                        onDelete = { viewModel.deleteChat(summary.otherUserId) }
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = Color.LightGray.copy(alpha = 0.3f))
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
            .background(Color.White)
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier.size(54.dp),
            shape = CircleShape,
            color = LightBlue
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Person, null, modifier = Modifier.size(30.dp), tint = PrimaryBlue)
            }
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = summary.otherUserName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextDark,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = timeFormat.format(Date(summary.lastMessageTimestamp)),
                    style = MaterialTheme.typography.bodySmall,
                    color = TextLight
                )
            }
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = summary.lastMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    color = if (summary.unreadCount > 0) TextDark else TextLight,
                    fontWeight = if (summary.unreadCount > 0) FontWeight.Bold else FontWeight.Normal,
                    modifier = Modifier.weight(1f)
                )
                if (summary.unreadCount > 0) {
                    Box(
                        modifier = Modifier
                            .padding(start = 8.dp)
                            .size(22.dp)
                            .background(PrimaryBlue, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = summary.unreadCount.toString(),
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, null, tint = Color.LightGray.copy(alpha = 0.6f), modifier = Modifier.size(20.dp))
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
        shape = RoundedCornerShape(24.dp),
        containerColor = Color.White,
        title = { Text("Mesaj Gönderilecek Hoca", color = PrimaryBlue, fontWeight = FontWeight.Bold) },
        text = {
            LazyColumn(modifier = Modifier.heightIn(max = 400.dp)) {
                items(teachers) { (username, fullName) ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable { onTeacherSelected(username) },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = BackgroundLight)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Person, null, tint = PrimaryBlue, modifier = Modifier.size(24.dp))
                            Spacer(Modifier.width(12.dp))
                            Text(text = fullName, fontWeight = FontWeight.Medium, color = TextDark)
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("İPTAL", color = Color.Gray) }
        }
    )
}
