@file:OptIn(ExperimentalLayoutApi::class)

package com.talent.monthlybudgetkeeper.ui.screens.networth

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
import androidx.compose.material.icons.outlined.ArrowOutward
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Paid
import androidx.compose.material.icons.outlined.Savings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.talent.monthlybudgetkeeper.data.local.entity.AccountEntity
import com.talent.monthlybudgetkeeper.data.model.AccountType
import com.talent.monthlybudgetkeeper.ui.components.ActionButtonStyle
import com.talent.monthlybudgetkeeper.ui.components.EmptyStateCard
import com.talent.monthlybudgetkeeper.ui.components.QuickActionButton
import com.talent.monthlybudgetkeeper.ui.components.SensitiveValueText
import com.talent.monthlybudgetkeeper.utils.CurrencyFormatter
import com.talent.monthlybudgetkeeper.viewmodel.NetWorthEntryKind
import com.talent.monthlybudgetkeeper.viewmodel.NetWorthTrendPoint
import com.talent.monthlybudgetkeeper.viewmodel.NetWorthViewModel
import kotlin.math.abs

@Composable
fun NetWorthScreen(
    contentPadding: PaddingValues,
    onOpenDebts: () -> Unit,
    viewModel: NetWorthViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var editor by remember { mutableStateOf<Pair<NetWorthEntryKind, AccountEntity?>?>(null) }

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
            NetWorthHeroCard(
                netWorth = CurrencyFormatter.format(state.netWorth, state.currency),
                assetTotal = CurrencyFormatter.format(state.assetTotal, state.currency),
                liabilityTotal = CurrencyFormatter.format(state.liabilityTotal, state.currency),
                hideSensitiveValues = state.privacyModeEnabled
            )
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                QuickActionButton(
                    label = "Add Asset",
                    icon = Icons.Outlined.Add,
                    modifier = Modifier.fillMaxWidth(),
                    style = ActionButtonStyle.PRIMARY,
                    onClick = { editor = NetWorthEntryKind.ASSET to null }
                )
                QuickActionButton(
                    label = "Add Liability",
                    icon = Icons.Outlined.CreditCard,
                    modifier = Modifier.fillMaxWidth(),
                    style = ActionButtonStyle.SECONDARY,
                    onClick = { editor = NetWorthEntryKind.LIABILITY to null }
                )
            }
        }

        if (state.trend.size > 1) {
            item {
                TrendCard(
                    points = state.trend,
                    hideSensitiveValues = state.privacyModeEnabled,
                    currency = state.currency
                )
            }
        }

        item {
            SectionHeader(
                title = "Assets",
                subtitle = "Cash, bank balances, savings, and anything else you own."
            )
        }
        if (state.assetAccounts.isEmpty()) {
            item {
                EmptyStateCard(
                    title = "No assets added yet",
                    message = "Add accounts, savings, or custom assets here so net worth has something real to grow from.",
                    icon = Icons.Outlined.Savings
                )
            }
        } else {
            items(state.assetAccounts, key = { it.id }) { account ->
                TrackedAccountCard(
                    account = account,
                    amount = CurrencyFormatter.format(account.currentBalance, state.currency),
                    hideSensitiveValues = state.privacyModeEnabled,
                    onEdit = { editor = NetWorthEntryKind.ASSET to account }
                )
            }
        }

        item {
            SectionHeader(
                title = "Liabilities",
                subtitle = "Credit cards, custom liabilities, and tracked debts all feed this side."
            )
        }
        if (state.liabilityAccounts.isEmpty() && state.debts.isEmpty()) {
            item {
                EmptyStateCard(
                    title = "No liabilities added yet",
                    message = "Add cards, loans, or other liabilities to see a true net worth number.",
                    icon = Icons.Outlined.Paid
                )
            }
        } else {
            items(state.liabilityAccounts, key = { it.id }) { account ->
                TrackedAccountCard(
                    account = account,
                    amount = CurrencyFormatter.format(abs(account.currentBalance), state.currency),
                    hideSensitiveValues = state.privacyModeEnabled,
                    onEdit = { editor = NetWorthEntryKind.LIABILITY to account }
                )
            }
            item {
                DebtSummaryCard(
                    debtCount = state.debts.size,
                    totalDebt = CurrencyFormatter.format(
                        state.debts.sumOf { it.remainingAmount.coerceAtLeast(0.0) },
                        state.currency
                    ),
                    hideSensitiveValues = state.privacyModeEnabled,
                    onOpenDebts = onOpenDebts
                )
            }
        }
    }

    editor?.let { (kind, account) ->
        NetWorthEditorDialog(
            kind = kind,
            existingAccount = account,
            onDismiss = { editor = null },
            onDelete = { existingAccount ->
                viewModel.deleteTrackedAccount(existingAccount)
                editor = null
            },
            onSave = { existingAccount, name, balance, selectedKind, accountType, institution, includeInNetWorth ->
                viewModel.saveTrackedAccount(
                    existingAccount = existingAccount,
                    name = name,
                    balance = balance,
                    kind = selectedKind,
                    accountType = accountType,
                    institution = institution,
                    includeInNetWorth = includeInNetWorth
                )
                editor = null
            }
        )
    }
}

