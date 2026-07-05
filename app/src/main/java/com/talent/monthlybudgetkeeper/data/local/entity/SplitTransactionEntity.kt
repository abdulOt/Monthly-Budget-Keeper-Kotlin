package com.talent.monthlybudgetkeeper.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.talent.monthlybudgetkeeper.data.model.SyncStatus
import com.talent.monthlybudgetkeeper.data.model.TransactionCategory

@Entity(
    tableName = "split_transactions",
    indices = [
        Index(value = ["remoteId"], unique = true),
        Index(value = ["userId"]),
        Index(value = ["parentTransactionRemoteId"])
    ]
)
data class SplitTransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val remoteId: String? = null,
    val userId: String? = null,
    val parentTransactionRemoteId: String,
    val title: String,
    val category: TransactionCategory,
    val amount: Double,
    val note: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = createdAt,
    val syncStatus: SyncStatus = SyncStatus.PENDING
)
