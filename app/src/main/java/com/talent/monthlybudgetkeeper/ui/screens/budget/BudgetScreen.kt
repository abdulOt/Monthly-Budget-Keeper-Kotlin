package com.talent.monthlybudgetkeeper.ui.screens.budget

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.Wallet
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.talent.monthlybudgetkeeper.data.model.TransactionCategory
import com.talent.monthlybudgetkeeper.ui.components.BudgetProgressCard
import com.talent.monthlybudgetkeeper.ui.components.EmptyStateCard
import com.talent.monthlybudgetkeeper.ui.components.MonthSelector
import com.talent.monthlybudgetkeeper.utils.CurrencyFormatter
import com.talent.monthlybudgetkeeper.viewmodel.BudgetViewModel

@Composable
fun BudgetScreen(
    contentPadding: PaddingValues,
    viewModel: BudgetViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    LazyColumn(
        contentPadding = PaddingValues(
            start = 20.dp,
            end = 20.dp,
            top = contentPadding.calculateTopPadding() + 16.dp,
            bottom = contentPadding.calculateBottomPadding() + 24.dp
        ),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(text = "Budgets", style = MaterialTheme.typography.displaySmall)
                Text(
                    text = "Keep monthly, category, and envelope budgets organized in one planning area without mixing them into transaction history or analytics.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        item {
            MonthSelector(
                month = state.month,
                onPrevious = viewModel::previousMonth,
                onNext = viewModel::nextMonth
            )
        }
        item {
            TotalBudgetEditor(
                currentValue = state.totalBudget,
                currencySymbol = state.currency.symbol,
                onSubmitStart = {
                    keyboardController?.hide()
                    focusManager.clearFocus(force = true)
                },
                onSave = viewModel::saveTotalBudget
            )
        }
        item {
            BudgetProgressCard(
                title = "Monthly budget",
                spentAmount = CurrencyFormatter.format(state.overview.totalSpent, state.currency),
                limitAmount = CurrencyFormatter.format(state.overview.totalBudget, state.currency),
                progress = if (state.overview.totalBudget > 0) {
                    (state.overview.totalSpent / state.overview.totalBudget).toFloat()
                } else {
                    0f
                },
                tone = if (state.overview.totalBudget > 0 && state.overview.totalSpent > state.overview.totalBudget) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.primary
                },
                statusText = "Remaining ${CurrencyFormatter.format(state.overview.remainingBudget, state.currency)}"
            )
        }
        item {
            SectionHeader(
                title = "Category budgets",
                subtitle = "Set guardrails for specific expense categories."
            )
        }
        if (state.overview.categories.isEmpty()) {
            item {
                EmptyStateCard(
                    title = "No categories available",
                    message = "Category budgets will appear here once expense categories are ready to plan.",
                    icon = Icons.Outlined.Wallet
                )
            }
        } else {
            items(state.overview.categories) { categoryStatus ->
                val currentLimit = state.categoryBudgets[categoryStatus.category]?.limitAmount ?: 0.0
                CategoryBudgetEditor(
                    category = categoryStatus.category,
                    currentLimit = currentLimit,
                    spentText = CurrencyFormatter.format(categoryStatus.spent, state.currency),
                    tone = when {
                        categoryStatus.isExceeded -> MaterialTheme.colorScheme.error
                        categoryStatus.isNearLimit -> MaterialTheme.colorScheme.tertiary
                        else -> MaterialTheme.colorScheme.primary
                    },
                    statusText = when {
                        categoryStatus.isExceeded -> "Budget exceeded"
                        categoryStatus.isNearLimit -> "Close to limit"
                        else -> "Remaining ${CurrencyFormatter.format(categoryStatus.remaining, state.currency)}"
                    },
                    onSubmitStart = {
                        keyboardController?.hide()
                        focusManager.clearFocus(force = true)
                    },
                    onSave = { viewModel.saveCategoryBudget(categoryStatus.category, it) }
                )
            }
        }

        item {
            SectionHeader(
                title = "Envelope and rollover budgets",
                subtitle = "See the available balance left in each active envelope."
            )
        }
        if (state.envelopeStatuses.isEmpty()) {
            item {
                EmptyStateCard(
                    title = "No envelope budgets yet",
                    message = "Envelope budgets stay separate from category budgets so you can track planned spending buckets and rollover amounts.",
                    icon = Icons.Outlined.Wallet
                )
            }
        } else {
            items(state.envelopeStatuses) { envelope ->
                BudgetProgressCard(
                    title = envelope.name,
                    spentAmount = CurrencyFormatter.format(envelope.spentAmount, state.currency),
                    limitAmount = CurrencyFormatter.format(
                        envelope.spentAmount + envelope.availableAmount,
                        state.currency
                    ),
                    progress = envelope.progress.coerceIn(0f, 1.5f),
                    tone = if (envelope.progress >= 1f) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.primary
                    },
                    statusText = "Available ${CurrencyFormatter.format(envelope.availableAmount, state.currency)}"
                )
            }
        }

        item {
            SectionHeader(
                title = "Budget alerts",
                subtitle = "Watch for budgets and envelopes that need attention."
            )
        }
        if (state.alertMessages.isEmpty()) {
            item {
                EmptyStateCard(
                    title = "No active budget alerts",
                    message = "When a monthly budget, category budget, or envelope gets close to its limit, the warning will show up here.",
                    icon = Icons.Outlined.NotificationsActive
                )
            }
        } else {
            items(state.alertMessages) { alert ->
                Card {
                    Text(
                        text = alert,
                        modifier = Modifier.padding(18.dp),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    subtitle: String
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(text = title, style = MaterialTheme.typography.titleLarge)
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun TotalBudgetEditor(
    currentValue: Double,
    currencySymbol: String,
    onSubmitStart: () -> Unit,
    onSave: (String) -> Unit
) {
    var value by remember(currentValue) {
        mutableStateOf(if (currentValue == 0.0) "" else currentValue.toInt().toString())
    }
    Card {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(text = "Monthly budget", style = MaterialTheme.typography.titleMedium)
            Text(
                text = "This is the overall spending plan for the selected month.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            OutlinedTextField(
                value = value,
                onValueChange = { value = it.filter(Char::isDigit) },
                label = { Text("Budget amount") },
                prefix = { Text(currencySymbol) },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        onSubmitStart()
                        onSave(value)
                    }
                )
            )
            Button(
                onClick = {
                    onSubmitStart()
                    onSave(value)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save monthly budget")
            }
        }
    }
}

@Composable
private fun CategoryBudgetEditor(
    category: TransactionCategory,
    currentLimit: Double,
    spentText: String,
    tone: androidx.compose.ui.graphics.Color,
    statusText: String,
    onSubmitStart: () -> Unit,
    onSave: (String) -> Unit
) {
    var value by remember(currentLimit) {
        mutableStateOf(if (currentLimit == 0.0) "" else currentLimit.toInt().toString())
    }
    Card {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(text = category.displayName, style = MaterialTheme.typography.titleMedium)
            Text(
                text = "Spent this month: $spentText",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            OutlinedTextField(
                value = value,
                onValueChange = { value = it.filter(Char::isDigit) },
                label = { Text("Category budget") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        onSubmitStart()
                        onSave(value)
                    }
                )
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = statusText, color = tone, style = MaterialTheme.typography.bodyMedium)
                Button(
                    onClick = {
                        onSubmitStart()
                        onSave(value)
                    }
                ) {
                    Text("Save")
                }
            }
        }
    }
}
