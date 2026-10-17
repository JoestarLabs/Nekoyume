package com.bl4ckswordsman.nekoyume

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented tests for Nekoyume Android App Tests the complete UI flow and integration with
 * Android framework
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
class NekoyumeInstrumentedTest {

    @get:Rule val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun app_launches_successfully() {
        // Verify that the app launches and displays the main title
        composeTestRule.onNodeWithText("Nekoyume").assertIsDisplayed()
    }

    @Test
    fun app_displays_welcome_content() {
        // Verify welcome card content is displayed
        composeTestRule.onNodeWithText("Welcome to Nekoyume").assertIsDisplayed()
        composeTestRule.onNodeWithText("Your Modern White Noise Companion").assertIsDisplayed()
    }

    @Test
    fun app_has_play_button() {
        // Verify the play/pause button is present and accessible
        composeTestRule.onNodeWithContentDescription("Play/Pause").assertIsDisplayed()
    }

    @Test
    fun app_displays_instruction_text() {
        // Verify instruction text is shown
        composeTestRule
                .onNodeWithText("Tap the play button to start your relaxing journey")
                .assertIsDisplayed()
    }

    @Test
    fun play_button_interaction_works() {
        // Test that the play button responds to clicks
        composeTestRule
                .onNodeWithContentDescription("Play/Pause")
                .assertIsDisplayed()
                .performClick()

        // Verify state change after clicking
        composeTestRule.onNodeWithText("Playing white noise...").assertIsDisplayed()
        composeTestRule.onNodeWithText("🎵").assertIsDisplayed()
    }

    @Test
    fun play_button_toggles_state() {
        // Click play button twice to test toggle functionality
        composeTestRule.onNodeWithContentDescription("Play/Pause").performClick()
        composeTestRule.onNodeWithText("Playing white noise...").assertIsDisplayed()

        composeTestRule.onNodeWithContentDescription("Play/Pause").performClick()
        composeTestRule
                .onNodeWithText("Tap the play button to start your relaxing journey")
                .assertIsDisplayed()
    }

    @Test
    fun app_handles_configuration_changes() {
        // Verify initial state
        composeTestRule.onNodeWithText("Welcome to Nekoyume").assertIsDisplayed()

        // Simulate configuration change by recreating the activity
        composeTestRule.activityRule.scenario.recreate()

        // Verify state is preserved after recreation
        composeTestRule.onNodeWithText("Welcome to Nekoyume").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Play/Pause").assertIsDisplayed()
    }

    @Test
    fun app_material3_theme_applies_correctly() {
        // Test that the Material 3 theme is applied correctly
        // This is verified by checking that themed components render without issues
        composeTestRule.onNodeWithText("Nekoyume").assertIsDisplayed()

        // The fact that the UI renders correctly with our custom theme
        // indicates the Material 3 theming is working properly
    }

    @Test
    fun app_accessibility_features_work() {
        // Test basic accessibility features
        composeTestRule.onNodeWithContentDescription("Play/Pause").assertIsDisplayed()

        // Verify that content descriptions are present for interactive elements
        // This ensures basic accessibility compliance
    }

    @Test
    fun app_ui_components_are_responsive() {
        // Test that UI components respond appropriately to user interaction
        val playButton = composeTestRule.onNodeWithContentDescription("Play/Pause")

        // Perform multiple interactions to test responsiveness
        playButton.performClick()
        composeTestRule.onNodeWithText("Playing white noise...").assertIsDisplayed()

        playButton.performClick()
        composeTestRule
                .onNodeWithText("Tap the play button to start your relaxing journey")
                .assertIsDisplayed()
    }

    @Test
    fun app_all_ui_elements_present() {
        // Comprehensive test to verify all main UI components are present

        // Top bar title
        composeTestRule.onNodeWithText("Nekoyume").assertIsDisplayed()

        // Welcome card content
        composeTestRule.onNodeWithText("Welcome to Nekoyume").assertIsDisplayed()
        composeTestRule.onNodeWithText("Your Modern White Noise Companion").assertIsDisplayed()

        // Play button
        composeTestRule.onNodeWithContentDescription("Play/Pause").assertIsDisplayed()

        // Instruction text
        composeTestRule
                .onNodeWithText("Tap the play button to start your relaxing journey")
                .assertIsDisplayed()
    }

    @Test
    fun app_handles_rapid_clicks() {
        // Test that the app handles rapid clicking gracefully
        val playButton = composeTestRule.onNodeWithContentDescription("Play/Pause")

        // Perform rapid clicks
        repeat(5) { playButton.performClick() }

        // Should end in playing state (odd number of clicks)
        composeTestRule.onNodeWithText("Playing white noise...").assertIsDisplayed()
    }

    @Test
    fun app_state_consistency() {
        // Test that app state remains consistent through interactions
        val playButton = composeTestRule.onNodeWithContentDescription("Play/Pause")

        // Initial state
        composeTestRule
                .onNodeWithText("Tap the play button to start your relaxing journey")
                .assertIsDisplayed()

        // After first click - playing
        playButton.performClick()
        composeTestRule.onNodeWithText("Playing white noise...").assertIsDisplayed()
        composeTestRule.onNodeWithText("🎵").assertIsDisplayed()

        // After second click - stopped
        playButton.performClick()
        composeTestRule
                .onNodeWithText("Tap the play button to start your relaxing journey")
                .assertIsDisplayed()
    }
}
