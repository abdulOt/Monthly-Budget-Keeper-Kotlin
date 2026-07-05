package com.talent.monthlybudgetkeeper.utils

import com.talent.monthlybudgetkeeper.data.model.BudgetCycleSnapshot
import com.talent.monthlybudgetkeeper.data.model.BudgetCycleType
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit
import java.time.format.DateTimeFormatter
import java.util.Locale

object DateUtils {
    private val readableDateFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.getDefault())
    private val monthLabelFormatter = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault())
    private val shortMonthFormatter = DateTimeFormatter.ofPattern("MMM", Locale.getDefault())

    fun toMonthKey(month: YearMonth): String = month.toString()

    fun fromMonthKey(monthKey: String): YearMonth = YearMonth.parse(monthKey)

    fun formatDate(date: LocalDate): String = readableDateFormatter.format(date)

    fun formatMonth(month: YearMonth): String = monthLabelFormatter.format(month)

    fun formatMonthShort(month: YearMonth): String = shortMonthFormatter.format(month)

    fun isInMonth(date: LocalDate, month: YearMonth): Boolean {
        return YearMonth.from(date) == month
    }

    fun recentMonths(count: Int, from: YearMonth = YearMonth.now()): List<YearMonth> {
        return (count - 1 downTo 0).map { from.minusMonths(it.toLong()) }
    }

    fun relativeDueLabel(date: LocalDate, today: LocalDate = LocalDate.now()): String {
        return when (val days = daysUntil(today, date)) {
            0L -> "Due today"
            1L -> "Due tomorrow"
            in 2L..Long.MAX_VALUE -> "Due in $days days"
            -1L -> "Overdue by 1 day"
            else -> "Overdue by ${kotlin.math.abs(days)} days"
        }
    }

    fun daysUntil(from: LocalDate, to: LocalDate): Long {
        return ChronoUnit.DAYS.between(from, to)
    }

    fun budgetCycleSnapshot(
        cycleType: BudgetCycleType,
        cycleStartDate: LocalDate,
        nextCycleDate: LocalDate,
        carryForwardRemainingBudget: Boolean,
        today: LocalDate = LocalDate.now()
    ): BudgetCycleSnapshot {
        var start = cycleStartDate
        var next = ensureFutureBoundary(cycleType, nextCycleDate, start)

        while (!today.isBefore(next)) {
            start = next
            next = advanceCycleDate(start, cycleType)
        }

        return BudgetCycleSnapshot(
            cycleType = cycleType,
            cycleStartDate = start,
            nextCycleDate = next,
            carryForwardRemainingBudget = carryForwardRemainingBudget
        )
    }

    fun advanceCycleDate(startDate: LocalDate, cycleType: BudgetCycleType): LocalDate {
        return when (cycleType) {
            BudgetCycleType.WEEKLY -> startDate.plusWeeks(1)
            BudgetCycleType.MONTHLY -> startDate.plusMonths(1)
            BudgetCycleType.YEARLY -> startDate.plusYears(1)
        }
    }

    fun previousCycleStart(nextCycleDate: LocalDate, cycleType: BudgetCycleType): LocalDate {
        return when (cycleType) {
            BudgetCycleType.WEEKLY -> nextCycleDate.minusWeeks(1)
            BudgetCycleType.MONTHLY -> nextCycleDate.minusMonths(1)
            BudgetCycleType.YEARLY -> nextCycleDate.minusYears(1)
        }
    }

    fun ensureFutureBoundary(
        cycleType: BudgetCycleType,
        nextCycleDate: LocalDate,
        fallbackStartDate: LocalDate
    ): LocalDate {
        return if (nextCycleDate.isAfter(fallbackStartDate)) {
            nextCycleDate
        } else {
            advanceCycleDate(fallbackStartDate, cycleType)
        }
    }
}
