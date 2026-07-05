package com.talent.monthlybudgetkeeper.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.CallMade
import androidx.compose.material.icons.automirrored.outlined.CallReceived
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.ArrowCircleDown
import androidx.compose.material.icons.outlined.ArrowCircleUp
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.PieChartOutline
import androidx.compose.material.icons.outlined.Wallet
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.talent.monthlybudgetkeeper.ui.components.ActionButtonStyle
import com.talent.monthlybudgetkeeper.ui.components.CategoryBreakdownChart
import com.talent.monthlybudgetkeeper.ui.components.DashboardAmountCard
import com.talent.monthlybudgetkeeper.ui.components.EmptyStateCard
import com.talent.monthlybudgetkeeper.ui.components.InsightCard
import com.talent.monthlybudgetkeeper.ui.components.MonthlyTrendChart
import com.talent.monthlybudgetkeeper.ui.components.QuickActionButton
import com.talent.monthlybudgetkeeper.ui.components.SensitiveValueText
import com.talent.monthlybudgetkeeper.ui.components.TransactionItemCard
import com.talent.monthlybudgetkeeper.utils.CurrencyFormatter
import com.talent.monthlybudgetkeeper.utils.DateUtils
import com.talent.monthlybudgetkeeper.viewmodel.HomeViewModel
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions

