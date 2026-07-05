package com.talent.monthlybudgetkeeper.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.talent.monthlybudgetkeeper.data.local.entity.TransactionEntity
import com.talent.monthlybudgetkeeper.data.model.TransactionCategory
import com.talent.monthlybudgetkeeper.data.model.TransactionType
import com.talent.monthlybudgetkeeper.data.repository.TransactionRepository
import com.talent.monthlybudgetkeeper.utils.TransactionValidators
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class TransactionFormUiState(
    val transactionId: Long = 0L,
    val title: String = "",
    val amount: String = "",
    val type: TransactionType = TransactionType.EXPENSE,
    val category: TransactionCategory = TransactionCategory.FOOD,
    val date: LocalDate = LocalDate.now(),
    val note: String = "",
    val titleError: String? = null,
    val amountError: String? = null,
    val isLoading: Boolean = true,
    val isSaving: Boolean = false
)

@HiltViewModel
class TransactionFormViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val transactionRepository: TransactionRepository
) : ViewModel() {

    private val transactionId: Long = savedStateHandle["transactionId"] ?: 0L
    private val presetType: TransactionType = savedStateHandle.get<String>("presetType")
        ?.let(TransactionType::valueOf)
        ?: TransactionType.EXPENSE

    private val _uiState = MutableStateFlow(
        TransactionFormUiState(
            isLoading = true,
            type = presetType,
            category = TransactionCategory.defaultFor(presetType)
        )
    )
    val uiState: StateFlow<TransactionFormUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<Long>()
    val events = _events.asSharedFlow()

    init {
        viewModelScope.launch {
            if (transactionId == 0L) {
                _uiState.value = _uiState.value.copy(transactionId = 0L, isLoading = false)
                return@launch
            }

            val transaction = transactionRepository.getTransaction(transactionId)
            if (transaction != null) {
                _uiState.value = TransactionFormUiState(
                    transactionId = transaction.id,
                    title = transaction.title,
                    amount = transaction.amount.toString(),
                    type = transaction.type,
                    category = transaction.category,
                    date = transaction.date,
                    note = transaction.note,
                    isLoading = false
                )
            } else {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }

    fun updateTitle(title: String) {
        _uiState.value = _uiState.value.copy(title = title, titleError = null)
    }

    fun updateAmount(amount: String) {
        _uiState.value = _uiState.value.copy(amount = amount, amountError = null)
    }

    fun updateType(type: TransactionType) {
        _uiState.value = _uiState.value.copy(
            type = type,
            category = TransactionCategory.defaultFor(type)
        )
    }

    fun updateCategory(category: TransactionCategory) {
        _uiState.value = _uiState.value.copy(category = category)
    }

    fun updateDate(date: LocalDate) {
        _uiState.value = _uiState.value.copy(date = date)
    }

    fun updateNote(note: String) {
        _uiState.value = _uiState.value.copy(note = note)
    }

    fun saveTransaction() {
        val state = _uiState.value
        val titleError = TransactionValidators.validateTitle(state.title)
        val amountError = TransactionValidators.validateAmount(state.amount)

        if (titleError != null || amountError != null) {
            _uiState.value = state.copy(
                titleError = titleError,
                amountError = amountError
            )
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true)
            val savedId = transactionRepository.saveTransaction(
                TransactionEntity(
                    id = state.transactionId,
                    title = state.title.trim(),
                    amount = state.amount.toDouble(),
                    type = state.type,
                    category = state.category,
                    date = state.date,
                    note = state.note.trim()
                )
            )
            _uiState.value = _uiState.value.copy(isSaving = false)
            _events.emit(savedId)
        }
    }
}
