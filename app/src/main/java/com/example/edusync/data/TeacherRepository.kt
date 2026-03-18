package com.example.edusync.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TeacherRepository @Inject constructor(
    private val teacherDao: TeacherDao
) {
    fun getAllTeachers(): Flow<List<Teacher>> = teacherDao.getAllTeachers()

    suspend fun getTeacherById(id: Int): Teacher? = teacherDao.getTeacherById(id)

    /**
     * Türkçe karakter duyarlı ve esnek arama yapar.
     * Hem veritabanındaki ismi hem de aranan ismi normalize ederek karşılaştırır.
     */
    suspend fun getTeacherByName(name: String, surname: String): Teacher? {
        val allTeachers = teacherDao.getAllTeachers().first()
        
        return allTeachers.find { 
            normalizeForComparison(it.name) == normalizeForComparison(name) &&
            normalizeForComparison(it.surname) == normalizeForComparison(surname)
        }
    }

    /**
     * Karşılaştırma için metni sadeleştirir:
     * Küçük harfe çevirir ve Türkçe karakterleri İngilizce karşılıklarına map eder.
     */
    private fun normalizeForComparison(text: String): String {
        return text.lowercase(Locale("tr"))
            .replace('ı', 'i')
            .replace('ğ', 'g')
            .replace('ü', 'u')
            .replace('ş', 's')
            .replace('ö', 'o')
            .replace('ç', 'c')
            .trim()
    }

    suspend fun insertTeacher(teacher: Teacher): Long {
        // MÜKERRER KAYIT KONTROLÜ: Önce bu isimde biri var mı bak
        val existing = getTeacherByName(teacher.name, teacher.surname)
        if (existing != null) {
            // Eğer varsa unvanını güncelle (opsiyonel) ve mevcut ID'yi dön
            val updated = existing.copy(title = teacher.title.ifEmpty { existing.title })
            teacherDao.updateTeacher(updated)
            return existing.id.toLong()
        }
        
        // Yoksa yeni kayıt oluştur
        return teacherDao.insertTeacher(teacher)
    }

    suspend fun updateTeacher(teacher: Teacher) {
        teacherDao.updateTeacher(teacher)
    }

    suspend fun deleteTeacher(teacher: Teacher) = teacherDao.deleteTeacher(teacher)

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
