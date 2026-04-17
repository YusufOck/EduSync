package com.example.edusync.data

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.example.edusync.util.SecurityUtils
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TeacherRepository @Inject constructor(
    private val database: FirebaseDatabase
) {
    private val teachersRef = database.getReference("teachers")
    private val coursesRef = database.getReference("courses")
    private val availabilityRef = database.getReference("availability")

    /**
     * READ: Verileri çekerken asenkron olarak deşifre eder (AES Decrypt).
     */
    fun getAllTeachers(): Flow<List<Teacher>> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = snapshot.children.mapNotNull { it.getValue(Teacher::class.java)?.let { t ->
                    t.copy(
                        name = SecurityUtils.decrypt(t.name),
                        surname = SecurityUtils.decrypt(t.surname),
                        department = SecurityUtils.decrypt(t.department),
                        title = SecurityUtils.decrypt(t.title),
                        adminNote = SecurityUtils.decrypt(t.adminNote)
                    )
                }}
                trySend(list)
            }
            override fun onCancelled(error: DatabaseError) { close(error.toException()) }
        }
        teachersRef.addValueEventListener(listener)
        awaitClose { teachersRef.removeEventListener(listener) }
    }

    /**
     * WRITE: Verileri kaydederken asenkron olarak şifreler (AES Encrypt).
     */
    suspend fun insertTeacher(teacher: Teacher): Long {
        val existing = getTeacherByName(teacher.name, teacher.surname)
        val targetRef = if (existing != null) teachersRef.child(existing.id.toString()) else teachersRef.push()
        
        val newId = existing?.id ?: (targetRef.key?.hashCode() ?: System.currentTimeMillis().toInt())
        
        // Hassas verileri AES ile şifreliyoruz
        val encryptedTeacher = teacher.copy(
            id = newId,
            name = SecurityUtils.encrypt(teacher.name),
            surname = SecurityUtils.encrypt(teacher.surname),
            department = SecurityUtils.encrypt(teacher.department),
            title = SecurityUtils.encrypt(teacher.title),
            adminNote = SecurityUtils.encrypt(teacher.adminNote)
        )
        
        targetRef.setValue(encryptedTeacher).await()
        return newId.toLong()
    }

    suspend fun getTeacherByName(name: String, surname: String): Teacher? {
        val allTeachers = getAllTeachers().first()
        return allTeachers.find { 
            normalizeForComparison(it.name) == normalizeForComparison(name) &&
            normalizeForComparison(it.surname) == normalizeForComparison(surname)
        }
    }

    private fun normalizeForComparison(text: String): String {
        return text.lowercase(Locale("tr"))
            .replace('ı', 'i').replace('ğ', 'g').replace('ü', 'u')
            .replace('ş', 's').replace('ö', 'o').replace('ç', 'c').trim()
    }

    suspend fun updateTeacher(teacher: Teacher) {
        val encrypted = teacher.copy(
            name = SecurityUtils.encrypt(teacher.name),
            surname = SecurityUtils.encrypt(teacher.surname),
            department = SecurityUtils.encrypt(teacher.department),
            title = SecurityUtils.encrypt(teacher.title),
            adminNote = SecurityUtils.encrypt(teacher.adminNote)
        )
        teachersRef.child(teacher.id.toString()).setValue(encrypted).await()
    }

    suspend fun deleteTeacher(teacher: Teacher) {
        teachersRef.child(teacher.id.toString()).removeValue().await()
        availabilityRef.child(teacher.id.toString()).removeValue().await()
    }

    fun getAvailability(teacherId: Int): Flow<List<TeacherAvailability>> = callbackFlow {
        val ref = availabilityRef.child(teacherId.toString())
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = snapshot.children.mapNotNull { it.getValue(TeacherAvailability::class.java) }
                trySend(list)
            }
            override fun onCancelled(error: DatabaseError) { close(error.toException()) }
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    suspend fun updateAvailability(availability: TeacherAvailability) {
        val key = "${availability.dayIndex}_${availability.slotIndex}"
        availabilityRef.child(availability.teacherId.toString()).child(key).setValue(availability).await()
    }

    // Courses (Ders isimlerini de şifreliyoruz)
    suspend fun insertCourse(course: Course) {
        val encryptedCourse = course.copy(name = SecurityUtils.encrypt(course.name))
        coursesRef.child(course.code).setValue(encryptedCourse).await()
    }

    fun getCoursesByTeacher(teacherId: Int): Flow<List<Course>> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = snapshot.children.mapNotNull { it.getValue(Course::class.java)?.let { c ->
                    c.copy(name = SecurityUtils.decrypt(c.name))
                }}.filter { it.teacherId == teacherId }
                trySend(list)
            }
            override fun onCancelled(error: DatabaseError) { close(error.toException()) }
        }
        coursesRef.addValueEventListener(listener)
        awaitClose { coursesRef.removeEventListener(listener) }
    }
}
