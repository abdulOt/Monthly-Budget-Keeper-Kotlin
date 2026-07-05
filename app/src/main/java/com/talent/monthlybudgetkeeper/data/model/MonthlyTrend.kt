package com.talent.monthlybudgetkeeper.data.model

import java.time.YearMonth

data class MonthlyTrend(
    val month: YearMonth,
    val income: Double,
    val expense: Double
)
