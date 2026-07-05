package com.talent.monthlybudgetkeeper.ui.screens.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudDone
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.talent.monthlybudgetkeeper.ui.components.ActionButtonStyle
import com.talent.monthlybudgetkeeper.ui.components.AuthScaffold
import com.talent.monthlybudgetkeeper.ui.components.QuickActionButton
import com.talent.monthlybudgetkeeper.viewmodel.AuthActionState
import com.talent.monthlybudgetkeeper.viewmodel.AuthViewModel

@Composable
fun AuthWelcomeScreen(
    onLogin: () -> Unit,
    onSignUp: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val actionState by viewModel.actions.collectAsStateWithLifecycle(initialValue = AuthActionState())

    val highlights = listOf(
        Icons.Outlined.CloudDone to "Restore your private workspace and continue where you left off.",
        Icons.Outlined.Sync to "Sign in with your email account and keep your budget data in sync.",
        Icons.Outlined.VerifiedUser to "Review setup preferences after login before entering the dashboard."
    )

    AuthScaffold(
        title = "Welcome to your finance workspace",
        subtitle = "Choose the sign-in path that fits you and enter a cleaner budget experience.",
        icon = Icons.Outlined.Lock
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                    shape = RoundedCornerShape(20.dp)
                )
                .padding(16.dp)
        ) {
            Text(
                text = "Ready to protect and restore",
                style = MaterialTheme.typography.titleSmall
            )
            Text(
                text = "Your region and currency can stay local until you authenticate, then your sync ownership is attached to your account.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        highlights.forEach { (icon, message) ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top
            ) {
                Row(
                    modifier = Modifier
                        .background(
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.82f),
                            shape = CircleShape
                        )
                        .padding(9.dp)
                ) {
                    androidx.compose.material3.Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        QuickActionButton(
            label = "Use email login",
            icon = Icons.Outlined.Lock,
            modifier = Modifier.fillMaxWidth(),
            enabled = !actionState.isLoading,
            onClick = onLogin
        )
        QuickActionButton(
            label = "Create account",
            icon = Icons.Outlined.VerifiedUser,
            modifier = Modifier.fillMaxWidth(),
            enabled = !actionState.isLoading,
            style = ActionButtonStyle.SECONDARY,
            onClick = onSignUp
        )
    }
}
