package com.talent.monthlybudgetkeeper.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.talent.monthlybudgetkeeper.data.local.entity.AccountEntity
import com.talent.monthlybudgetkeeper.data.local.entity.DebtEntity
import com.talent.monthlybudgetkeeper.data.local.entity.EnvelopeBudgetEntity
import com.talent.monthlybudgetkeeper.data.local.entity.GoalEntity
import com.talent.monthlybudgetkeeper.data.local.entity.NetWorthSnapshotEntity
import com.talent.monthlybudgetkeeper.data.local.entity.RecurringItemEntity
import com.talent.monthlybudgetkeeper.data.local.entity.SubscriptionBillEntity
import com.talent.monthlybudgetkeeper.data.local.entity.TransactionEntity
import com.talent.monthlybudgetkeeper.data.model.AccountSnapshot
import com.talent.monthlybudgetkeeper.data.model.AccountType
import com.talent.monthlybudgetkeeper.data.model.CategorySpend
import com.talent.monthlybudgetkeeper.data.model.CurrencyOption
import com.talent.monthlybudgetkeeper.data.model.EnvelopeStatus
import com.talent.monthlybudgetkeeper.data.model.FinancialInsight
import com.talent.monthlybudgetkeeper.data.model.GoalProgress
import com.talent.monthlybudgetkeeper.data.model.InsightSeverity
import com.talent.monthlybudgetkeeper.data.model.MonthlyTrend
import com.talent.monthlybudgetkeeper.data.model.TransactionType
import com.talent.monthlybudgetkeeper.data.repository.FinanceRepository
import com.talent.monthlybudgetkeeper.data.repository.SettingsRepository
import com.talent.monthlybudgetkeeper.data.repository.TransactionRepository
import com.talent.monthlybudgetkeeper.utils.DateUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.YearMonth
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlin.math.abs
import kotlin.math.roundToInt

data class DebtProgressUi(
    val id: Long,
    val name: String,
    val remainingAmount: Double,
    val totalAmount: Double,
    val minimumPayment: Double,
    val progress: Float,
    val dueDateLabel: String
)

data class NetWorthTrendPoint(
    val dateLabel: String,
    val netWorth: Double
)

data class ReportsUiState(
    val month: YearMonth = YearMonth.now(),
    val currency: CurrencyOption = CurrencyOption.PKR,
    val privacyModeEnabled: Boolean = false,
    val totalIncome: Double = 0.0,
    val totalExpenses: Double = 0.0,
    val balance: Double = 0.0,
    val categoryBreakdown: List<CategorySpend> = emptyList(),
    val trends: List<MonthlyTrend> = emptyList(),
    val transactions: List<TransactionEntity> = emptyList(),
    val netWorth: Double = 0.0,
    val assetTotal: Double = 0.0,
    val liabilityTotal: Double = 0.0,
    val debtOutstanding: Double = 0.0,
    val recurringMonthlyOutflow: Double = 0.0,
    val upcomingBills: Double = 0.0,
    val accountSnapshots: List<AccountSnapshot> = emptyList(),
    val debtProgress: List<DebtProgressUi> = emptyList(),
    val netWorthTrend: List<NetWorthTrendPoint> = emptyList(),
    val envelopeStatuses: List<EnvelopeStatus> = emptyList(),
    val goalProgress: List<GoalProgress> = emptyList(),
    val insights: List<FinancialInsight> = emptyList()
)

private data class ReportsBaseBundle(
    val month: YearMonth,
    val transactions: List<TransactionEntity>,
    val currency: CurrencyOption,
    val privacyModeEnabled: Boolean,
    val accountSnapshots: List<AccountSnapshot>,
    val availableBalance: Double,
    val assetTotal: Double,
    val liabilityAccountTotal: Double
)

private data class ReportsFinanceBundle(
    val bills: List<SubscriptionBillEntity>,
    val goals: List<GoalEntity>,
    val debts: List<DebtEntity>,
    val snapshots: List<NetWorthSnapshotEntity>,
    val envelopes: List<EnvelopeBudgetEntity>,
    val recurringItems: List<RecurringItemEntity>
)

