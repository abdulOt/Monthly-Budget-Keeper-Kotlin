package com.talent.monthlybudgetkeeper.data.repository

import com.talent.monthlybudgetkeeper.data.model.SyncState
import com.talent.monthlybudgetkeeper.data.sync.SupabaseSyncManager
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.StateFlow

@Singleton
class CloudSyncRepository @Inject constructor(
    private val syncManager: SupabaseSyncManager
) {
    val syncState: StateFlow<SyncState> = syncManager.syncState

    suspend fun syncNow(): Result<Unit> = syncManager.syncNow()

    suspend fun clearLocalUserData() = syncManager.clearLocalUserData()

    suspend fun resetLocalCacheOnly(): Result<Unit> = syncManager.resetLocalCacheOnly()

    suspend fun resetUserDataEverywhere(): Result<Unit> = syncManager.resetUserDataEverywhere()
}
