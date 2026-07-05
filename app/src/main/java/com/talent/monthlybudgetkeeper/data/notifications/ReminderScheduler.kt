package com.talent.monthlybudgetkeeper.data.notifications

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.talent.monthlybudgetkeeper.data.sync.SyncSignalBus
import com.talent.monthlybudgetkeeper.data.workers.ReminderWorker
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Singleton
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch

@Singleton
@OptIn(FlowPreview::class)
class ReminderScheduler @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val notificationCoordinator: NotificationCoordinator,
    syncSignalBus: SyncSignalBus
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    init {
        scope.launch {
            syncSignalBus.events
                .debounce(750)
                .collectLatest {
                    requestImmediateRefresh()
                }
        }
    }

    fun ensureScheduled() {
        notificationCoordinator.ensureChannels()
        val request = PeriodicWorkRequestBuilder<ReminderWorker>(12, TimeUnit.HOURS)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                    .build()
            )
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            ReminderWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
        requestImmediateRefresh()
    }

    fun requestImmediateRefresh() {
        val request = OneTimeWorkRequestBuilder<ReminderWorker>().build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            ReminderWorker.IMMEDIATE_WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            request
        )
    }
}
