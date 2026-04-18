package com.example.edusync.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.edusync.data.UserRole
import com.example.edusync.ui.*

@Composable
fun AppNavigation(
    chatViewModel: ChatViewModel = hiltViewModel()
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    var currentUserRole by remember { mutableStateOf<UserRole?>(null) }
    var currentUsername by remember { mutableStateOf<String?>(null) }
    var loggedInTeacherId by remember { mutableStateOf<Int?>(null) }

    val totalUnreadCount by chatViewModel.totalUnreadCount.collectAsState()

    LaunchedEffect(currentUsername, currentUserRole) {
        if (currentUserRole == UserRole.ADMIN) {
            chatViewModel.initUser("admin")
        } else if (currentUsername != null) {
            chatViewModel.initUser(currentUsername!!)
        }
    }

    val showAdminBar = currentUserRole == UserRole.ADMIN && currentDestination?.route != Screen.Login.route
    val showTeacherBar = currentUserRole == UserRole.TEACHER && currentDestination?.route != Screen.Login.route

    Scaffold(
        bottomBar = {
            if (showAdminBar) {
                NavigationBar {
                    adminBottomNavItems.forEach { screen ->
                        val isSelected = currentDestination?.hierarchy?.any { it.route == screen.route } == true
                        NavigationBarItem(
                            icon = { 
                                BadgedBox(
                                    badge = {
                                        if (screen == Screen.AdminMessages && totalUnreadCount > 0) {
                                            Badge { Text(totalUnreadCount.toString()) }
                                        }
                                    }
                                ) {
                                    Icon(screen.icon, contentDescription = screen.title)
                                }
                            },
                            label = { Text(screen.title) },
                            selected = isSelected,
                            onClick = {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            } else if (showTeacherBar) {
                NavigationBar {
                    teacherBottomNavItems.forEach { screen ->
                        val route = if (screen is Screen.TeacherSchedule && loggedInTeacherId != null) {
                            screen.createRoute(loggedInTeacherId!!)
                        } else {
                            screen.route
                        }
                        
                        val isSelected = currentDestination?.hierarchy?.any { it.route == screen.route } == true ||
                                (screen is Screen.TeacherSchedule && currentDestination?.route?.startsWith("teacher_schedule") == true)

                        NavigationBarItem(
                            icon = { 
                                BadgedBox(
                                    badge = {
                                        if (screen == Screen.TeacherMessages && totalUnreadCount > 0) {
                                            Badge { Text(totalUnreadCount.toString()) }
                                        }
                                    }
                                ) {
                                    Icon(screen.icon, contentDescription = screen.title)
                                }
                            },
                            label = { Text(screen.title) },
                            selected = isSelected,
                            onClick = {
                                navController.navigate(route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController, 
            startDestination = Screen.Login.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Login.route) {
                LoginScreen(
                    onLoginSuccess = { role, teacherId, username ->
                        currentUserRole = role
                        currentUsername = username
                        if (role == UserRole.ADMIN) {
                            navController.navigate(Screen.AdminDashboard.route) {
                                popUpTo(Screen.Login.route) { inclusive = true }
                            }
                        } else if (teacherId != null) {
                            loggedInTeacherId = teacherId
                            navController.navigate(Screen.TeacherSchedule.createRoute(teacherId)) {
                                popUpTo(Screen.Login.route) { inclusive = true }
                            }
                        }
                    }
                )
            }
            
            composable(
                route = Screen.TeacherSchedule.route,
                arguments = listOf(navArgument("teacherId") { type = NavType.IntType })
            ) { backStackEntry ->
                val teacherId = backStackEntry.arguments?.getInt("teacherId") ?: 0
                val showBackButton = currentUserRole == UserRole.ADMIN

                TeacherScheduleScreen(
                    teacherId = teacherId,
                    isReadOnly = currentUserRole == UserRole.ADMIN,
                    onNavigateBack = if (showBackButton) { { navController.popBackStack() } } else null,
                    onLogout = if (currentUserRole == UserRole.TEACHER) {
                        {
                            currentUserRole = null
                            currentUsername = null
                            loggedInTeacherId = null
                            navController.navigate(Screen.Login.route) { popUpTo(0) { inclusive = true } }
                        }
                    } else null
                )
            }

            composable(Screen.TeacherMessages.route) {
                ChatDetailScreen(
                    currentUserId = currentUsername ?: "",
                    targetUserId = "admin",
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(Screen.TeacherSettings.route) {
                TeacherSettingsScreen(
                    onLogout = {
                        currentUserRole = null
                        currentUsername = null
                        loggedInTeacherId = null
                        navController.navigate(Screen.Login.route) { popUpTo(0) { inclusive = true } }
                    }
                )
            }

            composable(Screen.AdminDashboard.route) {
                AdminDashboardScreen(
                    onLogout = {
                        currentUserRole = null
                        currentUsername = null
                        navController.navigate(Screen.Login.route) { popUpTo(0) { inclusive = true } }
                    }
                )
            }

            composable(Screen.TeacherManagement.route) {
                TeacherManagementScreen(
                    onNavigateBack = { navController.popBackStack(); Unit },
                    onTeacherClick = { teacherId ->
                        navController.navigate(Screen.TeacherSchedule.createRoute(teacherId))
                    }
                )
            }

            composable(Screen.AdminMessages.route) {
                AdminMessagesScreen(
                    onChatClick = { targetUserId ->
                        navController.navigate(Screen.ChatDetail.createRoute(targetUserId))
                    }
                )
            }

            composable(
                route = Screen.ChatDetail.route,
                arguments = listOf(navArgument("targetUserId") { type = NavType.StringType })
            ) { backStackEntry ->
                val targetUserId = backStackEntry.arguments?.getString("targetUserId") ?: ""
                ChatDetailScreen(
                    currentUserId = if (currentUserRole == UserRole.ADMIN) "admin" else (currentUsername ?: ""),
                    targetUserId = targetUserId,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(Screen.VerificationCodes.route) {
                AdminVerificationCodeScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(Screen.ExcelImport.route) {
                AdminExcelImportScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
    }
}
