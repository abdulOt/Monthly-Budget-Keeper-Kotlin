package com.talent.monthlybudgetkeeper.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.talent.monthlybudgetkeeper.data.local.entity.CategoryBudgetEntity
import com.talent.monthlybudgetkeeper.data.model.EnvelopeStatus
import com.talent.monthlybudgetkeeper.data.model.BudgetOverview
import com.talent.monthlybudgetkeeper.data.model.CategoryBudgetStatus
import com.talent.monthlybudgetkeeper.data.model.CurrencyOption
import com.talent.monthlybudgetkeeper.data.model.TransactionCategory
import com.talent.monthlybudgetkeeper.data.model.TransactionType
import com.talent.monthlybudgetkeeper.data.repository.BudgetRepository
import com.talent.monthlybudgetkeeper.data.repository.FinanceRepository
import com.talent.monthlybudgetkeeper.data.repository.SettingsRepository
import com.talent.monthlybudgetkeeper.data.repository.TransactionRepository
import com.talent.monthlybudgetkeeper.utils.DateUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.YearMonth
import javax.inject.Inject

data class BudgetUiState(
    val month: YearMonth = YearMonth.now(),
    val currency: CurrencyOption = CurrencyOption.USD,
    val totalBudget: Double = 0.0,
    val categoryBudgets: Map<TransactionCategory, CategoryBudgetEntity> = emptyMap(),
    val overview: BudgetOverview = BudgetOverview(),
    val envelopeStatuses: List<EnvelopeStatus> = emptyList(),
    val alertMessages: List<String> = emptyList()
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class BudgetViewModel @Inject constructor(
    private val budgetRepository: BudgetRepository,
    transactionRepository: TransactionRepository,
    settingsRepository: SettingsRepository,
    financeRepository: FinanceRepository
) : ViewModel() {
    private val selectedMonth = MutableStateFlow(YearMonth.now())

    val uiState: StateFlow<BudgetUiState> = selectedMonth.flatMapLatest { month ->
        combine(
            transactionRepository.observeTransactions(),
            budgetRepository.observeMonthlyBudget(month),
            budgetRepository.observeCategoryBudgets(month),
            settingsRepository.preferencesFlow,
            financeRepository.observeEnvelopeBudgets()
        ) { transactions, monthlyBudget, categoryBudgets, preferences, envelopeBudgets ->
            val expenseTransactions = transactions.filter {
                it.type == TransactionType.EXPENSE && DateUtils.isInMonth(it.date, month)
            }
            val categoryBudgetMap = categoryBudgets.associateBy(CategoryBudgetEntity::category)
            val categoryStatuses = TransactionCategory.forType(TransactionType.EXPENSE).map { category ->
                val spent = expenseTransactions
                    .filter { it.category == category }
                    .sumOf { it.amount }
                val limit = categoryBudgetMap[category]?.limitAmount ?: 0.0
                val remaining = if (limit > 0) limit - spent else 0.0
                CategoryBudgetStatus(
                    category = category,
                    spent = spent,
                    limit = limit,
                    remaining = remaining,
                    isNearLimit = limit > 0 && spent / limit >= 0.8 && spent <= limit,
                    isExceeded = limit > 0 && spent >= limit
                )
            }
            val totalBudgetAmount = monthlyBudget?.totalBudget ?: 0.0
            val totalSpent = expenseTransactions.sumOf { it.amount }
            val envelopeStatuses = envelopeBudgets
                .filter { it.monthKey == DateUtils.toMonthKey(month) }
                .sortedByDescending { it.spentAmount }
                .map {
                    val planned = it.plannedAmount + it.rolloverAmount
                    EnvelopeStatus(
                        name = it.name,
                        availableAmount = (planned - it.spentAmount).coerceAtLeast(0.0),
                        spentAmount = it.spentAmount,
                        progress = if (planned > 0) {
                            (it.spentAmount / planned).toFloat().coerceAtLeast(0f)
                        } else {
                            0f
                        }
                    )
                }
            val alertMessages = buildList {
                if (totalBudgetAmount > 0.0 && totalSpent >= totalBudgetAmount) {
                    add("Monthly budget exceeded by ${totalSpent - totalBudgetAmount}.")
                }
                categoryStatuses.filter { it.isExceeded }.forEach { status ->
                    add("${status.category.displayName} is over budget.")
                }
                categoryStatuses.filter { it.isNearLimit }.forEach { status ->
                    add("${status.category.displayName} is close to its budget limit.")
                }
                envelopeStatuses.filter { it.progress >= 1f }.forEach { status ->
                    add("${status.name} envelope has been fully used.")
                }
            }.take(5)

            BudgetUiState(
                month = month,
                currency = preferences.currency,
                totalBudget = totalBudgetAmount,
                categoryBudgets = categoryBudgetMap,
                envelopeStatuses = envelopeStatuses,
                alertMessages = alertMessages,
                overview = BudgetOverview(
                    monthKey = DateUtils.toMonthKey(month),
                    totalBudget = totalBudgetAmount,
                    totalSpent = totalSpent,
                    remainingBudget = totalBudgetAmount - totalSpent,
                    categories = categoryStatuses
                )
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = BudgetUiState()
    )

    fun previousMonth() {
        selectedMonth.value = selectedMonth.value.minusMonths(1)
    }

    fun nextMonth() {
        selectedMonth.value = selectedMonth.value.plusMonths(1)
    }

    fun saveTotalBudget(input: String) {
        val amount = input.toDoubleOrNull() ?: 0.0
        viewModelScope.launch {
            budgetRepository.setTotalBudget(selectedMonth.value, amount)
        }
    }

    fun saveCategoryBudget(category: TransactionCategory, input: String) {
        val amount = input.toDoubleOrNull() ?: 0.0
        viewModelScope.launch {
            budgetRepository.setCategoryBudget(selectedMonth.value, category, amount)
        }
    }
}
