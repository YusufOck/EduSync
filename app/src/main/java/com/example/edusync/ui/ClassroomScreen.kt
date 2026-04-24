package com.example.edusync.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.edusync.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClassroomScreen(
    onNavigateBack: () -> Unit,
    viewModel: AdminViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val classrooms by viewModel.classrooms.collectAsState()
    val error by viewModel.classroomError.collectAsState()
    val classroomImportState by viewModel.classroomImportState.collectAsState()
    val classroomPreview by viewModel.classroomPreview.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf<String?>(null) }

    val classroomExcelLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.loadClassroomPreview(context, it) }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Sınıf Yönetimi", fontWeight = FontWeight.Bold, color = PrimaryBlue) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Geri", tint = PrimaryBlue)
                    }
                },
                actions = {
                    // Excel Import Button
                    IconButton(onClick = {
                        classroomExcelLauncher.launch("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                    }) {
                        Icon(Icons.Default.FileUpload, "Excel Aktar", tint = SuccessGreen)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.White)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = PrimaryBlue,
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.Add, "Sınıf Ekle")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(BackgroundLight)
        ) {
            // Error banner
            AnimatedVisibility(visible = error != null) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    colors = CardDefaults.cardColors(containerColor = ErrorRed.copy(alpha = 0.1f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Error, null, tint = ErrorRed)
                        Spacer(Modifier.width(8.dp))
                        Text(error ?: "", color = ErrorRed, modifier = Modifier.weight(1f))
                        IconButton(onClick = { viewModel.clearClassroomError() }) {
                            Icon(Icons.Default.Close, null, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }

            // Summary header
            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = PrimaryBlue)
            ) {
                Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = Color.White.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(Icons.Default.MeetingRoom, null, tint = Color.White, modifier = Modifier.padding(12.dp))
                    }
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text("Toplam Sınıf", color = Color.White.copy(alpha = 0.8f), fontSize = 13.sp)
                        Text("${classrooms.size} Sınıf Kayıtlı", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    }
                }
            }

            if (classrooms.isEmpty()) {
                // Empty state
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.MeetingRoom, null, tint = Color.LightGray, modifier = Modifier.size(64.dp))
                        Spacer(Modifier.height(16.dp))
                        Text("Henüz sınıf eklenmemiş", color = Color.Gray, fontWeight = FontWeight.Medium)
                        Spacer(Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = {
                                classroomExcelLauncher.launch("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                            },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.FileUpload, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Excel'den Aktar")
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(top = 8.dp, bottom = 80.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(items = classrooms, key = { it.id }) { classroom ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    color = LightBlue,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                        Text(classroom.roomCode.take(2), fontWeight = FontWeight.Bold, color = PrimaryBlue, fontSize = 16.sp)
                                    }
                                }
                                Spacer(Modifier.width(16.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(classroom.roomCode, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextDark)
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.People, null, tint = Color.Gray, modifier = Modifier.size(14.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("${classroom.capacity} kişilik", fontSize = 12.sp, color = Color.Gray)
                                        if (classroom.department.isNotEmpty()) {
                                            Text(" • ${classroom.department}", fontSize = 12.sp, color = Color.Gray)
                                        }
                                    }
                                }
                                IconButton(onClick = { showDeleteDialog = classroom.id }) {
                                    Icon(Icons.Default.Delete, "Sil", tint = ErrorRed.copy(alpha = 0.7f))
                                }
                            }
                        }
                    }
                }
            }
        }

        // Add Classroom Dialog
        if (showAddDialog) {
            var roomCode by remember { mutableStateOf("") }
            var capacity by remember { mutableStateOf("") }
            var department by remember { mutableStateOf("") }

            AlertDialog(
                onDismissRequest = { showAddDialog = false },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.MeetingRoom, null, tint = PrimaryBlue)
                        Spacer(Modifier.width(12.dp))
                        Text("Yeni Sınıf Ekle", fontWeight = FontWeight.Bold)
                    }
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = roomCode,
                            onValueChange = { roomCode = it },
                            label = { Text("Sınıf Kodu *") },
                            placeholder = { Text("Örn: A101, LAB-1") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = capacity,
                            onValueChange = { capacity = it.filter { c -> c.isDigit() } },
                            label = { Text("Kapasite *") },
                            placeholder = { Text("Örn: 50") },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = department,
                            onValueChange = { department = it },
                            label = { Text("Bina / Bölüm") },
                            placeholder = { Text("Örn: Mühendislik Binası") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }
                },
                confirmButton = {
                    Button(
                        enabled = roomCode.isNotBlank() && capacity.isNotBlank(),
                        onClick = {
                            viewModel.addClassroom(roomCode.trim(), capacity.toIntOrNull() ?: 0, department.trim())
                            showAddDialog = false
                        },
                        shape = RoundedCornerShape(12.dp)
                    ) { Text("EKLE") }
                },
                dismissButton = {
                    TextButton(onClick = { showAddDialog = false }) { Text("İPTAL") }
                }
            )
        }

        // Delete Confirmation Dialog
        if (showDeleteDialog != null) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = null },
                title = { Text("Sınıfı Sil", fontWeight = FontWeight.Bold) },
                text = { Text("Bu sınıfı silmek istediğinize emin misiniz?") },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.deleteClassroom(showDeleteDialog!!)
                            showDeleteDialog = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ErrorRed),
                        shape = RoundedCornerShape(12.dp)
                    ) { Text("SİL") }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteDialog = null }) { Text("İPTAL") }
                }
            )
        }

        // ========== Classroom Excel Preview Dialog ==========
        val currentPreview = classroomPreview
        if (currentPreview != null) {
            AlertDialog(
                onDismissRequest = { viewModel.clearClassroomImportState() },
                modifier = Modifier.fillMaxWidth(0.95f),
                properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
                content = {
                    Surface(
                        shape = RoundedCornerShape(28.dp),
                        color = Color.White,
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(24.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.FileUpload, null, tint = SuccessGreen)
                                Spacer(Modifier.width(12.dp))
                                Text("Sınıf Önizleme", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            }
                            Spacer(Modifier.height(4.dp))
                            Text("Excel Formatı: Room Code | Capacity | Department", fontSize = 12.sp, color = Color.Gray)
                            Spacer(Modifier.height(16.dp))

                            // Preview list
                            Card(
                                modifier = Modifier.fillMaxWidth().heightIn(max = 240.dp),
                                colors = CardDefaults.cardColors(containerColor = BackgroundLight),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                LazyColumn(modifier = Modifier.padding(12.dp)) {
                                    items(currentPreview) { item ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Surface(
                                                color = PrimaryBlue.copy(alpha = 0.1f),
                                                shape = RoundedCornerShape(8.dp),
                                                modifier = Modifier.size(36.dp)
                                            ) {
                                                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                                    Text(item.roomCode.take(2), fontWeight = FontWeight.Bold, color = PrimaryBlue, fontSize = 12.sp)
                                                }
                                            }
                                            Spacer(Modifier.width(12.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(item.roomCode, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                                Text("${item.capacity} kişi • ${item.department}", fontSize = 11.sp, color = Color.Gray)
                                            }
                                        }
                                        HorizontalDivider(thickness = 0.5.dp, color = Color.LightGray.copy(alpha = 0.5f))
                                    }
                                }
                            }

                            Spacer(Modifier.height(20.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                OutlinedButton(
                                    onClick = { viewModel.clearClassroomImportState() },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp)
                                ) { Text("İPTAL") }
                                Button(
                                    onClick = { viewModel.confirmClassroomImport(context) },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen)
                                ) {
                                    Icon(Icons.Default.Check, null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text("AKTAR", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            )
        }

        // ========== Classroom Import Result Dialogs ==========
        when (val state = classroomImportState) {
            is ImportResult.Loading -> {
                Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.3f)), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            is ImportResult.Success -> {
                AlertDialog(
                    onDismissRequest = { viewModel.clearClassroomImportState() },
                    icon = { Icon(Icons.Default.CheckCircle, null, tint = SuccessGreen) },
                    title = { Text("Başarılı") },
                    text = { Text("${state.count} sınıf başarıyla sisteme aktarıldı.") },
                    confirmButton = { TextButton(onClick = { viewModel.clearClassroomImportState() }) { Text("Tamam") } }
                )
            }
            is ImportResult.Error -> {
                AlertDialog(
                    onDismissRequest = { viewModel.clearClassroomImportState() },
                    title = { Text("Hata") },
                    text = { Text(state.message) },
                    confirmButton = { TextButton(onClick = { viewModel.clearClassroomImportState() }) { Text("Kapat") } }
                )
            }
            else -> {}
        }
    }
}
