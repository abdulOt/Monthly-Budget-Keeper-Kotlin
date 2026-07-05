package com.talent.monthlybudgetkeeper.data.security

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

@Singleton
class AppLockController @Inject constructor() {
    private val _lockRequests = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val lockRequests = _lockRequests.asSharedFlow()

    fun requestLock() {
        _lockRequests.tryEmit(Unit)
    }
}
