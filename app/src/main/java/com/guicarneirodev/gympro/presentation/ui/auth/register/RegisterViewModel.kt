package com.guicarneirodev.gympro.presentation.ui.auth.register

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.guicarneirodev.gympro.R
import com.guicarneirodev.gympro.domain.repository.AuthRepository
import com.guicarneirodev.gympro.presentation.util.UiText
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class RegisterUiState(
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val isLoading: Boolean = false,
    val errorMessage: UiText? = null
)

sealed class RegisterEvent {
    data object NavigateToHome : RegisterEvent()
    data object NavigateBack : RegisterEvent()
}

class RegisterViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(RegisterUiState())
    val uiState = _uiState.asStateFlow()

    private val _events = MutableStateFlow<RegisterEvent?>(null)
    val events = _events.asStateFlow()

    fun onEmailChange(email: String) {
        _uiState.update { it.copy(email = email, errorMessage = null) }
    }

    fun onPasswordChange(password: String) {
        _uiState.update { it.copy(password = password, errorMessage = null) }
    }

    fun onConfirmPasswordChange(confirmPassword: String) {
        _uiState.update { it.copy(confirmPassword = confirmPassword, errorMessage = null) }
    }

    fun onRegisterClick() {
        val state = _uiState.value
        if (!validateInputs(state)) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            authRepository.signUp(state.email, state.password)
                .onSuccess {
                    createTestUser()
                    _events.value = RegisterEvent.NavigateToHome
                }
                .onFailure { exception ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = getErrorMessage(exception)
                        )
                    }
                }
        }
    }

    fun onBackClick() {
        _events.value = RegisterEvent.NavigateBack
    }

    fun clearEvent() {
        _events.value = null
    }

    private fun validateInputs(state: RegisterUiState): Boolean {
        return when {
            state.email.isBlank() -> {
                _uiState.update {
                    it.copy(errorMessage = UiText.StringResource(R.string.login_error_empty_email))
                }
                false
            }
            !android.util.Patterns.EMAIL_ADDRESS.matcher(state.email).matches() -> {
                _uiState.update {
                    it.copy(errorMessage = UiText.StringResource(R.string.login_error_invalid_email))
                }
                false
            }
            state.password.length < 6 -> {
                _uiState.update {
                    it.copy(errorMessage = UiText.StringResource(R.string.login_error_short_password))
                }
                false
            }
            state.password != state.confirmPassword -> {
                _uiState.update {
                    it.copy(errorMessage = UiText.StringResource(R.string.register_error_passwords_not_match))
                }
                false
            }
            !isPasswordStrong(state.password) -> {
                _uiState.update {
                    it.copy(errorMessage = UiText.StringResource(R.string.register_error_weak_password))
                }
                false
            }
            else -> true
        }
    }

    private fun isPasswordStrong(password: String): Boolean {
        val hasLetter = password.any { it.isLetter() }
        val hasDigit = password.any { it.isDigit() }
        return hasLetter && hasDigit
    }

    private fun getErrorMessage(exception: Throwable): UiText {
        return when {
            exception.message?.contains("network") == true ->
                UiText.StringResource(R.string.error_connection)
            exception.message?.contains("email-already-in-use") == true ->
                UiText.StringResource(R.string.register_error_email_in_use)
            exception.message?.contains("weak-password") == true ->
                UiText.StringResource(R.string.login_error_short_password)
            else ->
                UiText.StringResource(R.string.error_generic)
        }
    }

    private fun createTestUser() {
        viewModelScope.launch {
            try {
                authRepository.signUp(
                    "teste@gympro.com",
                    "teste123"
                )
            } catch (_: Exception) {
                // Usuário teste já existe
            }
        }
    }
}