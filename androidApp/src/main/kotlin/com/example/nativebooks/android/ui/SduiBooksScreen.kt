package com.example.nativebooks.android.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nativebooks.sdui.model.SduiBookItemState
import com.example.nativebooks.sdui.model.SduiComponentType
import com.example.nativebooks.sdui.model.SduiPaginationEvent
import com.example.nativebooks.sdui.model.SduiSearchStatus
import com.example.nativebooks.sdui.presentation.SduiBooksViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SduiBooksScreen(
    viewModel: SduiBooksViewModel,
    onBack: () -> Unit,
) {
    val state by viewModel.state.collectAsState()
    val status = state.status

    LaunchedEffect(viewModel) { viewModel.start() }
    DisposableEffect(viewModel) {
        onDispose { viewModel.clear() }
    }

    Scaffold(
        containerColor = NativeBooksColors.Paper,
        topBar = {
            TopAppBar(
                title = { Text(state.screenTitle, fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = NativeBooksColors.Paper),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { innerPadding ->
        when {
            status is SduiSearchStatus.Loading && state.sections.isEmpty() -> {
                Box(
                    Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(color = NativeBooksColors.Accent)
                }
            }

            status is SduiSearchStatus.Error && state.sections.isEmpty() -> {
                Box(
                    Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(status.apiError.message)
                        TextButton(onClick = viewModel::reload) { Text("Try again") }
                    }
                }
            }

            else -> LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .testTag("sdui.list"),
                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item(key = "search") {
                    OutlinedTextField(
                        value = state.query,
                        onValueChange = viewModel::setSearchQuery,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("sdui.search"),
                        placeholder = { Text(state.searchPlaceholder) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        trailingIcon = {
                            if (state.query.isNotEmpty()) {
                                IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear search")
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                    )
                }

                if (status is SduiSearchStatus.Loading) {
                    item(key = "search.loading") {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            LinearProgressIndicator(
                                modifier = Modifier.fillMaxWidth(),
                                color = NativeBooksColors.Accent,
                            )
                            Text(
                                "Calling fake KMP search API…",
                                style = MaterialTheme.typography.bodyMedium,
                                color = NativeBooksColors.SecondaryInk,
                            )
                        }
                    }
                }

                if (status is SduiSearchStatus.Error) {
                    item(key = "search.error") {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer,
                            ),
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    status.apiError.message,
                                    modifier = Modifier.weight(1f),
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                )
                                TextButton(onClick = viewModel::reload) { Text("Retry") }
                            }
                        }
                    }
                }

                state.sections.forEach { section ->
                    item(key = "header.${section.definition.id}") {
                        Text(
                            section.definition.title,
                            modifier = Modifier.padding(top = 10.dp, bottom = 2.dp),
                            style = MaterialTheme.typography.titleLarge,
                            color = NativeBooksColors.Ink,
                        )
                    }

                    if (section.items.isEmpty()) {
                        item(key = "empty.${section.definition.id}") {
                            Text(
                                section.definition.emptyMessage,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 14.dp),
                                color = NativeBooksColors.SecondaryInk,
                            )
                        }
                    } else {
                        when (section.definition.componentType) {
                            SduiComponentType.BOOK_ROW -> items(
                                items = section.items,
                                key = { "${section.definition.id}.${it.book.id}" },
                            ) { item ->
                                SduiBookRow(
                                    item = item,
                                    showReadingTime = state.showReadingTime,
                                    onToggleFavorite = {
                                        viewModel.toggleFavorite(item.book.id)
                                    },
                                )
                            }
                        }
                    }
                }

                if (status is SduiSearchStatus.Success) {
                    val pagination = status.result.pagination
                    item(key = "pagination") {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            if (pagination.totalPages > 0) {
                                Text(
                                    "Page ${pagination.currentPage} of ${pagination.totalPages} · " +
                                        "${status.result.components.size} loaded",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = NativeBooksColors.SecondaryInk,
                                )
                            }
                            when {
                                pagination.event == SduiPaginationEvent.LOADING -> {
                                    CircularProgressIndicator(
                                        color = NativeBooksColors.Accent,
                                    )
                                    Text("Loading next page from KMP…")
                                }
                                pagination.currentPage < pagination.totalPages -> {
                                    TextButton(
                                        onClick = viewModel::loadNextPage,
                                        modifier = Modifier.testTag("sdui.loadMore"),
                                    ) {
                                        Text("Load more")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SduiBookRow(
    item: SduiBookItemState,
    showReadingTime: Boolean,
    onToggleFavorite: () -> Unit,
) {
    val book = item.book
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = NativeBooksColors.Card),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BookCover(category = book.category, title = book.title, compact = true)
            Spacer(Modifier.width(14.dp))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(5.dp),
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
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    if (showReadingTime) {
                        "${book.author} · ${book.readingMinutes} min"
                    } else {
                        book.author
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = NativeBooksColors.SecondaryInk,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            IconButton(
                onClick = onToggleFavorite,
                modifier = Modifier.testTag("favorite.${book.id}"),
            ) {
                Icon(
                    imageVector = if (item.isFavorite) {
                        Icons.Default.Favorite
                    } else {
                        Icons.Default.FavoriteBorder
                    },
                    contentDescription = if (item.isFavorite) {
                        "Remove ${book.title} from favourites"
                    } else {
                        "Add ${book.title} to favourites"
                    },
                    tint = NativeBooksColors.Accent,
                )
            }
        }
    }
}
