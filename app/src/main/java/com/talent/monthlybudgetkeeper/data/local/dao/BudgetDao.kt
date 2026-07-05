package com.talent.monthlybudgetkeeper.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.talent.monthlybudgetkeeper.data.local.entity.CategoryBudgetEntity
import com.talent.monthlybudgetkeeper.data.local.entity.MonthlyBudgetEntity
import com.talent.monthlybudgetkeeper.data.model.TransactionCategory
import kotlinx.coroutines.flow.Flow

@Dao
interface BudgetDao {
    @Query("SELECT * FROM monthly_budgets WHERE monthKey = :monthKey LIMIT 1")
    fun observeMonthlyBudget(monthKey: String): Flow<MonthlyBudgetEntity?>

    @Query("SELECT * FROM category_budgets WHERE monthKey = :monthKey ORDER BY category ASC")
    fun observeCategoryBudgets(monthKey: String): Flow<List<CategoryBudgetEntity>>

    @Query("SELECT * FROM category_budgets WHERE monthKey = :monthKey AND category = :category LIMIT 1")
    suspend fun getCategoryBudget(monthKey: String, category: TransactionCategory): CategoryBudgetEntity?

    @Query("SELECT * FROM monthly_budgets WHERE monthKey = :monthKey LIMIT 1")
    suspend fun getMonthlyBudget(monthKey: String): MonthlyBudgetEntity?

    @Query("SELECT * FROM monthly_budgets ORDER BY monthKey DESC")
    suspend fun getAllMonthlyBudgets(): List<MonthlyBudgetEntity>

    @Query("SELECT * FROM category_budgets ORDER BY monthKey DESC, category ASC")
    suspend fun getAllCategoryBudgets(): List<CategoryBudgetEntity>

    @Query("SELECT * FROM monthly_budgets WHERE remoteId = :remoteId LIMIT 1")
    suspend fun getMonthlyBudgetByRemoteId(remoteId: String): MonthlyBudgetEntity?

    @Query("SELECT * FROM category_budgets WHERE remoteId = :remoteId LIMIT 1")
    suspend fun getCategoryBudgetByRemoteId(remoteId: String): CategoryBudgetEntity?

    @Upsert
    suspend fun upsertMonthlyBudget(monthlyBudget: MonthlyBudgetEntity)

    @Upsert
    suspend fun upsertCategoryBudget(categoryBudget: CategoryBudgetEntity)

    @Upsert
    suspend fun upsertMonthlyBudgets(monthlyBudgets: List<MonthlyBudgetEntity>)

    @Upsert
    suspend fun upsertCategoryBudgets(categoryBudgets: List<CategoryBudgetEntity>)

    @Query("DELETE FROM category_budgets WHERE monthKey = :monthKey AND category = :category")
    suspend fun deleteCategoryBudget(monthKey: String, category: TransactionCategory)

    @Query("DELETE FROM monthly_budgets")
    suspend fun clearMonthlyBudgets()

    @Query("DELETE FROM category_budgets")
    suspend fun clearCategoryBudgets()

    @Query("SELECT COUNT(*) FROM monthly_budgets")
    suspend fun getMonthlyBudgetCount(): Int
}
