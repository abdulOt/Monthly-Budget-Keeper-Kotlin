package com.talent.monthlybudgetkeeper.ui.screens.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.AddCircleOutline
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.talent.monthlybudgetkeeper.ui.components.ActionButtonStyle
import com.talent.monthlybudgetkeeper.data.model.TransactionCategory
import com.talent.monthlybudgetkeeper.data.model.TransactionSortOption
import com.talent.monthlybudgetkeeper.data.model.TransactionType
import com.talent.monthlybudgetkeeper.ui.components.EmptyStateCard
import com.talent.monthlybudgetkeeper.ui.components.MonthSelector
import com.talent.monthlybudgetkeeper.ui.components.QuickActionButton
import com.talent.monthlybudgetkeeper.ui.components.TransactionItemCard
import com.talent.monthlybudgetkeeper.viewmodel.HistoryViewModel

@Composable
fun HistoryScreen(
    contentPadding: PaddingValues,
    onAddTransaction: () -> Unit,
    onTransactionClick: (Long) -> Unit,
    viewModel: HistoryViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

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
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(text = "History", style = MaterialTheme.typography.displaySmall)
                Text(
                    text = "Search, filter, and sort every transaction you have stored locally.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        item {
            QuickActionButton(
                label = "Add transaction",
                icon = Icons.Outlined.AddCircleOutline,
                modifier = Modifier.fillMaxWidth(),
                style = ActionButtonStyle.PRIMARY,
                onClick = onAddTransaction
            )
        }
        item {
            MonthSelector(
                month = state.filter.month,
                onPrevious = { viewModel.updateMonth(state.filter.month.minusMonths(1)) },
                onNext = { viewModel.updateMonth(state.filter.month.plusMonths(1)) }
            )
        }
        item {
            Card {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        androidx.compose.material3.Icon(
                            imageVector = Icons.Outlined.FilterList,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Column {
                            Text("Refine results", style = MaterialTheme.typography.titleMedium)
                            Text(
                                text = "Narrow the list by type, category, or sort order.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    ColumnFilters(
                        search = state.filter.searchQuery,
                        selectedType = state.filter.type,
                        selectedCategory = state.filter.category,
                        selectedSort = state.filter.sortOption,
                        onSearchChange = viewModel::updateSearch,
                        onTypeSelected = viewModel::updateType,
                        onCategorySelected = viewModel::updateCategory,
                        onSortSelected = viewModel::updateSort
                    )
                }
            }
        }
        if (state.transactions.isEmpty()) {
            item {
                EmptyStateCard(
                    title = "No matching transactions",
                    message = "Try another month, search term, or filter combination.",
                    icon = Icons.AutoMirrored.Outlined.ReceiptLong
                )
            }
        } else {
            items(state.transactions.size) { index ->
                val transaction = state.transactions[index]
                TransactionItemCard(
                    transaction = transaction,
                    currency = state.currency,
                    onClick = { onTransactionClick(transaction.id) }
                )
            }
        }
    }
}

@Composable
private fun ColumnFilters(
    search: String,
    selectedType: TransactionType?,
    selectedCategory: TransactionCategory?,
    selectedSort: TransactionSortOption,
    onSearchChange: (String) -> Unit,
    onTypeSelected: (TransactionType?) -> Unit,
    onCategorySelected: (TransactionCategory?) -> Unit,
    onSortSelected: (TransactionSortOption) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        OutlinedTextField(
            value = search,
            onValueChange = onSearchChange,
            label = { Text("Search title or note") },
            modifier = Modifier.fillMaxWidth()
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = selectedType == null, onClick = { onTypeSelected(null) }, label = { Text("All") })
            TransactionType.entries.forEach { type ->
                FilterChip(
                    selected = selectedType == type,
                    onClick = { onTypeSelected(type) },
                    label = { Text(type.name.lowercase().replaceFirstChar(Char::uppercase)) }
                )
            }
        }
        androidx.compose.foundation.layout.Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = selectedCategory == null,
                    onClick = { onCategorySelected(null) },
                    label = { Text("Any category") }
                )
            }
            val categories = TransactionCategory.entries.filter {
                selectedType == null || it.type == selectedType
            }
            categories.chunked(4).forEach { rowCategories ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    rowCategories.forEach { category ->
                        FilterChip(
                            selected = selectedCategory == category,
                            onClick = { onCategorySelected(category) },
                            label = { Text(category.displayName) }
                        )
                    }
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TransactionSortOption.entries.forEach { option ->
                FilterChip(
                    selected = selectedSort == option,
                    onClick = { onSortSelected(option) },
                    label = { Text(option.label) }
                )
            }
        }
    }
}
