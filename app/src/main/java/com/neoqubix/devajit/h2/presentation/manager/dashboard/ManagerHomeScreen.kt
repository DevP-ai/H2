package com.neoqubix.devajit.h2.presentation.manager.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import com.neoqubix.devajit.h2.domain.model.Cart
import com.neoqubix.devajit.h2.domain.model.DateFilter
import com.neoqubix.devajit.h2.domain.model.FinancialSummary
import com.neoqubix.devajit.h2.domain.model.UserProfile
import com.neoqubix.devajit.h2.domain.usecase.ReportCalculator
import com.neoqubix.devajit.h2.presentation.common.UiState
import com.neoqubix.devajit.h2.presentation.components.DateFilterBar
import com.neoqubix.devajit.h2.presentation.components.EmptyState
import com.neoqubix.devajit.h2.presentation.components.ErrorView
import com.neoqubix.devajit.h2.presentation.components.ExpenseBreakdown
import com.neoqubix.devajit.h2.presentation.components.LoadingView
import com.neoqubix.devajit.h2.presentation.components.SectionTitle
import com.neoqubix.devajit.h2.presentation.components.SummaryCards
import com.neoqubix.devajit.h2.presentation.components.TransactionItem
import com.neoqubix.devajit.h2.presentation.manager.ManagerViewModel
import com.neoqubix.devajit.h2.ui.theme.ExpenseRed
import com.neoqubix.devajit.h2.ui.theme.RevenueGreen
import com.neoqubix.devajit.h2.utils.greetingFor

@Composable
fun ManagerHomeScreen(
    user: UserProfile,
    cart: Cart,
    viewModel: ManagerViewModel,
    onAddRevenue: () -> Unit,
    onAddExpense: () -> Unit,
    onSeeAll: () -> Unit
) {
    val selection by viewModel.homeSelection.collectAsStateWithLifecycle()
    val state by viewModel.homeTransactions.collectAsStateWithLifecycle()

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Column {
                Text("${greetingFor()}, ${user.name.substringBefore(' ').ifBlank { "there" }}", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                CartHeader(cart)
            }
        }
        item {
            DateFilterBar(selection, { viewModel.homeSelection.value = it })
        }
        when (val s = state) {
            is UiState.Loading -> item { LoadingView() }
            is UiState.Error -> item { ErrorView(s.message) }
            is UiState.Success -> {
                val transactions = s.data
                item {
                    SummaryCards(
                        FinancialSummary.of(transactions),
                        periodLabel = if (selection.filter == DateFilter.TODAY) "Today" else ""
                    )
                }
                item { AddButtons(cart.isActive, onAddRevenue, onAddExpense) }
                val breakdown = ReportCalculator.expenseBreakdown(transactions)
                if (breakdown.isNotEmpty()) {
                    item { SectionTitle("Expense breakdown") }
                    item { ExpenseBreakdown(breakdown) }
                }
                item {
                    SectionTitle("Recent transactions") {
                        if (transactions.isNotEmpty()) TextButton(onClick = onSeeAll) { Text("See all") }
                    }
                }
                if (transactions.isEmpty()) {
                    item {
                        EmptyState(
                            "No transactions for this period.",
                            message = "Start adding revenue and expenses to track your cart's performance."
                        )
                    }
                } else {
                    item {
                        Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                            Column(Modifier.padding(horizontal = 16.dp)) {
                                transactions.take(5).forEachIndexed { i, t ->
                                    if (i > 0) HorizontalDivider()
                                    TransactionItem(t)
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
private fun CartHeader(cart: Cart) {
    Card(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(cart.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary)
            if (cart.location.isNotBlank()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Place, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(16.dp))
                    Text(" ${cart.location}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onPrimary)
                }
            }
            if (!cart.isActive) {
                Spacer(Modifier.height(6.dp))
                Text(
                    "This cart is inactive. New revenue and expenses can't be added.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        }
    }
}

@Composable
fun AddButtons(enabled: Boolean, onAddRevenue: () -> Unit, onAddExpense: () -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Button(
            onClick = onAddRevenue,
            enabled = enabled,
            modifier = Modifier.weight(1f).height(52.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = RevenueGreen)
        ) {
            Icon(Icons.Filled.Add, contentDescription = null)
            Text(" Add Revenue")
        }
        Button(
            onClick = onAddExpense,
            enabled = enabled,
            modifier = Modifier.weight(1f).height(52.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = ExpenseRed)
        ) {
            Icon(Icons.Filled.Remove, contentDescription = null)
            Text(" Add Expense")
        }
    }
}
