package com.talent.monthlybudgetkeeper.data.repository

import com.talent.monthlybudgetkeeper.data.auth.SessionManager
import com.talent.monthlybudgetkeeper.data.local.dao.BudgetDao
import com.talent.monthlybudgetkeeper.data.local.entity.CategoryBudgetEntity
import com.talent.monthlybudgetkeeper.data.local.entity.MonthlyBudgetEntity
import com.talent.monthlybudgetkeeper.data.model.SyncStatus
import com.talent.monthlybudgetkeeper.data.model.TransactionCategory
import com.talent.monthlybudgetkeeper.data.sync.SyncSignalBus
import com.talent.monthlybudgetkeeper.utils.DateUtils
import kotlinx.coroutines.flow.Flow
import java.time.YearMonth
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BudgetRepository @Inject constructor(
    private val budgetDao: BudgetDao,
    private val sessionManager: SessionManager,
    private val syncSignalBus: SyncSignalBus
) {
    fun observeMonthlyBudget(month: YearMonth): Flow<MonthlyBudgetEntity?> {
        return budgetDao.observeMonthlyBudget(DateUtils.toMonthKey(month))
    }

    fun observeCategoryBudgets(month: YearMonth): Flow<List<CategoryBudgetEntity>> {
        return budgetDao.observeCategoryBudgets(DateUtils.toMonthKey(month))
    }

    suspend fun setTotalBudget(month: YearMonth, amount: Double) {
        val now = System.currentTimeMillis()
        val monthKey = DateUtils.toMonthKey(month)
        val existing = budgetDao.getMonthlyBudget(monthKey)
        budgetDao.upsertMonthlyBudget(
            MonthlyBudgetEntity(
                monthKey = monthKey,
                remoteId = existing?.remoteId,
                userId = existing?.userId ?: sessionManager.currentUserOrNull()?.id,
                totalBudget = amount,
                createdAt = existing?.createdAt ?: now,
                updatedAt = now,
                syncStatus = SyncStatus.PENDING
            )
        )
        syncSignalBus.notifyDataChanged()
    }

    suspend fun setCategoryBudget(
        month: YearMonth,
        category: TransactionCategory,
        amount: Double
    ) {
        val monthKey = DateUtils.toMonthKey(month)
        if (amount <= 0.0) {
            budgetDao.deleteCategoryBudget(monthKey, category)
            syncSignalBus.notifyDataChanged()
            return
        }

        val existing = budgetDao.getCategoryBudget(monthKey, category)
        val now = System.currentTimeMillis()
        budgetDao.upsertCategoryBudget(
            CategoryBudgetEntity(
                id = existing?.id ?: 0,
                remoteId = existing?.remoteId,
                userId = existing?.userId ?: sessionManager.currentUserOrNull()?.id,
                monthKey = monthKey,
                category = category,
                limitAmount = amount,
                createdAt = existing?.createdAt ?: now,
                updatedAt = now,
                syncStatus = SyncStatus.PENDING
            )
        )
        syncSignalBus.notifyDataChanged()
    }

    suspend fun clearAll(notifySync: Boolean = true) {
        budgetDao.clearCategoryBudgets()
        budgetDao.clearMonthlyBudgets()
        if (notifySync) {
            syncSignalBus.notifyDataChanged()
        }
    }

    suspend fun getAllMonthlyBudgets(): List<MonthlyBudgetEntity> = budgetDao.getAllMonthlyBudgets()

    suspend fun getAllCategoryBudgets(): List<CategoryBudgetEntity> = budgetDao.getAllCategoryBudgets()

    suspend fun replaceAllBudgets(
        monthlyBudgets: List<MonthlyBudgetEntity>,
        categoryBudgets: List<CategoryBudgetEntity>,
        notifySync: Boolean = true
    ) {
        clearAll(notifySync = false)
        if (monthlyBudgets.isNotEmpty()) {
            budgetDao.upsertMonthlyBudgets(monthlyBudgets)
        }
        if (categoryBudgets.isNotEmpty()) {
            budgetDao.upsertCategoryBudgets(categoryBudgets)
        }
        if (notifySync) {
            syncSignalBus.notifyDataChanged()
        }
    }

    suspend fun seedDemoBudgetsIfEmpty() {
        if (budgetDao.getMonthlyBudgetCount() > 0) return

        val currentMonth = YearMonth.now()
        setTotalBudget(currentMonth, 110000.0)
        setCategoryBudget(currentMonth, TransactionCategory.FOOD, 22000.0)
        setCategoryBudget(currentMonth, TransactionCategory.TRANSPORT, 10000.0)
        setCategoryBudget(currentMonth, TransactionCategory.BILLS, 15000.0)
        setCategoryBudget(currentMonth, TransactionCategory.RENT, 50000.0)
        setCategoryBudget(currentMonth, TransactionCategory.ENTERTAINMENT, 9000.0)
    }
}
