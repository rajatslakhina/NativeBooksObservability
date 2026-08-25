package com.example.nativebooks.presentation

import com.example.nativebooks.data.BooksRepository
import com.example.nativebooks.model.Book
import com.example.nativebooks.observability.KmpSpanStatus
import com.example.nativebooks.observability.KmpTracer
import com.example.nativebooks.observability.NativeTracer

/** Business-facing view-model state; Swift only adapts it to ObservableObject. */
class BooksViewModel(
    private val repository: BooksRepository,
    private val tracer: KmpTracer,
) {
    constructor() : this(BooksRepository(), NativeTracer)

    var books: List<Book> = emptyList()
        private set

    var isLoading: Boolean = false
        private set

    var errorMessage: String? = null
        private set

    var lastSpanEndAcknowledged: Boolean = false
        private set

    suspend fun loadBooks(forceReload: Boolean = false): List<Book> {
        if (!forceReload && books.isNotEmpty()) return books

        isLoading = true
        errorMessage = null
        val context = tracer.startSpan(
            name = "books.viewmodel.load",
            attributes = mapOf("books.force_reload" to forceReload.toString()),
        )
        var spanStatus = KmpSpanStatus.UNSET

        return try {
            repository.loadBooks(parent = context).also {
                books = it
                spanStatus = KmpSpanStatus.OK
            }
        } catch (error: Throwable) {
            spanStatus = KmpSpanStatus.ERROR
            errorMessage = error.message ?: "Unable to load books"
            books = emptyList()
            books
        } finally {
            val viewModelSpanEnded = tracer.endSpan(
                context = context,
                attributes = buildMap {
                    put("books.count", books.size.toString())
                    errorMessage?.let { put("error.message", it) }
                },
                status = spanStatus,
            )
            lastSpanEndAcknowledged = viewModelSpanEnded && repository.lastSpanEndAcknowledged
            isLoading = false
        }
    }

    fun book(id: String): Book? = books.firstOrNull { it.id == id }
}
