package com.example.nativebooks

import com.example.nativebooks.data.BooksRepository
import com.example.nativebooks.data.DummyBooksApi
import com.example.nativebooks.observability.KmpSpanContext
import com.example.nativebooks.observability.KmpSpanStatus
import com.example.nativebooks.observability.KmpTracer
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class BooksRepositoryTest {
    private val repository = BooksRepository()

    @Test
    fun dummyApiReturnsBooks() = runTest {
        val books = repository.loadBooks()

        assertEquals(5, books.size)
        assertNotNull(repository.book("clean-architecture"))
    }

    @Test
    fun ktorRequestCarriesNativeTraceparent() = runTest {
        val api = DummyBooksApi()
        val context = KmpSpanContext(
            traceId = "0123456789abcdef0123456789abcdef",
            spanId = "0123456789abcdef",
            sampled = true,
        )

        api.fetchBooks(context)

        assertEquals(
            "00-0123456789abcdef0123456789abcdef-0123456789abcdef-01",
            api.lastTraceparent,
        )
    }

    @Test
    fun repositoryReceivesUsesAndEndsTheNativeContext() = runTest {
        val api = DummyBooksApi()
        val tracer = RecordingTracer()
        val repository = BooksRepository(api, tracer)

        repository.loadBooks()

        assertEquals(tracer.context, tracer.endedContext)
        assertEquals(tracer.context.traceparent(), api.lastTraceparent)
        assertEquals(KmpSpanStatus.OK, tracer.endStatus)
        assertEquals("true", tracer.endAttributes["traceparent.attached"])
        assertTrue(tracer.endAcknowledged)
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
