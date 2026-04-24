package com.example.edusync.ui

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Class
import androidx.compose.material.icons.filled.Groups
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
import com.example.edusync.data.Teacher
import com.example.edusync.data.TeacherAvailability
import com.example.edusync.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlobalScheduleScreen(
    viewModel: TeacherViewModel = hiltViewModel()
) {
    val teachers by viewModel.teachers.collectAsState()
    // PDF Optimization: Use pre-grouped data to avoid nested loops in UI Rendering
    val groupedSchedules by viewModel.groupedGlobalSchedules.collectAsState()

    var selectedDayIndex by remember { mutableIntStateOf(0) }
    var selectedSlot by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    var showDetailDialog by remember { mutableStateOf(false) }

    val days = listOf("Pazartesi", "Salı", "Çarşamba", "Perşembe", "Cuma")
    val dayShorts = listOf("Pzt", "Sal", "Çar", "Per", "Cum")
    val timeSlots = listOf(
        "08:30 - 09:20", "09:30 - 10:20", "10:30 - 11:20", "11:30 - 12:20",
        "13:30 - 14:20", "14:30 - 15:20", "15:30 - 16:20", "16:30 - 17:20"
    )

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Fakülte Genel Akışı", fontWeight = FontWeight.ExtraBold, color = PrimaryBlue) },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.White)
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize().background(BackgroundLight)) {
            
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color.White,
                shadowElevation = 2.dp
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    dayShorts.forEachIndexed { index, day ->
                        val isSelected = selectedDayIndex == index
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) PrimaryBlue else Color.Transparent)
                                .border(1.dp, if (isSelected) PrimaryBlue else Color.LightGray.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                                .clickable { selectedDayIndex = index },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                day,
                                color = if (isSelected) Color.White else Color.Gray,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
                contentPadding = PaddingValues(top = 24.dp, bottom = 24.dp)
            ) {
                itemsIndexed(timeSlots) { slotIndex, time ->
                    val isLunch = slotIndex == 4
                    
                    // PDF Optimization: O(1) lookup instead of O(N*M) search
                    val lessonsAtThisTime = groupedSchedules["${selectedDayIndex}_${slotIndex}"] ?: emptyList()

                    Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
                        Column(
                            modifier = Modifier.width(60.dp).fillMaxHeight(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(time, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = if (isLunch) Color.Gray else PrimaryBlue)
                            Spacer(Modifier.height(8.dp))
                            Box(modifier = Modifier.width(2.dp).weight(1f).background(Color.LightGray.copy(alpha = 0.3f)))
                        }

                        Box(
                            modifier = Modifier
                                .padding(start = 12.dp, bottom = 20.dp)
                                .weight(1f)
                                .clip(RoundedCornerShape(20.dp))
                                .background(if (isLunch) Color(0xFFF1F3F4) else Color.White)
                                .border(1.dp, if (isLunch) Color.Transparent else Color.White, RoundedCornerShape(20.dp))
                                .clickable(enabled = !isLunch) {
                                    selectedSlot = selectedDayIndex to slotIndex
                                    showDetailDialog = true
                                }
                        ) {
                            if (isLunch) {
                                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Text("☕", fontSize = 18.sp)
                                    Spacer(Modifier.width(12.dp))
                                    Text("Mola / Öğle Arası", color = Color.Gray, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                }
                            } else {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Surface(
                                            color = if (lessonsAtThisTime.isNotEmpty()) SuccessGreen.copy(alpha = 0.1f) else Color.LightGray.copy(alpha = 0.1f),
                                            shape = CircleShape,
                                            modifier = Modifier.size(8.dp)
                                        ) {}
                                        Spacer(Modifier.width(8.dp))
                                        Text(
                                            if (lessonsAtThisTime.isEmpty()) "Atama Yapılmadı" else "${lessonsAtThisTime.size} Aktif Ders",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = if (lessonsAtThisTime.isNotEmpty()) SuccessGreen else Color.LightGray
                                        )
                                        Spacer(Modifier.weight(1f))
                                        if (lessonsAtThisTime.isNotEmpty()) Icon(Icons.Default.ChevronRight, null, tint = Color.LightGray, modifier = Modifier.size(18.dp))
                                    }

                                    if (lessonsAtThisTime.isNotEmpty()) {
                                        Spacer(Modifier.height(8.dp))
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            lessonsAtThisTime.take(3).forEach { (_, availability) ->
                                                Surface(
                                                    color = LightBlue.copy(alpha = 0.5f),
                                                    shape = RoundedCornerShape(8.dp)
                                                ) {
                                                    Text(
                                                        availability.classroom,
                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = PrimaryBlue
                                                    )
                                                }
                                            }
                                            if (lessonsAtThisTime.size > 3) {
                                                Text("+${lessonsAtThisTime.size - 3}", fontSize = 10.sp, color = Color.Gray, modifier = Modifier.align(Alignment.CenterVertically))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        if (showDetailDialog && selectedSlot != null) {
            val (day, slot) = selectedSlot!!
            val lessons = groupedSchedules["${day}_${slot}"] ?: emptyList()

            AlertDialog(
                onDismissRequest = { showDetailDialog = false },
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
                                Icon(Icons.Default.Class, null, tint = PrimaryBlue)
                                Spacer(Modifier.width(12.dp))
                                Text("${days[day]} ${timeSlots[slot]}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            }
                            
                            Divider(modifier = Modifier.padding(vertical = 16.dp), color = Color.LightGray.copy(alpha = 0.4f))
                            
                            if (lessons.isEmpty()) {
                                Text("Bu saat diliminde planlanmış bir ders bulunmuyor.", color = Color.Gray)
                            } else {
                                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    itemsIndexed(
                                        items = lessons,
                                        key = { _, (teacher, _) -> teacher.id }
                                    ) { _, (teacher, availability) ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(16.dp))
                                                .background(BackgroundLight)
                                                .padding(16.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text("${teacher.title} ${teacher.name} ${teacher.surname}", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                                Text("${availability.courseCode} - ${availability.courseName}", fontSize = 12.sp, color = Color.Gray)
                                            }
                                            Surface(
                                                color = PrimaryBlue,
                                                shape = RoundedCornerShape(8.dp)
                                            ) {
                                                Text(availability.classroom, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                                            }
                                        }
                                    }
                                }
                            }
                            
                            Button(
                                onClick = { showDetailDialog = false },
                                modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) { Text("Anladım") }
                        }
                    }
                }
            )
        }
    }
}
