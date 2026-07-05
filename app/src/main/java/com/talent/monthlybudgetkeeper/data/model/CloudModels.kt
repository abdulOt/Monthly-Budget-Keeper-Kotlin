package com.talent.monthlybudgetkeeper.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CloudProfileRow(
    @SerialName("id") val id: String,
    @SerialName("user_id") val userId: String,
    @SerialName("email") val email: String,
    @SerialName("full_name") val fullName: String,
    @SerialName("created_at") val createdAt: Long,
    @SerialName("updated_at") val updatedAt: Long
)

@Serializable
data class CloudUserSettingsRow(
    @SerialName("id") val id: String,
    @SerialName("user_id") val userId: String,
    @SerialName("setup_completed") val setupCompleted: Boolean = false,
    @SerialName("currency") val currency: String,
    @SerialName("region") val region: String = "Pakistan",
    @SerialName("monthly_income") val monthlyIncome: Double = 0.0,
    @SerialName("monthly_budget_target") val monthlyBudgetTarget: Double = 0.0,
    @SerialName("main_financial_goal") val mainFinancialGoal: String = "Track spending",
    @SerialName("cycle_type") val cycleType: String = "MONTHLY",
    @SerialName("cycle_start_date") val cycleStartDate: String,
    @SerialName("next_cycle_date") val nextCycleDate: String,
    @SerialName("carry_forward_remaining_budget") val carryForwardRemainingBudget: Boolean = false,
    @SerialName("notifications_enabled") val notificationsEnabled: Boolean = false,
    @SerialName("privacy_mode_enabled") val privacyModeEnabled: Boolean = false,
    @SerialName("created_at") val createdAt: String,
    @SerialName("updated_at") val updatedAt: String
)

@Serializable
data class LegacyCloudUserSettingsRow(
    @SerialName("id") val id: String,
    @SerialName("user_id") val userId: String,
    @SerialName("setup_completed") val setupCompleted: Boolean = false,
    @SerialName("currency") val currency: String,
    @SerialName("region") val region: String = "Pakistan",
    @SerialName("monthly_income") val monthlyIncome: Double = 0.0,
    @SerialName("monthly_budget_target") val monthlyBudgetTarget: Double = 0.0,
    @SerialName("main_financial_goal") val mainFinancialGoal: String = "Track spending",
    @SerialName("cycle_type") val cycleType: String = "MONTHLY",
    @SerialName("cycle_start_date") val cycleStartDate: String,
    @SerialName("next_cycle_date") val nextCycleDate: String,
    @SerialName("notifications_enabled") val notificationsEnabled: Boolean = false,
    @SerialName("privacy_mode_enabled") val privacyModeEnabled: Boolean = false,
    @SerialName("created_at") val createdAt: String,
    @SerialName("updated_at") val updatedAt: String
)

@Serializable
data class CloudTransactionRow(
    @SerialName("id") val id: String,
    @SerialName("user_id") val userId: String,
    @SerialName("account_remote_id") val accountRemoteId: String? = null,
    @SerialName("transfer_remote_id") val transferRemoteId: String? = null,
    @SerialName("title") val title: String,
    @SerialName("amount") val amount: Double,
    @SerialName("type") val type: String,
    @SerialName("category") val category: String,
    @SerialName("date") val date: String,
    @SerialName("note") val note: String,
    @SerialName("is_transfer") val isTransfer: Boolean,
    @SerialName("is_planned") val isPlanned: Boolean,
    @SerialName("created_at") val createdAt: Long,
    @SerialName("updated_at") val updatedAt: Long
)

@Serializable
data class CloudMonthlyBudgetRow(
    @SerialName("id") val id: String,
    @SerialName("user_id") val userId: String,
    @SerialName("month_key") val monthKey: String,
    @SerialName("total_budget") val totalBudget: Double,
    @SerialName("created_at") val createdAt: Long,
    @SerialName("updated_at") val updatedAt: Long
)

@Serializable
data class CloudCategoryBudgetRow(
    @SerialName("id") val id: String,
    @SerialName("user_id") val userId: String,
    @SerialName("month_key") val monthKey: String,
    @SerialName("category") val category: String,
    @SerialName("limit_amount") val limitAmount: Double,
    @SerialName("created_at") val createdAt: Long,
    @SerialName("updated_at") val updatedAt: Long
)

