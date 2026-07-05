package com.talent.monthlybudgetkeeper.utils

object TransactionValidators {
    fun validateTitle(title: String): String? {
        return when {
            title.isBlank() -> "Title is required."
            title.trim().length < 2 -> "Title should be at least 2 characters."
            else -> null
        }
    }

    fun validateAmount(amount: String): String? {
        val numericAmount = amount.toDoubleOrNull()
        return when {
            amount.isBlank() -> "Amount is required."
            numericAmount == null -> "Enter a valid amount."
            numericAmount <= 0.0 -> "Amount must be greater than zero."
            else -> null
        }
    }
}
