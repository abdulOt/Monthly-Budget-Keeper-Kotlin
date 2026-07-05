package com.talent.monthlybudgetkeeper.data.local

import androidx.room.TypeConverter
import com.talent.monthlybudgetkeeper.data.model.AccountType
import com.talent.monthlybudgetkeeper.data.model.BillingCycle
import com.talent.monthlybudgetkeeper.data.model.DebtStrategy
import com.talent.monthlybudgetkeeper.data.model.RecurrenceInterval
import com.talent.monthlybudgetkeeper.data.model.RuleMatchField
import com.talent.monthlybudgetkeeper.data.model.SyncStatus
import com.talent.monthlybudgetkeeper.data.model.TransactionCategory
import com.talent.monthlybudgetkeeper.data.model.TransactionType
import java.time.LocalDate

class Converters {
    @TypeConverter
    fun fromLocalDate(value: LocalDate?): String? = value?.toString()

    @TypeConverter
    fun toLocalDate(value: String?): LocalDate? = value?.let(LocalDate::parse)

    @TypeConverter
    fun fromTransactionType(value: TransactionType?): String? = value?.name

    @TypeConverter
    fun toTransactionType(value: String?): TransactionType? = value?.let(TransactionType::valueOf)

    @TypeConverter
    fun fromTransactionCategory(value: TransactionCategory?): String? = value?.name

    @TypeConverter
    fun toTransactionCategory(value: String?): TransactionCategory? = value?.let(TransactionCategory::valueOf)

    @TypeConverter
    fun fromSyncStatus(value: SyncStatus?): String? = value?.name

    @TypeConverter
    fun toSyncStatus(value: String?): SyncStatus? = value?.let(SyncStatus::valueOf)

    @TypeConverter
    fun fromAccountType(value: AccountType?): String? = value?.name

    @TypeConverter
    fun toAccountType(value: String?): AccountType? = value?.let(AccountType::valueOf)

    @TypeConverter
    fun fromRecurrenceInterval(value: RecurrenceInterval?): String? = value?.name

    @TypeConverter
    fun toRecurrenceInterval(value: String?): RecurrenceInterval? = value?.let(RecurrenceInterval::valueOf)

    @TypeConverter
    fun fromBillingCycle(value: BillingCycle?): String? = value?.name

    @TypeConverter
    fun toBillingCycle(value: String?): BillingCycle? = value?.let(BillingCycle::valueOf)

    @TypeConverter
    fun fromDebtStrategy(value: DebtStrategy?): String? = value?.name

    @TypeConverter
    fun toDebtStrategy(value: String?): DebtStrategy? = value?.let(DebtStrategy::valueOf)

    @TypeConverter
    fun fromRuleMatchField(value: RuleMatchField?): String? = value?.name

    @TypeConverter
    fun toRuleMatchField(value: String?): RuleMatchField? = value?.let(RuleMatchField::valueOf)
}
