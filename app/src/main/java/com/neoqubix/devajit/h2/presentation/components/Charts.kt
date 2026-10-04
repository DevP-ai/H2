package com.neoqubix.devajit.h2.presentation.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.neoqubix.devajit.h2.domain.model.CartPerformance
import com.neoqubix.devajit.h2.domain.model.ReportRow
import com.neoqubix.devajit.h2.ui.theme.ExpenseRed
import com.neoqubix.devajit.h2.ui.theme.LossRed
import com.neoqubix.devajit.h2.ui.theme.ProfitBlue
import com.neoqubix.devajit.h2.ui.theme.RevenueGreen
import com.neoqubix.devajit.h2.utils.formatRupees
import com.neoqubix.devajit.h2.utils.formatRupeesShort
import kotlin.math.abs
import kotlin.math.max

@Composable
private fun Legend(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(10.dp).background(color, CircleShape))
        Spacer(Modifier.width(4.dp))
        Text(label, style = MaterialTheme.typography.labelSmall)
    }
}

/*
 * Grouped bars: revenue and expenses (and profit when showProfit) for each point, oldest on the left.
 * Scrolls sideways when there are many points.
 */
@Composable
fun RevenueExpenseChart(rows: List<ReportRow>, modifier: Modifier = Modifier, showProfit: Boolean = false) {
    Card(modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
        Column(Modifier.padding(16.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Legend(RevenueGreen, "Revenue")
                Legend(ExpenseRed, "Expenses")
                if (showProfit) Legend(ProfitBlue, "Profit")
            }
            Spacer(Modifier.height(12.dp))
            if (rows.all { it.summary.revenue == 0.0 && it.summary.expenses == 0.0 }) {
                Text("No data for this period.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                return@Column
            }
            val maxValue = rows.maxOf { max(it.summary.revenue, it.summary.expenses) }.coerceAtLeast(1.0)
            Text("Max ${formatRupeesShort(maxValue)}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                rows.forEach { row ->
                    val bars = buildList {
                        add(row.summary.revenue to RevenueGreen)
                        add(row.summary.expenses to ExpenseRed)
                        if (showProfit) add(max(row.summary.profit, 0.0) to ProfitBlue)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(if (showProfit) 42.dp else 32.dp)) {
                        Canvas(Modifier.fillMaxWidth().height(140.dp)) {
                            val barWidth = size.width / bars.size - 2.dp.toPx()
                            bars.forEachIndexed { i, (value, color) ->
                                val h = (value / maxValue).toFloat() * size.height
                                drawRoundRect(
                                    color = color,
                                    topLeft = Offset(i * (barWidth + 2.dp.toPx()), size.height - h),
                                    size = Size(barWidth, h),
                                    cornerRadius = CornerRadius(3.dp.toPx())
                                )
                            }
                        }
                        Text(row.label, style = MaterialTheme.typography.labelSmall, maxLines = 2, textAlign = TextAlign.Center)
                    }
                }
            }
        }
    }
}

// Profit of each cart as a horizontal bar; losses in red
@Composable
fun CartPerformanceChart(items: List<CartPerformance>, modifier: Modifier = Modifier) {
    Card(modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (items.isEmpty()) {
                Text("No carts yet.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                return@Column
            }
            val maxAbs = items.maxOf { abs(it.summary.profit) }.coerceAtLeast(1.0)
            items.forEach { item ->
                val profit = item.summary.profit
                val color = if (profit < 0) LossRed else ProfitBlue
                Column {
                    Row(Modifier.fillMaxWidth()) {
                        Text(item.cart.name, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(
                            (if (profit < 0) "${formatRupees(abs(profit))} loss" else "${formatRupees(profit)} profit"),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = color
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Box(Modifier.fillMaxWidth().height(8.dp).background(color.copy(alpha = 0.12f), RoundedCornerShape(4.dp))) {
                        Box(
                            Modifier.fillMaxHeight()
                                .fillMaxWidth((abs(profit) / maxAbs).toFloat().coerceIn(0f, 1f))
                                .background(color, RoundedCornerShape(4.dp))
                        )
                    }
                }
            }
        }
    }
}
