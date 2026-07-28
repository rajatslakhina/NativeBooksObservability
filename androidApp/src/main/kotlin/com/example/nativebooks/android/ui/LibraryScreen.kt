package com.example.nativebooks.android.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nativebooks.android.AndroidBooksViewModel
import com.example.nativebooks.android.observability.ObservabilityStatus
import com.example.nativebooks.model.Book
import com.example.nativebooks.observability.KmpSpanStatus
import com.example.nativebooks.observability.NativeTracer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    email: String,
    observabilityStatus: ObservabilityStatus,
    viewModel: AndroidBooksViewModel,
    onBookSelected: (Book) -> Unit,
    onOpenSduiBooks: () -> Unit,
    onSignOut: () -> Unit,
) {
    val state = viewModel.uiState
    var menuExpanded by remember { mutableStateOf(false) }
    var showTelemetry by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { viewModel.load() }
    DisposableEffect(Unit) {
        val context = NativeTracer.startSpan(
            name = "screen.library.visible",
            attributes = mapOf("screen.name" to "library"),
        )
        onDispose {
            NativeTracer.endSpan(
                context,
                attributes = mapOf("screen.disappeared" to "true"),
                status = KmpSpanStatus.OK,
            )
        }
    }

    Scaffold(
        containerColor = NativeBooksColors.Paper,
        topBar = {
            TopAppBar(
                title = { Text("My library", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = NativeBooksColors.Paper),
                navigationIcon = {
                    IconButton(onClick = { showTelemetry = true }) {
                        Icon(Icons.Default.Timeline, contentDescription = "Telemetry details")
                    }
                },
                actions = {
                    IconButton(onClick = onOpenSduiBooks) {
                        Icon(
                            Icons.Default.FavoriteBorder,
                            contentDescription = "Open server-driven books",
                        )
                    }
                    Box {
                        IconButton(onClick = { menuExpanded = true }) {
                            Icon(Icons.Default.Person, contentDescription = "Account menu")
                        }
                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false },
                        ) {
                            DropdownMenuItem(
                                text = { Text(email, color = NativeBooksColors.SecondaryInk) },
                                onClick = {},
                                enabled = false,
                            )
                            DropdownMenuItem(
                                text = { Text("Reload books") },
                                leadingIcon = { Icon(Icons.Default.Refresh, contentDescription = null) },
                                onClick = {
                                    menuExpanded = false
                                    viewModel.load(forceReload = true)
                                },
                            )
                            DropdownMenuItem(
                                text = { Text("Sign out") },
                                leadingIcon = {
                                    Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null)
                                },
                                onClick = {
                                    menuExpanded = false
                                    val context = NativeTracer.startSpan("login.signout")
                                    NativeTracer.endSpan(context, status = KmpSpanStatus.OK)
                                    onSignOut()
                                },
                            )
                        }
                    }
                },
            )
        },
    ) { innerPadding ->
        when {
            state.isLoading && state.books.isEmpty() -> LoadingLibrary(Modifier.padding(innerPadding))
            state.errorMessage != null && state.books.isEmpty() -> ErrorLibrary(
                message = state.errorMessage,
                onRetry = { viewModel.load(forceReload = true) },
                modifier = Modifier.padding(innerPadding),
            )
            else -> LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .testTag("library.list"),
                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                item { LibraryIntroCard() }
                items(state.books, key = { it.id }) { book ->
                    BookRow(book = book, onClick = { onBookSelected(book) })
                }
                item { Spacer(Modifier.height(12.dp)) }
            }
        }
    }

    if (showTelemetry) {
        TelemetryDialog(
            observabilityStatus = observabilityStatus,
            kmpSpanEndAcknowledged = state.lastSpanEndAcknowledged,
            onDismiss = { showTelemetry = false },
        )
    }
}

@Composable
private fun LoadingLibrary(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = NativeBooksColors.Accent)
            Text(
                "Finding good books…",
                modifier = Modifier.padding(top = 14.dp),
                color = NativeBooksColors.SecondaryInk,
            )
        }
    }
}

@Composable
private fun ErrorLibrary(message: String?, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(message ?: "Unable to load books", color = MaterialTheme.colorScheme.error)
            TextButton(onClick = onRetry) { Text("Try again") }
        }
    }
}

@Composable
private fun LibraryIntroCard() {
    Card(
        colors = CardDefaults.cardColors(containerColor = NativeBooksColors.Card),
        shape = RoundedCornerShape(22.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                Text(
                    "CURATED FOR YOU",
                    color = NativeBooksColors.Accent,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp,
                )
                Text(
                    "Five books worth\nyour attention",
                    style = MaterialTheme.typography.headlineSmall,
                    color = NativeBooksColors.Ink,
                )
            }
            Spacer(Modifier.weight(1f))
            Icon(
                Icons.Default.Bookmark,
                contentDescription = null,
                tint = NativeBooksColors.Accent.copy(alpha = 0.75f),
                modifier = Modifier.width(42.dp),
            )
        }
    }
}

@Composable
private fun BookRow(book: Book, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("book.${book.id}"),
        colors = CardDefaults.cardColors(containerColor = NativeBooksColors.Card),
        shape = RoundedCornerShape(22.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BookCover(category = book.category, title = book.title, compact = true)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                Text(
                    book.category.uppercase(),
                    color = NativeBooksColors.Accent,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp,
                )
                Text(
                    book.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = NativeBooksColors.Ink,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    book.author,
                    style = MaterialTheme.typography.bodyMedium,
                    color = NativeBooksColors.SecondaryInk,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Icon(
                Icons.AutoMirrored.Filled.ArrowForwardIos,
                contentDescription = null,
                tint = NativeBooksColors.SecondaryInk.copy(alpha = 0.55f),
            )
        }
    }
}
