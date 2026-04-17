package com.example.edusync.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ForeignKey

enum class UserRole {
    ADMIN, TEACHER
}

enum class ScheduleStatus {
    PENDING,        // Hoca girdi yaptı, admin onayı bekliyor
    APPROVED,       // Onaylı / Her şey yolunda
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
    primaryKeys = ["teacherId", "dayIndex", "slotIndex"],
    foreignKeys = [
        ForeignKey(
            entity = Teacher::class,
            parentColumns = ["id"],
            childColumns = ["teacherId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class TeacherAvailability(
    val teacherId: Int = 0,
    val dayIndex: Int = 0,
    val slotIndex: Int = 0,
    val isBusy: Boolean = false
)

@Entity(
    tableName = "courses",
    foreignKeys = [
        ForeignKey(
            entity = Teacher::class,
            parentColumns = ["id"],
            childColumns = ["teacherId"],
            onDelete = ForeignKey.SET_NULL
        )
    ]
)
data class Course(
    @PrimaryKey
    val code: String = "",
    val name: String = "",
    val teacherId: Int? = null
)

@Entity(tableName = "verification_codes")
data class VerificationCode(
    @PrimaryKey
    val code: String = "",
    val isUsed: Boolean = false,
    val createdBy: String = "ADMIN"
)
