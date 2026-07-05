package com.talent.monthlybudgetkeeper.ui.screens.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.AddCard
import androidx.compose.material.icons.outlined.Cached
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.Paid
import androidx.compose.material.icons.outlined.Subscriptions
import androidx.compose.material.icons.outlined.Wallet
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.talent.monthlybudgetkeeper.ui.components.ActionButtonStyle
import com.talent.monthlybudgetkeeper.ui.components.EmptyStateCard
import com.talent.monthlybudgetkeeper.ui.components.InsightCard
import com.talent.monthlybudgetkeeper.ui.components.QuickActionButton
import com.talent.monthlybudgetkeeper.ui.components.TransactionItemCard
import com.talent.monthlybudgetkeeper.utils.CurrencyFormatter
import com.talent.monthlybudgetkeeper.utils.DateUtils
import com.talent.monthlybudgetkeeper.viewmodel.HomeViewModel

@Composable
fun DashboardScreen(
    contentPadding: PaddingValues,
    onAddTransaction: () -> Unit,
    onAddBill: () -> Unit,
    onAddSubscription: () -> Unit,
    onAddTransfer: () -> Unit,
    onOpenBudget: () -> Unit,
    onOpenGoals: () -> Unit,
    onTransactionClick: (Long) -> Unit,
    onOpenTransactions: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val hideSensitiveValues = state.privacyModeEnabled
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    var showGoalDialog by remember { mutableStateOf(false) }
    var goalName by remember { mutableStateOf("") }
    var goalTarget by remember { mutableStateOf("") }
    var goalCurrent by remember { mutableStateOf("") }

    fun submitGoal() {
        keyboardController?.hide()
        focusManager.clearFocus(force = true)
        viewModel.saveGoal(goalName, goalTarget, goalCurrent)
        goalName = ""
        goalTarget = ""
        goalCurrent = ""
        showGoalDialog = false
    }

    LazyColumn(
        contentPadding = PaddingValues(
            start = 20.dp,
            end = 20.dp,
            top = contentPadding.calculateTopPadding() + 20.dp,
            bottom = contentPadding.calculateBottomPadding() + 32.dp
        ),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item {
            DashboardHeroCard(
                monthLabel = DateUtils.formatMonth(state.month),
                balance = CurrencyFormatter.format(state.remainingBalance, state.currency),
                income = CurrencyFormatter.format(state.totalIncome, state.currency),
                expenses = CurrencyFormatter.format(state.totalExpenses, state.currency),
                budgetTarget = if (state.monthlyBudget > 0) {
                    CurrencyFormatter.format(state.monthlyBudget, state.currency)
                } else {
                    "Not set"
                },
                budgetProgress = state.budgetUsedPercentage.coerceIn(0f, 1f),
                netWorth = CurrencyFormatter.format(state.netWorth, state.currency),
                hideSensitiveValues = hideSensitiveValues
            )
        }

        item {
            Card(
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    SectionHeader(
                        title = "Quick actions",
                        subtitle = "The tasks you reach for most often."
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        QuickActionButton(
                            label = "Add Transaction",
                            icon = Icons.Outlined.AddCard,
                            modifier = Modifier.weight(1f),
                            onClick = onAddTransaction
                        )
                        QuickActionButton(
                            label = "Add Bill",
                            icon = Icons.AutoMirrored.Outlined.ReceiptLong,
                            modifier = Modifier.weight(1f),
                            style = ActionButtonStyle.SECONDARY,
                            onClick = onAddBill
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        QuickActionButton(
                            label = "Add Subscription",
                            icon = Icons.Outlined.Subscriptions,
                            modifier = Modifier.weight(1f),
                            style = ActionButtonStyle.SECONDARY,
                            onClick = onAddSubscription
                        )
                        QuickActionButton(
                            label = "Add Transfer",
                            icon = Icons.Outlined.Cached,
                            modifier = Modifier.weight(1f),
                            style = ActionButtonStyle.SECONDARY,
                            onClick = onAddTransfer
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        QuickActionButton(
                            label = "Set Budget",
                            icon = Icons.Outlined.Wallet,
                            modifier = Modifier.weight(1f),
                            style = ActionButtonStyle.SECONDARY,
                            onClick = onOpenBudget
                        )
                        QuickActionButton(
                            label = "View Goals",
                            icon = Icons.Outlined.Flag,
                            modifier = Modifier.weight(1f),
                            style = ActionButtonStyle.SECONDARY,
                            onClick = onOpenGoals
                        )
                    }
                }
            }
        }

        item {
            Card(
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    SectionHeader(
                        title = "Upcoming bills",
                        subtitle = "A compact look at what needs attention soon."
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = if (state.upcomingBillsCount > 0) {
                                    state.upcomingBillsStatusText
                                } else {
                                    "No upcoming bills"
                                },
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = if (state.upcomingBillsCount > 0) {
                                    "${state.upcomingBillsCount} item(s) - ${CurrencyFormatter.format(state.upcomingBillsAmount, state.currency)}"
                                } else {
                                    "No upcoming bill pressure"
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        StatusPill(
                            label = if (state.upcomingBillsCount > 0) "Active" else "Clear",
                            background = if (state.upcomingBillsCount > 0) {
                                MaterialTheme.colorScheme.tertiaryContainer
                            } else {
                                MaterialTheme.colorScheme.secondaryContainer
                            },
                            contentColor = if (state.upcomingBillsCount > 0) {
                                MaterialTheme.colorScheme.onTertiaryContainer
                            } else {
                                MaterialTheme.colorScheme.onSecondaryContainer
                            }
                        )
                    }
                    state.topCategories.firstOrNull()?.let { topCategory ->
                        Text(
                            text = "${topCategory.category.displayName} is the strongest spending category this month.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SectionHeader(
                    title = "Recent transactions",
                    subtitle = "Your latest completed activity."
                )
                TextButton(onClick = onOpenTransactions) {
                    Text("See all")
                }
            }
        }
        if (state.recentTransactions.isEmpty()) {
            item {
                EmptyStateCard(
                    title = "No completed activity yet",
                    message = "Transactions, paid bills, and generated recurring items will start showing up here as your month fills in.",
                    icon = Icons.AutoMirrored.Outlined.ReceiptLong
                )
            }
        } else {
            items(state.recentTransactions.size) { index ->
                val transaction = state.recentTransactions[index]
                TransactionItemCard(
                    transaction = transaction,
                    currency = state.currency,
                    hideSensitiveValues = hideSensitiveValues,
                    onClick = { onTransactionClick(transaction.id) }
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SectionHeader(
                    title = "Insights",
                    subtitle = "Short signals to help steer the month."
                )
                TextButton(onClick = { showGoalDialog = true }) {
                    Text("Add goal")
                }
            }
        }
        if (state.insights.isEmpty()) {
            item {
                EmptyStateCard(
                    title = "Insights will appear here",
                    message = "As your budgets, bills, and goals become active, the app will surface cleaner guidance here.",
                    icon = Icons.Outlined.Insights
                )
            }
        } else {
            items(state.insights.take(3).size) { index ->
                InsightCard(insight = state.insights.take(3)[index])
            }
        }
    }

    if (showGoalDialog) {
        AlertDialog(
            onDismissRequest = { showGoalDialog = false },
            title = { Text("Add Goal") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Create a savings target without leaving the dashboard.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = goalName,
                        onValueChange = { goalName = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Goal name") },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                    )
                    OutlinedTextField(
                        value = goalTarget,
                        onValueChange = { goalTarget = it.filter { character -> character.isDigit() || character == '.' } },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Target amount") },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Decimal,
                            imeAction = ImeAction.Next
                        )
                    )
                    OutlinedTextField(
                        value = goalCurrent,
                        onValueChange = { goalCurrent = it.filter { character -> character.isDigit() || character == '.' } },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Current amount") },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Decimal,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(onDone = { submitGoal() })
                    )
                }
            },
            confirmButton = {
                Button(onClick = { submitGoal() }) {
                    Text("Save goal")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        keyboardController?.hide()
                        focusManager.clearFocus(force = true)
                        showGoalDialog = false
                        goalName = ""
                        goalTarget = ""
                        goalCurrent = ""
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun DashboardHeroCard(
    monthLabel: String,
    balance: String,
    income: String,
    expenses: String,
    budgetTarget: String,
    budgetProgress: Float,
    netWorth: String,
    hideSensitiveValues: Boolean
) {
    Card(
        shape = MaterialTheme.shapes.extraLarge,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.verticalGradient(
                        listOf(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.92f),
                            MaterialTheme.colorScheme.secondary.copy(alpha = 0.86f)
                        )
                    )
                )
                .padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                text = "SMART MONEY VIEW",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.78f)
            )
                    Text(
                        text = "Dashboard",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
                StatusPill(
                    label = monthLabel,
                    background = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.14f),
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Available balance",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.78f)
                )
                com.talent.monthlybudgetkeeper.ui.components.SensitiveValueText(
                    text = balance,
                    hidden = hideSensitiveValues,
                    style = MaterialTheme.typography.displaySmall,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                HeroMetricCard(
                    modifier = Modifier.weight(1f),
                    label = "Income",
                    value = income,
                    hideSensitiveValues = hideSensitiveValues
                )
                HeroMetricCard(
                    modifier = Modifier.weight(1f),
                    label = "Expenses",
                    value = expenses,
                    hideSensitiveValues = hideSensitiveValues
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Budget progress",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Text(
                        text = if (budgetTarget == "Not set") {
                            "No target"
                        } else {
                            "${(budgetProgress * 100).toInt()}% of $budgetTarget"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.82f),
                        maxLines = 2,
                        overflow = TextOverflow.Clip
                    )
                }
                LinearProgressIndicator(
                    progress = { budgetProgress.coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.onPrimary,
                    trackColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.16f)
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Net worth",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.76f)
                )
                com.talent.monthlybudgetkeeper.ui.components.SensitiveValueText(
                    text = netWorth,
                    hidden = hideSensitiveValues,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    }
}

@Composable
private fun HeroMetricCard(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    hideSensitiveValues: Boolean
) {
    Column(
        modifier = modifier
            .background(
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.12f),
                shape = RoundedCornerShape(18.dp)
            )
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.78f)
        )
        com.talent.monthlybudgetkeeper.ui.components.SensitiveValueText(
            text = value,
            hidden = hideSensitiveValues,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onPrimary
        )
    }
}

@Composable
private fun SectionHeader(
    title: String,
    subtitle: String
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(text = title, style = MaterialTheme.typography.titleLarge)
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun StatusPill(
    label: String,
    background: Color,
    contentColor: Color
) {
    Box(
        modifier = Modifier
            .background(color = background, shape = CircleShape)
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = contentColor,
            maxLines = 1
        )
    }
}
