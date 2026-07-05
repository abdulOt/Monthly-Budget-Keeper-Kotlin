package com.talent.monthlybudgetkeeper.data.logic

import com.talent.monthlybudgetkeeper.data.local.entity.TransactionEntity
import com.talent.monthlybudgetkeeper.data.model.TransactionCategory
import com.talent.monthlybudgetkeeper.data.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import java.time.LocalDate

class CycleOpeningIncomePlannerTest {

    @Test
    fun `prepareTransaction creates setup income once for empty history`() {
        val cycleStart = LocalDate.of(2026, 5, 1)

        val result = CycleOpeningIncomePlanner.prepareTransaction(
            existingTransactions = emptyList(),
            cycleStartDate = cycleStart,
            amount = 1500.0
        )

        assertNotNull(result)
        assertEquals(CycleOpeningIncomePlanner.marker(cycleStart), result!!.note)
        assertEquals(1500.0, result.amount, 0.0)
        assertEquals(TransactionType.INCOME, result.type)
    }

    @Test
    fun `prepareTransaction updates existing cycle opening income instead of duplicating`() {
        val cycleStart = LocalDate.of(2026, 5, 1)
        val existing = TransactionEntity(
            id = 22L,
            title = "Cycle opening income",
            amount = 1000.0,
            type = TransactionType.INCOME,
            category = TransactionCategory.SALARY,
            date = cycleStart,
            note = CycleOpeningIncomePlanner.marker(cycleStart)
        )

        val result = CycleOpeningIncomePlanner.prepareTransaction(
            existingTransactions = listOf(existing),
            cycleStartDate = cycleStart,
            amount = 1800.0
        )

        assertNotNull(result)
        assertEquals(existing.id, result!!.id)
        assertEquals(1800.0, result.amount, 0.0)
        assertEquals(CycleOpeningIncomePlanner.marker(cycleStart), result.note)
    }
}
