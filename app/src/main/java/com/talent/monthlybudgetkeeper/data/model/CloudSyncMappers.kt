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
import java.time.Instant
import java.time.LocalDate
import java.time.format.DateTimeParseException
import java.util.UUID

fun AppPreferences.toCloudRow(userId: String): CloudUserSettingsRow = CloudUserSettingsRow(
    id = userId,
    userId = userId,
    setupCompleted = setupCompleted,
    currency = currency.code,
    region = region.label,
    monthlyIncome = monthlyIncome,
    monthlyBudgetTarget = monthlyBudgetTarget,
    mainFinancialGoal = mainFinancialGoal,
    cycleType = cycleType.name,
    cycleStartDate = cycleStartDate.toString(),
    nextCycleDate = nextCycleDate.toString(),
    carryForwardRemainingBudget = carryForwardRemainingBudget,
    notificationsEnabled = notificationsEnabled,
    privacyModeEnabled = privacyModeEnabled,
    createdAt = createdAt.toIsoTimestamp(),
    updatedAt = updatedAt.toIsoTimestamp()
)

fun CloudUserSettingsRow.toLocalPreferences(): AppPreferences = AppPreferences(
    id = id,
    userId = userId,
    setupCompleted = setupCompleted,
    currency = CurrencyOption.fromCodeOrDefault(currency, CurrencyOption.PKR),
    region = RegionOption.fromNameOrDefault(
        value = region,
        currency = CurrencyOption.fromCodeOrDefault(currency, CurrencyOption.PKR)
    ),
    monthlyIncome = monthlyIncome,
    monthlyBudgetTarget = monthlyBudgetTarget,
    mainFinancialGoal = mainFinancialGoal,
    cycleType = enumValueOrDefault(cycleType, BudgetCycleType.MONTHLY),
    cycleStartDate = cycleStartDate.toLocalDateOrDefault(LocalDate.now().withDayOfMonth(1)),
    nextCycleDate = nextCycleDate.toLocalDateOrDefault(LocalDate.now().withDayOfMonth(1).plusMonths(1)),
    carryForwardRemainingBudget = carryForwardRemainingBudget,
    notificationsEnabled = notificationsEnabled,
    privacyModeEnabled = privacyModeEnabled,
    createdAt = createdAt.toEpochMillis(),
    updatedAt = updatedAt.toEpochMillis()
)

