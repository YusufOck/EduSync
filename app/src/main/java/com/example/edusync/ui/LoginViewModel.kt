package com.example.edusync.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.edusync.data.*
import com.example.edusync.util.SecurityUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class LoginResult {
    data class Success(val user: User) : LoginResult()
    data class Error(val message: String) : LoginResult()
}

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val userRepository: FirebaseUserRepository,
    private val teacherRepository: TeacherRepository
) : ViewModel() {

    private val _loginState = MutableStateFlow<LoginResult?>(null)
    val loginState = _loginState.asStateFlow()

    init {
        viewModelScope.launch {
            try {
                val existingAdmin = userRepository.getUserByUsername("admin")
                if (existingAdmin == null) {
                    val hashedDefaultPassword = SecurityUtils.hashPassword("123")
                    userRepository.insertUser(User(username = "admin", password = hashedDefaultPassword, role = UserRole.ADMIN))
                }
            } catch (e: Exception) {
                Log.e("LoginViewModel", "Firebase Init Error: ${e.message}")
            }
        }
    }

    fun login(username: String, password: String) {
        viewModelScope.launch {
            try {
                val user = userRepository.getUserByUsername(username)
                val hashedInputPassword = SecurityUtils.hashPassword(password)
                if (user != null && user.password == hashedInputPassword) {
                    _loginState.value = LoginResult.Success(user)
                } else {
                    _loginState.value = LoginResult.Error("Hatalı kullanıcı adı veya şifre")
                }
            } catch (e: Exception) {
                _loginState.value = LoginResult.Error("Bağlantı Hatası")
            }
        }
    }

    fun resetState() {
        _loginState.value = null
    }
}
