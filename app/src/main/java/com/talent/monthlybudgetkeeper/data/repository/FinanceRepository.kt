package com.talent.monthlybudgetkeeper.data.repository

import com.talent.monthlybudgetkeeper.data.auth.SessionManager
import com.talent.monthlybudgetkeeper.data.logic.FinanceCalculations
import com.talent.monthlybudgetkeeper.data.local.dao.FinanceDao
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
import com.talent.monthlybudgetkeeper.data.model.AccountType
import com.talent.monthlybudgetkeeper.data.model.SyncStatus
import com.talent.monthlybudgetkeeper.data.sync.SyncSignalBus
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.math.abs

@Singleton
class FinanceRepository @Inject constructor(
    private val financeDao: FinanceDao,
    private val sessionManager: SessionManager,
    private val syncSignalBus: SyncSignalBus
) {
    companion object {
        const val SYSTEM_BALANCE_ACCOUNT_NAME = "Available Balance"
        const val SYSTEM_BALANCE_ACCOUNT_MARKER = "__system_balance__"
    }

    fun observeAccounts(): Flow<List<AccountEntity>> = financeDao.observeAccounts().map { accounts ->
        normalizeAccounts(accounts)
    }
    fun observeTransfers(): Flow<List<TransferEntity>> = financeDao.observeTransfers()
    fun observeRecurringItems(): Flow<List<RecurringItemEntity>> = financeDao.observeRecurringItems()
    fun observeSubscriptionBills(): Flow<List<SubscriptionBillEntity>> = financeDao.observeSubscriptionBills()
    fun observeGoals(): Flow<List<GoalEntity>> = financeDao.observeGoals()
    fun observeDebts(): Flow<List<DebtEntity>> = financeDao.observeDebts()
    fun observeNetWorthSnapshots(): Flow<List<NetWorthSnapshotEntity>> = financeDao.observeNetWorthSnapshots()
    fun observeEnvelopeBudgets(): Flow<List<EnvelopeBudgetEntity>> = financeDao.observeEnvelopeBudgets()
    fun observePaycheckPlans(): Flow<List<PaycheckPlanEntity>> = financeDao.observePaycheckPlans()
    fun observeTransactionRules(): Flow<List<TransactionRuleEntity>> = financeDao.observeTransactionRules()
    fun observeReceiptAttachments(transactionRemoteId: String): Flow<List<ReceiptAttachmentEntity>> =
        financeDao.observeReceiptAttachments(transactionRemoteId)

    suspend fun getAllAccounts(): List<AccountEntity> = normalizeAccounts(financeDao.getAllAccounts())
    suspend fun getAllTransfers(): List<TransferEntity> = financeDao.getAllTransfers()
    suspend fun getAllRecurringItems(): List<RecurringItemEntity> = financeDao.getAllRecurringItems()
    suspend fun getAllSubscriptionBills(): List<SubscriptionBillEntity> = financeDao.getAllSubscriptionBills()
    suspend fun getAllGoals(): List<GoalEntity> = financeDao.getAllGoals()
    suspend fun getAllDebts(): List<DebtEntity> = financeDao.getAllDebts()
    suspend fun getAllNetWorthSnapshots(): List<NetWorthSnapshotEntity> = financeDao.getAllNetWorthSnapshots()
    suspend fun getAllEnvelopeBudgets(): List<EnvelopeBudgetEntity> = financeDao.getAllEnvelopeBudgets()
    suspend fun getAllPaycheckPlans(): List<PaycheckPlanEntity> = financeDao.getAllPaycheckPlans()
    suspend fun getAllSplitTransactions(): List<SplitTransactionEntity> = financeDao.getAllSplitTransactions()
    suspend fun getAllReceiptAttachments(): List<ReceiptAttachmentEntity> = financeDao.getAllReceiptAttachments()
    suspend fun getAllTransactionRules(): List<TransactionRuleEntity> = financeDao.getAllTransactionRules()
    suspend fun getActiveTransactionRules(): List<TransactionRuleEntity> = financeDao.getActiveTransactionRules()

    suspend fun saveSubscriptionBill(item: SubscriptionBillEntity, notifySync: Boolean = true) {
        val now = System.currentTimeMillis()
        financeDao.upsertSubscriptionBills(
            listOf(
                item.copy(
                    userId = item.userId ?: sessionManager.currentUserOrNull()?.id,
                    createdAt = item.createdAt.takeIf { it > 0 } ?: now,
                    updatedAt = now,
                    syncStatus = SyncStatus.PENDING
                )
            )
        )
        if (notifySync) syncSignalBus.notifyDataChanged()
    }

    suspend fun saveTransfer(item: TransferEntity, notifySync: Boolean = true) {
        if (item.amount <= 0.0 || item.fromAccountRemoteId == item.toAccountRemoteId) return
        val sourceAccount = financeDao.getAccountByRemoteId(item.fromAccountRemoteId) ?: return
        val destinationAccount = financeDao.getAccountByRemoteId(item.toAccountRemoteId) ?: return
        val now = System.currentTimeMillis()
        financeDao.upsertAccounts(
            listOf(
                sourceAccount.copy(
                    currentBalance = sourceAccount.currentBalance - item.amount,
                    updatedAt = now,
                    syncStatus = SyncStatus.PENDING
                ),
                destinationAccount.copy(
                    currentBalance = destinationAccount.currentBalance + item.amount,
                    updatedAt = now,
                    syncStatus = SyncStatus.PENDING
                )
            )
        )
        financeDao.upsertTransfers(
            listOf(
                item.copy(
                    remoteId = item.remoteId ?: UUID.randomUUID().toString(),
                    userId = item.userId ?: sessionManager.currentUserOrNull()?.id,
                    createdAt = item.createdAt.takeIf { it > 0 } ?: now,
                    updatedAt = now,
                    syncStatus = SyncStatus.PENDING
                )
            )
        )
        captureNetWorthSnapshot()
        if (notifySync) syncSignalBus.notifyDataChanged()
    }

    suspend fun saveRecurringItem(item: RecurringItemEntity, notifySync: Boolean = true) {
        val now = System.currentTimeMillis()
        financeDao.upsertRecurringItems(
            listOf(
                item.copy(
                    remoteId = item.remoteId ?: UUID.randomUUID().toString(),
                    userId = item.userId ?: sessionManager.currentUserOrNull()?.id,
                    createdAt = item.createdAt.takeIf { it > 0 } ?: now,
                    updatedAt = now,
                    syncStatus = SyncStatus.PENDING
                )
            )
        )
        if (notifySync) syncSignalBus.notifyDataChanged()
    }

    suspend fun saveGoal(goal: GoalEntity, notifySync: Boolean = true) {
        val now = System.currentTimeMillis()
        financeDao.upsertGoals(
            listOf(
                goal.copy(
                    remoteId = goal.remoteId ?: UUID.randomUUID().toString(),
                    userId = goal.userId ?: sessionManager.currentUserOrNull()?.id,
                    createdAt = goal.createdAt.takeIf { it > 0 } ?: now,
                    updatedAt = now,
                    syncStatus = SyncStatus.PENDING
                )
            )
        )
        if (notifySync) syncSignalBus.notifyDataChanged()
    }

    suspend fun saveAccount(account: AccountEntity, notifySync: Boolean = true) {
        val now = System.currentTimeMillis()
        financeDao.upsertAccounts(
            listOf(
                account.copy(
                    remoteId = account.remoteId ?: UUID.randomUUID().toString(),
                    userId = resolveUserId(account.userId),
                    createdAt = account.createdAt.takeIf { it > 0 } ?: now,
                    updatedAt = now,
                    syncStatus = SyncStatus.PENDING
                )
            )
        )
        captureNetWorthSnapshot()
        if (notifySync) syncSignalBus.notifyDataChanged()
    }

    suspend fun saveDebt(debt: DebtEntity, notifySync: Boolean = true) {
        val now = System.currentTimeMillis()
        financeDao.upsertDebts(
            listOf(
                debt.copy(
                    remoteId = debt.remoteId ?: UUID.randomUUID().toString(),
                    userId = resolveUserId(debt.userId),
                    createdAt = debt.createdAt.takeIf { it > 0 } ?: now,
                    updatedAt = now,
                    syncStatus = SyncStatus.PENDING
                )
            )
        )
        captureNetWorthSnapshot()
        if (notifySync) syncSignalBus.notifyDataChanged()
    }

    suspend fun replaceAccounts(accounts: List<AccountEntity>, notifySync: Boolean = true) {
        financeDao.clearAccounts()
        if (accounts.isNotEmpty()) financeDao.upsertAccounts(accounts)
        captureNetWorthSnapshot()
        if (notifySync) syncSignalBus.notifyDataChanged()
    }

    suspend fun replaceTransfers(items: List<TransferEntity>, notifySync: Boolean = true) {
        financeDao.clearTransfers()
        if (items.isNotEmpty()) financeDao.upsertTransfers(items)
        if (notifySync) syncSignalBus.notifyDataChanged()
    }

    suspend fun replaceRecurringItems(items: List<RecurringItemEntity>, notifySync: Boolean = true) {
        financeDao.clearRecurringItems()
        if (items.isNotEmpty()) financeDao.upsertRecurringItems(items)
        if (notifySync) syncSignalBus.notifyDataChanged()
    }

    suspend fun replaceSubscriptionBills(items: List<SubscriptionBillEntity>, notifySync: Boolean = true) {
        financeDao.clearSubscriptionBills()
        if (items.isNotEmpty()) financeDao.upsertSubscriptionBills(items)
        if (notifySync) syncSignalBus.notifyDataChanged()
    }

    suspend fun replaceGoals(goals: List<GoalEntity>, notifySync: Boolean = true) {
        financeDao.clearGoals()
        if (goals.isNotEmpty()) financeDao.upsertGoals(goals)
        if (notifySync) syncSignalBus.notifyDataChanged()
    }

    suspend fun replaceDebts(debts: List<DebtEntity>, notifySync: Boolean = true) {
        financeDao.clearDebts()
        if (debts.isNotEmpty()) financeDao.upsertDebts(debts)
        captureNetWorthSnapshot()
        if (notifySync) syncSignalBus.notifyDataChanged()
    }

    suspend fun replaceEnvelopeBudgets(items: List<EnvelopeBudgetEntity>, notifySync: Boolean = true) {
        financeDao.clearEnvelopeBudgets()
        if (items.isNotEmpty()) financeDao.upsertEnvelopeBudgets(items)
        if (notifySync) syncSignalBus.notifyDataChanged()
    }

    suspend fun replacePaycheckPlans(items: List<PaycheckPlanEntity>, notifySync: Boolean = true) {
        financeDao.clearPaycheckPlans()
        if (items.isNotEmpty()) financeDao.upsertPaycheckPlans(items)
        if (notifySync) syncSignalBus.notifyDataChanged()
    }

    suspend fun replaceSplitTransactions(items: List<SplitTransactionEntity>, notifySync: Boolean = true) {
        financeDao.clearSplitTransactions()
        if (items.isNotEmpty()) financeDao.upsertSplitTransactions(items)
        if (notifySync) syncSignalBus.notifyDataChanged()
    }

    suspend fun replaceReceiptAttachments(items: List<ReceiptAttachmentEntity>, notifySync: Boolean = true) {
        financeDao.clearReceiptAttachments()
        if (items.isNotEmpty()) financeDao.upsertReceiptAttachments(items)
        if (notifySync) syncSignalBus.notifyDataChanged()
    }

    suspend fun replaceTransactionRules(items: List<TransactionRuleEntity>, notifySync: Boolean = true) {
        financeDao.clearTransactionRules()
        if (items.isNotEmpty()) financeDao.upsertTransactionRules(items)
        if (notifySync) syncSignalBus.notifyDataChanged()
    }

    suspend fun replaceAllFinanceData(
        accounts: List<AccountEntity>,
        transfers: List<TransferEntity>,
        recurringItems: List<RecurringItemEntity>,
        subscriptionBills: List<SubscriptionBillEntity>,
        goals: List<GoalEntity>,
        debts: List<DebtEntity>,
        envelopeBudgets: List<EnvelopeBudgetEntity>,
        paycheckPlans: List<PaycheckPlanEntity>,
        splitTransactions: List<SplitTransactionEntity>,
        receiptAttachments: List<ReceiptAttachmentEntity>,
        transactionRules: List<TransactionRuleEntity>,
        notifySync: Boolean = true
    ) {
        replaceAccounts(accounts, notifySync = false)
        replaceTransfers(transfers, notifySync = false)
        replaceRecurringItems(recurringItems, notifySync = false)
        replaceSubscriptionBills(subscriptionBills, notifySync = false)
        replaceGoals(goals, notifySync = false)
        replaceDebts(debts, notifySync = false)
        replaceEnvelopeBudgets(envelopeBudgets, notifySync = false)
        replacePaycheckPlans(paycheckPlans, notifySync = false)
        replaceSplitTransactions(splitTransactions, notifySync = false)
        replaceReceiptAttachments(receiptAttachments, notifySync = false)
        replaceTransactionRules(transactionRules, notifySync = false)
        captureNetWorthSnapshot()
        if (notifySync) syncSignalBus.notifyDataChanged()
    }

    suspend fun ensurePrimaryBalanceAccount(
        openingBalanceDelta: Double = 0.0,
        currencyCode: String = "PKR"
    ): AccountEntity {
        val now = System.currentTimeMillis()
        val existing = financeDao.getAccountByInstitution(SYSTEM_BALANCE_ACCOUNT_MARKER)
        val resolvedUserId = resolveUserId(existing?.userId)
        val account = if (existing == null) {
            AccountEntity(
                remoteId = UUID.randomUUID().toString(),
                userId = resolvedUserId,
                name = SYSTEM_BALANCE_ACCOUNT_NAME,
                accountType = AccountType.CASH,
                currentBalance = openingBalanceDelta,
                currencyCode = currencyCode,
                institution = SYSTEM_BALANCE_ACCOUNT_MARKER,
                includeInNetWorth = true,
                createdAt = now,
                updatedAt = now,
                syncStatus = SyncStatus.PENDING
            )
        } else {
            existing.copy(
                userId = resolvedUserId,
                currencyCode = existing.currencyCode.ifBlank { currencyCode },
                currentBalance = existing.currentBalance + openingBalanceDelta,
                updatedAt = now,
                syncStatus = SyncStatus.PENDING
            )
        }
        financeDao.upsertAccounts(listOf(account))
        captureNetWorthSnapshot()
        return account
    }

    suspend fun deleteAccount(account: AccountEntity, notifySync: Boolean = true) {
        financeDao.deleteAccount(account)
        captureNetWorthSnapshot()
        if (notifySync) syncSignalBus.notifyDataChanged()
    }

    suspend fun deleteDebt(debt: DebtEntity, notifySync: Boolean = true) {
        financeDao.deleteDebt(debt)
        captureNetWorthSnapshot()
        if (notifySync) syncSignalBus.notifyDataChanged()
    }

    suspend fun recalculateNetWorthSnapshot() {
        captureNetWorthSnapshot()
    }

    suspend fun clearAll(notifySync: Boolean = true) {
        financeDao.clearAccounts()
        financeDao.clearTransfers()
        financeDao.clearRecurringItems()
        financeDao.clearSubscriptionBills()
        financeDao.clearGoals()
        financeDao.clearDebts()
        financeDao.clearEnvelopeBudgets()
        financeDao.clearPaycheckPlans()
        financeDao.clearSplitTransactions()
        financeDao.clearReceiptAttachments()
        financeDao.clearTransactionRules()
        financeDao.clearNetWorthSnapshots()
        if (notifySync) syncSignalBus.notifyDataChanged()
    }

    suspend fun seedDemoDataIfEmpty() {
        if (financeDao.getAccountCount() > 0) return

        val currentMonth = YearMonth.now()
        val today = LocalDate.now()

        financeDao.upsertAccounts(
            listOf(
                AccountEntity(
                    name = "Primary Bank",
                    accountType = AccountType.BANK,
                    currentBalance = 152300.0,
                    institution = "Meezan Bank",
                    syncStatus = SyncStatus.PENDING
                ),
                AccountEntity(
                    name = "Cash Wallet",
                    accountType = AccountType.CASH,
                    currentBalance = 12500.0,
                    syncStatus = SyncStatus.PENDING
                ),
                AccountEntity(
                    name = "Savings Vault",
                    accountType = AccountType.SAVINGS,
                    currentBalance = 82000.0,
                    institution = "Alfalah",
                    syncStatus = SyncStatus.PENDING
                )
            )
        )

        financeDao.upsertSubscriptionBills(
            listOf(
                SubscriptionBillEntity(
                    name = "Netflix",
                    amount = 2499.0,
                    category = com.talent.monthlybudgetkeeper.data.model.TransactionCategory.ENTERTAINMENT,
                    dueDate = today.withDayOfMonth(9),
                    nextChargeDate = today.withDayOfMonth(9),
                    billingCycle = com.talent.monthlybudgetkeeper.data.model.BillingCycle.MONTHLY,
                    reminderDays = 2
                ),
                SubscriptionBillEntity(
                    name = "Internet",
                    amount = 5200.0,
                    category = com.talent.monthlybudgetkeeper.data.model.TransactionCategory.BILLS,
                    dueDate = today.withDayOfMonth(14),
                    nextChargeDate = today.withDayOfMonth(14),
                    billingCycle = com.talent.monthlybudgetkeeper.data.model.BillingCycle.MONTHLY
                )
            )
        )

        financeDao.upsertGoals(
            listOf(
                GoalEntity(
                    name = "Emergency Fund",
                    targetAmount = 300000.0,
                    currentAmount = 120000.0,
                    monthlyContribution = 25000.0
                ),
                GoalEntity(
                    name = "School Fees Sinking Fund",
                    targetAmount = 90000.0,
                    currentAmount = 25000.0,
                    monthlyContribution = 15000.0,
                    isSinkingFund = true
                )
            )
        )

        financeDao.upsertDebts(
            listOf(
                DebtEntity(
                    name = "Car Financing",
                    lender = "Bank",
                    totalAmount = 850000.0,
                    remainingAmount = 310000.0,
                    dueDate = today.plusMonths(18),
                    interestRate = 18.5,
                    minimumPayment = 28000.0,
                    plannedPayment = 35000.0
                )
            )
        )

        financeDao.upsertEnvelopeBudgets(
            listOf(
                EnvelopeBudgetEntity(
                    monthKey = "${currentMonth.year}-${currentMonth.monthValue.toString().padStart(2, '0')}",
                    name = "Groceries",
                    category = com.talent.monthlybudgetkeeper.data.model.TransactionCategory.FOOD,
                    plannedAmount = 22000.0,
                    rolloverAmount = 1800.0,
                    spentAmount = 14500.0
                ),
                EnvelopeBudgetEntity(
                    monthKey = "${currentMonth.year}-${currentMonth.monthValue.toString().padStart(2, '0')}",
                    name = "Commute",
                    category = com.talent.monthlybudgetkeeper.data.model.TransactionCategory.TRANSPORT,
                    plannedAmount = 10000.0,
                    rolloverAmount = 900.0,
                    spentAmount = 4200.0
                )
            )
        )

        financeDao.upsertPaycheckPlans(
            listOf(
                PaycheckPlanEntity(
                    name = "Month-start salary plan",
                    expectedAmount = 185000.0,
                    payday = today.withDayOfMonth(1),
                    allocatedAmount = 163000.0,
                    note = "Rent, bills, groceries, savings, debt payment"
                )
            )
        )

        financeDao.upsertTransactionRules(
            listOf(
                TransactionRuleEntity(
                    keyword = "uber",
                    targetType = com.talent.monthlybudgetkeeper.data.model.TransactionType.EXPENSE,
                    targetCategory = com.talent.monthlybudgetkeeper.data.model.TransactionCategory.TRANSPORT,
                    priority = 10
                ),
                TransactionRuleEntity(
                    keyword = "pharmacy",
                    targetType = com.talent.monthlybudgetkeeper.data.model.TransactionType.EXPENSE,
                    targetCategory = com.talent.monthlybudgetkeeper.data.model.TransactionCategory.HEALTH,
                    priority = 9
                )
            )
        )
        captureNetWorthSnapshot()
    }

    private suspend fun captureNetWorthSnapshot() {
        val userId = resolveUserId()
        val netWorthTotals = FinanceCalculations.calculateNetWorth(
            accounts = financeDao.getAllAccounts(),
            debts = financeDao.getAllDebts()
        )
        val snapshotDate = LocalDate.now()
        financeDao.upsertNetWorthSnapshots(
            listOf(
                NetWorthSnapshotEntity(
                    userId = userId,
                    snapshotDate = snapshotDate,
                    assetTotal = netWorthTotals.assetTotal,
                    liabilityTotal = netWorthTotals.liabilityTotal,
                    netWorth = netWorthTotals.netWorth
                )
            )
        )
    }

    private fun resolveUserId(explicitUserId: String? = null): String {
        return explicitUserId ?: sessionManager.currentUserOrNull()?.id ?: "local"
    }

    private suspend fun normalizeAccounts(accounts: List<AccountEntity>): List<AccountEntity> {
        if (accounts.none { it.remoteId.isNullOrBlank() }) return accounts

        val now = System.currentTimeMillis()
        val normalized = accounts.map { account ->
            if (account.remoteId.isNullOrBlank()) {
                account.copy(
                    remoteId = UUID.randomUUID().toString(),
                    userId = resolveUserId(account.userId),
                    updatedAt = now,
                    syncStatus = SyncStatus.PENDING
                )
            } else {
                account
            }
        }
        financeDao.upsertAccounts(normalized)
        return normalized
    }
}