fun TransactionEntity.toCloudRow(userId: String): CloudTransactionRow = CloudTransactionRow(
    id = remoteId ?: UUID.randomUUID().toString(),
    userId = userId,
    accountRemoteId = accountRemoteId,
    transferRemoteId = transferRemoteId,
    title = title,
    amount = amount,
    type = type.name,
    category = category.name,
    date = date.toString(),
    note = note,
    isTransfer = isTransfer,
    isPlanned = isPlanned,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun CloudTransactionRow.toLocalEntity(localId: Long = 0L): TransactionEntity {
    val parsedType = enumValueOrDefault(type, TransactionType.EXPENSE)
    return TransactionEntity(
        id = localId,
        remoteId = id,
        userId = userId,
        accountRemoteId = accountRemoteId,
        transferRemoteId = transferRemoteId,
        title = title,
        amount = amount,
        type = parsedType,
        category = enumValueOrDefault(category, TransactionCategory.defaultFor(parsedType)),
        date = LocalDate.parse(date),
        note = note,
        isTransfer = isTransfer,
        isPlanned = isPlanned,
        createdAt = createdAt,
        updatedAt = updatedAt,
        syncStatus = SyncStatus.SYNCED
    )
}

fun MonthlyBudgetEntity.toCloudRow(userId: String): CloudMonthlyBudgetRow = CloudMonthlyBudgetRow(
    id = remoteId ?: UUID.randomUUID().toString(),
    userId = userId,
    monthKey = monthKey,
    totalBudget = totalBudget,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun CloudMonthlyBudgetRow.toLocalEntity(): MonthlyBudgetEntity = MonthlyBudgetEntity(
    monthKey = monthKey,
    remoteId = id,
    userId = userId,
    totalBudget = totalBudget,
    createdAt = createdAt,
    updatedAt = updatedAt,
    syncStatus = SyncStatus.SYNCED
)

fun CategoryBudgetEntity.toCloudRow(userId: String): CloudCategoryBudgetRow = CloudCategoryBudgetRow(
    id = remoteId ?: UUID.randomUUID().toString(),
    userId = userId,
    monthKey = monthKey,
    category = category.name,
    limitAmount = limitAmount,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun CloudCategoryBudgetRow.toLocalEntity(localId: Long = 0L): CategoryBudgetEntity = CategoryBudgetEntity(
    id = localId,
    remoteId = id,
    userId = userId,
    monthKey = monthKey,
    category = enumValueOrDefault(category, TransactionCategory.OTHER_EXPENSE),
    limitAmount = limitAmount,
    createdAt = createdAt,
    updatedAt = updatedAt,
    syncStatus = SyncStatus.SYNCED
)

fun AccountEntity.toCloudRow(userId: String): CloudAccountRow = CloudAccountRow(
    id = remoteId ?: UUID.randomUUID().toString(),
    userId = userId,
    name = name,
    accountType = accountType.name,
    currentBalance = currentBalance,
    currencyCode = currencyCode,
    institution = institution,
    includeInNetWorth = includeInNetWorth,
    isArchived = isArchived,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun CloudAccountRow.toLocalEntity(localId: Long = 0L): AccountEntity = AccountEntity(
    id = localId,
    remoteId = id,
    userId = userId,
    name = name,
    accountType = enumValueOrDefault(accountType, AccountType.OTHER),
    currentBalance = currentBalance,
    currencyCode = currencyCode,
    institution = institution,
    includeInNetWorth = includeInNetWorth,
    isArchived = isArchived,
    createdAt = createdAt,
    updatedAt = updatedAt,
    syncStatus = SyncStatus.SYNCED
)

fun TransferEntity.toCloudRow(userId: String): CloudTransferRow = CloudTransferRow(
    id = remoteId ?: UUID.randomUUID().toString(),
    userId = userId,
    fromAccountRemoteId = fromAccountRemoteId,
    toAccountRemoteId = toAccountRemoteId,
    amount = amount,
    date = date.toString(),
    note = note,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun CloudTransferRow.toLocalEntity(localId: Long = 0L): TransferEntity = TransferEntity(
    id = localId,
    remoteId = id,
    userId = userId,
    fromAccountRemoteId = fromAccountRemoteId,
    toAccountRemoteId = toAccountRemoteId,
    amount = amount,
    date = LocalDate.parse(date),
    note = note,
    createdAt = createdAt,
    updatedAt = updatedAt,
    syncStatus = SyncStatus.SYNCED
)

fun RecurringItemEntity.toCloudRow(userId: String): CloudRecurringItemRow = CloudRecurringItemRow(
    id = remoteId ?: UUID.randomUUID().toString(),
    userId = userId,
    accountRemoteId = accountRemoteId,
    title = title,
    amount = amount,
    type = type.name,
    category = category.name,
    startDate = startDate.toString(),
    nextDueDate = nextDueDate.toString(),
    endDate = endDate?.toString(),
    interval = interval.name,
    note = note,
    autoCreateTransaction = autoCreateTransaction,
    isActive = isActive,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun CloudRecurringItemRow.toLocalEntity(localId: Long = 0L): RecurringItemEntity {
    val parsedType = enumValueOrDefault(type, TransactionType.EXPENSE)
    return RecurringItemEntity(
        id = localId,
        remoteId = id,
        userId = userId,
        accountRemoteId = accountRemoteId,
        title = title,
        amount = amount,
        type = parsedType,
        category = enumValueOrDefault(category, TransactionCategory.defaultFor(parsedType)),
        startDate = LocalDate.parse(startDate),
        nextDueDate = LocalDate.parse(nextDueDate),
        endDate = endDate?.let(LocalDate::parse),
        interval = enumValueOrDefault(interval, RecurrenceInterval.MONTHLY),
        note = note,
        autoCreateTransaction = autoCreateTransaction,
        isActive = isActive,
        createdAt = createdAt,
        updatedAt = updatedAt,
        syncStatus = SyncStatus.SYNCED
    )
}

fun SubscriptionBillEntity.toCloudRow(userId: String): CloudSubscriptionBillRow = CloudSubscriptionBillRow(
    id = remoteId ?: UUID.randomUUID().toString(),
    userId = userId,
    accountRemoteId = accountRemoteId,
    name = name,
    amount = amount,
    category = category.name,
    dueDate = dueDate.toString(),
    nextChargeDate = nextChargeDate.toString(),
    billingCycle = billingCycle.name,
    note = note,
    reminderDays = reminderDays,
    isAutoPay = isAutoPay,
    isActive = isActive,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun CloudSubscriptionBillRow.toLocalEntity(localId: Long = 0L): SubscriptionBillEntity = SubscriptionBillEntity(
    id = localId,
    remoteId = id,
    userId = userId,
    accountRemoteId = accountRemoteId,
    name = name,
    amount = amount,
    category = enumValueOrDefault(category, TransactionCategory.BILLS),
    dueDate = LocalDate.parse(dueDate),
    nextChargeDate = LocalDate.parse(nextChargeDate),
    billingCycle = enumValueOrDefault(billingCycle, BillingCycle.MONTHLY),
    note = note,
    reminderDays = reminderDays,
    isAutoPay = isAutoPay,
    isActive = isActive,
    createdAt = createdAt,
    updatedAt = updatedAt,
    syncStatus = SyncStatus.SYNCED
)

fun GoalEntity.toCloudRow(userId: String): CloudGoalRow = CloudGoalRow(
    id = remoteId ?: UUID.randomUUID().toString(),
    userId = userId,
    linkedAccountRemoteId = linkedAccountRemoteId,
    name = name,
    targetAmount = targetAmount,
    currentAmount = currentAmount,
    targetDate = targetDate?.toString(),
    monthlyContribution = monthlyContribution,
    isSinkingFund = isSinkingFund,
    note = note,
    isCompleted = isCompleted,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun CloudGoalRow.toLocalEntity(localId: Long = 0L): GoalEntity = GoalEntity(
    id = localId,
    remoteId = id,
    userId = userId,
    linkedAccountRemoteId = linkedAccountRemoteId,
    name = name,
    targetAmount = targetAmount,
    currentAmount = currentAmount,
    targetDate = targetDate?.let(LocalDate::parse),
    monthlyContribution = monthlyContribution,
    isSinkingFund = isSinkingFund,
    note = note,
    isCompleted = isCompleted,
    createdAt = createdAt,
    updatedAt = updatedAt,
    syncStatus = SyncStatus.SYNCED
)

fun DebtEntity.toCloudRow(userId: String): CloudDebtRow = CloudDebtRow(
    id = remoteId ?: UUID.randomUUID().toString(),
    userId = userId,
    accountRemoteId = accountRemoteId,
    name = name,
    lender = lender,
    totalAmount = totalAmount,
    remainingAmount = remainingAmount,
    dueDate = dueDate?.toString(),
    interestRate = interestRate,
    minimumPayment = minimumPayment,
    plannedPayment = plannedPayment,
    strategy = strategy.name,
    note = note,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun CloudDebtRow.toLocalEntity(localId: Long = 0L): DebtEntity = DebtEntity(
    id = localId,
    remoteId = id,
    userId = userId,
    accountRemoteId = accountRemoteId,
    name = name,
    lender = lender,
    totalAmount = totalAmount,
    remainingAmount = remainingAmount,
    dueDate = dueDate?.let(LocalDate::parse),
    interestRate = interestRate,
    minimumPayment = minimumPayment,
    plannedPayment = plannedPayment,
    strategy = enumValueOrDefault(strategy, DebtStrategy.CUSTOM),
    note = note,
    createdAt = createdAt,
    updatedAt = updatedAt,
    syncStatus = SyncStatus.SYNCED
)

fun EnvelopeBudgetEntity.toCloudRow(userId: String): CloudEnvelopeBudgetRow = CloudEnvelopeBudgetRow(
    id = remoteId ?: UUID.randomUUID().toString(),
    userId = userId,
    monthKey = monthKey,
    name = name,
    category = category?.name,
    plannedAmount = plannedAmount,
    rolloverAmount = rolloverAmount,
    spentAmount = spentAmount,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun CloudEnvelopeBudgetRow.toLocalEntity(localId: Long = 0L): EnvelopeBudgetEntity = EnvelopeBudgetEntity(
    id = localId,
    remoteId = id,
    userId = userId,
    monthKey = monthKey,
    name = name,
    category = category?.let { enumValueOrDefault(it, TransactionCategory.OTHER_EXPENSE) },
    plannedAmount = plannedAmount,
    rolloverAmount = rolloverAmount,
    spentAmount = spentAmount,
    createdAt = createdAt,
    updatedAt = updatedAt,
    syncStatus = SyncStatus.SYNCED
)

fun PaycheckPlanEntity.toCloudRow(userId: String): CloudPaycheckPlanRow = CloudPaycheckPlanRow(
    id = remoteId ?: UUID.randomUUID().toString(),
    userId = userId,
    name = name,
    accountRemoteId = accountRemoteId,
    expectedAmount = expectedAmount,
    payday = payday.toString(),
    allocatedAmount = allocatedAmount,
    note = note,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun CloudPaycheckPlanRow.toLocalEntity(localId: Long = 0L): PaycheckPlanEntity = PaycheckPlanEntity(
    id = localId,
    remoteId = id,
    userId = userId,
    name = name,
    accountRemoteId = accountRemoteId,
    expectedAmount = expectedAmount,
    payday = LocalDate.parse(payday),
    allocatedAmount = allocatedAmount,
    note = note,
    createdAt = createdAt,
    updatedAt = updatedAt,
    syncStatus = SyncStatus.SYNCED
)

fun SplitTransactionEntity.toCloudRow(userId: String): CloudSplitTransactionRow = CloudSplitTransactionRow(
    id = remoteId ?: UUID.randomUUID().toString(),
    userId = userId,
    parentTransactionRemoteId = parentTransactionRemoteId,
    title = title,
    category = category.name,
    amount = amount,
    note = note,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun CloudSplitTransactionRow.toLocalEntity(localId: Long = 0L): SplitTransactionEntity = SplitTransactionEntity(
    id = localId,
    remoteId = id,
    userId = userId,
    parentTransactionRemoteId = parentTransactionRemoteId,
    title = title,
    category = enumValueOrDefault(category, TransactionCategory.OTHER_EXPENSE),
    amount = amount,
    note = note,
    createdAt = createdAt,
    updatedAt = updatedAt,
    syncStatus = SyncStatus.SYNCED
)

fun ReceiptAttachmentEntity.toCloudRow(userId: String): CloudReceiptAttachmentRow = CloudReceiptAttachmentRow(
    id = remoteId ?: UUID.randomUUID().toString(),
    userId = userId,
    transactionRemoteId = transactionRemoteId,
    localUri = localUri,
    fileName = fileName,
    mimeType = mimeType,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun CloudReceiptAttachmentRow.toLocalEntity(localId: Long = 0L): ReceiptAttachmentEntity = ReceiptAttachmentEntity(
    id = localId,
    remoteId = id,
    userId = userId,
    transactionRemoteId = transactionRemoteId,
    localUri = localUri,
    fileName = fileName,
    mimeType = mimeType,
    createdAt = createdAt,
    updatedAt = updatedAt,
    syncStatus = SyncStatus.SYNCED
)

fun TransactionRuleEntity.toCloudRow(userId: String): CloudTransactionRuleRow = CloudTransactionRuleRow(
    id = remoteId ?: UUID.randomUUID().toString(),
    userId = userId,
    keyword = keyword,
    matchField = matchField.name,
    targetType = targetType.name,
    targetCategory = targetCategory.name,
    targetAccountRemoteId = targetAccountRemoteId,
    priority = priority,
    isActive = isActive,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun CloudTransactionRuleRow.toLocalEntity(localId: Long = 0L): TransactionRuleEntity {
    val parsedType = enumValueOrDefault(targetType, TransactionType.EXPENSE)
    return TransactionRuleEntity(
        id = localId,
        remoteId = id,
        userId = userId,
        keyword = keyword,
        matchField = enumValueOrDefault(matchField, RuleMatchField.TITLE_OR_NOTE),
        targetType = parsedType,
        targetCategory = enumValueOrDefault(targetCategory, TransactionCategory.defaultFor(parsedType)),
        targetAccountRemoteId = targetAccountRemoteId,
        priority = priority,
        isActive = isActive,
        createdAt = createdAt,
        updatedAt = updatedAt,
        syncStatus = SyncStatus.SYNCED
    )
}

private inline fun <reified T : Enum<T>> enumValueOrDefault(value: String, default: T): T {
    return enumValues<T>().firstOrNull { it.name.equals(value, ignoreCase = true) } ?: default
}

private fun Long.toIsoTimestamp(): String = Instant.ofEpochMilli(this).toString()

private fun String.toEpochMillis(): Long {
    return runCatching { Instant.parse(this).toEpochMilli() }
        .recoverCatching {
            val millis = this.toLongOrNull()
            when {
                millis != null && millis > 1_000_000_000_000L -> millis
                millis != null -> millis * 1000L
                else -> throw DateTimeParseException("Invalid timestamp", this, 0)
            }
        }
        .getOrElse { System.currentTimeMillis() }
}

private fun String.toLocalDateOrDefault(default: LocalDate): LocalDate {
    return runCatching { LocalDate.parse(this) }.getOrDefault(default)
}
