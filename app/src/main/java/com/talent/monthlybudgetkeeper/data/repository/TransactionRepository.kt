package com.talent.monthlybudgetkeeper.data.repository

import com.talent.monthlybudgetkeeper.data.auth.SessionManager
import com.talent.monthlybudgetkeeper.data.logic.CycleOpeningIncomePlanner
import com.talent.monthlybudgetkeeper.data.logic.FinanceCalculations
import com.talent.monthlybudgetkeeper.data.local.dao.FinanceDao
import com.talent.monthlybudgetkeeper.data.local.dao.TransactionDao
import com.talent.monthlybudgetkeeper.data.local.entity.AccountEntity
import com.talent.monthlybudgetkeeper.data.local.entity.DebtEntity
import com.talent.monthlybudgetkeeper.data.local.entity.NetWorthSnapshotEntity
import com.talent.monthlybudgetkeeper.data.local.entity.TransactionEntity
import com.talent.monthlybudgetkeeper.data.model.AccountType
import com.talent.monthlybudgetkeeper.data.local.entity.TransactionRuleEntity
import com.talent.monthlybudgetkeeper.data.model.RuleMatchField
import com.talent.monthlybudgetkeeper.data.model.SyncStatus
import com.talent.monthlybudgetkeeper.data.model.TransactionCategory
import com.talent.monthlybudgetkeeper.data.model.TransactionType
import com.talent.monthlybudgetkeeper.data.sync.SyncSignalBus
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlin.math.abs

