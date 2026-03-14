package com.example.edusync.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM users WHERE username = :username LIMIT 1")
    suspend fun getUserByUsername(username: String): User?

    @Query("SELECT * FROM users WHERE teacherId = :teacherId LIMIT 1")
    suspend fun getUserByTeacherId(teacherId: Int): User?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: User): Long

    @Query("SELECT * FROM verification_codes")
    fun getAllVerificationCodes(): Flow<List<VerificationCode>>

    @Query("SELECT * FROM verification_codes WHERE code = :code AND isUsed = 0 LIMIT 1")
    suspend fun getValidVerificationCode(code: String): VerificationCode?

    @Update
    suspend fun updateVerificationCode(code: VerificationCode)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVerificationCode(code: VerificationCode)

    @Delete
    suspend fun deleteVerificationCode(code: VerificationCode)
}
