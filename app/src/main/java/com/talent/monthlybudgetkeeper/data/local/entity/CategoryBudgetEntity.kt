package com.talent.monthlybudgetkeeper.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.talent.monthlybudgetkeeper.data.model.SyncStatus
import com.talent.monthlybudgetkeeper.data.model.TransactionCategory

@Entity(
    tableName = "category_budgets",
    indices = [
        Index(value = ["monthKey", "category"], unique = true),
        Index(value = ["remoteId"], unique = true),
        Index(value = ["userId"])
    ]
)
data class CategoryBudgetEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val remoteId: String? = null,
    val userId: String? = null,
    val monthKey: String,
    val category: TransactionCategory,
    val limitAmount: Double,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = createdAt,
    val syncStatus: SyncStatus = SyncStatus.PENDING
)
