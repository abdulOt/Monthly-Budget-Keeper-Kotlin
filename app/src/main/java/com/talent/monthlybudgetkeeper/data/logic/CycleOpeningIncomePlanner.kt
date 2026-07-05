package com.talent.monthlybudgetkeeper.data.logic

import com.talent.monthlybudgetkeeper.data.local.entity.TransactionEntity
import com.talent.monthlybudgetkeeper.data.model.TransactionCategory
import com.talent.monthlybudgetkeeper.data.model.TransactionType
import java.time.LocalDate

object CycleOpeningIncomePlanner {
    fun marker(cycleStartDate: LocalDate): String {
        return "system_cycle_opening_income:$cycleStartDate"
    }

    fun prepareTransaction(
        existingTransactions: List<TransactionEntity>,
        cycleStartDate: LocalDate,
        amount: Double,
        title: String = "Cycle opening income"
    ): TransactionEntity? {
        if (amount <= 0.0) return null
        val marker = marker(cycleStartDate)
        val existing = existingTransactions.firstOrNull { transaction ->
            transaction.type == TransactionType.INCOME &&
                transaction.date == cycleStartDate &&
                transaction.note.contains(marker)
        }
        return if (existing == null) {
            TransactionEntity(
                title = title,
                amount = amount,
                type = TransactionType.INCOME,
                category = TransactionCategory.SALARY,
                date = cycleStartDate,
                note = marker
            )
        } else {
            existing.copy(
                title = title,
                amount = amount,
                type = TransactionType.INCOME,
                category = TransactionCategory.SALARY,
                date = cycleStartDate,
                note = marker
            )
        }
    }
}
