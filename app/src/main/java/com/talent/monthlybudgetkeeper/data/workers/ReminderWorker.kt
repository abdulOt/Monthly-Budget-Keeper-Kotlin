package com.talent.monthlybudgetkeeper.data.workers

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.talent.monthlybudgetkeeper.data.local.entity.DebtEntity
import com.talent.monthlybudgetkeeper.data.model.TransactionCategory
import com.talent.monthlybudgetkeeper.data.logic.FinanceCalculations
import com.talent.monthlybudgetkeeper.data.notifications.NotificationEventStore
import com.talent.monthlybudgetkeeper.data.notifications.NotificationChannels
import com.talent.monthlybudgetkeeper.data.notifications.NotificationCoordinator
import com.talent.monthlybudgetkeeper.data.local.entity.SubscriptionBillEntity
import com.talent.monthlybudgetkeeper.data.model.TransactionType
import com.talent.monthlybudgetkeeper.data.repository.BudgetRepository
import com.talent.monthlybudgetkeeper.data.repository.FinanceRepository
import com.talent.monthlybudgetkeeper.data.repository.SettingsRepository
import com.talent.monthlybudgetkeeper.data.repository.TransactionRepository
import com.talent.monthlybudgetkeeper.utils.DateUtils
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.time.LocalDate
import java.time.YearMonth

