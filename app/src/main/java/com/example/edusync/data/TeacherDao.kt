package com.example.edusync.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TeacherDao {
    @Query("SELECT * FROM teachers")
    fun getAllTeachers(): Flow<List<Teacher>>

    @Query("SELECT * FROM teachers WHERE id = :id")
    suspend fun getTeacherById(id: Int): Teacher?

    @Query("""
        SELECT * FROM teachers 
        WHERE LOWER(name) = LOWER(:name) AND LOWER(surname) = LOWER(:surname) 
        LIMIT 1
    """)
    suspend fun getTeacherByName(name: String, surname: String): Teacher?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTeacher(teacher: Teacher): Long

    @Update
    suspend fun updateTeacher(teacher: Teacher)

    @Delete
    suspend fun deleteTeacher(teacher: Teacher)

    @Query("SELECT * FROM teacher_availability WHERE teacherId = :teacherId")
    fun getAvailability(teacherId: Int): Flow<List<TeacherAvailability>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateAvailability(availability: TeacherAvailability)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateAllAvailability(availabilities: List<TeacherAvailability>)

    // Courses
    @Query("SELECT * FROM courses")
    fun getAllCourses(): Flow<List<Course>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCourse(course: Course)

    @Query("SELECT * FROM courses WHERE teacherId = :teacherId")
    fun getCoursesByTeacher(teacherId: Int): Flow<List<Course>>
}
