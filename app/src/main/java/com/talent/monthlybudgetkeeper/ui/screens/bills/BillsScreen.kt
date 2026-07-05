@file:OptIn(ExperimentalLayoutApi::class)

package com.talent.monthlybudgetkeeper.ui.screens.bills

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Autorenew
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Subscriptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.talent.monthlybudgetkeeper.data.local.entity.RecurringItemEntity
import com.talent.monthlybudgetkeeper.data.local.entity.SubscriptionBillEntity
import com.talent.monthlybudgetkeeper.data.model.BillingCycle
import com.talent.monthlybudgetkeeper.data.model.CurrencyOption
import com.talent.monthlybudgetkeeper.data.model.RecurrenceInterval
import com.talent.monthlybudgetkeeper.ui.components.ActionButtonStyle
import com.talent.monthlybudgetkeeper.ui.components.DatePickerField
import com.talent.monthlybudgetkeeper.ui.components.EmptyStateCard
import com.talent.monthlybudgetkeeper.ui.components.QuickActionButton
import com.talent.monthlybudgetkeeper.ui.components.SensitiveValueText
import com.talent.monthlybudgetkeeper.utils.CurrencyFormatter
import com.talent.monthlybudgetkeeper.utils.DateUtils
import com.talent.monthlybudgetkeeper.viewmodel.BillsViewModel
import java.time.LocalDate

private enum class BillsTab(val label: String) {
    BILLS("Bills"),
    SUBSCRIPTIONS("Subscriptions"),
    RECURRING("Recurring"),
    UPCOMING("Upcoming")
}

