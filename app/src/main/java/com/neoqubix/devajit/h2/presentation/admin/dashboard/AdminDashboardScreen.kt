package com.neoqubix.devajit.h2.presentation.admin.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.neoqubix.devajit.h2.domain.model.Transaction
import com.neoqubix.devajit.h2.domain.model.UserProfile
import com.neoqubix.devajit.h2.presentation.admin.AdminViewModel
import com.neoqubix.devajit.h2.presentation.common.UiState
import com.neoqubix.devajit.h2.presentation.components.CartPerformanceChart
import com.neoqubix.devajit.h2.presentation.components.DateFilterBar
import com.neoqubix.devajit.h2.presentation.components.ErrorView
import com.neoqubix.devajit.h2.presentation.components.LoadingView
import com.neoqubix.devajit.h2.presentation.components.RevenueExpenseChart
import com.neoqubix.devajit.h2.presentation.components.SectionTitle
import com.neoqubix.devajit.h2.presentation.components.SummaryCards
import com.neoqubix.devajit.h2.presentation.components.TransactionItem
import com.neoqubix.devajit.h2.utils.greetingFor

@Composable
fun AdminDashboardScreen(
    user: UserProfile,
    viewModel: AdminViewModel,
    onOpenCarts: () -> Unit,
    onOpenManagers: () -> Unit,
    onOpenTransaction: (Transaction) -> Unit
) {
    val selection by viewModel.selection.collectAsStateWithLifecycle()
    val state by viewModel.overview.collectAsStateWithLifecycle()

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            Column {
                Text("${greetingFor()}, ${user.name.substringBefore(' ').ifBlank { "Admin" }}", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text("Whole business overview", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        item { DateFilterBar(selection, { viewModel.selection.value = it }) }
        when (val s = state) {
            is UiState.Loading -> item { LoadingView() }
            is UiState.Error -> item { ErrorView(s.message) }
            is UiState.Success -> {
                val data = s.data
                item { SummaryCards(data.summary) }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        CountCard("Total Carts", data.carts.size, Modifier.weight(1f), onOpenCarts)
                        CountCard("Active Carts", data.activeCarts, Modifier.weight(1f), onOpenCarts)
                        CountCard("Managers", data.managers.size, Modifier.weight(1f), onOpenManagers)
                    }
                }
                item { SectionTitle(if (data.trendIsMonthly) "Monthly performance" else "Revenue vs Expenses") }
                item { RevenueExpenseChart(data.trend, showProfit = data.trendIsMonthly) }
                item { SectionTitle("Cart performance") { TextButton(onClick = onOpenCarts) { Text("All carts") } } }
                item { CartPerformanceChart(data.performance) }
                if (data.transactions.isNotEmpty()) {
                    item { SectionTitle("Latest transactions") }
                    item {
                        Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                            Column(Modifier.padding(horizontal = 16.dp)) {
                                data.transactions.take(5).forEachIndexed { i, t ->
                                    if (i > 0) HorizontalDivider()
                                    TransactionItem(t, cartName = data.cartName(t.cartId) ?: "Unknown cart", onClick = { onOpenTransaction(t) })
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CountCard(label: String, count: Int, modifier: Modifier, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = modifier, shape = RoundedCornerShape(16.dp)) {
        Column(Modifier.fillMaxWidth().padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(count.toString(), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
