package com.talent.monthlybudgetkeeper.data.model

data class AuthenticatedUser(
    val id: String,
    val email: String,
    val displayName: String,
    val provider: String? = null
)
