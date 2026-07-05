@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.talent.monthlybudgetkeeper.ui.screens.reports

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.Paid
import androidx.compose.material.icons.outlined.Wallet
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.talent.monthlybudgetkeeper.AppState
import com.talent.monthlybudgetkeeper.data.model.FinancialInsight
import com.talent.monthlybudgetkeeper.data.model.InsightSeverity
import com.talent.monthlybudgetkeeper.ui.components.ActionButtonStyle
import com.talent.monthlybudgetkeeper.ui.components.CategoryBreakdownChart
import com.talent.monthlybudgetkeeper.ui.components.EmptyStateCard
import com.talent.monthlybudgetkeeper.ui.components.IncomeExpenseChart
import com.talent.monthlybudgetkeeper.ui.components.InsightCard
import com.talent.monthlybudgetkeeper.ui.components.MonthSelector
import com.talent.monthlybudgetkeeper.ui.components.MonthlyTrendChart
import com.talent.monthlybudgetkeeper.ui.components.QuickActionButton
import com.talent.monthlybudgetkeeper.ui.components.SensitiveValueText
import com.talent.monthlybudgetkeeper.utils.CsvExporter
import com.talent.monthlybudgetkeeper.utils.CurrencyFormatter
import com.talent.monthlybudgetkeeper.utils.DateUtils
import com.talent.monthlybudgetkeeper.viewmodel.DebtProgressUi
import com.talent.monthlybudgetkeeper.viewmodel.ReportsUiState
import com.talent.monthlybudgetkeeper.viewmodel.ReportsViewModel
import kotlinx.coroutines.launch

private enum class ReportsSection(val label: String, val icon: ImageVector) {
    OVERVIEW("Overview", Icons.Outlined.Insights),
    SPENDING("Spending", Icons.Outlined.Insights),
    BUDGETS("Budgets", Icons.Outlined.Wallet),
    BILLS("Bills", Icons.AutoMirrored.Outlined.ReceiptLong),
    NET_WORTH("Net Worth", Icons.Outlined.Wallet),
    DEBT("Debt", Icons.Outlined.Paid)
}

