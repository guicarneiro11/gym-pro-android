package com.guicarneirodev.gympro.presentation.ui.auth.login

import app.cash.turbine.test
import com.guicarneirodev.gympro.R
import com.guicarneirodev.gympro.domain.model.User
import com.guicarneirodev.gympro.domain.repository.AuthRepository
import com.guicarneirodev.gympro.presentation.util.UiText
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@ExperimentalCoroutinesApi
class LoginViewModelTest {

    private lateinit var authRepository: AuthRepository
    private lateinit var viewModel: TestableLoginViewModel
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        authRepository = mockk(relaxed = true)
        viewModel = TestableLoginViewModel(authRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state should have empty fields and no loading`() = runTest {
        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals("", state.email)
            assertEquals("", state.password)
            assertFalse(state.isLoading)
            assertNull(state.errorMessage)
        }
    }

    @Test
    fun `onEmailChange should update email in state`() = runTest {
        val testEmail = "test@example.com"

        viewModel.onEmailChange(testEmail)

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(testEmail, state.email)
        }
    }

    @Test
    fun `onPasswordChange should update password in state`() = runTest {
        val testPassword = "password123"

        viewModel.onPasswordChange(testPassword)

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(testPassword, state.password)
        }
    }

    @Test
    fun `onLoginClick with empty email should show error`() = runTest {
        viewModel.onPasswordChange("password123")

        viewModel.onLoginClick()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertTrue(state.errorMessage is UiText.StringResource)
        assertEquals(
            R.string.login_error_empty_email,
            (state.errorMessage as UiText.StringResource).resId
        )

        coVerify(exactly = 0) { authRepository.signIn(any(), any()) }
    }

    @Test
    fun `onLoginClick with invalid email should show error`() = runTest {
        viewModel.testEmailValidation = false
        viewModel.onEmailChange("invalid-email")
        viewModel.onPasswordChange("password123")

        viewModel.onLoginClick()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertTrue(state.errorMessage is UiText.StringResource)
        assertEquals(
            R.string.login_error_invalid_email,
            (state.errorMessage as UiText.StringResource).resId
        )

        coVerify(exactly = 0) { authRepository.signIn(any(), any()) }
    }

    @Test
    fun `onLoginClick with short password should show error`() = runTest {
        viewModel.testEmailValidation = true
        viewModel.onEmailChange("test@example.com")
        viewModel.onPasswordChange("12345")

        viewModel.onLoginClick()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertTrue(state.errorMessage is UiText.StringResource)
        assertEquals(
            R.string.login_error_short_password,
            (state.errorMessage as UiText.StringResource).resId
        )

        coVerify(exactly = 0) { authRepository.signIn(any(), any()) }
    }

    @Test
    fun `onLoginClick with valid credentials should call repository and navigate on success`() = runTest {
        val email = "test@example.com"
        val password = "password123"
        val mockUser = User(
            id = "123",
            email = email,
            displayName = "Test User",
            photoUrl = null
        )

        coEvery { authRepository.signIn(email, password) } returns Result.success(mockUser)

        viewModel.testEmailValidation = true
        viewModel.onEmailChange(email)
        viewModel.onPasswordChange(password)
        viewModel.onLoginClick()

        advanceUntilIdle()

        val event = viewModel.events.value
        assertEquals(LoginEvent.NavigateToHome, event)

        coVerify(exactly = 1) { authRepository.signIn(email, password) }
    }

    @Test
    fun `onLoginClick with repository failure should show error`() = runTest {
        val email = "test@example.com"
        val password = "password123"
        val errorMessage = "Invalid credentials"

        coEvery { authRepository.signIn(email, password) } returns
                Result.failure(Exception(errorMessage))

        viewModel.testEmailValidation = true
        viewModel.onEmailChange(email)
        viewModel.onPasswordChange(password)
        viewModel.onLoginClick()

        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertNotNull(state.errorMessage)

        coVerify(exactly = 1) { authRepository.signIn(email, password) }
    }

    @Test
    fun `onLoginClick with network error should show connection error`() = runTest {
        val email = "test@example.com"
        val password = "password123"

        coEvery { authRepository.signIn(email, password) } returns
                Result.failure(Exception("network error"))

        viewModel.testEmailValidation = true
        viewModel.onEmailChange(email)
        viewModel.onPasswordChange(password)
        viewModel.onLoginClick()

        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertTrue(state.errorMessage is UiText.StringResource)
        assertEquals(
            R.string.error_connection,
            (state.errorMessage as UiText.StringResource).resId
        )
    }

    @Test
    fun `onRegisterClick should emit NavigateToRegister event`() = runTest {
        viewModel.onRegisterClick()

        viewModel.events.test {
            val event = awaitItem()
            assertEquals(LoginEvent.NavigateToRegister, event)
        }
    }

    @Test
    fun `clearEvent should set events to null`() = runTest {
        viewModel.onRegisterClick()
        viewModel.clearEvent()

        viewModel.events.test {
            val event = awaitItem()
            assertNull(event)
        }
    }
}

class TestableLoginViewModel(
    authRepository: AuthRepository
) : LoginViewModel(authRepository) {
    var testEmailValidation = true

    override fun isValidEmail(email: String): Boolean {
        return if (email.contains("@")) testEmailValidation else false
    }
}