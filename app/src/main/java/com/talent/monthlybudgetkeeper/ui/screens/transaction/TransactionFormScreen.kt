@file:OptIn(ExperimentalLayoutApi::class)

package com.talent.monthlybudgetkeeper.ui.screens.transaction

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.talent.monthlybudgetkeeper.data.model.TransactionCategory
import com.talent.monthlybudgetkeeper.data.model.TransactionType
import com.talent.monthlybudgetkeeper.ui.components.DatePickerField
import com.talent.monthlybudgetkeeper.viewmodel.TransactionFormViewModel
import kotlinx.coroutines.flow.collectLatest

@Composable
fun TransactionFormScreen(
    contentPadding: PaddingValues = PaddingValues(),
    onBack: () -> Unit,
    onSaved: (Long) -> Unit,
    viewModel: TransactionFormViewModel = hiltViewModel()
) {
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    fun submit() {
        keyboardController?.hide()
        focusManager.clearFocus(force = true)
        if (!state.isSaving && !state.isLoading) {
            viewModel.saveTransaction()
        }
    }

    LaunchedEffect(Unit) {
        viewModel.events.collectLatest {
            keyboardController?.hide()
            focusManager.clearFocus(force = true)
            onSaved(it)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(
                start = 20.dp,
                top = contentPadding.calculateTopPadding() + 20.dp,
                end = 20.dp,
                bottom = contentPadding.calculateBottomPadding() + 28.dp
            ),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            shape = MaterialTheme.shapes.extraLarge,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top
            ) {
                Box {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                    }
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = if (state.transactionId == 0L) "Add transaction" else "Edit transaction",
                        style = MaterialTheme.typography.headlineMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Keep each entry accurate, readable, and easy to revisit later.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(text = "Transaction type", style = MaterialTheme.typography.titleMedium)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TransactionType.entries.forEach { type ->
                        FilterChip(
                            selected = state.type == type,
                            onClick = { viewModel.updateType(type) },
                            label = { Text(if (type == TransactionType.INCOME) "Income" else "Expense") }
                        )
                    }
                }

                OutlinedTextField(
                    value = state.title,
                    onValueChange = viewModel::updateTitle,
                    label = { Text("Title") },
                    supportingText = {
                        Text(state.titleError ?: "Example: Groceries, Salary, Taxi fare")
                    },
                    isError = state.titleError != null,
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        imeAction = ImeAction.Next
                    )
                )

                OutlinedTextField(
                    value = state.amount,
                    onValueChange = viewModel::updateAmount,
                    label = { Text("Amount") },
                    supportingText = {
                        Text(state.amountError ?: "Enter the exact amount for this record.")
                    },
                    isError = state.amountError != null,
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal,
                        imeAction = ImeAction.Next
                    )
                )

                Text(text = "Category", style = MaterialTheme.typography.titleMedium)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TransactionCategory.forType(state.type).forEach { category ->
                        FilterChip(
                            selected = state.category == category,
                            onClick = { viewModel.updateCategory(category) },
                            label = { Text(category.displayName) }
                        )
                    }
                }

                DatePickerField(
                    label = "Date",
                    selectedDate = state.date,
                    modifier = Modifier.fillMaxWidth(),
                    onDateSelected = viewModel::updateDate
                )

                OutlinedTextField(
                    value = state.note,
                    onValueChange = viewModel::updateNote,
                    label = { Text("Note (optional)") },
                    supportingText = { Text("Add context that will help later when you review the entry.") },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = { submit() }
                    )
                )

                Button(
                    onClick = ::submit,
                    enabled = !state.isSaving && !state.isLoading,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (state.transactionId == 0L) "Save transaction" else "Update transaction")
                }
            }
        }
    }
}