@HiltWorker
class ReminderWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val notificationCoordinator: NotificationCoordinator,
    private val settingsRepository: SettingsRepository,
    private val financeRepository: FinanceRepository,
    private val budgetRepository: BudgetRepository,
    private val transactionRepository: TransactionRepository
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val preferences = settingsRepository.getCurrentPreferences()
        notificationCoordinator.ensureChannels()
        val userId = preferences.userId
        if (userId.isNullOrBlank()) {
            notificationCoordinator.cancelAllReminderNotifications()
            return Result.success()
        }
        if (!preferences.notificationsEnabled || !canPostNotifications()) {
            NotificationEventStore.clearUserEvents(applicationContext, userId)
                .forEach { notificationCoordinator.cancelNotification(notificationIdFor(it)) }
            return Result.success()
        }

        val today = LocalDate.now()
        val leadDays = preferences.reminderLeadDays.coerceIn(1, 14)
        val activeEvents = mutableListOf<ReminderEvent>()

        val subscriptionBills = financeRepository.getAllSubscriptionBills()
            .filter { it.isActive }

        if (preferences.billRemindersEnabled) {
            subscriptionBills.forEach { bill ->
                val dueDate = bill.nextChargeDate
                val dueLabel = DateUtils.relativeDueLabel(dueDate, today)
                if (bill.category == TransactionCategory.BILLS) {
                    when {
                        dueDate.isBefore(today) -> {
                            activeEvents += ReminderEvent(
                                key = "bill_overdue_${bill.remoteId ?: bill.name}_${dueDate}",
                                channelId = NotificationChannels.BILL_REMINDERS,
                                title = "${bill.name} bill overdue",
                                message = "$dueLabel (${DateUtils.formatDate(dueDate)})."
                            )
                        }
                        isDueSoon(dueDate, today, leadDays) -> {
                            activeEvents += ReminderEvent(
                                key = "bill_due_${bill.remoteId ?: bill.name}_${dueDate}",
                                channelId = NotificationChannels.BILL_REMINDERS,
                                title = "${bill.name} bill due soon",
                                message = "$dueLabel (${DateUtils.formatDate(dueDate)})."
                            )
                        }
                    }
                } else if (isDueSoon(dueDate, today, leadDays)) {
                    activeEvents += ReminderEvent(
                        key = "subscription_due_${bill.remoteId ?: bill.name}_${dueDate}",
                        channelId = NotificationChannels.SUBSCRIPTION_REMINDERS,
                        title = "${bill.name} subscription due soon",
                        message = "$dueLabel (${DateUtils.formatDate(dueDate)})."
                    )
                }
            }
        }

        if (preferences.recurringRemindersEnabled) {
            financeRepository.getAllRecurringItems()
                .filter { it.isActive && isDueSoon(it.nextDueDate, today, leadDays) }
                .forEach { item ->
                    val dueLabel = DateUtils.relativeDueLabel(item.nextDueDate, today)
                    activeEvents += ReminderEvent(
                        key = "recurring_${item.remoteId ?: item.title}_${item.nextDueDate}",
                        channelId = NotificationChannels.RECURRING_REMINDERS,
                        title = "${item.title} is coming up",
                        message = "Recurring ${item.type.name.lowercase()} is $dueLabel (${DateUtils.formatDate(item.nextDueDate)})."
                    )
                }
        }

        if (preferences.debtRemindersEnabled) {
            financeRepository.getAllDebts()
                .filter { it.remainingAmount > 0.0 && it.dueDate != null }
                .forEach { debt ->
                    val dueDate = debt.dueDate ?: return@forEach
                    if (isDueSoonOrOverdue(dueDate, today, leadDays)) {
                        activeEvents += buildDebtReminderEvent(debt, dueDate, today)
                    }
                }
        }

        if (preferences.budgetAlertsEnabled) {
            val month = YearMonth.now()
            val monthKey = DateUtils.toMonthKey(month)
            val budgetEvents = FinanceCalculations.buildBudgetAlertEvents(
                month = month,
                today = today,
                monthlyBudget = budgetRepository.getAllMonthlyBudgets().firstOrNull { it.monthKey == monthKey },
                plannedBudgetTarget = preferences.monthlyBudgetTarget,
                categoryBudgets = budgetRepository.getAllCategoryBudgets(),
                transactions = transactionRepository.getAllTransactions(),
                accounts = financeRepository.getAllAccounts().filter { !it.isArchived },
                cycleType = preferences.cycleType,
                nextCycleDate = preferences.nextCycleDate,
                carryForwardRemainingBudget = preferences.carryForwardRemainingBudget
            )
            activeEvents += budgetEvents.map { event ->
                ReminderEvent(
                    key = event.key,
                    channelId = NotificationChannels.BUDGET_ALERTS,
                    title = event.title,
                    message = event.message
                )
            }
        }

        if (preferences.goalRemindersEnabled) {
            financeRepository.getAllGoals()
                .filter { !it.isCompleted && it.targetDate != null && isGoalBehindSchedule(it.currentAmount, it.targetAmount, it.createdAt, it.targetDate!!, today) }
                .forEach { goal ->
                    activeEvents += ReminderEvent(
                        key = "goal_${goal.remoteId ?: goal.name}_${goal.targetDate}",
                        channelId = NotificationChannels.GOAL_REMINDERS,
                        title = "${goal.name} needs attention",
                        message = "This goal is behind schedule for its target date."
                    )
                }
        }
        NotificationEventStore.removeInactiveEventKeys(
            context = applicationContext,
            userId = userId,
            activeEventKeys = activeEvents.map(ReminderEvent::key).toSet()
        ).forEach { notificationCoordinator.cancelNotification(notificationIdFor(it)) }

        activeEvents.forEach { event ->
            if (NotificationEventStore.shouldNotify(applicationContext, userId, event.key)) {
                notifyReminder(event)
            }
        }

        return Result.success()
    }

    private fun isDueSoon(date: LocalDate, today: LocalDate, leadDays: Int): Boolean {
        return !date.isBefore(today) && !date.isAfter(today.plusDays(leadDays.toLong()))
    }

    private fun isGoalBehindSchedule(
        currentAmount: Double,
        targetAmount: Double,
        createdAtMillis: Long,
        targetDate: LocalDate,
        today: LocalDate
    ): Boolean {
        if (targetAmount <= 0.0 || !targetDate.isAfter(today)) return false
        val startDate = java.time.Instant.ofEpochMilli(createdAtMillis)
            .atZone(java.time.ZoneId.systemDefault())
            .toLocalDate()
        val totalDays = java.time.temporal.ChronoUnit.DAYS.between(startDate, targetDate).coerceAtLeast(1)
        val elapsedDays = java.time.temporal.ChronoUnit.DAYS.between(startDate, today).coerceAtLeast(0)
        val expectedRatio = (elapsedDays.toDouble() / totalDays.toDouble()).coerceIn(0.0, 1.0)
        val actualRatio = (currentAmount / targetAmount).coerceIn(0.0, 1.0)
        return actualRatio + 0.1 < expectedRatio
    }

    private fun notifyReminder(
        event: ReminderEvent
    ) {
        notificationCoordinator.sendReminder(
            channelId = event.channelId,
            id = notificationIdFor(event.key),
            title = event.title,
            message = event.message
        )
    }

    private fun canPostNotifications(): Boolean {
        return ActivityCompat.checkSelfPermission(
            applicationContext,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED || android.os.Build.VERSION.SDK_INT < 33
    }

    private fun resolveBillChannel(item: SubscriptionBillEntity): String {
        return if (item.category == TransactionCategory.BILLS) {
            NotificationChannels.BILL_REMINDERS
        } else {
            NotificationChannels.SUBSCRIPTION_REMINDERS
        }
    }

    private fun isDueSoonOrOverdue(date: LocalDate, today: LocalDate, leadDays: Int): Boolean {
        return date.isBefore(today) || isDueSoon(date, today, leadDays)
    }

    private fun buildDebtReminderEvent(
        debt: DebtEntity,
        dueDate: LocalDate,
        today: LocalDate
    ): ReminderEvent {
        val dueLabel = DateUtils.relativeDueLabel(dueDate, today)
        return ReminderEvent(
            key = "debt_due_${debt.remoteId ?: debt.name}_${dueDate}",
            channelId = NotificationChannels.DEBT_REMINDERS,
            title = "${debt.name} payment due",
            message = "Payment target is $dueLabel (${DateUtils.formatDate(dueDate)})."
        )
    }

    private fun notificationIdFor(eventKey: String): Int = eventKey.hashCode()

    private data class ReminderEvent(
        val key: String,
        val channelId: String,
        val title: String,
        val message: String
    )

    companion object {
        const val WORK_NAME = "budget_keeper_reminders"
        const val IMMEDIATE_WORK_NAME = "budget_keeper_reminders_immediate"
    }
}
