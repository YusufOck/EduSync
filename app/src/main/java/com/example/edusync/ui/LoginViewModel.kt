package com.example.edusync.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.edusync.data.*
import com.example.edusync.util.SecurityUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

sealed class LoginResult {
    object Idle : LoginResult()
    object Loading : LoginResult()
    data class Success(val user: User) : LoginResult()
    data class Error(val message: String) : LoginResult()
}

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val userRepository: FirebaseUserRepository,
    private val teacherRepository: TeacherRepository
) : ViewModel() {

    private val _loginState = MutableStateFlow<LoginResult>(LoginResult.Idle)
    val loginState = _loginState.asStateFlow()

    init {
        // PDF Optimization: Start background initialization on IO dispatcher to avoid blocking App Startup.
        // We use a small delay or check to ensure Firebase is ready if necessary.
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val existingAdmin = userRepository.getUserByUsername("admin")
                if (existingAdmin == null) {
                    val hashedDefaultPassword = SecurityUtils.hashPassword("123")
                    userRepository.insertUser(User(username = "admin", password = hashedDefaultPassword, role = UserRole.ADMIN))
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Log.e("LoginViewModel", "Init Error: ${e.message}")
            }
        }
    }

    fun login(username: String, password: String) {
        if (username.isBlank() || password.isBlank()) {
            _loginState.value = LoginResult.Error("Lütfen tüm alanları doldurun")
            return
        }

        viewModelScope.launch {
            _loginState.value = LoginResult.Loading
            try {
                // PDF Rule 2.1: Heavy Crypto & Network work on IO/Default dispatcher
                val result = withContext(Dispatchers.IO) {
                    val user = userRepository.getUserByUsername(username)
                    val hashedInputPassword = SecurityUtils.hashPassword(password)
                    
                    if (user != null && user.password == hashedInputPassword) {
                        LoginResult.Success(user)
                    } else {
                        LoginResult.Error("Hatalı kullanıcı adı veya şifre")
                    }
                }
                _loginState.value = result
            } catch (e: Exception) {
                // PDF Rule 8: Always re-throw CancellationException
                if (e is CancellationException) throw e
                _loginState.value = LoginResult.Error("Sistem Meşgul veya Bağlantı Yok")
                Log.e("LoginViewModel", "Login Error: ${e.localizedMessage}")
            }
        }
    }

    fun resetState() {
        _loginState.value = LoginResult.Idle
    }
}
