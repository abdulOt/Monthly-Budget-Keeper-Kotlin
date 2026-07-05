package com.talent.monthlybudgetkeeper.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.talent.monthlybudgetkeeper.data.local.entity.AccountEntity
import com.talent.monthlybudgetkeeper.data.local.entity.TransactionEntity
import com.talent.monthlybudgetkeeper.data.local.entity.TransferEntity
import com.talent.monthlybudgetkeeper.data.model.CurrencyOption
import com.talent.monthlybudgetkeeper.data.repository.FinanceRepository
import com.talent.monthlybudgetkeeper.data.repository.SettingsRepository
import com.talent.monthlybudgetkeeper.data.repository.TransactionRepository
import com.talent.monthlybudgetkeeper.utils.DateUtils
import java.time.LocalDate
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.YearMonth
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class TransactionsUiState(
    val month: YearMonth = YearMonth.now(),
    val currency: CurrencyOption = CurrencyOption.USD,
    val privacyModeEnabled: Boolean = false,
    val searchQuery: String = "",
    val transactions: List<TransactionEntity> = emptyList(),
    val transfers: List<TransferEntity> = emptyList(),
    val accounts: List<AccountEntity> = emptyList()
)

@HiltViewModel
class TransactionsViewModel @Inject constructor(
    transactionRepository: TransactionRepository,
    private val financeRepository: FinanceRepository,
    settingsRepository: SettingsRepository
) : ViewModel() {

    private val selectedMonth = MutableStateFlow(YearMonth.now())
    private val searchQuery = MutableStateFlow("")
    private val _transferSaved = MutableSharedFlow<Unit>()
    val transferSaved = _transferSaved.asSharedFlow()
    private val financeOverviewFlow = combine(
        financeRepository.observeTransfers(),
        financeRepository.observeAccounts()
    ) { transfers, accounts ->
        transfers to accounts
    }

    val uiState: StateFlow<TransactionsUiState> = combine(
        transactionRepository.observeTransactions(),
        financeOverviewFlow,
        settingsRepository.preferencesFlow,
        selectedMonth,
        searchQuery
    ) { transactions, financeOverview, preferences, month, query ->
        val (transfers, accounts) = financeOverview
        val normalizedQuery = query.trim()
        val filteredTransactions = transactions
            .filter { DateUtils.isInMonth(it.date, month) }
            .filter {
                normalizedQuery.isBlank() ||
                    it.title.contains(normalizedQuery, ignoreCase = true) ||
                    it.note.contains(normalizedQuery, ignoreCase = true)
            }
            .sortedWith(compareByDescending<TransactionEntity> { it.date }.thenByDescending { it.id })
        val filteredTransfers = transfers
            .filter { DateUtils.isInMonth(it.date, month) }
            .filter { normalizedQuery.isBlank() || it.note.contains(normalizedQuery, ignoreCase = true) }
            .sortedWith(compareByDescending<TransferEntity> { it.date }.thenByDescending { it.id })

        TransactionsUiState(
            month = month,
            currency = preferences.currency,
            privacyModeEnabled = preferences.privacyModeEnabled,
            searchQuery = query,
            transactions = filteredTransactions,
            transfers = filteredTransfers,
            accounts = accounts.filter { !it.isArchived }
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = TransactionsUiState()
    )

    fun previousMonth() {
        selectedMonth.value = selectedMonth.value.minusMonths(1)
    }

    fun nextMonth() {
        selectedMonth.value = selectedMonth.value.plusMonths(1)
    }

    fun updateSearch(query: String) {
        searchQuery.value = query
    }

    fun saveTransfer(
        fromAccountRemoteId: String,
        toAccountRemoteId: String,
        amount: String,
        date: LocalDate,
        note: String
    ) {
        val parsedAmount = amount.toDoubleOrNull() ?: return
        if (fromAccountRemoteId.isBlank() || toAccountRemoteId.isBlank()) return
        if (fromAccountRemoteId == toAccountRemoteId || parsedAmount <= 0.0) return

        viewModelScope.launch {
            financeRepository.saveTransfer(
                TransferEntity(
                    fromAccountRemoteId = fromAccountRemoteId,
                    toAccountRemoteId = toAccountRemoteId,
                    amount = parsedAmount,
                    date = date,
                    note = note.trim()
                )
            )
            _transferSaved.emit(Unit)
        }
    }
}
