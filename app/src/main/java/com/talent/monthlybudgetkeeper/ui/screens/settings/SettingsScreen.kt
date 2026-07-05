@file:OptIn(ExperimentalLayoutApi::class)

package com.talent.monthlybudgetkeeper.ui.screens.settings

import android.Manifest
import android.app.Activity
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.automirrored.outlined.ArrowForwardIos
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PrivacyTip
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.res.stringResource
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.talent.monthlybudgetkeeper.AppState
import com.talent.monthlybudgetkeeper.R
import com.talent.monthlybudgetkeeper.data.notifications.NotificationPermissionManager
import com.talent.monthlybudgetkeeper.data.notifications.NotificationPermissionStatus
import com.talent.monthlybudgetkeeper.data.model.AuthSessionState
import com.talent.monthlybudgetkeeper.data.model.CurrencyOption
import com.talent.monthlybudgetkeeper.data.model.RegionOption
import com.talent.monthlybudgetkeeper.data.model.SyncState
import com.talent.monthlybudgetkeeper.data.model.WeekStartOption
import com.talent.monthlybudgetkeeper.ui.components.ActionButtonStyle
import com.talent.monthlybudgetkeeper.ui.components.QuickActionButton
import com.talent.monthlybudgetkeeper.ui.components.SearchableSelectionDialog
import com.talent.monthlybudgetkeeper.utils.BiometricAuthManager
import com.talent.monthlybudgetkeeper.viewmodel.AuthViewModel
import com.talent.monthlybudgetkeeper.viewmodel.SettingsViewModel
import kotlinx.coroutines.flow.collectLatest

