package com.example.edusync.data

import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TeacherRepository @Inject constructor(
    private val teacherDao: TeacherDao
) {
    fun getAllTeachers(): Flow<List<Teacher>> = teacherDao.getAllTeachers()

    suspend fun getTeacherById(id: Int): Teacher? = teacherDao.getTeacherById(id)

    suspend fun getTeacherByName(name: String, surname: String): Teacher? = 
        teacherDao.getTeacherByName(name, surname)

    suspend fun insertTeacher(teacher: Teacher): Long = teacherDao.insertTeacher(teacher)

    suspend fun updateTeacher(teacher: Teacher) = teacherDao.updateTeacher(teacher)

    fun getAvailability(teacherId: Int): Flow<List<TeacherAvailability>> = 
        teacherDao.getAvailability(teacherId)

    suspend fun updateAvailability(availability: TeacherAvailability) = 
        teacherDao.updateAvailability(availability)

    suspend fun updateAllAvailability(availabilities: List<TeacherAvailability>) = 
        teacherDao.updateAllAvailability(availabilities)

    // Courses
    fun getAllCourses(): Flow<List<Course>> = teacherDao.getAllCourses()

    suspend fun insertCourse(course: Course) = teacherDao.insertCourse(course)

    fun getCoursesByTeacher(teacherId: Int): Flow<List<Course>> = 
        teacherDao.getCoursesByTeacher(teacherId)
}
