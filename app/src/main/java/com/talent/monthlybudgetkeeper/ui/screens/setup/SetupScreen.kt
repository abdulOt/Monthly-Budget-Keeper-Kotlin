@file:OptIn(ExperimentalLayoutApi::class)

package com.talent.monthlybudgetkeeper.ui.screens.setup

import android.Manifest
import android.app.Activity
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.automirrored.outlined.ArrowForwardIos
import androidx.compose.material.icons.outlined.Cached
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.TrackChanges
import androidx.compose.material.icons.outlined.Wallet
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.fragment.app.FragmentActivity
import com.talent.monthlybudgetkeeper.AppState
import com.talent.monthlybudgetkeeper.data.model.BudgetCycleType
import com.talent.monthlybudgetkeeper.data.notifications.NotificationPermissionManager
import com.talent.monthlybudgetkeeper.data.notifications.NotificationPermissionStatus
import com.talent.monthlybudgetkeeper.data.model.CurrencyOption
import com.talent.monthlybudgetkeeper.data.model.RegionOption
import com.talent.monthlybudgetkeeper.ui.components.DatePickerField
import com.talent.monthlybudgetkeeper.ui.components.SearchableSelectionDialog
import com.talent.monthlybudgetkeeper.viewmodel.SetupViewModel
import java.time.LocalDate
import kotlinx.coroutines.flow.collectLatest

private val FinancialGoals = listOf(
    "Save money",
    "Control expenses",
    "Pay bills on time",
    "Reduce debt",
    "Track spending"
)

