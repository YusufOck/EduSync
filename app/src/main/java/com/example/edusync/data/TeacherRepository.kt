package com.example.edusync.data

import com.google.firebase.database.*
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
     * ASYNC READ: Firebase'den gelen şifreli verileri asenkron deşifre eder (AES Decrypt).
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

    suspend fun getTeacherByName(name: String, surname: String): Teacher? {
        val allTeachers = getAllTeachers().first()
        return allTeachers.find { 
            normalize(it.name) == normalize(name) && normalize(it.surname) == normalize(surname)
        }
    }

    private fun normalize(t: String) = t.lowercase(Locale("tr")).replace('ı','i').replace('ş','s').replace('ğ','g').replace('ü','u').replace('ö','o').replace('ç','c').trim()

    suspend fun insertTeacher(teacher: Teacher): Long {
        val existing = getTeacherByName(teacher.name, teacher.surname)
        val targetRef = if (existing != null) teachersRef.child(existing.id.toString()) else teachersRef.push()
        val id = existing?.id ?: targetRef.key?.hashCode() ?: System.currentTimeMillis().toInt()
        
        val encryptedTeacher = teacher.copy(
            id = id,
            name = SecurityUtils.encrypt(teacher.name),
            surname = SecurityUtils.encrypt(teacher.surname),
            department = SecurityUtils.encrypt(teacher.department),
            title = SecurityUtils.encrypt(teacher.title),
            adminNote = SecurityUtils.encrypt(teacher.adminNote)
        )
        targetRef.setValue(encryptedTeacher).await()
        return id.toLong()
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
                trySend(snapshot.children.mapNotNull { it.getValue(TeacherAvailability::class.java) })
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
