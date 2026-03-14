package com.example.edusync.domain.model

data class Course(
    val id: Int = 0,
    val code: String,
    val name: String,
    val lecturerName: String,
    val department: String
)