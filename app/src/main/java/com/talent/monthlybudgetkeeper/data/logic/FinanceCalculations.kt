package com.talent.monthlybudgetkeeper.data.logic

import com.talent.monthlybudgetkeeper.data.local.entity.AccountEntity
import com.talent.monthlybudgetkeeper.data.local.entity.CategoryBudgetEntity
import com.talent.monthlybudgetkeeper.data.local.entity.DebtEntity
import com.talent.monthlybudgetkeeper.data.local.entity.MonthlyBudgetEntity
import com.talent.monthlybudgetkeeper.data.local.entity.TransactionEntity
import com.talent.monthlybudgetkeeper.data.model.AccountType
import com.talent.monthlybudgetkeeper.data.model.BudgetCycleType
import com.talent.monthlybudgetkeeper.data.model.TransactionType
import com.talent.monthlybudgetkeeper.data.repository.FinanceRepository
import com.talent.monthlybudgetkeeper.utils.DateUtils
import java.time.LocalDate
import java.time.YearMonth
import kotlin.math.abs

data class MonthTotals(
    val totalIncome: Double,
    val totalExpenses: Double
)

data class TransferBalanceResult(
    val sourceBalance: Double,
    val destinationBalance: Double,
    val netWorthDelta: Double
)

data class DebtPaymentBalanceResult(
    val cashBalance: Double,
    val remainingDebt: Double
)

data class NetWorthTotals(
    val assetTotal: Double,
    val liabilityTotal: Double,
    val netWorth: Double
)

data class BudgetAlertEventSpec(
    val key: String,
    val title: String,
    val message: String
)

object FinanceCalculations {
    fun applyIncome(balance: Double, amount: Double): Double = balance + amount

    fun applyExpense(balance: Double, amount: Double): Double = balance - amount

    fun applyTransfer(
        sourceBalance: Double,
        destinationBalance: Double,
        amount: Double
    ): TransferBalanceResult = TransferBalanceResult(
        sourceBalance = sourceBalance - amount,
        destinationBalance = destinationBalance + amount,
        netWorthDelta = 0.0
    )

    fun applyDebtPayment(
        cashBalance: Double,
        debtBalance: Double,
        amount: Double
    ): DebtPaymentBalanceResult = DebtPaymentBalanceResult(
        cashBalance = cashBalance - amount,
        remainingDebt = (debtBalance - amount).coerceAtLeast(0.0)
    )

    fun calculateMonthTotals(
        transactions: List<TransactionEntity>,
        month: YearMonth
    ): MonthTotals {
        val monthTransactions = transactions.filter {
            DateUtils.isInMonth(it.date, month) && !it.isTransfer
        }
        return MonthTotals(
            totalIncome = monthTransactions
                .filter { it.type == TransactionType.INCOME }
                .sumOf(TransactionEntity::amount),
            totalExpenses = monthTransactions
                .filter { it.type == TransactionType.EXPENSE }
                .sumOf(TransactionEntity::amount)
        )
    }

    fun currentAvailableBalance(accounts: List<AccountEntity>): Double {
        return accounts.firstOrNull {
            !it.isArchived && it.institution == FinanceRepository.SYSTEM_BALANCE_ACCOUNT_MARKER
        }?.currentBalance ?: 0.0
    }

    fun calculateNetWorth(
        accounts: List<AccountEntity>,
        debts: List<DebtEntity>
    ): NetWorthTotals {
        val activeAccounts = accounts.filter { !it.isArchived && it.includeInNetWorth }
        val assetTotal = activeAccounts
            .filter { it.accountType != AccountType.CREDIT && it.currentBalance >= 0.0 }
            .sumOf { it.currentBalance }
        val liabilityAccounts = activeAccounts
            .filter { it.accountType == AccountType.CREDIT || it.currentBalance < 0.0 }
            .sumOf { abs(it.currentBalance) }
        val debtTotal = debts.sumOf { it.remainingAmount.coerceAtLeast(0.0) }
        val liabilityTotal = liabilityAccounts + debtTotal
        return NetWorthTotals(
            assetTotal = assetTotal,
            liabilityTotal = liabilityTotal,
            netWorth = assetTotal - liabilityTotal
        )
    }

    fun buildBudgetAlertEvents(
        month: YearMonth,
        today: LocalDate,
        monthlyBudget: MonthlyBudgetEntity?,
        plannedBudgetTarget: Double,
        categoryBudgets: List<CategoryBudgetEntity>,
        transactions: List<TransactionEntity>,
        accounts: List<AccountEntity>,
        cycleType: BudgetCycleType,
        nextCycleDate: LocalDate,
        carryForwardRemainingBudget: Boolean
    ): List<BudgetAlertEventSpec> {
        val monthKey = DateUtils.toMonthKey(month)
        val totals = calculateMonthTotals(transactions, month)
        val monthExpenses = totals.totalExpenses
        val expenseTransactions = transactions.filter {
            DateUtils.isInMonth(it.date, month) &&
                !it.isTransfer &&
                it.type == TransactionType.EXPENSE
        }
        val categorySpend = expenseTransactions
            .groupBy { it.category }
            .mapValues { (_, items) -> items.sumOf { it.amount } }
        val totalBudgetLimit = monthlyBudget?.totalBudget
            ?.takeIf { it > 0.0 }
            ?: plannedBudgetTarget.takeIf { it > 0.0 }
            ?: 0.0

        val events = mutableListOf<BudgetAlertEventSpec>()
        if (totalBudgetLimit > 0.0 && monthExpenses >= totalBudgetLimit) {
            events += BudgetAlertEventSpec(
                key = "budget_total_exceeded_$monthKey",
                title = "Budget exceeded",
                message = "Total spending is above your budget for ${month.month.name.lowercase().replaceFirstChar(Char::uppercase)}."
            )
        }
        categoryBudgets
            .filter { it.monthKey == monthKey }
            .filter { categorySpend[it.category].orEmptyValue() >= it.limitAmount && it.limitAmount > 0.0 }
            .forEach { budget ->
                events += BudgetAlertEventSpec(
                    key = "budget_category_exceeded_${budget.remoteId ?: budget.category.name}_$monthKey",
                    title = "${budget.category.displayName} budget exceeded",
                    message = "Spending in this category is above the limit for the current month."
                )
            }

        if (currentAvailableBalance(accounts) < 0.0) {
            events += BudgetAlertEventSpec(
                key = "negative_balance_$monthKey",
                title = "Total balance below zero",
                message = "Your current balance has dropped below zero."
            )
        }

        if (!today.isBefore(nextCycleDate)) {
            val cycleLabel = cycleType.label.lowercase()
            val carryForwardMessage = if (carryForwardRemainingBudget) {
                " Any unused budget will carry forward."
            } else {
                ""
            }
            events += BudgetAlertEventSpec(
                key = "cycle_ended_${nextCycleDate}",
                title = "${cycleType.label} budget cycle ready",
                message = "Your $cycleLabel cycle has ended. Continue the previous income and budget or enter new values for the next cycle.$carryForwardMessage"
            )
        }
        return events
    }

    private fun Double?.orEmptyValue(): Double = this ?: 0.0
}