@Composable
fun ReportsScreen(
    appState: AppState,
    contentPadding: PaddingValues,
    viewModel: ReportsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val hideSensitiveValues = state.privacyModeEnabled
    var selectedSection by rememberSaveable { androidx.compose.runtime.mutableStateOf(ReportsSection.OVERVIEW) }
    val friendlyInsights = rememberInsights(state).take(3)
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        scope.launch {
            CsvExporter.exportMonthlyTransactions(context, uri, state.transactions)
            appState.showMessage("Monthly CSV exported successfully.")
        }
    }

    LazyColumn(
        contentPadding = PaddingValues(
            start = 20.dp,
            end = 20.dp,
            top = contentPadding.calculateTopPadding() + 16.dp,
            bottom = contentPadding.calculateBottomPadding() + 24.dp
        ),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item {
            ReportsHeroCard(
                monthLabel = DateUtils.formatMonth(state.month),
                balance = CurrencyFormatter.format(state.balance, state.currency),
                income = CurrencyFormatter.format(state.totalIncome, state.currency),
                expenses = CurrencyFormatter.format(state.totalExpenses, state.currency),
                hideSensitiveValues = hideSensitiveValues
            )
        }
        item {
            MonthSelector(
                month = state.month,
                onPrevious = viewModel::previousMonth,
                onNext = viewModel::nextMonth
            )
        }
        item {
            QuickActionButton(
                label = "Export CSV",
                icon = Icons.Outlined.Download,
                modifier = Modifier.fillMaxWidth(),
                style = ActionButtonStyle.SECONDARY,
                onClick = { launcher.launch("monthly_budget_keeper_${state.month}.csv") }
            )
        }
        item {
            ReportsSectionPicker(
                selectedSection = selectedSection,
                onSectionSelected = { selectedSection = it }
            )
        }

        when (selectedSection) {
            ReportsSection.OVERVIEW -> overviewSection(state, hideSensitiveValues, friendlyInsights) {
                selectedSection = it
            }
            ReportsSection.SPENDING -> spendingSection(state, hideSensitiveValues)
            ReportsSection.BUDGETS -> budgetsSection(state, hideSensitiveValues)
            ReportsSection.BILLS -> billsSection(state, hideSensitiveValues)
            ReportsSection.NET_WORTH -> netWorthSection(state, hideSensitiveValues)
            ReportsSection.DEBT -> debtSection(state, hideSensitiveValues)
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.overviewSection(
    state: ReportsUiState,
    hideSensitiveValues: Boolean,
    friendlyInsights: List<UiInsight>,
    onAction: (ReportsSection) -> Unit
) {
    item {
        SectionCard(
            title = "Overview",
            subtitle = "Start here for the month at a glance.",
            icon = Icons.Outlined.Insights
        ) {
            MetricsGrid(
                cards = listOf(
                    MetricCardData(
                        "Balance",
                        "This month",
                        CurrencyFormatter.format(state.balance, state.currency),
                        Icons.Outlined.Wallet
                    ),
                    MetricCardData(
                        "Budget",
                        "Income vs spend",
                        CurrencyFormatter.format(
                            state.totalIncome - state.totalExpenses,
                            state.currency
                        ),
                        Icons.Outlined.Wallet
                    ),
                    MetricCardData(
                        "Bills",
                        "Scheduled soon",
                        CurrencyFormatter.format(state.upcomingBills, state.currency),
                        Icons.AutoMirrored.Outlined.ReceiptLong
                    ), // <- missing bracket was here
                    MetricCardData(
                        "Net worth",
                        "Assets less debt",
                        CurrencyFormatter.format(state.netWorth, state.currency),
                        Icons.Outlined.Paid
                    )
                ),
                hideSensitiveValues = hideSensitiveValues
            )
        }
    }
    if (friendlyInsights.isNotEmpty()) {
        item {
            SectionHeader(
                title = "Insights",
                subtitle = "Just the most useful nudges right now.",
                icon = Icons.Outlined.Insights
            )
        }
        items(friendlyInsights) { insight ->
            InsightCard(
                insight = insight.insight,
                actionLabel = insight.actionLabel,
                onAction = insight.targetSection?.let { { onAction(it) } }
            )
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.spendingSection(
    state: ReportsUiState,
    hideSensitiveValues: Boolean
) {
    item {
        SectionCard(
            title = "Spending",
            subtitle = "See what came in, what went out, and where it landed.",
            icon = Icons.Outlined.Insights
        ) {
            MetricsGrid(
                cards = listOf(
                    MetricCardData("Income", "Month total", CurrencyFormatter.format(state.totalIncome, state.currency), Icons.Outlined.Wallet),
                    MetricCardData("Spent", "Month total", CurrencyFormatter.format(state.totalExpenses, state.currency), Icons.Outlined.Paid)
                ),
                hideSensitiveValues = hideSensitiveValues
            )
        }
    }
    item {
        IncomeExpenseChart(
            income = state.totalIncome,
            expense = state.totalExpenses,
            currency = state.currency,
            modifier = if (hideSensitiveValues) Modifier.blur(10.dp) else Modifier
        )
    }
    item {
        if (state.categoryBreakdown.isEmpty()) {
            EmptyStateCard(
                title = "No expense data yet",
                message = "Once expenses land in this month, the category story will show up here.",
                icon = Icons.Outlined.Insights
            )
        } else {
            CategoryBreakdownChart(
                categories = state.categoryBreakdown,
                currency = state.currency,
                modifier = if (hideSensitiveValues) Modifier.blur(10.dp) else Modifier
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
}

private fun androidx.compose.foundation.lazy.LazyListScope.budgetsSection(
    state: ReportsUiState,
    hideSensitiveValues: Boolean
) {
    item {
        SectionCard(
            title = "Budgets",
            subtitle = "Focus on what is still available and what is under pressure.",
            icon = Icons.Outlined.Wallet
        ) {
            if (state.envelopeStatuses.isEmpty()) {
                EmptyStateCard(
                    title = "No active budget view",
                    message = "Envelope progress appears here once budgets are set up for the month.",
                    icon = Icons.Outlined.Wallet
                )
            } else {
                state.envelopeStatuses.forEach { envelope ->
                    CompactProgressRow(
                        title = envelope.name,
                        subtitle = CurrencyFormatter.format(envelope.spentAmount, state.currency),
                        value = CurrencyFormatter.format(envelope.availableAmount, state.currency),
                        progress = envelope.progress.coerceIn(0f, 1f),
                        hideSensitiveValues = hideSensitiveValues
                    )
                }
            }
        }
    }
    if (state.goalProgress.isNotEmpty()) {
        item {
            SectionHeader(
                title = "Goals",
                subtitle = "Savings goals and sinking funds still belong in the budget picture.",
                icon = Icons.Outlined.Insights
            )
        }
        items(state.goalProgress) { goal ->
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
                        Text(
                            text = goal.name,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
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
                }
            }
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.billsSection(
    state: ReportsUiState,
    hideSensitiveValues: Boolean
) {
    item {
        SectionCard(
            title = "Bills",
            subtitle = "Recurring commitments and upcoming charges in plain language.",
            icon = Icons.AutoMirrored.Outlined.ReceiptLong
        ) {
            MetricsGrid(
                cards = listOf(
                    MetricCardData("Upcoming", "This month", CurrencyFormatter.format(state.upcomingBills, state.currency), Icons.AutoMirrored.Outlined.ReceiptLong),
                    MetricCardData("Recurring", "Monthly outflow", CurrencyFormatter.format(state.recurringMonthlyOutflow, state.currency), Icons.Outlined.Paid)
                ),
                hideSensitiveValues = hideSensitiveValues
            )
        }
    }
    item {
        Card(shape = MaterialTheme.shapes.large) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "What this means",
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = if (state.upcomingBills > 0.0) {
                        "You already have scheduled charges coming this month, so this section helps you spot pressure before the cash flow chart gets noisy."
                    } else {
                        "No upcoming bill amount is showing in the selected month, which usually means the schedule is light or bills are already paid."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.netWorthSection(
    state: ReportsUiState,
    hideSensitiveValues: Boolean
) {
    item {
        SectionCard(
            title = "Net Worth",
            subtitle = "Separate assets and liabilities so the number is easier to trust.",
            icon = Icons.Outlined.Wallet
        ) {
            MetricsGrid(
                cards = listOf(
                    MetricCardData("Net worth", "Current", CurrencyFormatter.format(state.netWorth, state.currency), Icons.Outlined.Wallet),
                    MetricCardData("Assets", "Tracked", CurrencyFormatter.format(state.assetTotal, state.currency), Icons.Outlined.Wallet),
                    MetricCardData("Liabilities", "Tracked", CurrencyFormatter.format(state.liabilityTotal, state.currency), Icons.Outlined.Paid),
                    MetricCardData("Debt", "Outstanding", CurrencyFormatter.format(state.debtOutstanding, state.currency), Icons.Outlined.Paid)
                ),
                hideSensitiveValues = hideSensitiveValues
            )
        }
    }
    if (state.netWorthTrend.isNotEmpty()) {
        item {
            SectionCard(
                title = "Trend",
                subtitle = "Recent saved snapshots of your position.",
                icon = Icons.Outlined.Wallet
            ) {
                state.netWorthTrend.forEach { point ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = point.dateLabel,
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        SensitiveValueText(
                            text = CurrencyFormatter.format(point.netWorth, state.currency),
                            hidden = hideSensitiveValues,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }
    if (state.accountSnapshots.isNotEmpty()) {
        item {
            SectionHeader(
                title = "Tracked accounts",
                subtitle = "The biggest balances contributing to the current position.",
                icon = Icons.Outlined.Wallet
            )
        }
        items(state.accountSnapshots) { account ->
            ReportsMetricCard(
                title = account.name,
                subtitle = account.currencyCode,
                value = CurrencyFormatter.format(account.balance, state.currency),
                hideSensitiveValues = hideSensitiveValues,
                icon = Icons.Outlined.Wallet
            )
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.debtSection(
    state: ReportsUiState,
    hideSensitiveValues: Boolean
) {
    item {
        SectionCard(
            title = "Debt",
            subtitle = "Make payoff progress easier to read than one big total.",
            icon = Icons.Outlined.Paid
        ) {
            MetricsGrid(
                cards = listOf(
                    MetricCardData("Outstanding", "Current", CurrencyFormatter.format(state.debtOutstanding, state.currency), Icons.Outlined.Paid),
                    MetricCardData("Items", "Tracked", state.debtProgress.size.toString(), Icons.Outlined.Paid)
                ),
                hideSensitiveValues = hideSensitiveValues
            )
        }
    }
    if (state.debtProgress.isEmpty()) {
        item {
            EmptyStateCard(
                title = "No debt progress yet",
                message = "Once debt balances are tracked, their payoff progress will show here.",
                icon = Icons.Outlined.Paid
            )
        }
    } else {
        items(state.debtProgress) { debt ->
            DebtProgressCard(
                debt = debt,
                currency = state.currency,
                hideSensitiveValues = hideSensitiveValues
            )
        }
    }
}

@Composable
private fun ReportsHeroCard(
    monthLabel: String,
    balance: String,
    income: String,
    expenses: String,
    hideSensitiveValues: Boolean
) {
    Card(
        shape = MaterialTheme.shapes.extraLarge,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.40f),
                    shape = MaterialTheme.shapes.extraLarge
                )
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.weight(1f)) {
                    Text(text = "Reports", style = MaterialTheme.typography.displaySmall, maxLines = 1)
                    Text(
                        text = monthLabel,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Box(
                    modifier = Modifier
                        .background(
                            color = MaterialTheme.colorScheme.surface,
                            shape = CircleShape
                        )
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "Monthly",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1
                    )
                }
            }
            SensitiveValueText(
                text = balance,
                hidden = hideSensitiveValues,
                style = MaterialTheme.typography.headlineMedium
            )
            MetricsGrid(
                cards = listOf(
                    MetricCardData("Income", "Month", income, Icons.Outlined.Wallet),
                    MetricCardData("Expenses", "Month", expenses, Icons.Outlined.Paid)
                ),
                hideSensitiveValues = hideSensitiveValues
            )
        }
    }
}

@Composable
private fun ReportsSectionPicker(
    selectedSection: ReportsSection,
    onSectionSelected: (ReportsSection) -> Unit
) {
    Card(
        shape = MaterialTheme.shapes.large,
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Jump to",
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            androidx.compose.foundation.layout.FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ReportsSection.entries.forEach { section ->
                    FilterChip(
                        selected = selectedSection == section,
                        onClick = { onSectionSelected(section) },
                        label = {
                            Text(
                                text = section.label,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = section.icon,
                                contentDescription = null
                            )
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    content: @Composable () -> Unit
) {
    Card(
        shape = MaterialTheme.shapes.large,
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            SectionHeader(title = title, subtitle = subtitle, icon = icon)
            content()
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    subtitle: String,
    icon: ImageVector
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
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
                style = MaterialTheme.typography.titleLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

private data class MetricCardData(
    val title: String,
    val subtitle: String,
    val value: String,
    val icon: ImageVector
)

@Composable
private fun MetricsGrid(
    cards: List<MetricCardData>,
    hideSensitiveValues: Boolean
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        cards.chunked(2).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                row.forEach { card ->
                    ReportsMetricCard(
                        title = card.title,
                        subtitle = card.subtitle,
                        value = card.value,
                        hideSensitiveValues = hideSensitiveValues,
                        icon = card.icon,
                        modifier = Modifier.weight(1f)
                    )
                }
                if (row.size == 1) {
                    Box(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun ReportsMetricCard(
    title: String,
    subtitle: String,
    value: String,
    hideSensitiveValues: Boolean,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .background(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(14.dp)
                        )
                        .padding(9.dp)
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
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
private fun CompactProgressRow(
    title: String,
    subtitle: String,
    value: String,
    progress: Float,
    hideSensitiveValues: Boolean
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            SensitiveValueText(
                text = value,
                hidden = hideSensitiveValues,
                style = MaterialTheme.typography.titleSmall
            )
        }
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun DebtProgressCard(
    debt: DebtProgressUi,
    currency: com.talent.monthlybudgetkeeper.data.model.CurrencyOption,
    hideSensitiveValues: Boolean
) {
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
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = debt.name,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = debt.dueDateLabel,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                SensitiveValueText(
                    text = CurrencyFormatter.format(debt.remainingAmount, currency),
                    hidden = hideSensitiveValues,
                    style = MaterialTheme.typography.titleMedium
                )
            }
            LinearProgressIndicator(
                progress = { debt.progress },
                modifier = Modifier.fillMaxWidth()
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SensitiveValueText(
                    text = "Target ${CurrencyFormatter.format(debt.totalAmount, currency)}",
                    hidden = hideSensitiveValues,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                SensitiveValueText(
                    text = "Min ${CurrencyFormatter.format(debt.minimumPayment, currency)}",
                    hidden = hideSensitiveValues,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private data class UiInsight(
    val insight: FinancialInsight,
    val actionLabel: String?,
    val targetSection: ReportsSection?
)

@Composable
private fun rememberInsights(state: ReportsUiState): List<UiInsight> {
    return state.insights.mapNotNull { insight ->
        when {
            insight.title.contains("budget", ignoreCase = true) -> {
                UiInsight(
                    insight = insight.copy(
                        title = "Budget check-in",
                        message = insight.message.replace("already used", "already used, so this is a good time to slow the pace a bit.")
                    ),
                    actionLabel = "Open Budgets",
                    targetSection = ReportsSection.BUDGETS
                )
            }
            insight.title.contains("bill", ignoreCase = true) -> {
                UiInsight(
                    insight = insight.copy(
                        title = "Bills need a look",
                        message = insight.message.replace("bill(s)", "bill items")
                    ),
                    actionLabel = "See Bills",
                    targetSection = ReportsSection.BILLS
                )
            }
            insight.title.contains("debt", ignoreCase = true) -> {
                UiInsight(
                    insight = insight.copy(
                        title = "Debt pressure",
                        message = insight.message.replace("need attention soon", "could use attention soon")
                    ),
                    actionLabel = "See Debt",
                    targetSection = ReportsSection.DEBT
                )
            }
            insight.title.contains("net worth", ignoreCase = true) -> {
                UiInsight(
                    insight = insight.copy(
                        title = "Net worth snapshot",
                        message = if (insight.severity == InsightSeverity.POSITIVE) {
                            "Your tracked assets are still ahead right now, which is a solid sign."
                        } else {
                            "Liabilities are outweighing assets right now, so this section is worth checking."
                        }
                    ),
                    actionLabel = "See Net Worth",
                    targetSection = ReportsSection.NET_WORTH
                )
            }
            insight.title.contains("goal", ignoreCase = true) -> {
                UiInsight(
                    insight = insight.copy(
                        title = "Goal momentum",
                        message = insight.message
                    ),
                    actionLabel = "See Budgets",
                    targetSection = ReportsSection.BUDGETS
                )
            }
            else -> null
        }
    }
}
