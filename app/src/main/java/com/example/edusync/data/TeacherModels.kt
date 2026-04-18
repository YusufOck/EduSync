package com.example.edusync.data

import com.google.firebase.database.PropertyName

enum class UserRole { ADMIN, TEACHER }

enum class ScheduleStatus { PENDING, APPROVED, REJECTED, ADMIN_PROPOSAL }

data class User(
    var id: Int = 0,
    var username: String = "",
    var password: String = "",
    var role: UserRole = UserRole.TEACHER,
    var teacherId: Int? = null
)

data class Teacher(
    var id: Int = 0,
    var name: String = "",
    var surname: String = "",
    var department: String = "",
    var title: String = "",
    var scheduleStatus: ScheduleStatus = ScheduleStatus.APPROVED,
    var adminNote: String = "",
    var teacherNote: String = "" // Added to separate messages
)

data class TeacherAvailability(
    var teacherId: Int = 0,
    var dayIndex: Int = 0,
    var slotIndex: Int = 0,
    @get:PropertyName("busy")
    @set:PropertyName("busy")
    @PropertyName("busy")
    var isBusy: Boolean = false 
)

data class Course(
    var code: String = "",
    var name: String = "",
    var teacherId: Int? = null
)

data class VerificationCode(
    var code: String = "",
    @get:PropertyName("used")
    @set:PropertyName("used")
    @PropertyName("used")
    var isUsed: Boolean = false,
    var createdBy: String = "ADMIN"
)
