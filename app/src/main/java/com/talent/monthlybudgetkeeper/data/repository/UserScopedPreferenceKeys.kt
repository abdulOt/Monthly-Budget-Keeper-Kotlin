package com.talent.monthlybudgetkeeper.data.repository

object UserScopedPreferenceKeys {
    fun stringName(userId: String, field: String): String = "${userId}_$field"

    fun booleanName(userId: String, field: String): String = "${userId}_$field"
}
