package com.example.nativebooks.android.ui

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.nativebooks.android.AndroidBooksViewModel
import com.example.nativebooks.android.observability.ObservabilityStatus
import com.example.nativebooks.model.Book
import com.example.nativebooks.sdui.model.SduiScreenConfiguration
import com.example.nativebooks.sdui.presentation.SduiBooksViewModel

@Composable
fun NativeBooksApp(
    observabilityStatus: ObservabilityStatus,
    booksViewModel: AndroidBooksViewModel = viewModel(),
) {
    var signedInEmail by rememberSaveable { mutableStateOf("") }
    var selectedBook by remember { mutableStateOf<Book?>(null) }
    var showingSduiBooks by rememberSaveable { mutableStateOf(false) }

    when {
        signedInEmail.isEmpty() -> LoginScreen(
            observabilityStatus = observabilityStatus,
            onLogin = { signedInEmail = it.trim() },
        )

        showingSduiBooks -> {
            val sduiViewModel = remember {
                SduiBooksViewModel(
                    SduiScreenConfiguration(
                        titleOverride = null,
                        showReadingTime = false,
                        searchDebounceMillis = 180,
                        initialFavoriteIds = listOf("atomic-habits"),
                    ),
                )
            }
            BackHandler { showingSduiBooks = false }
            SduiBooksScreen(
                viewModel = sduiViewModel,
                onBack = { showingSduiBooks = false },
            )
        }

        selectedBook != null -> {
            BackHandler { selectedBook = null }
            BookDetailScreen(
                book = selectedBook!!,
                onBack = { selectedBook = null },
            )
        }

        else -> LibraryScreen(
            email = signedInEmail,
            observabilityStatus = observabilityStatus,
            viewModel = booksViewModel,
            onBookSelected = { selectedBook = it },
            onOpenSduiBooks = { showingSduiBooks = true },
            onSignOut = {
                booksViewModel.reset()
                showingSduiBooks = false
                signedInEmail = ""
            },
        )
    }
}
