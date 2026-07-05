package com.talent.monthlybudgetkeeper.data.auth

import android.content.Intent
import com.talent.monthlybudgetkeeper.data.model.AuthSessionState
import com.talent.monthlybudgetkeeper.data.model.AuthenticatedUser
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.auth.handleDeeplinks
import io.github.jan.supabase.auth.user.UserInfo
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@Singleton
class SessionManager @Inject constructor(
    private val supabaseClient: SupabaseClient
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val recoveryMode = MutableStateFlow(false)
    private val _sessionState = MutableStateFlow<AuthSessionState>(AuthSessionState.Loading)
    val sessionState: StateFlow<AuthSessionState> = _sessionState.asStateFlow()

    init {
        scope.launch {
            supabaseClient.auth.sessionStatus.collectLatest { status ->
                _sessionState.value = when (status) {
                    SessionStatus.Initializing -> AuthSessionState.Loading
                    is SessionStatus.RefreshFailure -> AuthSessionState.Unauthenticated
                    is SessionStatus.NotAuthenticated -> AuthSessionState.Unauthenticated
                    is SessionStatus.Authenticated -> {
                        val sessionUser = status.session.user
                        if (sessionUser != null) {
                            sessionUser.toSessionState(recoveryMode.value)
                        } else {
                            AuthSessionState.Unauthenticated
                        }
                    }
                }
            }
        }
    }

    fun handleIncomingIntent(intent: Intent?) {
        val rawIntent = intent ?: return
        val rawData = rawIntent.dataString.orEmpty()
        if (rawData.contains("type=recovery", ignoreCase = true)) {
            recoveryMode.value = true
        }
        supabaseClient.handleDeeplinks(rawIntent)
        scope.launch {
            _sessionState.value = currentUserState()
        }
    }

    fun clearRecoveryMode() {
        recoveryMode.value = false
        scope.launch {
            _sessionState.value = currentUserState()
        }
    }

    fun currentUserOrNull(): AuthenticatedUser? {
        val session = supabaseClient.auth.currentSessionOrNull() ?: return null
        val sessionUser = session.user ?: return null
        val email = sessionUser.email?.ifBlank { null } ?: return null
        val fullName = sessionUser.userMetadata?.get("full_name")?.toString()
            ?.replace("\"", "")
            ?.takeIf { it.isNotBlank() }
            ?: email.substringBefore('@')
        return AuthenticatedUser(
            id = sessionUser.id,
            email = email,
            displayName = fullName,
            provider = sessionUser.appMetadata?.get("provider")?.toString()?.replace("\"", "")
        )
    }

    private fun currentUserState(): AuthSessionState {
        val user = currentUserOrNull() ?: return AuthSessionState.Unauthenticated
        return AuthSessionState.Authenticated(
            user = user,
            requiresPasswordReset = recoveryMode.value
        )
    }

    private fun UserInfo.toSessionState(
        needsPasswordReset: Boolean
    ): AuthSessionState.Authenticated {
        val emailValue = email.orEmpty()
        val displayNameValue = userMetadata?.get("full_name")?.toString()
            ?.replace("\"", "")
            ?.takeIf { it.isNotBlank() }
            ?: emailValue.substringBefore('@')
        val providerValue = appMetadata?.get("provider")?.toString()?.replace("\"", "")
        return AuthSessionState.Authenticated(
            user = AuthenticatedUser(
                id = id,
                email = emailValue,
                displayName = displayNameValue,
                provider = providerValue
            ),
            requiresPasswordReset = needsPasswordReset
        )
    }
}
