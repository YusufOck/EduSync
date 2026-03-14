package com.example.edusync.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        Teacher::class, 
        TeacherAvailability::class, 
        User::class, 
        VerificationCode::class,
        Course::class
    ], 
    version = 4,
    exportSchema = false
)
abstract class EduSyncDatabase : RoomDatabase() {
    abstract fun teacherDao(): TeacherDao
    abstract fun userDao(): UserDao
}
