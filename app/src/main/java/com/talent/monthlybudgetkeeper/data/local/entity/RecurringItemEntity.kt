package com.talent.monthlybudgetkeeper.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.talent.monthlybudgetkeeper.data.model.RecurrenceInterval
import com.talent.monthlybudgetkeeper.data.model.SyncStatus
import com.talent.monthlybudgetkeeper.data.model.TransactionCategory
import com.talent.monthlybudgetkeeper.data.model.TransactionType
import java.time.LocalDate

@Entity(
    tableName = "recurring_items",
    indices = [
        Index(value = ["remoteId"], unique = true),
        Index(value = ["userId"])
    ]
)
data class RecurringItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val remoteId: String? = null,
    val userId: String? = null,
    val accountRemoteId: String? = null,
    val title: String,
    val amount: Double,
    val type: TransactionType,
    val category: TransactionCategory,
    val startDate: LocalDate,
    val nextDueDate: LocalDate = startDate,
    val endDate: LocalDate? = null,
    val interval: RecurrenceInterval,
    val note: String = "",
    val autoCreateTransaction: Boolean = false,
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = createdAt,
    val syncStatus: SyncStatus = SyncStatus.PENDING
)
