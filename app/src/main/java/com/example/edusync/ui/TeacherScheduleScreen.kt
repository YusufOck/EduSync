package com.example.edusync.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Book
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeacherScheduleScreen(
    teacherId: Int,
    isReadOnly: Boolean = false,
    onNavigateBack: (() -> Unit)? = null,
    onLogout: (() -> Unit)? = null,
    viewModel: TeacherViewModel = hiltViewModel()
) {
    LaunchedEffect(teacherId) {
        viewModel.selectTeacher(teacherId)
    }

    val teachers by viewModel.teachers.collectAsState()
    val availability by viewModel.availabilityMatrix.collectAsState()
    val courses by viewModel.teacherCourses.collectAsState()
    val currentTeacher = teachers.find { it.id == teacherId }

    val days = listOf("Pzt", "Sal", "Çar", "Per", "Cum")
    val timeSlots = listOf(
        "08:30", "09:30", "10:30", "11:30", 
        "12:30", // Öğle Arası
        "13:30", "14:30", "15:30", "16:30"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text(currentTeacher?.let { "${it.title} ${it.name} ${it.surname}" } ?: "Müsaitlik Çizelgesi")
                        if (currentTeacher?.department?.isNotEmpty() == true) {
                            Text(currentTeacher.department, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                },
                navigationIcon = {
                    if (onNavigateBack != null) {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri")
                        }
                    }
                },
                actions = {
                    if (onLogout != null) {
                        IconButton(onClick = onLogout) {
                            Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Çıkış Yap")
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            
            // Dersler Listesi Section
            if (courses.isNotEmpty()) {
                Text(
                    "Verdiği Dersler",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.primary
                )
                LazyColumn(
                    modifier = Modifier.heightIn(max = 120.dp).fillMaxWidth().padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(courses) { course ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Row(
                                modifier = Modifier.padding(8.dp).fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Book, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("${course.code} - ${course.name}", fontSize = 12.sp)
                            }
                        }
                    }
                }
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            }

            // Çizelge Başlığı (Günler)
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                Spacer(modifier = Modifier.width(60.dp))
                days.forEach { day ->
                    Text(
                        text = day,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Saatler ve Kutucuklar
            LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
                itemsIndexed(timeSlots) { slotIndex, time ->
                    Row(
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Saat Etiketi
                        Text(
                            text = time,
                            modifier = Modifier.width(60.dp),
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center
                        )

                        // Gün Kutucukları
                        for (dayIndex in 0..4) {
                            val isBusy = availability.find { 
                                it.dayIndex == dayIndex && it.slotIndex == slotIndex 
                            }?.isBusy ?: false
                            
                            val isLunchBreak = slotIndex == 4

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .padding(2.dp)
                                    .background(
                                        when {
                                            isLunchBreak -> Color.LightGray.copy(alpha = 0.5f)
                                            isBusy -> Color(0xFFEF5350)
                                            else -> Color(0xFF66BB6A)
                                        }
                                    )
                                    .border(0.5.dp, Color.LightGray)
                                    .clickable(enabled = !isLunchBreak && !isReadOnly) {
                                        viewModel.toggleAvailability(dayIndex, slotIndex)
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                if (isLunchBreak) {
                                    Text("Öğle", fontSize = 10.sp, color = Color.DarkGray)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
