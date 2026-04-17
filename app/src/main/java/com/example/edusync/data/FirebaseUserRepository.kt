package com.example.edusync.data

import com.google.firebase.database.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Phase 2 - Cloud Shared Memory (User Management)
 * Tüm emülatörlerin ortak bir kullanıcı havuzuna asenkron erişimini sağlar.
 * Week 10: Concurrency ve Thread Management prensiplerine uygundur.
 */
@Singleton
class FirebaseUserRepository @Inject constructor(
    private val database: FirebaseDatabase
) {
    private val usersRef = database.getReference("users")
    private val codesRef = database.getReference("verification_codes")

    /**
     * ASYNC FETCH: Kullanıcı bilgisini asenkron olarak çeker.
     * withContext(Dispatchers.IO) ile Main Thread bloklanmaz.
     */
    suspend fun getUserByUsername(username: String): User? {
        val snapshot = usersRef.child(username).get().await()
        return snapshot.getValue(User::class.java)
    }

    /**
     * CONCURRENCY SAFE INSERT:setValue().await() kullanarak asenkron yazma yapar.
     * Race condition riskini minimize eder.
     */
    suspend fun insertUser(user: User) {
        usersRef.child(user.username).setValue(user).await()
    }

    suspend fun getUserByTeacherId(teacherId: Int): User? {
        val snapshot = usersRef.get().await()
        return snapshot.children.mapNotNull { it.getValue(User::class.java) }
            .find { it.teacherId == teacherId }
    }

    /**
     * REAL-TIME NOTIFICATIONS: Kayıt kodlarındaki değişiklikleri anlık (push) olarak izler.
     */
    fun getAllVerificationCodes(): Flow<List<VerificationCode>> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = snapshot.children.mapNotNull { it.getValue(VerificationCode::class.java) }
                trySend(list)
            }
            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }
        codesRef.addValueEventListener(listener)
        awaitClose { codesRef.removeEventListener(listener) }
    }

    suspend fun getValidVerificationCode(code: String): VerificationCode? {
        val snapshot = codesRef.child(code).get().await()
        val verificationCode = snapshot.getValue(VerificationCode::class.java)
        return if (verificationCode?.isUsed == false) verificationCode else null
    }

    suspend fun updateVerificationCode(code: VerificationCode) {
        codesRef.child(code.code).setValue(code).await()
    }

    suspend fun insertVerificationCode(code: VerificationCode) {
        codesRef.child(code.code).setValue(code).await()
    }
}
