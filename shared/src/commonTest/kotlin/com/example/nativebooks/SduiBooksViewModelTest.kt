package com.example.nativebooks

import com.example.nativebooks.sdui.data.DummySduiApi
import com.example.nativebooks.sdui.data.SduiScreenRepository
import com.example.nativebooks.sdui.model.SduiPaginationEvent
import com.example.nativebooks.sdui.model.SduiScreenConfiguration
import com.example.nativebooks.sdui.model.SduiSearchStatus
import com.example.nativebooks.sdui.presentation.SduiBooksViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class SduiBooksViewModelTest {
    private val configuration = SduiScreenConfiguration(
        titleOverride = null,
        showReadingTime = true,
        searchDebounceMillis = 200,
        initialFavoriteIds = listOf("atomic-habits"),
    )

    @Test
    fun initialRequestTransitionsFromIdleThroughLoadingToSuccessResult() = runTest {
        val api = DummySduiApi(responseDelayMillis = 100)
        val viewModel = viewModel(api)
        assertIs<SduiSearchStatus.Idle>(viewModel.currentState.status)

        viewModel.start()
        runCurrent()
        assertIs<SduiSearchStatus.Loading>(viewModel.currentState.status)

        val state = viewModel.state.first { it.status is SduiSearchStatus.Success }
        val success = assertIs<SduiSearchStatus.Success>(state.status)

        assertEquals(1, api.searchRequestCount)
        assertEquals("", api.lastSearchQuery)
        assertEquals(listOf("favorites", "all"), state.sections.map { it.definition.id })
        assertEquals(listOf("atomic-habits"), state.sections.first().items.map { it.book.id })
        assertEquals(3, success.result.components.size)
        assertEquals(1, success.result.pagination.currentPage)
        assertEquals(3, success.result.pagination.totalPages)
    }

    @Test
    fun debouncedQueryCallsFakeApiAndPublishesSuccessPayload() = runTest {
        val api = DummySduiApi(responseDelayMillis = 100)
        val viewModel = viewModel(api)
        viewModel.start()
        viewModel.state.first { it.status is SduiSearchStatus.Success }

        viewModel.setSearchQuery("architecture")
        assertIs<SduiSearchStatus.Success>(viewModel.currentState.status)
        assertEquals(1, api.searchRequestCount)

        advanceTimeBy(200)
        runCurrent()
        assertIs<SduiSearchStatus.Loading>(viewModel.currentState.status)
        assertEquals(1, api.searchRequestCount)

        advanceTimeBy(100)
        val settledState = viewModel.state.first {
            val status = it.status
            status is SduiSearchStatus.Success && status.result.query == "architecture"
        }
        val success = assertIs<SduiSearchStatus.Success>(settledState.status)

        assertEquals(2, api.searchRequestCount)
        assertEquals("architecture", api.lastSearchQuery)
        assertEquals(listOf("clean-architecture"), success.result.components.map { it.id })
        assertEquals(
            listOf("clean-architecture"),
            viewModel.currentState.sections.last().items.map { it.book.id },
        )
    }

    @Test
    fun paginationPreservesRowsWhileLoadingAndAppendsNextResult() = runTest {
        val viewModel = viewModel(DummySduiApi(responseDelayMillis = 100))
        viewModel.start()
        viewModel.state.first { it.status is SduiSearchStatus.Success }

        viewModel.loadNextPage()
        val inFlight = assertIs<SduiSearchStatus.Success>(viewModel.currentState.status)
        assertEquals(SduiPaginationEvent.LOADING, inFlight.result.pagination.event)
        assertEquals(3, viewModel.currentState.sections.last().items.size)

        val settledState = viewModel.state.first {
            val status = it.status
            status is SduiSearchStatus.Success &&
                status.result.pagination.currentPage == 2 &&
                status.result.pagination.event == SduiPaginationEvent.SETTLED
        }
        val settled = assertIs<SduiSearchStatus.Success>(settledState.status)
        assertEquals(SduiPaginationEvent.SETTLED, settled.result.pagination.event)
        assertEquals(2, settled.result.pagination.currentPage)
        assertEquals(6, settled.result.components.size)
        assertEquals(6, viewModel.currentState.sections.last().items.size)
    }

    @Test
    fun fakeErrorQueryPublishesTypedErrorState() = runTest {
        val api = DummySduiApi(responseDelayMillis = 100)
        val viewModel = viewModel(api)
        viewModel.start()
        viewModel.state.first { it.status is SduiSearchStatus.Success }

        viewModel.setSearchQuery("error")
        val errorState = viewModel.state.first { it.status is SduiSearchStatus.Error }
        val error = assertIs<SduiSearchStatus.Error>(errorState.status)
        assertEquals("FAKE_SEARCH_ERROR", error.apiError.code)
        assertTrue(error.apiError.message.contains("503"))
        assertTrue(viewModel.currentState.sections.all { it.items.isEmpty() })
    }

    @Test
    fun cancelledObservationReleasesTheCallback() = runTest {
        val viewModel = viewModel(DummySduiApi(responseDelayMillis = 0))
        var emissionCount = 0
        val observation = viewModel.observe { emissionCount += 1 }
        viewModel.start()
        advanceUntilIdle()
        runCurrent()
        val countAtCancellation = emissionCount

        observation.cancel()
        runCurrent()
        viewModel.toggleFavorite("atomic-habits")
        runCurrent()

        assertEquals(countAtCancellation, emissionCount)
        viewModel.clear()
    }

    private fun kotlinx.coroutines.test.TestScope.viewModel(
        api: DummySduiApi,
    ): SduiBooksViewModel = SduiBooksViewModel(
        repository = SduiScreenRepository(api),
        configuration = configuration,
        scope = CoroutineScope(SupervisorJob() + StandardTestDispatcher(testScheduler)),
    )
}