@Composable
fun SetupScreen(
    contentPadding: PaddingValues,
    onCompleted: () -> Unit,
    appState: AppState,
    viewModel: SetupViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val activity = context as? FragmentActivity
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val preferences = uiState.preferences
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    var region by remember(preferences.region, preferences.setupCompleted) {
        mutableStateOf(preferences.region.takeIf { preferences.setupCompleted })
    }
    var currency by remember(preferences.currency, preferences.setupCompleted) {
        mutableStateOf(preferences.currency.takeIf { preferences.setupCompleted })
    }
    var showCountrySelector by remember { mutableStateOf(false) }
    var showCurrencySelector by remember { mutableStateOf(false) }
    var showNotificationPrimerDialog by remember { mutableStateOf(false) }
    var monthlyIncome by remember(preferences.monthlyIncome, preferences.setupCompleted) {
        mutableStateOf(
            if (preferences.setupCompleted && preferences.monthlyIncome > 0.0) {
                preferences.monthlyIncome.toInt().toString()
            } else {
                ""
            }
        )
    }
    var monthlyBudgetTarget by remember(preferences.monthlyBudgetTarget, preferences.setupCompleted) {
        mutableStateOf(
            if (preferences.setupCompleted && preferences.monthlyBudgetTarget > 0.0) {
                preferences.monthlyBudgetTarget.toInt().toString()
            } else {
                ""
            }
        )
    }
    var mainGoal by remember(preferences.mainFinancialGoal, preferences.setupCompleted) {
        mutableStateOf(
            preferences.mainFinancialGoal
                .takeIf { it.isNotBlank() && preferences.setupCompleted }
                ?: "Track spending"
        )
    }
    var cycleType by remember(preferences.cycleType, preferences.setupCompleted) {
        mutableStateOf(preferences.cycleType)
    }
    var nextCycleDate by remember(preferences.nextCycleDate, preferences.setupCompleted) {
        mutableStateOf(
            when {
                preferences.setupCompleted -> preferences.nextCycleDate
                else -> defaultNextCycleDate(cycleType, LocalDate.now())
            }
        )
    }
    var carryForwardRemainingBudget by remember(
        preferences.carryForwardRemainingBudget,
        preferences.setupCompleted
    ) {
        mutableStateOf(if (preferences.setupCompleted) preferences.carryForwardRemainingBudget else false)
    }
    var notificationsEnabled by remember(preferences.notificationsEnabled, preferences.setupCompleted) {
        mutableStateOf(
            if (preferences.setupCompleted) {
                preferences.notificationsEnabled
            } else {
                NotificationPermissionManager.canSendNotifications(context)
            }
        )
    }
    var hideBalances by remember(preferences.privacyModeEnabled, preferences.setupCompleted) {
        mutableStateOf(if (preferences.setupCompleted) preferences.privacyModeEnabled else false)
    }

    val requestNotificationPermission = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        notificationsEnabled = granted
        if (!granted) {
            appState.showMessage(
                "Notifications are enabled in the app, but Android permission is still blocked on this device. You can allow them later in Settings."
            )
        }
    }

    val canProceed = !uiState.isSaving && region != null && currency != null
    val cyclePeriodLabel = cycleType.label.lowercase()

    fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < 33) return
        val status = NotificationPermissionManager.permissionStatus(
            context = context,
            activity = activity as? Activity,
            userId = preferences.userId
        )
        when (status) {
            NotificationPermissionStatus.GRANTED -> Unit
            NotificationPermissionStatus.REQUESTABLE,
            NotificationPermissionStatus.RATIONALE_NEEDED -> {
                showNotificationPrimerDialog = true
            }
            NotificationPermissionStatus.BLOCKED,
            NotificationPermissionStatus.SYSTEM_DISABLED -> {
                notificationsEnabled = false
                appState.showMessage("Android notifications are blocked right now. You can allow them later in Settings.")
            }
        }
    }

    fun submit() {
        keyboardController?.hide()
        focusManager.clearFocus(force = true)
        if (region == null) {
            appState.showMessage("Select your country to continue.")
            return
        }
        if (currency == null) {
            appState.showMessage("Select your currency to continue.")
            return
        }
        viewModel.completeSetup(
            region = region!!,
            currency = currency!!,
            monthlyIncome = monthlyIncome,
            monthlyBudgetTarget = monthlyBudgetTarget,
            mainFinancialGoal = mainGoal,
            cycleType = cycleType,
            nextCycleDate = nextCycleDate,
            carryForwardRemainingBudget = carryForwardRemainingBudget,
            notificationsEnabled = notificationsEnabled,
            privacyModeEnabled = hideBalances
        )
    }

    LaunchedEffect(Unit) {
        viewModel.completed.collectLatest { onCompleted() }
    }

    LaunchedEffect(Unit) {
        viewModel.messages.collectLatest { appState.showMessage(it) }
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
            Card(
                shape = MaterialTheme.shapes.extraLarge,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                            shape = MaterialTheme.shapes.extraLarge
                        )
                        .padding(22.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SetupPill("Step 1")
                        SetupPill("Quick setup")
                    }
                    Text(
                        text = "Set up your workspace",
                        style = MaterialTheme.typography.displaySmall
                    )
                    Text(
                        text = "Choose your country and currency first, then finish the budget setup at your own pace.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        item {
            SetupSectionCard(
                title = "Region and currency",
                subtitle = "Pick the country and money format that should shape your dashboard, reminders, and reports.",
                icon = Icons.Outlined.Public
            ) {
                SelectionTriggerCard(
                    title = "Country / region",
                    value = region?.label ?: "Select your country",
                    supportingText = if (region == null) {
                        "Required to continue. Search from the full country list."
                    } else {
                        "You can change this later in Settings."
                    },
                    onClick = {
                        keyboardController?.hide()
                        focusManager.clearFocus(force = true)
                        showCountrySelector = true
                    }
                )
                SelectionTriggerCard(
                    title = "Currency",
                    value = currency?.selectorLabel ?: "Select your currency",
                    supportingText = if (currency == null) {
                        "Required to continue. Search by code, name, or symbol."
                    } else {
                        "You can change this later in Settings."
                    },
                    onClick = {
                        keyboardController?.hide()
                        focusManager.clearFocus(force = true)
                        showCurrencySelector = true
                    }
                )
            }
        }
        item {
            SetupSectionCard(
                title = "Budget cycle",
                subtitle = "Choose how often your plan resets, when the next cycle should start, and whether any remaining budget rolls forward.",
                icon = Icons.Outlined.Cached
            ) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    BudgetCycleType.entries.forEach { option ->
                        FilterChip(
                            selected = cycleType == option,
                            onClick = {
                                cycleType = option
                                if (!preferences.setupCompleted) {
                                    nextCycleDate = defaultNextCycleDate(option, LocalDate.now())
                                }
                            },
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
                DatePickerField(
                    label = "Next cycle restart date",
                    selectedDate = nextCycleDate,
                    modifier = Modifier.fillMaxWidth(),
                    onDateSelected = { nextCycleDate = it }
                )
                PreferenceToggleRow(
                    title = "Carry remaining budget forward",
                    description = "If this is on, the next cycle keeps any unused budget. If it is off, the new cycle starts fresh.",
                    icon = Icons.Outlined.Cached,
                    checked = carryForwardRemainingBudget,
                    onCheckedChange = { carryForwardRemainingBudget = it }
                )
            }
        }
        item {
            SetupSectionCard(
                title = "${cycleType.label} plan",
                subtitle = "These values personalize your setup without changing the budgeting logic underneath.",
                icon = Icons.Outlined.Wallet
            ) {
                OutlinedTextField(
                    value = monthlyIncome,
                    onValueChange = { monthlyIncome = it.filter { character -> character.isDigit() || character == '.' } },
                    label = { Text("${cycleType.label} income") },
                    supportingText = { Text("Example: salary or take-home income for each $cyclePeriodLabel cycle.") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal,
                        imeAction = ImeAction.Next
                    )
                )
                OutlinedTextField(
                    value = monthlyBudgetTarget,
                    onValueChange = { monthlyBudgetTarget = it.filter { character -> character.isDigit() || character == '.' } },
                    label = { Text("${cycleType.label} budget target") },
                    supportingText = { Text("The spending plan you want each $cyclePeriodLabel cycle to follow.") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(onDone = { submit() })
                )
            }
        }
        item {
            SetupSectionCard(
                title = "Main focus",
                subtitle = "Choose the financial outcome you care about most right now.",
                icon = Icons.Outlined.TrackChanges
            ) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FinancialGoals.forEach { goal ->
                        FilterChip(
                            selected = mainGoal == goal,
                            onClick = { mainGoal = goal },
                            label = {
                                Text(
                                    text = goal,
                                    maxLines = 2,
                                    overflow = TextOverflow.Clip
                                )
                            }
                        )
                    }
                }
            }
        }
        item {
            SetupSectionCard(
                title = "Notifications and privacy",
                subtitle = "Pick how proactive and private this device should feel.",
                icon = Icons.Outlined.Security
            ) {
                PreferenceToggleRow(
                    title = "Enable notifications",
                    description = "Get reminders for bills, budgets, goals, and recurring items.",
                    icon = Icons.Outlined.Notifications,
                    checked = notificationsEnabled,
                    onCheckedChange = { enabled ->
                        if (enabled) {
                            keyboardController?.hide()
                            focusManager.clearFocus(force = true)
                            if (Build.VERSION.SDK_INT < 33 ||
                                NotificationPermissionManager.canSendNotifications(context)
                            ) {
                                notificationsEnabled = true
                            } else {
                                requestNotificationPermissionIfNeeded()
                            }
                        } else {
                            notificationsEnabled = false
                        }
                    }
                )
                PreferenceToggleRow(
                    title = "Hide balances by default",
                    description = "Turn on privacy mode so sensitive amounts stay blurred across the app.",
                    icon = Icons.Outlined.Security,
                    checked = hideBalances,
                    onCheckedChange = { hideBalances = it }
                )
            }
        }
        item {
            Button(
                onClick = { submit() },
                enabled = canProceed,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (uiState.isSaving) "Saving..." else "Proceed to App")
            }
        }
    }

    if (showCountrySelector) {
        SearchableSelectionDialog(
            title = "Select your country",
            searchPlaceholder = "Search country",
            options = RegionOption.entries,
            selectedOption = region,
            emptyMessage = "No country matched your search.",
            onDismiss = { showCountrySelector = false },
            onSelected = { selectedRegion ->
                region = selectedRegion
                showCountrySelector = false
            },
            itemTitle = { it.label },
            itemSubtitle = { it.isoCode },
            itemSearchText = { "${it.label} ${it.isoCode}" }
        )
    }

    if (showCurrencySelector) {
        SearchableSelectionDialog(
            title = "Select your currency",
            searchPlaceholder = "Search currency",
            options = CurrencyOption.entries,
            selectedOption = currency,
            emptyMessage = "No currency matched your search.",
            onDismiss = { showCurrencySelector = false },
            onSelected = { selectedCurrency ->
                currency = selectedCurrency
                showCurrencySelector = false
            },
            itemTitle = { it.selectorLabel },
            itemSubtitle = { "Symbol: ${it.symbol}" },
            itemSearchText = { "${it.code} ${it.displayName} ${it.symbol}" }
        )
    }

    if (showNotificationPrimerDialog) {
        AlertDialog(
            onDismissRequest = { showNotificationPrimerDialog = false },
            title = { Text("Allow notifications?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Notifications help with bills, budget alerts, recurring reminders, and cycle renewal prompts.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "You can skip this for now and keep using the app. Notifications can be enabled later in Settings.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showNotificationPrimerDialog = false
                        NotificationPermissionManager.markPromptShown(context, preferences.userId)
                        requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                ) {
                    Text("Continue")
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(
                    onClick = {
                        showNotificationPrimerDialog = false
                        notificationsEnabled = false
                    }
                ) {
                    Text("Not now")
                }
            }
        )
    }
}

