package com.talent.monthlybudgetkeeper.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.talent.monthlybudgetkeeper.data.model.SyncStatus
import com.talent.monthlybudgetkeeper.data.model.TransactionCategory

@Entity(
    tableName = "envelope_budgets",
    indices = [
        Index(value = ["remoteId"], unique = true),
        Index(value = ["userId"]),
        Index(value = ["monthKey", "name"], unique = true)
    ]
)
data class EnvelopeBudgetEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val remoteId: String? = null,
    val userId: String? = null,
    val monthKey: String,
    val name: String,
    val category: TransactionCategory? = null,
    val plannedAmount: Double,
    val rolloverAmount: Double = 0.0,
    val spentAmount: Double = 0.0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = createdAt,
    val syncStatus: SyncStatus = SyncStatus.PENDING
)
