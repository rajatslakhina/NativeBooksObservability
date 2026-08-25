package com.example.nativebooks.sdui.data

import com.example.nativebooks.sdui.model.SduiScreenDefinition
import com.example.nativebooks.sdui.model.SduiSearchResult

internal class SduiScreenRepository(
    private val api: SduiApi = OpenLibrarySduiApi(),
) {
    private var cachedScreen: SduiScreenDefinition? = null

    suspend fun load(forceReload: Boolean): SduiScreenDefinition {
        if (!forceReload) cachedScreen?.let { return it }
        return api.fetchScreen().also { cachedScreen = it }
    }

    suspend fun search(query: String, page: Int): SduiSearchResult =
        api.searchBooks(query = query, page = page)
}
