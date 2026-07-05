@file:OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)

package com.talent.monthlybudgetkeeper.ui.screens.transactions

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.CompareArrows
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ArrowCircleDown
import androidx.compose.material.icons.outlined.ArrowCircleUp
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.talent.monthlybudgetkeeper.data.local.entity.AccountEntity
import com.talent.monthlybudgetkeeper.data.local.entity.TransferEntity
import com.talent.monthlybudgetkeeper.data.model.CurrencyOption
import com.talent.monthlybudgetkeeper.data.model.TransactionType
import com.talent.monthlybudgetkeeper.ui.components.DatePickerField
import com.talent.monthlybudgetkeeper.ui.components.EmptyStateCard
import com.talent.monthlybudgetkeeper.ui.components.MonthSelector
import com.talent.monthlybudgetkeeper.ui.components.SensitiveValueText
import com.talent.monthlybudgetkeeper.ui.components.TransactionItemCard
import com.talent.monthlybudgetkeeper.utils.CurrencyFormatter
import com.talent.monthlybudgetkeeper.utils.DateUtils
import com.talent.monthlybudgetkeeper.viewmodel.TransactionsViewModel
import java.time.LocalDate
import kotlinx.coroutines.flow.collectLatest

private enum class TransactionsTab(val label: String) {
    ALL("All"),
    INCOME("Income"),
    EXPENSES("Expenses"),
    TRANSFERS("Transfers")
}

private enum class AddRecordAction(val label: String) {
    INCOME("Income"),
    EXPENSE("Expense"),
    TRANSFER("Transfer")
}

