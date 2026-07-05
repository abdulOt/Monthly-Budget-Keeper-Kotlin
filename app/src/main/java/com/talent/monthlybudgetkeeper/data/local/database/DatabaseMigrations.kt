package com.talent.monthlybudgetkeeper.data.local.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object DatabaseMigrations {
    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE transactions ADD COLUMN remoteId TEXT")
            db.execSQL("ALTER TABLE transactions ADD COLUMN userId TEXT")
            db.execSQL("ALTER TABLE transactions ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE transactions ADD COLUMN syncStatus TEXT NOT NULL DEFAULT 'PENDING'")
            db.execSQL(
                "UPDATE transactions SET updatedAt = CASE WHEN createdAt > 0 THEN createdAt ELSE CAST(strftime('%s','now') AS INTEGER) * 1000 END"
            )
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_transactions_remoteId ON transactions(remoteId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_transactions_userId ON transactions(userId)")

            db.execSQL("ALTER TABLE monthly_budgets ADD COLUMN remoteId TEXT")
            db.execSQL("ALTER TABLE monthly_budgets ADD COLUMN userId TEXT")
            db.execSQL("ALTER TABLE monthly_budgets ADD COLUMN createdAt INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE monthly_budgets ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE monthly_budgets ADD COLUMN syncStatus TEXT NOT NULL DEFAULT 'PENDING'")
            db.execSQL("UPDATE monthly_budgets SET createdAt = CAST(strftime('%s','now') AS INTEGER) * 1000, updatedAt = CAST(strftime('%s','now') AS INTEGER) * 1000")
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_monthly_budgets_remoteId ON monthly_budgets(remoteId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_monthly_budgets_userId ON monthly_budgets(userId)")

            db.execSQL("ALTER TABLE category_budgets ADD COLUMN remoteId TEXT")
            db.execSQL("ALTER TABLE category_budgets ADD COLUMN userId TEXT")
            db.execSQL("ALTER TABLE category_budgets ADD COLUMN createdAt INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE category_budgets ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE category_budgets ADD COLUMN syncStatus TEXT NOT NULL DEFAULT 'PENDING'")
            db.execSQL("UPDATE category_budgets SET createdAt = CAST(strftime('%s','now') AS INTEGER) * 1000, updatedAt = CAST(strftime('%s','now') AS INTEGER) * 1000")
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_category_budgets_remoteId ON category_budgets(remoteId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_category_budgets_userId ON category_budgets(userId)")

            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS accounts (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    remoteId TEXT,
                    userId TEXT,
                    name TEXT NOT NULL,
                    accountType TEXT NOT NULL,
                    currentBalance REAL NOT NULL,
                    currencyCode TEXT NOT NULL,
                    isArchived INTEGER NOT NULL,
                    createdAt INTEGER NOT NULL,
                    updatedAt INTEGER NOT NULL,
                    syncStatus TEXT NOT NULL
                )
                """.trimIndent()
            )
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_accounts_remoteId ON accounts(remoteId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_accounts_userId ON accounts(userId)")

            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS recurring_items (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    remoteId TEXT,
                    userId TEXT,
                    title TEXT NOT NULL,
                    amount REAL NOT NULL,
                    type TEXT NOT NULL,
                    category TEXT NOT NULL,
                    startDate TEXT NOT NULL,
                    interval TEXT NOT NULL,
                    note TEXT NOT NULL,
                    isActive INTEGER NOT NULL,
                    createdAt INTEGER NOT NULL,
                    updatedAt INTEGER NOT NULL,
                    syncStatus TEXT NOT NULL
                )
                """.trimIndent()
            )
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_recurring_items_remoteId ON recurring_items(remoteId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_recurring_items_userId ON recurring_items(userId)")

            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS subscription_bills (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    remoteId TEXT,
                    userId TEXT,
                    name TEXT NOT NULL,
                    amount REAL NOT NULL,
                    category TEXT NOT NULL,
                    dueDate TEXT NOT NULL,
                    billingCycle TEXT NOT NULL,
                    note TEXT NOT NULL,
                    isAutoPay INTEGER NOT NULL,
                    isActive INTEGER NOT NULL,
                    createdAt INTEGER NOT NULL,
                    updatedAt INTEGER NOT NULL,
                    syncStatus TEXT NOT NULL
                )
                """.trimIndent()
            )
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_subscription_bills_remoteId ON subscription_bills(remoteId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_subscription_bills_userId ON subscription_bills(userId)")

            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS goals (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    remoteId TEXT,
                    userId TEXT,
                    name TEXT NOT NULL,
                    targetAmount REAL NOT NULL,
                    currentAmount REAL NOT NULL,
                    targetDate TEXT,
                    note TEXT NOT NULL,
                    isCompleted INTEGER NOT NULL,
                    createdAt INTEGER NOT NULL,
                    updatedAt INTEGER NOT NULL,
                    syncStatus TEXT NOT NULL
                )
                """.trimIndent()
            )
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_goals_remoteId ON goals(remoteId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_goals_userId ON goals(userId)")

            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS debts (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    remoteId TEXT,
                    userId TEXT,
                    name TEXT NOT NULL,
                    lender TEXT NOT NULL,
                    totalAmount REAL NOT NULL,
                    remainingAmount REAL NOT NULL,
                    dueDate TEXT,
                    interestRate REAL NOT NULL,
                    note TEXT NOT NULL,
                    createdAt INTEGER NOT NULL,
                    updatedAt INTEGER NOT NULL,
                    syncStatus TEXT NOT NULL
                )
                """.trimIndent()
            )
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_debts_remoteId ON debts(remoteId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_debts_userId ON debts(userId)")
        }
    }

    val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE transactions ADD COLUMN accountRemoteId TEXT")
            db.execSQL("ALTER TABLE transactions ADD COLUMN transferRemoteId TEXT")
            db.execSQL("ALTER TABLE transactions ADD COLUMN isTransfer INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE transactions ADD COLUMN isPlanned INTEGER NOT NULL DEFAULT 0")

            db.execSQL("ALTER TABLE accounts ADD COLUMN institution TEXT NOT NULL DEFAULT ''")
            db.execSQL("ALTER TABLE accounts ADD COLUMN includeInNetWorth INTEGER NOT NULL DEFAULT 1")

            db.execSQL("ALTER TABLE recurring_items ADD COLUMN accountRemoteId TEXT")
            db.execSQL("ALTER TABLE recurring_items ADD COLUMN nextDueDate TEXT NOT NULL DEFAULT '1970-01-01'")
            db.execSQL("ALTER TABLE recurring_items ADD COLUMN endDate TEXT")
            db.execSQL("ALTER TABLE recurring_items ADD COLUMN autoCreateTransaction INTEGER NOT NULL DEFAULT 0")
            db.execSQL("UPDATE recurring_items SET nextDueDate = startDate")

            db.execSQL("ALTER TABLE subscription_bills ADD COLUMN accountRemoteId TEXT")
            db.execSQL("ALTER TABLE subscription_bills ADD COLUMN nextChargeDate TEXT NOT NULL DEFAULT '1970-01-01'")
            db.execSQL("ALTER TABLE subscription_bills ADD COLUMN reminderDays INTEGER NOT NULL DEFAULT 3")
            db.execSQL("UPDATE subscription_bills SET nextChargeDate = dueDate")

            db.execSQL("ALTER TABLE goals ADD COLUMN linkedAccountRemoteId TEXT")
            db.execSQL("ALTER TABLE goals ADD COLUMN monthlyContribution REAL NOT NULL DEFAULT 0.0")
            db.execSQL("ALTER TABLE goals ADD COLUMN isSinkingFund INTEGER NOT NULL DEFAULT 0")

            db.execSQL("ALTER TABLE debts ADD COLUMN accountRemoteId TEXT")
            db.execSQL("ALTER TABLE debts ADD COLUMN minimumPayment REAL NOT NULL DEFAULT 0.0")
            db.execSQL("ALTER TABLE debts ADD COLUMN plannedPayment REAL NOT NULL DEFAULT 0.0")
            db.execSQL("ALTER TABLE debts ADD COLUMN strategy TEXT NOT NULL DEFAULT 'CUSTOM'")

            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS transfers (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    remoteId TEXT,
                    userId TEXT,
                    fromAccountRemoteId TEXT NOT NULL,
                    toAccountRemoteId TEXT NOT NULL,
                    amount REAL NOT NULL,
                    date TEXT NOT NULL,
                    note TEXT NOT NULL,
                    createdAt INTEGER NOT NULL,
                    updatedAt INTEGER NOT NULL,
                    syncStatus TEXT NOT NULL
                )
                """.trimIndent()
            )
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_transfers_remoteId ON transfers(remoteId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_transfers_userId ON transfers(userId)")

            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS split_transactions (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    remoteId TEXT,
                    userId TEXT,
                    parentTransactionRemoteId TEXT NOT NULL,
                    title TEXT NOT NULL,
                    category TEXT NOT NULL,
                    amount REAL NOT NULL,
                    note TEXT NOT NULL,
                    createdAt INTEGER NOT NULL,
                    updatedAt INTEGER NOT NULL,
                    syncStatus TEXT NOT NULL
                )
                """.trimIndent()
            )
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_split_transactions_remoteId ON split_transactions(remoteId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_split_transactions_userId ON split_transactions(userId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_split_transactions_parentTransactionRemoteId ON split_transactions(parentTransactionRemoteId)")

            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS receipt_attachments (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    remoteId TEXT,
                    userId TEXT,
                    transactionRemoteId TEXT NOT NULL,
                    localUri TEXT NOT NULL,
                    fileName TEXT NOT NULL,
                    mimeType TEXT NOT NULL,
                    createdAt INTEGER NOT NULL,
                    updatedAt INTEGER NOT NULL,
                    syncStatus TEXT NOT NULL
                )
                """.trimIndent()
            )
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_receipt_attachments_remoteId ON receipt_attachments(remoteId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_receipt_attachments_userId ON receipt_attachments(userId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_receipt_attachments_transactionRemoteId ON receipt_attachments(transactionRemoteId)")

            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS transaction_rules (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    remoteId TEXT,
                    userId TEXT,
                    keyword TEXT NOT NULL,
                    matchField TEXT NOT NULL,
                    targetType TEXT NOT NULL,
                    targetCategory TEXT NOT NULL,
                    targetAccountRemoteId TEXT,
                    priority INTEGER NOT NULL,
                    isActive INTEGER NOT NULL,
                    createdAt INTEGER NOT NULL,
                    updatedAt INTEGER NOT NULL,
                    syncStatus TEXT NOT NULL
                )
                """.trimIndent()
            )
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_transaction_rules_remoteId ON transaction_rules(remoteId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_transaction_rules_userId ON transaction_rules(userId)")

            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS envelope_budgets (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    remoteId TEXT,
                    userId TEXT,
                    monthKey TEXT NOT NULL,
                    name TEXT NOT NULL,
                    category TEXT,
                    plannedAmount REAL NOT NULL,
                    rolloverAmount REAL NOT NULL,
                    spentAmount REAL NOT NULL,
                    createdAt INTEGER NOT NULL,
                    updatedAt INTEGER NOT NULL,
                    syncStatus TEXT NOT NULL
                )
                """.trimIndent()
            )
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_envelope_budgets_remoteId ON envelope_budgets(remoteId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_envelope_budgets_userId ON envelope_budgets(userId)")
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_envelope_budgets_monthKey_name ON envelope_budgets(monthKey, name)")

            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS paycheck_plans (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    remoteId TEXT,
                    userId TEXT,
                    name TEXT NOT NULL,
                    accountRemoteId TEXT,
                    expectedAmount REAL NOT NULL,
                    payday TEXT NOT NULL,
                    allocatedAmount REAL NOT NULL,
                    note TEXT NOT NULL,
                    createdAt INTEGER NOT NULL,
                    updatedAt INTEGER NOT NULL,
                    syncStatus TEXT NOT NULL
                )
                """.trimIndent()
            )
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_paycheck_plans_remoteId ON paycheck_plans(remoteId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_paycheck_plans_userId ON paycheck_plans(userId)")
        }
    }

    val MIGRATION_3_4 = object : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS net_worth_snapshots (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    userId TEXT NOT NULL,
                    snapshotDate TEXT NOT NULL,
                    assetTotal REAL NOT NULL,
                    liabilityTotal REAL NOT NULL,
                    netWorth REAL NOT NULL,
                    createdAt INTEGER NOT NULL
                )
                """.trimIndent()
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS index_net_worth_snapshots_userId ON net_worth_snapshots(userId)")
            db.execSQL(
                "CREATE UNIQUE INDEX IF NOT EXISTS index_net_worth_snapshots_userId_snapshotDate ON net_worth_snapshots(userId, snapshotDate)"
            )
        }
    }
}
