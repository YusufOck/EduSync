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
    private val proposalsRef = database.getReference("proposals")
    private val codesRef = database.getReference("verification_codes")
    private val usersRef = database.getReference("users")

    // Yardımcı: Bir hocanın Firebase'deki gerçek "Key"ini ID üzerinden bulur
    private suspend fun getTeacherKeyById(id: Int): String {
        val snapshot = teachersRef.get().await()
        for (child in snapshot.children) {
            if (child.child("id").getValue(Int::class.java) == id) {
                return child.key ?: id.toString()
            }
        }
        return id.toString()
    }

    fun getAllTeachers(): Flow<List<Teacher>> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = snapshot.children.mapNotNull { it.getValue(Teacher::class.java)?.let { t ->
                    t.copy(
                        name = SecurityUtils.decrypt(t.name),
                        surname = SecurityUtils.decrypt(t.surname),
                        department = SecurityUtils.decrypt(t.department),
                        title = SecurityUtils.decrypt(t.title),
                        adminNote = SecurityUtils.decrypt(t.adminNote),
                        teacherNote = SecurityUtils.decrypt(t.teacherNote)
                    )
                }}
                trySend(list)
            }
            override fun onCancelled(error: DatabaseError) { close(error.toException()) }
        }
        teachersRef.addValueEventListener(listener)
        awaitClose { teachersRef.removeEventListener(listener) }
    }

    suspend fun getTeacherById(id: Int): Teacher? {
        // ID bazlı arama yapıyoruz (en güvenli yol)
        val all = getAllTeachers().first()
        return all.find { it.id == id }
    }

    suspend fun insertTeacher(teacher: Teacher): Long {
        val all = getAllTeachers().first()
        val existing = all.find { 
            normalize(it.name) == normalize(teacher.name) && normalize(it.surname) == normalize(teacher.surname) 
        }
        if (existing != null) return existing.id.toLong()

        val id = System.currentTimeMillis().toInt() 
        val targetRef = teachersRef.child(id.toString()) // Yeni kayıtları hep ID ile açıyoruz
        
        val encryptedTeacher = teacher.copy(
            id = id,
            name = SecurityUtils.encrypt(teacher.name),
            surname = SecurityUtils.encrypt(teacher.surname),
            department = SecurityUtils.encrypt(teacher.department),
            title = SecurityUtils.encrypt(teacher.title)
        )
        targetRef.setValue(encryptedTeacher).await()
        return id.toLong()
    }

    suspend fun updateScheduleStatus(teacherId: Int, status: ScheduleStatus, adminNote: String? = null, teacherNote: String? = null) {
        val key = getTeacherKeyById(teacherId)
        val updates = mutableMapOf<String, Any>("scheduleStatus" to status.name)
        adminNote?.let { updates["adminNote"] = if (it.isEmpty()) "" else SecurityUtils.encrypt(it) }
        teacherNote?.let { updates["teacherNote"] = if (it.isEmpty()) "" else SecurityUtils.encrypt(it) }
        teachersRef.child(key).updateChildren(updates).await()
    }

    private fun normalize(t: String) = t.lowercase(Locale("tr")).replace('ı','i').replace('ş','s').replace('ğ','g').replace('ü','u').replace('ö','o').replace('ç','c').trim()

    // --- Aktivasyon İşlemleri ---
    suspend fun generateCodeForTeacher(teacherId: Int): String {
        val code = (100000..999999).random().toString()
        codesRef.child(code).setValue(VerificationCode(code = code, teacherId = teacherId, isUsed = false)).await()
        return code
    }

    suspend fun getVerificationCode(code: String): VerificationCode? {
        val snap = codesRef.child(code).get().await()
        return snap.getValue(VerificationCode::class.java)
    }

    suspend fun activateTeacherAccount(code: String, user: User): Boolean {
        val vCode = getVerificationCode(code) ?: return false
        if (vCode.isUsed || vCode.teacherId == null) return false

        val newUser = user.copy(
            role = UserRole.TEACHER,
            teacherId = vCode.teacherId,
            password = SecurityUtils.hashPassword(user.password)
        )
        usersRef.child(newUser.username).setValue(newUser).await()
        codesRef.child(code).child("used").setValue(true).await()
        return true
    }

    // --- Program İşlemleri ---
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

    fun getProposal(teacherId: Int): Flow<List<TeacherAvailability>> = callbackFlow {
        val ref = proposalsRef.child(teacherId.toString())
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                trySend(snapshot.children.mapNotNull { it.getValue(TeacherAvailability::class.java) })
            }
            override fun onCancelled(error: DatabaseError) { close(error.toException()) }
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    suspend fun updateProposal(availability: TeacherAvailability) {
        val key = "${availability.dayIndex}_${availability.slotIndex}"
        proposalsRef.child(availability.teacherId.toString()).child(key).setValue(availability).await()
    }

    suspend fun applyProposal(teacherId: Int) {
        val snapshot = proposalsRef.child(teacherId.toString()).get().await()
        availabilityRef.child(teacherId.toString()).setValue(snapshot.value).await()
        proposalsRef.child(teacherId.toString()).removeValue().await()
    }

    suspend fun discardProposal(teacherId: Int) {
        proposalsRef.child(teacherId.toString()).removeValue().await()
    }

    suspend fun copyAvailabilityToProposal(teacherId: Int) {
        val snapshot = availabilityRef.child(teacherId.toString()).get().await()
        proposalsRef.child(teacherId.toString()).setValue(snapshot.value).await()
    }

    // --- Çakışma Kontrolü ---
    suspend fun checkClassroomConflict(dayIndex: Int, slotIndex: Int, classroom: String, excludeTeacherId: Int): String? {
        val snapshot = availabilityRef.get().await()
        for (teacherSnap in snapshot.children) {
            val tId = teacherSnap.key?.toIntOrNull() ?: continue
            if (tId == excludeTeacherId) continue
            val slot = teacherSnap.child("${dayIndex}_${slotIndex}").getValue(TeacherAvailability::class.java)
            if (slot?.isBusy == true && slot.classroom == classroom) {
                val tKey = getTeacherKeyById(tId)
                val tData = teachersRef.child(tKey).get().await().getValue(Teacher::class.java)
                return "${SecurityUtils.decrypt(tData?.name)} ${SecurityUtils.decrypt(tData?.surname)} hocanın dersi var."
            }
        }
        return null
    }

    fun getAllAvailabilities(): Flow<Map<Int, List<TeacherAvailability>>> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val resultMap = mutableMapOf<Int, List<TeacherAvailability>>()
                snapshot.children.forEach { teacherSnap ->
                    val tid = teacherSnap.key?.toIntOrNull() ?: return@forEach
                    resultMap[tid] = teacherSnap.children.mapNotNull { it.getValue(TeacherAvailability::class.java) }
                }
                trySend(resultMap)
            }
            override fun onCancelled(error: DatabaseError) { close(error.toException()) }
        }
        availabilityRef.addValueEventListener(listener)
        awaitClose { availabilityRef.removeEventListener(listener) }
    }

    suspend fun deleteTeacher(teacher: Teacher) {
        val key = getTeacherKeyById(teacher.id)
        teachersRef.child(key).removeValue().await()
        availabilityRef.child(teacher.id.toString()).removeValue().await()
        proposalsRef.child(teacher.id.toString()).removeValue().await()
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
