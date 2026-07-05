package com.talent.monthlybudgetkeeper.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.talent.monthlybudgetkeeper.data.model.CategorySpend
import com.talent.monthlybudgetkeeper.data.model.CurrencyOption
import com.talent.monthlybudgetkeeper.data.model.MonthlyTrend
import com.talent.monthlybudgetkeeper.utils.CurrencyFormatter
import com.talent.monthlybudgetkeeper.utils.DateUtils
import kotlin.math.max

private val IncomeTint = Color(0xFF1D8F69)

@Composable
fun IncomeExpenseChart(
    income: Double,
    expense: Double,
    currency: CurrencyOption,
    modifier: Modifier = Modifier
) {
    val total = max(income + expense, 1.0)
    val incomeSweep = (income / total * 360f).toFloat()
    val expenseSweep = 360f - incomeSweep
    val expenseColor = MaterialTheme.colorScheme.error
    var selectedSegment by remember(income, expense) { mutableIntStateOf(if (income >= expense) 0 else 1) }
    val selectedLabel = if (selectedSegment == 0) "Income" else "Expenses"
    val selectedValue = if (selectedSegment == 0) income else expense
    val selectedTint = if (selectedSegment == 0) IncomeTint else expenseColor

    Card(
        modifier = modifier.fillMaxWidth(),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.24f)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            ChartHeader(
                eyebrow = "Cash flow",
                title = "Income vs expense",
                subtitle = "A fast snapshot of what this month is generating and consuming."
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Canvas(modifier = Modifier.width(146.dp).height(146.dp)) {
                        val stroke = Stroke(width = 28f, cap = StrokeCap.Round)
                        drawArc(
                            color = if (selectedSegment == 1) IncomeTint.copy(alpha = 0.35f) else IncomeTint,
                            startAngle = -90f,
                            sweepAngle = incomeSweep,
                            useCenter = false,
                            style = stroke,
                            size = Size(size.width, size.height)
                        )
                        drawArc(
                            color = if (selectedSegment == 0) expenseColor.copy(alpha = 0.35f) else expenseColor,
                            startAngle = -90f + incomeSweep,
                            sweepAngle = expenseSweep,
                            useCenter = false,
                            style = stroke,
                            size = Size(size.width, size.height)
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Net",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = CurrencyFormatter.format(income - expense, currency),
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.padding(horizontal = 24.dp)
                        )
                    }
                }
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    ChartLegendItem(
                        label = "Income",
                        value = CurrencyFormatter.format(income, currency),
                        color = IncomeTint,
                        selected = selectedSegment == 0,
                        onClick = { selectedSegment = 0 }
                    )
                    ChartLegendItem(
                        label = "Expenses",
                        value = CurrencyFormatter.format(expense, currency),
                        color = MaterialTheme.colorScheme.error,
                        selected = selectedSegment == 1,
                        onClick = { selectedSegment = 1 }
                    )
                }
            }
            Text(
                text = "$selectedLabel is ${CurrencyFormatter.format(selectedValue, currency)}. Net cash flow is ${CurrencyFormatter.format(income - expense, currency)}.",
                style = MaterialTheme.typography.bodySmall,
                color = selectedTint
            )
        }
    }
}

@Composable
fun CategoryBreakdownChart(
    categories: List<CategorySpend>,
    currency: CurrencyOption,
    modifier: Modifier = Modifier
) {
    val maxValue = categories.maxOfOrNull { it.amount } ?: 1.0
    var selectedIndex by remember(categories) { mutableIntStateOf(0) }
    val selectedItem = categories.getOrNull(selectedIndex) ?: categories.firstOrNull()
    Card(
        modifier = modifier.fillMaxWidth(),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.24f)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(20.dp)
        ) {
            ChartHeader(
                eyebrow = "Composition",
                title = "Category spread",
                subtitle = "Where most of your spending is concentrating this month."
            )
            categories.forEachIndexed { index, item ->
                val tint = CategoryVisuals.color(item.category)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedIndex = index },
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = item.category.displayName,
                            style = MaterialTheme.typography.bodyLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = if (selectedIndex == index) {
                                MaterialTheme.colorScheme.onSurface
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                        Text(
                            text = CurrencyFormatter.format(item.amount, currency),
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (selectedIndex == index) tint else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    LinearProgressIndicator(
                        progress = { (item.amount / maxValue).toFloat().coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth(),
                        color = tint,
                        trackColor = tint.copy(alpha = if (selectedIndex == index) 0.24f else 0.12f)
                    )
                }
            }
            selectedItem?.let { item ->
                Text(
                    text = "${item.category.displayName} is one of the bigger spend areas at ${CurrencyFormatter.format(item.amount, currency)} this month.",
                    style = MaterialTheme.typography.bodySmall,
                    color = CategoryVisuals.color(item.category)
                )
            }
        }
    }
}

@Composable
fun MonthlyTrendChart(
    trends: List<MonthlyTrend>,
    currency: CurrencyOption,
    modifier: Modifier = Modifier
) {
    val maxValue = trends.flatMap { listOf(it.income, it.expense) }.maxOrNull() ?: 1.0
    var selectedIndex by remember(trends) { mutableIntStateOf((trends.size - 1).coerceAtLeast(0)) }
    val selectedItem = trends.getOrNull(selectedIndex)
    Card(
        modifier = modifier.fillMaxWidth(),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.24f)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(20.dp)
        ) {
            ChartHeader(
                eyebrow = "Trend",
                title = "Monthly trend",
                subtitle = "A rolling view of how income and expenses are moving over time."
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.Bottom
            ) {
                trends.forEachIndexed { index, item ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Bottom,
                        modifier = Modifier
                            .fillMaxHeight()
                            .clickable { selectedIndex = index }
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.Bottom,
                            modifier = Modifier.weight(1f)
                        ) {
                            TrendBar(
                                fraction = (item.income / maxValue).toFloat(),
                                color = if (selectedIndex == index) IncomeTint else IncomeTint.copy(alpha = 0.42f)
                            )
                            TrendBar(
                                fraction = (item.expense / maxValue).toFloat(),
                                color = if (selectedIndex == index) {
                                    MaterialTheme.colorScheme.error
                                } else {
                                    MaterialTheme.colorScheme.error.copy(alpha = 0.42f)
                                }
                            )
                        }
                        Text(
                            text = DateUtils.formatMonthShort(item.month),
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (selectedIndex == index) {
                                MaterialTheme.colorScheme.onSurface
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            }
            selectedItem?.let {
                Text(
                    text = "${DateUtils.formatMonth(it.month)} shows ${CurrencyFormatter.format(it.income, currency)} in income and ${CurrencyFormatter.format(it.expense, currency)} in expenses.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun TrendBar(fraction: Float, color: Color) {
    Box(
        modifier = Modifier
            .width(18.dp)
            .fillMaxHeight(fraction.coerceIn(0.04f, 1f))
            .background(color = color, shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
    )
}

@Composable
private fun ChartLegendItem(
    label: String,
    value: String,
    color: Color,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier.clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .width(12.dp)
                .height(12.dp)
                .background(color = color, shape = CircleShape)
        )
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = if (selected) color else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun ChartHeader(
    eyebrow: String,
    title: String,
    subtitle: String
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = eyebrow.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
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
