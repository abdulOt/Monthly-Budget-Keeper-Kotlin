package com.talent.monthlybudgetkeeper.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.talent.monthlybudgetkeeper.data.local.Converters
import com.talent.monthlybudgetkeeper.data.local.dao.BudgetDao
import com.talent.monthlybudgetkeeper.data.local.dao.FinanceDao
import com.talent.monthlybudgetkeeper.data.local.dao.TransactionDao
import com.talent.monthlybudgetkeeper.data.local.entity.AccountEntity
import com.talent.monthlybudgetkeeper.data.local.entity.CategoryBudgetEntity
import com.talent.monthlybudgetkeeper.data.local.entity.DebtEntity
import com.talent.monthlybudgetkeeper.data.local.entity.EnvelopeBudgetEntity
import com.talent.monthlybudgetkeeper.data.local.entity.GoalEntity
import com.talent.monthlybudgetkeeper.data.local.entity.MonthlyBudgetEntity
import com.talent.monthlybudgetkeeper.data.local.entity.NetWorthSnapshotEntity
import com.talent.monthlybudgetkeeper.data.local.entity.PaycheckPlanEntity
import com.talent.monthlybudgetkeeper.data.local.entity.ReceiptAttachmentEntity
import com.talent.monthlybudgetkeeper.data.local.entity.RecurringItemEntity
import com.talent.monthlybudgetkeeper.data.local.entity.SplitTransactionEntity
import com.talent.monthlybudgetkeeper.data.local.entity.SubscriptionBillEntity
import com.talent.monthlybudgetkeeper.data.local.entity.TransactionRuleEntity
import com.talent.monthlybudgetkeeper.data.local.entity.TransactionEntity
import com.talent.monthlybudgetkeeper.data.local.entity.TransferEntity

@Database(
    entities = [
        TransactionEntity::class,
        MonthlyBudgetEntity::class,
        CategoryBudgetEntity::class,
        AccountEntity::class,
        TransferEntity::class,
        RecurringItemEntity::class,
        SubscriptionBillEntity::class,
        GoalEntity::class,
        DebtEntity::class,
        NetWorthSnapshotEntity::class,
        EnvelopeBudgetEntity::class,
        PaycheckPlanEntity::class,
        SplitTransactionEntity::class,
        ReceiptAttachmentEntity::class,
        TransactionRuleEntity::class
    ],
    version = 4,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class BudgetKeeperDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    abstract fun budgetDao(): BudgetDao
    abstract fun financeDao(): FinanceDao
}
