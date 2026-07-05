package com.talent.monthlybudgetkeeper.data.model

import java.time.YearMonth

data class TransactionFilter(
    val month: YearMonth = YearMonth.now(),
    val type: TransactionType? = null,
    val category: TransactionCategory? = null,
    val searchQuery: String = "",
    val sortOption: TransactionSortOption = TransactionSortOption.NEWEST
)
