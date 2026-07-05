package com.talent.monthlybudgetkeeper.data.model

data class GoalProgress(
    val name: String,
    val currentAmount: Double,
    val targetAmount: Double,
    val completionRatio: Float
)