@Serializable
data class CloudAccountRow(
    @SerialName("id") val id: String,
    @SerialName("user_id") val userId: String,
    @SerialName("name") val name: String,
    @SerialName("account_type") val accountType: String,
    @SerialName("current_balance") val currentBalance: Double,
    @SerialName("currency_code") val currencyCode: String,
    @SerialName("institution") val institution: String,
    @SerialName("include_in_net_worth") val includeInNetWorth: Boolean,
    @SerialName("is_archived") val isArchived: Boolean,
    @SerialName("created_at") val createdAt: Long,
    @SerialName("updated_at") val updatedAt: Long
)

@Serializable
data class CloudTransferRow(
    @SerialName("id") val id: String,
    @SerialName("user_id") val userId: String,
    @SerialName("from_account_remote_id") val fromAccountRemoteId: String,
    @SerialName("to_account_remote_id") val toAccountRemoteId: String,
    @SerialName("amount") val amount: Double,
    @SerialName("date") val date: String,
    @SerialName("note") val note: String,
    @SerialName("created_at") val createdAt: Long,
    @SerialName("updated_at") val updatedAt: Long
)

@Serializable
data class CloudRecurringItemRow(
    @SerialName("id") val id: String,
    @SerialName("user_id") val userId: String,
    @SerialName("account_remote_id") val accountRemoteId: String? = null,
    @SerialName("title") val title: String,
    @SerialName("amount") val amount: Double,
    @SerialName("type") val type: String,
    @SerialName("category") val category: String,
    @SerialName("start_date") val startDate: String,
    @SerialName("next_due_date") val nextDueDate: String,
    @SerialName("end_date") val endDate: String? = null,
    @SerialName("interval") val interval: String,
    @SerialName("note") val note: String,
    @SerialName("auto_create_transaction") val autoCreateTransaction: Boolean,
    @SerialName("is_active") val isActive: Boolean,
    @SerialName("created_at") val createdAt: Long,
    @SerialName("updated_at") val updatedAt: Long
)

@Serializable
data class CloudSubscriptionBillRow(
    @SerialName("id") val id: String,
    @SerialName("user_id") val userId: String,
    @SerialName("account_remote_id") val accountRemoteId: String? = null,
    @SerialName("name") val name: String,
    @SerialName("amount") val amount: Double,
    @SerialName("category") val category: String,
    @SerialName("due_date") val dueDate: String,
    @SerialName("next_charge_date") val nextChargeDate: String,
    @SerialName("billing_cycle") val billingCycle: String,
    @SerialName("note") val note: String,
    @SerialName("reminder_days") val reminderDays: Int,
    @SerialName("is_auto_pay") val isAutoPay: Boolean,
    @SerialName("is_active") val isActive: Boolean,
    @SerialName("created_at") val createdAt: Long,
    @SerialName("updated_at") val updatedAt: Long
)

@Serializable
data class CloudGoalRow(
    @SerialName("id") val id: String,
    @SerialName("user_id") val userId: String,
    @SerialName("linked_account_remote_id") val linkedAccountRemoteId: String? = null,
    @SerialName("name") val name: String,
    @SerialName("target_amount") val targetAmount: Double,
    @SerialName("current_amount") val currentAmount: Double,
    @SerialName("target_date") val targetDate: String? = null,
    @SerialName("monthly_contribution") val monthlyContribution: Double,
    @SerialName("is_sinking_fund") val isSinkingFund: Boolean,
    @SerialName("note") val note: String,
    @SerialName("is_completed") val isCompleted: Boolean,
    @SerialName("created_at") val createdAt: Long,
    @SerialName("updated_at") val updatedAt: Long
)

@Serializable
data class CloudDebtRow(
    @SerialName("id") val id: String,
    @SerialName("user_id") val userId: String,
    @SerialName("account_remote_id") val accountRemoteId: String? = null,
    @SerialName("name") val name: String,
    @SerialName("lender") val lender: String,
    @SerialName("total_amount") val totalAmount: Double,
    @SerialName("remaining_amount") val remainingAmount: Double,
    @SerialName("due_date") val dueDate: String? = null,
    @SerialName("interest_rate") val interestRate: Double,
    @SerialName("minimum_payment") val minimumPayment: Double,
    @SerialName("planned_payment") val plannedPayment: Double,
    @SerialName("strategy") val strategy: String,
    @SerialName("note") val note: String,
    @SerialName("created_at") val createdAt: Long,
    @SerialName("updated_at") val updatedAt: Long
)

