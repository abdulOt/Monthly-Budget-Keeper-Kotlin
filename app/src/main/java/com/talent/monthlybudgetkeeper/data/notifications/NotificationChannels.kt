package com.talent.monthlybudgetkeeper.data.notifications

data class NotificationChannelSpec(
    val id: String,
    val name: String,
    val description: String
)

object NotificationChannels {
    const val BUDGET_ALERTS = "budget_alerts"
    const val BILL_REMINDERS = "bill_reminders"
    const val SUBSCRIPTION_REMINDERS = "subscription_reminders"
    const val RECURRING_REMINDERS = "recurring_reminders"
    const val DEBT_REMINDERS = "debt_reminders"
    const val GOAL_REMINDERS = "goal_reminders"

    val all = listOf(
        NotificationChannelSpec(
            id = BUDGET_ALERTS,
            name = "Budget alerts",
            description = "Alerts for exceeded budgets, negative balance, and budget cycle renewal."
        ),
        NotificationChannelSpec(
            id = BILL_REMINDERS,
            name = "Bill reminders",
            description = "Reminders for bill payments that are due soon or overdue."
        ),
        NotificationChannelSpec(
            id = SUBSCRIPTION_REMINDERS,
            name = "Subscription reminders",
            description = "Reminders for active subscriptions and service renewals."
        ),
        NotificationChannelSpec(
            id = RECURRING_REMINDERS,
            name = "Recurring reminders",
            description = "Reminders for recurring income and expense items."
        ),
        NotificationChannelSpec(
            id = DEBT_REMINDERS,
            name = "Debt reminders",
            description = "Reminders when debt payments are due soon."
        ),
        NotificationChannelSpec(
            id = GOAL_REMINDERS,
            name = "Goal reminders",
            description = "Alerts when savings goals fall behind schedule."
        )
    )
}
