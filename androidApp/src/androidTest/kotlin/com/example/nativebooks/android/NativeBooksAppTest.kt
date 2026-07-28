package com.example.nativebooks.android

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NativeBooksAppTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun loginOpensKmpBackedLibrary() {
        composeRule.onNodeWithTag("login.button").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText("My library").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("My library").assertIsDisplayed()
        composeRule.onNodeWithTag("library.list").assertIsDisplayed()
    }

    @Test
    fun serverDrivenScreenRendersSearchAndBothSections() {
        composeRule.onNodeWithTag("login.button").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText("My library").fetchSemanticsNodes().isNotEmpty()
        }

        composeRule.onNodeWithContentDescription("Open server-driven books").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText("All books").fetchSemanticsNodes().isNotEmpty()
        }

        composeRule.onNodeWithTag("sdui.search").assertIsDisplayed()
        composeRule.onNodeWithText("Favourites").assertIsDisplayed()
        composeRule.onNodeWithText("All books").assertIsDisplayed()

        // "Staff Engineer" is not in page one, so seeing it proves the debounced KMP search
        // request updated the native Compose renderer rather than only filtering visible rows.
        composeRule.onNodeWithTag("sdui.search").performTextReplacement("leadership")
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText("Staff Engineer").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("Staff Engineer").assertIsDisplayed()
    }
}