@Composable
fun BillsScreen(
    contentPadding: PaddingValues,
    viewModel: BillsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    var selectedTab by rememberSaveable { mutableStateOf(BillsTab.BILLS) }
    var composer by rememberSaveable { mutableStateOf<BillsComposer?>(null) }
    val today = LocalDate.now()

    val upcomingBills = (state.bills + state.subscriptions)
        .filter { it.nextChargeDate >= today }
        .sortedBy { it.nextChargeDate }
    val overdueBills = (state.bills + state.subscriptions)
        .filter { it.nextChargeDate < today }
        .sortedBy { it.nextChargeDate }
    val upcomingRecurring = state.recurringItems
        .filter { it.nextDueDate >= today }
        .sortedBy { it.nextDueDate }
    val overdueRecurring = state.recurringItems
        .filter { it.nextDueDate < today }
        .sortedBy { it.nextDueDate }

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
            BillsHeroCard(
                totalItems = state.bills.size + state.subscriptions.size + state.recurringItems.size,
                overdueCount = overdueBills.size + overdueRecurring.size,
                upcomingCount = upcomingBills.size + upcomingRecurring.size,
                currency = state.currency,
                upcomingAmount = upcomingBills.sumOf { it.amount } + upcomingRecurring.sumOf { it.amount },
                hideSensitiveValues = state.privacyModeEnabled
            )
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                QuickActionButton(
                    label = "Add Bill",
                    icon = Icons.AutoMirrored.Outlined.ReceiptLong,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { composer = BillsComposer.BILL }
                )
                QuickActionButton(
                    label = "Add Subscription",
                    icon = Icons.Outlined.Subscriptions,
                    modifier = Modifier.fillMaxWidth(),
                    style = ActionButtonStyle.SECONDARY,
                    onClick = { composer = BillsComposer.SUBSCRIPTION }
                )
                QuickActionButton(
                    label = "Add Recurring Item",
                    icon = Icons.Outlined.Autorenew,
                    modifier = Modifier.fillMaxWidth(),
                    style = ActionButtonStyle.SECONDARY,
                    onClick = { composer = BillsComposer.RECURRING }
                )
            }
        }
        item {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                BillsTab.entries.forEach { tab ->
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
        when (selectedTab) {
            BillsTab.BILLS -> {
                if (state.bills.isEmpty()) {
                    item {
                        EmptyStateCard(
                            title = "No bills scheduled yet",
                            message = "Bills are planned obligations like rent, school fees, or utilities. They only become expenses after you mark them paid.",
                            icon = Icons.AutoMirrored.Outlined.ReceiptLong
                        )
                    }
                } else {
                    items(state.bills) { item ->
                        BillRecordCard(
                            item = item,
                            currency = state.currency,
                            hideSensitiveValues = state.privacyModeEnabled,
                            helper = "Marking this paid will generate an expense transaction.",
                            onPrimaryAction = { viewModel.markBillPaid(item) },
                            primaryLabel = "Mark paid",
                            statusLabel = statusLabel(item.nextChargeDate),
                            isAutoPay = item.isAutoPay
                        )
                    }
                }
            }

            BillsTab.SUBSCRIPTIONS -> {
                if (state.subscriptions.isEmpty()) {
                    item {
                        EmptyStateCard(
                            title = "No subscriptions planned yet",
                            message = "Subscriptions are repeating merchant charges like streaming, software, or memberships and stay separate until paid.",
                            icon = Icons.Outlined.Subscriptions
                        )
                    }
                } else {
                    items(state.subscriptions) { item ->
                        BillRecordCard(
                            item = item,
                            currency = state.currency,
                            hideSensitiveValues = state.privacyModeEnabled,
                            helper = "Subscriptions stay here until the payment is confirmed.",
                            onPrimaryAction = { viewModel.markBillPaid(item) },
                            primaryLabel = "Mark paid",
                            statusLabel = statusLabel(item.nextChargeDate),
                            isAutoPay = true
                        )
                    }
                }
            }

            BillsTab.RECURRING -> {
                if (state.recurringItems.isEmpty()) {
                    item {
                        EmptyStateCard(
                            title = "No recurring items scheduled yet",
                            message = "Recurring items cover repeating income or expenses and stay upcoming until you generate a real transaction.",
                            icon = Icons.Outlined.Autorenew
                        )
                    }
                } else {
                    items(state.recurringItems) { item ->
                        RecurringRecordCard(
                            item = item,
                            currency = state.currency,
                            hideSensitiveValues = state.privacyModeEnabled,
                            onGenerate = { viewModel.generateRecurring(item) }
                        )
                    }
                }
            }

            BillsTab.UPCOMING -> {
                val hasTimelineItems = overdueBills.isNotEmpty() ||
                    overdueRecurring.isNotEmpty() ||
                    upcomingBills.isNotEmpty() ||
                    upcomingRecurring.isNotEmpty()
                if (!hasTimelineItems) {
                    item {
                        EmptyStateCard(
                            title = "Nothing upcoming right now",
                            message = "Upcoming combines due-soon and overdue bills, subscriptions, and recurring items in one cleaner timeline.",
                            icon = Icons.Outlined.Schedule
                        )
                    }
                } else {
                    if (overdueBills.isNotEmpty() || overdueRecurring.isNotEmpty()) {
                        item {
                            TimelineHeader(
                                title = "Overdue",
                                subtitle = "These planned items have passed their due date and still need attention."
                            )
                        }
                    }
                    items(overdueBills) { item ->
                        BillRecordCard(
                            item = item,
                            currency = state.currency,
                            hideSensitiveValues = state.privacyModeEnabled,
                            helper = "This is overdue and still has not created an expense transaction.",
                            onPrimaryAction = { viewModel.markBillPaid(item) },
                            primaryLabel = "Mark paid",
                            statusLabel = statusLabel(item.nextChargeDate),
                            isAutoPay = item.isAutoPay
                        )
                    }
                    items(overdueRecurring) { item ->
                        RecurringRecordCard(
                            item = item,
                            currency = state.currency,
                            hideSensitiveValues = state.privacyModeEnabled,
                            onGenerate = { viewModel.generateRecurring(item) }
                        )
                    }
                    if (upcomingBills.isNotEmpty() || upcomingRecurring.isNotEmpty()) {
                        item {
                            TimelineHeader(
                                title = "Upcoming",
                                subtitle = "These are scheduled next and will stay here until they are paid or generated."
                            )
                        }
                    }
                    items(upcomingBills) { item ->
                        BillRecordCard(
                            item = item,
                            currency = state.currency,
                            hideSensitiveValues = state.privacyModeEnabled,
                            helper = "Still upcoming. It will not affect transaction history until paid.",
                            onPrimaryAction = { viewModel.markBillPaid(item) },
                            primaryLabel = "Mark paid",
                            statusLabel = statusLabel(item.nextChargeDate),
                            isAutoPay = item.isAutoPay
                        )
                    }
                    items(upcomingRecurring) { item ->
                        RecurringRecordCard(
                            item = item,
                            currency = state.currency,
                            hideSensitiveValues = state.privacyModeEnabled,
                            onGenerate = { viewModel.generateRecurring(item) }
                        )
                    }
                }
            }
        }
    }

    when (composer) {
        BillsComposer.BILL -> BillComposerDialog(
            title = "Add Bill",
            helper = "Bills stay planned here until you mark them paid. They do not count as expenses yet.",
            onDismiss = {
                keyboardController?.hide()
                focusManager.clearFocus(force = true)
                composer = null
            },
            onSave = { name, amount, date, cycle, note ->
                keyboardController?.hide()
                focusManager.clearFocus(force = true)
                viewModel.saveBill(name, amount, date, cycle, note)
                composer = null
            }
        )

        BillsComposer.SUBSCRIPTION -> BillComposerDialog(
            title = "Add Subscription",
            helper = "Subscriptions are separate from one-off bills and normal transaction entry.",
            onDismiss = {
                keyboardController?.hide()
                focusManager.clearFocus(force = true)
                composer = null
            },
            onSave = { name, amount, date, cycle, note ->
                keyboardController?.hide()
                focusManager.clearFocus(force = true)
                viewModel.saveSubscription(name, amount, date, cycle, note)
                composer = null
            }
        )

        BillsComposer.RECURRING -> RecurringComposerDialog(
            onDismiss = {
                keyboardController?.hide()
                focusManager.clearFocus(force = true)
                composer = null
            },
            onSave = { title, amount, date, interval, note ->
                keyboardController?.hide()
                focusManager.clearFocus(force = true)
                viewModel.saveRecurring(title, amount, date, interval, note)
                composer = null
            }
        )

        null -> Unit
    }
}

