package com.talent.monthlybudgetkeeper.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.talent.monthlybudgetkeeper.data.local.entity.AccountEntity
import com.talent.monthlybudgetkeeper.data.local.entity.ReceiptAttachmentEntity
import com.talent.monthlybudgetkeeper.data.local.entity.TransactionEntity
import com.talent.monthlybudgetkeeper.data.model.CurrencyOption
import com.talent.monthlybudgetkeeper.data.repository.FinanceRepository
import com.talent.monthlybudgetkeeper.data.repository.SettingsRepository
import com.talent.monthlybudgetkeeper.data.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.ExperimentalCoroutinesApi
import javax.inject.Inject

data class TransactionDetailUiState(
    val currency: CurrencyOption = CurrencyOption.PKR,
    val transaction: TransactionEntity? = null,
    val account: AccountEntity? = null,
    val receipts: List<ReceiptAttachmentEntity> = emptyList()
)

@HiltViewModel
@OptIn(ExperimentalCoroutinesApi::class)
class TransactionDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val transactionRepository: TransactionRepository,
    financeRepository: FinanceRepository,
    settingsRepository: SettingsRepository
) : ViewModel() {
    private val transactionId: Long? = savedStateHandle.get<Long>("transactionId")?.takeIf { it > 0L }

    private val _deleted = MutableSharedFlow<Unit>()
    val deleted = _deleted.asSharedFlow()

    private val transactionFlow = transactionId?.let(transactionRepository::observeTransaction)
        ?: flowOf(null)
    private val accountFlow = combine(
        transactionFlow,
        financeRepository.observeAccounts()
    ) { transaction, accounts ->
        transaction?.accountRemoteId?.let { remoteId ->
            accounts.firstOrNull { it.remoteId == remoteId }
        }
    }
    private val receiptsFlow = transactionFlow.flatMapLatest { transaction ->
        val remoteId = transaction?.remoteId
        if (remoteId.isNullOrBlank()) {
            flowOf(emptyList())
        } else {
            financeRepository.observeReceiptAttachments(remoteId)
        }
    }

    val uiState: StateFlow<TransactionDetailUiState> = combine(
        transactionFlow,
        accountFlow,
        receiptsFlow,
        settingsRepository.preferencesFlow
    ) { transaction, account, receipts, preferences ->
        TransactionDetailUiState(
            currency = preferences.currency,
            transaction = transaction,
            account = account,
            receipts = receipts
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = TransactionDetailUiState()
    )

    fun deleteTransaction() {
        val transaction = uiState.value.transaction ?: return
        viewModelScope.launch {
            transactionRepository.deleteTransaction(transaction)
            _deleted.emit(Unit)
        }
    }
}
