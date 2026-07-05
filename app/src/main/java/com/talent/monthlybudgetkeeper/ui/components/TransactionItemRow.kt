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
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.unit.dp
import com.talent.monthlybudgetkeeper.data.local.entity.TransactionEntity
import com.talent.monthlybudgetkeeper.data.model.CurrencyOption
import com.talent.monthlybudgetkeeper.data.model.TransactionType
import com.talent.monthlybudgetkeeper.utils.CurrencyFormatter
import com.talent.monthlybudgetkeeper.utils.DateUtils

@Composable
fun TransactionItemRow(
    transaction: TransactionEntity,
    currency: CurrencyOption,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val categoryColor = CategoryVisuals.color(transaction.category)
    val amountColor = if (transaction.type == TransactionType.INCOME) {
        Color(0xFF0E9F6E)
    } else {
        MaterialTheme.colorScheme.error
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
        Box(
            modifier = Modifier
                .size(50.dp)
                .clip(CircleShape)
                .background(categoryColor.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = CategoryVisuals.icon(transaction.category),
                contentDescription = null,
                tint = categoryColor
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(text = transaction.title, style = MaterialTheme.typography.titleMedium)
            Text(
                text = "${transaction.category.displayName} • ${DateUtils.formatDate(transaction.date)}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (transaction.note.isNotBlank()) {
                Text(
                    text = transaction.note,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Text(
            text = buildString {
                append(if (transaction.type == TransactionType.INCOME) "+ " else "- ")
                append(CurrencyFormatter.format(transaction.amount, currency))
            },
            style = MaterialTheme.typography.titleMedium,
            color = amountColor
        )
        }
    }
}
