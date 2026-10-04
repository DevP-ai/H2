package com.neoqubix.devajit.h2.presentation.admin.carts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.neoqubix.devajit.h2.presentation.admin.AdminViewModel
import com.neoqubix.devajit.h2.presentation.common.UiState
import com.neoqubix.devajit.h2.presentation.components.CartCard
import com.neoqubix.devajit.h2.presentation.components.DateFilterBar
import com.neoqubix.devajit.h2.presentation.components.EmptyState
import com.neoqubix.devajit.h2.presentation.components.ErrorView
import com.neoqubix.devajit.h2.presentation.components.LoadingView

@Composable
fun CartsScreen(viewModel: AdminViewModel, onOpenCart: (String) -> Unit, onAddCart: () -> Unit) {
    val selection by viewModel.selection.collectAsStateWithLifecycle()
    val state by viewModel.overview.collectAsStateWithLifecycle()

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 88.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { DateFilterBar(selection, { viewModel.selection.value = it }) }
        when (val s = state) {
            is UiState.Loading -> item { LoadingView() }
            is UiState.Error -> item { ErrorView(s.message) }
            is UiState.Success -> {
                val data = s.data
                if (data.carts.isEmpty()) {
                    item {
                        EmptyState(
                            "No food carts available.",
                            message = "Create your first cart to get started.",
                            action = { Button(onClick = onAddCart) { Text("Add cart") } }
                        )
                    }
                }
                val summaries = data.performance.associateBy { it.cart.id }
                items(data.carts, key = { it.id }) { cart ->
                    CartCard(
                        cart = cart,
                        managerName = data.managerName(cart.managerId),
                        summary = summaries[cart.id]?.summary,
                        onClick = { onOpenCart(cart.id) }
                    )
                }
            }
        }
    }
}
