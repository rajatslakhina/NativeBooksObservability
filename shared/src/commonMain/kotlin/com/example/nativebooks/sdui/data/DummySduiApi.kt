package com.example.nativebooks.sdui.data

import com.example.nativebooks.model.Book
import com.example.nativebooks.sdui.model.SduiPaginationEvent
import com.example.nativebooks.sdui.model.SduiPaginationState
import com.example.nativebooks.sdui.model.SduiScreenDefinition
import com.example.nativebooks.sdui.model.SduiSearchResult
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.delay
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Offline Ktor BFF used to demonstrate actual request-driven reactive state.
 *
 * - GET /v1/screens/discover-books returns the SDUI layout contract.
 * - GET /v1/search?q=...&page=... returns paginated component data.
 * - Searching for "error" intentionally returns HTTP 503 so both clients can exercise Error.
 */
internal class DummySduiApi(
    private val responseDelayMillis: Long = 350,
) {
    var searchRequestCount: Int = 0
        private set

    var lastSearchQuery: String? = null
        private set

    private val json = Json { ignoreUnknownKeys = true }
    private val client = HttpClient(
        MockEngine { request ->
            when (request.url.encodedPath) {
                "/v1/screens/discover-books" -> respond(
                    content = screenJson,
                    status = HttpStatusCode.OK,
                    headers = jsonHeaders,
                )
                "/v1/search" -> {
                    searchRequestCount += 1
                    val query = request.url.parameters["q"].orEmpty()
                    val page = request.url.parameters["page"]?.toIntOrNull() ?: 1
                    val pageSize = request.url.parameters["pageSize"]?.toIntOrNull() ?: PAGE_SIZE
                    lastSearchQuery = query

                    if (query.equals("error", ignoreCase = true)) {
                        respond(
                            content = """{"message":"The fake search API is unavailable"}""",
                            status = HttpStatusCode.ServiceUnavailable,
                            headers = jsonHeaders,
                        )
                    } else {
                        respond(
                            content = json.encodeToString(search(query, page, pageSize)),
                            status = HttpStatusCode.OK,
                            headers = jsonHeaders,
                        )
                    }
                }
                else -> respond(
                    content = """{"message":"Not found"}""",
                    status = HttpStatusCode.NotFound,
                    headers = jsonHeaders,
                )
            }
        },
    ) {
        install(ContentNegotiation) {
            json(json)
        }
    }

    suspend fun fetchScreen(): SduiScreenDefinition {
        simulateNetwork()
        val response = client.get("https://dummy.books.local/v1/screens/discover-books")
        check(response.status == HttpStatusCode.OK) {
            "The server-driven screen is temporarily unavailable"
        }
        return response.body()
    }

    suspend fun searchBooks(query: String, page: Int): SduiSearchResult {
        simulateNetwork()
        val response = client.get("https://dummy.books.local/v1/search") {
            parameter("q", query)
            parameter("page", page)
            parameter("pageSize", PAGE_SIZE)
        }
        check(response.status == HttpStatusCode.OK) {
            "Search failed with HTTP ${response.status.value}. Try a different query."
        }
        return response.body()
    }

    private suspend fun simulateNetwork() {
        if (responseDelayMillis > 0) delay(responseDelayMillis)
    }

    private fun search(query: String, requestedPage: Int, pageSize: Int): SduiSearchResult {
        val normalized = query.trim()
        val matches = catalog.filter { book ->
            normalized.isBlank() ||
                book.title.contains(normalized, ignoreCase = true) ||
                book.author.contains(normalized, ignoreCase = true) ||
                book.category.contains(normalized, ignoreCase = true)
        }
        val totalPages = if (matches.isEmpty()) 0 else (matches.size + pageSize - 1) / pageSize
        val page = requestedPage.coerceIn(1, maxOf(totalPages, 1))
        val pageItems = if (totalPages == 0) {
            emptyList()
        } else {
            matches.drop((page - 1) * pageSize).take(pageSize)
        }
        return SduiSearchResult(
            query = query,
            components = pageItems,
            pagination = SduiPaginationState(
                currentPage = if (totalPages == 0) 0 else page,
                totalPages = totalPages,
                event = SduiPaginationEvent.SETTLED,
            ),
        )
    }

    private companion object {
        const val PAGE_SIZE = 3
        val jsonHeaders = headersOf(HttpHeaders.ContentType, "application/json")

        val screenJson = """
            {
              "schemaVersion": 1,
              "screenId": "discover-books",
              "title": "Discover books",
              "search": {
                "placeholder": "Search books (try “architecture” or “error”)",
                "minimumCharacters": 0
              },
              "sections": [
                {
                  "id": "favorites",
                  "title": "Favourites",
                  "source": "FAVORITES",
                  "componentType": "BOOK_ROW",
                  "emptyMessage": "Tap a heart to keep a loaded book here."
                },
                {
                  "id": "all",
                  "title": "All books",
                  "source": "ALL",
                  "componentType": "BOOK_ROW",
                  "emptyMessage": "The search API returned no matching books."
                }
              ]
            }
        """.trimIndent()

        val catalog = listOf(
            Book(
                id = "atomic-habits",
                title = "Atomic Habits",
                author = "James Clear",
                summary = "Build better habits through small improvements and thoughtful systems.",
                category = "Productivity",
                year = 2018,
                readingMinutes = 300,
            ),
            Book(
                id = "clean-architecture",
                title = "Clean Architecture",
                author = "Robert C. Martin",
                summary = "Keep business rules independent from frameworks and delivery mechanisms.",
                category = "Software Design",
                year = 2017,
                readingMinutes = 420,
            ),
            Book(
                id = "pragmatic-programmer",
                title = "The Pragmatic Programmer",
                author = "David Thomas & Andrew Hunt",
                summary = "Durable habits for developers who care about the quality of their craft.",
                category = "Engineering",
                year = 2019,
                readingMinutes = 360,
            ),
            Book(
                id = "designing-data-intensive",
                title = "Designing Data-Intensive Applications",
                author = "Martin Kleppmann",
                summary = "Trade-offs behind storage, streams, replication, and reliable data systems.",
                category = "Distributed Systems",
                year = 2017,
                readingMinutes = 760,
            ),
            Book(
                id = "creative-act",
                title = "The Creative Act",
                author = "Rick Rubin",
                summary = "A reflective guide to attention, experimentation, and creative practice.",
                category = "Creativity",
                year = 2023,
                readingMinutes = 280,
            ),
            Book(
                id = "domain-driven-design",
                title = "Domain-Driven Design",
                author = "Eric Evans",
                summary = "Model complex domains with a shared language and explicit boundaries.",
                category = "Software Design",
                year = 2003,
                readingMinutes = 620,
            ),
            Book(
                id = "refactoring",
                title = "Refactoring",
                author = "Martin Fowler",
                summary = "Improve the design of existing code through small, safe transformations.",
                category = "Engineering",
                year = 2018,
                readingMinutes = 480,
            ),
            Book(
                id = "staff-engineer",
                title = "Staff Engineer",
                author = "Will Larson",
                summary = "Explore the technical leadership paths available beyond senior engineer.",
                category = "Leadership",
                year = 2021,
                readingMinutes = 340,
            ),
        )
    }
}
