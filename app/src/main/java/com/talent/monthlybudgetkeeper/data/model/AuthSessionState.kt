package com.talent.monthlybudgetkeeper.data.model

sealed interface AuthSessionState {
    data object Loading : AuthSessionState
    data object Unauthenticated : AuthSessionState
    data class Authenticated(
        val user: AuthenticatedUser,
        val requiresPasswordReset: Boolean = false
    ) : AuthSessionState
}
