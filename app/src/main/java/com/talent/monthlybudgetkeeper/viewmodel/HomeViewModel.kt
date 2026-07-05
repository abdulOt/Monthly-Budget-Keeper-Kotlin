package com.talent.monthlybudgetkeeper.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import com.talent.monthlybudgetkeeper.data.logic.FinanceCalculations
import com.talent.monthlybudgetkeeper.data.local.entity.SubscriptionBillEntity
import com.talent.monthlybudgetkeeper.data.local.entity.TransactionEntity
import com.talent.monthlybudgetkeeper.data.local.entity.TransferEntity
import com.talent.monthlybudgetkeeper.data.model.AccountSnapshot
import com.talent.monthlybudgetkeeper.data.model.AccountType
import com.talent.monthlybudgetkeeper.data.model.BudgetCycleType
import com.talent.monthlybudgetkeeper.data.model.CategorySpend
import com.talent.monthlybudgetkeeper.data.model.CurrencyOption
import com.talent.monthlybudgetkeeper.data.model.EnvelopeStatus
import com.talent.monthlybudgetkeeper.data.model.FinancialInsight
import com.talent.monthlybudgetkeeper.data.model.GoalProgress
import com.talent.monthlybudgetkeeper.data.model.InsightSeverity
import com.talent.monthlybudgetkeeper.data.model.MonthlyTrend
import com.talent.monthlybudgetkeeper.data.model.AppPreferences
import com.talent.monthlybudgetkeeper.data.model.TransactionType
import com.talent.monthlybudgetkeeper.data.notifications.ReminderScheduler
import com.talent.monthlybudgetkeeper.data.repository.BudgetRepository
import com.talent.monthlybudgetkeeper.data.repository.FinanceRepository
import com.talent.monthlybudgetkeeper.data.repository.SettingsRepository
import com.talent.monthlybudgetkeeper.data.repository.TransactionRepository
import com.talent.monthlybudgetkeeper.utils.DateUtils
import java.time.LocalDate
import java.time.YearMonth
import kotlin.math.abs
import kotlin.math.roundToInt
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HomeUiState(
    val month: YearMonth = YearMonth.now(),
    val currency: CurrencyOption = CurrencyOption.PKR,
    val privacyModeEnabled: Boolean = false,
    val totalIncome: Double = 0.0,
    val totalExpenses: Double = 0.0,
    val remainingBalance: Double = 0.0,
    val monthlyBudget: Double = 0.0,
    val budgetUsedPercentage: Float = 0f,
    val topCategories: List<CategorySpend> = emptyList(),
    val recentTransactions: List<TransactionEntity> = emptyList(),
    val trends: List<MonthlyTrend> = emptyList(),
    val netWorth: Double = 0.0,
    val assetTotal: Double = 0.0,
    val debtTotal: Double = 0.0,
    val upcomingBillsCount: Int = 0,
    val upcomingBillsAmount: Double = 0.0,
    val accountSnapshots: List<AccountSnapshot> = emptyList(),
    val goalProgress: List<GoalProgress> = emptyList(),
    val envelopeStatuses: List<EnvelopeStatus> = emptyList(),
    val insights: List<FinancialInsight> = emptyList(),
    val cycleType: BudgetCycleType = BudgetCycleType.MONTHLY,
    val cycleStartDate: LocalDate = LocalDate.now().withDayOfMonth(1),
    val nextCycleDate: LocalDate = LocalDate.now().withDayOfMonth(1).plusMonths(1),
    val cyclePlannedIncome: Double = 0.0,
    val cyclePlannedBudget: Double = 0.0,
    val carryForwardRemainingBudget: Boolean = false,
    val cycleRenewalDue: Boolean = false,
    val cycleRenewalMessage: String = "",
    val upcomingBillsStatusText: String = "No bills due soon"
)

