package dev.thermaltrace.android.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import dev.thermaltrace.android.data.api.ClaimsPackRepository
import dev.thermaltrace.android.data.api.DashboardExtrasRepository
import dev.thermaltrace.android.data.api.HistoryRepository
import dev.thermaltrace.android.data.api.SettingsRepository
import dev.thermaltrace.android.data.model.ChartPointDto
import dev.thermaltrace.android.data.model.HistoryReadingDto
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

data class HistoryUiState(
    val loading: Boolean = true,
    val days: Int = 7,
    val points: List<ChartPointDto> = emptyList(),
    val housePoints: List<ChartPointDto> = emptyList(),
    val houseOverlaySource: String? = null,
    val readings: List<HistoryReadingDto> = emptyList(),
    val canUseClaimsPack: Boolean = false,
    val error: String? = null,
    val claimsBusy: Boolean = false,
    val claimsMessage: String? = null,
    val claimsFile: File? = null,
    val emailDialogOpen: Boolean = false,
    val adjusterEmail: String = "",
    val emailBusy: Boolean = false,
    val emailMessage: String? = null,
)

class HistoryViewModel(
    private val historyRepository: HistoryRepository,
    private val claimsPackRepository: ClaimsPackRepository,
    private val dashboardExtrasRepository: DashboardExtrasRepository,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    init {
        refresh(7)
    }

    fun refresh(days: Int = _uiState.value.days) {
        viewModelScope.launch {
            val keepContent = _uiState.value.points.isNotEmpty() || _uiState.value.readings.isNotEmpty()
            _uiState.update {
                it.copy(loading = !keepContent, days = days, error = null, claimsMessage = null)
            }
            coroutineScope {
                val entitlementsDeferred = async {
                    settingsRepository.loadExport().getOrNull()?.entitlements
                }
                val historyDeferred = async { historyRepository.load(days = days) }
                val entitlements = entitlementsDeferred.await()
                historyDeferred.await().fold(
                    onSuccess = { body ->
                        _uiState.update {
                            it.copy(
                                loading = false,
                                points = body.chart?.points.orEmpty(),
                                housePoints = body.houseOverlay?.points.orEmpty(),
                                houseOverlaySource = body.houseOverlay?.source,
                                readings = body.readings?.readings.orEmpty(),
                                canUseClaimsPack = entitlements?.canUseClaimsPack == true,
                                error = body.chart?.error ?: body.readings?.error,
                            )
                        }
                    },
                    onFailure = { err ->
                        _uiState.update {
                            it.copy(
                                loading = false,
                                canUseClaimsPack = entitlements?.canUseClaimsPack == true,
                                error = err.message ?: "Failed to load history",
                            )
                        }
                    },
                )
            }
        }
    }

    fun downloadClaimsPack() {
        if (!_uiState.value.canUseClaimsPack) {
            _uiState.update {
                it.copy(claimsMessage = "Claims pack requires ProbeHarbor Pro — upgrade on probeharbor.dev")
            }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(claimsBusy = true, claimsMessage = null, error = null) }
            claimsPackRepository.download(_uiState.value.days).fold(
                onSuccess = { file ->
                    _uiState.update {
                        it.copy(
                            claimsBusy = false,
                            claimsFile = file,
                            claimsMessage = "Claims pack ready",
                        )
                    }
                },
                onFailure = { err ->
                    _uiState.update {
                        it.copy(
                            claimsBusy = false,
                            claimsMessage = null,
                            error = err.message ?: "Claims pack failed",
                        )
                    }
                },
            )
        }
    }

    fun clearClaimsFile() {
        _uiState.update { it.copy(claimsFile = null) }
    }

    fun openEmailDialog() {
        if (!_uiState.value.canUseClaimsPack) {
            _uiState.update {
                it.copy(claimsMessage = "Claims pack requires ProbeHarbor Pro — upgrade on probeharbor.dev")
            }
            return
        }
        _uiState.update { it.copy(emailDialogOpen = true, emailMessage = null, error = null) }
    }

    fun dismissEmailDialog() {
        _uiState.update { it.copy(emailDialogOpen = false, adjusterEmail = "", emailMessage = null) }
    }

    fun onAdjusterEmailChange(value: String) {
        _uiState.update { it.copy(adjusterEmail = value, emailMessage = null) }
    }

    fun emailClaimsPack() {
        val email = _uiState.value.adjusterEmail.trim()
        if (email.isBlank() || !email.contains("@")) {
            _uiState.update { it.copy(emailMessage = "Enter a valid adjuster email") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(emailBusy = true, emailMessage = null, error = null) }
            dashboardExtrasRepository.emailClaimsPack(adjusterEmail = email).fold(
                onSuccess = { response ->
                    _uiState.update {
                        it.copy(
                            emailBusy = false,
                            emailDialogOpen = false,
                            adjusterEmail = "",
                            emailMessage = response.verificationCode?.let { code ->
                                "Email sent. Verification code: $code"
                            } ?: "Claims pack email sent",
                        )
                    }
                },
                onFailure = { err ->
                    _uiState.update {
                        it.copy(
                            emailBusy = false,
                            emailMessage = err.message ?: "Email failed",
                        )
                    }
                },
            )
        }
    }

    companion object {
        fun factory(
            historyRepository: HistoryRepository,
            claimsPackRepository: ClaimsPackRepository,
            dashboardExtrasRepository: DashboardExtrasRepository,
            settingsRepository: SettingsRepository,
        ): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    HistoryViewModel(
                        historyRepository,
                        claimsPackRepository,
                        dashboardExtrasRepository,
                        settingsRepository,
                    ) as T
            }
    }
}
