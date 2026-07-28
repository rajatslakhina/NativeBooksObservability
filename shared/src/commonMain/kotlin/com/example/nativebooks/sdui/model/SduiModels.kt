package com.example.nativebooks.sdui.model

import com.example.nativebooks.model.Book
import kotlinx.serialization.Serializable

/**
 * Component types are the versioned contract between the server document and each native
 * renderer. Adding a type requires an implementation on both platforms before the API emits it.
 */
@Serializable
enum class SduiComponentType {
    BOOK_ROW,
}

@Serializable
enum class SduiSectionSource {
    FAVORITES,
    ALL,
}

@Serializable
data class SduiSearchDefinition(
    val placeholder: String,
    val minimumCharacters: Int,
)

@Serializable
data class SduiSectionDefinition(
    val id: String,
    val title: String,
    val source: SduiSectionSource,
    val componentType: SduiComponentType,
    val emptyMessage: String,
)

/** The document and content that a real KMP network client would decode from JSON. */
@Serializable
data class SduiScreenDefinition(
    val schemaVersion: Int,
    val screenId: String,
    val title: String,
    val search: SduiSearchDefinition,
    val sections: List<SduiSectionDefinition>,
)

@Serializable
enum class SduiPaginationEvent {
    IDLE,
    LOADING,
    SETTLED,
}

@Serializable
data class SduiPaginationState(
    val currentPage: Int,
    val totalPages: Int,
    val event: SduiPaginationEvent,
)

@Serializable
data class SduiSearchResult(
    val query: String,
    val components: List<Book>,
    val pagination: SduiPaginationState,
)

data class SduiApiError(
    val code: String,
    val message: String,
)

/**
 * A state is replayable; Success carries the complete result needed to render the current page
 * set. Unlike a transient event stream, a late collector can always recover the latest state.
 */
sealed class SduiSearchStatus {
    data object Idle : SduiSearchStatus()
    data object Loading : SduiSearchStatus()
    data class Success(val result: SduiSearchResult) : SduiSearchStatus()
    data class Error(val apiError: SduiApiError) : SduiSearchStatus()
}

/**
 * Host-provided policy for the small differences a shared screen may need on each platform.
 * Business state and event handling remain in the same shared view model.
 */
data class SduiScreenConfiguration(
    val titleOverride: String?,
    val showReadingTime: Boolean,
    val searchDebounceMillis: Long,
    val initialFavoriteIds: List<String>,
)

data class SduiBookItemState(
    val book: Book,
    val isFavorite: Boolean,
)

data class SduiRenderedSection(
    val definition: SduiSectionDefinition,
    val items: List<SduiBookItemState>,
)

data class SduiBooksUiState(
    val screenTitle: String,
    val searchPlaceholder: String,
    val query: String,
    val status: SduiSearchStatus,
    val sections: List<SduiRenderedSection>,
    val showReadingTime: Boolean,
)
