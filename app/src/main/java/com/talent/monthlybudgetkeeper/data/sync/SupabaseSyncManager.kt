package com.talent.monthlybudgetkeeper.data.sync

import com.talent.monthlybudgetkeeper.data.auth.SessionManager
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
import com.talent.monthlybudgetkeeper.data.model.AppPreferences
import com.talent.monthlybudgetkeeper.data.model.AuthSessionState
import com.talent.monthlybudgetkeeper.data.model.AuthenticatedUser
import com.talent.monthlybudgetkeeper.data.model.CloudAccountRow
import com.talent.monthlybudgetkeeper.data.model.CloudCategoryBudgetRow
import com.talent.monthlybudgetkeeper.data.model.CloudDebtRow
import com.talent.monthlybudgetkeeper.data.model.CloudEnvelopeBudgetRow
import com.talent.monthlybudgetkeeper.data.model.CloudGoalRow
import com.talent.monthlybudgetkeeper.data.model.CloudMonthlyBudgetRow
import com.talent.monthlybudgetkeeper.data.model.CloudPaycheckPlanRow
import com.talent.monthlybudgetkeeper.data.model.CloudProfileRow
import com.talent.monthlybudgetkeeper.data.model.CloudReceiptAttachmentRow
import com.talent.monthlybudgetkeeper.data.model.CloudRecurringItemRow
import com.talent.monthlybudgetkeeper.data.model.CloudSnapshot
import com.talent.monthlybudgetkeeper.data.model.CloudSplitTransactionRow
import com.talent.monthlybudgetkeeper.data.model.CloudSubscriptionBillRow
import com.talent.monthlybudgetkeeper.data.model.CloudTables
import com.talent.monthlybudgetkeeper.data.model.CloudTransactionRow
import com.talent.monthlybudgetkeeper.data.model.CloudTransactionRuleRow
import com.talent.monthlybudgetkeeper.data.model.CloudTransferRow
import com.talent.monthlybudgetkeeper.data.model.CloudUserSettingsRow
import com.talent.monthlybudgetkeeper.data.model.LocalSyncSnapshot
import com.talent.monthlybudgetkeeper.data.model.SyncState
import com.talent.monthlybudgetkeeper.data.model.SyncStatus
import com.talent.monthlybudgetkeeper.data.notifications.NotificationCoordinator
import com.talent.monthlybudgetkeeper.data.notifications.ReminderScheduler
import com.talent.monthlybudgetkeeper.data.security.UserOwnershipValidator
import com.talent.monthlybudgetkeeper.data.model.toCloudRow
import com.talent.monthlybudgetkeeper.data.model.toLocalEntity
import com.talent.monthlybudgetkeeper.data.model.toLocalPreferences
import com.talent.monthlybudgetkeeper.data.repository.BudgetRepository
import com.talent.monthlybudgetkeeper.data.repository.FinanceRepository
import com.talent.monthlybudgetkeeper.data.repository.SettingsRepository
import com.talent.monthlybudgetkeeper.data.repository.TransactionRepository
import com.talent.monthlybudgetkeeper.data.repository.UserProfileRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Singleton
@OptIn(FlowPreview::class)
class SupabaseSyncManager @Inject constructor(
    private val supabaseClient: SupabaseClient,
    private val sessionManager: SessionManager,
    private val settingsRepository: SettingsRepository,
    private val transactionRepository: TransactionRepository,
    private val budgetRepository: BudgetRepository,
    private val financeRepository: FinanceRepository,
    private val userProfileRepository: UserProfileRepository,
    private val notificationCoordinator: NotificationCoordinator,
    private val reminderScheduler: ReminderScheduler,
    syncSignalBus: SyncSignalBus
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _syncState = MutableStateFlow<SyncState>(SyncState.Idle)
    val syncState: StateFlow<SyncState> = _syncState.asStateFlow()

    private var lastSyncedUserId: String? = null

    init {
        scope.launch {
            sessionManager.sessionState.collectLatest { session ->
                when (session) {
                    AuthSessionState.Loading -> Unit
                    AuthSessionState.Unauthenticated -> {
                        lastSyncedUserId = null
                        _syncState.value = SyncState.Idle
                    }
                    is AuthSessionState.Authenticated -> {
                        if (lastSyncedUserId != session.user.id) {
                            runCatching {
                                syncForAuthenticatedUser(session.user)
                            }.onSuccess {
                                lastSyncedUserId = session.user.id
                            }.onFailure { throwable ->
                                _syncState.value = SyncState.Error(
                                    throwable.message ?: "Cloud sync failed."
                                )
                            }
                        }
                    }
                }
            }
        }

        scope.launch {
            syncSignalBus.events
                .debounce(1_500)
                .collectLatest {
                    val state = sessionManager.sessionState.value as? AuthSessionState.Authenticated
                    if (state != null) {
                        runCatching { syncForAuthenticatedUser(state.user) }
                            .onFailure { throwable ->
                                _syncState.value = SyncState.Error(
                                    throwable.message ?: "Background sync failed."
                                )
                            }
                    }
                }
        }
    }

    suspend fun syncNow(): Result<Unit> = runCatching {
        val authenticatedState = sessionManager.sessionState.value as? AuthSessionState.Authenticated
            ?: error("Sign in to sync your budget data.")
        syncForAuthenticatedUser(authenticatedState.user)
    }

    suspend fun clearLocalUserData() {
        val cachedUserId = settingsRepository.getCurrentPreferences().userId
        transactionRepository.clearAll(notifySync = false)
        budgetRepository.clearAll(notifySync = false)
        financeRepository.clearAll(notifySync = false)
        if (!cachedUserId.isNullOrBlank()) {
            settingsRepository.clearUserScopedPreferences(cachedUserId)
        }
        settingsRepository.bindUser(null)
        settingsRepository.resetUserScopedPreferences()
        notificationCoordinator.cancelAllReminderNotifications()
        lastSyncedUserId = null
    }

    suspend fun resetLocalCacheOnly(): Result<Unit> = runCatching {
        val authenticatedState = sessionManager.sessionState.value as? AuthSessionState.Authenticated
            ?: error("Sign in to reset your synced budget data.")
        val user = authenticatedState.user
        val preservedOnboardingCompleted = settingsRepository.getCurrentPreferences().onboardingCompleted

        transactionRepository.clearAll(notifySync = false)
        budgetRepository.clearAll(notifySync = false)
        financeRepository.clearAll(notifySync = false)
        settingsRepository.clearUserScopedPreferences(user.id)
        settingsRepository.bindUser(user.id)
        notificationCoordinator.cancelAllReminderNotifications()
        val remoteSnapshot = fetchRemoteSnapshot(user)
        applyLocalSnapshot(
            remoteSnapshot.toLocalSnapshot(
                userId = user.id,
                onboardingCompleted = preservedOnboardingCompleted
            )
        )
        reminderScheduler.requestImmediateRefresh()
        _syncState.value = SyncState.Success(System.currentTimeMillis())
    }

    suspend fun resetUserDataEverywhere(): Result<Unit> = runCatching {
        val authenticatedState = sessionManager.sessionState.value as? AuthSessionState.Authenticated
            ?: error("Sign in to delete your synced budget data.")
        val user = authenticatedState.user

        _syncState.value = SyncState.Syncing("Deleting your cloud and local data")
        deleteRemoteUserData(user.id)

        transactionRepository.clearAll(notifySync = false)
        budgetRepository.clearAll(notifySync = false)
        financeRepository.clearAll(notifySync = false)
        settingsRepository.clearUserScopedPreferences(user.id)
        settingsRepository.bindUser(null)
        settingsRepository.resetUserScopedPreferences()
        notificationCoordinator.cancelAllReminderNotifications()
        lastSyncedUserId = null
        _syncState.value = SyncState.Success(System.currentTimeMillis())
    }

    private suspend fun syncForAuthenticatedUser(user: AuthenticatedUser) {
        _syncState.value = SyncState.Syncing("Syncing your finance data")
        val currentPreferences = settingsRepository.getCurrentPreferences()
        if (!currentPreferences.userId.isNullOrBlank() && currentPreferences.userId != user.id) {
            clearLocalUserData()
        }
        settingsRepository.bindUser(user.id)
        val reboundPreferences = settingsRepository.getCurrentPreferences()
        if (reboundPreferences.setupResetPending) {
            notificationCoordinator.cancelAllReminderNotifications()
            _syncState.value = SyncState.Success(System.currentTimeMillis())
            return
        }
        if (reboundPreferences.profileName == "My Budget Profile") {
            settingsRepository.updateProfileName(user.displayName, notifySync = false)
        }

        val localSnapshot = loadLocalSnapshot()
        val remoteSnapshot = fetchRemoteSnapshot(user)
        val mergedSnapshot = mergeSnapshots(user, localSnapshot, remoteSnapshot)

        applyLocalSnapshot(mergedSnapshot)
        pushSnapshot(user, mergedSnapshot)
        reminderScheduler.requestImmediateRefresh()

        _syncState.value = SyncState.Success(System.currentTimeMillis())
    }

    private suspend fun loadLocalSnapshot(): LocalSyncSnapshot = LocalSyncSnapshot(
        preferences = settingsRepository.getCurrentPreferences(),
        transactions = transactionRepository.getAllTransactions(),
        monthlyBudgets = budgetRepository.getAllMonthlyBudgets(),
        categoryBudgets = budgetRepository.getAllCategoryBudgets(),
        accounts = financeRepository.getAllAccounts(),
        transfers = financeRepository.getAllTransfers(),
        recurringItems = financeRepository.getAllRecurringItems(),
        subscriptionBills = financeRepository.getAllSubscriptionBills(),
        goals = financeRepository.getAllGoals(),
        debts = financeRepository.getAllDebts(),
        envelopeBudgets = financeRepository.getAllEnvelopeBudgets(),
        paycheckPlans = financeRepository.getAllPaycheckPlans(),
        splitTransactions = financeRepository.getAllSplitTransactions(),
        receiptAttachments = financeRepository.getAllReceiptAttachments(),
        transactionRules = financeRepository.getAllTransactionRules()
    )

    private suspend fun applyLocalSnapshot(snapshot: LocalSyncSnapshot) {
        settingsRepository.applyPreferences(snapshot.preferences)
        transactionRepository.replaceAllTransactions(snapshot.transactions, notifySync = false)
        budgetRepository.replaceAllBudgets(
            monthlyBudgets = snapshot.monthlyBudgets,
            categoryBudgets = snapshot.categoryBudgets,
            notifySync = false
        )
        financeRepository.replaceAllFinanceData(
            accounts = snapshot.accounts,
            transfers = snapshot.transfers,
            recurringItems = snapshot.recurringItems,
            subscriptionBills = snapshot.subscriptionBills,
            goals = snapshot.goals,
            debts = snapshot.debts,
            envelopeBudgets = snapshot.envelopeBudgets,
            paycheckPlans = snapshot.paycheckPlans,
            splitTransactions = snapshot.splitTransactions,
            receiptAttachments = snapshot.receiptAttachments,
            transactionRules = snapshot.transactionRules,
            notifySync = false
        )
    }

    private suspend fun fetchRemoteSnapshot(user: AuthenticatedUser): CloudSnapshot {
        val profile = supabaseClient.from(CloudTables.PROFILES).select {
            filter { eq("user_id", user.id) }
        }.decodeSingleOrNull<CloudProfileRow>()?.also {
            UserOwnershipValidator.requireOwnedByUser(user.id, it.userId, CloudTables.PROFILES)
        }

        val preferences = userProfileRepository.getUserSettings(user.id)?.also {
            UserOwnershipValidator.requireOwnedByUser(user.id, it.userId, CloudTables.USER_SETTINGS)
        }

        return CloudSnapshot(
            profile = profile,
            preferences = preferences,
            transactions = requireOwnedRows(user.id, CloudTables.TRANSACTIONS, supabaseClient.from(CloudTables.TRANSACTIONS).select {
                filter { eq("user_id", user.id) }
            }.decodeList<CloudTransactionRow>(), CloudTransactionRow::userId),
            monthlyBudgets = requireOwnedRows(user.id, CloudTables.MONTHLY_BUDGETS, supabaseClient.from(CloudTables.MONTHLY_BUDGETS).select {
                filter { eq("user_id", user.id) }
            }.decodeList<CloudMonthlyBudgetRow>(), CloudMonthlyBudgetRow::userId),
            categoryBudgets = requireOwnedRows(user.id, CloudTables.CATEGORY_BUDGETS, supabaseClient.from(CloudTables.CATEGORY_BUDGETS).select {
                filter { eq("user_id", user.id) }
            }.decodeList<CloudCategoryBudgetRow>(), CloudCategoryBudgetRow::userId),
            accounts = requireOwnedRows(user.id, CloudTables.ACCOUNTS, supabaseClient.from(CloudTables.ACCOUNTS).select {
                filter { eq("user_id", user.id) }
            }.decodeList<CloudAccountRow>(), CloudAccountRow::userId),
            transfers = requireOwnedRows(user.id, CloudTables.TRANSFERS, supabaseClient.from(CloudTables.TRANSFERS).select {
                filter { eq("user_id", user.id) }
            }.decodeList<CloudTransferRow>(), CloudTransferRow::userId),
            recurringItems = requireOwnedRows(user.id, CloudTables.RECURRING_ITEMS, supabaseClient.from(CloudTables.RECURRING_ITEMS).select {
                filter { eq("user_id", user.id) }
            }.decodeList<CloudRecurringItemRow>(), CloudRecurringItemRow::userId),
            subscriptionBills = requireOwnedRows(user.id, CloudTables.SUBSCRIPTION_BILLS, supabaseClient.from(CloudTables.SUBSCRIPTION_BILLS).select {
                filter { eq("user_id", user.id) }
            }.decodeList<CloudSubscriptionBillRow>(), CloudSubscriptionBillRow::userId),
            goals = requireOwnedRows(user.id, CloudTables.GOALS, supabaseClient.from(CloudTables.GOALS).select {
                filter { eq("user_id", user.id) }
            }.decodeList<CloudGoalRow>(), CloudGoalRow::userId),
            debts = requireOwnedRows(user.id, CloudTables.DEBTS, supabaseClient.from(CloudTables.DEBTS).select {
                filter { eq("user_id", user.id) }
            }.decodeList<CloudDebtRow>(), CloudDebtRow::userId),
            envelopeBudgets = requireOwnedRows(user.id, CloudTables.ENVELOPE_BUDGETS, supabaseClient.from(CloudTables.ENVELOPE_BUDGETS).select {
                filter { eq("user_id", user.id) }
            }.decodeList<CloudEnvelopeBudgetRow>(), CloudEnvelopeBudgetRow::userId),
            paycheckPlans = requireOwnedRows(user.id, CloudTables.PAYCHECK_PLANS, supabaseClient.from(CloudTables.PAYCHECK_PLANS).select {
                filter { eq("user_id", user.id) }
            }.decodeList<CloudPaycheckPlanRow>(), CloudPaycheckPlanRow::userId),
            splitTransactions = requireOwnedRows(user.id, CloudTables.SPLIT_TRANSACTIONS, supabaseClient.from(CloudTables.SPLIT_TRANSACTIONS).select {
                filter { eq("user_id", user.id) }
            }.decodeList<CloudSplitTransactionRow>(), CloudSplitTransactionRow::userId),
            receiptAttachments = requireOwnedRows(user.id, CloudTables.RECEIPT_ATTACHMENTS, supabaseClient.from(CloudTables.RECEIPT_ATTACHMENTS).select {
                filter { eq("user_id", user.id) }
            }.decodeList<CloudReceiptAttachmentRow>(), CloudReceiptAttachmentRow::userId),
            transactionRules = requireOwnedRows(user.id, CloudTables.TRANSACTION_RULES, supabaseClient.from(CloudTables.TRANSACTION_RULES).select {
                filter { eq("user_id", user.id) }
            }.decodeList<CloudTransactionRuleRow>(), CloudTransactionRuleRow::userId)
        )
    }

    private suspend fun pushSnapshot(user: AuthenticatedUser, snapshot: LocalSyncSnapshot) {
        val profileCreatedAt = snapshot.preferences.createdAt
        val profileUpdatedAt = maxOf(
            snapshot.preferences.updatedAt,
            snapshot.transactions.maxOfOrNull(TransactionEntity::updatedAt) ?: snapshot.preferences.updatedAt
        )
        supabaseClient.from(CloudTables.PROFILES).upsert(
            CloudProfileRow(
                id = user.id,
                userId = user.id,
                email = user.email,
                fullName = snapshot.preferences.profileName.ifBlank { user.displayName },
                createdAt = profileCreatedAt,
                updatedAt = profileUpdatedAt
            )
        )

        userProfileRepository.upsertUserSettings(snapshot.preferences.toCloudRow(user.id))
        val transactionRows = snapshot.transactions.map { it.toCloudRow(user.id) }
        if (transactionRows.isNotEmpty()) supabaseClient.from(CloudTables.TRANSACTIONS).upsert(transactionRows)
        val monthlyBudgetRows = snapshot.monthlyBudgets.map { it.toCloudRow(user.id) }
        if (monthlyBudgetRows.isNotEmpty()) supabaseClient.from(CloudTables.MONTHLY_BUDGETS).upsert(monthlyBudgetRows)
        val categoryBudgetRows = snapshot.categoryBudgets.map { it.toCloudRow(user.id) }
        if (categoryBudgetRows.isNotEmpty()) supabaseClient.from(CloudTables.CATEGORY_BUDGETS).upsert(categoryBudgetRows)
        val accountRows = snapshot.accounts.map { it.toCloudRow(user.id) }
        if (accountRows.isNotEmpty()) supabaseClient.from(CloudTables.ACCOUNTS).upsert(accountRows)
        val transferRows = snapshot.transfers.map { it.toCloudRow(user.id) }
        if (transferRows.isNotEmpty()) supabaseClient.from(CloudTables.TRANSFERS).upsert(transferRows)
        val recurringRows = snapshot.recurringItems.map { it.toCloudRow(user.id) }
        if (recurringRows.isNotEmpty()) supabaseClient.from(CloudTables.RECURRING_ITEMS).upsert(recurringRows)
        val billRows = snapshot.subscriptionBills.map { it.toCloudRow(user.id) }
        if (billRows.isNotEmpty()) supabaseClient.from(CloudTables.SUBSCRIPTION_BILLS).upsert(billRows)
        val goalRows = snapshot.goals.map { it.toCloudRow(user.id) }
        if (goalRows.isNotEmpty()) supabaseClient.from(CloudTables.GOALS).upsert(goalRows)
        val debtRows = snapshot.debts.map { it.toCloudRow(user.id) }
        if (debtRows.isNotEmpty()) supabaseClient.from(CloudTables.DEBTS).upsert(debtRows)
        val envelopeRows = snapshot.envelopeBudgets.map { it.toCloudRow(user.id) }
        if (envelopeRows.isNotEmpty()) supabaseClient.from(CloudTables.ENVELOPE_BUDGETS).upsert(envelopeRows)
        val paycheckRows = snapshot.paycheckPlans.map { it.toCloudRow(user.id) }
        if (paycheckRows.isNotEmpty()) supabaseClient.from(CloudTables.PAYCHECK_PLANS).upsert(paycheckRows)
        val splitRows = snapshot.splitTransactions.map { it.toCloudRow(user.id) }
        if (splitRows.isNotEmpty()) supabaseClient.from(CloudTables.SPLIT_TRANSACTIONS).upsert(splitRows)
        val attachmentRows = snapshot.receiptAttachments.map { it.toCloudRow(user.id) }
        if (attachmentRows.isNotEmpty()) supabaseClient.from(CloudTables.RECEIPT_ATTACHMENTS).upsert(attachmentRows)
        val ruleRows = snapshot.transactionRules.map { it.toCloudRow(user.id) }
        if (ruleRows.isNotEmpty()) supabaseClient.from(CloudTables.TRANSACTION_RULES).upsert(ruleRows)

        pruneRemoteRows(CloudTables.TRANSACTIONS, user.id, snapshot.transactions.mapNotNull(TransactionEntity::remoteId))
        pruneRemoteRows(CloudTables.MONTHLY_BUDGETS, user.id, snapshot.monthlyBudgets.mapNotNull(MonthlyBudgetEntity::remoteId))
        pruneRemoteRows(CloudTables.CATEGORY_BUDGETS, user.id, snapshot.categoryBudgets.mapNotNull(CategoryBudgetEntity::remoteId))
        pruneRemoteRows(CloudTables.ACCOUNTS, user.id, snapshot.accounts.mapNotNull(AccountEntity::remoteId))
        pruneRemoteRows(CloudTables.TRANSFERS, user.id, snapshot.transfers.mapNotNull(TransferEntity::remoteId))
        pruneRemoteRows(CloudTables.RECURRING_ITEMS, user.id, snapshot.recurringItems.mapNotNull(RecurringItemEntity::remoteId))
        pruneRemoteRows(CloudTables.SUBSCRIPTION_BILLS, user.id, snapshot.subscriptionBills.mapNotNull(SubscriptionBillEntity::remoteId))
        pruneRemoteRows(CloudTables.GOALS, user.id, snapshot.goals.mapNotNull(GoalEntity::remoteId))
        pruneRemoteRows(CloudTables.DEBTS, user.id, snapshot.debts.mapNotNull(DebtEntity::remoteId))
        pruneRemoteRows(CloudTables.ENVELOPE_BUDGETS, user.id, snapshot.envelopeBudgets.mapNotNull(EnvelopeBudgetEntity::remoteId))
        pruneRemoteRows(CloudTables.PAYCHECK_PLANS, user.id, snapshot.paycheckPlans.mapNotNull(PaycheckPlanEntity::remoteId))
        pruneRemoteRows(CloudTables.SPLIT_TRANSACTIONS, user.id, snapshot.splitTransactions.mapNotNull(SplitTransactionEntity::remoteId))
        pruneRemoteRows(CloudTables.RECEIPT_ATTACHMENTS, user.id, snapshot.receiptAttachments.mapNotNull(ReceiptAttachmentEntity::remoteId))
        pruneRemoteRows(CloudTables.TRANSACTION_RULES, user.id, snapshot.transactionRules.mapNotNull(TransactionRuleEntity::remoteId))
    }

    private suspend fun pruneRemoteRows(table: String, userId: String, keepIds: List<String>) {
        val remoteIds = supabaseClient.from(table)
            .select { filter { eq("user_id", userId) } }
            .decodeList<CloudIdOnlyRow>()
            .map(CloudIdOnlyRow::id)

        remoteIds.filterNot(keepIds.toSet()::contains).forEach { staleId ->
            supabaseClient.from(table).delete {
                filter {
                    eq("user_id", userId)
                    eq("id", staleId)
                }
            }
        }
    }

    private suspend fun deleteRemoteUserData(userId: String) {
        deleteRemoteRows(CloudTables.RECEIPT_ATTACHMENTS, userId)
        deleteRemoteRows(CloudTables.SPLIT_TRANSACTIONS, userId)
        deleteRemoteRows(CloudTables.TRANSACTION_RULES, userId)
        deleteRemoteRows(CloudTables.TRANSACTIONS, userId)
        deleteRemoteRows(CloudTables.TRANSFERS, userId)
        deleteRemoteRows(CloudTables.CATEGORY_BUDGETS, userId)
        deleteRemoteRows(CloudTables.MONTHLY_BUDGETS, userId)
        deleteRemoteRows(CloudTables.ENVELOPE_BUDGETS, userId)
        deleteRemoteRows(CloudTables.PAYCHECK_PLANS, userId)
        deleteRemoteRows(CloudTables.RECURRING_ITEMS, userId)
        deleteRemoteRows(CloudTables.SUBSCRIPTION_BILLS, userId)
        deleteRemoteRows(CloudTables.GOALS, userId)
        deleteRemoteRows(CloudTables.DEBTS, userId)
        deleteRemoteRows(CloudTables.ACCOUNTS, userId)
        deleteRemoteRows(CloudTables.USER_SETTINGS, userId)
        deleteRemoteRows(CloudTables.PROFILES, userId)
    }

    private suspend fun deleteRemoteRows(table: String, userId: String) {
        supabaseClient.from(table).delete {
            filter {
                eq("user_id", userId)
            }
        }
    }

    private fun CloudSnapshot.toLocalSnapshot(
        userId: String,
        onboardingCompleted: Boolean
    ): LocalSyncSnapshot {
        val remotePreferences = preferences?.toLocalPreferences()?.copy(
            id = userId,
            userId = userId,
            onboardingCompleted = onboardingCompleted
        ) ?: AppPreferences(
            id = userId,
            userId = userId,
            onboardingCompleted = onboardingCompleted
        )

        return LocalSyncSnapshot(
            preferences = remotePreferences,
            transactions = transactions.map { it.toLocalEntity().copy(userId = userId) },
            monthlyBudgets = monthlyBudgets.map { it.toLocalEntity().copy(userId = userId) },
            categoryBudgets = categoryBudgets.map { it.toLocalEntity().copy(userId = userId) },
            accounts = accounts.map { it.toLocalEntity().copy(userId = userId) },
            transfers = transfers.map { it.toLocalEntity().copy(userId = userId) },
            recurringItems = recurringItems.map { it.toLocalEntity().copy(userId = userId) },
            subscriptionBills = subscriptionBills.map { it.toLocalEntity().copy(userId = userId) },
            goals = goals.map { it.toLocalEntity().copy(userId = userId) },
            debts = debts.map { it.toLocalEntity().copy(userId = userId) },
            envelopeBudgets = envelopeBudgets.map { it.toLocalEntity().copy(userId = userId) },
            paycheckPlans = paycheckPlans.map { it.toLocalEntity().copy(userId = userId) },
            splitTransactions = splitTransactions.map { it.toLocalEntity().copy(userId = userId) },
            receiptAttachments = receiptAttachments.map { it.toLocalEntity().copy(userId = userId) },
            transactionRules = transactionRules.map { it.toLocalEntity().copy(userId = userId) }
        )
    }

    private fun mergeSnapshots(
        user: AuthenticatedUser,
        local: LocalSyncSnapshot,
        remote: CloudSnapshot
    ): LocalSyncSnapshot {
        val mergedPreferences = mergePreferences(
            userId = user.id,
            userDisplayName = user.displayName,
            local = local.preferences,
            remote = remote.preferences,
            remoteHasFinanceData = remote.hasRemoteData
        )
        return LocalSyncSnapshot(
            preferences = mergedPreferences,
            transactions = mergeLists(
                user.id, local.transactions, remote.transactions,
                { it.remoteId }, { it.updatedAt }, { "${it.title}|${it.amount}|${it.type}|${it.category}|${it.date}|${it.note}|${it.accountRemoteId}" },
                { it.id }, { it.updatedAt }, { "${it.title}|${it.amount}|${it.type}|${it.category}|${it.date}|${it.note}|${it.accountRemoteId}" },
                { remoteRow, existing -> remoteRow.toLocalEntity(existing?.id ?: 0L) },
                { item, remoteIdValue, boundUserId -> item.copy(remoteId = remoteIdValue, userId = boundUserId, syncStatus = SyncStatus.SYNCED) }
            ),
            monthlyBudgets = mergeLists(
                user.id, local.monthlyBudgets, remote.monthlyBudgets,
                { it.remoteId }, { it.updatedAt }, { it.monthKey },
                { it.id }, { it.updatedAt }, { it.monthKey },
                { remoteRow, _ -> remoteRow.toLocalEntity() },
                { item, remoteIdValue, boundUserId -> item.copy(remoteId = remoteIdValue, userId = boundUserId, syncStatus = SyncStatus.SYNCED) }
            ),
            categoryBudgets = mergeLists(
                user.id, local.categoryBudgets, remote.categoryBudgets,
                { it.remoteId }, { it.updatedAt }, { "${it.monthKey}|${it.category}" },
                { it.id }, { it.updatedAt }, { "${it.monthKey}|${it.category}" },
                { remoteRow, existing -> remoteRow.toLocalEntity(existing?.id ?: 0L) },
                { item, remoteIdValue, boundUserId -> item.copy(remoteId = remoteIdValue, userId = boundUserId, syncStatus = SyncStatus.SYNCED) }
            ),
            accounts = mergeLists(
                user.id, local.accounts, remote.accounts,
                { it.remoteId }, { it.updatedAt }, { "${it.name}|${it.accountType}|${it.currencyCode}|${it.institution}" },
                { it.id }, { it.updatedAt }, { "${it.name}|${it.accountType}|${it.currencyCode}|${it.institution}" },
                { remoteRow, existing -> remoteRow.toLocalEntity(existing?.id ?: 0L) },
                { item, remoteIdValue, boundUserId -> item.copy(remoteId = remoteIdValue, userId = boundUserId, syncStatus = SyncStatus.SYNCED) }
            ),
            transfers = mergeLists(
                user.id, local.transfers, remote.transfers,
                { it.remoteId }, { it.updatedAt }, { "${it.fromAccountRemoteId}|${it.toAccountRemoteId}|${it.amount}|${it.date}" },
                { it.id }, { it.updatedAt }, { "${it.fromAccountRemoteId}|${it.toAccountRemoteId}|${it.amount}|${it.date}" },
                { remoteRow, existing -> remoteRow.toLocalEntity(existing?.id ?: 0L) },
                { item, remoteIdValue, boundUserId -> item.copy(remoteId = remoteIdValue, userId = boundUserId, syncStatus = SyncStatus.SYNCED) }
            ),
            recurringItems = mergeLists(
                user.id, local.recurringItems, remote.recurringItems,
                { it.remoteId }, { it.updatedAt }, { "${it.title}|${it.amount}|${it.type}|${it.category}|${it.startDate}|${it.interval}" },
                { it.id }, { it.updatedAt }, { "${it.title}|${it.amount}|${it.type}|${it.category}|${it.startDate}|${it.interval}" },
                { remoteRow, existing -> remoteRow.toLocalEntity(existing?.id ?: 0L) },
                { item, remoteIdValue, boundUserId -> item.copy(remoteId = remoteIdValue, userId = boundUserId, syncStatus = SyncStatus.SYNCED) }
            ),
            subscriptionBills = mergeLists(
                user.id, local.subscriptionBills, remote.subscriptionBills,
                { it.remoteId }, { it.updatedAt }, { "${it.name}|${it.amount}|${it.category}|${it.nextChargeDate}|${it.billingCycle}" },
                { it.id }, { it.updatedAt }, { "${it.name}|${it.amount}|${it.category}|${it.nextChargeDate}|${it.billingCycle}" },
                { remoteRow, existing -> remoteRow.toLocalEntity(existing?.id ?: 0L) },
                { item, remoteIdValue, boundUserId -> item.copy(remoteId = remoteIdValue, userId = boundUserId, syncStatus = SyncStatus.SYNCED) }
            ),
            goals = mergeLists(
                user.id, local.goals, remote.goals,
                { it.remoteId }, { it.updatedAt }, { "${it.name}|${it.targetAmount}|${it.targetDate}|${it.isSinkingFund}" },
                { it.id }, { it.updatedAt }, { "${it.name}|${it.targetAmount}|${it.targetDate}|${it.isSinkingFund}" },
                { remoteRow, existing -> remoteRow.toLocalEntity(existing?.id ?: 0L) },
                { item, remoteIdValue, boundUserId -> item.copy(remoteId = remoteIdValue, userId = boundUserId, syncStatus = SyncStatus.SYNCED) }
            ),
            debts = mergeLists(
                user.id, local.debts, remote.debts,
                { it.remoteId }, { it.updatedAt }, { "${it.name}|${it.lender}|${it.totalAmount}" },
                { it.id }, { it.updatedAt }, { "${it.name}|${it.lender}|${it.totalAmount}" },
                { remoteRow, existing -> remoteRow.toLocalEntity(existing?.id ?: 0L) },
                { item, remoteIdValue, boundUserId -> item.copy(remoteId = remoteIdValue, userId = boundUserId, syncStatus = SyncStatus.SYNCED) }
            ),
            envelopeBudgets = mergeLists(
                user.id, local.envelopeBudgets, remote.envelopeBudgets,
                { it.remoteId }, { it.updatedAt }, { "${it.monthKey}|${it.name}" },
                { it.id }, { it.updatedAt }, { "${it.monthKey}|${it.name}" },
                { remoteRow, existing -> remoteRow.toLocalEntity(existing?.id ?: 0L) },
                { item, remoteIdValue, boundUserId -> item.copy(remoteId = remoteIdValue, userId = boundUserId, syncStatus = SyncStatus.SYNCED) }
            ),
            paycheckPlans = mergeLists(
                user.id, local.paycheckPlans, remote.paycheckPlans,
                { it.remoteId }, { it.updatedAt }, { "${it.name}|${it.payday}|${it.expectedAmount}" },
                { it.id }, { it.updatedAt }, { "${it.name}|${it.payday}|${it.expectedAmount}" },
                { remoteRow, existing -> remoteRow.toLocalEntity(existing?.id ?: 0L) },
                { item, remoteIdValue, boundUserId -> item.copy(remoteId = remoteIdValue, userId = boundUserId, syncStatus = SyncStatus.SYNCED) }
            ),
            splitTransactions = mergeLists(
                user.id, local.splitTransactions, remote.splitTransactions,
                { it.remoteId }, { it.updatedAt }, { "${it.parentTransactionRemoteId}|${it.title}|${it.amount}|${it.category}" },
                { it.id }, { it.updatedAt }, { "${it.parentTransactionRemoteId}|${it.title}|${it.amount}|${it.category}" },
                { remoteRow, existing -> remoteRow.toLocalEntity(existing?.id ?: 0L) },
                { item, remoteIdValue, boundUserId -> item.copy(remoteId = remoteIdValue, userId = boundUserId, syncStatus = SyncStatus.SYNCED) }
            ),
            receiptAttachments = mergeLists(
                user.id, local.receiptAttachments, remote.receiptAttachments,
                { it.remoteId }, { it.updatedAt }, { "${it.transactionRemoteId}|${it.fileName}|${it.localUri}" },
                { it.id }, { it.updatedAt }, { "${it.transactionRemoteId}|${it.fileName}|${it.localUri}" },
                { remoteRow, existing -> remoteRow.toLocalEntity(existing?.id ?: 0L) },
                { item, remoteIdValue, boundUserId -> item.copy(remoteId = remoteIdValue, userId = boundUserId, syncStatus = SyncStatus.SYNCED) }
            ),
            transactionRules = mergeLists(
                user.id, local.transactionRules, remote.transactionRules,
                { it.remoteId }, { it.updatedAt }, { "${it.keyword}|${it.targetType}|${it.targetCategory}|${it.priority}" },
                { it.id }, { it.updatedAt }, { "${it.keyword}|${it.targetType}|${it.targetCategory}|${it.priority}" },
                { remoteRow, existing -> remoteRow.toLocalEntity(existing?.id ?: 0L) },
                { item, remoteIdValue, boundUserId -> item.copy(remoteId = remoteIdValue, userId = boundUserId, syncStatus = SyncStatus.SYNCED) }
            )
        )
    }

    private fun mergePreferences(
        userId: String,
        userDisplayName: String,
        local: AppPreferences,
        remote: CloudUserSettingsRow?,
        remoteHasFinanceData: Boolean
    ): AppPreferences {
        val localBound = local.copy(id = userId, userId = userId)
        val remoteLocal = remote?.toLocalPreferences()
        val localLooksLikeFreshShell = localBound.setupCompleted.not() &&
            localBound.monthlyIncome == 0.0 &&
            localBound.monthlyBudgetTarget == 0.0 &&
            localBound.mainFinancialGoal == "Track spending" &&
            (localBound.profileName == "My Budget Profile" || localBound.profileName == userDisplayName) &&
            !localBound.notificationsEnabled &&
            !localBound.privacyModeEnabled
        val merged = when {
            remoteLocal == null -> localBound
            localLooksLikeFreshShell -> remoteLocal.copy(id = userId, userId = userId)
            localBound.updatedAt >= remoteLocal.updatedAt -> localBound
            else -> remoteLocal.copy(id = userId, userId = userId)
        }
        val inferredSetupCompleted = (remoteLocal?.setupCompleted == true) ||
            merged.setupCompleted ||
            remoteHasFinanceData ||
            (remoteLocal?.monthlyIncome ?: 0.0) > 0.0 ||
            (remoteLocal?.monthlyBudgetTarget ?: 0.0) > 0.0
        return merged.copy(
            onboardingCompleted = local.onboardingCompleted,
            setupCompleted = inferredSetupCompleted,
            darkMode = local.darkMode,
            profileName = local.profileName,
            weekStart = local.weekStart,
            billRemindersEnabled = local.billRemindersEnabled,
            recurringRemindersEnabled = local.recurringRemindersEnabled,
            debtRemindersEnabled = local.debtRemindersEnabled,
            budgetAlertsEnabled = local.budgetAlertsEnabled,
            goalRemindersEnabled = local.goalRemindersEnabled,
            reminderLeadDays = local.reminderLeadDays,
            appLockEnabled = local.appLockEnabled,
            biometricLockEnabled = local.biometricLockEnabled,
            pinHash = local.pinHash
        )
    }

    private fun <L, R> mergeLists(
        userId: String,
        localItems: List<L>,
        remoteItems: List<R>,
        localRemoteId: (L) -> String?,
        localUpdatedAt: (L) -> Long,
        localKey: (L) -> String,
        remoteId: (R) -> String,
        remoteUpdatedAt: (R) -> Long,
        remoteKey: (R) -> String,
        remoteToLocal: (R, L?) -> L,
        normalizeLocal: (L, String, String) -> L
    ): List<L> {
        val remoteById = remoteItems.associateBy(remoteId)
        val remoteByKey = remoteItems.associateBy(remoteKey)
        val matchedRemoteIds = mutableSetOf<String>()
        val merged = mutableListOf<L>()

        localItems.forEach { localItem ->
            val matchedRemote = localRemoteId(localItem)?.let(remoteById::get)
                ?: remoteByKey[localKey(localItem)]

            if (matchedRemote == null) {
                merged += normalizeLocal(localItem, localRemoteId(localItem) ?: UUID.randomUUID().toString(), userId)
            } else {
                val matchedRemoteId = remoteId(matchedRemote)
                matchedRemoteIds += matchedRemoteId
                val winner = if (remoteUpdatedAt(matchedRemote) > localUpdatedAt(localItem)) {
                    remoteToLocal(matchedRemote, localItem)
                } else {
                    normalizeLocal(localItem, matchedRemoteId, userId)
                }
                merged += winner
            }
        }

        remoteItems.filterNot { matchedRemoteIds.contains(remoteId(it)) }
            .forEach { merged += remoteToLocal(it, null) }

        return merged
    }

    private fun <T> requireOwnedRows(
        expectedUserId: String,
        table: String,
        rows: List<T>,
        userIdSelector: (T) -> String
    ): List<T> {
        rows.forEach { row ->
            UserOwnershipValidator.requireOwnedByUser(expectedUserId, userIdSelector(row), table)
        }
        return rows
    }

    @Serializable
    private data class CloudIdOnlyRow(@SerialName("id") val id: String)
}
