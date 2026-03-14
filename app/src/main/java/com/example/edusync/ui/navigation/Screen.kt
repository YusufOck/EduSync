package com.example.edusync.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Login
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Login : Screen("login", "Giriş", Icons.Default.Login)
    
    // Admin Screens
    object AdminDashboard : Screen("admin_dashboard", "Panel", Icons.Default.AdminPanelSettings)
    object TeacherManagement : Screen("teacher_management", "Hocalar", Icons.Default.People)
    object VerificationCodes : Screen("verification_codes", "Kodlar", Icons.Default.VpnKey)
    object ExcelImport : Screen("excel_import", "Excel Aktar", Icons.Default.FileUpload)
    
    // Teacher Screens
    object TeacherSchedule : Screen("teacher_schedule/{teacherId}", "Program", Icons.Default.CalendarMonth) {
        fun createRoute(teacherId: Int) = "teacher_schedule/$teacherId"
    }
    object TeacherSettings : Screen("teacher_settings", "Ayarlar", Icons.Default.Settings)
}

val adminBottomNavItems = listOf(
    Screen.AdminDashboard,
    Screen.TeacherManagement,
    Screen.VerificationCodes,
    Screen.ExcelImport
)

val teacherBottomNavItems = listOf(
    Screen.TeacherSchedule, // Not: teacherId parametresi yüzünden navigasyon zorlaşabilir, yönetilmeli
    Screen.TeacherSettings
)
