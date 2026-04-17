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
    object RegisterSuccess : LoginResult()
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
                    // Admin şifresini SHA-256 ile şifreleyerek kaydediyoruz
                    val hashedDefaultPassword = SecurityUtils.hashPassword("123")
                    userRepository.insertUser(User(username = "admin", password = hashedDefaultPassword, role = UserRole.ADMIN))
                }
                userRepository.insertVerificationCode(VerificationCode(code = "HOCA2024"))
            } catch (e: Exception) {
                Log.e("LoginViewModel", "Firebase Init Error: ${e.message}")
            }
        }
    }

    fun login(username: String, password: String) {
        viewModelScope.launch {
            try {
                val user = userRepository.getUserByUsername(username)
                // Gelen şifreyi hashleyip DB'deki hash ile karşılaştırıyoruz
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

    fun registerTeacher(username: String, password: String, code: String, name: String, surname: String) {
        viewModelScope.launch {
            try {
                if (userRepository.getUserByUsername(username) != null) {
                    _loginState.value = LoginResult.Error("Bu kullanıcı adı zaten alınmış")
                    return@launch
                }

                val validCode = userRepository.getValidVerificationCode(code)
                if (validCode == null) {
                    _loginState.value = LoginResult.Error("Geçersiz veya kullanılmış doğrulama kodu")
                    return@launch
                }

                val existingTeacher = teacherRepository.getTeacherByName(name, surname)
                val teacherId: Int

                if (existingTeacher != null) {
                    val existingUser = userRepository.getUserByTeacherId(existingTeacher.id)
                    if (existingUser != null) {
                        _loginState.value = LoginResult.Error("Bu hoca ismiyle zaten bir hesap oluşturulmuş")
                        return@launch
                    }
                    teacherId = existingTeacher.id
                } else {
                    val idString = teacherRepository.insertTeacher(
                        Teacher(name = name, surname = surname)
                    )
                    teacherId = idString.hashCode()
                }

                // Kayıt sırasında şifreyi hashleyerek kaydediyoruz
                val hashedRegisterPassword = SecurityUtils.hashPassword(password)
                userRepository.insertUser(
                    User(
                        username = username,
                        password = hashedRegisterPassword,
                        role = UserRole.TEACHER,
                        teacherId = teacherId
                    )
                )

                userRepository.updateVerificationCode(validCode.copy(isUsed = true))
                _loginState.value = LoginResult.RegisterSuccess
            } catch (e: Exception) {
                _loginState.value = LoginResult.Error("Kayıt Hatası")
            }
        }
    }

    fun resetState() {
        _loginState.value = null
    }
}