private enum class BillsComposer {
    BILL,
    SUBSCRIPTION,
    RECURRING
}

@Composable
private fun BillsHeroCard(
    totalItems: Int,
    overdueCount: Int,
    upcomingCount: Int,
    currency: CurrencyOption,
    upcomingAmount: Double,
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
            Text(text = "Bills", style = MaterialTheme.typography.displaySmall)
            Text(
                text = "Keep planned bills, subscriptions, and recurring items organized before they become real spending.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                BillHeroMetric(
                    modifier = Modifier.weight(1f),
                    label = "Tracked",
                    value = totalItems.toString()
                )
                BillHeroMetric(
                    modifier = Modifier.weight(1f),
                    label = "Overdue",
                    value = overdueCount.toString()
                )
                BillHeroMetric(
                    modifier = Modifier.weight(1f),
                    label = "Upcoming",
                    value = upcomingCount.toString()
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Upcoming amount",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                SensitiveValueText(
                    text = CurrencyFormatter.format(upcomingAmount, currency),
                    hidden = hideSensitiveValues,
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
    }
}

@Composable
private fun BillHeroMetric(
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
private fun BillRecordCard(
    item: SubscriptionBillEntity,
    currency: CurrencyOption,
    hideSensitiveValues: Boolean,
    helper: String,
    onPrimaryAction: () -> Unit,
    primaryLabel: String,
    statusLabel: String,
    isAutoPay: Boolean
) {
    Card(
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = MaterialTheme.shapes.large
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
                        text = item.name,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Clip
                    )
                    Text(
                        text = "${item.category.displayName} - ${item.billingCycle.label}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Clip
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        BillStatusPill(statusLabel)
                        if (isAutoPay) {
                            BillStatusPill("Auto-pay")
                        }
                    }
                }
                SensitiveValueText(
                    text = CurrencyFormatter.format(item.amount, currency),
                    hidden = hideSensitiveValues,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Text(
                text = "${DateUtils.relativeDueLabel(item.nextChargeDate)} (${DateUtils.formatDate(item.nextChargeDate)})",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = helper,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(onClick = onPrimaryAction) {
                Text(primaryLabel)
            }
        }
    }
}

