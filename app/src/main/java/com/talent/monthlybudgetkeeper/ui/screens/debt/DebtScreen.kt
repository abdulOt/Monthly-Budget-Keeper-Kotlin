@file:OptIn(ExperimentalLayoutApi::class)

package com.talent.monthlybudgetkeeper.ui.screens.debt

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.talent.monthlybudgetkeeper.data.local.entity.AccountEntity
import com.talent.monthlybudgetkeeper.data.local.entity.DebtEntity
import com.talent.monthlybudgetkeeper.ui.components.ActionButtonStyle
import com.talent.monthlybudgetkeeper.ui.components.DatePickerField
import com.talent.monthlybudgetkeeper.ui.components.EmptyStateCard
import com.talent.monthlybudgetkeeper.ui.components.QuickActionButton
import com.talent.monthlybudgetkeeper.ui.components.SensitiveValueText
import com.talent.monthlybudgetkeeper.utils.CurrencyFormatter
import com.talent.monthlybudgetkeeper.utils.DateUtils
import com.talent.monthlybudgetkeeper.viewmodel.DebtViewModel
import java.time.LocalDate

@Composable
fun DebtScreen(
    contentPadding: PaddingValues,
    viewModel: DebtViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var editingDebt by remember { mutableStateOf<DebtEntity?>(null) }
    var payingDebtId by rememberSaveable { mutableStateOf<Long?>(null) }
    val payingDebt = state.debts.firstOrNull { it.id == payingDebtId }

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
            DebtHeroCard(
                totalOutstanding = CurrencyFormatter.format(state.totalOutstanding, state.currency),
                dueSoonCount = state.dueSoonCount,
                averageProgress = state.averageProgress,
                hideSensitiveValues = state.privacyModeEnabled
            )
        }
        item {
            QuickActionButton(
                label = "Add Debt",
                icon = Icons.Outlined.Add,
                modifier = Modifier.fillMaxWidth(),
                style = ActionButtonStyle.PRIMARY,
                onClick = { editingDebt = DebtEntity(name = "", lender = "", totalAmount = 0.0, remainingAmount = 0.0) }
            )
        }

        if (state.debts.isEmpty()) {
            item {
                EmptyStateCard(
                    title = "No debts tracked yet",
                    message = "Add loans, financing, or cards here so payments and payoff progress stop disappearing from your numbers.",
                    icon = Icons.Outlined.CreditCard
                )
            }
        } else {
            items(state.debts, key = { it.id }) { debt ->
                DebtRecordCard(
                    debt = debt,
                    currency = state.currency,
                    hideSensitiveValues = state.privacyModeEnabled,
                    onEdit = { editingDebt = debt },
                    onPay = { payingDebtId = debt.id }
                )
            }
        }
    }

    editingDebt?.let { debt ->
        DebtEditorDialog(
            existingDebt = debt.takeIf { it.id != 0L },
            onDismiss = { editingDebt = null },
            onDelete = { existingDebt ->
                viewModel.deleteDebt(existingDebt)
                editingDebt = null
            },
            onSave = { existingDebt, name, balance, minimumPayment, dueDate, interestRate, note ->
                viewModel.saveDebt(
                    existingDebt = existingDebt,
                    name = name,
                    totalBalance = balance,
                    minimumPayment = minimumPayment,
                    dueDate = dueDate,
                    interestRate = interestRate,
                    note = note
                )
                editingDebt = null
            }
        )
    }

    payingDebt?.let { debt ->
        DebtPaymentDialog(
            debt = debt,
            currencyLabel = state.currency.code,
            accounts = state.paymentAccounts,
            onDismiss = { payingDebtId = null },
            onConfirm = { amount, date, accountRemoteId, createExpense, note ->
                viewModel.payDebt(
                    debtId = debt.id,
                    amount = amount,
                    paymentDate = date,
                    paymentAccountRemoteId = accountRemoteId,
                    createExpenseTransaction = createExpense,
                    note = note
                )
                payingDebtId = null
            }
        )
    }
}

