package com.example.edusync.ui

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.edusync.data.ExcelManager
import com.example.edusync.data.ExcelPreviewItem
import com.example.edusync.data.UserDao
import com.example.edusync.data.VerificationCode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

sealed class ImportResult {
    object Loading : ImportResult()
    data class Success(val count: Int) : ImportResult()
    data class Error(val message: String) : ImportResult()
}

@HiltViewModel
class AdminViewModel @Inject constructor(
    private val excelManager: ExcelManager,
    private val userDao: UserDao
) : ViewModel() {

    private val _importState = MutableStateFlow<ImportResult?>(null)
    val importState = _importState.asStateFlow()

    private val _excelPreview = MutableStateFlow<List<ExcelPreviewItem>?>(null)
    val excelPreview = _excelPreview.asStateFlow()

    private var currentUri: Uri? = null

    // Kayıt Kodları - Doğrudan DB'den dinle
    val verificationCodes = userDao.getAllVerificationCodes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun generateCode() {
        viewModelScope.launch {
            val newCode = UUID.randomUUID().toString().substring(0, 8).uppercase()
            userDao.insertVerificationCode(VerificationCode(code = newCode))
        }
    }

    fun loadPreview(context: Context, uri: Uri) {
        currentUri = uri
        val result = excelManager.getPreview(context, uri)
        if (result.isSuccess) {
            _excelPreview.value = result.getOrNull()
        } else {
            _importState.value = ImportResult.Error(result.exceptionOrNull()?.message ?: "Önizleme yüklenemedi")
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
