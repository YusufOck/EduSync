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
import androidx.compose.material.icons.automirrored.filled.Send
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
    val isAdminEditing by viewModel.isAdminEditing.collectAsState()
    val isTeacherEditing by viewModel.isTeacherEditing.collectAsState()

    var showNoteDialog by remember { mutableStateOf(false) }
    var noteInput by remember { mutableStateOf("") }

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
                                ScheduleStatus.PENDING -> "Admin Onayı Bekliyor"
                                ScheduleStatus.APPROVED -> "Onaylandı"
                                ScheduleStatus.REJECTED -> "Revize İsteniyor"
                                ScheduleStatus.ADMIN_PROPOSAL -> "Admin Öneri Sundu"
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
                        // ADMIN EDIT TOGGLE
                        IconButton(onClick = { 
                            if (isAdminEditing) viewModel.cancelAdminEdit() else viewModel.startAdminEdit()
                        }) {
                            Icon(
                                if (isAdminEditing) Icons.Default.EditOff else Icons.Default.Edit, 
                                contentDescription = "Düzenle",
                                tint = if (isAdminEditing) MaterialTheme.colorScheme.primary else LocalContentColor.current
                            )
                        }
                    } else {
                        // TEACHER EDIT TOGGLE
                        val status = teacher?.scheduleStatus
                        if (status != ScheduleStatus.PENDING && status != ScheduleStatus.ADMIN_PROPOSAL) {
                            IconButton(onClick = { 
                                if (isTeacherEditing) viewModel.cancelTeacherEdit() else viewModel.startTeacherEdit()
                            }) {
                                Icon(
                                    if (isTeacherEditing) Icons.Default.EditOff else Icons.Default.Edit, 
                                    contentDescription = "Düzenle",
                                    tint = if (isTeacherEditing) MaterialTheme.colorScheme.primary else LocalContentColor.current
                                )
                            }
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
                // --- ADMIN BOTTOM BAR ---
                BottomAppBar {
                    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (isAdminEditing) {
                            Button(onClick = { showNoteDialog = true }, modifier = Modifier.weight(1f)) {
                                Icon(Icons.AutoMirrored.Filled.Send, null)
                                Spacer(Modifier.width(4.dp))
                                Text("HOCAYA SUN")
                            }
                            OutlinedButton(onClick = { viewModel.cancelAdminEdit() }, modifier = Modifier.weight(1f)) {
                                Text("VAZGEÇ")
                            }
                        } else if (teacher?.scheduleStatus == ScheduleStatus.PENDING) {
                            Button(
                                onClick = { viewModel.approveTeacherRequest("Admin isteğinizi onayladı.") },
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
                // --- TEACHER BOTTOM BAR ---
                val status = teacher?.scheduleStatus
                BottomAppBar {
                    if (status == ScheduleStatus.ADMIN_PROPOSAL) {
                        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { viewModel.approveAdminProposal() },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                            ) {
                                Icon(Icons.Default.Check, null)
                                Spacer(Modifier.width(4.dp))
                                Text("ÖNERİYİ ONAYLA")
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
                    } else if (isTeacherEditing) {
                        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = { showNoteDialog = true }, modifier = Modifier.weight(1f)) {
                                Icon(Icons.AutoMirrored.Filled.Send, null)
                                Spacer(Modifier.width(4.dp))
                                Text("ADMİNE GÖNDER")
                            }
                            OutlinedButton(onClick = { viewModel.cancelTeacherEdit() }, modifier = Modifier.weight(1f)) {
                                Text("VAZGEÇ")
                            }
                        }
                    } else {
                        // Normal view mode for teacher
                        Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                            Text(
                                text = if (status == ScheduleStatus.PENDING) "İsteğiniz Admin onayında..." else "Düzenlemek için kalem ikonuna tıklayın",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.Gray
                            )
                        }
                    }
                }
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            
            // --- DYNAMIC MESSAGE BOX ---
            val messageToShow = if (isReadOnly) {
                // Admin ekranında hocanın notu varsa göster (PENDING ise)
                if (teacher?.scheduleStatus == ScheduleStatus.PENDING && !teacher?.teacherNote.isNullOrEmpty()) teacher?.teacherNote else ""
            } else {
                // Hoca ekranında adminin notu varsa göster (REJECTED veya PROPOSAL ise)
                if ((teacher?.scheduleStatus == ScheduleStatus.REJECTED || teacher?.scheduleStatus == ScheduleStatus.ADMIN_PROPOSAL) 
                    && !teacher?.adminNote.isNullOrEmpty()) teacher?.adminNote else ""
            }

            if (!messageToShow.isNullOrEmpty()) {
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    modifier = Modifier.fillMaxWidth().padding(8.dp),
                    shape = MaterialTheme.shapes.small
                ) {
                    Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isReadOnly) "Hoca Notu: $messageToShow" else "Admin Mesajı: $messageToShow",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
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

            val isInteractionEnabled = isAdminEditing || isTeacherEditing
            
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
                                    .clickable(enabled = !isLunch && isInteractionEnabled) {
                                        viewModel.toggleAvailability(dayIndex, slotIndex)
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
                title = { 
                    Text(when {
                        isReadOnly && isAdminEditing -> "Hocaya İletilecek Mesaj"
                        isReadOnly -> "Reddetme Sebebi"
                        teacher?.scheduleStatus == ScheduleStatus.ADMIN_PROPOSAL -> "Reddetme Sebebi"
                        else -> "Admin'e İletilecek Not"
                    }) 
                },
                text = {
                    OutlinedTextField(
                        value = noteInput,
                        onValueChange = { noteInput = it },
                        placeholder = { Text("Buraya yazın...") },
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        if (isReadOnly) {
                            if (isAdminEditing) viewModel.submitAdminProposal(noteInput)
                            else viewModel.rejectTeacherRequest(noteInput)
                        } else {
                            if (teacher?.scheduleStatus == ScheduleStatus.ADMIN_PROPOSAL) viewModel.rejectAdminProposal(noteInput)
                            else viewModel.sendTeacherRequest(noteInput)
                        }
                        showNoteDialog = false
                        noteInput = ""
                    }) { Text("GÖNDER") }
                },
                dismissButton = {
                    TextButton(onClick = { showNoteDialog = false }) { Text("İPTAL") }
                }
            )
        }
    }
}
