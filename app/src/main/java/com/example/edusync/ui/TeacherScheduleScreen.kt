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
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.edusync.data.ScheduleStatus

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

    val teacher by viewModel.currentTeacher.collectAsState()
    val availability by viewModel.availabilityMatrix.collectAsState()
    val courses by viewModel.teacherCourses.collectAsState()

    var isAdminEditing by remember { mutableStateOf(false) }
    var hasAdminMadeChanges by remember { mutableStateOf(false) }
    var showNoteDialog by remember { mutableStateOf(false) }
    var adminNote by remember { mutableStateOf("") }

    val days = listOf("Pzt", "Sal", "Çar", "Per", "Cum")
    val timeSlots = listOf("08:30", "09:30", "10:30", "11:30", "12:30", "13:30", "14:30", "15:30", "16:30")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text(teacher?.let { "${it.title} ${it.name} ${it.surname}" } ?: "Yükleniyor...")
                        teacher?.let { 
                            val statusText = when(it.scheduleStatus) {
                                ScheduleStatus.PENDING -> "Onay Bekliyor"
                                ScheduleStatus.APPROVED -> "Onaylandı"
                                ScheduleStatus.REJECTED -> "Reddedildi"
                                ScheduleStatus.ADMIN_PROPOSAL -> "Admin Önerisi Bekliyor"
                            }
                            Text("Durum: $statusText", style = MaterialTheme.typography.bodySmall)
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
                    if (isReadOnly) {
                        IconButton(onClick = { 
                            isAdminEditing = !isAdminEditing 
                            if (!isAdminEditing) hasAdminMadeChanges = false 
                        }) {
                            Icon(
                                if (isAdminEditing) Icons.Default.EditOff else Icons.Default.Edit, 
                                contentDescription = "Düzenle",
                                tint = if (isAdminEditing) MaterialTheme.colorScheme.primary else LocalContentColor.current
                            )
                        }
                    }
                    if (onLogout != null) {
                        IconButton(onClick = onLogout) {
                            Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Çıkış Yap")
                        }
                    }
                }
            )
        },
        bottomBar = {
            if (isReadOnly) {
                // Admin Panelinde
                BottomAppBar {
                    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (isAdminEditing && hasAdminMadeChanges) {
                            Button(
                                onClick = { 
                                    viewModel.updateScheduleStatus(ScheduleStatus.ADMIN_PROPOSAL)
                                    isAdminEditing = false
                                    hasAdminMadeChanges = false
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Send, null)
                                Spacer(Modifier.width(4.dp))
                                Text("HOCAYA SUN")
                            }
                        } else if (teacher?.scheduleStatus == ScheduleStatus.PENDING) {
                            Button(
                                onClick = { viewModel.updateScheduleStatus(ScheduleStatus.APPROVED) },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                            ) {
                                Icon(Icons.Default.Check, null)
                                Spacer(Modifier.width(4.dp))
                                Text("ONAYLA")
                            }
                            Button(
                                onClick = { showNoteDialog = true },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                            ) {
                                Icon(Icons.Default.Close, null)
                                Spacer(Modifier.width(4.dp))
                                Text("REDDET")
                            }
                        }
                    }
                }
            } else {
                // Hoca Panelinde
                val status = teacher?.scheduleStatus
                BottomAppBar {
                    if (status == ScheduleStatus.ADMIN_PROPOSAL) {
                        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { viewModel.updateScheduleStatus(ScheduleStatus.APPROVED) },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                            ) {
                                Text("ÖNERİYİ ONAYLA")
                            }
                            Button(
                                onClick = { viewModel.updateScheduleStatus(ScheduleStatus.REJECTED, "Admin önerisini kabul etmedi.") },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                            ) {
                                Text("REDDET")
                            }
                        }
                    } else if (status == ScheduleStatus.REJECTED || status == ScheduleStatus.PENDING) {
                        Button(
                            onClick = { viewModel.updateScheduleStatus(ScheduleStatus.PENDING) },
                            modifier = Modifier.fillMaxWidth().padding(8.dp)
                        ) {
                            Icon(Icons.Default.Send, null)
                            Spacer(Modifier.width(8.dp))
                            Text("ADMİNE GÖNDER")
                        }
                    } else if (status == ScheduleStatus.APPROVED) {
                        Button(
                            onClick = { viewModel.updateScheduleStatus(ScheduleStatus.REJECTED, "Hoca yeni değişiklik talep etti.") },
                            modifier = Modifier.fillMaxWidth().padding(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                        ) {
                            Text("YENİ DEĞİŞİKLİK İSTE")
                        }
                    }
                }
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            
            if (!isReadOnly && teacher?.adminNote?.isNotEmpty() == true) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    modifier = Modifier.fillMaxWidth().padding(8.dp),
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        "Bilgi: ${teacher?.adminNote}",
                        modifier = Modifier.padding(8.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }

            if (courses.isNotEmpty()) {
                LazyColumn(modifier = Modifier.heightIn(max = 100.dp).padding(horizontal = 16.dp)) {
                    items(courses) { course ->
                        Text("• ${course.code} - ${course.name}", fontSize = 12.sp)
                    }
                }
                HorizontalDivider(Modifier.padding(vertical = 8.dp))
            }

            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                Spacer(modifier = Modifier.width(60.dp))
                days.forEach { Text(it, modifier = Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.Center, fontWeight = FontWeight.Bold) }
            }

            val canEdit = (!isReadOnly && (teacher?.scheduleStatus == ScheduleStatus.REJECTED || teacher?.scheduleStatus == ScheduleStatus.PENDING)) || isAdminEditing
            
            LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
                itemsIndexed(timeSlots) { slotIndex, time ->
                    Row(modifier = Modifier.fillMaxWidth().height(45.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(time, modifier = Modifier.width(60.dp), fontSize = 11.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                        for (dayIndex in 0..4) {
                            val isBusy = availability.find { it.dayIndex == dayIndex && it.slotIndex == slotIndex }?.isBusy ?: false
                            val isLunch = slotIndex == 4
                            Box(
                                modifier = Modifier
                                    .weight(1f).fillMaxHeight().padding(1.dp)
                                    .background(when {
                                        isLunch -> Color.LightGray.copy(alpha = 0.4f)
                                        isBusy -> Color(0xFFEF5350)
                                        else -> Color(0xFF66BB6A)
                                    })
                                    .border(0.5.dp, Color.LightGray)
                                    .clickable(enabled = !isLunch && canEdit) {
                                        viewModel.toggleAvailability(dayIndex, slotIndex)
                                        if (isReadOnly && isAdminEditing) hasAdminMadeChanges = true
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                if (isLunch) Text("Öğle", fontSize = 9.sp)
                            }
                        }
                    }
                }
            }
        }

        if (showNoteDialog) {
            AlertDialog(
                onDismissRequest = { showNoteDialog = false },
                title = { Text("Reddetme Sebebi") },
                text = {
                    OutlinedTextField(
                        value = adminNote,
                        onValueChange = { adminNote = it },
                        label = { Text("Hocaya mesajınız...") },
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.updateScheduleStatus(ScheduleStatus.REJECTED, adminNote)
                        showNoteDialog = false
                    }) { Text("GÖNDER") }
                }
            )
        }
    }
}
