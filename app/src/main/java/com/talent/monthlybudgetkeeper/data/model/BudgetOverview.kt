package com.talent.monthlybudgetkeeper.data.model

data class BudgetOverview(
    val monthKey: String = "",
    val totalBudget: Double = 0.0,
    val totalSpent: Double = 0.0,
    val remainingBudget: Double = 0.0,
    val categories: List<CategoryBudgetStatus> = emptyList()
)

data class CategoryBudgetStatus(
    val category: TransactionCategory,
    val spent: Double,
    val limit: Double,
    val remaining: Double,
    val isNearLimit: Boolean,
    val isExceeded: Boolean
)