@Composable
private fun NetWorthHeroCard(
    netWorth: String,
    assetTotal: String,
    liabilityTotal: String,
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
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.38f),
                    shape = MaterialTheme.shapes.extraLarge
                )
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(text = "Net worth", style = MaterialTheme.typography.displaySmall)
            Text(
                text = "Assets minus liabilities, updated from the balances you actually track.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            SensitiveValueText(
                text = netWorth,
                hidden = hideSensitiveValues,
                style = MaterialTheme.typography.headlineMedium
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                HeroMetric(
                    modifier = Modifier.weight(1f),
                    label = "Assets",
                    value = assetTotal,
                    hideSensitiveValues = hideSensitiveValues
                )
                HeroMetric(
                    modifier = Modifier.weight(1f),
                    label = "Liabilities",
                    value = liabilityTotal,
                    hideSensitiveValues = hideSensitiveValues
                )
            }
        }
    }
}

@Composable
private fun HeroMetric(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    hideSensitiveValues: Boolean
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
        SensitiveValueText(
            text = value,
            hidden = hideSensitiveValues,
            style = MaterialTheme.typography.titleMedium
        )
    }
}

@Composable
private fun TrendCard(
    points: List<NetWorthTrendPoint>,
    hideSensitiveValues: Boolean,
    currency: com.talent.monthlybudgetkeeper.data.model.CurrencyOption
) {
    Card(shape = MaterialTheme.shapes.large) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SectionHeader(
                title = "Trend",
                subtitle = "Recent net worth snapshots, shown when enough history exists."
            )
            points.forEach { point ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = point.dateLabel,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    SensitiveValueText(
                        text = CurrencyFormatter.format(point.netWorth, currency),
                        hidden = hideSensitiveValues,
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
private fun TrackedAccountCard(
    account: AccountEntity,
    amount: String,
    hideSensitiveValues: Boolean,
    onEdit: () -> Unit
) {
    Card(
        shape = MaterialTheme.shapes.large,
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = account.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Clip
                )
                Text(
                    text = buildString {
                        append(account.accountType.label)
                        if (account.institution.isNotBlank()) {
                            append(" - ")
                            append(account.institution)
                        }
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SensitiveValueText(
                    text = amount,
                    hidden = hideSensitiveValues,
                    style = MaterialTheme.typography.titleMedium
                )
                TextButton(onClick = onEdit) {
                    Icon(imageVector = Icons.Outlined.Edit, contentDescription = null)
                    Text("Edit")
                }
            }
        }
    }
}

@Composable
private fun DebtSummaryCard(
    debtCount: Int,
    totalDebt: String,
    hideSensitiveValues: Boolean,
    onOpenDebts: () -> Unit
) {
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
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(text = "Tracked debts", style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = "$debtCount debt item(s) are managed from the debt module.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                SensitiveValueText(
                    text = totalDebt,
                    hidden = hideSensitiveValues,
                    style = MaterialTheme.typography.titleMedium
                )
            }
            Button(onClick = onOpenDebts) {
                Icon(imageVector = Icons.Outlined.ArrowOutward, contentDescription = null)
                Text("Manage debts")
            }
        }
    }
}