@Singleton
class TransactionRepository @Inject constructor(
    private val transactionDao: TransactionDao,
    private val financeDao: FinanceDao,
    private val sessionManager: SessionManager,
    private val syncSignalBus: SyncSignalBus
) {
    fun observeTransactions(): Flow<List<TransactionEntity>> = transactionDao.observeTransactions()

    fun observeTransaction(id: Long): Flow<TransactionEntity?> = transactionDao.observeTransaction(id)

    suspend fun getTransaction(id: Long): TransactionEntity? = transactionDao.getTransaction(id)

    suspend fun saveTransaction(transaction: TransactionEntity): Long {
        val now = System.currentTimeMillis()
        val existing = if (transaction.id != 0L) {
            transactionDao.getTransaction(transaction.id)
        } else {
            null
        }
        val ruleAdjusted = applyMatchingRule(transaction)
        val resolvedUserId = ruleAdjusted.userId ?: existing?.userId ?: sessionManager.currentUserOrNull()?.id
        val accountRemoteId = ruleAdjusted.accountRemoteId
            ?: existing?.accountRemoteId
            ?: ensurePrimaryBalanceAccountRemoteId()
        val prepared = if (ruleAdjusted.id == 0L) {
            ruleAdjusted.copy(
                remoteId = ruleAdjusted.remoteId ?: UUID.randomUUID().toString(),
                userId = resolvedUserId,
                accountRemoteId = accountRemoteId,
                createdAt = if (ruleAdjusted.createdAt == 0L) now else ruleAdjusted.createdAt,
                updatedAt = now,
                syncStatus = SyncStatus.PENDING
            )
        } else {
            ruleAdjusted.copy(
                remoteId = ruleAdjusted.remoteId ?: existing?.remoteId ?: UUID.randomUUID().toString(),
                userId = resolvedUserId,
                accountRemoteId = accountRemoteId,
                createdAt = existing?.createdAt ?: ruleAdjusted.createdAt.takeIf { it > 0 } ?: now,
                updatedAt = now,
                syncStatus = SyncStatus.PENDING
            )
        }
        applyTransactionBalanceDelta(
            previous = existing,
            updated = prepared,
            userId = resolvedUserId
        )
        val id = if (prepared.id == 0L) {
            transactionDao.insertTransaction(prepared)
        } else {
            transactionDao.updateTransaction(prepared)
            prepared.id
        }
        captureNetWorthSnapshot(resolvedUserId)
        syncSignalBus.notifyDataChanged()
        return id
    }

    suspend fun deleteTransaction(transaction: TransactionEntity) {
        applyTransactionBalanceDelta(
            previous = transaction,
            updated = null,
            userId = transaction.userId ?: sessionManager.currentUserOrNull()?.id
        )
        transactionDao.deleteTransaction(transaction)
        captureNetWorthSnapshot(transaction.userId ?: sessionManager.currentUserOrNull()?.id)
        syncSignalBus.notifyDataChanged()
    }

    suspend fun clearAll(notifySync: Boolean = true) {
        transactionDao.clearTransactions()
        if (notifySync) syncSignalBus.notifyDataChanged()
    }

    suspend fun getAllTransactions(): List<TransactionEntity> = transactionDao.getAllTransactions()

    suspend fun replaceAllTransactions(
        transactions: List<TransactionEntity>,
        notifySync: Boolean = true
    ) {
        transactionDao.clearTransactions()
        if (transactions.isNotEmpty()) {
            transactionDao.insertTransactions(transactions)
        }
        if (notifySync) syncSignalBus.notifyDataChanged()
    }

    suspend fun upsertTransactions(
        transactions: List<TransactionEntity>,
        notifySync: Boolean = true
    ) {
        if (transactions.isNotEmpty()) {
            transactionDao.upsertTransactions(transactions)
        }
        if (notifySync) syncSignalBus.notifyDataChanged()
    }

    suspend fun seedDemoDataIfEmpty() {
        if (transactionDao.getCount() > 0) return

        val today = LocalDate.now()
        val transactions = listOf(
            TransactionEntity(
                title = "Monthly salary",
                amount = 185000.0,
                type = TransactionType.INCOME,
                category = TransactionCategory.SALARY,
                date = today.withDayOfMonth(1),
                note = "Primary income for the month"
            ),
            TransactionEntity(
                title = "House rent",
                amount = 50000.0,
                type = TransactionType.EXPENSE,
                category = TransactionCategory.RENT,
                date = today.withDayOfMonth(2),
                note = "Apartment rent"
            ),
            TransactionEntity(
                title = "Groceries",
                amount = 14500.0,
                type = TransactionType.EXPENSE,
                category = TransactionCategory.FOOD,
                date = today.withDayOfMonth(4),
                note = "Weekly groceries"
            ),
            TransactionEntity(
                title = "Ride-hailing",
                amount = 4200.0,
                type = TransactionType.EXPENSE,
                category = TransactionCategory.TRANSPORT,
                date = today.withDayOfMonth(7),
                note = "Office commute"
            ),
            TransactionEntity(
                title = "Electricity bill",
                amount = 8700.0,
                type = TransactionType.EXPENSE,
                category = TransactionCategory.BILLS,
                date = today.withDayOfMonth(10),
                note = "Monthly utility payment"
            ),
            TransactionEntity(
                title = "Freelance design project",
                amount = 38000.0,
                type = TransactionType.INCOME,
                category = TransactionCategory.FREELANCE,
                date = today.minusMonths(1).withDayOfMonth(18),
                note = "Website UI design milestone"
            ),
            TransactionEntity(
                title = "Family dinner",
                amount = 6200.0,
                type = TransactionType.EXPENSE,
                category = TransactionCategory.ENTERTAINMENT,
                date = today.minusMonths(1).withDayOfMonth(20),
                note = "Dinner outing"
            ),
            TransactionEntity(
                title = "Medicines",
                amount = 3500.0,
                type = TransactionType.EXPENSE,
                category = TransactionCategory.HEALTH,
                date = today.minusMonths(1).withDayOfMonth(23),
                note = "Pharmacy purchase"
            )
        )

        for (transaction in transactions) {
            transactionDao.insertTransaction(transaction)
        }
    }

    private suspend fun applyMatchingRule(transaction: TransactionEntity): TransactionEntity {
        if (transaction.isTransfer) return transaction

        val rules = financeDao.getActiveTransactionRules()
        val matchingRule = rules.firstOrNull { rule -> matchesRule(transaction, rule) } ?: return transaction

        val shouldReplaceCategory = transaction.category == TransactionCategory.defaultFor(transaction.type)
        return transaction.copy(
            type = matchingRule.targetType,
            category = if (shouldReplaceCategory) matchingRule.targetCategory else transaction.category,
            accountRemoteId = transaction.accountRemoteId ?: matchingRule.targetAccountRemoteId
        )
    }

    private fun matchesRule(
        transaction: TransactionEntity,
        rule: TransactionRuleEntity
    ): Boolean {
        if (!rule.isActive) return false
        if (rule.targetType != transaction.type) return false
        val keyword = rule.keyword.trim().lowercase()
        if (keyword.isBlank()) return false

        val title = transaction.title.lowercase()
        val note = transaction.note.lowercase()
        return when (rule.matchField) {
            RuleMatchField.TITLE -> title.contains(keyword)
            RuleMatchField.NOTE -> note.contains(keyword)
            RuleMatchField.TITLE_OR_NOTE -> title.contains(keyword) || note.contains(keyword)
        }
    }

    suspend fun ensureCycleOpeningIncome(
        cycleStartDate: LocalDate,
        amount: Double,
        title: String = "Cycle opening income"
    ): Long? {
        if (amount <= 0.0) return null
        val target = CycleOpeningIncomePlanner.prepareTransaction(
            existingTransactions = transactionDao.getAllTransactions(),
            cycleStartDate = cycleStartDate,
            amount = amount,
            title = title
        ) ?: return null
        return saveTransaction(target)
    }

    private suspend fun ensurePrimaryBalanceAccountRemoteId(): String {
        val existing = financeDao.getAccountByInstitution(FinanceRepository.SYSTEM_BALANCE_ACCOUNT_MARKER)
        if (existing?.remoteId != null) return existing.remoteId

        val now = System.currentTimeMillis()
        val account = AccountEntity(
            remoteId = UUID.randomUUID().toString(),
            userId = existing?.userId ?: sessionManager.currentUserOrNull()?.id,
            name = FinanceRepository.SYSTEM_BALANCE_ACCOUNT_NAME,
            accountType = AccountType.CASH,
            currentBalance = existing?.currentBalance ?: 0.0,
            currencyCode = existing?.currencyCode ?: "PKR",
            institution = FinanceRepository.SYSTEM_BALANCE_ACCOUNT_MARKER,
            includeInNetWorth = true,
            createdAt = existing?.createdAt ?: now,
            updatedAt = now,
            syncStatus = SyncStatus.PENDING
        )
        financeDao.upsertAccounts(listOf(account))
        return account.remoteId.orEmpty()
    }

    private suspend fun applyTransactionBalanceDelta(
        previous: TransactionEntity?,
        updated: TransactionEntity?,
        userId: String?
    ) {
        val previousEffect = previous.accountBalanceEffect()
        val updatedEffect = updated.accountBalanceEffect()
        val remoteIds = buildSet {
            previousEffect?.remoteId?.let(::add)
            updatedEffect?.remoteId?.let(::add)
        }
        if (remoteIds.isEmpty()) return

        val accountsByRemoteId = financeDao.getAllAccounts()
            .associateBy { it.remoteId.orEmpty() }
            .toMutableMap()
        val now = System.currentTimeMillis()

        previousEffect?.let { effect ->
            val account = accountsByRemoteId[effect.remoteId] ?: return@let
            val reverted = account.copy(
                currentBalance = account.currentBalance - effect.delta,
                updatedAt = now,
                syncStatus = SyncStatus.PENDING
            )
            accountsByRemoteId[effect.remoteId] = reverted
        }
        updatedEffect?.let { effect ->
            val account = accountsByRemoteId[effect.remoteId]
                ?: buildMissingPrimaryBalanceAccount(effect.remoteId, userId, now).also {
                    accountsByRemoteId[effect.remoteId] = it
                }
            val adjusted = account.copy(
                currentBalance = account.currentBalance + effect.delta,
                updatedAt = now,
                syncStatus = SyncStatus.PENDING
            )
            accountsByRemoteId[effect.remoteId] = adjusted
        }

        financeDao.upsertAccounts(accountsByRemoteId.values.toList())
    }

    private fun TransactionEntity?.accountBalanceEffect(): AccountBalanceEffect? {
        if (this == null || isTransfer) return null
        val remoteId = accountRemoteId?.takeIf { it.isNotBlank() } ?: return null
        val delta = when (type) {
            TransactionType.INCOME -> amount
            TransactionType.EXPENSE -> -amount
        }
        return AccountBalanceEffect(remoteId = remoteId, delta = delta)
    }

    private fun buildMissingPrimaryBalanceAccount(
        remoteId: String,
        userId: String?,
        now: Long
    ): AccountEntity {
        return AccountEntity(
            remoteId = remoteId,
            userId = userId ?: sessionManager.currentUserOrNull()?.id,
            name = FinanceRepository.SYSTEM_BALANCE_ACCOUNT_NAME,
            accountType = AccountType.CASH,
            currentBalance = 0.0,
            currencyCode = "PKR",
            institution = FinanceRepository.SYSTEM_BALANCE_ACCOUNT_MARKER,
            includeInNetWorth = true,
            createdAt = now,
            updatedAt = now,
            syncStatus = SyncStatus.PENDING
        )
    }

    private suspend fun captureNetWorthSnapshot(userId: String?) {
        val resolvedUserId = userId ?: sessionManager.currentUserOrNull()?.id ?: "local"
        val netWorthTotals = FinanceCalculations.calculateNetWorth(
            accounts = financeDao.getAllAccounts(),
            debts = financeDao.getAllDebts()
        )
        financeDao.upsertNetWorthSnapshots(
            listOf(
                NetWorthSnapshotEntity(
                    userId = resolvedUserId,
                    snapshotDate = LocalDate.now(),
                    assetTotal = netWorthTotals.assetTotal,
                    liabilityTotal = netWorthTotals.liabilityTotal,
                    netWorth = netWorthTotals.netWorth
                )
            )
        )
    }

    private data class AccountBalanceEffect(
        val remoteId: String,
        val delta: Double
    )
}
