package dev.thermaltrace.android.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import dev.thermaltrace.android.data.auth.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LoginUiState(
    val loading: Boolean = false,
    val error: String? = null,
)

/** Every way in opens the website; the session comes back through the app link. */
class LoginViewModel(
    private val authRepository: AuthRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun emailSignInUrl(): String = authRepository.emailSignInUrl()

    fun registerUrl(): String = authRepository.registerUrl()

    fun forgotPasswordUrl(): String = authRepository.forgotPasswordUrl()

    fun oauthUrl(provider: String): String = authRepository.oauthStartUrl(provider)

    fun exchangeOAuth(token: String, onSuccess: () -> Unit, onNeedsMfa: () -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(loading = true, error = null) }
            val result = authRepository.exchangeMobileOAuth(token)
            _uiState.update { it.copy(loading = false) }
            result.fold(
                onSuccess = { outcome ->
                    if (outcome.needsMfa) onNeedsMfa() else onSuccess()
                },
                onFailure = { err ->
                    _uiState.update { it.copy(error = err.message ?: "Sign-in failed") }
                },
            )
        }
    }

    companion object {
        fun factory(authRepository: AuthRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    LoginViewModel(authRepository) as T
            }
    }
}