@Composable
fun TransactionsScreen(
    contentPadding: PaddingValues,
    onAddIncome: () -> Unit,
    onAddExpense: () -> Unit,
    onTransactionClick: (Long) -> Unit,
    viewModel: TransactionsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    var selectedTab by rememberSaveable { mutableStateOf(TransactionsTab.ALL) }
    var showActionSheet by remember { mutableStateOf(false) }
    var showTransferDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.transferSaved.collectLatest {
            showTransferDialog = false
        }
    }

    val filteredTransactions = when (selectedTab) {
        TransactionsTab.ALL -> state.transactions
        TransactionsTab.INCOME -> state.transactions.filter { !it.isTransfer && it.type == TransactionType.INCOME }
        TransactionsTab.EXPENSES -> state.transactions.filter { !it.isTransfer && it.type == TransactionType.EXPENSE }
        TransactionsTab.TRANSFERS -> emptyList()
    }
    val totalIncome = state.transactions
        .filter { !it.isTransfer && it.type == TransactionType.INCOME }
        .sumOf { it.amount }
    val totalExpenses = state.transactions
        .filter { !it.isTransfer && it.type == TransactionType.EXPENSE }
        .sumOf { it.amount }
    val accountNameByRemoteId = remember(state.accounts) {
        state.accounts.associateBy { it.remoteId.orEmpty() }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            contentPadding = PaddingValues(
                start = 20.dp,
                end = 20.dp,
                top = contentPadding.calculateTopPadding() + 16.dp,
                bottom = contentPadding.calculateBottomPadding() + 92.dp
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                TransactionsHeroCard(
                    monthLabel = DateUtils.formatMonth(state.month),
                    transactionCount = state.transactions.size,
                    transferCount = state.transfers.size,
                    income = CurrencyFormatter.format(totalIncome, state.currency),
                    expenses = CurrencyFormatter.format(totalExpenses, state.currency),
                    hideSensitiveValues = state.privacyModeEnabled
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
                OutlinedTextField(
                    value = state.searchQuery,
                    onValueChange = viewModel::updateSearch,
                    label = { Text("Search transactions") },
                    supportingText = { Text("Search titles, notes, and transfer notes in the selected month.") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(
                        onSearch = {
                            keyboardController?.hide()
                            focusManager.clearFocus(force = true)
                        }
                    )
                )
            }
            item {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TransactionsTab.entries.forEach { tab ->
                        FilterChip(
                            selected = selectedTab == tab,
                            onClick = {
                                keyboardController?.hide()
                                focusManager.clearFocus(force = true)
                                selectedTab = tab
                            },
                            label = {
                                Text(
                                    text = tab.label,
                                    maxLines = 2,
                                    overflow = TextOverflow.Clip
                                )
                            }
                        )
                    }
                }
            }
            item {
                ActionStrip(
                    onAddIncome = onAddIncome,
                    onAddExpense = onAddExpense,
                    onAddTransfer = { showTransferDialog = true }
                )
            }

            if (selectedTab == TransactionsTab.TRANSFERS) {
                if (state.transfers.isEmpty()) {
                    item {
                        EmptyStateCard(
                            title = "No transfer records yet",
                            message = "Transfers appear here after money moves between accounts and stay separate from income and expenses.",
                            icon = Icons.AutoMirrored.Outlined.CompareArrows
                        )
                    }
                } else {
                    items(state.transfers) { transfer ->
                        TransferRecordCard(
                            transfer = transfer,
                            currency = state.currency,
                            hideSensitiveValues = state.privacyModeEnabled,
                            fromAccount = accountNameByRemoteId[transfer.fromAccountRemoteId],
                            toAccount = accountNameByRemoteId[transfer.toAccountRemoteId]
                        )
                    }
                }
            } else if (filteredTransactions.isEmpty()) {
                item {
                    EmptyStateCard(
                        title = when (selectedTab) {
                            TransactionsTab.ALL -> "No transactions recorded yet"
                            TransactionsTab.INCOME -> "No income records in this month"
                            TransactionsTab.EXPENSES -> "No expense records in this month"
                            TransactionsTab.TRANSFERS -> "No transfer records yet"
                        },
                        message = when (selectedTab) {
                            TransactionsTab.ALL -> "Use the add menu for income, expense, or transfer records. Bills and subscriptions appear here only after they are paid."
                            TransactionsTab.INCOME -> "Income entries stay separate so the money-in side of your history stays easy to scan."
                            TransactionsTab.EXPENSES -> "Paid bills and generated recurring items become expenses and appear here."
                            TransactionsTab.TRANSFERS -> "Transfers appear here after money actually moves between accounts."
                        },
                        icon = Icons.AutoMirrored.Outlined.ReceiptLong
                    )
                }
            } else {
                items(filteredTransactions) { transaction ->
                    TransactionItemCard(
                        transaction = transaction,
                        currency = state.currency,
                        hideSensitiveValues = state.privacyModeEnabled,
                        onClick = { onTransactionClick(transaction.id) }
                    )
                }
            }
        }

        if (selectedTab != TransactionsTab.TRANSFERS && !showTransferDialog) {
            FloatingActionButton(
                onClick = {
                    keyboardController?.hide()
                    focusManager.clearFocus(force = true)
                    showActionSheet = true
                },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 24.dp, bottom = contentPadding.calculateBottomPadding() + 20.dp)
            ) {
                Icon(Icons.Outlined.Add, contentDescription = "Add record")
            }
        }
    }

    if (showActionSheet) {
        AlertDialog(
            onDismissRequest = { showActionSheet = false },
            title = { Text("Add a record") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Choose the kind of record you want to add.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    AddRecordAction.entries.forEach { action ->
                        TextButton(
                            onClick = {
                                showActionSheet = false
                                when (action) {
                                    AddRecordAction.INCOME -> onAddIncome()
                                    AddRecordAction.EXPENSE -> onAddExpense()
                                    AddRecordAction.TRANSFER -> showTransferDialog = true
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = when (action) {
                                        AddRecordAction.INCOME -> Icons.Outlined.ArrowCircleUp
                                        AddRecordAction.EXPENSE -> Icons.Outlined.ArrowCircleDown
                                        AddRecordAction.TRANSFER -> Icons.Outlined.SwapHoriz
                                    },
                                    contentDescription = null
                                )
                                Text(action.label)
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showActionSheet = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showTransferDialog) {
        TransferComposerDialog(
            accounts = state.accounts,
            currency = state.currency,
            onDismiss = { showTransferDialog = false },
            onSave = { fromAccountRemoteId, toAccountRemoteId, amount, date, note ->
                viewModel.saveTransfer(
                    fromAccountRemoteId = fromAccountRemoteId,
                    toAccountRemoteId = toAccountRemoteId,
                    amount = amount,
                    date = date,
                    note = note
                )
            }
        )
    }
}

@Composable
private fun ActionStrip(
    onAddIncome: () -> Unit,
    onAddExpense: () -> Unit,
    onAddTransfer: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        QuickAddCard(
            modifier = Modifier.weight(1f),
            title = "Add Income",
            icon = Icons.Outlined.ArrowCircleUp,
            onClick = onAddIncome
        )
        QuickAddCard(
            modifier = Modifier.weight(1f),
            title = "Add Expense",
            icon = Icons.Outlined.ArrowCircleDown,
            onClick = onAddExpense
        )
        QuickAddCard(
            modifier = Modifier.weight(1f),
            title = "Add Transfer",
            icon = Icons.Outlined.SwapHoriz,
            onClick = onAddTransfer
        )
    }
}

@Composable
private fun QuickAddCard(
    modifier: Modifier = Modifier,
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .background(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = CircleShape
                    )
                    .padding(8.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 2,
                overflow = TextOverflow.Clip
            )
        }
    }
}

@Composable
private fun TransactionsHeroCard(
    monthLabel: String,
    transactionCount: Int,
    transferCount: Int,
    income: String,
    expenses: String,
    hideSensitiveValues: Boolean
) {
    Card(
        shape = MaterialTheme.shapes.extraLarge,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
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
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Transactions", style = MaterialTheme.typography.displaySmall)
                    Text(
                        text = monthLabel,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
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
                        text = "$transactionCount items",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                TransactionMetric(
                    modifier = Modifier.weight(1f),
                    label = "Income",
                    value = income,
                    hideSensitiveValues = hideSensitiveValues
                )
                TransactionMetric(
                    modifier = Modifier.weight(1f),
                    label = "Expenses",
                    value = expenses,
                    hideSensitiveValues = hideSensitiveValues
                )
            }
            Text(
                text = "$transferCount transfer records are kept separate from reports and spending totals.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun TransactionMetric(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    hideSensitiveValues: Boolean
) {
    Column(
        modifier = modifier
            .background(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(20.dp)
            )
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        SensitiveValueText(
            text = value,
            hidden = hideSensitiveValues,
            style = MaterialTheme.typography.titleMedium
        )
    }
}

@Composable
private fun TransferRecordCard(
    transfer: TransferEntity,
    currency: CurrencyOption,
    hideSensitiveValues: Boolean,
    fromAccount: AccountEntity?,
    toAccount: AccountEntity?
) {
    Card(
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = MaterialTheme.shapes.large
    ) {
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
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .background(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(14.dp)
                            )
                            .padding(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.CompareArrows,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text("Transfer", style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = DateUtils.formatDate(transfer.date),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                SensitiveValueText(
                    text = CurrencyFormatter.format(transfer.amount, currency),
                    hidden = hideSensitiveValues,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Text(
                text = "${fromAccount?.name ?: "Source account"} to ${toAccount?.name ?: "Destination account"}",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = if (transfer.note.isBlank()) {
                    "Moved between linked accounts."
                } else {
                    transfer.note
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Transfers do not count as income or expense.",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun TransferComposerDialog(
    accounts: List<AccountEntity>,
    currency: CurrencyOption,
    onDismiss: () -> Unit,
    onSave: (String, String, String, LocalDate, String) -> Unit
) {
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val accountRemoteIds = remember(accounts) { accounts.mapNotNull { it.remoteId } }
    var fromAccountRemoteId by rememberSaveable(accountRemoteIds) {
        mutableStateOf(accountRemoteIds.firstOrNull().orEmpty())
    }
    var toAccountRemoteId by rememberSaveable(accountRemoteIds) {
        mutableStateOf(accountRemoteIds.firstOrNull { it != fromAccountRemoteId }.orEmpty())
    }
    var amount by rememberSaveable { mutableStateOf("") }
    var note by rememberSaveable { mutableStateOf("") }
    var date by rememberSaveable { mutableStateOf(LocalDate.now()) }

    LaunchedEffect(accountRemoteIds, fromAccountRemoteId, toAccountRemoteId) {
        if (fromAccountRemoteId !in accountRemoteIds) {
            fromAccountRemoteId = accountRemoteIds.firstOrNull().orEmpty()
        }
        if (toAccountRemoteId !in accountRemoteIds || toAccountRemoteId == fromAccountRemoteId) {
            toAccountRemoteId = accountRemoteIds.firstOrNull { it != fromAccountRemoteId }.orEmpty()
        }
    }

    fun submit() {
        keyboardController?.hide()
        focusManager.clearFocus(force = true)
        onSave(fromAccountRemoteId, toAccountRemoteId, amount, date, note)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add transfer") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Move money between accounts without affecting income or expense reports.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Currency: ${currency.code}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(text = "From account", style = MaterialTheme.typography.titleSmall)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    accounts.forEach { account ->
                        val remoteId = account.remoteId.orEmpty()
                        FilterChip(
                            selected = fromAccountRemoteId == remoteId,
                            onClick = {
                                fromAccountRemoteId = remoteId
                                if (toAccountRemoteId == remoteId) {
                                    toAccountRemoteId = accounts.firstOrNull { it.remoteId != remoteId }?.remoteId.orEmpty()
                                }
                            },
                            label = {
                                Text(
                                    text = account.name,
                                    maxLines = 2,
                                    overflow = TextOverflow.Clip
                                )
                            }
                        )
                    }
                }
                Text(text = "To account", style = MaterialTheme.typography.titleSmall)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    accounts.forEach { account ->
                        val remoteId = account.remoteId.orEmpty()
                        FilterChip(
                            selected = toAccountRemoteId == remoteId,
                            enabled = remoteId != fromAccountRemoteId,
                            onClick = { toAccountRemoteId = remoteId },
                            label = {
                                Text(
                                    text = account.name,
                                    maxLines = 2,
                                    overflow = TextOverflow.Clip
                                )
                            }
                        )
                    }
                }
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it.filter { character -> character.isDigit() || character == '.' } },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Amount") },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal,
                        imeAction = ImeAction.Next
                    )
                )
                DatePickerField(
                    label = "Transfer date",
                    selectedDate = date,
                    modifier = Modifier.fillMaxWidth(),
                    onDateSelected = { date = it }
                )
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Note") },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { submit() })
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { submit() },
                enabled = accounts.size >= 2 &&
                    fromAccountRemoteId.isNotBlank() &&
                    toAccountRemoteId.isNotBlank() &&
                    fromAccountRemoteId != toAccountRemoteId &&
                    (amount.toDoubleOrNull() ?: 0.0) > 0.0
            ) {
                Text("Save transfer")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
