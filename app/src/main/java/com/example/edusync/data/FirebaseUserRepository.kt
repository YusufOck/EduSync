package com.example.edusync.data

import com.google.firebase.database.*
import com.example.edusync.util.SecurityUtils
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Phase 2 - Cloud Shared Memory (User Management)
 */
@Singleton
class FirebaseUserRepository @Inject constructor(
    private val database: FirebaseDatabase
) {
    private val usersRef = database.getReference("users")
    private val codesRef = database.getReference("verification_codes")

    suspend fun getUserByUsername(username: String): User? {
        val snapshot = usersRef.child(username).get().await()
        return snapshot.getValue(User::class.java)
    }

    suspend fun insertUser(user: User) {
        usersRef.child(user.username).setValue(user).await()
    }

    suspend fun getUserByTeacherId(teacherId: Int): User? {
        val snapshot = usersRef.get().await()
        return snapshot.children.mapNotNull { it.getValue(User::class.java) }
            .find { it.teacherId == teacherId }
    }

    fun getAllUsersFlow(): Flow<List<User>> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = snapshot.children.mapNotNull { it.getValue(User::class.java) }
                trySend(list)
            }
            override fun onCancelled(error: DatabaseError) { close(error.toException()) }
        }
        usersRef.addValueEventListener(listener)
        awaitClose { usersRef.removeEventListener(listener) }
    }

    fun getAllVerificationCodes(): Flow<List<VerificationCode>> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = snapshot.children.mapNotNull { it.getValue(VerificationCode::class.java)?.let { vc ->
                    vc.copy(code = SecurityUtils.decrypt(vc.code))
                }}
                trySend(list)
            }
            override fun onCancelled(error: DatabaseError) { close(error.toException()) }
        }
        codesRef.addValueEventListener(listener)
        awaitClose { codesRef.removeEventListener(listener) }
    }

    suspend fun getValidVerificationCode(inputCode: String): VerificationCode? {
        val snapshot = codesRef.get().await()
        val allCodes = snapshot.children.mapNotNull { it.getValue(VerificationCode::class.java) }
        return allCodes.find { 
            SecurityUtils.decrypt(it.code) == inputCode && !it.isUsed 
        }
    }

    suspend fun updateVerificationCode(code: VerificationCode) {
        val encryptedCodeValue = SecurityUtils.encrypt(code.code)
        val encryptedObject = code.copy(code = encryptedCodeValue)
        codesRef.child(encryptedCodeValue.hashCode().toString()).setValue(encryptedObject).await()
    }

    suspend fun insertVerificationCode(code: VerificationCode) {
        val encryptedCodeValue = SecurityUtils.encrypt(code.code)
        val encryptedObject = code.copy(code = encryptedCodeValue)
        codesRef.child(encryptedCodeValue.hashCode().toString()).setValue(encryptedObject).await()
    }
}
