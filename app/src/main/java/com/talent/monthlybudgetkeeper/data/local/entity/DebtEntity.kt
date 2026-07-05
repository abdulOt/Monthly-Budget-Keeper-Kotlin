package com.talent.monthlybudgetkeeper.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.talent.monthlybudgetkeeper.data.model.DebtStrategy
import com.talent.monthlybudgetkeeper.data.model.SyncStatus
import java.time.LocalDate

@Entity(
    tableName = "debts",
    indices = [
        Index(value = ["remoteId"], unique = true),
        Index(value = ["userId"])
    ]
)
data class DebtEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val remoteId: String? = null,
    val userId: String? = null,
    val accountRemoteId: String? = null,
    val name: String,
    val lender: String,
    val totalAmount: Double,
    val remainingAmount: Double,
    val dueDate: LocalDate? = null,
    val interestRate: Double = 0.0,
    val minimumPayment: Double = 0.0,
    val plannedPayment: Double = 0.0,
    val strategy: DebtStrategy = DebtStrategy.CUSTOM,
    val note: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = createdAt,
    val syncStatus: SyncStatus = SyncStatus.PENDING
)
