package com.example.edusync.ui

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.edusync.data.Course
import com.example.edusync.data.ScheduleStatus
import com.example.edusync.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeacherScheduleScreen(
    teacherId: Int,
    isReadOnly: Boolean = false,
    onNavigateBack: (() -> Unit)? = null,
    onLogout: (() -> Unit)? = null,
    viewModel: TeacherViewModel = hiltViewModel()
) {
    LaunchedEffect(teacherId) { viewModel.selectTeacher(teacherId) }

    val teacher by viewModel.currentTeacher.collectAsState()
    val availability by viewModel.availabilityMatrix.collectAsState()
    val courses by viewModel.teacherCourses.collectAsState()
    val isAdminEditing by viewModel.isAdminEditing.collectAsState()
    val conflictError by viewModel.conflictError.collectAsState()

    var showNoteDialog by remember { mutableStateOf(false) }
    var showAssignDialog by remember { mutableStateOf(false) }
    var selectedSlot by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    var noteInput by remember { mutableStateOf("") }

    val days = listOf("Pzt", "Sal", "Çar", "Per", "Cum")
    val timeSlots = listOf("08:30", "09:30", "10:30", "11:30", "12:30", "13:30", "14:30", "15:30", "16:30")

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { 
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = teacher?.let { "${it.title} ${it.name} ${it.surname}" } ?: "Yükleniyor...",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryBlue
                        )
                        teacher?.let { StatusBadge(it.scheduleStatus, isReadOnly) }
                    }
                },
                navigationIcon = {
                    if (onNavigateBack != null) {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Geri", tint = PrimaryBlue)
                        }
                    }
                },
                actions = {
                    if (isReadOnly) {
                        IconButton(onClick = { if (isAdminEditing) viewModel.cancelAdminEdit() else viewModel.startAdminEdit() }) {
                            Icon(if (isAdminEditing) Icons.Default.EditOff else Icons.Default.Edit, "Düzenle", tint = if (isAdminEditing) PrimaryBlue else Color.Gray)
                        }
                    }
                    if (onLogout != null) {
                        IconButton(onClick = onLogout) {
                            Icon(Icons.AutoMirrored.Filled.ExitToApp, "Çıkış Yap", tint = ErrorRed)
                        }
                    }
                }
            )
        },
        bottomBar = {
            if (isReadOnly && isAdminEditing) {
                Surface(tonalElevation = 8.dp) {
                    Button(
                        onClick = { showNoteDialog = true },
                        modifier = Modifier.fillMaxWidth().padding(16.dp).height(54.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, null)
                        Spacer(Modifier.width(8.dp))
                        Text("PROGRAMI HOCAYA GÖNDER", fontWeight = FontWeight.Bold)
                    }
                }
            } else if (!isReadOnly) {
                val status = teacher?.scheduleStatus
                if (status == ScheduleStatus.ADMIN_PROPOSAL) {
                    Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(
                            onClick = { viewModel.approveAdminProposal() },
                            modifier = Modifier.weight(1f).height(54.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Icon(Icons.Default.CheckCircle, null)
                            Spacer(Modifier.width(8.dp))
                            Text("ONAYLA", fontWeight = FontWeight.Bold)
                        }
                        Button(
                            onClick = { showNoteDialog = true },
                            modifier = Modifier.weight(1f).height(54.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = ErrorRed),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Icon(Icons.Default.Cancel, null)
                            Spacer(Modifier.width(8.dp))
                            Text("REDDET")
                        }
                    }
                }
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize().background(BackgroundLight)) {
            
            val msgToShow = if (isReadOnly) {
                if (teacher?.scheduleStatus == ScheduleStatus.REJECTED) teacher?.teacherNote else ""
            } else {
                if (teacher?.scheduleStatus == ScheduleStatus.ADMIN_PROPOSAL) teacher?.adminNote else ""
            }

            AnimatedVisibility(visible = !msgToShow.isNullOrEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    colors = CardDefaults.cardColors(containerColor = LightBlue),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.ChatBubbleOutline, null, tint = PrimaryBlue)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            val label = if (isReadOnly) "Hoca'dan Gelen Gerekçe" else "Admin'den Gelen Not"
                            Text(text = label, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = PrimaryBlue)
                            Text(msgToShow ?: "", style = MaterialTheme.typography.bodyMedium, color = TextDark)
                        }
                    }
                }
            }

            AnimatedVisibility(visible = conflictError != null) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = ErrorRed.copy(alpha = 0.1f))
                ) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Error, null, tint = ErrorRed)
                        Spacer(Modifier.width(8.dp))
                        Text(conflictError ?: "", color = ErrorRed, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                        IconButton(onClick = { viewModel.clearConflictError() }) { Icon(Icons.Default.Close, null, modifier = Modifier.size(16.dp)) }
                    }
                }
            }

            Row(modifier = Modifier.fillMaxWidth().padding(top = 16.dp, start = 12.dp, end = 12.dp)) {
                Spacer(modifier = Modifier.width(54.dp))
                days.forEach { day ->
                    Text(day, modifier = Modifier.weight(1f), textAlign = TextAlign.Center, fontWeight = FontWeight.Bold, color = PrimaryBlue, fontSize = 14.sp)
                }
            }

            LazyColumn(modifier = Modifier.fillMaxSize().padding(12.dp)) {
                itemsIndexed(timeSlots) { slotIndex, time ->
                    Row(modifier = Modifier.fillMaxWidth().height(65.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(time, modifier = Modifier.width(54.dp), fontSize = 11.sp, color = TextLight, textAlign = TextAlign.Center)
                        for (dayIndex in 0..4) {
                            val slotData = availability.find { it.dayIndex == dayIndex && it.slotIndex == slotIndex }
                            val isBusy = slotData?.isBusy ?: false
                            val isLunch = slotIndex == 4
                            
                            Box(
                                modifier = Modifier
                                    .weight(1f).fillMaxHeight().padding(2.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isLunch) Color(0xFFECEFF1) else if (isBusy) ErrorRed else SuccessGreen)
                                    .clickable(enabled = !isLunch && isAdminEditing) {
                                        if (isBusy) {
                                            viewModel.clearSlot(dayIndex, slotIndex)
                                        } else {
                                            selectedSlot = dayIndex to slotIndex
                                            showAssignDialog = true
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                if (isLunch) Text("☕", fontSize = 14.sp)
                                else if (isBusy && slotData != null) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(2.dp)) {
                                        Text(slotData.courseCode, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                        Text(slotData.classroom, fontSize = 8.sp, color = Color.White.copy(alpha = 0.9f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    }
                                } else {
                                    Icon(Icons.Default.Add, null, tint = Color.White.copy(alpha = 0.5f), modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            }
        }

        if (showAssignDialog && selectedSlot != null) {
            var selectedCourse by remember { mutableStateOf<Course?>(null) }
            var classroomInput by remember { mutableStateOf("") }
            
            AlertDialog(
                onDismissRequest = { showAssignDialog = false },
                title = { Text("Ders Ata") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Ders Seçin", style = MaterialTheme.typography.labelMedium)
                        courses.forEach { course ->
                            Row(
                                modifier = Modifier.fillMaxWidth().clickable { selectedCourse = course }
                                    .background(if (selectedCourse == course) PrimaryBlue.copy(alpha = 0.1f) else Color.Transparent)
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(selected = (selectedCourse == course), onClick = { selectedCourse = course })
                                Text("${course.code} - ${course.name}", fontSize = 12.sp)
                            }
                        }
                        OutlinedTextField(
                            value = classroomInput,
                            onValueChange = { classroomInput = it },
                            label = { Text("Derslik (örn: D101)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        enabled = selectedCourse != null && classroomInput.isNotBlank(),
                        onClick = {
                            viewModel.assignCourse(selectedSlot!!.first, selectedSlot!!.second, selectedCourse!!, classroomInput)
                            showAssignDialog = false
                        }
                    ) { Text("ATA") }
                },
                dismissButton = { TextButton(onClick = { showAssignDialog = false }) { Text("İPTAL") } }
            )
        }

        if (showNoteDialog) {
            AlertDialog(
                onDismissRequest = { showNoteDialog = false },
                title = { Text(if (isReadOnly) "Hocaya Öneri Notu" else "Reddetme Gerekçesi") },
                text = {
                    OutlinedTextField(
                        value = noteInput,
                        onValueChange = { noteInput = it },
                        modifier = Modifier.fillMaxWidth().height(120.dp),
                        placeholder = { Text("Mesajınızı buraya yazın...") }
                    )
                },
                confirmButton = {
                    Button(onClick = {
                        if (isReadOnly) viewModel.submitAdminProposal(noteInput) else viewModel.rejectAdminProposal(noteInput)
                        showNoteDialog = false
                        noteInput = ""
                    }) { Text("GÖNDER") }
                },
                dismissButton = { TextButton(onClick = { showNoteDialog = false }) { Text("İPTAL") } }
            )
        }
    }
}

@Composable
fun StatusBadge(status: ScheduleStatus, isReadOnly: Boolean) {
    val (text, color) = when(status) {
        ScheduleStatus.PENDING -> "Onay Bekliyor" to WarningOrange
        ScheduleStatus.APPROVED -> "Onaylandı" to SuccessGreen
        ScheduleStatus.REJECTED -> (if (isReadOnly) "Hoca Revize İstedi" else "Revize Gerekli") to ErrorRed
        ScheduleStatus.ADMIN_PROPOSAL -> (if (isReadOnly) "Öneri Gönderildi" else "Yeni Öneri Var") to PrimaryBlue
    }
    Surface(
        color = color.copy(alpha = 0.15f),
        contentColor = color,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.padding(top = 4.dp)
    ) {
        Text(text, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp), fontWeight = FontWeight.ExtraBold)
    }
}
