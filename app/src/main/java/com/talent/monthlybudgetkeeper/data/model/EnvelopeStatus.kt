package com.talent.monthlybudgetkeeper.data.model

data class EnvelopeStatus(
    val name: String,
    val availableAmount: Double,
    val spentAmount: Double,
    val progress: Float
)
