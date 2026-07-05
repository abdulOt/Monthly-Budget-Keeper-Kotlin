package com.talent.monthlybudgetkeeper.data.repository

import android.content.Context
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.talent.monthlybudgetkeeper.data.model.AppPreferences
import com.talent.monthlybudgetkeeper.data.model.BudgetCycleType
import com.talent.monthlybudgetkeeper.data.model.CloudUserSettingsRow
import com.talent.monthlybudgetkeeper.data.model.CurrencyOption
import com.talent.monthlybudgetkeeper.data.notifications.NotificationEventStore
import com.talent.monthlybudgetkeeper.data.notifications.NotificationPermissionManager
import com.talent.monthlybudgetkeeper.data.model.RegionOption
import com.talent.monthlybudgetkeeper.data.model.WeekStartOption
import com.talent.monthlybudgetkeeper.data.model.toLocalPreferences
import com.talent.monthlybudgetkeeper.data.security.SecurePinStore
import com.talent.monthlybudgetkeeper.data.sync.SyncSignalBus
import com.talent.monthlybudgetkeeper.utils.DateUtils
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch

private val Context.dataStore by preferencesDataStore(name = "monthly_budget_keeper_preferences")

@Singleton
class SettingsRepository @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val syncSignalBus: SyncSignalBus,
    private val securePinStore: SecurePinStore
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private object GlobalKeys {
        val activeUserId = stringPreferencesKey("active_user_id")
        val onboardingCompleted = booleanPreferencesKey("onboarding_completed")
        val migratedToUserScopedStorage = booleanPreferencesKey("migrated_to_user_scoped_storage")
    }

    private object LegacyKeys {
        val id = stringPreferencesKey("preferences_id")
        val userId = stringPreferencesKey("preferences_user_id")
        val setupCompleted = booleanPreferencesKey("setup_completed")
        val currency = stringPreferencesKey("currency")
        val region = stringPreferencesKey("region")
        val monthlyIncome = stringPreferencesKey("monthly_income")
        val monthlyBudgetTarget = stringPreferencesKey("monthly_budget_target")
        val mainFinancialGoal = stringPreferencesKey("main_financial_goal")
        val darkMode = booleanPreferencesKey("dark_mode")
        val profileName = stringPreferencesKey("profile_name")
        val weekStart = stringPreferencesKey("week_start")
        val notificationsEnabled = booleanPreferencesKey("notifications_enabled")
        val billRemindersEnabled = booleanPreferencesKey("bill_reminders_enabled")
        val recurringRemindersEnabled = booleanPreferencesKey("recurring_reminders_enabled")
        val debtRemindersEnabled = booleanPreferencesKey("debt_reminders_enabled")
        val budgetAlertsEnabled = booleanPreferencesKey("budget_alerts_enabled")
        val goalRemindersEnabled = booleanPreferencesKey("goal_reminders_enabled")
        val reminderLeadDays = stringPreferencesKey("reminder_lead_days")
        val privacyModeEnabled = booleanPreferencesKey("privacy_mode_enabled")
        val appLockEnabled = booleanPreferencesKey("app_lock_enabled")
        val biometricLockEnabled = booleanPreferencesKey("biometric_lock_enabled")
        val legacyPinHash = stringPreferencesKey("pin_hash")
        val createdAt = stringPreferencesKey("preferences_created_at")
        val updatedAt = stringPreferencesKey("preferences_updated_at")
    }

    private object UserField {
        const val setupCompleted = "setup_completed"
        const val setupResetPending = "setup_reset_pending"
        const val currency = "currency"
        const val region = "region"
        const val monthlyIncome = "monthly_income"
        const val monthlyBudgetTarget = "monthly_budget_target"
        const val mainFinancialGoal = "main_financial_goal"
        const val cycleType = "cycle_type"
        const val cycleStartDate = "cycle_start_date"
        const val nextCycleDate = "next_cycle_date"
        const val carryForwardRemainingBudget = "carry_forward_remaining_budget"
        const val darkMode = "dark_mode"
        const val profileName = "profile_name"
        const val weekStart = "week_start"
        const val notificationsEnabled = "notifications_enabled"
        const val billRemindersEnabled = "bill_reminders_enabled"
        const val recurringRemindersEnabled = "recurring_reminders_enabled"
        const val debtRemindersEnabled = "debt_reminders_enabled"
        const val budgetAlertsEnabled = "budget_alerts_enabled"
        const val goalRemindersEnabled = "goal_reminders_enabled"
        const val reminderLeadDays = "reminder_lead_days"
        const val privacyModeEnabled = "privacy_mode_enabled"
        const val appLockEnabled = "app_lock_enabled"
        const val biometricLockEnabled = "biometric_lock_enabled"
        const val createdAt = "created_at"
        const val updatedAt = "updated_at"
    }

    val preferencesFlow: Flow<AppPreferences> = combine(
        context.dataStore.data,
        securePinStore.pinHashFlow
    ) { prefs, pinHash ->
        prefs.toAppPreferences(
            activeUserId = prefs[GlobalKeys.activeUserId],
            pinHash = pinHash
        )
    }

    val preferencesReadyFlow: Flow<Boolean> = context.dataStore.data
        .map { true }
        .onStart { emit(false) }
        .distinctUntilChanged()

    init {
        scope.launch {
            migrateLegacyStorageIfNeeded()
            val activeUserId = context.dataStore.data.first()[GlobalKeys.activeUserId]
            securePinStore.bindUser(activeUserId)
        }
    }

    suspend fun completeOnboarding() {
        context.dataStore.edit { it[GlobalKeys.onboardingCompleted] = true }
    }

    suspend fun completePostLoginSetup(
        region: RegionOption,
        currency: CurrencyOption,
        monthlyIncome: Double,
        monthlyBudgetTarget: Double,
        mainFinancialGoal: String,
        notificationsEnabled: Boolean,
        privacyModeEnabled: Boolean
    ) {
        touchUserPreferences { prefs, userId ->
            prefs[userBooleanKey(userId, UserField.setupCompleted)] = true
            prefs[userBooleanKey(userId, UserField.setupResetPending)] = false
            prefs[userStringKey(userId, UserField.region)] = region.label
            prefs[userStringKey(userId, UserField.currency)] = currency.code
            prefs[userStringKey(userId, UserField.monthlyIncome)] = monthlyIncome.coerceAtLeast(0.0).toString()
            prefs[userStringKey(userId, UserField.monthlyBudgetTarget)] = monthlyBudgetTarget.coerceAtLeast(0.0).toString()
            prefs[userStringKey(userId, UserField.mainFinancialGoal)] = mainFinancialGoal.trim().ifBlank { "Track spending" }
            prefs[userBooleanKey(userId, UserField.notificationsEnabled)] = notificationsEnabled
            prefs[userBooleanKey(userId, UserField.privacyModeEnabled)] = privacyModeEnabled
        }
    }

    suspend fun updateCurrency(currency: CurrencyOption) {
        touchUserPreferences { prefs, userId ->
            prefs[userStringKey(userId, UserField.currency)] = currency.code
        }
        syncSignalBus.notifyDataChanged()
    }

    suspend fun updateDarkMode(enabled: Boolean) {
        touchUserPreferences { prefs, userId ->
            prefs[userBooleanKey(userId, UserField.darkMode)] = enabled
        }
    }

    suspend fun updateRegion(region: RegionOption) {
        touchUserPreferences { prefs, userId ->
            prefs[userStringKey(userId, UserField.region)] = region.label
        }
        syncSignalBus.notifyDataChanged()
    }

    suspend fun updateProfileName(name: String, notifySync: Boolean = true) {
        touchUserPreferences { prefs, userId ->
            prefs[userStringKey(userId, UserField.profileName)] = name.trim().ifBlank { "My Budget Profile" }
        }
        if (notifySync) {
            syncSignalBus.notifyDataChanged()
        }
    }

    suspend fun updateBudgetCycle(
        cycleType: BudgetCycleType,
        cycleStartDate: LocalDate,
        nextCycleDate: LocalDate,
        carryForwardRemainingBudget: Boolean,
        notifySync: Boolean = true
    ) {
        touchUserPreferences { prefs, userId ->
            prefs[userStringKey(userId, UserField.cycleType)] = cycleType.name
            prefs[userStringKey(userId, UserField.cycleStartDate)] = cycleStartDate.toString()
            prefs[userStringKey(userId, UserField.nextCycleDate)] = nextCycleDate.toString()
            prefs[userBooleanKey(userId, UserField.carryForwardRemainingBudget)] = carryForwardRemainingBudget
        }
        if (notifySync) {
            syncSignalBus.notifyDataChanged()
        }
    }

    suspend fun continueBudgetCycleWithPreviousValues(today: LocalDate = LocalDate.now()) {
        val current = getCurrentPreferences()
        val currentNextCycleDate = current.nextCycleDate.takeIf { !it.isBefore(today) } ?: today
        val upcomingStart = if (today.isAfter(currentNextCycleDate)) today else currentNextCycleDate
        val followingDate = DateUtils.advanceCycleDate(upcomingStart, current.cycleType)
        updateBudgetCycle(
            cycleType = current.cycleType,
            cycleStartDate = upcomingStart,
            nextCycleDate = followingDate,
            carryForwardRemainingBudget = current.carryForwardRemainingBudget
        )
    }

    suspend fun renewBudgetCycleWithNewValues(
        income: Double,
        budgetTarget: Double,
        today: LocalDate = LocalDate.now()
    ) {
        val current = getCurrentPreferences()
        val currentNextCycleDate = current.nextCycleDate.takeIf { !it.isBefore(today) } ?: today
        val upcomingStart = if (today.isAfter(currentNextCycleDate)) today else currentNextCycleDate
        val followingDate = DateUtils.advanceCycleDate(upcomingStart, current.cycleType)
        touchUserPreferences { prefs, userId ->
            prefs[userStringKey(userId, UserField.monthlyIncome)] = income.coerceAtLeast(0.0).toString()
            prefs[userStringKey(userId, UserField.monthlyBudgetTarget)] = budgetTarget.coerceAtLeast(0.0).toString()
            prefs[userStringKey(userId, UserField.cycleType)] = current.cycleType.name
            prefs[userStringKey(userId, UserField.cycleStartDate)] = upcomingStart.toString()
            prefs[userStringKey(userId, UserField.nextCycleDate)] = followingDate.toString()
            prefs[userBooleanKey(userId, UserField.carryForwardRemainingBudget)] = current.carryForwardRemainingBudget
        }
        syncSignalBus.notifyDataChanged()
    }

    suspend fun updateWeekStart(option: WeekStartOption) {
        touchUserPreferences { prefs, userId ->
            prefs[userStringKey(userId, UserField.weekStart)] = option.name
        }
    }

    suspend fun updateNotificationsEnabled(enabled: Boolean) {
        touchUserPreferences { prefs, userId ->
            prefs[userBooleanKey(userId, UserField.notificationsEnabled)] = enabled
        }
        syncSignalBus.notifyDataChanged()
    }

    suspend fun updateBillRemindersEnabled(enabled: Boolean) {
        touchUserPreferences { prefs, userId ->
            prefs[userBooleanKey(userId, UserField.billRemindersEnabled)] = enabled
        }
    }

    suspend fun updateRecurringRemindersEnabled(enabled: Boolean) {
        touchUserPreferences { prefs, userId ->
            prefs[userBooleanKey(userId, UserField.recurringRemindersEnabled)] = enabled
        }
    }

    suspend fun updateDebtRemindersEnabled(enabled: Boolean) {
        touchUserPreferences { prefs, userId ->
            prefs[userBooleanKey(userId, UserField.debtRemindersEnabled)] = enabled
        }
    }

    suspend fun updateBudgetAlertsEnabled(enabled: Boolean) {
        touchUserPreferences { prefs, userId ->
            prefs[userBooleanKey(userId, UserField.budgetAlertsEnabled)] = enabled
        }
    }

    suspend fun updateGoalRemindersEnabled(enabled: Boolean) {
        touchUserPreferences { prefs, userId ->
            prefs[userBooleanKey(userId, UserField.goalRemindersEnabled)] = enabled
        }
    }

    suspend fun updateReminderLeadDays(days: Int) {
        val normalized = days.coerceIn(1, 14)
        touchUserPreferences { prefs, userId ->
            prefs[userStringKey(userId, UserField.reminderLeadDays)] = normalized.toString()
        }
    }

    suspend fun updatePrivacyModeEnabled(enabled: Boolean) {
        touchUserPreferences { prefs, userId ->
            prefs[userBooleanKey(userId, UserField.privacyModeEnabled)] = enabled
        }
    }

    suspend fun updateAppLockEnabled(enabled: Boolean) {
        touchUserPreferences { prefs, userId ->
            prefs[userBooleanKey(userId, UserField.appLockEnabled)] = enabled
            if (!enabled) {
                prefs[userBooleanKey(userId, UserField.biometricLockEnabled)] = false
            }
        }
    }

    suspend fun updateBiometricLockEnabled(enabled: Boolean) {
        touchUserPreferences { prefs, userId ->
            prefs[userBooleanKey(userId, UserField.biometricLockEnabled)] =
                enabled && !securePinStore.currentPinHash().isNullOrBlank()
        }
    }

    suspend fun updatePinHash(hash: String?) {
        val activeUserId = context.dataStore.data.first()[GlobalKeys.activeUserId]
        securePinStore.savePinHash(hash)
        if (activeUserId.isNullOrBlank()) return

        touchUserPreferences { prefs, userId ->
            if (hash.isNullOrBlank()) {
                prefs[userBooleanKey(userId, UserField.appLockEnabled)] = false
                prefs[userBooleanKey(userId, UserField.biometricLockEnabled)] = false
            } else {
                prefs[userBooleanKey(userId, UserField.appLockEnabled)] = true
            }
        }
    }

    suspend fun bindUser(userId: String?) {
        securePinStore.bindUser(userId)
        context.dataStore.edit { prefs ->
            if (userId.isNullOrBlank()) {
                prefs.remove(GlobalKeys.activeUserId)
            } else {
                prefs[GlobalKeys.activeUserId] = userId
                ensureUserPreferenceDefaults(prefs, userId)
            }
        }
    }

    suspend fun resetPreferences() {
        securePinStore.clearAllPins()
        context.dataStore.edit { preferences ->
            preferences.clear()
        }
    }

    suspend fun resetUserScopedPreferences() {
        securePinStore.bindUser(null)
        context.dataStore.edit { prefs ->
            prefs.remove(GlobalKeys.activeUserId)
        }
    }

    suspend fun clearUserScopedPreferences(userId: String, clearPin: Boolean = true) {
        context.dataStore.edit { prefs ->
            prefs.remove(userBooleanKey(userId, UserField.setupCompleted))
            prefs.remove(userBooleanKey(userId, UserField.setupResetPending))
            prefs.remove(userStringKey(userId, UserField.currency))
            prefs.remove(userStringKey(userId, UserField.region))
            prefs.remove(userStringKey(userId, UserField.monthlyIncome))
            prefs.remove(userStringKey(userId, UserField.monthlyBudgetTarget))
            prefs.remove(userStringKey(userId, UserField.mainFinancialGoal))
            prefs.remove(userStringKey(userId, UserField.cycleType))
            prefs.remove(userStringKey(userId, UserField.cycleStartDate))
            prefs.remove(userStringKey(userId, UserField.nextCycleDate))
            prefs.remove(userBooleanKey(userId, UserField.carryForwardRemainingBudget))
            prefs.remove(userBooleanKey(userId, UserField.darkMode))
            prefs.remove(userStringKey(userId, UserField.profileName))
            prefs.remove(userStringKey(userId, UserField.weekStart))
            prefs.remove(userBooleanKey(userId, UserField.notificationsEnabled))
            prefs.remove(userBooleanKey(userId, UserField.billRemindersEnabled))
            prefs.remove(userBooleanKey(userId, UserField.recurringRemindersEnabled))
            prefs.remove(userBooleanKey(userId, UserField.debtRemindersEnabled))
            prefs.remove(userBooleanKey(userId, UserField.budgetAlertsEnabled))
            prefs.remove(userBooleanKey(userId, UserField.goalRemindersEnabled))
            prefs.remove(userStringKey(userId, UserField.reminderLeadDays))
            prefs.remove(userBooleanKey(userId, UserField.privacyModeEnabled))
            prefs.remove(userBooleanKey(userId, UserField.appLockEnabled))
            prefs.remove(userBooleanKey(userId, UserField.biometricLockEnabled))
            prefs.remove(userStringKey(userId, UserField.createdAt))
            prefs.remove(userStringKey(userId, UserField.updatedAt))
        }
        NotificationPermissionManager.clearPromptState(context, userId)
        NotificationEventStore.clearUserEvents(context, userId)
        if (clearPin) {
            securePinStore.savePinHashForUser(userId, null)
        }
    }

    suspend fun prepareForSetupReentry() {
        touchUserPreferences { prefs, userId ->
            prefs[userBooleanKey(userId, UserField.setupCompleted)] = false
            prefs[userBooleanKey(userId, UserField.setupResetPending)] = true
            prefs[userStringKey(userId, UserField.monthlyIncome)] = "0.0"
            prefs[userStringKey(userId, UserField.monthlyBudgetTarget)] = "0.0"
            prefs[userStringKey(userId, UserField.mainFinancialGoal)] = "Track spending"
            prefs[userStringKey(userId, UserField.cycleType)] = BudgetCycleType.MONTHLY.name
            prefs[userStringKey(userId, UserField.cycleStartDate)] = LocalDate.now().withDayOfMonth(1).toString()
            prefs[userStringKey(userId, UserField.nextCycleDate)] = LocalDate.now().withDayOfMonth(1).plusMonths(1).toString()
            prefs[userBooleanKey(userId, UserField.carryForwardRemainingBudget)] = false
            prefs[userBooleanKey(userId, UserField.privacyModeEnabled)] = false
        }
    }

    suspend fun getCurrentPreferences(): AppPreferences = preferencesFlow.first()

    suspend fun applyPreferences(preferences: AppPreferences) {
        val targetUserId = preferences.userId
        securePinStore.bindUser(targetUserId)
        context.dataStore.edit { prefs ->
            prefs[GlobalKeys.onboardingCompleted] = preferences.onboardingCompleted
            if (targetUserId.isNullOrBlank()) {
                prefs.remove(GlobalKeys.activeUserId)
            } else {
                prefs[GlobalKeys.activeUserId] = targetUserId
                writeUserPreferences(
                    prefs = prefs,
                    userId = targetUserId,
                    preferences = preferences.copy(
                        id = targetUserId,
                        userId = targetUserId
                    )
                )
            }
        }
        securePinStore.savePinHash(preferences.pinHash)
    }

    suspend fun syncUserProfileToLocal(
        profile: CloudUserSettingsRow,
        preserveSetupResetPending: Boolean = true
    ) {
        val currentPreferences = getCurrentPreferences()
        applyPreferences(
            profile.toLocalPreferences().copy(
                id = profile.userId,
                userId = profile.userId,
                onboardingCompleted = currentPreferences.onboardingCompleted,
                setupResetPending = if (preserveSetupResetPending) {
                    currentPreferences.setupResetPending
                } else {
                    false
                },
                darkMode = currentPreferences.darkMode,
                profileName = currentPreferences.profileName,
                weekStart = currentPreferences.weekStart,
                billRemindersEnabled = currentPreferences.billRemindersEnabled,
                recurringRemindersEnabled = currentPreferences.recurringRemindersEnabled,
                debtRemindersEnabled = currentPreferences.debtRemindersEnabled,
                budgetAlertsEnabled = currentPreferences.budgetAlertsEnabled,
                goalRemindersEnabled = currentPreferences.goalRemindersEnabled,
                reminderLeadDays = currentPreferences.reminderLeadDays,
                appLockEnabled = currentPreferences.appLockEnabled,
                biometricLockEnabled = currentPreferences.biometricLockEnabled,
                pinHash = currentPreferences.pinHash
            )
        )
    }

    private suspend fun migrateLegacyStorageIfNeeded() {
        var legacyUserId: String? = null
        var legacyPinHash: String? = null
        context.dataStore.edit { prefs ->
            if (prefs[GlobalKeys.migratedToUserScopedStorage] == true) {
                return@edit
            }

            legacyUserId = prefs[LegacyKeys.userId]
            legacyPinHash = prefs[LegacyKeys.legacyPinHash]

            if (!legacyUserId.isNullOrBlank() && !hasUserScopedState(prefs, legacyUserId.orEmpty())) {
                val migratedPreferences = prefs.toLegacyAppPreferences(
                    userId = legacyUserId.orEmpty(),
                    pinHash = legacyPinHash
                )
                writeUserPreferences(
                    prefs = prefs,
                    userId = legacyUserId.orEmpty(),
                    preferences = migratedPreferences.copy(
                        id = legacyUserId.orEmpty(),
                        userId = legacyUserId.orEmpty(),
                        pinHash = null
                    )
                )
                prefs[GlobalKeys.activeUserId] = legacyUserId.orEmpty()
            }

            clearLegacyPreferenceKeys(prefs)
            prefs[GlobalKeys.migratedToUserScopedStorage] = true
        }

        securePinStore.clearLegacyGlobalPin()
        if (!legacyUserId.isNullOrBlank() && !legacyPinHash.isNullOrBlank()) {
            securePinStore.savePinHashForUser(legacyUserId.orEmpty(), legacyPinHash)
        }
    }

    private fun Preferences.toAppPreferences(
        activeUserId: String?,
        pinHash: String?
    ): AppPreferences {
        val now = System.currentTimeMillis()
        val defaultCycleStart = LocalDate.now().withDayOfMonth(1)
        val defaultCycleType = BudgetCycleType.MONTHLY
        if (activeUserId.isNullOrBlank()) {
            return AppPreferences(
                onboardingCompleted = this[GlobalKeys.onboardingCompleted] ?: false,
                pinHash = null
            )
        }

        val parsedCurrency = this[userStringKey(activeUserId, UserField.currency)].toCurrencyOption()
        return AppPreferences(
            id = activeUserId,
            userId = activeUserId,
            onboardingCompleted = this[GlobalKeys.onboardingCompleted] ?: false,
            setupCompleted = this[userBooleanKey(activeUserId, UserField.setupCompleted)] ?: false,
            setupResetPending = this[userBooleanKey(activeUserId, UserField.setupResetPending)] ?: false,
            currency = parsedCurrency,
            region = RegionOption.fromNameOrDefault(
                this[userStringKey(activeUserId, UserField.region)],
                parsedCurrency
            ),
            monthlyIncome = this[userStringKey(activeUserId, UserField.monthlyIncome)]
                ?.toDoubleOrNull()
                ?.coerceAtLeast(0.0)
                ?: 0.0,
            monthlyBudgetTarget = this[userStringKey(activeUserId, UserField.monthlyBudgetTarget)]
                ?.toDoubleOrNull()
                ?.coerceAtLeast(0.0)
                ?: 0.0,
            mainFinancialGoal = this[userStringKey(activeUserId, UserField.mainFinancialGoal)]
                .orEmpty()
                .ifBlank { "Track spending" },
            cycleType = this[userStringKey(activeUserId, UserField.cycleType)].toBudgetCycleType(),
            cycleStartDate = this[userStringKey(activeUserId, UserField.cycleStartDate)]
                .toLocalDateOrDefault(defaultCycleStart),
            nextCycleDate = this[userStringKey(activeUserId, UserField.nextCycleDate)]
                .toLocalDateOrDefault(defaultCycleStart.plusMonths(1)),
            carryForwardRemainingBudget = this[userBooleanKey(activeUserId, UserField.carryForwardRemainingBudget)] ?: false,
            darkMode = this[userBooleanKey(activeUserId, UserField.darkMode)] ?: false,
            profileName = this[userStringKey(activeUserId, UserField.profileName)] ?: "My Budget Profile",
            weekStart = this[userStringKey(activeUserId, UserField.weekStart)].toWeekStartOption(),
            notificationsEnabled = this[userBooleanKey(activeUserId, UserField.notificationsEnabled)] ?: false,
            billRemindersEnabled = this[userBooleanKey(activeUserId, UserField.billRemindersEnabled)] ?: true,
            recurringRemindersEnabled = this[userBooleanKey(activeUserId, UserField.recurringRemindersEnabled)] ?: true,
            debtRemindersEnabled = this[userBooleanKey(activeUserId, UserField.debtRemindersEnabled)] ?: true,
            budgetAlertsEnabled = this[userBooleanKey(activeUserId, UserField.budgetAlertsEnabled)] ?: true,
            goalRemindersEnabled = this[userBooleanKey(activeUserId, UserField.goalRemindersEnabled)] ?: true,
            reminderLeadDays = this[userStringKey(activeUserId, UserField.reminderLeadDays)]
                ?.toIntOrNull()
                ?.coerceIn(1, 14)
                ?: 3,
            privacyModeEnabled = this[userBooleanKey(activeUserId, UserField.privacyModeEnabled)] ?: false,
            appLockEnabled = this[userBooleanKey(activeUserId, UserField.appLockEnabled)] ?: false,
            biometricLockEnabled = this[userBooleanKey(activeUserId, UserField.biometricLockEnabled)] ?: false,
            pinHash = pinHash,
            createdAt = this[userStringKey(activeUserId, UserField.createdAt)]?.toLongOrNull() ?: now,
            updatedAt = this[userStringKey(activeUserId, UserField.updatedAt)]?.toLongOrNull() ?: now
        )
    }

    private fun Preferences.toLegacyAppPreferences(
        userId: String,
        pinHash: String?
    ): AppPreferences {
        val now = System.currentTimeMillis()
        val parsedCurrency = this[LegacyKeys.currency].toCurrencyOption()
        val defaultCycleStart = LocalDate.now().withDayOfMonth(1)
        return AppPreferences(
            id = userId,
            userId = userId,
            onboardingCompleted = this[GlobalKeys.onboardingCompleted] ?: false,
            setupCompleted = this[LegacyKeys.setupCompleted] ?: false,
            setupResetPending = false,
            currency = parsedCurrency,
            region = RegionOption.fromNameOrDefault(this[LegacyKeys.region], parsedCurrency),
            monthlyIncome = this[LegacyKeys.monthlyIncome]?.toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0,
            monthlyBudgetTarget = this[LegacyKeys.monthlyBudgetTarget]?.toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0,
            mainFinancialGoal = this[LegacyKeys.mainFinancialGoal].orEmpty().ifBlank { "Track spending" },
            cycleType = BudgetCycleType.MONTHLY,
            cycleStartDate = defaultCycleStart,
            nextCycleDate = defaultCycleStart.plusMonths(1),
            carryForwardRemainingBudget = false,
            darkMode = this[LegacyKeys.darkMode] ?: false,
            profileName = this[LegacyKeys.profileName] ?: "My Budget Profile",
            weekStart = this[LegacyKeys.weekStart].toWeekStartOption(),
            notificationsEnabled = this[LegacyKeys.notificationsEnabled] ?: true,
            billRemindersEnabled = this[LegacyKeys.billRemindersEnabled] ?: true,
            recurringRemindersEnabled = this[LegacyKeys.recurringRemindersEnabled] ?: true,
            debtRemindersEnabled = this[LegacyKeys.debtRemindersEnabled] ?: true,
            budgetAlertsEnabled = this[LegacyKeys.budgetAlertsEnabled] ?: true,
            goalRemindersEnabled = this[LegacyKeys.goalRemindersEnabled] ?: true,
            reminderLeadDays = this[LegacyKeys.reminderLeadDays]?.toIntOrNull()?.coerceIn(1, 14) ?: 3,
            privacyModeEnabled = this[LegacyKeys.privacyModeEnabled] ?: false,
            appLockEnabled = this[LegacyKeys.appLockEnabled] ?: false,
            biometricLockEnabled = this[LegacyKeys.biometricLockEnabled] ?: false,
            pinHash = pinHash,
            createdAt = this[LegacyKeys.createdAt]?.toLongOrNull() ?: now,
            updatedAt = this[LegacyKeys.updatedAt]?.toLongOrNull() ?: now
        )
    }

    private suspend fun touchUserPreferences(
        update: (MutablePreferences, String) -> Unit
    ) {
        context.dataStore.edit { prefs ->
            val activeUserId = prefs[GlobalKeys.activeUserId]
            if (activeUserId.isNullOrBlank()) return@edit

            ensureUserPreferenceDefaults(prefs, activeUserId)
            update(prefs, activeUserId)
            prefs[userStringKey(activeUserId, UserField.updatedAt)] = System.currentTimeMillis().toString()
        }
    }

    private fun ensureUserPreferenceDefaults(
        prefs: MutablePreferences,
        userId: String
    ) {
        if (prefs[userStringKey(userId, UserField.createdAt)] == null) {
            prefs[userStringKey(userId, UserField.createdAt)] = System.currentTimeMillis().toString()
        }
        if (prefs[userStringKey(userId, UserField.updatedAt)] == null) {
            prefs[userStringKey(userId, UserField.updatedAt)] = System.currentTimeMillis().toString()
        }
        if (prefs[userStringKey(userId, UserField.currency)] == null) {
            prefs[userStringKey(userId, UserField.currency)] = CurrencyOption.PKR.code
        }
        if (prefs[userStringKey(userId, UserField.region)] == null) {
            prefs[userStringKey(userId, UserField.region)] = RegionOption.defaultFor(CurrencyOption.PKR).label
        }
        if (prefs[userStringKey(userId, UserField.mainFinancialGoal)] == null) {
            prefs[userStringKey(userId, UserField.mainFinancialGoal)] = "Track spending"
        }
        if (prefs[userStringKey(userId, UserField.profileName)] == null) {
            prefs[userStringKey(userId, UserField.profileName)] = "My Budget Profile"
        }
        if (prefs[userStringKey(userId, UserField.weekStart)] == null) {
            prefs[userStringKey(userId, UserField.weekStart)] = WeekStartOption.MONDAY.name
        }
        if (prefs[userStringKey(userId, UserField.monthlyIncome)] == null) {
            prefs[userStringKey(userId, UserField.monthlyIncome)] = "0.0"
        }
        if (prefs[userStringKey(userId, UserField.monthlyBudgetTarget)] == null) {
            prefs[userStringKey(userId, UserField.monthlyBudgetTarget)] = "0.0"
        }
        if (prefs[userStringKey(userId, UserField.cycleType)] == null) {
            prefs[userStringKey(userId, UserField.cycleType)] = BudgetCycleType.MONTHLY.name
        }
        if (prefs[userStringKey(userId, UserField.cycleStartDate)] == null) {
            prefs[userStringKey(userId, UserField.cycleStartDate)] = LocalDate.now().withDayOfMonth(1).toString()
        }
        if (prefs[userStringKey(userId, UserField.nextCycleDate)] == null) {
            prefs[userStringKey(userId, UserField.nextCycleDate)] = LocalDate.now().withDayOfMonth(1).plusMonths(1).toString()
        }
        if (prefs[userBooleanKey(userId, UserField.carryForwardRemainingBudget)] == null) {
            prefs[userBooleanKey(userId, UserField.carryForwardRemainingBudget)] = false
        }
        if (prefs[userStringKey(userId, UserField.reminderLeadDays)] == null) {
            prefs[userStringKey(userId, UserField.reminderLeadDays)] = "3"
        }
        if (prefs[userBooleanKey(userId, UserField.setupCompleted)] == null) {
            prefs[userBooleanKey(userId, UserField.setupCompleted)] = false
        }
        if (prefs[userBooleanKey(userId, UserField.setupResetPending)] == null) {
            prefs[userBooleanKey(userId, UserField.setupResetPending)] = false
        }
        if (prefs[userBooleanKey(userId, UserField.darkMode)] == null) {
            prefs[userBooleanKey(userId, UserField.darkMode)] = false
        }
        if (prefs[userBooleanKey(userId, UserField.notificationsEnabled)] == null) {
            prefs[userBooleanKey(userId, UserField.notificationsEnabled)] = false
        }
        if (prefs[userBooleanKey(userId, UserField.billRemindersEnabled)] == null) {
            prefs[userBooleanKey(userId, UserField.billRemindersEnabled)] = true
        }
        if (prefs[userBooleanKey(userId, UserField.recurringRemindersEnabled)] == null) {
            prefs[userBooleanKey(userId, UserField.recurringRemindersEnabled)] = true
        }
        if (prefs[userBooleanKey(userId, UserField.debtRemindersEnabled)] == null) {
            prefs[userBooleanKey(userId, UserField.debtRemindersEnabled)] = true
        }
        if (prefs[userBooleanKey(userId, UserField.budgetAlertsEnabled)] == null) {
            prefs[userBooleanKey(userId, UserField.budgetAlertsEnabled)] = true
        }
        if (prefs[userBooleanKey(userId, UserField.goalRemindersEnabled)] == null) {
            prefs[userBooleanKey(userId, UserField.goalRemindersEnabled)] = true
        }
        if (prefs[userBooleanKey(userId, UserField.privacyModeEnabled)] == null) {
            prefs[userBooleanKey(userId, UserField.privacyModeEnabled)] = false
        }
        if (prefs[userBooleanKey(userId, UserField.appLockEnabled)] == null) {
            prefs[userBooleanKey(userId, UserField.appLockEnabled)] = false
        }
        if (prefs[userBooleanKey(userId, UserField.biometricLockEnabled)] == null) {
            prefs[userBooleanKey(userId, UserField.biometricLockEnabled)] = false
        }
    }

    private fun writeUserPreferences(
        prefs: MutablePreferences,
        userId: String,
        preferences: AppPreferences
    ) {
        ensureUserPreferenceDefaults(prefs, userId)
        prefs[userBooleanKey(userId, UserField.setupCompleted)] = preferences.setupCompleted
        prefs[userBooleanKey(userId, UserField.setupResetPending)] = preferences.setupResetPending
        prefs[userStringKey(userId, UserField.currency)] = preferences.currency.code
        prefs[userStringKey(userId, UserField.region)] = preferences.region.label
        prefs[userStringKey(userId, UserField.monthlyIncome)] = preferences.monthlyIncome.toString()
        prefs[userStringKey(userId, UserField.monthlyBudgetTarget)] = preferences.monthlyBudgetTarget.toString()
        prefs[userStringKey(userId, UserField.mainFinancialGoal)] = preferences.mainFinancialGoal
        prefs[userStringKey(userId, UserField.cycleType)] = preferences.cycleType.name
        prefs[userStringKey(userId, UserField.cycleStartDate)] = preferences.cycleStartDate.toString()
        prefs[userStringKey(userId, UserField.nextCycleDate)] = preferences.nextCycleDate.toString()
        prefs[userBooleanKey(userId, UserField.carryForwardRemainingBudget)] = preferences.carryForwardRemainingBudget
        prefs[userBooleanKey(userId, UserField.darkMode)] = preferences.darkMode
        prefs[userStringKey(userId, UserField.profileName)] = preferences.profileName
        prefs[userStringKey(userId, UserField.weekStart)] = preferences.weekStart.name
        prefs[userBooleanKey(userId, UserField.notificationsEnabled)] = preferences.notificationsEnabled
        prefs[userBooleanKey(userId, UserField.billRemindersEnabled)] = preferences.billRemindersEnabled
        prefs[userBooleanKey(userId, UserField.recurringRemindersEnabled)] = preferences.recurringRemindersEnabled
        prefs[userBooleanKey(userId, UserField.debtRemindersEnabled)] = preferences.debtRemindersEnabled
        prefs[userBooleanKey(userId, UserField.budgetAlertsEnabled)] = preferences.budgetAlertsEnabled
        prefs[userBooleanKey(userId, UserField.goalRemindersEnabled)] = preferences.goalRemindersEnabled
        prefs[userStringKey(userId, UserField.reminderLeadDays)] = preferences.reminderLeadDays.toString()
        prefs[userBooleanKey(userId, UserField.privacyModeEnabled)] = preferences.privacyModeEnabled
        prefs[userBooleanKey(userId, UserField.appLockEnabled)] = preferences.appLockEnabled
        prefs[userBooleanKey(userId, UserField.biometricLockEnabled)] = preferences.biometricLockEnabled
        prefs[userStringKey(userId, UserField.createdAt)] = preferences.createdAt.toString()
        prefs[userStringKey(userId, UserField.updatedAt)] = preferences.updatedAt.toString()
    }

    private fun hasUserScopedState(prefs: Preferences, userId: String): Boolean {
        return prefs[userStringKey(userId, UserField.profileName)] != null ||
            prefs[userStringKey(userId, UserField.currency)] != null ||
            prefs[userStringKey(userId, UserField.createdAt)] != null
    }

    private fun clearLegacyPreferenceKeys(prefs: MutablePreferences) {
        prefs.remove(LegacyKeys.id)
        prefs.remove(LegacyKeys.userId)
        prefs.remove(LegacyKeys.setupCompleted)
        prefs.remove(LegacyKeys.currency)
        prefs.remove(LegacyKeys.region)
        prefs.remove(LegacyKeys.monthlyIncome)
        prefs.remove(LegacyKeys.monthlyBudgetTarget)
        prefs.remove(LegacyKeys.mainFinancialGoal)
        prefs.remove(LegacyKeys.darkMode)
        prefs.remove(LegacyKeys.profileName)
        prefs.remove(LegacyKeys.weekStart)
        prefs.remove(LegacyKeys.notificationsEnabled)
        prefs.remove(LegacyKeys.billRemindersEnabled)
        prefs.remove(LegacyKeys.recurringRemindersEnabled)
        prefs.remove(LegacyKeys.debtRemindersEnabled)
        prefs.remove(LegacyKeys.budgetAlertsEnabled)
        prefs.remove(LegacyKeys.goalRemindersEnabled)
        prefs.remove(LegacyKeys.reminderLeadDays)
        prefs.remove(LegacyKeys.privacyModeEnabled)
        prefs.remove(LegacyKeys.appLockEnabled)
        prefs.remove(LegacyKeys.biometricLockEnabled)
        prefs.remove(LegacyKeys.legacyPinHash)
        prefs.remove(LegacyKeys.createdAt)
        prefs.remove(LegacyKeys.updatedAt)
    }

    private fun userStringKey(userId: String, field: String) =
        stringPreferencesKey(UserScopedPreferenceKeys.stringName(userId, field))

    private fun userBooleanKey(userId: String, field: String) =
        booleanPreferencesKey(UserScopedPreferenceKeys.booleanName(userId, field))

    private fun String?.toCurrencyOption(): CurrencyOption {
        return CurrencyOption.fromCodeOrDefault(this, CurrencyOption.PKR)
    }

    private fun String?.toWeekStartOption(): WeekStartOption {
        return WeekStartOption.entries.firstOrNull { it.name.equals(this, ignoreCase = true) }
            ?: WeekStartOption.MONDAY
    }

    private fun String?.toBudgetCycleType(): BudgetCycleType {
        return BudgetCycleType.entries.firstOrNull { it.name.equals(this, ignoreCase = true) }
            ?: BudgetCycleType.MONTHLY
    }

    private fun String?.toLocalDateOrDefault(default: LocalDate): LocalDate {
        return runCatching { this?.let(LocalDate::parse) ?: default }.getOrDefault(default)
    }
}
