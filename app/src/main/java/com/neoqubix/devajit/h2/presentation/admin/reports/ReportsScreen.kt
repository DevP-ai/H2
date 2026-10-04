package com.neoqubix.devajit.h2.presentation.admin.reports

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.neoqubix.devajit.h2.domain.model.ReportRow
import com.neoqubix.devajit.h2.domain.model.ReportType
import com.neoqubix.devajit.h2.domain.model.UserProfile
import com.neoqubix.devajit.h2.presentation.common.UiState
import com.neoqubix.devajit.h2.presentation.components.DateFilterBar
import com.neoqubix.devajit.h2.presentation.components.DropdownField
import com.neoqubix.devajit.h2.presentation.components.EmptyState
import com.neoqubix.devajit.h2.presentation.components.ErrorView
import com.neoqubix.devajit.h2.presentation.components.LoadingView
import com.neoqubix.devajit.h2.presentation.components.RevenueExpenseChart
import com.neoqubix.devajit.h2.presentation.components.SectionTitle
import com.neoqubix.devajit.h2.presentation.components.SummaryCards
import com.neoqubix.devajit.h2.ui.theme.ExpenseRed
import com.neoqubix.devajit.h2.ui.theme.LossRed
import com.neoqubix.devajit.h2.ui.theme.ProfitBlue
import com.neoqubix.devajit.h2.ui.theme.RevenueGreen
import com.neoqubix.devajit.h2.utils.formatRupees

private const val ALL_CARTS = "__all__"

@Composable
fun ReportsScreen(user: UserProfile, viewModel: ReportsViewModel = hiltViewModel()) {
    LaunchedEffect(user) { viewModel.setUser(user) }
    val selection by viewModel.selection.collectAsStateWithLifecycle()
    val type by viewModel.type.collectAsStateWithLifecycle()
    val cartFilter by viewModel.cartFilter.collectAsStateWithLifecycle()
    val carts by viewModel.carts.collectAsStateWithLifecycle()
    val report by viewModel.report.collectAsStateWithLifecycle()

    val types = if (user.isAdmin) ReportType.entries else ReportType.entries - ReportType.CART_WISE

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { DateFilterBar(selection, { viewModel.selection.value = it }) }
        if (user.isAdmin) {
            item {
                DropdownField(
                    label = "Cart",
                    options = listOf(ALL_CARTS) + carts.map { it.id },
                    selected = cartFilter ?: ALL_CARTS,
                    optionLabel = { id -> if (id == ALL_CARTS) "All carts" else carts.find { it.id == id }?.name ?: "Cart" },
                    onSelect = { viewModel.cartFilter.value = it.takeIf { id -> id != ALL_CARTS } },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
        item {
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                types.forEach { t ->
                    FilterChip(selected = type == t, onClick = { viewModel.type.value = t }, label = { Text(t.label) })
                }
            }
        }
        when (val s = report) {
            is UiState.Loading -> item { LoadingView() }
            is UiState.Error -> item { ErrorView(s.message) }
            is UiState.Success -> {
                val data = s.data
                item { SummaryCards(data.summary) }
                item { SectionTitle(if (data.trendIsMonthly) "Monthly performance" else "Revenue vs Expenses") }
                item { RevenueExpenseChart(data.trend, showProfit = data.trendIsMonthly) }
                if (type != ReportType.OVERALL) {
                    item { SectionTitle("${type.label} report") }
                    item {
                        if (data.rows.isEmpty()) EmptyState("No revenue recorded for this period.")
                        else ReportTable(data.rows, firstColumn = when (type) {
                            ReportType.DAILY -> "Date"
                            ReportType.WEEKLY -> "Week"
                            ReportType.MONTHLY -> "Month"
                            ReportType.YEARLY -> "Year"
                            else -> "Cart"
                        })
                    }
                }
            }
        }
    }
}

@Composable
private fun ReportTable(rows: List<ReportRow>, firstColumn: String) {
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
        Column(Modifier.padding(12.dp)) {
            TableRow(firstColumn, "Revenue", "Expenses", "Profit/Loss", header = true)
            rows.forEach { row ->
                HorizontalDivider()
                val profit = row.summary.profit
                TableRow(
                    row.label,
                    formatRupees(row.summary.revenue),
                    formatRupees(row.summary.expenses),
                    formatRupees(profit),
                    profitIsLoss = profit < 0
                )
            }
        }
    }
}

@Composable
private fun TableRow(
    label: String,
    revenue: String,
    expenses: String,
    profit: String,
    header: Boolean = false,
    profitIsLoss: Boolean = false
) {
    val style = if (header) MaterialTheme.typography.labelMedium else MaterialTheme.typography.bodySmall
    val weight = if (header) FontWeight.Bold else FontWeight.Normal
    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Text(label, style = style, fontWeight = if (header) FontWeight.Bold else FontWeight.Medium, modifier = Modifier.weight(1.3f))
        Text(revenue, style = style, fontWeight = weight, color = if (header) MaterialTheme.colorScheme.onSurface else RevenueGreen, textAlign = TextAlign.End, modifier = Modifier.weight(1f))
        Text(expenses, style = style, fontWeight = weight, color = if (header) MaterialTheme.colorScheme.onSurface else ExpenseRed, textAlign = TextAlign.End, modifier = Modifier.weight(1f))
        Text(
            profit,
            style = style,
            fontWeight = if (header) FontWeight.Bold else FontWeight.SemiBold,
            color = when {
                header -> MaterialTheme.colorScheme.onSurface
                profitIsLoss -> LossRed
                else -> ProfitBlue
            },
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1.1f)
        )
    }
}