@Composable
private fun NetWorthEditorDialog(
    kind: NetWorthEntryKind,
    existingAccount: AccountEntity?,
    onDismiss: () -> Unit,
    onDelete: (AccountEntity) -> Unit,
    onSave: (AccountEntity?, String, String, NetWorthEntryKind, AccountType, String, Boolean) -> Unit
) {
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val existingKind = existingAccount?.let {
        if (it.accountType == AccountType.CREDIT || it.currentBalance < 0.0) {
            NetWorthEntryKind.LIABILITY
        } else {
            NetWorthEntryKind.ASSET
        }
    } ?: kind
    var selectedKind by rememberSaveable(existingAccount?.id, kind.name) { mutableStateOf(existingKind) }
    var name by rememberSaveable(existingAccount?.id, kind.name) { mutableStateOf(existingAccount?.name.orEmpty()) }
    var balance by rememberSaveable(existingAccount?.id, kind.name) {
        mutableStateOf(existingAccount?.currentBalance?.let { abs(it).toString() }.orEmpty())
    }
    var institution by rememberSaveable(existingAccount?.id, kind.name) {
        mutableStateOf(existingAccount?.institution.orEmpty())
    }
    var includeInNetWorth by rememberSaveable(existingAccount?.id, kind.name) {
        mutableStateOf(existingAccount?.includeInNetWorth ?: true)
    }
    var accountType by rememberSaveable(existingAccount?.id, kind.name) {
        mutableStateOf(existingAccount?.accountType ?: defaultAccountTypeFor(kind))
    }

    fun submit() {
        keyboardController?.hide()
        focusManager.clearFocus(force = true)
        onSave(existingAccount, name, balance, selectedKind, accountType, institution, includeInNetWorth)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existingAccount == null) "Add ${kind.label}" else "Edit ${selectedKind.label}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    NetWorthEntryKind.entries.forEach { option ->
                        FilterChip(
                            selected = selectedKind == option,
                            onClick = {
                                selectedKind = option
                                accountType = defaultAccountTypeFor(option)
                            },
                            label = { Text(option.label) }
                        )
                    }
                }
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
                    label = { Text(if (selectedKind == NetWorthEntryKind.ASSET) "Balance" else "Liability balance") },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal,
                        imeAction = ImeAction.Next
                    )
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    accountTypeOptionsFor(selectedKind).forEach { option ->
                        FilterChip(
                            selected = accountType == option,
                            onClick = { accountType = option },
                            label = {
                                Text(
                                    text = option.label,
                                    maxLines = 2,
                                    overflow = TextOverflow.Clip
                                )
                            }
                        )
                    }
                }
                OutlinedTextField(
                    value = institution,
                    onValueChange = { institution = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Institution or note") },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Include in net worth", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            text = "Turn this off if you want to keep the account without affecting net worth.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = includeInNetWorth,
                        onCheckedChange = { includeInNetWorth = it }
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = { submit() }) {
                Text("Save")
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                existingAccount?.takeIf {
                    it.institution != com.talent.monthlybudgetkeeper.data.repository.FinanceRepository.SYSTEM_BALANCE_ACCOUNT_MARKER
                }?.let {
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

private fun accountTypeOptionsFor(kind: NetWorthEntryKind): List<AccountType> {
    return if (kind == NetWorthEntryKind.LIABILITY) {
        listOf(AccountType.CREDIT, AccountType.OTHER)
    } else {
        listOf(
            AccountType.CASH,
            AccountType.BANK,
            AccountType.SAVINGS,
            AccountType.DIGITAL_WALLET,
            AccountType.OTHER
        )
    }
}

private fun defaultAccountTypeFor(kind: NetWorthEntryKind): AccountType {
    return if (kind == NetWorthEntryKind.LIABILITY) AccountType.CREDIT else AccountType.BANK
}
