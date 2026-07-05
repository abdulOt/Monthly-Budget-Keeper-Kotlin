package com.talent.monthlybudgetkeeper.ui.screens.transaction

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Notes
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Wallet
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.talent.monthlybudgetkeeper.ui.components.CategoryVisuals
import com.talent.monthlybudgetkeeper.utils.CurrencyFormatter
import com.talent.monthlybudgetkeeper.utils.DateUtils
import com.talent.monthlybudgetkeeper.viewmodel.TransactionDetailViewModel
import kotlinx.coroutines.flow.collectLatest

@Composable
fun TransactionDetailScreen(
    contentPadding: PaddingValues = PaddingValues(),
    onBack: () -> Unit,
    onEdit: (Long) -> Unit,
    onDeleted: () -> Unit,
    viewModel: TransactionDetailViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showDeleteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.deleted.collectLatest { onDeleted() }
    }

    val transaction = state.transaction

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 20.dp,
            end = 20.dp,
            top = contentPadding.calculateTopPadding() + 16.dp,
            bottom = contentPadding.calculateBottomPadding() + 24.dp
        ),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                }
                if (transaction != null) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { onEdit(transaction.id) }) {
                            Icon(Icons.Outlined.Edit, contentDescription = null)
                            Text("Edit", modifier = Modifier.padding(start = 8.dp))
                        }
                        OutlinedButton(onClick = { showDeleteDialog = true }) {
                            Icon(Icons.Outlined.DeleteOutline, contentDescription = null)
                            Text("Delete", modifier = Modifier.padding(start = 8.dp))
                        }
                    }
                }
            }
        }

        if (transaction == null) {
            item {
                Card {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("Transaction not found.", style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = "This entry may have been removed or is no longer available locally.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            item {
                AmountHeroCard(
                    title = transaction.title,
                    amount = CurrencyFormatter.format(transaction.amount, state.currency),
                    typeLabel = transaction.type.name.lowercase().replaceFirstChar(Char::uppercase),
                    categoryLabel = transaction.category.displayName,
                    categoryIcon = CategoryVisuals.icon(transaction.category),
                    accentColor = CategoryVisuals.color(transaction.category),
                    isIncome = transaction.type.name == "INCOME"
                )
            }
            item {
                DetailInfoCard(
                    title = "Transaction details",
                    items = listOfNotNull(
                        DetailItem(
                            label = "Category",
                            value = transaction.category.displayName,
                            icon = Icons.Outlined.Payments
                        ),
                        DetailItem(
                            label = "Type",
                            value = transaction.type.name.lowercase().replaceFirstChar(Char::uppercase),
                            icon = Icons.Outlined.Description
                        ),
                        DetailItem(
                            label = "Account",
                            value = state.account?.name ?: "No linked account",
                            icon = Icons.Outlined.Wallet
                        ),
                        DetailItem(
                            label = "Date",
                            value = DateUtils.formatDate(transaction.date),
                            icon = Icons.Outlined.Event
                        )
                    )
                )
            }
            if (transaction.note.isNotBlank()) {
                item {
                    SingleBlockCard(
                        title = "Note",
                        icon = Icons.AutoMirrored.Outlined.Notes,
                        content = transaction.note
                    )
                }
            }
            if (state.receipts.isNotEmpty()) {
                item {
                    Card(
                        shape = MaterialTheme.shapes.large,
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .background(
                                            color = MaterialTheme.colorScheme.secondaryContainer,
                                            shape = CircleShape
                                        )
                                        .padding(10.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Outlined.ReceiptLong,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                }
                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text("Receipt attachments", style = MaterialTheme.typography.titleMedium)
                                    Text(
                                        text = "Saved with this transaction.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            state.receipts.forEach { receipt ->
                                ReceiptAttachmentCard(
                                    fileName = receipt.fileName,
                                    mimeType = receipt.mimeType,
                                    localUri = receipt.localUri
                                )
                            }
                        }
                    }
                }
            }
            item {
                Card(
                    shape = MaterialTheme.shapes.large,
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = { onEdit(transaction.id) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Outlined.Edit, contentDescription = null)
                            Text("Edit", modifier = Modifier.padding(start = 8.dp))
                        }
                        OutlinedButton(
                            onClick = { showDeleteDialog = true },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Outlined.DeleteOutline, contentDescription = null)
                            Text("Delete", modifier = Modifier.padding(start = 8.dp))
                        }
                    }
                }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete transaction?") },
            text = { Text("This action removes the transaction permanently from your local history.") },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteDialog = false
                        viewModel.deleteTransaction()
                    }
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun AmountHeroCard(
    title: String,
    amount: String,
    typeLabel: String,
    categoryLabel: String,
    categoryIcon: ImageVector,
    accentColor: Color,
    isIncome: Boolean
) {
    Card(
        shape = MaterialTheme.shapes.extraLarge,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.32f),
                    shape = MaterialTheme.shapes.extraLarge
                )
                .padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
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
                        text = title,
                        style = MaterialTheme.typography.headlineMedium,
                        maxLines = 3,
                        overflow = TextOverflow.Clip
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        BadgePill(
                            text = typeLabel,
                            background = if (isIncome) Color(0xFFDDF5EB) else MaterialTheme.colorScheme.errorContainer,
                            contentColor = if (isIncome) Color(0xFF176246) else MaterialTheme.colorScheme.onErrorContainer
                        )
                        BadgePill(
                            text = categoryLabel,
                            background = accentColor.copy(alpha = 0.16f),
                            contentColor = accentColor
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .background(
                            color = accentColor.copy(alpha = 0.14f),
                            shape = RoundedCornerShape(18.dp)
                        )
                        .padding(12.dp)
                ) {
                    Icon(
                        imageVector = categoryIcon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            Text(
                text = amount,
                style = MaterialTheme.typography.displaySmall,
                color = if (isIncome) Color(0xFF176246) else MaterialTheme.colorScheme.error
            )
        }
    }
}

@Composable
private fun DetailInfoCard(
    title: String,
    items: List<DetailItem>
) {
    Card(
        shape = MaterialTheme.shapes.large,
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            items.forEach { item ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Box(
                        modifier = Modifier
                            .background(
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(14.dp)
                            )
                            .padding(9.dp)
                    ) {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(
                            text = item.label,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = item.value,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SingleBlockCard(
    title: String,
    icon: ImageVector,
    content: String
) {
    Card(
        shape = MaterialTheme.shapes.large,
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .background(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = CircleShape
                        )
                        .padding(9.dp)
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                Text(text = title, style = MaterialTheme.typography.titleMedium)
            }
            Text(
                text = content,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ReceiptAttachmentCard(
    fileName: String,
    mimeType: String,
    localUri: String
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.28f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = fileName,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 2,
                overflow = TextOverflow.Clip
            )
            Text(
                text = mimeType,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = localUri,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun BadgePill(
    text: String,
    background: Color,
    contentColor: Color
) {
    Box(
        modifier = Modifier
            .background(background, RoundedCornerShape(999.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = contentColor
        )
    }
}

private data class DetailItem(
    val label: String,
    val value: String,
    val icon: ImageVector
)
