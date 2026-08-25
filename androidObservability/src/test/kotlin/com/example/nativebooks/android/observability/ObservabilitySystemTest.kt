package com.example.nativebooks.android.observability

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ObservabilitySystemTest {
    @Test
    fun newRelicAttributesOmitBlankStringValues() {
        val attributes = mapOf(
            "empty" to "",
            "whitespace" to "   \n",
            "name" to "explore.search",
            "page" to "1",
        )

        val sanitized = attributes.withoutBlankStringValues()

        assertFalse(sanitized.containsKey("empty"))
        assertFalse(sanitized.containsKey("whitespace"))
        assertEquals("explore.search", sanitized["name"])
        assertEquals("1", sanitized["page"])
        assertTrue(sanitized.size == 2)
    }
}
