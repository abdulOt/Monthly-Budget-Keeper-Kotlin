package com.talent.monthlybudgetkeeper.ui.screens.security

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.LockOpen
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import com.talent.monthlybudgetkeeper.ui.components.ActionButtonStyle
import com.talent.monthlybudgetkeeper.ui.components.QuickActionButton
import com.talent.monthlybudgetkeeper.utils.BiometricAuthManager
import com.talent.monthlybudgetkeeper.utils.PinSecurity

@Composable
fun AppLockScreen(
    activity: FragmentActivity,
    pinHash: String,
    biometricEnabled: Boolean,
    onUnlock: () -> Unit,
    onMessage: (String) -> Unit
) {
    var pin by rememberSaveable { mutableStateOf("") }
    var attemptedBiometric by rememberSaveable { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val canUseBiometrics = biometricEnabled && BiometricAuthManager.isBiometricAvailable(activity)

    fun unlockWithPin() {
        keyboardController?.hide()
        focusManager.clearFocus(force = true)
        if (PinSecurity.verify(pin, pinHash)) {
            pin = ""
            onUnlock()
        } else {
            onMessage("Incorrect PIN.")
        }
    }

    LaunchedEffect(canUseBiometrics) {
        if (canUseBiometrics && !attemptedBiometric) {
            attemptedBiometric = true
            BiometricAuthManager.authenticate(
                activity = activity,
                title = "Unlock Monthly Budget Keeper",
                subtitle = "Verify your identity to continue",
                onSuccess = {
                    pin = ""
                    onUnlock()
                },
                onFallback = { },
                onError = onMessage
            )
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f),
                        MaterialTheme.colorScheme.surface,
                        MaterialTheme.colorScheme.background
                    )
                )
            )
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            contentWindowInsets = WindowInsets.systemBars
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 24.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Lock,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text("App locked", style = MaterialTheme.typography.headlineSmall)
                        Text(
                            "Unlock to view balances, budgets, and your synced financial history on this device.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        OutlinedTextField(
                            value = pin,
                            onValueChange = { pin = it.filter(Char::isDigit).take(8) },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("PIN") },
                            supportingText = { Text("Use the PIN you created in Settings.") },
                            singleLine = true,
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.NumberPassword,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(onDone = { unlockWithPin() })
                        )
                        Button(
                            onClick = { unlockWithPin() },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(imageVector = Icons.Outlined.LockOpen, contentDescription = null)
                            Text("Unlock", modifier = Modifier.padding(start = 8.dp))
                        }
                        if (canUseBiometrics) {
                            QuickActionButton(
                                label = "Use biometrics",
                                icon = Icons.Outlined.Fingerprint,
                                modifier = Modifier.fillMaxWidth(),
                                style = ActionButtonStyle.SECONDARY,
                                onClick = {
                                    keyboardController?.hide()
                                    focusManager.clearFocus(force = true)
                                    BiometricAuthManager.authenticate(
                                        activity = activity,
                                        title = "Unlock Monthly Budget Keeper",
                                        subtitle = "Verify your identity to continue",
                                        onSuccess = {
                                            pin = ""
                                            onUnlock()
                                        },
                                        onFallback = { },
                                        onError = onMessage
                                    )
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
