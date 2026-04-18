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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
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
    LaunchedEffect(teacherId) {
        viewModel.selectTeacher(teacherId)
    }

    val teacher by viewModel.currentTeacher.collectAsState()
    val availability by viewModel.availabilityMatrix.collectAsState()
    val courses by viewModel.teacherCourses.collectAsState()
    val isAdminEditing by viewModel.isAdminEditing.collectAsState()

    var showNoteDialog by remember { mutableStateOf(false) }
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
                        IconButton(
                            onClick = { if (isAdminEditing) viewModel.cancelAdminEdit() else viewModel.startAdminEdit() }
                        ) {
                            Icon(
                                if (isAdminEditing) Icons.Default.EditOff else Icons.Default.Edit, 
                                "Düzenle",
                                tint = if (isAdminEditing) PrimaryBlue else Color.Gray
                            )
                        }
                    }
                    if (onLogout != null) {
                        IconButton(onClick = onLogout) {
                            Icon(Icons.AutoMirrored.Filled.ExitToApp, "Çıkış Yap", tint = ErrorRed)
                        }
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
                )
            )
        },
        bottomBar = {
            Surface(tonalElevation = 8.dp, shadowElevation = 12.dp) {
                if (isReadOnly) {
                    Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        if (isAdminEditing) {
                            Button(
                                onClick = { showNoteDialog = true },
                                modifier = Modifier.weight(1.3f).height(54.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                            ) {
                                Icon(Icons.AutoMirrored.Filled.Send, null)
                                Spacer(Modifier.width(8.dp))
                                Text("HOCAYA SUN", fontWeight = FontWeight.Bold)
                            }
                            OutlinedButton(
                                onClick = { viewModel.cancelAdminEdit() },
                                modifier = Modifier.weight(1f).height(54.dp),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Text("İPTAL")
                            }
                        } else if (teacher?.scheduleStatus == ScheduleStatus.PENDING) {
                            Button(
                                onClick = { viewModel.approveTeacherRequest("Talebiniz kabul edildi.") },
                                modifier = Modifier.weight(1f).height(54.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Icon(Icons.Default.Check, null)
                                Spacer(Modifier.width(8.dp))
                                Text("ONAYLA")
                            }
                            Button(
                                onClick = { showNoteDialog = true },
                                modifier = Modifier.weight(1f).height(54.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = ErrorRed),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Icon(Icons.Default.Close, null)
                                Spacer(Modifier.width(8.dp))
                                Text("REDDET")
                            }
                        }
                    }
                } else {
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
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(BackgroundLight)
        ) {
            // Logic for showing notes
            val msgToShow = if (isReadOnly) {
                // Admin sees what teacher wrote when rejecting OR if there's a legacy pending
                if (teacher?.scheduleStatus == ScheduleStatus.REJECTED || teacher?.scheduleStatus == ScheduleStatus.PENDING) teacher?.teacherNote else ""
            } else {
                // Teacher sees admin's note for proposal
                if (teacher?.scheduleStatus == ScheduleStatus.ADMIN_PROPOSAL || teacher?.scheduleStatus == ScheduleStatus.REJECTED) teacher?.adminNote else ""
            }

            AnimatedVisibility(visible = !msgToShow.isNullOrEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    colors = CardDefaults.cardColors(containerColor = LightBlue),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(4.dp)
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.ChatBubbleOutline, null, tint = PrimaryBlue)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            val label = if (isReadOnly) "Hoca'dan Gelen Gerekçe" else "Admin'den Gelen Not"
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = PrimaryBlue
                            )
                            Text(msgToShow ?: "", style = MaterialTheme.typography.bodyMedium, color = TextDark)
                        }
                    }
                }
            }

            if (courses.isNotEmpty()) {
                Text("Sorumlu Olduğu Dersler", modifier = Modifier.padding(start = 16.dp, top = 8.dp), style = MaterialTheme.typography.labelMedium, color = TextLight)
                LazyColumn(modifier = Modifier.heightIn(max = 100.dp).padding(horizontal = 12.dp)) {
                    items(courses) { course ->
                        Card(
                            modifier = Modifier.padding(4.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White)
                        ) {
                            Text("${course.code} - ${course.name}", modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), fontSize = 12.sp, color = TextDark)
                        }
                    }
                }
            }

            // Table Header
            Row(modifier = Modifier.fillMaxWidth().padding(top = 16.dp, start = 12.dp, end = 12.dp)) {
                Spacer(modifier = Modifier.width(54.dp))
                days.forEach { day ->
                    Text(day, modifier = Modifier.weight(1f), textAlign = TextAlign.Center, fontWeight = FontWeight.Bold, color = PrimaryBlue, fontSize = 14.sp)
                }
            }

            LazyColumn(modifier = Modifier.fillMaxSize().padding(12.dp)) {
                itemsIndexed(timeSlots) { slotIndex, time ->
                    Row(modifier = Modifier.fillMaxWidth().height(52.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(time, modifier = Modifier.width(54.dp), fontSize = 11.sp, fontWeight = FontWeight.Medium, color = TextLight, textAlign = TextAlign.Center)
                        for (dayIndex in 0..4) {
                            val isBusy = availability.find { it.dayIndex == dayIndex && it.slotIndex == slotIndex }?.isBusy ?: false
                            val isLunch = slotIndex == 4
                            
                            Box(
                                modifier = Modifier
                                    .weight(1f).fillMaxHeight().padding(2.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        when {
                                            isLunch -> Color(0xFFECEFF1)
                                            isBusy -> ErrorRed
                                            else -> SuccessGreen
                                        }
                                    )
                                    .clickable(enabled = !isLunch && isAdminEditing) {
                                        viewModel.toggleAvailability(dayIndex, slotIndex)
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                if (isLunch) Text("☕", fontSize = 14.sp)
                                else Icon(
                                    imageVector = if (isBusy) Icons.Default.Close else Icons.Default.Check,
                                    contentDescription = null,
                                    tint = Color.White.copy(alpha = 0.9f),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        if (showNoteDialog) {
            AlertDialog(
                onDismissRequest = { showNoteDialog = false },
                shape = RoundedCornerShape(28.dp),
                title = { 
                    Text(
                        if (isReadOnly) "Hocaya Öneri Notu" else "Reddetme Gerekçesi",
                        fontWeight = FontWeight.ExtraBold
                    ) 
                },
                text = {
                    OutlinedTextField(
                        value = noteInput,
                        onValueChange = { noteInput = it },
                        placeholder = { Text(if (isReadOnly) "Hocaya yeni program notunuz..." else "Hangi saatlerin değişmesi gerektiğini yazın...") },
                        modifier = Modifier.fillMaxWidth().height(140.dp),
                        shape = RoundedCornerShape(16.dp)
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (isReadOnly) {
                                if (isAdminEditing) viewModel.submitAdminProposal(noteInput)
                                else viewModel.approveTeacherRequest(noteInput) // Fallback for legacy
                            } else {
                                if (teacher?.scheduleStatus == ScheduleStatus.ADMIN_PROPOSAL) {
                                    viewModel.rejectAdminProposal(noteInput)
                                }
                            }
                            showNoteDialog = false
                            noteInput = ""
                        },
                        shape = RoundedCornerShape(12.dp)
                    ) { Text("GÖNDER") }
                }
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
