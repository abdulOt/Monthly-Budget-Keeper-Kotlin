package com.talent.monthlybudgetkeeper

import android.os.Bundle
import android.os.SystemClock
import android.view.WindowManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.material3.MaterialTheme
import androidx.core.view.WindowCompat
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.talent.monthlybudgetkeeper.data.security.AppLockController
import com.talent.monthlybudgetkeeper.ui.navigation.AppNavGraph
import com.talent.monthlybudgetkeeper.ui.screens.security.AppLockScreen
import com.talent.monthlybudgetkeeper.ui.theme.MonthlyBudgetKeeperTheme
import com.talent.monthlybudgetkeeper.data.auth.SessionManager
import com.talent.monthlybudgetkeeper.data.model.AuthSessionState
import com.talent.monthlybudgetkeeper.data.repository.SettingsRepository
import com.talent.monthlybudgetkeeper.viewmodel.SettingsViewModel
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : FragmentActivity() {
    @Inject
    lateinit var sessionManager: SessionManager

    @Inject
    lateinit var settingsRepository: SettingsRepository

    @Inject
    lateinit var appLockController: AppLockController

    private var lockEnabled by mutableStateOf(false)
    private var biometricEnabled by mutableStateOf(false)
    private var pinHash by mutableStateOf<String?>(null)
    private var isLocked by mutableStateOf(false)
    private var securityStateReady by mutableStateOf(false)
    private var hasInitializedLockState = false
    private var backgroundedAtElapsedMillis: Long? = null
    private var activeSecurityUserId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sessionManager.handleIncomingIntent(intent)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        lifecycleScope.launch {
            settingsRepository.preferencesFlow.collect { preferences ->
                lockEnabled = preferences.appLockEnabled && !preferences.pinHash.isNullOrBlank()
                biometricEnabled = preferences.biometricLockEnabled
                pinHash = preferences.pinHash
                isLocked = when {
                    !lockEnabled || pinHash.isNullOrBlank() -> false
                    !hasInitializedLockState -> true
                    else -> isLocked
                }
                hasInitializedLockState = true
                if (preferences.privacyModeEnabled) {
                    window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
                } else {
                    window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
                }
                securityStateReady = true
            }
        }

        lifecycleScope.launch {
            sessionManager.sessionState.collect { sessionState ->
                when (sessionState) {
                    AuthSessionState.Loading -> Unit
                    AuthSessionState.Unauthenticated -> {
                        activeSecurityUserId = null
                        backgroundedAtElapsedMillis = null
                        lockEnabled = false
                        biometricEnabled = false
                        pinHash = null
                        isLocked = false
                        hasInitializedLockState = false
                    }
                    is AuthSessionState.Authenticated -> {
                        if (activeSecurityUserId != sessionState.user.id) {
                            activeSecurityUserId = sessionState.user.id
                            backgroundedAtElapsedMillis = null
                            hasInitializedLockState = false
                            isLocked = false
                        }
                    }
                }
            }
        }

        lifecycleScope.launch {
            appLockController.lockRequests.collect {
                if (lockEnabled && !pinHash.isNullOrBlank()) {
                    isLocked = true
                }
            }
        }

        val composeView = ComposeView(this).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                val settingsViewModel: SettingsViewModel = hiltViewModel()
                val context = LocalContext.current
                val appState = remember(context) { AppState(context) }
                val preferences = settingsViewModel.preferences.collectAsStateWithLifecycle().value

                MonthlyBudgetKeeperTheme(
                    darkTheme = preferences.darkMode
                ) {
                    if (!securityStateReady) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.background)
                        )
                    } else if (isLocked && lockEnabled && !pinHash.isNullOrBlank()) {
                        AppLockScreen(
                            activity = this@MainActivity,
                            pinHash = pinHash.orEmpty(),
                            biometricEnabled = biometricEnabled,
                            onUnlock = { isLocked = false },
                            onMessage = appState::showMessage
                        )
                    } else {
                        AppNavGraph(appState = appState)
                    }
                }
            }
        }
        setContentView(composeView)
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        sessionManager.handleIncomingIntent(intent)
    }

    override fun onStart() {
        super.onStart()
        if (lockEnabled && !pinHash.isNullOrBlank()) {
            val timedOut = backgroundedAtElapsedMillis?.let { startedAt ->
                SystemClock.elapsedRealtime() - startedAt >= APP_LOCK_TIMEOUT_MILLIS
            } ?: false
            if (timedOut) {
                isLocked = true
            }
        }
        backgroundedAtElapsedMillis = null
    }

    override fun onStop() {
        super.onStop()
        if (lockEnabled && !pinHash.isNullOrBlank()) {
            backgroundedAtElapsedMillis = SystemClock.elapsedRealtime()
        }
    }

    private companion object {
        const val APP_LOCK_TIMEOUT_MILLIS = 15_000L
    }
}
