package com.example.edusync.domain.model

enum class UserRole {
    ADMIN, LECTURER
}

data class User(
    val id: Int = 0,
    val name: String,
    val surname: String,
    val department: String,
    val role: UserRole,
    val username: String = "",
    val password: String = ""
)