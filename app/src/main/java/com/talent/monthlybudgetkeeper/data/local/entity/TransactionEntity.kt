package com.talent.monthlybudgetkeeper.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.talent.monthlybudgetkeeper.data.model.SyncStatus
import com.talent.monthlybudgetkeeper.data.model.TransactionCategory
import com.talent.monthlybudgetkeeper.data.model.TransactionType
import java.time.LocalDate

@Entity(
    tableName = "transactions",
    indices = [
        Index(value = ["remoteId"], unique = true),
        Index(value = ["userId"])
    ]
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val remoteId: String? = null,
    val userId: String? = null,
    val accountRemoteId: String? = null,
    val transferRemoteId: String? = null,
    val title: String,
    val amount: Double,
    val type: TransactionType,
    val category: TransactionCategory,
    val date: LocalDate,
    val note: String = "",
    val isTransfer: Boolean = false,
    val isPlanned: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = createdAt,
    val syncStatus: SyncStatus = SyncStatus.PENDING
)
