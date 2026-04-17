package com.example.edusync.ui

/**
 * Phase 2 - Asynchronous State Management
 * Excel aktarım durumlarını merkezi bir yerden yönetmek için kullanılır.
 */
sealed class ImportResult {
    object Loading : ImportResult()
    data class Success(val count: Int) : ImportResult()
    data class Error(val message: String) : ImportResult()
}
