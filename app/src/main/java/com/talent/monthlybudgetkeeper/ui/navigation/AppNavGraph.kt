package com.talent.monthlybudgetkeeper.ui.navigation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.talent.monthlybudgetkeeper.AppState
import com.talent.monthlybudgetkeeper.data.model.AuthSessionState
import com.talent.monthlybudgetkeeper.data.model.TransactionType
import com.talent.monthlybudgetkeeper.ui.screens.auth.AuthWelcomeScreen
import com.talent.monthlybudgetkeeper.ui.screens.auth.ForgotPasswordScreen
import com.talent.monthlybudgetkeeper.ui.screens.auth.LoginScreen
import com.talent.monthlybudgetkeeper.ui.screens.auth.SignUpScreen
import com.talent.monthlybudgetkeeper.ui.screens.bills.BillsScreen
import com.talent.monthlybudgetkeeper.ui.screens.budget.BudgetScreen
import com.talent.monthlybudgetkeeper.ui.screens.dashboard.DashboardScreen
import com.talent.monthlybudgetkeeper.ui.screens.launch.LaunchScreen
import com.talent.monthlybudgetkeeper.ui.screens.more.MoreScreen
import com.talent.monthlybudgetkeeper.ui.screens.networth.NetWorthScreen
import com.talent.monthlybudgetkeeper.ui.screens.onboarding.OnboardingScreen
import com.talent.monthlybudgetkeeper.ui.screens.reports.ReportsScreen
import com.talent.monthlybudgetkeeper.ui.screens.debt.DebtScreen
import com.talent.monthlybudgetkeeper.ui.screens.setup.ReadyScreen
import com.talent.monthlybudgetkeeper.ui.screens.setup.SetupScreen
import com.talent.monthlybudgetkeeper.ui.screens.settings.SettingsScreen
import com.talent.monthlybudgetkeeper.ui.screens.transaction.TransactionDetailScreen
import com.talent.monthlybudgetkeeper.ui.screens.transaction.TransactionFormScreen
import com.talent.monthlybudgetkeeper.ui.screens.transactions.TransactionsScreen
import com.talent.monthlybudgetkeeper.viewmodel.AuthViewModel
import com.talent.monthlybudgetkeeper.viewmodel.ProfileBootstrapState
import kotlinx.coroutines.flow.collect

private sealed interface AppRouteResolution {
    data object Loading : AppRouteResolution
    data class Ready(val route: String) : AppRouteResolution
    data class Error(val message: String) : AppRouteResolution
}

