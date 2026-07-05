package com.talent.monthlybudgetkeeper.data.model

import kotlinx.serialization.Serializable

@Serializable
data class BackupPayload(
    val format: String = FORMAT,
    val version: Int = VERSION,
    val exportedAt: Long,
    val backupUserId: String? = null,
    val profile: CloudProfileRow? = null,
    val preferences: CloudUserSettingsRow,
    val transactions: List<CloudTransactionRow> = emptyList(),
    val monthlyBudgets: List<CloudMonthlyBudgetRow> = emptyList(),
    val categoryBudgets: List<CloudCategoryBudgetRow> = emptyList(),
    val accounts: List<CloudAccountRow> = emptyList(),
    val transfers: List<CloudTransferRow> = emptyList(),
    val recurringItems: List<CloudRecurringItemRow> = emptyList(),
    val subscriptionBills: List<CloudSubscriptionBillRow> = emptyList(),
    val goals: List<CloudGoalRow> = emptyList(),
    val debts: List<CloudDebtRow> = emptyList(),
    val envelopeBudgets: List<CloudEnvelopeBudgetRow> = emptyList(),
    val paycheckPlans: List<CloudPaycheckPlanRow> = emptyList(),
    val splitTransactions: List<CloudSplitTransactionRow> = emptyList(),
    val receiptAttachments: List<CloudReceiptAttachmentRow> = emptyList(),
    val transactionRules: List<CloudTransactionRuleRow> = emptyList()
) {
    companion object {
        const val FORMAT = "monthly_budget_keeper_backup"
        const val VERSION = 2
    }
}
