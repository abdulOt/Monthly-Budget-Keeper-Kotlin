package com.talent.monthlybudgetkeeper.utils

object AuthValidators {
    fun validateEmail(email: String): String? {
        val normalized = email.trim()
        if (normalized.isEmpty()) return "Enter your email address."
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(normalized).matches()) {
            return "Enter a valid email address."
        }
        return null
    }

    fun validatePassword(password: String): String? {
        if (password.length < 8) return "Password must be at least 8 characters."
        return null
    }

    fun validateFullName(name: String): String? {
        if (name.trim().length < 2) return "Enter your full name."
        return null
    }
}