@HiltViewModel
class ReportsViewModel @Inject constructor(
    transactionRepository: TransactionRepository,
    settingsRepository: SettingsRepository,
    financeRepository: FinanceRepository
) : ViewModel() {
    private val selectedMonth = MutableStateFlow(YearMonth.now())

    private val baseFlow = combine(
        selectedMonth,
        transactionRepository.observeTransactions(),
        settingsRepository.preferencesFlow,
        financeRepository.observeAccounts()
    ) { month, transactions, preferences, accounts ->
        val primaryBalanceAccount = accounts.firstOrNull {
            !it.isArchived && it.institution == FinanceRepository.SYSTEM_BALANCE_ACCOUNT_MARKER
        }
        ReportsBaseBundle(
            month = month,
            transactions = transactions,
            currency = preferences.currency,
            privacyModeEnabled = preferences.privacyModeEnabled,
            accountSnapshots = accounts
                .filter { !it.isArchived }
                .sortedByDescending { abs(it.currentBalance) }
                .take(4)
                .map { AccountSnapshot(it.name, it.currentBalance, it.currencyCode) },
            availableBalance = primaryBalanceAccount?.currentBalance ?: 0.0,
            assetTotal = accounts
                .filter { !it.isArchived && it.includeInNetWorth && it.isAssetLike() }
                .sumOf { it.currentBalance.coerceAtLeast(0.0) },
            liabilityAccountTotal = accounts
                .filter { !it.isArchived && it.includeInNetWorth && it.isLiabilityLike() }
                .sumOf { abs(it.currentBalance) }
        )
    }

    private val financeFlow = combine(
        combine(
            financeRepository.observeSubscriptionBills(),
            financeRepository.observeGoals(),
            financeRepository.observeDebts(),
            financeRepository.observeEnvelopeBudgets(),
            financeRepository.observeRecurringItems()
        ) { bills, goals, debts, envelopes, recurringItems ->
            arrayOf(bills, goals, debts, envelopes, recurringItems)
        },
        financeRepository.observeNetWorthSnapshots()
    ) { financeParts, snapshots ->
        @Suppress("UNCHECKED_CAST")
        ReportsFinanceBundle(
            bills = financeParts[0] as List<SubscriptionBillEntity>,
            goals = financeParts[1] as List<GoalEntity>,
            debts = financeParts[2] as List<DebtEntity>,
            envelopes = financeParts[3] as List<EnvelopeBudgetEntity>,
            recurringItems = financeParts[4] as List<RecurringItemEntity>,
            snapshots = snapshots
        )
    }

    val uiState: StateFlow<ReportsUiState> = combine(
        baseFlow,
        financeFlow
    ) { base, finance ->
        val monthTransactions = base.transactions.filter {
            DateUtils.isInMonth(it.date, base.month) && !it.isTransfer
        }
        val totalIncome = monthTransactions
            .filter { it.type == TransactionType.INCOME }
            .sumOf(TransactionEntity::amount)
        val totalExpenses = monthTransactions
            .filter { it.type == TransactionType.EXPENSE }
            .sumOf(TransactionEntity::amount)
        val breakdown = monthTransactions
            .filter { it.type == TransactionType.EXPENSE }
            .groupBy { it.category }
            .map { (category, values) -> CategorySpend(category, values.sumOf(TransactionEntity::amount)) }
            .sortedByDescending { it.amount }
        val trends = DateUtils.recentMonths(6, from = base.month).map { trendMonth ->
            val trendTransactions = base.transactions.filter {
                DateUtils.isInMonth(it.date, trendMonth) && !it.isTransfer
            }
            MonthlyTrend(
                month = trendMonth,
                income = trendTransactions
                    .filter { it.type == TransactionType.INCOME }
                    .sumOf(TransactionEntity::amount),
                expense = trendTransactions
                    .filter { it.type == TransactionType.EXPENSE }
                    .sumOf(TransactionEntity::amount)
            )
        }
        val debtOutstanding = finance.debts.sumOf { it.remainingAmount }
        val liabilityTotal = base.liabilityAccountTotal + debtOutstanding
        val debtProgress = finance.debts
            .sortedBy { it.dueDate ?: java.time.LocalDate.MAX }
            .map { debt ->
                val totalAmount = debt.totalAmount.takeIf { it > 0.0 } ?: debt.remainingAmount
                val progress = if (totalAmount > 0.0) {
                    (1.0 - (debt.remainingAmount / totalAmount)).toFloat().coerceIn(0f, 1f)
                } else {
                    0f
                }
                DebtProgressUi(
                    id = debt.id,
                    name = debt.name,
                    remainingAmount = debt.remainingAmount,
                    totalAmount = totalAmount,
                    minimumPayment = debt.minimumPayment,
                    progress = progress,
                    dueDateLabel = debt.dueDate?.let { DateUtils.relativeDueLabel(it) } ?: "No due date"
                )
            }
        val netWorthTrend = finance.snapshots
            .sortedBy { it.snapshotDate }
            .takeLast(6)
            .map {
                NetWorthTrendPoint(
                    dateLabel = DateUtils.formatDate(it.snapshotDate),
                    netWorth = it.netWorth
                )
            }
        val envelopeStatuses = finance.envelopes
            .filter { it.monthKey == DateUtils.toMonthKey(base.month) }
            .sortedByDescending { it.spentAmount }
            .take(4)
            .map {
                EnvelopeStatus(
                    name = it.name,
                    availableAmount = (it.plannedAmount + it.rolloverAmount - it.spentAmount).coerceAtLeast(0.0),
                    spentAmount = it.spentAmount,
                    progress = if (it.plannedAmount + it.rolloverAmount > 0) {
                        (it.spentAmount / (it.plannedAmount + it.rolloverAmount)).toFloat()
                    } else {
                        0f
                    }
                )
            }
        val goalProgress = finance.goals
            .sortedByDescending { if (it.targetAmount > 0) it.currentAmount / it.targetAmount else 0.0 }
            .take(4)
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
        val upcomingBills = finance.bills
            .filter { it.isActive && DateUtils.isInMonth(it.nextChargeDate, base.month) }
            .sumOf { it.amount }
        val recurringMonthlyOutflow = finance.recurringItems
            .filter { it.isActive && it.type == TransactionType.EXPENSE }
            .sumOf { it.amount }

        ReportsUiState(
            month = base.month,
            currency = base.currency,
            privacyModeEnabled = base.privacyModeEnabled,
            totalIncome = totalIncome,
            totalExpenses = totalExpenses,
            balance = base.availableBalance,
            categoryBreakdown = breakdown,
            trends = trends,
            transactions = monthTransactions.sortedWith(
                compareByDescending<TransactionEntity> { it.date }.thenByDescending { it.id }
            ),
            netWorth = base.assetTotal - liabilityTotal,
            assetTotal = base.assetTotal,
            liabilityTotal = liabilityTotal,
            debtOutstanding = debtOutstanding,
            recurringMonthlyOutflow = recurringMonthlyOutflow,
            upcomingBills = upcomingBills,
            accountSnapshots = base.accountSnapshots,
            debtProgress = debtProgress,
            netWorthTrend = netWorthTrend,
            envelopeStatuses = envelopeStatuses,
            goalProgress = goalProgress,
            insights = buildInsights(
                netWorth = base.assetTotal - liabilityTotal,
                recurringOutflow = recurringMonthlyOutflow,
                upcomingBills = upcomingBills,
                goalProgress = goalProgress
            )
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ReportsUiState()
    )

    fun previousMonth() {
        selectedMonth.value = selectedMonth.value.minusMonths(1)
    }

    fun nextMonth() {
        selectedMonth.value = selectedMonth.value.plusMonths(1)
    }

    private fun buildInsights(
        netWorth: Double,
        recurringOutflow: Double,
        upcomingBills: Double,
        goalProgress: List<GoalProgress>
    ): List<FinancialInsight> {
        val list = mutableListOf<FinancialInsight>()
        if (netWorth > 0) {
            list += FinancialInsight(
                title = "Positive net worth",
                message = "Tracked assets are currently ahead of debts.",
                severity = InsightSeverity.POSITIVE
            )
        } else {
            list += FinancialInsight(
                title = "Net worth needs attention",
                message = "Tracked debts are higher than tracked assets right now.",
                severity = InsightSeverity.WARNING
            )
        }
        if (recurringOutflow > 0) {
            list += FinancialInsight(
                title = "Recurring commitments",
                message = "Recurring outflows total ${recurringOutflow.roundToInt()} this cycle.",
                severity = InsightSeverity.NEUTRAL
            )
        }
        if (upcomingBills > 0) {
            list += FinancialInsight(
                title = "Bills scheduled this month",
                message = "Upcoming bills total ${upcomingBills.roundToInt()} in the selected month.",
                severity = InsightSeverity.NEUTRAL
            )
        }
        goalProgress.firstOrNull { it.completionRatio > 0.8f }?.let {
            list += FinancialInsight(
                title = "Savings momentum",
                message = "${it.name} is ${(it.completionRatio * 100).roundToInt()}% funded.",
                severity = InsightSeverity.POSITIVE
            )
        }
        return list.take(4)
    }
}

private fun AccountEntity.isLiabilityLike(): Boolean {
    return accountType == AccountType.CREDIT || currentBalance < 0.0
}

private fun AccountEntity.isAssetLike(): Boolean = !isLiabilityLike()
