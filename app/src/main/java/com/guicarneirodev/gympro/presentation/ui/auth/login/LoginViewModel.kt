package com.guicarneirodev.gympro.presentation.ui.auth.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.guicarneirodev.gympro.R
import com.guicarneirodev.gympro.domain.repository.AuthRepository
import com.guicarneirodev.gympro.presentation.util.UiText
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val errorMessage: UiText? = null
)

sealed class LoginEvent {
    data object NavigateToHome : LoginEvent()
    data object NavigateToRegister : LoginEvent()
}

open class LoginViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState = _uiState.asStateFlow()

    private val _events = MutableStateFlow<LoginEvent?>(null)
    val events = _events.asStateFlow()

    fun onEmailChange(email: String) {
        _uiState.update { it.copy(email = email, errorMessage = null) }
    }

    fun onPasswordChange(password: String) {
        _uiState.update { it.copy(password = password, errorMessage = null) }
    }

    fun onLoginClick() {
        val state = _uiState.value
        if (!validateInputs(state)) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            authRepository.signIn(state.email, state.password)
                .onSuccess {
                    _events.value = LoginEvent.NavigateToHome
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

    fun onRegisterClick() {
        _events.value = LoginEvent.NavigateToRegister
    }

    fun clearEvent() {
        _events.value = null
    }

    private fun validateInputs(state: LoginUiState): Boolean {
        return when {
            state.email.isBlank() -> {
                _uiState.update {
                    it.copy(errorMessage = UiText.StringResource(R.string.login_error_empty_email))
                }
                false
            }
            !isValidEmail(state.email) -> {
                _uiState.update {
                    it.copy(errorMessage = UiText.StringResource(R.string.login_error_invalid_email))
                }
                false
            }
            state.password.isBlank() -> {
                _uiState.update {
                    it.copy(errorMessage = UiText.StringResource(R.string.login_error_empty_password))
                }
                false
            }
            state.password.length < 6 -> {
                _uiState.update {
                    it.copy(errorMessage = UiText.StringResource(R.string.login_error_short_password))
                }
                false
            }
            else -> true
        }
    }

    protected open fun isValidEmail(email: String): Boolean {
        return try {
            android.util.Patterns.EMAIL_ADDRESS?.matcher(email)?.matches() ?: email.contains("@")
        } catch (_: Exception) {
            email.contains("@") && email.contains(".")
        }
    }

    private fun getErrorMessage(exception: Throwable): UiText {
        return when (exception) {
            is FirebaseAuthInvalidUserException -> {
                UiText.StringResource(R.string.login_error_user_not_found)
            }
            is FirebaseAuthInvalidCredentialsException -> {
                when {
                    exception.errorCode == "ERROR_WRONG_PASSWORD" ->
                        UiText.StringResource(R.string.login_error_wrong_password)
                    exception.errorCode == "ERROR_INVALID_EMAIL" ->
                        UiText.StringResource(R.string.login_error_invalid_email_format)
                    else ->
                        UiText.StringResource(R.string.login_error_invalid_credentials)
                }
            }
            else -> {
                when {
                    exception.message?.contains("network", ignoreCase = true) == true ||
                            exception.message?.contains("connection", ignoreCase = true) == true ->
                        UiText.StringResource(R.string.error_connection)

                    exception.message?.contains("password", ignoreCase = true) == true ->
                        UiText.StringResource(R.string.login_error_wrong_password)

                    exception.message?.contains("user", ignoreCase = true) == true ||
                            exception.message?.contains("account", ignoreCase = true) == true ->
                        UiText.StringResource(R.string.login_error_user_not_found)

                    exception.message?.contains("email", ignoreCase = true) == true ->
                        UiText.StringResource(R.string.login_error_invalid_email_format)

                    exception.message?.contains("credential", ignoreCase = true) == true ->
                        UiText.StringResource(R.string.login_error_invalid_credentials)

                    exception.message?.contains("too many", ignoreCase = true) == true ||
                            exception.message?.contains("blocked", ignoreCase = true) == true ->
                        UiText.StringResource(R.string.login_error_too_many_attempts)

                    else ->
                        UiText.StringResource(R.string.login_error_invalid_credentials)
                }
            }
        }
    }
}