@Composable
fun HomeScreen(
    contentPadding: PaddingValues,
    onAddIncome: () -> Unit,
    onAddExpense: () -> Unit,
    onTransactionClick: (Long) -> Unit,
    onViewHistory: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val balanceText = CurrencyFormatter.format(state.remainingBalance, state.currency)
    val hideSensitiveValues = state.privacyModeEnabled
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    var showNewCycleDialog by remember(state.cycleRenewalDue) { mutableStateOf(false) }
    var renewalIncome by remember(state.cycleRenewalDue, state.cyclePlannedIncome) {
        mutableStateOf(state.cyclePlannedIncome.takeIf { it > 0.0 }?.toInt()?.toString().orEmpty())
    }
    var renewalBudget by remember(state.cycleRenewalDue, state.cyclePlannedBudget) {
        mutableStateOf(state.cyclePlannedBudget.takeIf { it > 0.0 }?.toInt()?.toString().orEmpty())
    }

    LazyColumn(
        contentPadding = PaddingValues(
            start = 20.dp,
            end = 20.dp,
            top = contentPadding.calculateTopPadding() + 12.dp,
            bottom = contentPadding.calculateBottomPadding() + 28.dp
        ),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = DateUtils.formatMonth(state.month).uppercase(),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Your financial pulse",
                    style = MaterialTheme.typography.displaySmall
                )
                Text(
                    text = "Track cash flow, obligations, and long-term progress from one calm monthly view.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        if (state.cycleRenewalDue) {
            item {
                CycleRenewalCard(
                    message = state.cycleRenewalMessage,
                    onContinuePrevious = {
                        viewModel.continuePreviousCycleBudget()
                    },
                    onEnterNewBudget = {
                        renewalIncome = state.cyclePlannedIncome.takeIf { it > 0.0 }?.toInt()?.toString().orEmpty()
                        renewalBudget = state.cyclePlannedBudget.takeIf { it > 0.0 }?.toInt()?.toString().orEmpty()
                        showNewCycleDialog = true
                    }
                )
            }
        }
        item {
            DashboardAmountCard(
                title = "Remaining balance",
                amount = balanceText,
                subtitle = "Income minus expenses for the current month",
                badge = if (state.monthlyBudget > 0.0) {
                    "${(state.budgetUsedPercentage * 100).toInt()}% of budget used"
                } else {
                    "No monthly budget set yet"
                },
                colors = listOf(
                    MaterialTheme.colorScheme.primaryContainer,
                    MaterialTheme.colorScheme.secondaryContainer
                ),
                icon = Icons.Outlined.Wallet,
                minHeight = 206.dp,
                hideAmount = hideSensitiveValues
            )
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                DashboardAmountCard(
                    title = "Income",
                    amount = CurrencyFormatter.format(state.totalIncome, state.currency),
                    colors = listOf(Color(0xFFDDF5EB), Color(0xFFF7FCF9)),
                    modifier = Modifier.weight(1f),
                    icon = Icons.AutoMirrored.Outlined.CallReceived,
                    amountStyle = MaterialTheme.typography.titleLarge,
                    minHeight = 156.dp,
                    hideAmount = hideSensitiveValues
                )
                DashboardAmountCard(
                    title = "Expenses",
                    amount = CurrencyFormatter.format(state.totalExpenses, state.currency),
                    colors = listOf(Color(0xFFFCE4E6), Color(0xFFFFF4F5)),
                    modifier = Modifier.weight(1f),
                    icon = Icons.AutoMirrored.Outlined.CallMade,
                    amountStyle = MaterialTheme.typography.titleLarge,
                    minHeight = 156.dp,
                    hideAmount = hideSensitiveValues
                )
            }
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                QuickActionButton(
                    label = "Add income",
                    icon = Icons.Outlined.ArrowCircleUp,
                    modifier = Modifier.weight(1f),
                    style = ActionButtonStyle.SECONDARY,
                    onClick = onAddIncome
                )
                QuickActionButton(
                    label = "Add expense",
                    icon = Icons.Outlined.ArrowCircleDown,
                    modifier = Modifier.weight(1f),
                    style = ActionButtonStyle.PRIMARY,
                    onClick = onAddExpense
                )
            }
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                DashboardAmountCard(
                    title = "Net worth",
                    amount = CurrencyFormatter.format(state.netWorth, state.currency),
                    subtitle = "Assets less tracked liabilities",
                    colors = listOf(Color(0xFFE8F0FF), Color(0xFFF8FAFF)),
                    modifier = Modifier.weight(1f),
                    icon = Icons.Outlined.Insights,
                    amountStyle = MaterialTheme.typography.titleLarge,
                    minHeight = 156.dp,
                    hideAmount = hideSensitiveValues
                )
                DashboardAmountCard(
                    title = "Bills due soon",
                    amount = "${state.upcomingBillsCount}",
                    subtitle = if (state.upcomingBillsCount > 0) {
                        "${state.upcomingBillsStatusText}. ${CurrencyFormatter.format(state.upcomingBillsAmount, state.currency)}"
                    } else {
                        "No bills due soon"
                    },
                    colors = listOf(Color(0xFFFFF1E0), Color(0xFFFFFBF5)),
                    modifier = Modifier.weight(1f),
                    icon = Icons.AutoMirrored.Outlined.ReceiptLong,
                    amountStyle = MaterialTheme.typography.titleLarge,
                    minHeight = 156.dp,
                    hideAmount = hideSensitiveValues
                )
            }
        }
        item {
            MonthlyTrendChart(
                trends = state.trends,
                currency = state.currency,
                modifier = if (hideSensitiveValues) Modifier.blur(10.dp) else Modifier
            )
        }

        if (state.accountSnapshots.isNotEmpty()) {
            item {
                SectionHeading(
                    title = "Accounts and net worth",
                    subtitle = "Tracked balances shaping your overall financial position.",
                    icon = Icons.Outlined.Wallet
                )
            }
            items(state.accountSnapshots.size) { index ->
                val account = state.accountSnapshots[index]
                HighlightMetricRow(
                    title = account.name,
                    subtitle = account.currencyCode,
                    value = CurrencyFormatter.format(account.balance, state.currency),
                    hideSensitiveValues = hideSensitiveValues
                )
            }
        }

        if (state.goalProgress.isNotEmpty()) {
            item {
                SectionHeading(
                    title = "Savings goals",
                    subtitle = "Your top goals and sinking funds in motion.",
                    icon = Icons.Outlined.ArrowCircleUp
                )
            }
            items(state.goalProgress.size) { index ->
                val goal = state.goalProgress[index]
                Card(shape = MaterialTheme.shapes.large) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(goal.name, style = MaterialTheme.typography.titleMedium)
                                Text(
                                    text = "Target progress",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .background(
                                        color = MaterialTheme.colorScheme.secondaryContainer,
                                        shape = CircleShape
                                    )
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = "${(goal.completionRatio * 100).toInt()}%",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }
                        LinearProgressIndicator(
                            progress = { goal.completionRatio },
                            modifier = Modifier.fillMaxWidth()
                        )
                        SensitiveValueText(
                            text = "${CurrencyFormatter.format(goal.currentAmount, state.currency)} of ${CurrencyFormatter.format(goal.targetAmount, state.currency)}",
                            hidden = hideSensitiveValues,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        if (state.envelopeStatuses.isNotEmpty()) {
            item {
                SectionHeading(
                    title = "Envelope budgeting",
                    subtitle = "Available balances across your most active envelopes.",
                    icon = Icons.Outlined.PieChartOutline
                )
            }
            items(state.envelopeStatuses.size) { index ->
                val envelope = state.envelopeStatuses[index]
                Card(shape = MaterialTheme.shapes.large) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(envelope.name, style = MaterialTheme.typography.titleMedium)
                            SensitiveValueText(
                                text = CurrencyFormatter.format(envelope.availableAmount, state.currency),
                                hidden = hideSensitiveValues,
                                style = MaterialTheme.typography.titleSmall
                            )
                        }
                        LinearProgressIndicator(
                            progress = { envelope.progress.coerceIn(0f, 1f) },
                            modifier = Modifier.fillMaxWidth()
                        )
                        SensitiveValueText(
                            text = "Spent ${CurrencyFormatter.format(envelope.spentAmount, state.currency)}",
                            hidden = hideSensitiveValues,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        if (state.insights.isNotEmpty()) {
            item {
                SectionHeading(
                    title = "Financial health insights",
                    subtitle = "A few signals worth watching this month.",
                    icon = Icons.Outlined.Insights
                )
            }
            items(state.insights.size) { index ->
                InsightCard(insight = state.insights[index])
            }
        }

        item {
            SectionHeading(
                title = "Top spending categories",
                subtitle = "The categories shaping this month the most.",
                icon = Icons.Outlined.PieChartOutline
            )
        }
        item {
            if (state.topCategories.isEmpty()) {
                EmptyStateCard(
                    title = "No expense activity yet",
                    message = "Your top categories will appear here as soon as you record some spending.",
                    icon = Icons.AutoMirrored.Outlined.ReceiptLong
                )
            } else {
                CategoryBreakdownChart(
                    categories = state.topCategories,
                    currency = state.currency,
                    modifier = if (hideSensitiveValues) Modifier.blur(10.dp) else Modifier
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SectionHeading(
                    title = "Recent transactions",
                    subtitle = "Your latest activity at a glance.",
                    icon = Icons.Outlined.Insights,
                    modifier = Modifier.weight(1f)
                )
                TextButton(onClick = onViewHistory) {
                    Text("See all")
                }
            }
        }
        if (state.recentTransactions.isEmpty()) {
            item {
                EmptyStateCard(
                    title = "Nothing to show yet",
                    message = "Add your first income or expense to start building a monthly picture.",
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
    }

    if (showNewCycleDialog && state.cycleRenewalDue) {
        AlertDialog(
            onDismissRequest = {
                keyboardController?.hide()
                focusManager.clearFocus(force = true)
                showNewCycleDialog = false
            },
            title = { Text("Start new ${state.cycleType.label.lowercase()} cycle") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Enter the income and budget you want to use for the next cycle.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = renewalIncome,
                        onValueChange = {
                            renewalIncome = it.filter { character -> character.isDigit() || character == '.' }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("${state.cycleType.label} income") },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Decimal,
                            imeAction = ImeAction.Next
                        )
                    )
                    OutlinedTextField(
                        value = renewalBudget,
                        onValueChange = {
                            renewalBudget = it.filter { character -> character.isDigit() || character == '.' }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("${state.cycleType.label} budget") },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Decimal,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                viewModel.saveNewCycleBudget(renewalIncome, renewalBudget)
                                keyboardController?.hide()
                                focusManager.clearFocus(force = true)
                                showNewCycleDialog = false
                            }
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.saveNewCycleBudget(renewalIncome, renewalBudget)
                        keyboardController?.hide()
                        focusManager.clearFocus(force = true)
                        showNewCycleDialog = false
                    },
                    enabled = (renewalIncome.toDoubleOrNull() ?: 0.0) > 0.0 &&
                        (renewalBudget.toDoubleOrNull() ?: 0.0) > 0.0
                ) {
                    Text("Save cycle")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        keyboardController?.hide()
                        focusManager.clearFocus(force = true)
                        showNewCycleDialog = false
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun SectionHeading(
    title: String,
    subtitle: String,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .background(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.82f),
                    shape = CircleShape
                )
                .padding(10.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun HighlightMetricRow(
    title: String,
    subtitle: String,
    value: String,
    hideSensitiveValues: Boolean
) {
    Card(shape = MaterialTheme.shapes.large) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            SensitiveValueText(
                text = value,
                hidden = hideSensitiveValues,
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}

@Composable
private fun CycleRenewalCard(
    message: String,
    onContinuePrevious: () -> Unit,
    onEnterNewBudget: () -> Unit
) {
    Card(shape = MaterialTheme.shapes.large) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Budget cycle ready to renew",
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                TextButton(
                    onClick = onContinuePrevious,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Continue previous")
                }
                Button(
                    onClick = onEnterNewBudget,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Enter new plan")
                }
            }
        }
    }
}
