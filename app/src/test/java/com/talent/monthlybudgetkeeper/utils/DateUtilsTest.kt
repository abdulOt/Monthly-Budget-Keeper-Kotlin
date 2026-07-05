package com.talent.monthlybudgetkeeper.utils

import com.talent.monthlybudgetkeeper.data.model.BudgetCycleType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import java.time.LocalDate

class DateUtilsTest {

    @Test
    fun `relativeDueLabel returns due tomorrow for next day`() {
        val today = LocalDate.of(2026, 5, 13)

        val label = DateUtils.relativeDueLabel(today.plusDays(1), today)

        assertEquals("Due tomorrow", label)
    }

    @Test
    fun `relativeDueLabel returns overdue label for past date`() {
        val today = LocalDate.of(2026, 5, 13)

        val label = DateUtils.relativeDueLabel(today.minusDays(2), today)

        assertEquals("Overdue by 2 days", label)
    }

    @Test
    fun `budgetCycleSnapshot advances ended cycle into future`() {
        val today = LocalDate.of(2026, 5, 18)

        val snapshot = DateUtils.budgetCycleSnapshot(
            cycleType = BudgetCycleType.MONTHLY,
            cycleStartDate = LocalDate.of(2026, 4, 1),
            nextCycleDate = LocalDate.of(2026, 5, 1),
            carryForwardRemainingBudget = true,
            today = today
        )

        assertEquals(LocalDate.of(2026, 5, 1), snapshot.cycleStartDate)
        assertEquals(LocalDate.of(2026, 6, 1), snapshot.nextCycleDate)
        assertFalse(today.isBefore(snapshot.cycleStartDate))
    }
}