@Composable
fun AppNavGraph(
    appState: AppState,
    modifier: Modifier = Modifier,
    authViewModel: AuthViewModel = hiltViewModel()
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val sessionState by authViewModel.sessionState.collectAsStateWithLifecycle()
    val preferences by authViewModel.preferences.collectAsStateWithLifecycle()
    val preferencesReady by authViewModel.preferencesReady.collectAsStateWithLifecycle()
    val profileBootstrapState by authViewModel.profileBootstrapState.collectAsStateWithLifecycle()
    val effectiveSetupCompleted = if (preferences.setupResetPending) {
        false
    } else {
        when (val state = profileBootstrapState) {
            is ProfileBootstrapState.Ready -> state.setupCompletedInCloud || preferences.setupCompleted
            else -> preferences.setupCompleted
        }
    }
    val bottomDestinationRoute = resolveBottomDestinationRoute(currentRoute)
    val routeResolution = resolveRouteResolution(
        sessionState = sessionState,
        preferencesReady = preferencesReady,
        onboardingCompleted = preferences.onboardingCompleted,
        effectiveSetupCompleted = effectiveSetupCompleted,
        profileBootstrapState = profileBootstrapState
    )
    val showBottomBar = sessionState is AuthSessionState.Authenticated &&
        bottomDestinationRoute != null &&
        currentRoute !in routesWithoutBottomBar

    LaunchedEffect(Unit) {
        authViewModel.messages.collect { appState.showMessage(it) }
    }

    LaunchedEffect(routeResolution, currentRoute) {
        when (routeResolution) {
            AppRouteResolution.Loading,
            is AppRouteResolution.Error -> {
                if (currentRoute != AppDestination.AppLoading.route) {
                    navController.navigate(AppDestination.AppLoading.route) {
                        popUpTo(navController.graph.startDestinationId) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            }

            is AppRouteResolution.Ready -> {
                val targetRoute = routeResolution.route
                val shouldNavigate = when (targetRoute) {
                    AppDestination.Dashboard.route -> !isShellRoute(currentRoute)
                    AppDestination.AuthWelcome.route -> currentRoute !in unauthenticatedRoutes
                    AppDestination.Login.route -> currentRoute !in unauthenticatedRoutes
                    else -> currentRoute != targetRoute
                }
                if (shouldNavigate) {
                    navController.navigate(targetRoute) {
                        popUpTo(navController.graph.startDestinationId) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            }
        }
    }

    Scaffold(
        modifier = modifier,
        contentWindowInsets = WindowInsets.systemBars,
        bottomBar = {
            if (showBottomBar) {
                FloatingBottomBar(
                    currentRoute = bottomDestinationRoute,
                    onNavigate = { route ->
                        navController.navigate(route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = AppDestination.AppLoading.route
        ) {
            composable(AppDestination.AppLoading.route) {
                LaunchScreen(
                    sessionLoading = sessionState == AuthSessionState.Loading,
                    settingsLoading = !preferencesReady,
                    profileLoading = routeResolution == AppRouteResolution.Loading,
                    profileError = (routeResolution as? AppRouteResolution.Error)?.message,
                    onRetry = authViewModel::retryProfileBootstrap
                )
            }
            composable(AppDestination.Onboarding.route) {
                OnboardingScreen(
                    onContinue = { route ->
                        navController.navigate(route) {
                            popUpTo(AppDestination.Onboarding.route) { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                )
            }
            composable(AppDestination.AuthWelcome.route) {
                AuthWelcomeScreen(
                    onLogin = { navController.navigate(AppDestination.Login.route) },
                    onSignUp = { navController.navigate(AppDestination.SignUp.route) },
                    viewModel = authViewModel
                )
            }
            composable(AppDestination.Login.route) {
                LoginScreen(
                    onSignUp = { navController.navigate(AppDestination.SignUp.route) },
                    onForgotPassword = { navController.navigate(AppDestination.ForgotPassword.route) },
                    viewModel = authViewModel
                )
            }
            composable(AppDestination.SignUp.route) {
                SignUpScreen(
                    onLogin = { navController.navigate(AppDestination.Login.route) },
                    viewModel = authViewModel
                )
            }
            composable(AppDestination.ForgotPassword.route) {
                ForgotPasswordScreen(
                    onBackToLogin = {
                        navController.navigate(AppDestination.Login.route) {
                            popUpTo(AppDestination.ForgotPassword.route) { inclusive = true }
                            launchSingleTop = true
                        }
                    },
                    viewModel = authViewModel
                )
            }
            composable(AppDestination.Setup.route) {
                SetupScreen(
                    contentPadding = innerPadding,
                    onCompleted = {
                        navController.navigate(AppDestination.Ready.route) {
                            popUpTo(AppDestination.Setup.route) { inclusive = true }
                            launchSingleTop = true
                        }
                    },
                    appState = appState
                )
            }
            composable(AppDestination.Ready.route) {
                ReadyScreen(
                    onContinue = {
                        navController.navigate(AppDestination.Dashboard.route) {
                            popUpTo(AppDestination.Ready.route) { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                )
            }
            composable(AppDestination.Dashboard.route) {
                DashboardScreen(
                    contentPadding = innerPadding,
                    onAddTransaction = {
                        navController.navigate(
                            AppDestination.TransactionForm.createRoute(presetType = TransactionType.EXPENSE)
                        )
                    },
                    onAddBill = { navController.navigate(AppDestination.Bills.route) },
                    onAddSubscription = { navController.navigate(AppDestination.Bills.route) },
                    onAddTransfer = { navController.navigate(AppDestination.Transactions.route) },
                    onOpenBudget = { navController.navigate(AppDestination.Budget.route) },
                    onOpenGoals = { navController.navigate(AppDestination.Reports.route) },
                    onTransactionClick = { transactionId ->
                        navController.navigate(AppDestination.TransactionDetail.createRoute(transactionId))
                    },
                    onOpenTransactions = { navController.navigate(AppDestination.Transactions.route) }
                )
            }
            composable(AppDestination.Transactions.route) {
                TransactionsScreen(
                    contentPadding = innerPadding,
                    onAddIncome = {
                        navController.navigate(
                            AppDestination.TransactionForm.createRoute(presetType = TransactionType.INCOME)
                        )
                    },
                    onAddExpense = {
                        navController.navigate(
                            AppDestination.TransactionForm.createRoute(presetType = TransactionType.EXPENSE)
                        )
                    },
                    onTransactionClick = { transactionId ->
                        navController.navigate(AppDestination.TransactionDetail.createRoute(transactionId))
                    }
                )
            }
            composable(AppDestination.Bills.route) {
                BillsScreen(contentPadding = innerPadding)
            }
            composable(AppDestination.Budget.route) {
                BudgetScreen(contentPadding = innerPadding)
            }
            composable(AppDestination.Reports.route) {
                ReportsScreen(appState = appState, contentPadding = innerPadding)
            }
            composable(AppDestination.Debts.route) {
                DebtScreen(contentPadding = innerPadding)
            }
            composable(AppDestination.NetWorth.route) {
                NetWorthScreen(
                    contentPadding = innerPadding,
                    onOpenDebts = { navController.navigate(AppDestination.Debts.route) }
                )
            }
            composable(AppDestination.More.route) {
                MoreScreen(
                    contentPadding = innerPadding,
                    onOpenBudgets = { navController.navigate(AppDestination.Budget.route) },
                    onOpenAccounts = { navController.navigate(AppDestination.NetWorth.route) },
                    onOpenGoals = { navController.navigate(AppDestination.Reports.route) },
                    onOpenDebts = { navController.navigate(AppDestination.Debts.route) },
                    onOpenSettings = { navController.navigate(AppDestination.Settings.route) },
                    onOpenSecurity = { navController.navigate(AppDestination.Settings.route) },
                    onOpenBackupRestore = { navController.navigate(AppDestination.Settings.route) },
                    onOpenSyncStatus = { navController.navigate(AppDestination.Settings.route) }
                )
            }
            composable(AppDestination.Settings.route) {
                SettingsScreen(
                    contentPadding = innerPadding,
                    appState = appState,
                    onResetComplete = {
                        navController.navigate(AppDestination.AppLoading.route) {
                            popUpTo(AppDestination.Settings.route) { inclusive = true }
                            launchSingleTop = true
                        }
                    },
                    authViewModel = authViewModel
                )
            }
            composable(
                route = AppDestination.TransactionForm.route,
                arguments = listOf(
                    navArgument("transactionId") {
                        type = NavType.LongType
                        defaultValue = 0L
                    },
                    navArgument("presetType") {
                        type = NavType.StringType
                        defaultValue = TransactionType.EXPENSE.name
                    }
                )
            ) {
                TransactionFormScreen(
                    contentPadding = innerPadding,
                    onBack = { navController.popBackStack() },
                    onSaved = { transactionId ->
                        navController.navigate(AppDestination.TransactionDetail.createRoute(transactionId)) {
                            popUpTo(AppDestination.TransactionForm.route) { inclusive = true }
                        }
                    }
                )
            }
            composable(
                route = AppDestination.TransactionDetail.route,
                arguments = listOf(navArgument("transactionId") { type = NavType.LongType })
            ) {
                TransactionDetailScreen(
                    contentPadding = innerPadding,
                    onBack = { navController.popBackStack() },
                    onEdit = { transactionId ->
                        navController.navigate(
                            AppDestination.TransactionForm.createRoute(transactionId = transactionId)
                        )
                    },
                    onDeleted = { navController.popBackStack() }
                )
            }
        }
    }
}

@Composable
private fun FloatingBottomBar(
    currentRoute: String?,
    onNavigate: (String) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 18.dp, vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
            border = BorderStroke(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.75f)
            ),
            tonalElevation = 10.dp,
            shadowElevation = 10.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 9.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                bottomNavigationDestinations.forEach { destination ->
                    val selected = currentRoute == destination.route
                    val itemShape = RoundedCornerShape(22.dp)
                    val interactionSource = remember { MutableInteractionSource() }

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(itemShape)
                            .background(
                                color = if (selected) {
                                    MaterialTheme.colorScheme.primaryContainer
                                } else {
                                    MaterialTheme.colorScheme.surface.copy(alpha = 0f)
                                },
                                shape = itemShape
                            )
                            .clickable(
                                interactionSource = interactionSource,
                                indication = null
                            ) {
                                onNavigate(destination.route)
                            }
                            .padding(horizontal = 6.dp, vertical = 11.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        destination.icon?.let {
                            Icon(
                                imageVector = it,
                                contentDescription = destination.label,
                                tint = if (selected) {
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                        }
                        Text(
                            text = destination.label,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.labelMedium,
                            color = if (selected) {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                }
            }
        }
    }
}

private fun resolveRouteResolution(
    sessionState: AuthSessionState,
    preferencesReady: Boolean,
    onboardingCompleted: Boolean,
    effectiveSetupCompleted: Boolean,
    profileBootstrapState: ProfileBootstrapState
): AppRouteResolution {
    return when {
        !preferencesReady -> AppRouteResolution.Loading
        sessionState == AuthSessionState.Loading -> AppRouteResolution.Loading
        sessionState is AuthSessionState.Authenticated && sessionState.requiresPasswordReset -> {
            AppRouteResolution.Ready(AppDestination.ForgotPassword.route)
        }

        sessionState is AuthSessionState.Authenticated && profileBootstrapState is ProfileBootstrapState.Error -> {
            AppRouteResolution.Error(profileBootstrapState.message)
        }

        sessionState is AuthSessionState.Authenticated &&
            (profileBootstrapState == ProfileBootstrapState.Idle || profileBootstrapState is ProfileBootstrapState.Loading) -> {
            AppRouteResolution.Loading
        }

        sessionState is AuthSessionState.Authenticated && !effectiveSetupCompleted -> {
            AppRouteResolution.Ready(AppDestination.Setup.route)
        }

        sessionState is AuthSessionState.Authenticated -> AppRouteResolution.Ready(AppDestination.Dashboard.route)
        !onboardingCompleted -> AppRouteResolution.Ready(AppDestination.Onboarding.route)
        sessionState == AuthSessionState.Unauthenticated -> AppRouteResolution.Ready(AppDestination.AuthWelcome.route)
        else -> AppRouteResolution.Ready(AppDestination.Dashboard.route)
    }
}

private fun isShellRoute(route: String?): Boolean = resolveBottomDestinationRoute(route) != null

private val routesWithoutBottomBar = setOf(
    AppDestination.AppLoading.route,
    AppDestination.Launch.route,
    AppDestination.Onboarding.route,
    AppDestination.AuthWelcome.route,
    AppDestination.Login.route,
    AppDestination.SignUp.route,
    AppDestination.ForgotPassword.route,
    AppDestination.Setup.route,
    AppDestination.Ready.route
)

private val unauthenticatedRoutes = setOf(
    AppDestination.Login.route,
    AppDestination.SignUp.route,
    AppDestination.ForgotPassword.route,
    AppDestination.AuthWelcome.route
)

private fun resolveBottomDestinationRoute(route: String?): String? {
    return when {
        route == null -> null
        route == AppDestination.Dashboard.route -> AppDestination.Dashboard.route
        route == AppDestination.Transactions.route -> AppDestination.Transactions.route
        route == AppDestination.Bills.route -> AppDestination.Bills.route
        route == AppDestination.Reports.route -> AppDestination.Reports.route
        route == AppDestination.More.route -> AppDestination.More.route
        route == AppDestination.Budget.route -> AppDestination.More.route
        route == AppDestination.Debts.route -> AppDestination.More.route
        route == AppDestination.NetWorth.route -> AppDestination.More.route
        route == AppDestination.Settings.route -> AppDestination.More.route
        route.startsWith("transaction_form") -> AppDestination.Transactions.route
        route.startsWith("transaction_detail") -> AppDestination.Transactions.route
        else -> null
    }
}
