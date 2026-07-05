package com.talent.monthlybudgetkeeper.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.talent.monthlybudgetkeeper.data.auth.SessionManager
import com.talent.monthlybudgetkeeper.data.model.AuthSessionState
import com.talent.monthlybudgetkeeper.data.model.AppPreferences
import com.talent.monthlybudgetkeeper.data.model.SyncState
import com.talent.monthlybudgetkeeper.data.repository.AuthRepository
import com.talent.monthlybudgetkeeper.data.repository.CloudSyncRepository
import com.talent.monthlybudgetkeeper.data.repository.SettingsRepository
import com.talent.monthlybudgetkeeper.data.repository.PostLoginDestination
import com.talent.monthlybudgetkeeper.data.repository.UserProfileRepository
import com.talent.monthlybudgetkeeper.utils.AuthValidators
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class AuthActionState(
    val isLoading: Boolean = false
)

sealed interface ProfileBootstrapState {
    data object Idle : ProfileBootstrapState
    data class Loading(val userId: String) : ProfileBootstrapState
    data class Ready(
        val userId: String,
        val setupCompletedInCloud: Boolean
    ) : ProfileBootstrapState
    data class Error(
        val userId: String,
        val message: String
    ) : ProfileBootstrapState
}

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val sessionManager: SessionManager,
    private val cloudSyncRepository: CloudSyncRepository,
    private val settingsRepository: SettingsRepository,
    private val userProfileRepository: UserProfileRepository
) : ViewModel() {

    val sessionState: StateFlow<AuthSessionState> = sessionManager.sessionState
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = AuthSessionState.Loading
        )

    val syncState: StateFlow<SyncState> = cloudSyncRepository.syncState
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = SyncState.Idle
        )

    val preferences: StateFlow<AppPreferences> = settingsRepository.preferencesFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = AppPreferences()
        )

    val preferencesReady: StateFlow<Boolean> = settingsRepository.preferencesReadyFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = false
        )

    private val _actions = MutableSharedFlow<AuthActionState>(replay = 1)
    val actions = _actions.asSharedFlow()

    private val _messages = MutableSharedFlow<String>()
    val messages = _messages.asSharedFlow()

    private val _profileBootstrapState = MutableStateFlow<ProfileBootstrapState>(
        ProfileBootstrapState.Idle
    )
    val profileBootstrapState: StateFlow<ProfileBootstrapState> = _profileBootstrapState.asStateFlow()

    init {
        viewModelScope.launch {
            _actions.emit(AuthActionState())
        }
        viewModelScope.launch {
            sessionState.collectLatest { state ->
                when (state) {
                    AuthSessionState.Loading -> Unit
                    AuthSessionState.Unauthenticated -> {
                        _profileBootstrapState.value = ProfileBootstrapState.Idle
                    }
                    is AuthSessionState.Authenticated -> {
                        val currentState = _profileBootstrapState.value
                        val alreadyResolvedForUser = currentState is ProfileBootstrapState.Ready &&
                            currentState.userId == state.user.id
                        val alreadyLoadingForUser = currentState is ProfileBootstrapState.Loading &&
                            currentState.userId == state.user.id
                        if (!alreadyResolvedForUser && !alreadyLoadingForUser) {
                            bootstrapUserProfile(state.user.id)
                        }
                    }
                }
            }
        }
    }

    fun signIn(email: String, password: String) {
        val emailError = AuthValidators.validateEmail(email)
        val passwordError = AuthValidators.validatePassword(password)
        if (emailError != null) {
            emitMessage(emailError)
            return
        }
        if (passwordError != null) {
            emitMessage(passwordError)
            return
        }
        if (!authRepository.isConfigured()) {
            emitMessage("Add your Supabase keys to local.properties before signing in.")
            return
        }
        launchAuthAction {
            authRepository.signIn(email, password)
            emitMessage("Welcome back. Restoring your budget data now.")
        }
    }

    fun signUp(fullName: String, email: String, password: String, confirmPassword: String) {
        val nameError = AuthValidators.validateFullName(fullName)
        val emailError = AuthValidators.validateEmail(email)
        val passwordError = AuthValidators.validatePassword(password)
        when {
            nameError != null -> emitMessage(nameError)
            emailError != null -> emitMessage(emailError)
            passwordError != null -> emitMessage(passwordError)
            password != confirmPassword -> emitMessage("Passwords do not match.")
            !authRepository.isConfigured() -> emitMessage(
                "Add your Supabase keys to local.properties before creating an account."
            )
            else -> launchAuthAction {
                val signedInImmediately = authRepository.signUp(fullName, email, password)
                if (signedInImmediately) {
                    sessionManager.currentUserOrNull()?.let { authenticatedUser ->
                        settingsRepository.bindUser(authenticatedUser.id)
                        settingsRepository.updateProfileName(fullName, notifySync = false)
                        val settings = userProfileRepository.createInitialUserSettings(authenticatedUser.id)
                        userProfileRepository.syncUserSettingsToLocal(settings)
                        _profileBootstrapState.value = ProfileBootstrapState.Ready(
                            userId = authenticatedUser.id,
                            setupCompletedInCloud = settings.setupCompleted
                        )
                    }
                    emitMessage("Your account is ready.")
                } else {
                    emitMessage("Check your email to verify your account, then sign in.")
                }
            }
        }
    }

    fun sendPasswordReset(email: String) {
        val emailError = AuthValidators.validateEmail(email)
        if (emailError != null) {
            emitMessage(emailError)
            return
        }
        if (!authRepository.isConfigured()) {
            emitMessage("Add your Supabase keys to local.properties before using password reset.")
            return
        }
        launchAuthAction {
            authRepository.sendPasswordReset(email)
            emitMessage("Password reset email sent.")
        }
    }

    fun updatePassword(password: String, confirmPassword: String) {
        val passwordError = AuthValidators.validatePassword(password)
        when {
            passwordError != null -> emitMessage(passwordError)
            password != confirmPassword -> emitMessage("Passwords do not match.")
            else -> launchAuthAction {
                authRepository.updatePassword(password)
                sessionManager.clearRecoveryMode()
                emitMessage("Password updated. You can continue to your budget.")
            }
        }
    }

    fun signOut() {
        launchAuthAction {
            authRepository.logout()
            cloudSyncRepository.clearLocalUserData()
            sessionManager.clearRecoveryMode()
            _profileBootstrapState.value = ProfileBootstrapState.Idle
            emitMessage("Signed out successfully.")
        }
    }

    fun syncNow() {
        launchAuthAction {
            cloudSyncRepository.syncNow().getOrThrow()
            emitMessage("Cloud sync complete.")
        }
    }

    fun reportMessage(message: String) {
        emitMessage(message)
    }

    fun retryProfileBootstrap() {
        val authenticatedState = sessionState.value as? AuthSessionState.Authenticated ?: return
        viewModelScope.launch {
            bootstrapUserProfile(authenticatedState.user.id, force = true)
        }
    }

    private suspend fun bootstrapUserProfile(
        userId: String,
        force: Boolean = false
    ) {
        val currentState = _profileBootstrapState.value
        if (!force) {
            when (currentState) {
                is ProfileBootstrapState.Loading -> if (currentState.userId == userId) return
                is ProfileBootstrapState.Ready -> if (currentState.userId == userId) return
                else -> Unit
            }
        }

        _profileBootstrapState.value = ProfileBootstrapState.Loading(userId)
        runCatching {
            when (userProfileRepository.resolvePostLoginDestination(userId)) {
                PostLoginDestination.HOME -> ProfileBootstrapState.Ready(
                    userId = userId,
                    setupCompletedInCloud = true
                )
                PostLoginDestination.SETUP -> ProfileBootstrapState.Ready(
                    userId = userId,
                    setupCompletedInCloud = false
                )
            }
        }.onSuccess { resolvedState ->
            _profileBootstrapState.value = resolvedState
        }.onFailure { throwable ->
            _profileBootstrapState.value = ProfileBootstrapState.Error(
                userId = userId,
                message = throwable.message ?: "We couldn't restore your cloud profile."
            )
            emitMessage(throwable.message ?: "We couldn't restore your cloud profile.")
        }
    }

    private fun launchAuthAction(block: suspend () -> Unit) {
        viewModelScope.launch {
            _actions.emit(AuthActionState(isLoading = true))
            runCatching { block() }
                .onFailure {
                    emitMessage(it.message ?: "Something went wrong. Please try again.")
                }
            _actions.emit(AuthActionState(isLoading = false))
        }
    }

    private fun emitMessage(message: String) {
        viewModelScope.launch {
            _messages.emit(message)
        }
    }
}
