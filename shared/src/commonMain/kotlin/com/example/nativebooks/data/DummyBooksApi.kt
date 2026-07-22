package com.example.nativebooks.data

import com.example.nativebooks.model.Book
import com.example.nativebooks.observability.KmpSpanContext
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf

/**
 * A deterministic Ktor-backed stand-in for a network API. The MockEngine keeps the sample
 * offline while exercising the same request/header pipeline as a real service.
 */
internal class DummyBooksApi {
    var lastTraceparent: String? = null
        private set

    private val engine = MockEngine { request ->
        lastTraceparent = request.headers["traceparent"]
        respond(
            content = "{\"status\":\"ok\"}",
            status = HttpStatusCode.OK,
            headers = headersOf(HttpHeaders.ContentType, "application/json"),
        )
    }
    private val client = HttpClient(engine)

    suspend fun fetchBooks(spanContext: KmpSpanContext): List<Book> {
        val response = client.get("https://dummy.books.local/v1/books") {
            spanContext.traceparent()?.let { header("traceparent", it) }
        }
        println(
            "[KMP][DummyBooksApi] received BFF response " +
                "status=${response.status.value} " +
                "traceparentAttached=${lastTraceparent != null}",
        )

        return listOf(
            Book(
                id = "clean-architecture",
                title = "Clean Architecture",
                author = "Robert C. Martin",
                summary = "A practical guide to designing software systems whose core business rules stay independent from frameworks, delivery mechanisms, and infrastructure.",
                category = "Software Design",
                year = 2017,
                readingMinutes = 420,
            ),
            Book(
                id = "pragmatic-programmer",
                title = "The Pragmatic Programmer",
                author = "David Thomas & Andrew Hunt",
                summary = "A collection of durable habits for developers: make change easy, automate repetition, communicate clearly, and take responsibility for the quality of your craft.",
                category = "Engineering",
                year = 2019,
                readingMinutes = 360,
            ),
            Book(
                id = "designing-data-intensive",
                title = "Designing Data-Intensive Applications",
                author = "Martin Kleppmann",
                summary = "An exploration of the trade-offs behind storage engines, distributed systems, streams, replication, and reliable data platforms.",
                category = "Distributed Systems",
                year = 2017,
                readingMinutes = 760,
            ),
            Book(
                id = "atomic-habits",
                title = "Atomic Habits",
                author = "James Clear",
                summary = "A systems-oriented approach to building better habits through small improvements, thoughtful environments, and identity-based behavior change.",
                category = "Productivity",
                year = 2018,
                readingMinutes = 300,
            ),
            Book(
                id = "creative-act",
                title = "The Creative Act",
                author = "Rick Rubin",
                summary = "A calm, reflective guide to attention, experimentation, and the everyday practice of making meaningful creative work.",
                category = "Creativity",
                year = 2023,
                readingMinutes = 280,
            ),
        )
    }
}
