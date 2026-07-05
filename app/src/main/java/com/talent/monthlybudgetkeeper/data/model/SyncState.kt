package com.talent.monthlybudgetkeeper.data.model

sealed interface SyncState {
    data object Idle : SyncState
    data class Syncing(val message: String) : SyncState
    data class Success(val completedAt: Long) : SyncState
    data class Error(val message: String) : SyncState
}
