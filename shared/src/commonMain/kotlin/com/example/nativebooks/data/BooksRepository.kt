package com.example.nativebooks.data

import com.example.nativebooks.model.Book
import com.example.nativebooks.observability.KmpSpanStatus
import com.example.nativebooks.observability.KmpTracer
import com.example.nativebooks.observability.NativeTracer

class BooksRepository internal constructor(
    private val api: DummyBooksApi,
    private val tracer: KmpTracer,
) {
    constructor() : this(DummyBooksApi(), NativeTracer)

    private var cachedBooks: List<Book> = emptyList()

    var lastSpanEndAcknowledged: Boolean = false
        private set

    suspend fun loadBooks(): List<Book> {
        val context = tracer.startSpan(
            name = "books.api.fetch",
            attributes = mapOf(
                "app.feature" to "library",
                "app.operation" to "load-books",
                "http.request.method" to "GET",
                "server.address" to "dummy.books.local",
                "url.path" to "/v1/books",
            ),
        )
        println(
            "[KMP][BooksRepository] context accepted for Ktor " +
                "traceparent=${context.traceparent() ?: "none"}",
        )

        var spanStatus = KmpSpanStatus.ERROR
        var responseStatus = "error"
        return try {
            api.fetchBooks(context).also {
                cachedBooks = it
                spanStatus = KmpSpanStatus.OK
                responseStatus = "200"
                println(
                    "[KMP][BooksRepository] received BFF books " +
                        "count=${it.size} ids=${it.joinToString(",") { book -> book.id }}",
                )
            }
        } finally {
            lastSpanEndAcknowledged = tracer.endSpan(
                context = context,
                attributes = mapOf(
                    "http.response.status_code" to responseStatus,
                    "traceparent.attached" to (api.lastTraceparent != null).toString(),
                ),
                status = spanStatus,
            )
            println(
                "[KMP][BooksRepository] request completed " +
                    "traceparentAttached=${api.lastTraceparent != null} " +
                    "endAcknowledged=$lastSpanEndAcknowledged",
            )
        }
    }

    fun book(id: String): Book? {
        val context = tracer.startSpan(
            name = "books.repository.detail",
            attributes = mapOf("book.id" to id),
        )
        val result = cachedBooks.firstOrNull { it.id == id }
        lastSpanEndAcknowledged = tracer.endSpan(
            context = context,
            attributes = mapOf("book.found" to (result != null).toString()),
            status = KmpSpanStatus.OK,
        )
        return result
    }
}
