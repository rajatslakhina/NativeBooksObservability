package com.example.nativebooks.sdui.presentation

import com.example.nativebooks.model.Book
import com.example.nativebooks.sdui.data.SduiScreenRepository
import com.example.nativebooks.sdui.model.SduiApiError
import com.example.nativebooks.sdui.model.SduiBookItemState
import com.example.nativebooks.sdui.model.SduiBooksUiState
import com.example.nativebooks.sdui.model.SduiPaginationEvent
import com.example.nativebooks.sdui.model.SduiRenderedSection
import com.example.nativebooks.sdui.model.SduiScreenConfiguration
import com.example.nativebooks.sdui.model.SduiScreenDefinition
import com.example.nativebooks.sdui.model.SduiSearchResult
import com.example.nativebooks.sdui.model.SduiSearchStatus
import com.example.nativebooks.sdui.model.SduiSectionSource
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** A cancellable bridge used by Swift without adding a native ObservableObject view model. */
class SduiObservation internal constructor(
    private var job: Job?,
) {
    fun cancel() {
        job?.cancel()
        job = null
    }
}

/**
 * Shared reactive state owner used directly by Compose and SwiftUI.
 *
 * Search input is debounced, every settled query calls the fake KMP API, and StateFlow replays the
 * latest Idle/Loading/Success/Error state. Pagination emits an intermediate Success whose
 * result.pagination.event is LOADING, preserving the already rendered components.
 */
class SduiBooksViewModel internal constructor(
    private val repository: SduiScreenRepository,
    private val configuration: SduiScreenConfiguration,
    private val scope: CoroutineScope,
) {
    constructor(configuration: SduiScreenConfiguration) : this(
        repository = SduiScreenRepository(),
        configuration = configuration,
        scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate),
    )

    private val _state = MutableStateFlow(
        SduiBooksUiState(
            screenTitle = configuration.titleOverride ?: "Discover books",
            searchPlaceholder = "Search books",
            query = "",
            status = SduiSearchStatus.Idle,
            sections = emptyList(),
            showReadingTime = configuration.showReadingTime,
        ),
    )
    val state: StateFlow<SduiBooksUiState> = _state.asStateFlow()
    val currentState: SduiBooksUiState get() = _state.value

    private var document: SduiScreenDefinition? = null
    private var loadedComponents: List<Book> = emptyList()
    private var favoriteIds = configuration.initialFavoriteIds.toSet()
    private var appliedQuery = ""
    private var loadJob: Job? = null
    private var searchJob: Job? = null
    private var paginationJob: Job? = null
    private var isCleared = false

    fun start() {
        if (document != null || loadJob?.isActive == true || isCleared) return
        load(forceReload = false)
    }

    fun reload() {
        if (isCleared) return
        load(forceReload = true)
    }

    private fun load(forceReload: Boolean) {
        searchJob?.cancel()
        paginationJob?.cancel()
        loadJob?.cancel()
        loadJob = scope.launch {
            publishStatus(SduiSearchStatus.Loading)
            try {
                document = repository.load(forceReload)
                performFreshSearch(_state.value.query)
            } catch (error: Throwable) {
                if (error is CancellationException) throw error
                publishError(error)
            }
        }
    }

    fun setSearchQuery(query: String) {
        if (isCleared) return
        _state.update { it.copy(query = query) }
        searchJob?.cancel()
        paginationJob?.cancel()
        searchJob = scope.launch {
            delay(configuration.searchDebounceMillis)
            performFreshSearch(query)
        }
    }

    fun loadNextPage() {
        if (isCleared || paginationJob?.isActive == true) return
        val success = _state.value.status as? SduiSearchStatus.Success ?: return
        val pagination = success.result.pagination
        if (
            pagination.event == SduiPaginationEvent.LOADING ||
            pagination.currentPage >= pagination.totalPages
        ) {
            return
        }

        val loadingResult = success.result.copy(
            pagination = pagination.copy(event = SduiPaginationEvent.LOADING),
        )
        publish(SduiSearchStatus.Success(loadingResult), loadedComponents)

        paginationJob = scope.launch {
            try {
                val nextPage = repository.search(
                    query = appliedQuery,
                    page = pagination.currentPage + 1,
                )
                loadedComponents = (loadedComponents + nextPage.components)
                    .distinctBy { it.id }
                publish(
                    status = SduiSearchStatus.Success(
                        nextPage.copy(components = loadedComponents),
                    ),
                    components = loadedComponents,
                )
            } catch (error: Throwable) {
                if (error is CancellationException) throw error
                publishError(error)
            }
        }
    }

    fun toggleFavorite(bookId: String) {
        if (isCleared || loadedComponents.none { it.id == bookId }) return
        favoriteIds = if (bookId in favoriteIds) {
            favoriteIds - bookId
        } else {
            favoriteIds + bookId
        }
        publish(_state.value.status, loadedComponents)
    }

    /**
     * StateFlow remains the Android API. This thin callback is only an interop surface for Swift;
     * cancelling it releases the callback and everything captured by the native view.
     */
    fun observe(observer: (SduiBooksUiState) -> Unit): SduiObservation {
        if (isCleared) {
            observer(_state.value)
            return SduiObservation(null)
        }
        return SduiObservation(
            scope.launch {
                state.collect(observer)
            },
        )
    }

    /** Must be called when the native screen permanently leaves its navigation stack. */
    fun clear() {
        if (isCleared) return
        isCleared = true
        scope.cancel()
    }

    private suspend fun performFreshSearch(query: String) {
        appliedQuery = query
        publishStatus(SduiSearchStatus.Loading)
        try {
            val result = repository.search(query = query, page = 1)
            loadedComponents = result.components
            publish(SduiSearchStatus.Success(result), loadedComponents)
        } catch (error: Throwable) {
            if (error is CancellationException) throw error
            loadedComponents = emptyList()
            publishError(error)
        }
    }

    private fun publishError(error: Throwable) {
        publish(
            status = SduiSearchStatus.Error(
                SduiApiError(
                    code = "FAKE_SEARCH_ERROR",
                    message = error.message ?: "Unable to search books",
                ),
            ),
            components = loadedComponents,
        )
    }

    private fun publishStatus(status: SduiSearchStatus) {
        publish(status = status, components = loadedComponents)
    }

    private fun publish(status: SduiSearchStatus, components: List<Book>) {
        val definition = document
        val sections = definition?.sections.orEmpty().map { section ->
            val books = when (section.source) {
                SduiSectionSource.FAVORITES -> components.filter { it.id in favoriteIds }
                SduiSectionSource.ALL -> components
            }
            SduiRenderedSection(
                definition = section,
                items = books.map { book ->
                    SduiBookItemState(book = book, isFavorite = book.id in favoriteIds)
                },
            )
        }
        _state.value = SduiBooksUiState(
            screenTitle = configuration.titleOverride ?: definition?.title ?: _state.value.screenTitle,
            searchPlaceholder = definition?.search?.placeholder ?: _state.value.searchPlaceholder,
            query = _state.value.query,
            status = status,
            sections = sections,
            showReadingTime = configuration.showReadingTime,
        )
    }
}
