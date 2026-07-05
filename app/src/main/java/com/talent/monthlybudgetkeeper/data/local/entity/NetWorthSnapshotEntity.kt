package com.talent.monthlybudgetkeeper.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDate

@Entity(
    tableName = "net_worth_snapshots",
    indices = [
        Index(value = ["userId"]),
        Index(value = ["userId", "snapshotDate"], unique = true)
    ]
)
data class NetWorthSnapshotEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: String,
    val snapshotDate: LocalDate,
    val assetTotal: Double,
    val liabilityTotal: Double,
    val netWorth: Double,
    val createdAt: Long = System.currentTimeMillis()
)
