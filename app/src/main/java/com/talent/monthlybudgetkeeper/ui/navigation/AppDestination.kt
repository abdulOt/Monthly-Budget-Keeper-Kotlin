package com.talent.monthlybudgetkeeper.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Login
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.MarkEmailRead
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.PersonAddAlt1
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Wallet
import androidx.compose.ui.graphics.vector.ImageVector
import com.talent.monthlybudgetkeeper.data.model.TransactionType

sealed class AppDestination(
    val route: String,
    val label: String,
    val icon: ImageVector? = null
) {
    data object AppLoading : AppDestination("app_loading", "Loading")
    data object Launch : AppDestination("launch", "Launch")
    data object Onboarding : AppDestination("onboarding", "Welcome")
    data object AuthWelcome : AppDestination("auth_welcome", "Secure Access", Icons.Outlined.Lock)
    data object Login : AppDestination("login", "Log In", Icons.AutoMirrored.Outlined.Login)
    data object SignUp : AppDestination("signup", "Create Account", Icons.Outlined.PersonAddAlt1)
    data object ForgotPassword : AppDestination("forgot_password", "Reset Password", Icons.Outlined.MarkEmailRead)
    data object Setup : AppDestination("setup", "Setup")
    data object Ready : AppDestination("ready", "Ready")
    data object Dashboard : AppDestination("dashboard", "Home", Icons.Outlined.Home)
    data object Transactions : AppDestination("transactions", "Transactions", Icons.AutoMirrored.Outlined.ReceiptLong)
    data object Bills : AppDestination("bills", "Bills", Icons.Outlined.CreditCard)
    data object Budget : AppDestination("budget", "Budgets", Icons.Outlined.Wallet)
    data object Reports : AppDestination("reports", "Reports", Icons.Outlined.Analytics)
    data object More : AppDestination("more", "More", Icons.Outlined.MoreHoriz)
    data object Debts : AppDestination("debts", "Debts")
    data object NetWorth : AppDestination("net_worth", "Net Worth")
    data object Settings : AppDestination("settings", "Settings", Icons.Outlined.Settings)

    data object TransactionForm : AppDestination(
        route = "transaction_form?transactionId={transactionId}&presetType={presetType}",
        label = "Transaction"
    ) {
        fun createRoute(
            transactionId: Long = 0L,
            presetType: TransactionType = TransactionType.EXPENSE
        ): String = "transaction_form?transactionId=$transactionId&presetType=${presetType.name}"
    }

    data object TransactionDetail : AppDestination(
        route = "transaction_detail/{transactionId}",
        label = "Transaction Detail"
    ) {
        fun createRoute(transactionId: Long): String = "transaction_detail/$transactionId"
    }
}

val bottomNavigationDestinations = listOf(
    AppDestination.Dashboard,
    AppDestination.Transactions,
    AppDestination.Bills,
    AppDestination.Reports,
    AppDestination.More
)

val authDestinations = setOf(
    AppDestination.AuthWelcome.route,
    AppDestination.Login.route,
    AppDestination.SignUp.route,
    AppDestination.ForgotPassword.route
)

val protectedDestinations = setOf(
    AppDestination.Setup.route,
    AppDestination.Ready.route,
    AppDestination.Dashboard.route,
    AppDestination.Transactions.route,
    AppDestination.Bills.route,
    AppDestination.Budget.route,
    AppDestination.Reports.route,
    AppDestination.More.route,
    AppDestination.Debts.route,
    AppDestination.NetWorth.route,
    AppDestination.Settings.route,
    AppDestination.TransactionForm.route,
    AppDestination.TransactionDetail.route
)
