package com.example.nativebooks.sdui.data

import com.example.nativebooks.model.Book
import com.example.nativebooks.sdui.model.SduiComponentType
import com.example.nativebooks.sdui.model.SduiPaginationEvent
import com.example.nativebooks.sdui.model.SduiPaginationState
import com.example.nativebooks.sdui.model.SduiScreenDefinition
import com.example.nativebooks.sdui.model.SduiSearchDefinition
import com.example.nativebooks.sdui.model.SduiSearchResult
import com.example.nativebooks.sdui.model.SduiSectionDefinition
import com.example.nativebooks.sdui.model.SduiSectionSource
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

internal interface SduiApi {
    suspend fun fetchScreen(): SduiScreenDefinition
    suspend fun searchBooks(query: String, page: Int): SduiSearchResult
}

/** Real search source used by both native apps' Explore screen. */
internal class OpenLibrarySduiApi(
    private val client: HttpClient = HttpClient {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    },
) : SduiApi {
    override suspend fun fetchScreen(): SduiScreenDefinition = SduiScreenDefinition(
        schemaVersion = 1,
        screenId = "discover-books",
        title = "Discover books",
        search = SduiSearchDefinition(
            placeholder = "Search the Open Library catalog",
            minimumCharacters = 0,
        ),
        sections = listOf(
            SduiSectionDefinition(
                id = "favorites",
                title = "Favourites",
                source = SduiSectionSource.FAVORITES,
                componentType = SduiComponentType.BOOK_ROW,
                emptyMessage = "Tap a heart to keep a loaded book here.",
            ),
            SduiSectionDefinition(
                id = "all",
                title = "All books",
                source = SduiSectionSource.ALL,
                componentType = SduiComponentType.BOOK_ROW,
                emptyMessage = "Open Library returned no matching books.",
            ),
        ),
    )

    override suspend fun searchBooks(query: String, page: Int): SduiSearchResult {
        val effectiveQuery = query.trim().ifEmpty { "software engineering design" }
        val response = client.get("https://openlibrary.org/search.json") {
            parameter("q", effectiveQuery)
            parameter("fields", "key,title,author_name,first_publish_year,subject")
            parameter("page", page)
            parameter("limit", PAGE_SIZE)
        }
        check(response.status == HttpStatusCode.OK) {
            "Open Library search failed with HTTP ${response.status.value}"
        }

        val payload = response.body<OpenLibrarySduiResponse>()
        val totalPages = if (payload.numFound == 0) 0 else (payload.numFound + PAGE_SIZE - 1) / PAGE_SIZE
        return SduiSearchResult(
            query = query,
            components = payload.docs.map(OpenLibrarySduiBook::toBook),
            pagination = SduiPaginationState(
                currentPage = if (totalPages == 0) 0 else page,
                totalPages = totalPages,
                event = SduiPaginationEvent.SETTLED,
            ),
        )
    }

    private companion object {
        const val PAGE_SIZE = 3
    }
}

@Serializable
private data class OpenLibrarySduiResponse(
    @SerialName("numFound") val numFound: Int = 0,
    val docs: List<OpenLibrarySduiBook> = emptyList(),
)

@Serializable
private data class OpenLibrarySduiBook(
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
