package com.example.edusync.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Login : Screen("login", "Giriş", Icons.Default.Login)
    
    // Admin Screens
    object AdminDashboard : Screen("admin_dashboard", "Panel", Icons.Default.AdminPanelSettings)
    object TeacherManagement : Screen("teacher_management", "Hocalar", Icons.Default.People)
    object VerificationCodes : Screen("verification_codes", "Kodlar", Icons.Default.VpnKey)
    object ExcelImport : Screen("excel_import", "Excel Aktar", Icons.Default.FileUpload)
    object AdminMessages : Screen("admin_messages", "Mesajlar", Icons.Default.Chat)
    
    // Teacher Screens
    object TeacherSchedule : Screen("teacher_schedule/{teacherId}", "Program", Icons.Default.CalendarMonth) {
        fun createRoute(teacherId: Int) = "teacher_schedule/$teacherId"
    }
    object TeacherMessages : Screen("teacher_messages", "Mesajlar", Icons.Default.Chat)
    object TeacherSettings : Screen("teacher_settings", "Ayarlar", Icons.Default.Settings)

    // Common
    object ChatDetail : Screen("chat_detail/{targetUserId}", "Sohbet", Icons.Default.Chat) {
        fun createRoute(targetUserId: String) = "chat_detail/$targetUserId"
    }
}

val adminBottomNavItems = listOf(
    Screen.AdminDashboard,
    Screen.TeacherManagement,
    Screen.AdminMessages,
    Screen.VerificationCodes,
    Screen.ExcelImport
)

val teacherBottomNavItems = listOf(
    Screen.TeacherSchedule,
    Screen.TeacherMessages,
    Screen.TeacherSettings
)
