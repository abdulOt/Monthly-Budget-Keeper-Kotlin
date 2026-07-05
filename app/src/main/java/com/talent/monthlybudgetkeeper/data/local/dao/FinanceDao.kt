package com.talent.monthlybudgetkeeper.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import com.talent.monthlybudgetkeeper.data.local.entity.AccountEntity
import com.talent.monthlybudgetkeeper.data.local.entity.DebtEntity
import com.talent.monthlybudgetkeeper.data.local.entity.EnvelopeBudgetEntity
import com.talent.monthlybudgetkeeper.data.local.entity.GoalEntity
import com.talent.monthlybudgetkeeper.data.local.entity.NetWorthSnapshotEntity
import com.talent.monthlybudgetkeeper.data.local.entity.PaycheckPlanEntity
import com.talent.monthlybudgetkeeper.data.local.entity.ReceiptAttachmentEntity
import com.talent.monthlybudgetkeeper.data.local.entity.RecurringItemEntity
import com.talent.monthlybudgetkeeper.data.local.entity.SplitTransactionEntity
import com.talent.monthlybudgetkeeper.data.local.entity.SubscriptionBillEntity
import com.talent.monthlybudgetkeeper.data.local.entity.TransactionRuleEntity
import com.talent.monthlybudgetkeeper.data.local.entity.TransferEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FinanceDao {
    @Query("SELECT * FROM accounts ORDER BY isArchived ASC, name ASC")
    fun observeAccounts(): Flow<List<AccountEntity>>

    @Query("SELECT * FROM accounts ORDER BY isArchived ASC, name ASC")
    suspend fun getAllAccounts(): List<AccountEntity>

    @Query("SELECT * FROM accounts WHERE remoteId = :remoteId LIMIT 1")
    suspend fun getAccountByRemoteId(remoteId: String): AccountEntity?

    @Query("SELECT * FROM accounts WHERE institution = :institution LIMIT 1")
    suspend fun getAccountByInstitution(institution: String): AccountEntity?

    @Upsert
    suspend fun upsertAccounts(accounts: List<AccountEntity>)

    @Delete
    suspend fun deleteAccount(account: AccountEntity)

    @Query("DELETE FROM accounts")
    suspend fun clearAccounts()

    @Query("SELECT COUNT(*) FROM accounts")
    suspend fun getAccountCount(): Int

    @Query("SELECT * FROM transfers ORDER BY date DESC, updatedAt DESC")
    fun observeTransfers(): Flow<List<TransferEntity>>

    @Query("SELECT * FROM transfers ORDER BY date DESC, updatedAt DESC")
    suspend fun getAllTransfers(): List<TransferEntity>

    @Upsert
    suspend fun upsertTransfers(items: List<TransferEntity>)

    @Query("DELETE FROM transfers")
    suspend fun clearTransfers()

    @Query("SELECT * FROM recurring_items ORDER BY isActive DESC, nextDueDate ASC, updatedAt DESC")
    fun observeRecurringItems(): Flow<List<RecurringItemEntity>>

    @Query("SELECT * FROM recurring_items ORDER BY isActive DESC, nextDueDate ASC, updatedAt DESC")
    suspend fun getAllRecurringItems(): List<RecurringItemEntity>

    @Upsert
    suspend fun upsertRecurringItems(items: List<RecurringItemEntity>)

    @Query("DELETE FROM recurring_items")
    suspend fun clearRecurringItems()

    @Query("SELECT * FROM subscription_bills ORDER BY isActive DESC, nextChargeDate ASC")
    fun observeSubscriptionBills(): Flow<List<SubscriptionBillEntity>>

    @Query("SELECT * FROM subscription_bills ORDER BY isActive DESC, nextChargeDate ASC")
    suspend fun getAllSubscriptionBills(): List<SubscriptionBillEntity>

    @Upsert
    suspend fun upsertSubscriptionBills(items: List<SubscriptionBillEntity>)

    @Query("DELETE FROM subscription_bills")
    suspend fun clearSubscriptionBills()

    @Query("SELECT * FROM goals ORDER BY isCompleted ASC, updatedAt DESC")
    fun observeGoals(): Flow<List<GoalEntity>>

    @Query("SELECT * FROM goals ORDER BY isCompleted ASC, updatedAt DESC")
    suspend fun getAllGoals(): List<GoalEntity>

    @Upsert
    suspend fun upsertGoals(goals: List<GoalEntity>)

    @Query("DELETE FROM goals")
    suspend fun clearGoals()

    @Query("SELECT * FROM debts ORDER BY dueDate ASC, updatedAt DESC")
    fun observeDebts(): Flow<List<DebtEntity>>

    @Query("SELECT * FROM debts ORDER BY dueDate ASC, updatedAt DESC")
    suspend fun getAllDebts(): List<DebtEntity>

    @Upsert
    suspend fun upsertDebts(debts: List<DebtEntity>)

    @Delete
    suspend fun deleteDebt(debt: DebtEntity)

    @Query("DELETE FROM debts")
    suspend fun clearDebts()

    @Query("SELECT * FROM net_worth_snapshots ORDER BY snapshotDate DESC")
    fun observeNetWorthSnapshots(): Flow<List<NetWorthSnapshotEntity>>

    @Query("SELECT * FROM net_worth_snapshots ORDER BY snapshotDate DESC")
    suspend fun getAllNetWorthSnapshots(): List<NetWorthSnapshotEntity>

    @Upsert
    suspend fun upsertNetWorthSnapshots(items: List<NetWorthSnapshotEntity>)

    @Query("DELETE FROM net_worth_snapshots")
    suspend fun clearNetWorthSnapshots()

    @Query("SELECT * FROM envelope_budgets ORDER BY monthKey DESC, name ASC")
    fun observeEnvelopeBudgets(): Flow<List<EnvelopeBudgetEntity>>

    @Query("SELECT * FROM envelope_budgets ORDER BY monthKey DESC, name ASC")
    suspend fun getAllEnvelopeBudgets(): List<EnvelopeBudgetEntity>

    @Upsert
    suspend fun upsertEnvelopeBudgets(items: List<EnvelopeBudgetEntity>)

    @Query("DELETE FROM envelope_budgets")
    suspend fun clearEnvelopeBudgets()

    @Query("SELECT * FROM paycheck_plans ORDER BY payday ASC, updatedAt DESC")
    fun observePaycheckPlans(): Flow<List<PaycheckPlanEntity>>

    @Query("SELECT * FROM paycheck_plans ORDER BY payday ASC, updatedAt DESC")
    suspend fun getAllPaycheckPlans(): List<PaycheckPlanEntity>

    @Upsert
    suspend fun upsertPaycheckPlans(items: List<PaycheckPlanEntity>)

    @Query("DELETE FROM paycheck_plans")
    suspend fun clearPaycheckPlans()

    @Query("SELECT * FROM split_transactions WHERE parentTransactionRemoteId = :transactionRemoteId ORDER BY amount DESC")
    fun observeSplitTransactions(transactionRemoteId: String): Flow<List<SplitTransactionEntity>>

    @Query("SELECT * FROM split_transactions ORDER BY updatedAt DESC")
    suspend fun getAllSplitTransactions(): List<SplitTransactionEntity>

    @Upsert
    suspend fun upsertSplitTransactions(items: List<SplitTransactionEntity>)

    @Query("DELETE FROM split_transactions")
    suspend fun clearSplitTransactions()

    @Query("SELECT * FROM receipt_attachments WHERE transactionRemoteId = :transactionRemoteId ORDER BY createdAt DESC")
    fun observeReceiptAttachments(transactionRemoteId: String): Flow<List<ReceiptAttachmentEntity>>

    @Query("SELECT * FROM receipt_attachments ORDER BY updatedAt DESC")
    suspend fun getAllReceiptAttachments(): List<ReceiptAttachmentEntity>

    @Upsert
    suspend fun upsertReceiptAttachments(items: List<ReceiptAttachmentEntity>)

    @Query("DELETE FROM receipt_attachments")
    suspend fun clearReceiptAttachments()

    @Query("SELECT * FROM transaction_rules WHERE isActive = 1 ORDER BY priority DESC, updatedAt DESC")
    suspend fun getActiveTransactionRules(): List<TransactionRuleEntity>

    @Query("SELECT * FROM transaction_rules ORDER BY priority DESC, updatedAt DESC")
    fun observeTransactionRules(): Flow<List<TransactionRuleEntity>>

    @Query("SELECT * FROM transaction_rules ORDER BY priority DESC, updatedAt DESC")
    suspend fun getAllTransactionRules(): List<TransactionRuleEntity>

    @Upsert
    suspend fun upsertTransactionRules(items: List<TransactionRuleEntity>)

    @Query("DELETE FROM transaction_rules")
    suspend fun clearTransactionRules()
}
