package com.example.edusync.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.edusync.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(
    onLogout: () -> Unit,
    onNavigateToExcel: () -> Unit,
    onNavigateToCodes: () -> Unit
) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("EduSync Yönetim", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = onLogout) {
                        Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Çıkış Yap", tint = ErrorRed)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(BackgroundLight)
                .padding(16.dp)
        ) {
            // Hoşgeldin Kartı - Daha Modern
            Card(
                modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = PrimaryBlue)
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text("Hoşgeldiniz,", color = Color.White.copy(alpha = 0.8f), fontSize = 16.sp)
                    Text("Sistem Yöneticisi", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Tüm akademik takvimi ve hoca planlamalarını buradan tek tıkla yönetebilirsiniz.", color = Color.White.copy(alpha = 0.7f), fontSize = 13.sp)
                }
            }

            Text("Hızlı Erişim & Ayarlar", fontWeight = FontWeight.Bold, color = PrimaryBlue, modifier = Modifier.padding(bottom = 16.dp), fontSize = 18.sp)

            // Grid Layout for Navigation
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    QuickActionCard(
                        title = "Excel Aktar",
                        subtitle = "Toplu Veri Yükle",
                        icon = Icons.Default.FileUpload,
                        color = SuccessGreen,
                        onClick = onNavigateToExcel
                    )
                }
                item {
                    QuickActionCard(
                        title = "Kayıt Kodları",
                        subtitle = "Hoca Yetkilendirme",
                        icon = Icons.Default.VpnKey,
                        color = WarningOrange,
                        onClick = onNavigateToCodes
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Bilgi Kartı
            Text("Sistem Bilgisi", fontWeight = FontWeight.Bold, color = PrimaryBlue, modifier = Modifier.padding(bottom = 12.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = LightBlue.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.size(50.dp)
                    ) {
                        Icon(Icons.Default.Schedule, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.padding(12.dp))
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text("2023-2024 Akademik Yıl", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Text("Bahar Dönemi Aktif", color = Color.Gray, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun QuickActionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().height(140.dp).clickable { onClick() },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Surface(
                color = color.copy(alpha = 0.15f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.size(40.dp)
            ) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.padding(10.dp))
            }
            Column {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextDark)
                Text(subtitle, fontSize = 11.sp, color = Color.Gray)
            }
        }
    }
}
