package com.example.nativebooks

import com.example.nativebooks.data.BooksRepository
import com.example.nativebooks.data.BooksApi
import com.example.nativebooks.model.Book
import com.example.nativebooks.observability.KmpSpanContext
import com.example.nativebooks.observability.KmpSpanStatus
import com.example.nativebooks.observability.KmpTracer
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class BooksRepositoryTest {
    private val repository = BooksRepository(FakeBooksApi(), RecordingTracer())

    @Test
    fun injectedApiReturnsBooks() = runTest {
        val books = repository.loadBooks()

        assertEquals(5, books.size)
        assertNotNull(repository.book("clean-architecture"))
    }

    @Test
    fun repositoryReceivesUsesAndEndsTheNativeContext() = runTest {
        val api = FakeBooksApi()
        val tracer = RecordingTracer()
        val repository = BooksRepository(api, tracer)

        repository.loadBooks()

        assertEquals(tracer.context, tracer.endedContext)
        assertEquals(KmpSpanStatus.OK, tracer.endStatus)
        assertEquals("new-relic-mobile-agent", tracer.endAttributes["network.instrumentation"])
        assertEquals(tracer.context.traceparent(), api.receivedTraceHeaders["traceparent"])
        assertTrue(tracer.endAcknowledged)
    }
}

private class FakeBooksApi : BooksApi {
    var receivedTraceHeaders: Map<String, String> = emptyMap()

    override suspend fun fetchBooks(traceHeaders: Map<String, String>): List<Book> {
        receivedTraceHeaders = traceHeaders
        return listOf(
        Book(
            id = "clean-architecture",
            title = "Clean Architecture",
            author = "Robert C. Martin",
            summary = "Test fixture",
            category = "Software Design",
            year = 2017,
            readingMinutes = 420,
        ),
        Book("pragmatic-programmer", "The Pragmatic Programmer", "David Thomas", "Test fixture", "Engineering", 2019, 360),
        Book("designing-data-intensive", "Designing Data-Intensive Applications", "Martin Kleppmann", "Test fixture", "Distributed Systems", 2017, 760),
        Book("atomic-habits", "Atomic Habits", "James Clear", "Test fixture", "Productivity", 2018, 300),
        Book("creative-act", "The Creative Act", "Rick Rubin", "Test fixture", "Creativity", 2023, 280),
        )
    }
}

private class RecordingTracer : KmpTracer {
    val context = KmpSpanContext(
        traceId = "11111111111111111111111111111111",
        spanId = "2222222222222222",
        sampled = true,
    )
    var endedContext: KmpSpanContext? = null
    var endAttributes: Map<String, String> = emptyMap()
    var endStatus = KmpSpanStatus.UNSET
    var endAcknowledged = false

    override fun startSpan(
        name: String,
        attributes: Map<String, String>,
        parent: KmpSpanContext?,
    ): KmpSpanContext = context

    override fun endSpan(
        context: KmpSpanContext,
        attributes: Map<String, String>,
        status: KmpSpanStatus,
    ): Boolean {
        endedContext = context
        endAttributes = attributes
        endStatus = status
        endAcknowledged = context == this.context
        return endAcknowledged
    }
}
