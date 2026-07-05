package com.talent.monthlybudgetkeeper.ui.screens.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.LockReset
import androidx.compose.material.icons.outlined.Password
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.talent.monthlybudgetkeeper.data.model.AuthSessionState
import com.talent.monthlybudgetkeeper.ui.components.AuthScaffold
import com.talent.monthlybudgetkeeper.ui.components.QuickActionButton
import com.talent.monthlybudgetkeeper.viewmodel.AuthActionState
import com.talent.monthlybudgetkeeper.viewmodel.AuthViewModel

@Composable
fun ForgotPasswordScreen(
    onBackToLogin: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val actionState by viewModel.actions.collectAsStateWithLifecycle(initialValue = AuthActionState())
    val sessionState by viewModel.sessionState.collectAsStateWithLifecycle()
    val isRecoveryFlow = (sessionState as? AuthSessionState.Authenticated)?.requiresPasswordReset == true

    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var confirmPassword by rememberSaveable { mutableStateOf("") }

    LaunchedEffect(actionState.isLoading) {
        if (actionState.isLoading) {
            keyboardController?.hide()
            focusManager.clearFocus(force = true)
        }
    }

    AuthScaffold(
        title = if (isRecoveryFlow) "Set a new password" else "Reset your password",
        subtitle = if (isRecoveryFlow) {
            "Your recovery link was verified. Choose a new password to finish securing your account."
        } else {
            "We'll send a secure reset link to your email so you can create a new password."
        },
        icon = Icons.Outlined.LockReset
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                    shape = RoundedCornerShape(18.dp)
                )
                .padding(14.dp)
        ) {
            Text(
                text = if (isRecoveryFlow) "Final step" else "Secure recovery",
                style = MaterialTheme.typography.titleSmall
            )
            Text(
                text = if (isRecoveryFlow) {
                    "Choose something memorable to you and difficult for others to guess."
                } else {
                    "The link will bring you back into the app so you can safely set a fresh password."
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (!isRecoveryFlow) {
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Email address") },
                supportingText = { Text("We'll send the reset link to this email.") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        keyboardController?.hide()
                        focusManager.clearFocus(force = true)
                        if (!actionState.isLoading) {
                            viewModel.sendPasswordReset(email)
                        }
                    }
                ),
                leadingIcon = {
                    androidx.compose.material3.Icon(
                        imageVector = Icons.Outlined.Email,
                        contentDescription = null
                    )
                }
            )
            QuickActionButton(
                label = if (actionState.isLoading) "Sending reset link..." else "Send reset link",
                icon = Icons.Outlined.LockReset,
                modifier = Modifier.fillMaxWidth(),
                enabled = !actionState.isLoading,
                onClick = {
                    keyboardController?.hide()
                    focusManager.clearFocus(force = true)
                    viewModel.sendPasswordReset(email)
                }
            )
        } else {
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("New password") },
                supportingText = { Text("Choose a strong replacement password.") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Next
                ),
                leadingIcon = {
                    androidx.compose.material3.Icon(
                        imageVector = Icons.Outlined.Password,
                        contentDescription = null
                    )
                }
            )
            OutlinedTextField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Confirm new password") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        keyboardController?.hide()
                        focusManager.clearFocus(force = true)
                        if (!actionState.isLoading) {
                            viewModel.updatePassword(password, confirmPassword)
                        }
                    }
                ),
                leadingIcon = {
                    androidx.compose.material3.Icon(
                        imageVector = Icons.Outlined.Password,
                        contentDescription = null
                    )
                }
            )
            QuickActionButton(
                label = if (actionState.isLoading) "Saving password..." else "Update password",
                icon = Icons.Outlined.LockReset,
                modifier = Modifier.fillMaxWidth(),
                enabled = !actionState.isLoading,
                onClick = {
                    keyboardController?.hide()
                    focusManager.clearFocus(force = true)
                    viewModel.updatePassword(password, confirmPassword)
                }
            )
        }

        TextButton(onClick = onBackToLogin, modifier = Modifier.fillMaxWidth()) {
            Text("Back to login")
        }
    }
}
