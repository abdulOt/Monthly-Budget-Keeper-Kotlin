package com.talent.monthlybudgetkeeper.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.talent.monthlybudgetkeeper.data.local.entity.TransactionEntity
import com.talent.monthlybudgetkeeper.data.model.CurrencyOption
import com.talent.monthlybudgetkeeper.data.model.TransactionCategory
import com.talent.monthlybudgetkeeper.data.model.TransactionFilter
import com.talent.monthlybudgetkeeper.data.model.TransactionSortOption
import com.talent.monthlybudgetkeeper.data.model.TransactionType
import com.talent.monthlybudgetkeeper.data.repository.SettingsRepository
import com.talent.monthlybudgetkeeper.data.repository.TransactionRepository
import com.talent.monthlybudgetkeeper.utils.DateUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.YearMonth
import javax.inject.Inject

data class HistoryUiState(
    val currency: CurrencyOption = CurrencyOption.PKR,
    val filter: TransactionFilter = TransactionFilter(),
    val availableMonths: List<YearMonth> = listOf(YearMonth.now()),
    val transactions: List<TransactionEntity> = emptyList()
)

@HiltViewModel
class HistoryViewModel @Inject constructor(
    transactionRepository: TransactionRepository,
    settingsRepository: SettingsRepository
) : ViewModel() {

    private val filter = MutableStateFlow(TransactionFilter())
    val activeFilter: StateFlow<TransactionFilter> = filter.asStateFlow()

    val uiState: StateFlow<HistoryUiState> = combine(
        transactionRepository.observeTransactions(),
        settingsRepository.preferencesFlow,
        filter
    ) { transactions, preferences, activeFilter ->
        val months = (transactions.map { YearMonth.from(it.date) } + YearMonth.now())
            .distinct()
            .sortedDescending()

        val filteredTransactions = transactions
            .filter { DateUtils.isInMonth(it.date, activeFilter.month) }
            .filter { activeFilter.type == null || it.type == activeFilter.type }
            .filter { activeFilter.category == null || it.category == activeFilter.category }
            .filter {
                activeFilter.searchQuery.isBlank() ||
                    it.title.contains(activeFilter.searchQuery, ignoreCase = true) ||
                    it.note.contains(activeFilter.searchQuery, ignoreCase = true)
            }
            .let { items ->
                when (activeFilter.sortOption) {
                    TransactionSortOption.NEWEST -> items.sortedWith(
                        compareByDescending<TransactionEntity> { it.date }.thenByDescending { it.id }
                    )
                    TransactionSortOption.OLDEST -> items.sortedWith(
                        compareBy<TransactionEntity> { it.date }.thenBy { it.id }
                    )
                    TransactionSortOption.HIGHEST_AMOUNT -> items.sortedByDescending { it.amount }
                    TransactionSortOption.LOWEST_AMOUNT -> items.sortedBy { it.amount }
                }
            }

        HistoryUiState(
            currency = preferences.currency,
            filter = activeFilter,
            availableMonths = months,
            transactions = filteredTransactions
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HistoryUiState()
    )

    fun updateMonth(month: YearMonth) {
        filter.value = filter.value.copy(month = month)
    }

    fun updateType(type: TransactionType?) {
        filter.value = filter.value.copy(
            type = type,
            category = filter.value.category?.takeIf { it.type == type || type == null }
        )
    }

    fun updateCategory(category: TransactionCategory?) {
        filter.value = filter.value.copy(category = category)
    }

    fun updateSearch(query: String) {
        filter.value = filter.value.copy(searchQuery = query)
    }

    fun updateSort(sortOption: TransactionSortOption) {
        filter.value = filter.value.copy(sortOption = sortOption)
    }
}
