package com.example.edusync.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ForeignKey
import com.google.firebase.database.PropertyName

enum class UserRole {
    ADMIN, TEACHER
}

enum class ScheduleStatus {
    PENDING,        // Hoca girdi yaptı, admin onayı bekliyor
    APPROVED,       // Onaylı
    REJECTED,       // Admin reddetti
    ADMIN_PROPOSAL  // Admin teklif sundu, hoca onayı bekliyor
}

@Entity(tableName = "users")
data class User(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val username: String = "",
    val password: String = "",
    val role: UserRole = UserRole.TEACHER,
    val teacherId: Int? = null
)

@Entity(tableName = "teachers")
data class Teacher(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String = "",
    val surname: String = "",
    val department: String = "",
    val title: String = "",
    val scheduleStatus: ScheduleStatus = ScheduleStatus.APPROVED,
    val adminNote: String = ""
)

@Entity(
    tableName = "teacher_availability",
    primaryKeys = ["teacherId", "dayIndex", "slotIndex"]
)
data class TeacherAvailability(
    var teacherId: Int = 0,
    var dayIndex: Int = 0,
    var slotIndex: Int = 0,
    
    @get:PropertyName("busy")
    @set:PropertyName("busy")
    var isBusy: Boolean = false
)

@Entity(tableName = "courses")
data class Course(
    @PrimaryKey
    val code: String = "",
    val name: String = "",
    val teacherId: Int? = null
)

@Entity(tableName = "verification_codes")
data class VerificationCode(
    @PrimaryKey
    var code: String = "",
    
    @get:PropertyName("used")
    @set:PropertyName("used")
    var isUsed: Boolean = false,
    
    var createdBy: String = "ADMIN"
)
