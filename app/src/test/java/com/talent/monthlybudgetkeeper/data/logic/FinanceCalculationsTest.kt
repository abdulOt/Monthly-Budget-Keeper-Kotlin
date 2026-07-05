package com.talent.monthlybudgetkeeper.data.logic

import com.talent.monthlybudgetkeeper.data.local.entity.AccountEntity
import com.talent.monthlybudgetkeeper.data.local.entity.CategoryBudgetEntity
import com.talent.monthlybudgetkeeper.data.local.entity.DebtEntity
import com.talent.monthlybudgetkeeper.data.local.entity.MonthlyBudgetEntity
import com.talent.monthlybudgetkeeper.data.local.entity.TransactionEntity
import com.talent.monthlybudgetkeeper.data.model.AccountType
import com.talent.monthlybudgetkeeper.data.model.BudgetCycleType
import com.talent.monthlybudgetkeeper.data.model.TransactionCategory
import com.talent.monthlybudgetkeeper.data.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class FinanceCalculationsTest {

    @Test
    fun `income increases balance`() {
        val updated = FinanceCalculations.applyIncome(balance = 1000.0, amount = 250.0)

        assertEquals(1250.0, updated, 0.0)
    }

    @Test
    fun `expense decreases balance`() {
        val updated = FinanceCalculations.applyExpense(balance = 1000.0, amount = 250.0)

        assertEquals(750.0, updated, 0.0)
    }

    @Test
    fun `transfer moves balances without changing net worth`() {
        val result = FinanceCalculations.applyTransfer(
            sourceBalance = 1200.0,
            destinationBalance = 300.0,
            amount = 200.0
        )

        assertEquals(1000.0, result.sourceBalance, 0.0)
        assertEquals(500.0, result.destinationBalance, 0.0)
        assertEquals(0.0, result.netWorthDelta, 0.0)
    }

    @Test
    fun `debt payment reduces cash and debt`() {
        val result = FinanceCalculations.applyDebtPayment(
            cashBalance = 900.0,
            debtBalance = 400.0,
            amount = 150.0
        )

        assertEquals(750.0, result.cashBalance, 0.0)
        assertEquals(250.0, result.remainingDebt, 0.0)
    }

    @Test
    fun `calculateMonthTotals aggregates income and expenses without transfers`() {
        val month = YearMonth.of(2026, 5)
        val transactions = listOf(
            TransactionEntity(
                title = "Opening income",
                amount = 1000.0,
                type = TransactionType.INCOME,
                category = TransactionCategory.SALARY,
                date = LocalDate.of(2026, 5, 1)
            ),
            TransactionEntity(
                title = "Expense",
                amount = 250.0,
                type = TransactionType.EXPENSE,
                category = TransactionCategory.FOOD,
                date = LocalDate.of(2026, 5, 2)
            ),
            TransactionEntity(
                title = "Transfer mirror",
                amount = 300.0,
                type = TransactionType.EXPENSE,
                category = TransactionCategory.OTHER_EXPENSE,
                date = LocalDate.of(2026, 5, 3),
                isTransfer = true
            )
        )

        val totals = FinanceCalculations.calculateMonthTotals(transactions, month)

        assertEquals(1000.0, totals.totalIncome, 0.0)
        assertEquals(250.0, totals.totalExpenses, 0.0)
    }

    @Test
    fun `budget alerts include total category and notification event keys`() {
        val month = YearMonth.of(2026, 5)
        val events = FinanceCalculations.buildBudgetAlertEvents(
            month = month,
            today = LocalDate.of(2026, 5, 18),
            monthlyBudget = MonthlyBudgetEntity(
                monthKey = month.toString(),
                totalBudget = 500.0
            ),
            plannedBudgetTarget = 0.0,
            categoryBudgets = listOf(
                CategoryBudgetEntity(
                    monthKey = month.toString(),
                    category = TransactionCategory.FOOD,
                    limitAmount = 200.0
                )
            ),
            transactions = listOf(
                TransactionEntity(
                    title = "Salary",
                    amount = 1000.0,
                    type = TransactionType.INCOME,
                    category = TransactionCategory.SALARY,
                    date = LocalDate.of(2026, 5, 1)
                ),
                TransactionEntity(
                    title = "Groceries",
                    amount = 220.0,
                    type = TransactionType.EXPENSE,
                    category = TransactionCategory.FOOD,
                    date = LocalDate.of(2026, 5, 2)
                ),
                TransactionEntity(
                    title = "Utilities",
                    amount = 300.0,
                    type = TransactionType.EXPENSE,
                    category = TransactionCategory.BILLS,
                    date = LocalDate.of(2026, 5, 3)
                )
            ),
            accounts = listOf(
                AccountEntity(
                    name = "Available Balance",
                    accountType = AccountType.CASH,
                    currentBalance = -20.0,
                    institution = "__system_balance__"
                )
            ),
            cycleType = BudgetCycleType.MONTHLY,
            nextCycleDate = LocalDate.of(2026, 5, 1),
            carryForwardRemainingBudget = true
        )

        val eventKeys = events.map { it.key }.toSet()

        assertTrue(eventKeys.contains("budget_total_exceeded_${month}"))
        assertTrue(eventKeys.any { it.startsWith("budget_category_exceeded_") })
        assertTrue(eventKeys.contains("negative_balance_${month}"))
        assertTrue(eventKeys.contains("cycle_ended_2026-05-01"))
    }

    @Test
    fun `net worth includes assets minus liabilities and debts`() {
        val totals = FinanceCalculations.calculateNetWorth(
            accounts = listOf(
                AccountEntity(
                    name = "Cash",
                    accountType = AccountType.CASH,
                    currentBalance = 1200.0
                ),
                AccountEntity(
                    name = "Card",
                    accountType = AccountType.CREDIT,
                    currentBalance = -300.0
                )
            ),
            debts = listOf(
                DebtEntity(
                    name = "Loan",
                    lender = "Bank",
                    totalAmount = 1000.0,
                    remainingAmount = 200.0
                )
            )
        )

        assertEquals(1200.0, totals.assetTotal, 0.0)
        assertEquals(500.0, totals.liabilityTotal, 0.0)
        assertEquals(700.0, totals.netWorth, 0.0)
    }
}
