package com.example.nativebooks.android

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nativebooks.model.Book
import com.example.nativebooks.presentation.BooksViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

data class BooksUiState(
    val books: List<Book> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val lastSpanEndAcknowledged: Boolean = false,
)

class AndroidBooksViewModel : ViewModel() {
    private val sharedViewModel = BooksViewModel()
    private var loadJob: Job? = null

    var uiState by mutableStateOf(BooksUiState())
        private set

    fun load(forceReload: Boolean = false) {
        if (!forceReload && (uiState.books.isNotEmpty() || uiState.isLoading)) return
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            uiState = uiState.copy(isLoading = true, errorMessage = null)
            val books = sharedViewModel.loadBooks(forceReload)
            uiState = BooksUiState(
                books = books,
                isLoading = false,
                errorMessage = sharedViewModel.errorMessage,
                lastSpanEndAcknowledged = sharedViewModel.lastSpanEndAcknowledged,
            )
        }
    }

    fun reset() {
        loadJob?.cancel()
        uiState = BooksUiState()
    }
}
