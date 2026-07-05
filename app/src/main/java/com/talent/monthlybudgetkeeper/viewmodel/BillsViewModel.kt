package com.talent.monthlybudgetkeeper.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.talent.monthlybudgetkeeper.data.local.entity.RecurringItemEntity
import com.talent.monthlybudgetkeeper.data.local.entity.SubscriptionBillEntity
import com.talent.monthlybudgetkeeper.data.local.entity.TransactionEntity
import com.talent.monthlybudgetkeeper.data.model.BillingCycle
import com.talent.monthlybudgetkeeper.data.model.CurrencyOption
import com.talent.monthlybudgetkeeper.data.model.RecurrenceInterval
import com.talent.monthlybudgetkeeper.data.model.TransactionCategory
import com.talent.monthlybudgetkeeper.data.model.TransactionType
import com.talent.monthlybudgetkeeper.data.repository.FinanceRepository
import com.talent.monthlybudgetkeeper.data.repository.SettingsRepository
import com.talent.monthlybudgetkeeper.data.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class BillsUiState(
    val currency: CurrencyOption = CurrencyOption.USD,
    val privacyModeEnabled: Boolean = false,
    val bills: List<SubscriptionBillEntity> = emptyList(),
    val subscriptions: List<SubscriptionBillEntity> = emptyList(),
    val recurringItems: List<RecurringItemEntity> = emptyList()
)

@HiltViewModel
class BillsViewModel @Inject constructor(
    private val financeRepository: FinanceRepository,
    private val transactionRepository: TransactionRepository,
    settingsRepository: SettingsRepository
) : ViewModel() {

    val uiState: StateFlow<BillsUiState> = combine(
        financeRepository.observeSubscriptionBills(),
        financeRepository.observeRecurringItems(),
        settingsRepository.preferencesFlow
    ) { billItems, recurringItems, preferences ->
        BillsUiState(
            currency = preferences.currency,
            privacyModeEnabled = preferences.privacyModeEnabled,
            bills = billItems.filter { !it.isAutoPay }.sortedBy { it.nextChargeDate },
            subscriptions = billItems.filter { it.isAutoPay }.sortedBy { it.nextChargeDate },
            recurringItems = recurringItems.sortedBy { it.nextDueDate }
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = BillsUiState()
    )

    fun saveBill(
        name: String,
        amount: String,
        dueDate: LocalDate,
        cycle: BillingCycle,
        note: String
    ) {
        val parsedAmount = amount.toDoubleOrNull() ?: return
        if (name.isBlank() || parsedAmount <= 0.0) return

        viewModelScope.launch {
            financeRepository.saveSubscriptionBill(
                SubscriptionBillEntity(
                    name = name.trim(),
                    amount = parsedAmount,
                    category = TransactionCategory.BILLS,
                    dueDate = dueDate,
                    nextChargeDate = nextOccurrence(dueDate, cycle),
                    billingCycle = cycle,
                    note = note.trim(),
                    isAutoPay = false
                )
            )
        }
    }

    fun saveSubscription(
        name: String,
        amount: String,
        dueDate: LocalDate,
        cycle: BillingCycle,
        note: String
    ) {
        val parsedAmount = amount.toDoubleOrNull() ?: return
        if (name.isBlank() || parsedAmount <= 0.0) return

        viewModelScope.launch {
            financeRepository.saveSubscriptionBill(
                SubscriptionBillEntity(
                    name = name.trim(),
                    amount = parsedAmount,
                    category = TransactionCategory.ENTERTAINMENT,
                    dueDate = dueDate,
                    nextChargeDate = nextOccurrence(dueDate, cycle),
                    billingCycle = cycle,
                    note = note.trim(),
                    isAutoPay = true
                )
            )
        }
    }

    fun saveRecurring(
        title: String,
        amount: String,
        dueDate: LocalDate,
        interval: RecurrenceInterval,
        note: String
    ) {
        val parsedAmount = amount.toDoubleOrNull() ?: return
        if (title.isBlank() || parsedAmount <= 0.0) return

        viewModelScope.launch {
            financeRepository.saveRecurringItem(
                RecurringItemEntity(
                    title = title.trim(),
                    amount = parsedAmount,
                    type = TransactionType.EXPENSE,
                    category = TransactionCategory.BILLS,
                    startDate = dueDate,
                    nextDueDate = dueDate,
                    interval = interval,
                    note = note.trim(),
                    autoCreateTransaction = false
                )
            )
        }
    }

    fun markBillPaid(item: SubscriptionBillEntity) {
        viewModelScope.launch {
            transactionRepository.saveTransaction(
                TransactionEntity(
                    title = item.name,
                    amount = item.amount,
                    type = TransactionType.EXPENSE,
                    category = item.category,
                    date = LocalDate.now(),
                    note = item.note.ifBlank { "Paid from Bills screen" }
                )
            )
            val nextDate = advance(item.nextChargeDate, item.billingCycle)
            financeRepository.saveSubscriptionBill(
                item.copy(
                    dueDate = nextDate,
                    nextChargeDate = nextDate
                )
            )
        }
    }

    fun generateRecurring(item: RecurringItemEntity) {
        viewModelScope.launch {
            transactionRepository.saveTransaction(
                TransactionEntity(
                    title = item.title,
                    amount = item.amount,
                    type = item.type,
                    category = item.category,
                    date = LocalDate.now(),
                    note = item.note.ifBlank { "Generated from recurring schedule" }
                )
            )
            val nextDate = advance(item.nextDueDate, item.interval)
            financeRepository.saveRecurringItem(
                item.copy(nextDueDate = nextDate)
            )
        }
    }

    private fun advance(date: LocalDate, cycle: BillingCycle): LocalDate {
        return when (cycle) {
            BillingCycle.WEEKLY -> date.plusWeeks(1)
            BillingCycle.MONTHLY -> date.plusMonths(1)
            BillingCycle.QUARTERLY -> date.plusMonths(3)
            BillingCycle.YEARLY -> date.plusYears(1)
        }
    }

    private fun nextOccurrence(
        dueDate: LocalDate,
        cycle: BillingCycle,
        today: LocalDate = LocalDate.now()
    ): LocalDate {
        var nextDate = dueDate
        while (nextDate.isBefore(today)) {
            nextDate = advance(nextDate, cycle)
        }
        return nextDate
    }

    private fun advance(date: LocalDate, interval: RecurrenceInterval): LocalDate {
        return when (interval) {
            RecurrenceInterval.DAILY -> date.plusDays(1)
            RecurrenceInterval.WEEKLY -> date.plusWeeks(1)
            RecurrenceInterval.MONTHLY -> date.plusMonths(1)
            RecurrenceInterval.QUARTERLY -> date.plusMonths(3)
            RecurrenceInterval.YEARLY -> date.plusYears(1)
        }
    }
}
