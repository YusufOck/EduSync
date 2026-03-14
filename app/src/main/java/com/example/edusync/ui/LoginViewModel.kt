package com.example.edusync.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.edusync.data.Teacher
import com.example.edusync.data.TeacherRepository
import com.example.edusync.data.User
import com.example.edusync.data.UserDao
import com.example.edusync.data.UserRole
import com.example.edusync.data.VerificationCode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val userDao: UserDao,
    private val teacherRepository: TeacherRepository
) : ViewModel() {

    private val _loginState = MutableStateFlow<LoginResult?>(null)
    val loginState = _loginState.asStateFlow()

    init {
        viewModelScope.launch {
            if (userDao.getUserByUsername("admin") == null) {
                userDao.insertUser(User(username = "admin", password = "123", role = UserRole.ADMIN))
            }
            userDao.insertVerificationCode(VerificationCode(code = "HOCA2024"))
        }
    }

    fun login(username: String, password: String) {
        viewModelScope.launch {
            val user = userDao.getUserByUsername(username)
            if (user != null && user.password == password) {
                _loginState.value = LoginResult.Success(user)
            } else {
                _loginState.value = LoginResult.Error("Hatalı kullanıcı adı veya şifre")
            }
        }
    }

    fun registerTeacher(username: String, password: String, code: String, name: String, surname: String) {
        viewModelScope.launch {
            // 1. Kullanıcı adı çakışma kontrolü
            if (userDao.getUserByUsername(username) != null) {
                _loginState.value = LoginResult.Error("Bu kullanıcı adı zaten alınmış")
                return@launch
            }

            // 2. Kod kontrolü
            val validCode = userDao.getValidVerificationCode(code)
            if (validCode == null) {
                _loginState.value = LoginResult.Error("Geçersiz veya kullanılmış doğrulama kodu")
                return@launch
            }

            // 3. Hoca Çakışma ve Eşleşme Mantığı
            // Eğer Excel'den bu isimde bir hoca zaten eklenmişse, yeni hoca oluşturmak yerine onu kullanmalıyız.
            var existingTeacher = teacherRepository.getTeacherByName(name, surname)
            
            val teacherId: Int
            if (existingTeacher != null) {
                // Bu hoca zaten sistemde var (Excel'den gelmiş olabilir).
                // Ama bu hocaya bağlı bir kullanıcı hesabı var mı?
                val existingUser = userDao.getUserByTeacherId(existingTeacher.id)
                if (existingUser != null) {
                    _loginState.value = LoginResult.Error("Bu hoca ismiyle zaten bir hesap oluşturulmuş")
                    return@launch
                }
                teacherId = existingTeacher.id
            } else {
                // Hoca sistemde yok, yeni hoca kaydı oluştur
                teacherId = teacherRepository.insertTeacher(
                    Teacher(name = name, surname = surname)
                ).toInt()
            }

            // 4. Kullanıcıyı hoca ID'si ile oluştur
            userDao.insertUser(
                User(
                    username = username,
                    password = password,
                    role = UserRole.TEACHER,
                    teacherId = teacherId
                )
            )

            // 5. Kodu yak
            userDao.updateVerificationCode(validCode.copy(isUsed = true))
            
            _loginState.value = LoginResult.RegisterSuccess
        }
    }

    fun resetState() {
        _loginState.value = null
    }
}

sealed class LoginResult {
    data class Success(val user: User) : LoginResult()
    object RegisterSuccess : LoginResult()
    data class Error(val message: String) : LoginResult()
}
