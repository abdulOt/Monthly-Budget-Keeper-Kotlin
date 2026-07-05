package com.talent.monthlybudgetkeeper.data.model

import java.time.LocalDate

data class BudgetCycleSnapshot(
    val cycleType: BudgetCycleType,
    val cycleStartDate: LocalDate,
    val nextCycleDate: LocalDate,
    val carryForwardRemainingBudget: Boolean
)
