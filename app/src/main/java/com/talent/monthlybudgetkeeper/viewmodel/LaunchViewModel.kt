package com.talent.monthlybudgetkeeper.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.talent.monthlybudgetkeeper.data.model.SyncState
import com.talent.monthlybudgetkeeper.data.repository.CloudSyncRepository
import com.talent.monthlybudgetkeeper.data.auth.SessionManager
import com.talent.monthlybudgetkeeper.data.model.AuthSessionState
import com.talent.monthlybudgetkeeper.data.repository.SettingsRepository
import com.talent.monthlybudgetkeeper.ui.navigation.AppDestination
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LaunchUiState(
    val isLoading: Boolean = true,
    val destinationRoute: String? = null
)

@HiltViewModel
class LaunchViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val sessionManager: SessionManager,
    private val cloudSyncRepository: CloudSyncRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LaunchUiState())
    val uiState: StateFlow<LaunchUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            delay(350)
            val state = combine(
                settingsRepository.preferencesFlow,
                sessionManager.sessionState,
                cloudSyncRepository.syncState
            ) { preferences, sessionState, syncState ->
                when {
                    !preferences.onboardingCompleted -> AppDestination.Onboarding.route
                    sessionState is AuthSessionState.Authenticated &&
                        sessionState.requiresPasswordReset -> AppDestination.ForgotPassword.route
                    sessionState is AuthSessionState.Authenticated &&
                        !preferences.setupCompleted &&
                        (syncState is SyncState.Idle || syncState is SyncState.Syncing) -> null
                    sessionState is AuthSessionState.Authenticated &&
                        !preferences.setupCompleted -> AppDestination.Setup.route
                    sessionState is AuthSessionState.Authenticated -> AppDestination.Dashboard.route
                    sessionState is AuthSessionState.Unauthenticated -> AppDestination.AuthWelcome.route
                    else -> null
                }
            }.first { it != null }
            _uiState.value = LaunchUiState(isLoading = false, destinationRoute = state)
        }
    }
}
