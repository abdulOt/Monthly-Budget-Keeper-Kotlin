package com.talent.monthlybudgetkeeper.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.talent.monthlybudgetkeeper.data.model.SyncStatus

@Entity(
    tableName = "receipt_attachments",
    indices = [
        Index(value = ["remoteId"], unique = true),
        Index(value = ["userId"]),
        Index(value = ["transactionRemoteId"])
    ]
)
data class ReceiptAttachmentEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val remoteId: String? = null,
    val userId: String? = null,
    val transactionRemoteId: String,
    val localUri: String,
    val fileName: String,
    val mimeType: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = createdAt,
    val syncStatus: SyncStatus = SyncStatus.PENDING
)