@Composable
private fun DebtHeroCard(
    totalOutstanding: String,
    dueSoonCount: Int,
    averageProgress: Float,
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
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.34f),
                    shape = MaterialTheme.shapes.extraLarge
                )
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(text = "Debts", style = MaterialTheme.typography.displaySmall)
            Text(
                text = "Track balances, due dates, and payoff movement in one place.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            SensitiveValueText(
                text = totalOutstanding,
                hidden = hideSensitiveValues,
                style = MaterialTheme.typography.headlineMedium
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                DebtHeroMetric(
                    modifier = Modifier.weight(1f),
                    label = "Due soon",
                    value = dueSoonCount.toString()
                )
                DebtHeroMetric(
                    modifier = Modifier.weight(1f),
                    label = "Paid down",
                    value = "${(averageProgress * 100).toInt()}%"
                )
            }
        }
    }
}

@Composable
private fun DebtHeroMetric(
    modifier: Modifier = Modifier,
    label: String,
    value: String
) {
    Column(
        modifier = modifier
            .background(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(18.dp)
            )
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(text = value, style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
private fun DebtRecordCard(
    debt: DebtEntity,
    currency: com.talent.monthlybudgetkeeper.data.model.CurrencyOption,
    hideSensitiveValues: Boolean,
    onEdit: () -> Unit,
    onPay: () -> Unit
) {
    val totalAmount = debt.totalAmount.takeIf { it > 0.0 } ?: debt.remainingAmount
    val progress = if (totalAmount > 0.0) {
        (1.0 - (debt.remainingAmount / totalAmount)).toFloat().coerceIn(0f, 1f)
    } else {
        0f
    }
    Card(
        shape = MaterialTheme.shapes.large,
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = debt.name,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Clip
                    )
                    Text(
                        text = debt.dueDate?.let { "${DateUtils.relativeDueLabel(it)} (${DateUtils.formatDate(it)})" }
                            ?: "No due date",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                SensitiveValueText(
                    text = CurrencyFormatter.format(debt.remainingAmount, currency),
                    hidden = hideSensitiveValues,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.error
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DebtPill("Min ${CurrencyFormatter.format(debt.minimumPayment, currency)}")
                if (debt.interestRate > 0.0) {
                    DebtPill("${debt.interestRate}% APR")
                }
            }
            androidx.compose.material3.LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth()
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SensitiveValueText(
                    text = "Paid ${CurrencyFormatter.format(totalAmount - debt.remainingAmount, currency)} of ${CurrencyFormatter.format(totalAmount, currency)}",
                    hidden = hideSensitiveValues,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = onEdit) {
                        Icon(imageVector = Icons.Outlined.Edit, contentDescription = null)
                        Text("Edit")
                    }
                    Button(onClick = onPay) {
                        Icon(imageVector = Icons.Outlined.Payments, contentDescription = null)
                        Text("Pay")
                    }
                }
            }
        }
    }
}

@Composable
private fun DebtPill(label: String) {
    Box(
        modifier = Modifier
            .background(
                color = MaterialTheme.colorScheme.secondaryContainer,
                shape = CircleShape
            )
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSecondaryContainer
        )
    }
}

@Composable
private fun DebtEditorDialog(
    existingDebt: DebtEntity?,
    onDismiss: () -> Unit,
    onDelete: (DebtEntity) -> Unit,
    onSave: (DebtEntity?, String, String, String, LocalDate?, String, String) -> Unit
) {
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    var name by rememberSaveable(existingDebt?.id) { mutableStateOf(existingDebt?.name.orEmpty()) }
    var balance by rememberSaveable(existingDebt?.id) {
        mutableStateOf(existingDebt?.remainingAmount?.takeIf { it > 0.0 }?.toString().orEmpty())
    }
    var minimumPayment by rememberSaveable(existingDebt?.id) {
        mutableStateOf(existingDebt?.minimumPayment?.takeIf { it > 0.0 }?.toString().orEmpty())
    }
    var interestRate by rememberSaveable(existingDebt?.id) {
        mutableStateOf(existingDebt?.interestRate?.takeIf { it > 0.0 }?.toString().orEmpty())
    }
    var note by rememberSaveable(existingDebt?.id) { mutableStateOf(existingDebt?.note.orEmpty()) }
    var dueDate by rememberSaveable(existingDebt?.id) { mutableStateOf(existingDebt?.dueDate ?: LocalDate.now()) }

    fun submit() {
        keyboardController?.hide()
        focusManager.clearFocus(force = true)
        onSave(existingDebt, name, balance, minimumPayment, dueDate, interestRate, note)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existingDebt == null) "Add Debt" else "Edit Debt") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Name") },
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words,
                        imeAction = ImeAction.Next
                    )
                )
                OutlinedTextField(
                    value = balance,
                    onValueChange = { balance = it.filter { character -> character.isDigit() || character == '.' } },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Total balance") },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal,
                        imeAction = ImeAction.Next
                    )
                )
                OutlinedTextField(
                    value = minimumPayment,
                    onValueChange = { minimumPayment = it.filter { character -> character.isDigit() || character == '.' } },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Minimum payment") },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal,
                        imeAction = ImeAction.Next
                    )
                )
                DatePickerField(
                    label = "Due date",
                    selectedDate = dueDate,
                    modifier = Modifier.fillMaxWidth(),
                    onDateSelected = { dueDate = it }
                )
                OutlinedTextField(
                    value = interestRate,
                    onValueChange = { interestRate = it.filter { character -> character.isDigit() || character == '.' } },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Interest rate (optional)") },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal,
                        imeAction = ImeAction.Next
                    )
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
            Button(onClick = { submit() }) {
                Text("Save")
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                existingDebt?.let {
                    TextButton(onClick = { onDelete(it) }) {
                        Text("Delete")
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        }
    )
}

