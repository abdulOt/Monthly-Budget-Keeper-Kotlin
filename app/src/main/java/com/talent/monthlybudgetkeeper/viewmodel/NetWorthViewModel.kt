package com.talent.monthlybudgetkeeper.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.talent.monthlybudgetkeeper.data.local.entity.AccountEntity
import com.talent.monthlybudgetkeeper.data.local.entity.DebtEntity
import com.talent.monthlybudgetkeeper.data.model.AccountType
import com.talent.monthlybudgetkeeper.data.model.CurrencyOption
import com.talent.monthlybudgetkeeper.data.repository.FinanceRepository
import com.talent.monthlybudgetkeeper.data.repository.SettingsRepository
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

enum class NetWorthEntryKind(val label: String) {
    ASSET("Asset"),
    LIABILITY("Liability")
}

data class NetWorthUiState(
    val currency: CurrencyOption = CurrencyOption.PKR,
    val privacyModeEnabled: Boolean = false,
    val assetAccounts: List<AccountEntity> = emptyList(),
    val liabilityAccounts: List<AccountEntity> = emptyList(),
    val debts: List<DebtEntity> = emptyList(),
    val assetTotal: Double = 0.0,
    val liabilityTotal: Double = 0.0,
    val netWorth: Double = 0.0,
    val trend: List<NetWorthTrendPoint> = emptyList()
)

@HiltViewModel
class NetWorthViewModel @Inject constructor(
    private val financeRepository: FinanceRepository,
    settingsRepository: SettingsRepository
) : ViewModel() {

    val uiState: StateFlow<NetWorthUiState> = combine(
        financeRepository.observeAccounts(),
        financeRepository.observeDebts(),
        financeRepository.observeNetWorthSnapshots(),
        settingsRepository.preferencesFlow
    ) { accounts, debts, snapshots, preferences ->
        val activeAccounts = accounts.filter { !it.isArchived && it.includeInNetWorth }
        val assetAccounts = activeAccounts
            .filter { it.isAssetLike() }
            .sortedByDescending { it.currentBalance }
        val liabilityAccounts = activeAccounts
            .filter { it.isLiabilityLike() }
            .sortedByDescending { abs(it.currentBalance) }
        val assetTotal = assetAccounts.sumOf { it.currentBalance.coerceAtLeast(0.0) }
        val liabilityAccountsTotal = liabilityAccounts.sumOf { abs(it.currentBalance) }
        val debtTotal = debts.sumOf { it.remainingAmount.coerceAtLeast(0.0) }
        val totalLiabilities = liabilityAccountsTotal + debtTotal

        NetWorthUiState(
            currency = preferences.currency,
            privacyModeEnabled = preferences.privacyModeEnabled,
            assetAccounts = assetAccounts,
            liabilityAccounts = liabilityAccounts,
            debts = debts.sortedWith(
                compareBy<DebtEntity> { it.dueDate ?: LocalDate.MAX }
                    .thenByDescending { it.updatedAt }
            ),
            assetTotal = assetTotal,
            liabilityTotal = totalLiabilities,
            netWorth = assetTotal - totalLiabilities,
            trend = snapshots
                .sortedBy { it.snapshotDate }
                .takeLast(6)
                .map {
                    NetWorthTrendPoint(
                        dateLabel = DateUtils.formatDate(it.snapshotDate),
                        netWorth = it.netWorth
                    )
                }
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = NetWorthUiState()
    )

    fun saveTrackedAccount(
        existingAccount: AccountEntity?,
        name: String,
        balance: String,
        kind: NetWorthEntryKind,
        accountType: AccountType,
        institution: String,
        includeInNetWorth: Boolean
    ) {
        val parsedBalance = balance.toDoubleOrNull() ?: return
        if (name.isBlank() || parsedBalance <= 0.0) return

        val normalizedBalance = if (kind == NetWorthEntryKind.LIABILITY) {
            -abs(parsedBalance)
        } else {
            abs(parsedBalance)
        }

        val account = (existingAccount ?: AccountEntity(
            name = name.trim(),
            accountType = accountType,
            currentBalance = normalizedBalance,
            institution = institution.trim(),
            includeInNetWorth = includeInNetWorth
        )).copy(
            name = name.trim(),
            accountType = accountType,
            currentBalance = normalizedBalance,
            institution = institution.trim(),
            includeInNetWorth = includeInNetWorth
        )

        viewModelScope.launch {
            financeRepository.saveAccount(account)
        }
    }

    fun deleteTrackedAccount(account: AccountEntity) {
        if (account.institution == FinanceRepository.SYSTEM_BALANCE_ACCOUNT_MARKER) return
        viewModelScope.launch {
            financeRepository.deleteAccount(account)
        }
    }
}

private fun AccountEntity.isLiabilityLike(): Boolean {
    return accountType == AccountType.CREDIT || currentBalance < 0.0
}

private fun AccountEntity.isAssetLike(): Boolean = !isLiabilityLike()
