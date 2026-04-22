package com.example.edusync.ui

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.edusync.data.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class AdminViewModel @Inject constructor(
    private val excelManager: ExcelManager,
    private val teacherRepository: TeacherRepository,
    private val userRepository: FirebaseUserRepository
) : ViewModel() {

    private val _importState = MutableStateFlow<ImportResult?>(null)
    val importState = _importState.asStateFlow()

    private val _excelPreview = MutableStateFlow<List<ExcelPreviewItem>?>(null)
    val excelPreview = _excelPreview.asStateFlow()

    private var currentUri: Uri? = null

    val teachers = teacherRepository.getAllTeachers().stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    // PDF Optimization: Use Dispatchers.Default for heavy mapping and lookup
    val verificationCodes = userRepository.getAllVerificationCodes()
        .combine(teacherRepository.getAllTeachers()) { codes, allTeachers ->
            val teacherMap = allTeachers.associateBy { it.id }
            codes.map { code ->
                val teacher = teacherMap[code.teacherId]
                code.copy(createdBy = teacher?.let { "${it.title} ${it.name} ${it.surname}" } ?: "Genel Kod")
            }
        }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun generateCodeForTeacher(teacherId: Int) {
        viewModelScope.launch {
            teacherRepository.generateCodeForTeacher(teacherId)
        }
    }

    fun loadPreview(context: Context, uri: Uri) {
        currentUri = uri
        viewModelScope.launch {
            val result = excelManager.getPreview(context, uri)
            if (result.isSuccess) {
                _excelPreview.value = result.getOrNull()
            } else {
                _importState.value = ImportResult.Error(result.exceptionOrNull()?.message ?: "Önizleme yüklenemedi")
            }
        }
    }

    fun confirmImport(context: Context) {
        val uri = currentUri ?: return
        viewModelScope.launch {
            _importState.value = ImportResult.Loading
            val result = excelManager.importExcel(context, uri)
            _importState.value = if (result.isSuccess) {
                ImportResult.Success(result.getOrNull() ?: 0)
            } else {
                ImportResult.Error(result.exceptionOrNull()?.message ?: "Aktarım hatası")
            }
            _excelPreview.value = null
        }
    }

    fun clearImportState() {
        _importState.value = null
        _excelPreview.value = null
        currentUri = null
    }
}
