package com.talent.monthlybudgetkeeper.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.talent.monthlybudgetkeeper.data.model.BillingCycle
import com.talent.monthlybudgetkeeper.data.model.SyncStatus
import com.talent.monthlybudgetkeeper.data.model.TransactionCategory
import java.time.LocalDate

@Entity(
    tableName = "subscription_bills",
    indices = [
        Index(value = ["remoteId"], unique = true),
        Index(value = ["userId"])
    ]
)
data class SubscriptionBillEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val remoteId: String? = null,
    val userId: String? = null,
    val accountRemoteId: String? = null,
    val name: String,
    val amount: Double,
    val category: TransactionCategory,
    val dueDate: LocalDate,
    val nextChargeDate: LocalDate = dueDate,
    val billingCycle: BillingCycle,
    val note: String = "",
    val reminderDays: Int = 3,
    val isAutoPay: Boolean = false,
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = createdAt,
    val syncStatus: SyncStatus = SyncStatus.PENDING
)
