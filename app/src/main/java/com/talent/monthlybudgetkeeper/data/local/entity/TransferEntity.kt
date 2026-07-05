package com.talent.monthlybudgetkeeper.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.talent.monthlybudgetkeeper.data.model.SyncStatus
import java.time.LocalDate

@Entity(
    tableName = "transfers",
    indices = [
        Index(value = ["remoteId"], unique = true),
        Index(value = ["userId"])
    ]
)
data class TransferEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val remoteId: String? = null,
    val userId: String? = null,
    val fromAccountRemoteId: String,
    val toAccountRemoteId: String,
    val amount: Double,
    val date: LocalDate,
    val note: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = createdAt,
    val syncStatus: SyncStatus = SyncStatus.PENDING
)