@Composable
fun SettingsScreen(
    contentPadding: PaddingValues,
    onResetComplete: () -> Unit,
    appState: AppState,
    authViewModel: AuthViewModel,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val privacyPolicyUrl = stringResource(id = R.string.privacy_policy_url)
    val lifecycleOwner = LocalLifecycleOwner.current
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val activity = context as? FragmentActivity
    val preferences by viewModel.preferences.collectAsStateWithLifecycle()
    val sessionState by authViewModel.sessionState.collectAsStateWithLifecycle()
    val syncState by authViewModel.syncState.collectAsStateWithLifecycle()
    var profileName by remember(preferences.profileName) { mutableStateOf(preferences.profileName) }
    var reminderLeadDays by remember(preferences.reminderLeadDays) {
        mutableStateOf(preferences.reminderLeadDays.toString())
    }
    var showResetDialog by remember { mutableStateOf(false) }
    var pendingRestoreUri by remember { mutableStateOf<Uri?>(null) }
    var showPinDialog by remember { mutableStateOf(false) }
    var showClearPinDialog by remember { mutableStateOf(false) }
    var pin by remember { mutableStateOf("") }
    var pinConfirm by remember { mutableStateOf("") }
    var showCountrySelector by remember { mutableStateOf(false) }
    var showCurrencySelector by remember { mutableStateOf(false) }
    var notificationPermissionVersion by remember { mutableStateOf(0) }
    var showNotificationPrimerDialog by remember { mutableStateOf(false) }
    var showDeleteAccountDialog by remember { mutableStateOf(false) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                notificationPermissionVersion += 1
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val notificationPermissionStatus = remember(
        notificationPermissionVersion,
        preferences.notificationsEnabled,
        activity,
        preferences.userId
    ) {
        NotificationPermissionManager.permissionStatus(
            context = context,
            activity = activity as? Activity,
            userId = preferences.userId
        )
    }
    val notificationsEffectivelyEnabled =
        preferences.notificationsEnabled &&
            notificationPermissionStatus == NotificationPermissionStatus.GRANTED

    val requestNotificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        notificationPermissionVersion += 1
        if (granted) {
            viewModel.updateNotificationsEnabled(true)
        } else {
            viewModel.updateNotificationsEnabled(false)
            appState.showMessage(
                "Notifications are enabled in the app, but Android permission is still blocked on this device."
            )
        }
    }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri?.let { viewModel.exportBackup(context, it) }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        pendingRestoreUri = uri
    }

    fun saveProfile() {
        keyboardController?.hide()
        focusManager.clearFocus(force = true)
        viewModel.updateProfileName(profileName)
    }

    fun saveReminderTiming() {
        keyboardController?.hide()
        focusManager.clearFocus(force = true)
        viewModel.updateReminderLeadDays(
            reminderLeadDays.toIntOrNull() ?: preferences.reminderLeadDays
        )
    }

    fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT < 33) return
        NotificationPermissionManager.markPromptShown(context, preferences.userId)
        requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    fun enableNotificationsWithPermissionFlow() {
        when (notificationPermissionStatus) {
            NotificationPermissionStatus.GRANTED -> viewModel.updateNotificationsEnabled(true)
            NotificationPermissionStatus.REQUESTABLE,
            NotificationPermissionStatus.RATIONALE_NEEDED -> showNotificationPrimerDialog = true
            NotificationPermissionStatus.BLOCKED -> {
                viewModel.updateNotificationsEnabled(false)
                appState.showMessage("Android blocked notification permission. Open system settings to allow it.")
            }
            NotificationPermissionStatus.SYSTEM_DISABLED -> {
                viewModel.updateNotificationsEnabled(false)
                appState.showMessage("Notifications are turned off for this app in Android settings.")
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.resetCompleted.collectLatest {
            authViewModel.signOut()
            onResetComplete()
        }
    }

    LaunchedEffect(Unit) {
        viewModel.messages.collectLatest { appState.showMessage(it) }
    }

    LazyColumn(
        contentPadding = PaddingValues(
            start = 20.dp,
            end = 20.dp,
            top = contentPadding.calculateTopPadding() + 20.dp,
            bottom = contentPadding.calculateBottomPadding() + 28.dp
        ),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item {
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
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(text = "Settings", style = MaterialTheme.typography.displaySmall)
                    Text(
                        text = "Manage account access, privacy, reminders, sync, and your device backup from one cleaner place.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        item {
            SettingsCard(
                title = "Account",
                subtitle = "Your signed-in identity and profile details.",
                icon = Icons.Outlined.Person
            ) {
                when (val state = sessionState) {
                    is AuthSessionState.Authenticated -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(state.user.displayName, style = MaterialTheme.typography.titleMedium)
                                Text(
                                    state.user.email,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            BoxPill(text = "Signed in")
                        }
                        OutlinedTextField(
                            value = profileName,
                            onValueChange = { profileName = it },
                            label = { Text("Profile name") },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = { saveProfile() })
                        )
                        Button(
                            onClick = { saveProfile() },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Save profile")
                        }
                        QuickActionButton(
                            label = "Log out",
                            icon = Icons.AutoMirrored.Outlined.Logout,
                            modifier = Modifier.fillMaxWidth(),
                            style = ActionButtonStyle.SECONDARY,
                            onClick = authViewModel::signOut
                        )
                    }
                    else -> {
                        Text(
                            "You are not signed in right now.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        item {
            SettingsCard(
                title = "Sync status",
                subtitle = "The state of your private cloud backup and cross-device sync.",
                icon = Icons.Outlined.Sync
            ) {
                Text(
                    text = when (val sync = syncState) {
                        SyncState.Idle -> "Cloud sync is ready."
                        is SyncState.Syncing -> sync.message
                        is SyncState.Success -> "Last sync completed successfully."
                        is SyncState.Error -> sync.message
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                QuickActionButton(
                    label = "Sync now",
                    icon = Icons.Outlined.Sync,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = authViewModel::syncNow
                )
            }
        }

        item {
            SettingsCard(
                title = "Security",
                subtitle = "Protect the app locally with a PIN, biometrics, and privacy controls.",
                icon = Icons.Outlined.Security
            ) {
                SettingToggleRow(
                    title = "App lock",
                    description = if (preferences.pinHash.isNullOrBlank()) {
                        "Create a PIN to require unlock whenever the app returns from background."
                    } else {
                        "PIN protection is active for this device."
                    },
                    checked = preferences.appLockEnabled && !preferences.pinHash.isNullOrBlank(),
                    onCheckedChange = { enabled ->
                        if (enabled) {
                            showPinDialog = true
                        } else {
                            showClearPinDialog = true
                        }
                    },
                    icon = Icons.Outlined.Lock
                )
                if (!preferences.pinHash.isNullOrBlank()) {
                    QuickActionButton(
                        label = "Change PIN",
                        icon = Icons.Outlined.Lock,
                        modifier = Modifier.fillMaxWidth(),
                        style = ActionButtonStyle.SECONDARY,
                        onClick = { showPinDialog = true }
                    )
                    QuickActionButton(
                        label = "Lock app now",
                        icon = Icons.Outlined.Lock,
                        modifier = Modifier.fillMaxWidth(),
                        onClick = viewModel::lockAppNow
                    )
                }
                SettingToggleRow(
                    title = "Biometric unlock",
                    description = "Use device biometrics as a faster unlock option alongside your PIN.",
                    checked = preferences.biometricLockEnabled,
                    enabled = activity != null &&
                        !preferences.pinHash.isNullOrBlank() &&
                        BiometricAuthManager.isBiometricAvailable(activity),
                    onCheckedChange = viewModel::updateBiometricLockEnabled,
                    icon = Icons.Outlined.Security
                )
                SettingToggleRow(
                    title = "Privacy mode",
                    description = "Blur balances and sensitive charts, and protect the app from screenshots.",
                    checked = preferences.privacyModeEnabled,
                    onCheckedChange = viewModel::updatePrivacyModeEnabled,
                    icon = Icons.Outlined.PrivacyTip
                )
            }
        }

        item {
            SettingsCard(
                title = "Notifications and reminders",
                subtitle = "Control the nudges that help you stay ahead of bills, budgets, debts, and goals.",
                icon = Icons.Outlined.Notifications
            ) {
                SettingToggleRow(
                    title = "Notifications",
                    description = "Allow local reminders about bills, recurring items, debts, budgets, and goals.",
                    checked = notificationsEffectivelyEnabled,
                    onCheckedChange = { enabled ->
                        if (enabled) {
                            enableNotificationsWithPermissionFlow()
                        } else {
                            viewModel.updateNotificationsEnabled(false)
                        }
                    },
                    icon = Icons.Outlined.Notifications
                )
                if (preferences.notificationsEnabled &&
                    notificationPermissionStatus != NotificationPermissionStatus.GRANTED
                ) {
                    Text(
                        text = when (notificationPermissionStatus) {
                            NotificationPermissionStatus.REQUESTABLE ->
                                "Android still needs your permission before reminders can be shown."
                            NotificationPermissionStatus.RATIONALE_NEEDED ->
                                "Allow notifications so bill, budget, debt, and goal reminders can reach you."
                            NotificationPermissionStatus.BLOCKED ->
                                "Notification permission was denied. Open Android settings to allow it later."
                            NotificationPermissionStatus.SYSTEM_DISABLED ->
                                "Android notifications are currently switched off for this app."
                            NotificationPermissionStatus.GRANTED -> ""
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (notificationPermissionStatus == NotificationPermissionStatus.REQUESTABLE ||
                        notificationPermissionStatus == NotificationPermissionStatus.RATIONALE_NEEDED
                    ) {
                        QuickActionButton(
                            label = "Allow notifications",
                            icon = Icons.Outlined.Notifications,
                            modifier = Modifier.fillMaxWidth(),
                            style = ActionButtonStyle.SECONDARY,
                            onClick = {
                                keyboardController?.hide()
                                focusManager.clearFocus(force = true)
                                requestNotificationPermission()
                            }
                        )
                    } else {
                        QuickActionButton(
                            label = "Open notification settings",
                            icon = Icons.Outlined.Notifications,
                            modifier = Modifier.fillMaxWidth(),
                            style = ActionButtonStyle.SECONDARY,
                            onClick = {
                                keyboardController?.hide()
                                focusManager.clearFocus(force = true)
                                NotificationPermissionManager.openAppNotificationSettings(context)
                            }
                        )
                    }
                }
                SettingToggleRow(
                    title = "Bills due soon",
                    description = "Remind me before subscription bills are charged.",
                    checked = preferences.billRemindersEnabled,
                    enabled = preferences.notificationsEnabled,
                    onCheckedChange = viewModel::updateBillRemindersEnabled,
                    icon = Icons.Outlined.Notifications
                )
                SettingToggleRow(
                    title = "Recurring transactions",
                    description = "Remind me before recurring income or expenses come up.",
                    checked = preferences.recurringRemindersEnabled,
                    enabled = preferences.notificationsEnabled,
                    onCheckedChange = viewModel::updateRecurringRemindersEnabled,
                    icon = Icons.Outlined.Notifications
                )
                SettingToggleRow(
                    title = "Debt payments",
                    description = "Warn me when a debt payment is due soon.",
                    checked = preferences.debtRemindersEnabled,
                    enabled = preferences.notificationsEnabled,
                    onCheckedChange = viewModel::updateDebtRemindersEnabled,
                    icon = Icons.Outlined.Notifications
                )
                SettingToggleRow(
                    title = "Budget exceeded",
                    description = "Alert me when category or envelope spending goes over plan.",
                    checked = preferences.budgetAlertsEnabled,
                    enabled = preferences.notificationsEnabled,
                    onCheckedChange = viewModel::updateBudgetAlertsEnabled,
                    icon = Icons.Outlined.Notifications
                )
                SettingToggleRow(
                    title = "Goals behind schedule",
                    description = "Remind me when a savings goal slips behind target pace.",
                    checked = preferences.goalRemindersEnabled,
                    enabled = preferences.notificationsEnabled,
                    onCheckedChange = viewModel::updateGoalRemindersEnabled,
                    icon = Icons.Outlined.Notifications
                )
                OutlinedTextField(
                    value = reminderLeadDays,
                    onValueChange = { reminderLeadDays = it.filter(Char::isDigit).take(2) },
                    label = { Text("Reminder lead time (days)") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = preferences.notificationsEnabled,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(onDone = { saveReminderTiming() })
                )
                Button(
                    onClick = { saveReminderTiming() },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = preferences.notificationsEnabled
                ) {
                    Text("Save reminder timing")
                }
                QuickActionButton(
                    label = "Send test notification",
                    icon = Icons.Outlined.Notifications,
                    modifier = Modifier.fillMaxWidth(),
                    style = ActionButtonStyle.SECONDARY,
                    onClick = {
                        keyboardController?.hide()
                        focusManager.clearFocus(force = true)
                        viewModel.sendTestNotification()
                    }
                )
            }
        }

        item {
            SettingsCard(
                title = "Backup and export",
                subtitle = "Export your finance data as JSON before deleting it, or restore a backup later.",
                icon = Icons.Outlined.Download
            ) {
                QuickActionButton(
                    label = "Export JSON backup",
                    icon = Icons.Outlined.Download,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { exportLauncher.launch("monthly_budget_keeper_backup.json") }
                )
                QuickActionButton(
                    label = "Import and restore backup",
                    icon = Icons.Outlined.Download,
                    modifier = Modifier.fillMaxWidth(),
                    style = ActionButtonStyle.SECONDARY,
                    onClick = { importLauncher.launch(arrayOf("application/json", "text/plain")) }
                )
            }
        }

        item {
            SettingsCard(
                title = "Display and region",
                subtitle = "Set the currency, appearance, and week structure that fit your routine.",
                icon = Icons.Outlined.Public
            ) {
                SelectionSettingsRow(
                    title = "Currency",
                    value = preferences.currency.selectorLabel,
                    supportingText = "Search by ISO code, name, or symbol.",
                    onClick = {
                        keyboardController?.hide()
                        focusManager.clearFocus(force = true)
                        showCurrencySelector = true
                    }
                )
                SelectionSettingsRow(
                    title = "Country / region",
                    value = preferences.region.label,
                    supportingText = "Search the full country list and update it any time.",
                    onClick = {
                        keyboardController?.hide()
                        focusManager.clearFocus(force = true)
                        showCountrySelector = true
                    }
                )
                SettingToggleRow(
                    title = "Dark mode",
                    description = "Switch between light and dark appearance.",
                    checked = preferences.darkMode,
                    onCheckedChange = viewModel::updateDarkMode,
                    icon = Icons.Outlined.PrivacyTip
                )
                Text("Week starts on", style = MaterialTheme.typography.titleMedium)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    WeekStartOption.entries.forEach { option ->
                        FilterChip(
                            selected = preferences.weekStart == option,
                            onClick = { viewModel.updateWeekStart(option) },
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
            }
        }

        item {
            SettingsCard(
                title = "Reset local cache",
                subtitle = "Clear finance records and preferences stored on this device without deleting your cloud account.",
                icon = Icons.Outlined.Lock
            ) {
                Text(
                    "This clears Room data and user-scoped settings on this device. If cloud sync is enabled, your data can restore again later.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Button(
                    onClick = { showResetDialog = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Reset local cache")
                }
            }
        }

        item {
            SettingsCard(
                title = "Privacy & data",
                subtitle = "Review what this app stores, why it is used, and how to export or delete it.",
                icon = Icons.Outlined.PrivacyTip
            ) {
                Text(
                    text = "Stored data can include account identity, budgets, transactions, bills and subscriptions, goals, debts, assets, liabilities, and notification preferences.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "The app uses this data to calculate balances, keep your budget history accurate, and sync your finance workspace across devices when you sign in.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Export your JSON backup before deletion if you want a portable copy of your data.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                QuickActionButton(
                    label = "Open privacy policy",
                    icon = Icons.Outlined.Public,
                    modifier = Modifier.fillMaxWidth(),
                    style = ActionButtonStyle.SECONDARY,
                    onClick = { uriHandler.openUri(privacyPolicyUrl) }
                )
                QuickActionButton(
                    label = "Delete Account and Data",
                    icon = Icons.Outlined.Lock,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        keyboardController?.hide()
                        focusManager.clearFocus(force = true)
                        showDeleteAccountDialog = true
                    }
                )
            }
        }
    }

    if (showPinDialog) {
        AlertDialog(
            onDismissRequest = { showPinDialog = false },
            title = {
                Text(
                    if (preferences.pinHash.isNullOrBlank()) {
                        "Set app lock PIN"
                    } else {
                        "Change app lock PIN"
                    }
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = pin,
                        onValueChange = { pin = it.filter(Char::isDigit).take(8) },
                        label = { Text("New PIN") },
                        modifier = Modifier.fillMaxWidth(),
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.NumberPassword,
                            imeAction = ImeAction.Next
                        )
                    )
                    OutlinedTextField(
                        value = pinConfirm,
                        onValueChange = { pinConfirm = it.filter(Char::isDigit).take(8) },
                        label = { Text("Confirm PIN") },
                        modifier = Modifier.fillMaxWidth(),
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.NumberPassword,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                keyboardController?.hide()
                                focusManager.clearFocus(force = true)
                                showPinDialog = false
                                viewModel.savePin(pin, pinConfirm)
                                pin = ""
                                pinConfirm = ""
                            }
                        )
                    )
                    Text(
                        text = "Use 4 to 8 digits. Your PIN stays on this device and is stored securely.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        keyboardController?.hide()
                        focusManager.clearFocus(force = true)
                        showPinDialog = false
                        viewModel.savePin(pin, pinConfirm)
                        pin = ""
                        pinConfirm = ""
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        keyboardController?.hide()
                        focusManager.clearFocus(force = true)
                        showPinDialog = false
                        pin = ""
                        pinConfirm = ""
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showClearPinDialog) {
        AlertDialog(
            onDismissRequest = { showClearPinDialog = false },
            title = { Text("Disable app lock?") },
            text = { Text("This will remove the local PIN and biometric unlock for this device.") },
            confirmButton = {
                Button(
                    onClick = {
                        keyboardController?.hide()
                        focusManager.clearFocus(force = true)
                        showClearPinDialog = false
                        viewModel.clearPin()
                    }
                ) {
                    Text("Disable")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearPinDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    pendingRestoreUri?.let { uri ->
        AlertDialog(
            onDismissRequest = { pendingRestoreUri = null },
            title = { Text("Restore from backup?") },
            text = {
                Text("This will replace your local finance data and settings with the selected backup file.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        keyboardController?.hide()
                        focusManager.clearFocus(force = true)
                        pendingRestoreUri = null
                        viewModel.restoreBackup(context, uri)
                    }
                ) {
                    Text("Restore")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingRestoreUri = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Reset local cache") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "This clears finance records and user-scoped settings on this device only.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "If cloud sync is enabled, your data may be downloaded again after the next sync.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        keyboardController?.hide()
                        focusManager.clearFocus(force = true)
                        showResetDialog = false
                        viewModel.resetLocalCache()
                    }
                ) {
                    Text("Reset local cache")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showDeleteAccountDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteAccountDialog = false },
            title = { Text("Delete account and data?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "This permanently deletes your user-owned finance data from Supabase, clears local Room data, removes user-scoped settings, and signs you out.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "Export your JSON backup first if you want to keep a copy before deletion.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "This action cannot be undone.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        keyboardController?.hide()
                        focusManager.clearFocus(force = true)
                        showDeleteAccountDialog = false
                        viewModel.deleteAccountAndData()
                    }
                ) {
                    Text("Delete account")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteAccountDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showNotificationPrimerDialog) {
        AlertDialog(
            onDismissRequest = { showNotificationPrimerDialog = false },
            title = { Text("Allow notifications?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Monthly Budget Keeper uses notifications for bill reminders, budget alerts, debt due dates, and cycle renewal prompts.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "You can keep using the app without this permission and turn notifications on later from Settings.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showNotificationPrimerDialog = false
                        requestNotificationPermission()
                    }
                ) {
                    Text("Continue")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showNotificationPrimerDialog = false
                        viewModel.updateNotificationsEnabled(false)
                    }
                ) {
                    Text("Not now")
                }
            }
        )
    }

    if (showCountrySelector) {
        SearchableSelectionDialog(
            title = "Select your country",
            searchPlaceholder = "Search country",
            options = RegionOption.entries,
            selectedOption = preferences.region,
            emptyMessage = "No country matched your search.",
            onDismiss = { showCountrySelector = false },
            onSelected = { selectedRegion ->
                showCountrySelector = false
                viewModel.updateRegion(selectedRegion)
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
            selectedOption = preferences.currency,
            emptyMessage = "No currency matched your search.",
            onDismiss = { showCurrencySelector = false },
            onSelected = { selectedCurrency ->
                showCurrencySelector = false
                viewModel.updateCurrency(selectedCurrency)
            },
            itemTitle = { it.selectorLabel },
            itemSubtitle = { "Symbol: ${it.symbol}" },
            itemSearchText = { "${it.code} ${it.displayName} ${it.symbol}" }
        )
    }
}

@Composable
private fun SettingsCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        shape = MaterialTheme.shapes.large,
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
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
                            shape = CircleShape
                        )
                        .padding(10.dp)
                ) {
                    androidx.compose.material3.Icon(
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
            content()
        }
    }
}

@Composable
private fun SettingToggleRow(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    icon: ImageVector,
    enabled: Boolean = true
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
                        shape = RoundedCornerShape(14.dp)
                    )
                    .padding(8.dp)
            ) {
                androidx.compose.material3.Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled
        )
    }
}

@Composable
private fun BoxPill(text: String) {
    Box(
        modifier = Modifier
            .background(
                color = MaterialTheme.colorScheme.secondaryContainer,
                shape = CircleShape
            )
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSecondaryContainer
        )
    }
}

@Composable
private fun SelectionSettingsRow(
    title: String,
    value: String,
    supportingText: String,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.28f)
        )
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
            androidx.compose.material3.Icon(
                imageVector = Icons.AutoMirrored.Outlined.ArrowForwardIos,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outline
            )
        }
    }
}
