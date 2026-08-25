package com.example.nativebooks.data

import com.example.nativebooks.model.Book
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.client.request.parameter
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

internal interface BooksApi {
    suspend fun fetchBooks(traceHeaders: Map<String, String> = emptyMap()): List<Book>
}

/**
 * Production network source. Ktor uses NSURLSession through the Darwin engine on iOS and
 * OkHttp on Android, allowing each New Relic mobile agent to create real network spans and
 * inject its own distributed-tracing headers.
 */
internal class OpenLibraryBooksApi(
    private val client: HttpClient = defaultClient(),
) : BooksApi {
    override suspend fun fetchBooks(traceHeaders: Map<String, String>): List<Book> {
        val response = client.get("https://openlibrary.org/search.json") {
            headers {
                traceHeaders.forEach { (name, value) -> append(name, value) }
            }
            parameter("q", "software engineering design")
            parameter("fields", "key,title,author_name,first_publish_year,subject")
            parameter("limit", BOOK_LIMIT)
        }
        check(response.status == HttpStatusCode.OK) {
            "Open Library returned HTTP ${response.status.value}"
        }

        return response.body<OpenLibrarySearchResponse>().docs
            .take(BOOK_LIMIT)
            .map(OpenLibraryBook::toBook)
    }

    private companion object {
        const val BOOK_LIMIT = 5

        fun defaultClient() = HttpClient {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }
    }
}

@Serializable
private data class OpenLibrarySearchResponse(
    val docs: List<OpenLibraryBook> = emptyList(),
)

@Serializable
private data class OpenLibraryBook(
    val key: String,
    val title: String,
    @SerialName("author_name") val authors: List<String> = emptyList(),
    @SerialName("first_publish_year") val firstPublishYear: Int? = null,
    val subject: List<String> = emptyList(),
) {
    fun toBook(): Book {
        val author = authors.firstOrNull() ?: "Unknown author"
        return Book(
            id = key.substringAfterLast('/'),
            title = title,
            author = author,
            summary = "$title by $author, loaded live from the Open Library catalog.",
            category = subject.firstOrNull() ?: "Books",
            year = firstPublishYear ?: 0,
            readingMinutes = 300,
        )
    }
}
