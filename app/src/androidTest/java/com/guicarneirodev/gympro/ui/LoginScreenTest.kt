package com.guicarneirodev.gympro.ui

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.guicarneirodev.gympro.MainActivity
import com.guicarneirodev.gympro.R
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LoginScreenTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun setup() {
        // Ensure we start from login screen
        // Firebase Auth should be logged out
    }

    @Test
    fun loginScreen_displaysCorrectly() {
        composeTestRule.apply {
            onNodeWithText(activity.getString(R.string.login_title))
                .assertIsDisplayed()

            onNodeWithText(activity.getString(R.string.email))
                .assertIsDisplayed()

            onNodeWithText(activity.getString(R.string.password))
                .assertIsDisplayed()

            onNodeWithText(activity.getString(R.string.login_button))
                .assertIsDisplayed()

            onNodeWithText(activity.getString(R.string.login_register_prompt))
                .assertIsDisplayed()
        }
    }

    @Test
    fun loginScreen_showsErrorOnEmptyEmail() {
        composeTestRule.apply {
            onNodeWithText(activity.getString(R.string.login_button))
                .performClick()

            waitForIdle()

            onNodeWithText(activity.getString(R.string.login_error_empty_email))
                .assertIsDisplayed()
        }
    }

    @Test
    fun loginScreen_showsErrorOnInvalidEmail() {
        composeTestRule.apply {
            onNodeWithText(activity.getString(R.string.email))
                .performTextInput("invalidemail")

            onNodeWithText(activity.getString(R.string.password))
                .performTextInput("password123")

            onNodeWithText(activity.getString(R.string.login_button))
                .performClick()

            waitForIdle()

            onNodeWithText(activity.getString(R.string.login_error_invalid_email))
                .assertIsDisplayed()
        }
    }

    @Test
    fun loginScreen_showsErrorOnShortPassword() {
        composeTestRule.apply {
            onNodeWithText(activity.getString(R.string.email))
                .performTextInput("test@example.com")

            onNodeWithText(activity.getString(R.string.password))
                .performTextInput("12345")

            onNodeWithText(activity.getString(R.string.login_button))
                .performClick()

            waitForIdle()

            onNodeWithText(activity.getString(R.string.login_error_short_password))
                .assertIsDisplayed()
        }
    }

    @Test
    fun loginScreen_navigatesToRegister() {
        composeTestRule.apply {
            onNodeWithText(activity.getString(R.string.login_register_button))
                .performClick()

            waitForIdle()

            onNodeWithText(activity.getString(R.string.register_title))
                .assertIsDisplayed()
        }
    }

    @Test
    fun loginScreen_successfulLogin() {
        composeTestRule.apply {
            onNodeWithText(activity.getString(R.string.email))
                .performTextInput("teste@gympro.com")

            onNodeWithText(activity.getString(R.string.password))
                .performTextInput("teste123")

            onNodeWithText(activity.getString(R.string.login_button))
                .performClick()

            waitForIdle()

            onNodeWithText(activity.getString(R.string.workouts_title))
                .assertIsDisplayed()
        }
    }
}