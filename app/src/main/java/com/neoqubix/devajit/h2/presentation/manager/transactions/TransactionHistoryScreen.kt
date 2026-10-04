package com.neoqubix.devajit.h2.presentation.manager.transactions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.neoqubix.devajit.h2.domain.model.DateSelection
import com.neoqubix.devajit.h2.domain.model.Transaction
import com.neoqubix.devajit.h2.presentation.common.UiState
import com.neoqubix.devajit.h2.presentation.components.DateFilterBar
import com.neoqubix.devajit.h2.presentation.components.EmptyState
import com.neoqubix.devajit.h2.presentation.components.ErrorView
import com.neoqubix.devajit.h2.presentation.components.LoadingView
import com.neoqubix.devajit.h2.presentation.components.TransactionItem
import com.neoqubix.devajit.h2.utils.formatDate
import com.neoqubix.devajit.h2.utils.toLocalDate

// Revenue and expenses grouped by day, newest first, loaded a page at a time
@Composable
fun TransactionHistoryList(
    selection: DateSelection,
    onSelectionChange: (DateSelection) -> Unit,
    state: UiState<List<Transaction>>,
    canLoadMore: Boolean,
    onLoadMore: () -> Unit,
    modifier: Modifier = Modifier,
    cartNameOf: ((String) -> String?)? = null,
    header: (@Composable () -> Unit)? = null
) {
    LazyColumn(
        modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 88.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        if (header != null) item { header() }
        item { DateFilterBar(selection, onSelectionChange, Modifier.padding(bottom = 8.dp)) }
        when (state) {
            is UiState.Loading -> item { LoadingView() }
            is UiState.Error -> item { ErrorView(state.message) }
            is UiState.Success -> {
                if (state.data.isEmpty()) {
                    item {
                        EmptyState(
                            "No transactions found.",
                            message = "Revenue and expenses added for this period will appear here."
                        )
                    }
                }
                state.data.groupBy { it.date.toLocalDate() }.forEach { (day, items) ->
                    item(key = "day-$day") {
                        Text(
                            formatDate(day),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = 12.dp, bottom = 2.dp)
                        )
                    }
                    items(items, key = { "${it.type}-${it.id}" }) { t ->
                        Column {
                            TransactionItem(t, cartName = cartNameOf?.invoke(t.cartId))
                            HorizontalDivider()
                        }
                    }
                }
                if (canLoadMore) {
                    item {
                        Box(Modifier.fillMaxWidth().padding(12.dp), contentAlignment = Alignment.Center) {
                            OutlinedButton(onClick = onLoadMore) { Text("Load more") }
                        }
                    }
                }
            }
        }
    }
}
