package com.example.edusync.data

import com.google.firebase.database.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Phase 2 - Shared Memory & Asynchronous Data Management
 * Bu sınıf, Firebase Realtime DB kullanarak tüm paydaşların ortak bir bellek alanını (Cloud Shared Memory) 
 * asenkron ve güvenli bir şekilde kullanmasını sağlar.
 */
@Singleton
class FirebaseTeacherRepository @Inject constructor(
    private val database: FirebaseDatabase
) {
    private val teachersRef = database.getReference("teachers")
    private val coursesRef = database.getReference("courses")
    private val availabilityRef = database.getReference("availability")

    /**
     * ASYNC FLOW (Real-time): Firebase'deki her değişiklik asenkron bir akış (Flow) olarak döner.
     * Bu yapı, Admin'in yaptığı onay işleminin Hoca ekranına anında düşmesini sağlar.
     */
    fun getAllTeachers(): Flow<List<Teacher>> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = snapshot.children.mapNotNull { it.getValue(Teacher::class.java) }
                trySend(list) // UI Thread bloklanmadan veri asenkron gönderilir
            }
            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }
        teachersRef.addValueEventListener(listener)
        awaitClose { teachersRef.removeEventListener(listener) }
    }

    /**
     * CONCURRENCY CONTROL: Suspend fonksiyon kullanarak Race Condition riskini azaltıyoruz.
     */
    suspend fun insertTeacher(teacher: Teacher): String {
        val ref = if (teacher.id == 0) teachersRef.push() else teachersRef.child(teacher.id.toString())
        val id = ref.key ?: ""
        val normalizedTeacher = teacher.copy(
            id = id.hashCode(),
            name = teacher.name.lowercase(Locale("tr")).trim(),
            surname = teacher.surname.lowercase(Locale("tr")).trim()
        )
        ref.setValue(normalizedTeacher).await() // İşlem bitene kadar asenkron bekler (Main thread serbest)
        return id
    }

    /**
     * SHARED UPDATE: Admin onayladığında veya hoca öneriyi kabul ettiğinde tetiklenir.
     */
    suspend fun updateScheduleStatus(teacherId: Int, status: ScheduleStatus, note: String = "") {
        val updates = mapOf(
            "scheduleStatus" to status.name,
            "adminNote" to note
        )
        teachersRef.child(teacherId.toString()).updateChildren(updates).await()
    }

    fun getAvailability(teacherId: Int): Flow<List<TeacherAvailability>> = callbackFlow {
        val ref = availabilityRef.child(teacherId.toString())
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = snapshot.children.mapNotNull { it.getValue(TeacherAvailability::class.java) }
                trySend(list)
            }
            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    suspend fun updateAvailability(availability: TeacherAvailability) {
        val path = "${availability.teacherId}/${availability.dayIndex}_${availability.slotIndex}"
        availabilityRef.child(path).setValue(availability).await()
    }

    suspend fun deleteTeacher(teacherId: Int) {
        teachersRef.child(teacherId.toString()).removeValue().await()
    }
}