@Serializable
data class CloudEnvelopeBudgetRow(
    @SerialName("id") val id: String,
    @SerialName("user_id") val userId: String,
    @SerialName("month_key") val monthKey: String,
    @SerialName("name") val name: String,
    @SerialName("category") val category: String? = null,
    @SerialName("planned_amount") val plannedAmount: Double,
    @SerialName("rollover_amount") val rolloverAmount: Double,
    @SerialName("spent_amount") val spentAmount: Double,
    @SerialName("created_at") val createdAt: Long,
    @SerialName("updated_at") val updatedAt: Long
)

@Serializable
data class CloudPaycheckPlanRow(
    @SerialName("id") val id: String,
    @SerialName("user_id") val userId: String,
    @SerialName("name") val name: String,
    @SerialName("account_remote_id") val accountRemoteId: String? = null,
    @SerialName("expected_amount") val expectedAmount: Double,
    @SerialName("payday") val payday: String,
    @SerialName("allocated_amount") val allocatedAmount: Double,
    @SerialName("note") val note: String,
    @SerialName("created_at") val createdAt: Long,
    @SerialName("updated_at") val updatedAt: Long
)

@Serializable
data class CloudSplitTransactionRow(
    @SerialName("id") val id: String,
    @SerialName("user_id") val userId: String,
    @SerialName("parent_transaction_remote_id") val parentTransactionRemoteId: String,
    @SerialName("title") val title: String,
    @SerialName("category") val category: String,
    @SerialName("amount") val amount: Double,
    @SerialName("note") val note: String,
    @SerialName("created_at") val createdAt: Long,
    @SerialName("updated_at") val updatedAt: Long
)

@Serializable
data class CloudReceiptAttachmentRow(
    @SerialName("id") val id: String,
    @SerialName("user_id") val userId: String,
    @SerialName("transaction_remote_id") val transactionRemoteId: String,
    @SerialName("local_uri") val localUri: String,
    @SerialName("file_name") val fileName: String,
    @SerialName("mime_type") val mimeType: String,
    @SerialName("created_at") val createdAt: Long,
    @SerialName("updated_at") val updatedAt: Long
)

@Serializable
data class CloudTransactionRuleRow(
    @SerialName("id") val id: String,
    @SerialName("user_id") val userId: String,
    @SerialName("keyword") val keyword: String,
    @SerialName("match_field") val matchField: String,
    @SerialName("target_type") val targetType: String,
    @SerialName("target_category") val targetCategory: String,
    @SerialName("target_account_remote_id") val targetAccountRemoteId: String? = null,
    @SerialName("priority") val priority: Int,
    @SerialName("is_active") val isActive: Boolean,
    @SerialName("created_at") val createdAt: Long,
    @SerialName("updated_at") val updatedAt: Long
)

data class CloudSnapshot(
    val profile: CloudProfileRow?,
    val preferences: CloudUserSettingsRow?,
    val transactions: List<CloudTransactionRow>,
    val monthlyBudgets: List<CloudMonthlyBudgetRow>,
    val categoryBudgets: List<CloudCategoryBudgetRow>,
    val accounts: List<CloudAccountRow>,
    val transfers: List<CloudTransferRow>,
    val recurringItems: List<CloudRecurringItemRow>,
    val subscriptionBills: List<CloudSubscriptionBillRow>,
    val goals: List<CloudGoalRow>,
    val debts: List<CloudDebtRow>,
    val envelopeBudgets: List<CloudEnvelopeBudgetRow>,
    val paycheckPlans: List<CloudPaycheckPlanRow>,
    val splitTransactions: List<CloudSplitTransactionRow>,
    val receiptAttachments: List<CloudReceiptAttachmentRow>,
    val transactionRules: List<CloudTransactionRuleRow>
) {
    val hasRemoteData: Boolean
        get() = transactions.isNotEmpty() ||
            monthlyBudgets.isNotEmpty() ||
            categoryBudgets.isNotEmpty() ||
            accounts.isNotEmpty() ||
            transfers.isNotEmpty() ||
            recurringItems.isNotEmpty() ||
            subscriptionBills.isNotEmpty() ||
            goals.isNotEmpty() ||
            debts.isNotEmpty() ||
            envelopeBudgets.isNotEmpty() ||
            paycheckPlans.isNotEmpty() ||
            splitTransactions.isNotEmpty() ||
            receiptAttachments.isNotEmpty() ||
            transactionRules.isNotEmpty()
}
