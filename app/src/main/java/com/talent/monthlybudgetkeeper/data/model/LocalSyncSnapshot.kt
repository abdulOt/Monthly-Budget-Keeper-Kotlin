package com.talent.monthlybudgetkeeper.data.model

import com.talent.monthlybudgetkeeper.data.local.entity.AccountEntity
import com.talent.monthlybudgetkeeper.data.local.entity.CategoryBudgetEntity
import com.talent.monthlybudgetkeeper.data.local.entity.DebtEntity
import com.talent.monthlybudgetkeeper.data.local.entity.EnvelopeBudgetEntity
import com.talent.monthlybudgetkeeper.data.local.entity.GoalEntity
import com.talent.monthlybudgetkeeper.data.local.entity.MonthlyBudgetEntity
import com.talent.monthlybudgetkeeper.data.local.entity.PaycheckPlanEntity
import com.talent.monthlybudgetkeeper.data.local.entity.ReceiptAttachmentEntity
import com.talent.monthlybudgetkeeper.data.local.entity.RecurringItemEntity
import com.talent.monthlybudgetkeeper.data.local.entity.SplitTransactionEntity
import com.talent.monthlybudgetkeeper.data.local.entity.SubscriptionBillEntity
import com.talent.monthlybudgetkeeper.data.local.entity.TransactionEntity
import com.talent.monthlybudgetkeeper.data.local.entity.TransactionRuleEntity
import com.talent.monthlybudgetkeeper.data.local.entity.TransferEntity

data class LocalSyncSnapshot(
    val preferences: AppPreferences,
    val transactions: List<TransactionEntity>,
    val monthlyBudgets: List<MonthlyBudgetEntity>,
    val categoryBudgets: List<CategoryBudgetEntity>,
    val accounts: List<AccountEntity>,
    val transfers: List<TransferEntity>,
    val recurringItems: List<RecurringItemEntity>,
    val subscriptionBills: List<SubscriptionBillEntity>,
    val goals: List<GoalEntity>,
    val debts: List<DebtEntity>,
    val envelopeBudgets: List<EnvelopeBudgetEntity>,
    val paycheckPlans: List<PaycheckPlanEntity>,
    val splitTransactions: List<SplitTransactionEntity>,
    val receiptAttachments: List<ReceiptAttachmentEntity>,
    val transactionRules: List<TransactionRuleEntity>
)