@Composable
private fun SetupSectionCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    content: @Composable ColumnScope.() -> Unit
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
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top
            ) {
                Box(
                    modifier = Modifier
                        .background(
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.82f),
                            shape = RoundedCornerShape(16.dp)
                        )
                        .padding(10.dp)
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(text = title, style = MaterialTheme.typography.titleLarge)
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            content()
        }
    }
}

@Composable
private fun SelectionTriggerCard(
    title: String,
    value: String,
    supportingText: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.28f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Clip
                )
                Text(
                    text = supportingText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Clip
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.ArrowForwardIos,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outline
            )
        }
    }
}

@Composable
private fun PreferenceToggleRow(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .background(
                        color = MaterialTheme.colorScheme.surfaceVariant,
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
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(text = title, style = MaterialTheme.typography.titleSmall)
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
private fun SetupPill(label: String) {
    Box(
        modifier = Modifier
            .background(
                color = MaterialTheme.colorScheme.surface,
                shape = CircleShape
            )
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

private fun defaultNextCycleDate(
    cycleType: BudgetCycleType,
    today: LocalDate
): LocalDate {
    return when (cycleType) {
        BudgetCycleType.WEEKLY -> today.plusWeeks(1)
        BudgetCycleType.MONTHLY -> today.plusMonths(1)
        BudgetCycleType.YEARLY -> today.plusYears(1)
    }
}
