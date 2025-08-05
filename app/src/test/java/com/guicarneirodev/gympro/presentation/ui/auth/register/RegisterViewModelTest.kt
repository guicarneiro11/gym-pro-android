package com.guicarneirodev.gympro.presentation.ui.auth.register

import app.cash.turbine.test
import com.guicarneirodev.gympro.R
import com.guicarneirodev.gympro.domain.model.User
import com.guicarneirodev.gympro.domain.repository.AuthRepository
import com.guicarneirodev.gympro.presentation.util.UiText
import com.guicarneirodev.gympro.util.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@ExperimentalCoroutinesApi
class RegisterViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var authRepository: AuthRepository
    private lateinit var viewModel: RegisterViewModel

    @Before
    fun setup() {
        authRepository = mockk(relaxed = true)
        viewModel = RegisterViewModel(authRepository)
    }

    @Test
    fun `password strength validation works correctly`() = runTest {
        viewModel.onEmailChange("test@test.com")
        viewModel.onPasswordChange("123456")
        viewModel.onConfirmPasswordChange("123456")
        viewModel.onRegisterClick()

        val error1 = viewModel.uiState.value.errorMessage
        assertNotNull(error1)
        assertTrue(error1 is UiText.StringResource)
        assertEquals(R.string.register_error_weak_password, (error1 as UiText.StringResource).resId)
        coVerify(exactly = 0) { authRepository.signUp(any(), any()) }

        viewModel.onEmailChange("test@test.com")

        viewModel.onPasswordChange("abcdef")
        viewModel.onConfirmPasswordChange("abcdef")
        viewModel.onRegisterClick()

        val error2 = viewModel.uiState.value.errorMessage
        assertNotNull(error2)
        assertTrue(error2 is UiText.StringResource)
        assertEquals(R.string.register_error_weak_password, (error2 as UiText.StringResource).resId)

        viewModel.onPasswordChange("Pass123")
        viewModel.onConfirmPasswordChange("Pass123")
        coEvery { authRepository.signUp(any(), any()) } returns Result.success(
            User(id = "123", email = "test@test.com")
        )

        viewModel.onRegisterClick()

        coVerify(atLeast = 1) { authRepository.signUp("test@test.com", "Pass123") }
    }

    @Test
    fun `passwords must match validation`() = runTest {
        viewModel.onEmailChange("test@test.com")
        viewModel.onPasswordChange("Pass123")
        viewModel.onConfirmPasswordChange("Pass456")

        viewModel.onRegisterClick()

        val error = viewModel.uiState.value.errorMessage
        assertNotNull(error)
        assertTrue(error is UiText.StringResource)
        assertEquals(R.string.register_error_passwords_not_match, (error as UiText.StringResource).resId)
        coVerify(exactly = 0) { authRepository.signUp(any(), any()) }
    }

    @Test
    fun `email already in use error handling`() = runTest {
        val email = "existing@test.com"
        val password = "Pass123"
        val error = Exception("email-already-in-use")

        coEvery { authRepository.signUp(email, password) } returns Result.failure(error)

        viewModel.onEmailChange(email)
        viewModel.onPasswordChange(password)
        viewModel.onConfirmPasswordChange(password)
        viewModel.onRegisterClick()

        val errorMessage = viewModel.uiState.value.errorMessage
        assertNotNull(errorMessage)
        assertTrue(errorMessage is UiText.StringResource)
        assertEquals(R.string.register_error_email_in_use, (errorMessage as UiText.StringResource).resId)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun `typing clears error messages`() = runTest {
        viewModel.onEmailChange("")
        viewModel.onRegisterClick()

        assertNotNull(viewModel.uiState.value.errorMessage)

        viewModel.onEmailChange("test")

        assertNull(viewModel.uiState.value.errorMessage)
    }

    @Test
    fun `back navigation event`() = runTest {
        viewModel.events.test {
            viewModel.onBackClick()

            val firstItem = awaitItem()
            if (firstItem == null) {
                assertEquals(RegisterEvent.NavigateBack, awaitItem())
            } else {
                assertEquals(RegisterEvent.NavigateBack, firstItem)
            }
        }
    }
}