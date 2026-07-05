package com.talent.monthlybudgetkeeper.data.model

import java.time.LocalDate

data class AppPreferences(
    val id: String = "local_preferences",
    val userId: String? = null,
    val onboardingCompleted: Boolean = false,
    val setupCompleted: Boolean = false,
    val setupResetPending: Boolean = false,
    val currency: CurrencyOption = CurrencyOption.PKR,
    val region: RegionOption = RegionOption.defaultFor(CurrencyOption.PKR),
    val monthlyIncome: Double = 0.0,
    val monthlyBudgetTarget: Double = 0.0,
    val mainFinancialGoal: String = "Track spending",
    val cycleType: BudgetCycleType = BudgetCycleType.MONTHLY,
    val cycleStartDate: LocalDate = LocalDate.now().withDayOfMonth(1),
    val nextCycleDate: LocalDate = LocalDate.now().withDayOfMonth(1).plusMonths(1),
    val carryForwardRemainingBudget: Boolean = false,
    val darkMode: Boolean = false,
    val profileName: String = "My Budget Profile",
    val weekStart: WeekStartOption = WeekStartOption.MONDAY,
    val notificationsEnabled: Boolean = true,
    val billRemindersEnabled: Boolean = true,
    val recurringRemindersEnabled: Boolean = true,
    val debtRemindersEnabled: Boolean = true,
    val budgetAlertsEnabled: Boolean = true,
    val goalRemindersEnabled: Boolean = true,
    val reminderLeadDays: Int = 3,
    val privacyModeEnabled: Boolean = false,
    val appLockEnabled: Boolean = false,
    val biometricLockEnabled: Boolean = false,
    val pinHash: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
