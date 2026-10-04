package com.neoqubix.devajit.h2.presentation.admin.managers

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.neoqubix.devajit.h2.domain.model.Cart
import com.neoqubix.devajit.h2.domain.model.UserProfile
import com.neoqubix.devajit.h2.presentation.admin.AdminOverview
import com.neoqubix.devajit.h2.presentation.admin.AdminViewModel
import com.neoqubix.devajit.h2.presentation.common.UiState
import com.neoqubix.devajit.h2.presentation.components.DropdownField
import com.neoqubix.devajit.h2.presentation.components.EmptyState
import com.neoqubix.devajit.h2.presentation.components.ErrorView
import com.neoqubix.devajit.h2.presentation.components.LoadingView
import com.neoqubix.devajit.h2.presentation.components.ManagerCard
import com.neoqubix.devajit.h2.utils.formatDate

private const val NO_CART = "__none__"

@Composable
fun ManagersScreen(viewModel: AdminViewModel) {
    val state by viewModel.overview.collectAsStateWithLifecycle()
    var selected by remember { mutableStateOf<UserProfile?>(null) }

    when (val s = state) {
        is UiState.Loading -> LoadingView()
        is UiState.Error -> ErrorView(s.message)
        is UiState.Success -> {
            val data = s.data
            if (data.managers.isEmpty()) {
                EmptyState(
                    "No managers yet.",
                    icon = Icons.Outlined.Group,
                    message = "People who register in the app appear here as managers. Then assign each one to a cart."
                )
            } else {
                LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    item {
                        Text(
                            "${data.managers.size} managers · ${data.managers.count { it.cartId == null }} without a cart",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    items(data.managers, key = { it.id }) { m ->
                        ManagerCard(m, data.cartName(m.cartId), onClick = { selected = m })
                    }
                }
            }
            selected?.let { manager ->
                // Always show the latest version of the manager
                val current = data.managers.find { it.id == manager.id } ?: manager
                AssignCartDialog(current, data, viewModel, onDismiss = { selected = null })
            }
        }
    }
}

// Manager details and cart assignment
@Composable
private fun AssignCartDialog(manager: UserProfile, data: AdminOverview, viewModel: AdminViewModel, onDismiss: () -> Unit) {
    val assigning by viewModel.assigning.collectAsStateWithLifecycle()
    var cartId by remember(manager.id) { mutableStateOf(manager.cartId) }
    val target: Cart? = data.carts.find { it.id == cartId }
    val replaced = target?.managerId?.takeIf { it != manager.id }?.let { data.managerName(it) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(manager.name.ifBlank { "Manager" }) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(manager.email, style = MaterialTheme.typography.bodyMedium)
                if (manager.phone.isNotBlank()) Text("Phone: ${manager.phone}", style = MaterialTheme.typography.bodyMedium)
                manager.createdAt?.let { Text("Joined: ${formatDate(it)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                DropdownField(
                    label = "Assigned cart",
                    options = listOf(NO_CART) + data.carts.map { it.id },
                    selected = cartId ?: NO_CART,
                    optionLabel = { id ->
                        if (id == NO_CART) "Not assigned"
                        else data.carts.find { it.id == id }?.let { c -> c.name + if (!c.isActive) " (inactive)" else "" } ?: "Cart"
                    },
                    onSelect = { cartId = it.takeIf { id -> id != NO_CART } },
                    modifier = Modifier.fillMaxWidth(),
                    supportingText = replaced?.let { "$it will be removed from ${target.name}." }
                )
                if (data.carts.isEmpty()) {
                    Text("Create a cart first in the Carts tab.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { viewModel.assign(manager, cartId, onDismiss) },
                enabled = !assigning && cartId != manager.cartId
            ) { Text(if (assigning) "Saving…" else "Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Close") } }
    )
}