private data class HomeBaseBundle(
    val transactions: List<TransactionEntity>,
    val transfers: List<TransferEntity>,
    val monthlyBudget: Double,
    val plannedMonthlyBudget: Double,
    val plannedMonthlyIncome: Double,
    val currency: CurrencyOption,
    val privacyModeEnabled: Boolean,
    val profileName: String,
    val accounts: List<AccountSnapshot>,
    val availableBalance: Double,
    val assetTotal: Double,
    val liabilityAccountTotal: Double,
    val upcomingBills: List<SubscriptionBillEntity>,
    val activeBills: List<SubscriptionBillEntity>,
    val reminderLeadDays: Int,
    val cycleType: BudgetCycleType,
    val cycleStartDate: LocalDate,
    val nextCycleDate: LocalDate,
    val carryForwardRemainingBudget: Boolean
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    budgetRepository: BudgetRepository,
    private val settingsRepository: SettingsRepository,
    private val financeRepository: FinanceRepository,
    private val reminderScheduler: ReminderScheduler
) : ViewModel() {
    private val month = YearMonth.now()
    private val financeOverviewFlow = combine(
        financeRepository.observeAccounts(),
        financeRepository.observeTransfers(),
        financeRepository.observeSubscriptionBills()
    ) { accounts, transfers, bills ->
        Triple(accounts, transfers, bills)
    }

    private val baseFlow = combine(
        transactionRepository.observeTransactions(),
        budgetRepository.observeMonthlyBudget(month),
        settingsRepository.preferencesFlow,
        financeOverviewFlow
    ) { transactions, monthlyBudget, preferences, financeOverview ->
        val (accounts, transfers, bills) = financeOverview
        val today = LocalDate.now()
        val primaryBalanceAccount = accounts.firstOrNull {
            !it.isArchived && it.institution == FinanceRepository.SYSTEM_BALANCE_ACCOUNT_MARKER
        }
        val accountSnapshots = accounts
            .filter { !it.isArchived }
            .sortedByDescending { abs(it.currentBalance) }
            .map { AccountSnapshot(it.name, it.currentBalance, it.currencyCode) }
            .take(4)
        val netWorthTotals = FinanceCalculations.calculateNetWorth(accounts, emptyList())
        val activeBills = bills
            .filter { it.isActive }
            .sortedBy { it.nextChargeDate }
        val upcomingBills = activeBills
            .filter { !it.nextChargeDate.isBefore(today) && !it.nextChargeDate.isAfter(today.plusDays(14)) }
            .sortedBy { it.nextChargeDate }
        HomeBaseBundle(
            transactions = transactions,
            transfers = transfers,
            monthlyBudget = monthlyBudget?.totalBudget ?: 0.0,
            plannedMonthlyBudget = preferences.monthlyBudgetTarget,
            plannedMonthlyIncome = preferences.monthlyIncome,
            currency = preferences.currency,
            privacyModeEnabled = preferences.privacyModeEnabled,
            profileName = preferences.profileName,
            accounts = accountSnapshots,
            availableBalance = primaryBalanceAccount?.currentBalance ?: 0.0,
            assetTotal = netWorthTotals.assetTotal,
            liabilityAccountTotal = netWorthTotals.liabilityTotal,
            upcomingBills = upcomingBills,
            activeBills = activeBills,
            reminderLeadDays = preferences.reminderLeadDays,
            cycleType = preferences.cycleType,
            cycleStartDate = preferences.cycleStartDate,
            nextCycleDate = preferences.nextCycleDate,
            carryForwardRemainingBudget = preferences.carryForwardRemainingBudget
        )
    }

    val uiState: StateFlow<HomeUiState> = combine(
        baseFlow,
        financeRepository.observeGoals(),
        financeRepository.observeDebts(),
        financeRepository.observeEnvelopeBudgets()
    ) { base, goals, debts, envelopes ->
        val currentMonthTransactions = base.transactions.filter {
            DateUtils.isInMonth(it.date, month) && !it.isTransfer
        }
        val monthTotals = FinanceCalculations.calculateMonthTotals(base.transactions, month)
        val expenses = monthTotals.totalExpenses
        val actualIncome = monthTotals.totalIncome
        val income = actualIncome
        val effectiveMonthlyBudget = base.monthlyBudget.takeIf { it > 0.0 } ?: base.plannedMonthlyBudget
        val topCategories = currentMonthTransactions
            .filter { it.type == TransactionType.EXPENSE }
            .groupBy { it.category }
            .map { (category, items) -> CategorySpend(category, items.sumOf(TransactionEntity::amount)) }
            .sortedByDescending { it.amount }
            .take(4)
        val trends = DateUtils.recentMonths(6).map { trendMonth ->
            val monthTransactions = base.transactions.filter {
                DateUtils.isInMonth(it.date, trendMonth) && !it.isTransfer
            }
            MonthlyTrend(
                month = trendMonth,
                income = monthTransactions
                    .filter { it.type == TransactionType.INCOME }
                    .sumOf(TransactionEntity::amount),
                expense = monthTransactions
                    .filter { it.type == TransactionType.EXPENSE }
                    .sumOf(TransactionEntity::amount)
            )
        }
        val debtTotal = debts.sumOf { it.remainingAmount }
        val totalLiabilities = base.liabilityAccountTotal + debtTotal
        val netWorth = base.assetTotal - totalLiabilities
        val goalProgress = goals
            .sortedByDescending { if (it.targetAmount > 0) it.currentAmount / it.targetAmount else 0.0 }
            .take(3)
            .map {
                GoalProgress(
                    name = it.name,
                    currentAmount = it.currentAmount,
                    targetAmount = it.targetAmount,
                    completionRatio = if (it.targetAmount > 0) {
                        (it.currentAmount / it.targetAmount).toFloat().coerceIn(0f, 1f)
                    } else {
                        0f
                    }
                )
            }
        val envelopeStatuses = envelopes
            .filter { it.monthKey == DateUtils.toMonthKey(month) }
            .sortedByDescending { (it.spentAmount / (it.plannedAmount + it.rolloverAmount).coerceAtLeast(1.0)) }
            .take(3)
            .map {
                val available = (it.plannedAmount + it.rolloverAmount - it.spentAmount).coerceAtLeast(0.0)
                EnvelopeStatus(
                    name = it.name,
                    availableAmount = available,
                    spentAmount = it.spentAmount,
                    progress = if (it.plannedAmount + it.rolloverAmount > 0) {
                        (it.spentAmount / (it.plannedAmount + it.rolloverAmount)).toFloat().coerceAtLeast(0f)
                    } else {
                        0f
                    }
                )
        }
        val budgetUsed = if (effectiveMonthlyBudget > 0) {
            (expenses / effectiveMonthlyBudget).toFloat().coerceAtMost(1.5f)
        } else {
            0f
        }
        val today = LocalDate.now()
        val cycleRenewalDue = !today.isBefore(base.nextCycleDate)
        val nextUpcomingBill = base.upcomingBills.firstOrNull()
        val remainingBalance = base.availableBalance
        val dueDebtsCount = debts.count { debt ->
            val dueDate = debt.dueDate
            dueDate != null &&
                debt.remainingAmount > 0.0 &&
                (dueDate.isBefore(today) || !dueDate.isAfter(today.plusDays(base.reminderLeadDays.toLong())))
        }
        val insights = buildInsights(
            budgetUsedPercentage = budgetUsed,
            debtTotal = debtTotal,
            assetTotal = base.assetTotal,
            remainingBalance = remainingBalance,
            activeBills = base.activeBills,
            dueDebtsCount = dueDebtsCount,
            goals = goalProgress
        )

        HomeUiState(
            month = month,
            currency = base.currency,
            privacyModeEnabled = base.privacyModeEnabled,
            totalIncome = income,
            totalExpenses = expenses,
            remainingBalance = remainingBalance,
            monthlyBudget = effectiveMonthlyBudget,
            budgetUsedPercentage = budgetUsed,
            topCategories = topCategories,
            recentTransactions = currentMonthTransactions
                .sortedWith(compareByDescending<TransactionEntity> { it.date }.thenByDescending { it.id })
                .take(5),
            trends = trends,
            netWorth = netWorth,
            assetTotal = base.assetTotal,
            debtTotal = debtTotal,
            upcomingBillsCount = base.upcomingBills.size,
            upcomingBillsAmount = base.upcomingBills.sumOf { it.amount },
            accountSnapshots = base.accounts,
            goalProgress = goalProgress,
            envelopeStatuses = envelopeStatuses,
            insights = insights,
            cycleType = base.cycleType,
            cycleStartDate = base.cycleStartDate,
            nextCycleDate = base.nextCycleDate,
            cyclePlannedIncome = base.plannedMonthlyIncome,
            cyclePlannedBudget = effectiveMonthlyBudget,
            carryForwardRemainingBudget = base.carryForwardRemainingBudget,
            cycleRenewalDue = cycleRenewalDue,
            cycleRenewalMessage = buildCycleRenewalMessage(
                cycleType = base.cycleType,
                nextCycleDate = base.nextCycleDate,
                carryForwardRemainingBudget = base.carryForwardRemainingBudget
            ),
            upcomingBillsStatusText = nextUpcomingBill?.let {
                DateUtils.relativeDueLabel(it.nextChargeDate, today)
            } ?: "No bills due soon"
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HomeUiState()
    )

    private fun buildInsights(
        budgetUsedPercentage: Float,
        debtTotal: Double,
        assetTotal: Double,
        remainingBalance: Double,
        activeBills: List<SubscriptionBillEntity>,
        dueDebtsCount: Int,
        goals: List<GoalProgress>
    ): List<FinancialInsight> {
        val items = mutableListOf<FinancialInsight>()
        if (remainingBalance < 0.0) {
            items += FinancialInsight(
                title = "Balance below zero",
                message = "Your current balance is below zero and needs attention.",
                severity = InsightSeverity.WARNING
            )
        }
        if (budgetUsedPercentage >= 1f) {
            items += FinancialInsight(
                title = "Budget exceeded",
                message = "Spending has reached or passed the monthly budget limit.",
                severity = InsightSeverity.WARNING
            )
        } else if (budgetUsedPercentage >= 0.9f) {
            items += FinancialInsight(
                title = "Budget pressure",
                message = "${(budgetUsedPercentage * 100).roundToInt()}% of the monthly budget is already used.",
                severity = InsightSeverity.WARNING
            )
        } else if (budgetUsedPercentage in 0.0f..0.55f) {
            items += FinancialInsight(
                title = "Healthy budget pacing",
                message = "Spending is staying well under your monthly limit so far.",
                severity = InsightSeverity.POSITIVE
            )
        }
        if (assetTotal > 0 && debtTotal > assetTotal * 0.6) {
            items += FinancialInsight(
                title = "Debt load is high",
                message = "Outstanding debt is more than 60% of current tracked assets.",
                severity = InsightSeverity.WARNING
            )
        }
        val overdueBillsCount = activeBills.count { it.nextChargeDate.isBefore(LocalDate.now()) }
        if (overdueBillsCount > 0) {
            items += FinancialInsight(
                title = "Bills overdue",
                message = "$overdueBillsCount bill(s) have passed their due date.",
                severity = InsightSeverity.WARNING
            )
        } else if (activeBills.any { !it.nextChargeDate.isAfter(LocalDate.now().plusDays(14)) && !it.nextChargeDate.isBefore(LocalDate.now()) }) {
            items += FinancialInsight(
                title = "Bills due soon",
                message = "${activeBills.count { !it.nextChargeDate.isAfter(LocalDate.now().plusDays(14)) && !it.nextChargeDate.isBefore(LocalDate.now()) }} bill(s) are due in the next two weeks.",
                severity = InsightSeverity.NEUTRAL
            )
        }
        if (dueDebtsCount > 0) {
            items += FinancialInsight(
                title = "Debt payment due",
                message = "$dueDebtsCount debt payment item(s) need attention soon.",
                severity = InsightSeverity.WARNING
            )
        }
        goals.firstOrNull { it.completionRatio >= 0.75f }?.let { goal ->
            items += FinancialInsight(
                title = "Goal almost complete",
                message = "${goal.name} is ${(goal.completionRatio * 100).roundToInt()}% funded.",
                severity = InsightSeverity.POSITIVE
            )
        }
        return items.take(4)
    }

    fun saveGoal(name: String, targetAmount: String, currentAmount: String) {
        val parsedTarget = targetAmount.toDoubleOrNull() ?: return
        val parsedCurrent = currentAmount.toDoubleOrNull() ?: 0.0
        if (name.isBlank() || parsedTarget <= 0.0) return

        viewModelScope.launch {
            financeRepository.saveGoal(
                com.talent.monthlybudgetkeeper.data.local.entity.GoalEntity(
                    name = name.trim(),
                    targetAmount = parsedTarget,
                    currentAmount = parsedCurrent.coerceIn(0.0, parsedTarget),
                    monthlyContribution = 0.0
                )
            )
        }
    }

    fun continuePreviousCycleBudget() {
        viewModelScope.launch {
            val currentPreferences = settingsRepository.getCurrentPreferences()
            settingsRepository.continueBudgetCycleWithPreviousValues()
            transactionRepository.ensureCycleOpeningIncome(
                cycleStartDate = currentPreferences.nextCycleDate,
                amount = currentPreferences.monthlyIncome
            )
            reminderScheduler.requestImmediateRefresh()
        }
    }

    fun saveNewCycleBudget(income: String, budgetTarget: String) {
        val parsedIncome = income.toDoubleOrNull() ?: return
        val parsedBudgetTarget = budgetTarget.toDoubleOrNull() ?: return
        if (parsedIncome <= 0.0 || parsedBudgetTarget <= 0.0) return

        viewModelScope.launch {
            val currentPreferences = settingsRepository.getCurrentPreferences()
            settingsRepository.renewBudgetCycleWithNewValues(
                income = parsedIncome,
                budgetTarget = parsedBudgetTarget
            )
            transactionRepository.ensureCycleOpeningIncome(
                cycleStartDate = currentPreferences.nextCycleDate,
                amount = parsedIncome
            )
            reminderScheduler.requestImmediateRefresh()
        }
    }

    private fun buildCycleRenewalMessage(
        cycleType: BudgetCycleType,
        nextCycleDate: LocalDate,
        carryForwardRemainingBudget: Boolean
    ): String {
        val carryForwardMessage = if (carryForwardRemainingBudget) {
            "Any unused budget will carry forward."
        } else {
            "The next cycle will start fresh."
        }
        return "${cycleType.label} cycle ended on ${DateUtils.formatDate(nextCycleDate)}. Choose whether to keep the same income and budget or enter new values. $carryForwardMessage"
    }
}

private fun com.talent.monthlybudgetkeeper.data.local.entity.AccountEntity.isLiabilityLike(): Boolean {
    return accountType == AccountType.CREDIT || currentBalance < 0.0
}

private fun com.talent.monthlybudgetkeeper.data.local.entity.AccountEntity.isAssetLike(): Boolean = !isLiabilityLike()
