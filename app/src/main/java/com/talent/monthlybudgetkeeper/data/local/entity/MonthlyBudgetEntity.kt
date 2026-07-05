package com.talent.monthlybudgetkeeper.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.talent.monthlybudgetkeeper.data.model.SyncStatus

@Entity(
    tableName = "monthly_budgets",
    indices = [
        Index(value = ["remoteId"], unique = true),
        Index(value = ["userId"])
    ]
)
data class MonthlyBudgetEntity(
    @PrimaryKey
    val monthKey: String,
    val remoteId: String? = null,
    val userId: String? = null,
    val totalBudget: Double,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = createdAt,
    val syncStatus: SyncStatus = SyncStatus.PENDING
)
