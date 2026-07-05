package com.talent.monthlybudgetkeeper.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.talent.monthlybudgetkeeper.data.auth.SessionManager
import com.talent.monthlybudgetkeeper.data.model.AppPreferences
import com.talent.monthlybudgetkeeper.data.model.BudgetCycleType
import com.talent.monthlybudgetkeeper.data.model.CurrencyOption
import com.talent.monthlybudgetkeeper.data.model.RegionOption
import com.talent.monthlybudgetkeeper.data.notifications.ReminderScheduler
import com.talent.monthlybudgetkeeper.data.repository.FinanceRepository
import com.talent.monthlybudgetkeeper.data.repository.SettingsRepository
import com.talent.monthlybudgetkeeper.data.repository.TransactionRepository
import com.talent.monthlybudgetkeeper.data.repository.UserSetupData
import com.talent.monthlybudgetkeeper.data.repository.UserProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SetupUiState(
    val preferences: AppPreferences = AppPreferences(),
    val isSaving: Boolean = false
)

@HiltViewModel
class SetupViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val sessionManager: SessionManager,
    private val userProfileRepository: UserProfileRepository,
    private val reminderScheduler: ReminderScheduler,
    private val transactionRepository: TransactionRepository,
    private val financeRepository: FinanceRepository
) : ViewModel() {

    val preferences: StateFlow<AppPreferences> = settingsRepository.preferencesFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = AppPreferences()
        )

    private val _completed = MutableSharedFlow<Unit>()
    val completed = _completed.asSharedFlow()

    private val _messages = MutableSharedFlow<String>()
    val messages = _messages.asSharedFlow()

    private val _uiState = MutableStateFlow(SetupUiState())
    val uiState: StateFlow<SetupUiState> = _uiState

    init {
        viewModelScope.launch {
            preferences.collect { currentPreferences ->
                _uiState.value = _uiState.value.copy(preferences = currentPreferences)
            }
        }
    }

    fun completeSetup(
        region: RegionOption,
        currency: CurrencyOption,
        monthlyIncome: String,
        monthlyBudgetTarget: String,
        mainFinancialGoal: String,
        cycleType: BudgetCycleType,
        nextCycleDate: LocalDate,
        carryForwardRemainingBudget: Boolean,
        notificationsEnabled: Boolean,
        privacyModeEnabled: Boolean
    ) {
        val parsedIncome = monthlyIncome.toDoubleOrNull()
        val parsedBudgetTarget = monthlyBudgetTarget.toDoubleOrNull()
        val cycleStartDate = when {
            nextCycleDate.isAfter(LocalDate.now()) -> {
                when (cycleType) {
                    BudgetCycleType.WEEKLY -> nextCycleDate.minusWeeks(1)
                    BudgetCycleType.MONTHLY -> nextCycleDate.minusMonths(1)
                    BudgetCycleType.YEARLY -> nextCycleDate.minusYears(1)
                }
            }
            else -> nextCycleDate
        }
        when {
            parsedIncome == null || parsedIncome <= 0.0 -> emitMessage("Enter a valid monthly income.")
            parsedBudgetTarget == null || parsedBudgetTarget <= 0.0 -> {
                emitMessage("Enter a valid monthly budget target.")
            }
            mainFinancialGoal.isBlank() -> emitMessage("Choose a main financial goal.")
            !nextCycleDate.isAfter(LocalDate.now()) -> emitMessage("Choose a future cycle restart date.")
            else -> {
                viewModelScope.launch {
                    _uiState.value = _uiState.value.copy(isSaving = true)
                    runCatching {
                        val currentUser = sessionManager.currentUserOrNull()
                            ?: error("Your session expired. Please sign in again.")
                        val settings = userProfileRepository.saveCompletedUserSetup(
                            userId = currentUser.id,
                            setupData = UserSetupData(
                                region = region,
                                currency = currency,
                                monthlyIncome = parsedIncome,
                                monthlyBudgetTarget = parsedBudgetTarget,
                                mainFinancialGoal = mainFinancialGoal,
                                cycleType = cycleType,
                                cycleStartDate = cycleStartDate,
                                nextCycleDate = nextCycleDate,
                                carryForwardRemainingBudget = carryForwardRemainingBudget,
                                notificationsEnabled = notificationsEnabled,
                                privacyModeEnabled = privacyModeEnabled
                            )
                        )
                        userProfileRepository.syncUserSettingsToLocal(
                            settings = settings,
                            preserveSetupResetPending = false
                        )
                        financeRepository.ensurePrimaryBalanceAccount(currencyCode = currency.code)
                        transactionRepository.ensureCycleOpeningIncome(
                            cycleStartDate = cycleStartDate,
                            amount = parsedIncome
                        )
                        if (notificationsEnabled) {
                            reminderScheduler.requestImmediateRefresh()
                        }
                    }.onSuccess {
                        _completed.emit(Unit)
                    }.onFailure { throwable ->
                        emitMessage(throwable.message ?: "We couldn't save your setup right now.")
                    }
                    _uiState.value = _uiState.value.copy(isSaving = false)
                }
            }
        }
    }

    private fun emitMessage(message: String) {
        viewModelScope.launch {
            _messages.emit(message)
        }
    }
}
