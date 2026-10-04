package com.neoqubix.devajit.h2.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.neoqubix.devajit.h2.domain.model.CategoryTotal
import com.neoqubix.devajit.h2.domain.model.FinancialSummary
import com.neoqubix.devajit.h2.ui.theme.ExpenseRed
import com.neoqubix.devajit.h2.ui.theme.LossRed
import com.neoqubix.devajit.h2.ui.theme.ProfitBlue
import com.neoqubix.devajit.h2.ui.theme.RevenueGreen
import com.neoqubix.devajit.h2.utils.formatRupees

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier, trailing: (@Composable () -> Unit)? = null) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
        trailing?.invoke()
    }
}

@Composable
fun FinancialSummaryCard(
    title: String,
    amount: Double,
    color: Color,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.08f))
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Spacer(Modifier.height(8.dp))
            Text(formatRupees(amount), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = color, maxLines = 1)
        }
    }
}

// "Profit" in blue or "Loss" in red, never typed in by anyone
@Composable
fun ProfitLossCard(summary: FinancialSummary, modifier: Modifier = Modifier, label: String = "") {
    val color = if (summary.isLoss) LossRed else ProfitBlue
    FinancialSummaryCard(
        title = (if (label.isBlank()) "" else "$label ") + if (summary.isLoss) "Loss" else "Profit",
        amount = kotlin.math.abs(summary.profit),
        color = color,
        icon = if (summary.isLoss) Icons.AutoMirrored.Filled.TrendingDown else Icons.Filled.AccountBalanceWallet,
        modifier = modifier
    )
}

// Revenue, expenses and profit/loss for a period
@Composable
fun SummaryCards(summary: FinancialSummary, modifier: Modifier = Modifier, periodLabel: String = "") {
    val prefix = if (periodLabel.isBlank()) "" else "$periodLabel's "
    Column(modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            FinancialSummaryCard("${prefix}Revenue", summary.revenue, RevenueGreen, Icons.AutoMirrored.Filled.TrendingUp, Modifier.weight(1f))
            FinancialSummaryCard("${prefix}Expenses", summary.expenses, ExpenseRed, Icons.AutoMirrored.Filled.TrendingDown, Modifier.weight(1f))
        }
        ProfitLossCard(summary, Modifier.fillMaxWidth(), label = periodLabel.let { if (it.isBlank()) "" else "$it's" })
    }
}

// Revenue / Expenses / Profit on one line, for list items
@Composable
fun SummaryLine(summary: FinancialSummary, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        LabeledAmount("Revenue", summary.revenue, RevenueGreen)
        LabeledAmount("Expenses", summary.expenses, ExpenseRed)
        LabeledAmount(if (summary.isLoss) "Loss" else "Profit", kotlin.math.abs(summary.profit), if (summary.isLoss) LossRed else ProfitBlue)
    }
}

@Composable
private fun LabeledAmount(label: String, amount: Double, color: Color) {
    Column {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(formatRupees(amount), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = color)
    }
}

@Composable
fun ExpenseBreakdown(items: List<CategoryTotal>, modifier: Modifier = Modifier) {
    val total = items.sumOf { it.amount }.takeIf { it > 0 } ?: return
    Card(modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items.forEach { item ->
                Column {
                    Row(Modifier.fillMaxWidth()) {
                        Text(item.category, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                        Text(formatRupees(item.amount), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    }
                    Spacer(Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { (item.amount / total).toFloat() },
                        modifier = Modifier.fillMaxWidth().height(6.dp),
                        color = ExpenseRed,
                        trackColor = ExpenseRed.copy(alpha = 0.12f),
                        drawStopIndicator = {}
                    )
                }
            }
        }
    }
}
