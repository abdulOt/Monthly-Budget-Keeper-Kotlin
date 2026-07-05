package com.talent.monthlybudgetkeeper.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.talent.monthlybudgetkeeper.data.local.entity.TransactionEntity
import com.talent.monthlybudgetkeeper.data.model.CurrencyOption
import com.talent.monthlybudgetkeeper.data.model.TransactionType
import com.talent.monthlybudgetkeeper.utils.CurrencyFormatter
import com.talent.monthlybudgetkeeper.utils.DateUtils

private val IncomeAmountColor = Color(0xFF1D8F69)

@Composable
fun TransactionItemCard(
    transaction: TransactionEntity,
    currency: CurrencyOption,
    modifier: Modifier = Modifier,
    hideSensitiveValues: Boolean = false,
    onClick: () -> Unit
) {
    val categoryColor = CategoryVisuals.color(transaction.category)
    val amountColor = if (transaction.type == TransactionType.INCOME) {
        IncomeAmountColor
    } else {
        MaterialTheme.colorScheme.error
    }
    val typeLabel = when {
        transaction.isTransfer -> "Transfer"
        transaction.type == TransactionType.INCOME -> "Income"
        else -> "Expense"
    }
    val typePillColor = when {
        transaction.isTransfer -> MaterialTheme.colorScheme.tertiaryContainer
        transaction.type == TransactionType.INCOME -> MaterialTheme.colorScheme.secondaryContainer
        else -> MaterialTheme.colorScheme.errorContainer
    }
    val typePillTextColor = when {
        transaction.isTransfer -> MaterialTheme.colorScheme.onTertiaryContainer
        transaction.type == TransactionType.INCOME -> MaterialTheme.colorScheme.onSecondaryContainer
        else -> MaterialTheme.colorScheme.onErrorContainer
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.18f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(categoryColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = CategoryVisuals.icon(transaction.category),
                    contentDescription = null,
                    tint = categoryColor
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = transaction.title,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Box(
                        modifier = Modifier
                            .background(
                                color = typePillColor,
                                shape = RoundedCornerShape(50)
                            )
                            .padding(horizontal = 8.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = typeLabel,
                            style = MaterialTheme.typography.labelMedium,
                            color = typePillTextColor,
                            maxLines = 1
                        )
                    }
                }
                Text(
                    text = "${transaction.category.displayName} - ${DateUtils.formatDate(transaction.date)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (transaction.note.isNotBlank()) {
                    Text(
                        text = transaction.note,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Spacer(modifier = Modifier.size(2.dp))
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                SensitiveValueText(
                    text = buildString {
                        append(if (transaction.type == TransactionType.INCOME) "+ " else "- ")
                        append(CurrencyFormatter.format(transaction.amount, currency))
                    },
                    hidden = hideSensitiveValues,
                    style = MaterialTheme.typography.titleMedium,
                    color = amountColor
                )
                Text(
                    text = if (transaction.type == TransactionType.INCOME) "Cash in" else "Cash out",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
