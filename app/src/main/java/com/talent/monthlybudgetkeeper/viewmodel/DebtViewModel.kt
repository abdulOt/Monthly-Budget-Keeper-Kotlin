package com.talent.monthlybudgetkeeper.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.talent.monthlybudgetkeeper.data.local.entity.AccountEntity
import com.talent.monthlybudgetkeeper.data.local.entity.DebtEntity
import com.talent.monthlybudgetkeeper.data.local.entity.TransactionEntity
import com.talent.monthlybudgetkeeper.data.model.AccountType
import com.talent.monthlybudgetkeeper.data.model.CurrencyOption
import com.talent.monthlybudgetkeeper.data.model.TransactionCategory
import com.talent.monthlybudgetkeeper.data.model.TransactionType
import com.talent.monthlybudgetkeeper.data.repository.FinanceRepository
import com.talent.monthlybudgetkeeper.data.repository.SettingsRepository
import com.talent.monthlybudgetkeeper.data.repository.TransactionRepository
import com.talent.monthlybudgetkeeper.utils.DateUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.min

data class DebtUiState(
    val currency: CurrencyOption = CurrencyOption.PKR,
    val privacyModeEnabled: Boolean = false,
    val debts: List<DebtEntity> = emptyList(),
    val paymentAccounts: List<AccountEntity> = emptyList(),
    val totalOutstanding: Double = 0.0,
    val dueSoonCount: Int = 0,
    val averageProgress: Float = 0f
)

@HiltViewModel
class DebtViewModel @Inject constructor(
    private val financeRepository: FinanceRepository,
    private val transactionRepository: TransactionRepository,
    settingsRepository: SettingsRepository
) : ViewModel() {

    val uiState: StateFlow<DebtUiState> = combine(
        financeRepository.observeDebts(),
        financeRepository.observeAccounts(),
        settingsRepository.preferencesFlow
    ) { debts, accounts, preferences ->
        val today = LocalDate.now()
        val dueSoonCount = debts.count { debt ->
            val dueDate = debt.dueDate ?: return@count false
            debt.remainingAmount > 0.0 &&
                (dueDate.isBefore(today) || !dueDate.isAfter(today.plusDays(preferences.reminderLeadDays.toLong())))
        }
        val averageProgress = debts
            .mapNotNull { debt ->
                val total = debt.totalAmount.takeIf { it > 0.0 } ?: return@mapNotNull null
                (1.0 - (debt.remainingAmount / total)).toFloat().coerceIn(0f, 1f)
            }
            .average()
            .toFloat()
            .takeIf { !it.isNaN() }
            ?: 0f

        DebtUiState(
            currency = preferences.currency,
            privacyModeEnabled = preferences.privacyModeEnabled,
            debts = debts.sortedWith(
                compareBy<DebtEntity> { it.dueDate ?: LocalDate.MAX }
                    .thenByDescending { it.updatedAt }
            ),
            paymentAccounts = accounts
                .filter { !it.isArchived && it.isAssetLike() }
                .sortedByDescending { it.currentBalance },
            totalOutstanding = debts.sumOf { it.remainingAmount.coerceAtLeast(0.0) },
            dueSoonCount = dueSoonCount,
            averageProgress = averageProgress
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = DebtUiState()
    )

    fun saveDebt(
        existingDebt: DebtEntity?,
        name: String,
        totalBalance: String,
        minimumPayment: String,
        dueDate: LocalDate?,
        interestRate: String,
        note: String
    ) {
        val parsedBalance = totalBalance.toDoubleOrNull() ?: return
        val parsedMinimumPayment = minimumPayment.toDoubleOrNull() ?: 0.0
        val parsedInterestRate = interestRate.toDoubleOrNull() ?: 0.0
        if (name.isBlank() || parsedBalance <= 0.0) return

        val debt = if (existingDebt == null) {
            DebtEntity(
                name = name.trim(),
                lender = name.trim(),
                totalAmount = parsedBalance,
                remainingAmount = parsedBalance,
                dueDate = dueDate,
                interestRate = parsedInterestRate,
                minimumPayment = parsedMinimumPayment.coerceAtLeast(0.0),
                plannedPayment = parsedMinimumPayment.coerceAtLeast(0.0),
                note = note.trim()
            )
        } else {
            val normalizedBalance = parsedBalance.coerceAtLeast(0.0)
            existingDebt.copy(
                name = name.trim(),
                lender = existingDebt.lender.ifBlank { name.trim() },
                totalAmount = maxOf(existingDebt.totalAmount, normalizedBalance),
                remainingAmount = normalizedBalance,
                dueDate = dueDate,
                interestRate = parsedInterestRate.coerceAtLeast(0.0),
                minimumPayment = parsedMinimumPayment.coerceAtLeast(0.0),
                plannedPayment = parsedMinimumPayment.coerceAtLeast(0.0),
                note = note.trim()
            )
        }

        viewModelScope.launch {
            financeRepository.saveDebt(debt)
        }
    }

    fun payDebt(
        debtId: Long,
        amount: String,
        paymentDate: LocalDate,
        paymentAccountRemoteId: String?,
        createExpenseTransaction: Boolean,
        note: String
    ) {
        val parsedAmount = amount.toDoubleOrNull() ?: return
        if (parsedAmount <= 0.0) return

        viewModelScope.launch {
            val debt = financeRepository.getAllDebts().firstOrNull { it.id == debtId } ?: return@launch
            val paymentAmount = min(parsedAmount, debt.remainingAmount)
            if (paymentAmount <= 0.0) return@launch

            financeRepository.saveDebt(
                debt.copy(
                    remainingAmount = (debt.remainingAmount - paymentAmount).coerceAtLeast(0.0)
                )
            )

            if (createExpenseTransaction) {
                transactionRepository.saveTransaction(
                    TransactionEntity(
                        title = "${debt.name} payment",
                        amount = paymentAmount,
                        type = TransactionType.EXPENSE,
                        category = TransactionCategory.BILLS,
                        date = paymentDate,
                        note = note.trim().ifBlank {
                            "Debt payment recorded ${DateUtils.formatDate(paymentDate)}"
                        },
                        accountRemoteId = paymentAccountRemoteId?.takeIf { it.isNotBlank() }
                    )
                )
            } else {
                paymentAccountRemoteId
                    ?.takeIf { it.isNotBlank() }
                    ?.let { accountRemoteId ->
                        val account = financeRepository.getAllAccounts().firstOrNull { it.remoteId == accountRemoteId }
                        if (account != null) {
                            financeRepository.saveAccount(
                                account.copy(currentBalance = account.currentBalance - paymentAmount)
                            )
                        }
                    }
            }
        }
    }

    fun deleteDebt(debt: DebtEntity) {
        viewModelScope.launch {
            financeRepository.deleteDebt(debt)
        }
    }
}

private fun AccountEntity.isLiabilityLike(): Boolean {
    return accountType == AccountType.CREDIT || currentBalance < 0.0
}

private fun AccountEntity.isAssetLike(): Boolean = !isLiabilityLike()
