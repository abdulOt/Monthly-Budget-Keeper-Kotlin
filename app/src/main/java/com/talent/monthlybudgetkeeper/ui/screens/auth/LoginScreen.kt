package com.talent.monthlybudgetkeeper.ui.screens.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Lock
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.talent.monthlybudgetkeeper.ui.components.AuthScaffold
import com.talent.monthlybudgetkeeper.ui.components.QuickActionButton
import com.talent.monthlybudgetkeeper.viewmodel.AuthActionState
import com.talent.monthlybudgetkeeper.viewmodel.AuthViewModel

@Composable
fun LoginScreen(
    onSignUp: () -> Unit,
    onForgotPassword: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val actionState by viewModel.actions.collectAsStateWithLifecycle(initialValue = AuthActionState())
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }

    fun submitLogin() {
        keyboardController?.hide()
        focusManager.clearFocus(force = true)
        if (!actionState.isLoading) {
            viewModel.signIn(email, password)
        }
    }

    LaunchedEffect(actionState.isLoading) {
        if (actionState.isLoading) {
            keyboardController?.hide()
            focusManager.clearFocus(force = true)
        }
    }

    AuthScaffold(
        title = "Sign in to your workspace",
        subtitle = "Use your account to restore private budget data, reminders, and preferences.",
        icon = Icons.Outlined.Lock
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            LoginStepPill(label = "1. Secure access")
            LoginStepPill(label = "2. Setup review")
        }

        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.52f),
                    shape = RoundedCornerShape(20.dp)
                )
                .padding(16.dp)
        ) {
            Text(
                text = "Private by default",
                style = MaterialTheme.typography.titleSmall
            )
            Text(
                text = "Your records stay local for day-to-day use and sync privately to your account when available.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Email address") },
            supportingText = {
                Text(
                    text = "Use the same email tied to your cloud backup."
                )
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                capitalization = KeyboardCapitalization.None,
                imeAction = ImeAction.Next
            ),
            leadingIcon = {
                androidx.compose.material3.Icon(
                    imageVector = Icons.Outlined.Email,
                    contentDescription = null
                )
            }
        )
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Password") },
            supportingText = { Text("Use the Done action on the keyboard to submit.") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done
            ),
            keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                onDone = { submitLogin() }
            ),
            leadingIcon = {
                androidx.compose.material3.Icon(
                    imageVector = Icons.Outlined.Password,
                    contentDescription = null
                )
            }
        )

        QuickActionButton(
            label = if (actionState.isLoading) "Signing in..." else "Login with email",
            icon = Icons.Outlined.Lock,
            modifier = Modifier.fillMaxWidth(),
            enabled = !actionState.isLoading,
            onClick = ::submitLogin
        )

        TextButton(onClick = onForgotPassword, modifier = Modifier.fillMaxWidth()) {
            Text("Forgot password?")
        }
        TextButton(onClick = onSignUp, modifier = Modifier.fillMaxWidth()) {
            Text("Create a new account")
        }
    }
}

@Composable
private fun LoginStepPill(label: String) {
    Row(
        modifier = Modifier
            .background(
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.82f),
                shape = CircleShape
            )
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            maxLines = 1
        )
    }
}
