package com.neoqubix.devajit.h2.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neoqubix.devajit.h2.domain.usecase.LoginUseCase
import com.neoqubix.devajit.h2.domain.usecase.RegisterUseCase
import com.neoqubix.devajit.h2.domain.usecase.ResetPasswordUseCase
import com.neoqubix.devajit.h2.utils.toUserMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AuthUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val resetSending: Boolean = false,
    val resetMessage: String? = null,
    val resetError: String? = null
)

// Login, registration and password reset. After success the session switches screens on its own.
@HiltViewModel
class AuthViewModel @Inject constructor(
    private val loginUseCase: LoginUseCase,
    private val registerUseCase: RegisterUseCase,
    private val resetPasswordUseCase: ResetPasswordUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(AuthUiState())
    val state: StateFlow<AuthUiState> = _state.asStateFlow()

    fun login(email: String, password: String) = run { loginUseCase(email, password) }

    fun register(name: String, email: String, password: String, confirmPassword: String) =
        run { registerUseCase(name, email, password, confirmPassword) }

    private fun run(block: suspend () -> Result<Unit>) {
        if (_state.value.isLoading) return
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            val result = block()
            _state.update { it.copy(isLoading = false, error = result.exceptionOrNull()?.toUserMessage()) }
        }
    }

    fun sendPasswordReset(email: String) {
        viewModelScope.launch {
            _state.update { it.copy(resetSending = true, resetError = null, resetMessage = null) }
            resetPasswordUseCase(email)
                .onSuccess {
                    _state.update { it.copy(resetSending = false, resetMessage = "If an account exists for this email, a reset link has been sent.") }
                }
                .onFailure { e ->
                    // Unknown emails get the same message so accounts can't be discovered
                    val message = if (e is com.google.firebase.auth.FirebaseAuthInvalidUserException) null else e.toUserMessage()
                    _state.update {
                        if (message == null) it.copy(resetSending = false, resetMessage = "If an account exists for this email, a reset link has been sent.")
                        else it.copy(resetSending = false, resetError = message)
                    }
                }
        }
    }

    fun clearReset() = _state.update { it.copy(resetMessage = null, resetError = null, resetSending = false) }

    fun clearError() = _state.update { it.copy(error = null) }
}
