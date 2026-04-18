package com.example.edusync.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.edusync.data.ChatSummary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeacherMessagesScreen(
    currentUsername: String,
    onChatClick: (String) -> Unit,
    viewModel: ChatViewModel = hiltViewModel()
) {
    val chatSummaries by viewModel.chatSummaries.collectAsState()

    LaunchedEffect(currentUsername) {
        viewModel.initUser(currentUsername)
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Mesajlar") })
        }
    ) { padding ->
        if (chatSummaries.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Henüz mesaj yok.", color = Color.Gray)
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { onChatClick("admin") }) {
                        Text("Admin'e Mesaj Gönder")
                    }
                }
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
    }
}
