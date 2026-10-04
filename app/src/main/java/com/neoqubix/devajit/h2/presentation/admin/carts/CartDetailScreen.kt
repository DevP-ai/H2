package com.neoqubix.devajit.h2.presentation.admin.carts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.neoqubix.devajit.h2.domain.model.Cart
import com.neoqubix.devajit.h2.domain.model.DateFilter
import com.neoqubix.devajit.h2.domain.model.UserProfile
import com.neoqubix.devajit.h2.presentation.common.UiState
import com.neoqubix.devajit.h2.presentation.components.EmptyState
import com.neoqubix.devajit.h2.presentation.components.LoadingView
import com.neoqubix.devajit.h2.presentation.components.SectionTitle
import com.neoqubix.devajit.h2.presentation.components.StatusChip
import com.neoqubix.devajit.h2.presentation.components.SummaryCards
import com.neoqubix.devajit.h2.presentation.manager.transactions.TransactionHistoryList

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CartDetailScreen(
    user: UserProfile,
    onBack: () -> Unit,
    onEdit: (String) -> Unit,
    viewModel: CartDetailViewModel = hiltViewModel()
) {
    LaunchedEffect(user) { viewModel.setUser(user) }
    val cartState by viewModel.cart.collectAsStateWithLifecycle()
    val managerName by viewModel.managerName.collectAsStateWithLifecycle()
    val selection by viewModel.selection.collectAsStateWithLifecycle()
    val summary by viewModel.summary.collectAsStateWithLifecycle()
    val transactions by viewModel.transactions.collectAsStateWithLifecycle()
    val canLoadMore by viewModel.canLoadMore.collectAsStateWithLifecycle()
    val cart = (cartState as? UiState.Success)?.data

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(cart?.name ?: "Cart") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") } },
                actions = {
                    if (cart != null) IconButton(onClick = { onEdit(cart.id) }) { Icon(Icons.Outlined.Edit, contentDescription = "Edit cart") }
                }
            )
        }
    ) { padding ->
        when {
            cartState is UiState.Loading -> LoadingView(Modifier.padding(padding))
            cart == null -> EmptyState("Cart not found.", Modifier.padding(padding), message = "It may have been removed.")
            else -> TransactionHistoryList(
                selection = selection,
                onSelectionChange = viewModel::setSelection,
                state = transactions,
                canLoadMore = canLoadMore,
                onLoadMore = viewModel::loadMore,
                modifier = Modifier.padding(padding),
                header = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(bottom = 12.dp)) {
                        CartInfo(cart, managerName)
                        when (val s = summary) {
                            is UiState.Success -> SummaryCards(s.data, periodLabel = if (selection.filter == DateFilter.TODAY) "Today" else "")
                            is UiState.Loading -> LoadingView()
                            is UiState.Error -> Text(s.message, color = MaterialTheme.colorScheme.error)
                        }
                        SectionTitle("Transactions")
                    }
                }
            )
        }
    }
}

@Composable
private fun CartInfo(cart: Cart, managerName: String?) {
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(cart.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                StatusChip(cart.isActive)
            }
            Text("Location: ${cart.location.ifBlank { "—" }}", style = MaterialTheme.typography.bodyMedium)
            Text(
                "Manager: ${managerName ?: "Not assigned"}",
                style = MaterialTheme.typography.bodyMedium,
                color = if (managerName == null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
