package com.example.edusync.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.edusync.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminVerificationCodeScreen(
    onNavigateBack: () -> Unit,
    viewModel: AdminViewModel = hiltViewModel()
) {
    val codes by viewModel.verificationCodes.collectAsState()
    val teachers by viewModel.teachers.collectAsState()
    val clipboardManager = LocalClipboardManager.current
    
    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Hoca Kayıt Kodları", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Geri", tint = PrimaryBlue)
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = PrimaryBlue,
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.Add, null)
                Spacer(Modifier.width(8.dp))
                Text("YENİ KOD ÜRET")
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize().background(BackgroundLight)) {
            
            if (codes.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Henüz kod oluşturulmamış.", color = Color.Gray)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(codes) { code ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(2.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(20.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    color = if (code.isUsed) Color.LightGray.copy(alpha = 0.2f) else PrimaryBlue.copy(alpha = 0.1f),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Icon(
                                        Icons.Default.VpnKey, 
                                        null, 
                                        tint = if (code.isUsed) Color.Gray else PrimaryBlue,
                                        modifier = Modifier.padding(12.dp)
                                    )
                                }
                                
                                Spacer(modifier = Modifier.width(16.dp))
                                
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = code.createdBy, // Buraya ViewModel'de hoca ismini koymuştuk
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = if (code.isUsed) Color.Gray else TextDark
                                    )
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(text = "KOD: ", fontSize = 12.sp, color = Color.Gray)
                                        Text(text = code.code, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = PrimaryBlue, letterSpacing = 1.sp)
                                    }
                                }

                                if (!code.isUsed) {
                                    IconButton(onClick = { 
                                        clipboardManager.setText(AnnotatedString(code.code))
                                    }) {
                                        Icon(Icons.Default.ContentCopy, "Kopyala", tint = Color.Gray)
                                    }
                                } else {
                                    Text("KULLANILDI", fontSize = 10.sp, fontWeight = FontWeight.Black, color = ErrorRed)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Hoca Seçme Diyaloğu
        if (showAddDialog) {
            AlertDialog(
                onDismissRequest = { showAddDialog = false },
                title = { Text("Hoca Seçin") },
                text = {
                    LazyColumn(modifier = Modifier.heightIn(max = 400.dp)) {
                        items(teachers) { teacher ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { 
                                        viewModel.generateCodeForTeacher(teacher.id)
                                        showAddDialog = false
                                    }
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Person, null, tint = PrimaryBlue, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(12.dp))
                                Text("${teacher.title} ${teacher.name} ${teacher.surname}")
                            }
                            HorizontalDivider(color = Color.LightGray.copy(alpha = 0.3f))
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showAddDialog = false }) { Text("İPTAL") }
                }
            )
        }
    }
}
