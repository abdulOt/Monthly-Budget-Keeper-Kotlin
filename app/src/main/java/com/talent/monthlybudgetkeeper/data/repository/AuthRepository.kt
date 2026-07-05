package com.talent.monthlybudgetkeeper.data.repository

import com.talent.monthlybudgetkeeper.BuildConfig
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

@Singleton
class AuthRepository @Inject constructor(
    private val supabaseClient: SupabaseClient
) {
    suspend fun signIn(email: String, password: String) {
        supabaseClient.auth.signInWith(Email) {
            this.email = email.trim()
            this.password = password
        }
    }

    suspend fun signUp(
        fullName: String,
        email: String,
        password: String
    ): Boolean {
        supabaseClient.auth.signUpWith(
            provider = Email,
            redirectUrl = redirectUrl()
        ) {
            this.email = email.trim()
            this.password = password
            data = buildJsonObject {
                put("full_name", fullName.trim())
            }
        }
        return supabaseClient.auth.currentSessionOrNull() != null
    }

    suspend fun sendPasswordReset(email: String) {
        supabaseClient.auth.resetPasswordForEmail(
            email = email.trim(),
            redirectUrl = redirectUrl()
        )
    }

    suspend fun updatePassword(newPassword: String) {
        supabaseClient.auth.updateUser {
            password = newPassword
        }
    }

    suspend fun signOut() {
        supabaseClient.auth.signOut()
    }

    fun getCurrentUserId(): String? {
        return supabaseClient.auth.currentUserOrNull()?.id
    }

    suspend fun logout() {
        signOut()
    }

    fun isConfigured(): Boolean {
        return BuildConfig.SUPABASE_URL.isNotBlank() && BuildConfig.SUPABASE_ANON_KEY.isNotBlank()
    }

    private fun redirectUrl(): String {
        return "${BuildConfig.SUPABASE_REDIRECT_SCHEME}://${BuildConfig.SUPABASE_REDIRECT_HOST}"
    }
}
