package com.talent.monthlybudgetkeeper.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.content.Context
import android.net.Uri
import com.talent.monthlybudgetkeeper.data.model.AppPreferences
import com.talent.monthlybudgetkeeper.data.model.BackupPayload
import com.talent.monthlybudgetkeeper.data.model.CloudUserSettingsRow
import com.talent.monthlybudgetkeeper.data.model.CurrencyOption
import com.talent.monthlybudgetkeeper.data.model.RegionOption
import com.talent.monthlybudgetkeeper.data.model.WeekStartOption
import com.talent.monthlybudgetkeeper.data.notifications.NotificationCoordinator
import com.talent.monthlybudgetkeeper.data.notifications.ReminderScheduler
import com.talent.monthlybudgetkeeper.data.repository.BudgetRepository
import com.talent.monthlybudgetkeeper.data.repository.CloudSyncRepository
import com.talent.monthlybudgetkeeper.data.repository.FinanceRepository
import com.talent.monthlybudgetkeeper.data.repository.SettingsRepository
import com.talent.monthlybudgetkeeper.data.repository.TransactionRepository
import com.talent.monthlybudgetkeeper.data.security.AppLockController
import com.talent.monthlybudgetkeeper.data.model.toCloudRow
import com.talent.monthlybudgetkeeper.data.model.toLocalEntity
import com.talent.monthlybudgetkeeper.data.model.toLocalPreferences
import com.talent.monthlybudgetkeeper.utils.PinSecurity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import com.talent.monthlybudgetkeeper.utils.JsonBackupManager
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val transactionRepository: TransactionRepository,
    private val budgetRepository: BudgetRepository,
    private val financeRepository: FinanceRepository,
    private val cloudSyncRepository: CloudSyncRepository,
    private val appLockController: AppLockController,
    private val notificationCoordinator: NotificationCoordinator,
    private val reminderScheduler: ReminderScheduler
) : ViewModel() {

    val preferences: StateFlow<AppPreferences> = settingsRepository.preferencesFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = AppPreferences()
    )

    private val _resetCompleted = MutableSharedFlow<Unit>()
    val resetCompleted = _resetCompleted.asSharedFlow()

    private val _messages = MutableSharedFlow<String>()
    val messages = _messages.asSharedFlow()

    fun updateCurrency(currency: CurrencyOption) {
        viewModelScope.launch {
            settingsRepository.updateCurrency(currency)
        }
    }

    fun updateDarkMode(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateDarkMode(enabled)
        }
    }

    fun updateRegion(region: RegionOption) {
        viewModelScope.launch {
            settingsRepository.updateRegion(region)
        }
    }

    fun updateProfileName(name: String) {
        viewModelScope.launch {
            settingsRepository.updateProfileName(name)
        }
    }

    fun updateWeekStart(option: WeekStartOption) {
        viewModelScope.launch {
            settingsRepository.updateWeekStart(option)
        }
    }

    fun updateNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateNotificationsEnabled(enabled)
            if (enabled) {
                reminderScheduler.requestImmediateRefresh()
            } else {
                notificationCoordinator.cancelAllReminderNotifications()
            }
        }
    }

    fun updateBillRemindersEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateBillRemindersEnabled(enabled)
            if (preferences.value.notificationsEnabled) {
                reminderScheduler.requestImmediateRefresh()
            } else {
                notificationCoordinator.cancelAllReminderNotifications()
            }
        }
    }

    fun updateRecurringRemindersEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateRecurringRemindersEnabled(enabled)
            if (preferences.value.notificationsEnabled) {
                reminderScheduler.requestImmediateRefresh()
            } else {
                notificationCoordinator.cancelAllReminderNotifications()
            }
        }
    }

    fun updateDebtRemindersEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateDebtRemindersEnabled(enabled)
            if (preferences.value.notificationsEnabled) {
                reminderScheduler.requestImmediateRefresh()
            } else {
                notificationCoordinator.cancelAllReminderNotifications()
            }
        }
    }

    fun updateBudgetAlertsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateBudgetAlertsEnabled(enabled)
            if (preferences.value.notificationsEnabled) {
                reminderScheduler.requestImmediateRefresh()
            } else {
                notificationCoordinator.cancelAllReminderNotifications()
            }
        }
    }

    fun updateGoalRemindersEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateGoalRemindersEnabled(enabled)
            if (preferences.value.notificationsEnabled) {
                reminderScheduler.requestImmediateRefresh()
            } else {
                notificationCoordinator.cancelAllReminderNotifications()
            }
        }
    }

    fun updateReminderLeadDays(days: Int) {
        viewModelScope.launch {
            settingsRepository.updateReminderLeadDays(days)
            if (preferences.value.notificationsEnabled) {
                reminderScheduler.requestImmediateRefresh()
            }
        }
    }

    fun updatePrivacyModeEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.updatePrivacyModeEnabled(enabled)
        }
    }

    fun updateBiometricLockEnabled(enabled: Boolean) {
        viewModelScope.launch {
            if (preferences.value.pinHash.isNullOrBlank()) {
                _messages.emit("Set a PIN before enabling biometric unlock.")
            } else {
                settingsRepository.updateBiometricLockEnabled(enabled)
            }
        }
    }

    fun savePin(pin: String, confirmation: String) {
        viewModelScope.launch {
            val normalized = PinSecurity.normalizePin(pin)
            when {
                !PinSecurity.isValidPin(normalized) -> _messages.emit("PIN must be 4 to 8 digits.")
                normalized != PinSecurity.normalizePin(confirmation) -> _messages.emit("PIN confirmation does not match.")
                else -> {
                    settingsRepository.updatePinHash(PinSecurity.hashPin(normalized))
                    _messages.emit("App lock PIN saved.")
                }
            }
        }
    }

    fun clearPin() {
        viewModelScope.launch {
            settingsRepository.updatePinHash(null)
            _messages.emit("App lock disabled.")
        }
    }

    fun lockAppNow() {
        appLockController.requestLock()
    }

    fun sendTestNotification() {
        viewModelScope.launch {
            runCatching {
                check(notificationCoordinator.canSendNotifications()) {
                    "Allow notifications first, then try the test notification again."
                }
                notificationCoordinator.sendTestNotification()
            }.onSuccess {
                _messages.emit("Test notification sent.")
            }.onFailure {
                _messages.emit(it.message ?: "Unable to send a test notification.")
            }
        }
    }

    fun resetLocalCache() {
        viewModelScope.launch {
            runCatching {
                cloudSyncRepository.resetLocalCacheOnly().getOrThrow()
            }.onSuccess {
                _messages.emit("Local cache cleared. Your cloud data can restore on the next sync.")
            }.onFailure {
                _messages.emit(it.message ?: "Unable to reset local cache right now.")
            }
        }
    }

    fun resetAllDataEverywhere() {
        deleteAccountAndData()
    }

    fun deleteAccountAndData() {
        viewModelScope.launch {
            runCatching {
                cloudSyncRepository.resetUserDataEverywhere().getOrThrow()
            }.onSuccess {
                _messages.emit("Your account data was deleted from this device and the cloud. Signing out now.")
                _resetCompleted.emit(Unit)
            }.onFailure {
                _messages.emit(it.message ?: "Unable to delete your cloud and local data right now.")
            }
        }
    }

    fun exportBackup(context: Context, uri: Uri) {
        viewModelScope.launch {
            runCatching {
                val preferences = settingsRepository.getCurrentPreferences()
                val backupUserId = preferences.userId ?: "local_backup"
                val payload = BackupPayload(
                    exportedAt = System.currentTimeMillis(),
                    backupUserId = preferences.userId,
                    preferences = preferences.toCloudRow(backupUserId),
                    transactions = transactionRepository.getAllTransactions().map { it.toCloudRow(backupUserId) },
                    monthlyBudgets = budgetRepository.getAllMonthlyBudgets().map { it.toCloudRow(backupUserId) },
                    categoryBudgets = budgetRepository.getAllCategoryBudgets().map { it.toCloudRow(backupUserId) },
                    accounts = financeRepository.getAllAccounts().map { it.toCloudRow(backupUserId) },
                    transfers = financeRepository.getAllTransfers().map { it.toCloudRow(backupUserId) },
                    recurringItems = financeRepository.getAllRecurringItems().map { it.toCloudRow(backupUserId) },
                    subscriptionBills = financeRepository.getAllSubscriptionBills().map { it.toCloudRow(backupUserId) },
                    goals = financeRepository.getAllGoals().map { it.toCloudRow(backupUserId) },
                    debts = financeRepository.getAllDebts().map { it.toCloudRow(backupUserId) },
                    envelopeBudgets = financeRepository.getAllEnvelopeBudgets().map { it.toCloudRow(backupUserId) },
                    paycheckPlans = financeRepository.getAllPaycheckPlans().map { it.toCloudRow(backupUserId) },
                    splitTransactions = financeRepository.getAllSplitTransactions().map { it.toCloudRow(backupUserId) },
                    receiptAttachments = financeRepository.getAllReceiptAttachments().map { it.toCloudRow(backupUserId) },
                    transactionRules = financeRepository.getAllTransactionRules().map { it.toCloudRow(backupUserId) }
                )
                JsonBackupManager.exportBackup(context, uri, payload)
            }.onSuccess {
                _messages.emit("Backup exported as JSON.")
            }.onFailure {
                _messages.emit("Backup export failed.")
            }
        }
    }

    fun restoreBackup(context: Context, uri: Uri) {
        viewModelScope.launch {
            runCatching {
                val payload = JsonBackupManager.importBackup(context, uri)
                val currentPreferences = settingsRepository.getCurrentPreferences()
                val currentUserId = currentPreferences.userId
                val backupUserId = payload.backupUserId
                require(
                    currentUserId == null ||
                        backupUserId == null ||
                        backupUserId == currentUserId ||
                        backupUserId == "local_backup"
                ) {
                    "This backup belongs to a different signed-in account."
                }

                val targetUserId = currentUserId ?: payload.backupUserId?.takeUnless { it == "local_backup" }
                val restoredPreferences = payload.preferences.toLocalPreferences().copy(
                    userId = targetUserId,
                    appLockEnabled = currentPreferences.appLockEnabled,
                    biometricLockEnabled = currentPreferences.biometricLockEnabled,
                    pinHash = currentPreferences.pinHash
                )

                transactionRepository.replaceAllTransactions(
                    payload.transactions.map { it.toLocalEntity().copy(userId = targetUserId) }
                )
                budgetRepository.replaceAllBudgets(
                    monthlyBudgets = payload.monthlyBudgets.map { it.toLocalEntity().copy(userId = targetUserId) },
                    categoryBudgets = payload.categoryBudgets.map { it.toLocalEntity().copy(userId = targetUserId) }
                )
                financeRepository.replaceAllFinanceData(
                    accounts = payload.accounts.map { it.toLocalEntity().copy(userId = targetUserId) },
                    transfers = payload.transfers.map { it.toLocalEntity().copy(userId = targetUserId) },
                    recurringItems = payload.recurringItems.map { it.toLocalEntity().copy(userId = targetUserId) },
                    subscriptionBills = payload.subscriptionBills.map { it.toLocalEntity().copy(userId = targetUserId) },
                    goals = payload.goals.map { it.toLocalEntity().copy(userId = targetUserId) },
                    debts = payload.debts.map { it.toLocalEntity().copy(userId = targetUserId) },
                    envelopeBudgets = payload.envelopeBudgets.map { it.toLocalEntity().copy(userId = targetUserId) },
                    paycheckPlans = payload.paycheckPlans.map { it.toLocalEntity().copy(userId = targetUserId) },
                    splitTransactions = payload.splitTransactions.map { it.toLocalEntity().copy(userId = targetUserId) },
                    receiptAttachments = payload.receiptAttachments.map { it.toLocalEntity().copy(userId = targetUserId) },
                    transactionRules = payload.transactionRules.map { it.toLocalEntity().copy(userId = targetUserId) }
                )
                settingsRepository.applyPreferences(restoredPreferences)
            }.onSuccess {
                _messages.emit("Backup restored successfully.")
            }.onFailure {
                _messages.emit(it.message ?: "Restore failed. Please choose a valid backup file.")
            }
        }
    }
}