@Composable
private fun RecurringRecordCard(
    item: RecurringItemEntity,
    currency: CurrencyOption,
    hideSensitiveValues: Boolean,
    onGenerate: () -> Unit
) {
    val label = DateUtils.relativeDueLabel(item.nextDueDate)
    Card(
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = MaterialTheme.shapes.large
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
                        text = item.title,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Clip
                    )
                    Text(
                        text = "${item.type.name.lowercase().replaceFirstChar(Char::uppercase)} - ${item.interval.label}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        BillStatusPill(label)
                    }
                }
                SensitiveValueText(
                    text = CurrencyFormatter.format(item.amount, currency),
                    hidden = hideSensitiveValues,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (item.type == com.talent.monthlybudgetkeeper.data.model.TransactionType.INCOME) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.error
                    }
                )
            }
            Text(
                text = "${DateUtils.relativeDueLabel(item.nextDueDate)} (${DateUtils.formatDate(item.nextDueDate)})",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "Recurring items stay upcoming until you generate a real transaction from them.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(onClick = onGenerate) {
                Text("Generate transaction")
            }
        }
    }
}

@Composable
private fun TimelineHeader(
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
private fun BillStatusPill(label: String) {
    val background = when {
        label.startsWith("Overdue") -> MaterialTheme.colorScheme.errorContainer
        label == "Due today" -> MaterialTheme.colorScheme.errorContainer
        label == "Due tomorrow" || label.startsWith("Due in") -> MaterialTheme.colorScheme.tertiaryContainer
        label == "Auto-pay" -> MaterialTheme.colorScheme.secondaryContainer
        label == "Paid" -> MaterialTheme.colorScheme.primaryContainer
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
    val contentColor = when {
        label.startsWith("Overdue") -> MaterialTheme.colorScheme.onErrorContainer
        label == "Due today" -> MaterialTheme.colorScheme.onErrorContainer
        label == "Due tomorrow" || label.startsWith("Due in") -> MaterialTheme.colorScheme.onTertiaryContainer
        label == "Auto-pay" -> MaterialTheme.colorScheme.onSecondaryContainer
        label == "Paid" -> MaterialTheme.colorScheme.onPrimaryContainer
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Box(
        modifier = Modifier
            .background(background, CircleShape)
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = contentColor
        )
    }
}

private fun statusLabel(date: LocalDate): String {
    return DateUtils.relativeDueLabel(date)
}

@Composable
private fun BillComposerDialog(
    title: String,
    helper: String,
    onDismiss: () -> Unit,
    onSave: (String, String, LocalDate, BillingCycle, String) -> Unit
) {
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    var name by rememberSaveable { mutableStateOf("") }
    var amount by rememberSaveable { mutableStateOf("") }
    var note by rememberSaveable { mutableStateOf("") }
    var dueDate by rememberSaveable { mutableStateOf(LocalDate.now()) }
    var cycle by rememberSaveable { mutableStateOf(BillingCycle.MONTHLY) }

    fun submit() {
        keyboardController?.hide()
        focusManager.clearFocus(force = true)
        onSave(name, amount, dueDate, cycle, note)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = helper,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
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
                    label = "Due date",
                    selectedDate = dueDate,
                    modifier = Modifier.fillMaxWidth(),
                    onDateSelected = { dueDate = it }
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    BillingCycle.entries.forEach { option ->
                        FilterChip(
                            selected = cycle == option,
                            onClick = { cycle = option },
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
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun RecurringComposerDialog(
    onDismiss: () -> Unit,
    onSave: (String, String, LocalDate, RecurrenceInterval, String) -> Unit
) {
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    var title by rememberSaveable { mutableStateOf("") }
    var amount by rememberSaveable { mutableStateOf("") }
    var note by rememberSaveable { mutableStateOf("") }
    var dueDate by rememberSaveable { mutableStateOf(LocalDate.now()) }
    var interval by rememberSaveable { mutableStateOf(RecurrenceInterval.MONTHLY) }

    fun submit() {
        keyboardController?.hide()
        focusManager.clearFocus(force = true)
        onSave(title, amount, dueDate, interval, note)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Recurring Item") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Recurring items stay upcoming until you choose to generate a real transaction from them.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Title") },
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        imeAction = ImeAction.Next
                    )
                )
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
                    label = "Next due date",
                    selectedDate = dueDate,
                    modifier = Modifier.fillMaxWidth(),
                    onDateSelected = { dueDate = it }
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    RecurrenceInterval.entries.forEach { option ->
                        FilterChip(
                            selected = interval == option,
                            onClick = { interval = option },
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
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