@Composable
private fun DebtPaymentDialog(
    debt: DebtEntity,
    currencyLabel: String,
    accounts: List<AccountEntity>,
    onDismiss: () -> Unit,
    onConfirm: (String, LocalDate, String?, Boolean, String) -> Unit
) {
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    var amount by rememberSaveable(debt.id) {
        mutableStateOf(
            debt.minimumPayment
                .takeIf { it > 0.0 }
                ?.coerceAtMost(debt.remainingAmount)
                ?.toString()
                ?: debt.remainingAmount.toString()
        )
    }
    var paymentDate by rememberSaveable(debt.id) { mutableStateOf(LocalDate.now()) }
    var createExpenseTransaction by rememberSaveable(debt.id) { mutableStateOf(true) }
    var note by rememberSaveable(debt.id) { mutableStateOf("") }
    var selectedAccountRemoteId by rememberSaveable(debt.id) { mutableStateOf(accounts.firstOrNull()?.remoteId) }
    var accountsExpanded by rememberSaveable(debt.id) { mutableStateOf(false) }

    fun submit() {
        keyboardController?.hide()
        focusManager.clearFocus(force = true)
        onConfirm(amount, paymentDate, selectedAccountRemoteId, createExpenseTransaction, note)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Pay Debt") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Record a payment against ${debt.name}. You can also create a matching expense transaction.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it.filter { character -> character.isDigit() || character == '.' } },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Payment amount ($currencyLabel)") },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal,
                        imeAction = ImeAction.Next
                    )
                )
                DatePickerField(
                    label = "Payment date",
                    selectedDate = paymentDate,
                    modifier = Modifier.fillMaxWidth(),
                    onDateSelected = { paymentDate = it }
                )
                if (accounts.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Pay from account",
                            style = MaterialTheme.typography.labelLarge
                        )
                        Button(
                            onClick = { accountsExpanded = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            val selectedAccount = accounts.firstOrNull { it.remoteId == selectedAccountRemoteId }
                            Text(
                                selectedAccount?.let {
                                    "${it.name} (${formatAccountBalance(it)})"
                                } ?: "Choose account"
                            )
                        }
                        DropdownMenu(
                            expanded = accountsExpanded,
                            onDismissRequest = { accountsExpanded = false }
                        ) {
                            accounts.forEach { account ->
                                DropdownMenuItem(
                                    text = {
                                        Text("${account.name} (${formatAccountBalance(account)})")
                                    },
                                    onClick = {
                                        selectedAccountRemoteId = account.remoteId
                                        accountsExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Checkbox(
                        checked = createExpenseTransaction,
                        onCheckedChange = { createExpenseTransaction = it }
                    )
                    Text(
                        text = "Also create an expense transaction",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
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
            Button(onClick = { submit() }) {
                Text("Save payment")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

private fun formatAccountBalance(account: AccountEntity): String {
    return "${account.currencyCode} ${"%,.0f".format(account.currentBalance)}"
}
