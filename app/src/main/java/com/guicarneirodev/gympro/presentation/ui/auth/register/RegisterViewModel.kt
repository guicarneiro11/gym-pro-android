package com.guicarneirodev.gympro.presentation.ui.auth.register

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
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

open class RegisterViewModel(
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

    protected open fun isValidEmail(email: String): Boolean {
        return try {
            android.util.Patterns.EMAIL_ADDRESS?.matcher(email)?.matches() ?: email.contains("@")
        } catch (_: Exception) {
            email.contains("@") && email.contains(".")
        }
    }

    private fun validateInputs(state: RegisterUiState): Boolean {
        return when {
            state.email.isBlank() -> {
                _uiState.update {
                    it.copy(errorMessage = UiText.StringResource(R.string.register_error_empty_email))
                }
                false
            }
            !isValidEmail(state.email) -> {
                _uiState.update {
                    it.copy(errorMessage = UiText.StringResource(R.string.register_error_invalid_email))
                }
                false
            }
            state.password.isBlank() -> {
                _uiState.update {
                    it.copy(errorMessage = UiText.StringResource(R.string.register_error_empty_password))
                }
                false
            }
            state.password.length < 6 -> {
                _uiState.update {
                    it.copy(errorMessage = UiText.StringResource(R.string.register_error_short_password))
                }
                false
            }
            state.confirmPassword.isBlank() -> {
                _uiState.update {
                    it.copy(errorMessage = UiText.StringResource(R.string.register_error_empty_confirm_password))
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
        return hasLetter && hasDigit && password.length >= 6
    }

    private fun getErrorMessage(exception: Throwable): UiText {
        return when (exception) {
            is FirebaseAuthWeakPasswordException -> {
                UiText.StringResource(R.string.register_error_weak_password_firebase)
            }
            is FirebaseAuthInvalidCredentialsException -> {
                UiText.StringResource(R.string.register_error_invalid_email_format)
            }
            is FirebaseAuthUserCollisionException -> {
                UiText.StringResource(R.string.register_error_email_already_exists)
            }
            else -> {
                when {
                    exception.message?.contains("network", ignoreCase = true) == true ||
                            exception.message?.contains("connection", ignoreCase = true) == true ->
                        UiText.StringResource(R.string.error_connection)

                    exception.message?.contains("email-already-in-use", ignoreCase = true) == true ||
                            exception.message?.contains("already in use", ignoreCase = true) == true ||
                            exception.message?.contains("already exists", ignoreCase = true) == true ->
                        UiText.StringResource(R.string.register_error_email_already_exists)

                    exception.message?.contains("weak-password", ignoreCase = true) == true ->
                        UiText.StringResource(R.string.register_error_weak_password_firebase)

                    exception.message?.contains("invalid-email", ignoreCase = true) == true ->
                        UiText.StringResource(R.string.register_error_invalid_email_format)

                    exception.message?.contains("operation-not-allowed", ignoreCase = true) == true ->
                        UiText.StringResource(R.string.register_error_operation_not_allowed)

                    else ->
                        UiText.StringResource(R.string.register_error_generic)
                }
            }
        }
    }
}