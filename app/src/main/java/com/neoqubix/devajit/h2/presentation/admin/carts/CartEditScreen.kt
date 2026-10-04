package com.neoqubix.devajit.h2.presentation.admin.carts

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.neoqubix.devajit.h2.domain.model.CartStatus
import com.neoqubix.devajit.h2.presentation.components.DropdownField
import com.neoqubix.devajit.h2.presentation.components.LoadingView

private const val NO_MANAGER = "__none__"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CartEditScreen(onDone: () -> Unit, viewModel: CartEditViewModel = hiltViewModel()) {
    val form by viewModel.form.collectAsStateWithLifecycle()
    val managers by viewModel.managers.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(form.saved) {
        if (form.saved) {
            Toast.makeText(context, if (form.existing == null) "Cart created." else "Cart saved.", Toast.LENGTH_SHORT).show()
            onDone()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (form.existing == null) "Add Cart" else "Edit Cart") },
                navigationIcon = { IconButton(onClick = onDone) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") } }
            )
        }
    ) { padding ->
        if (form.loading) {
            LoadingView(Modifier.padding(padding))
            return@Scaffold
        }
        Column(
            Modifier.fillMaxSize().padding(padding).imePadding().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            OutlinedTextField(
                value = form.name,
                onValueChange = viewModel::onName,
                label = { Text("Cart name") },
                singleLine = true,
                isError = form.nameError,
                supportingText = if (form.nameError) ({ Text("Please enter the cart name") }) else null,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = form.location,
                onValueChange = viewModel::onLocation,
                label = { Text("Location") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            val currentHolder = managers.find { it.id == form.managerId }
            DropdownField(
                label = "Manager",
                options = listOf(NO_MANAGER) + managers.map { it.id },
                selected = form.managerId ?: NO_MANAGER,
                optionLabel = { id ->
                    if (id == NO_MANAGER) "No manager"
                    else managers.find { it.id == id }?.let { m ->
                        val elsewhere = m.cartId != null && m.cartId != form.existing?.id
                        m.name.ifBlank { m.email } + if (elsewhere) " (assigned to another cart)" else ""
                    } ?: "Manager"
                },
                onSelect = { viewModel.onManager(it.takeIf { id -> id != NO_MANAGER }) },
                supportingText = when {
                    managers.isEmpty() -> "No managers yet. Managers appear here after they register."
                    currentHolder != null && currentHolder.cartId != null && currentHolder.cartId != form.existing?.id ->
                        "${currentHolder.name} will be moved from their current cart to this one."
                    else -> null
                },
                modifier = Modifier.fillMaxWidth()
            )
            Text("Status", style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CartStatus.entries.forEach { s ->
                    FilterChip(selected = form.status == s, onClick = { viewModel.onStatus(s) }, label = { Text(s.label) })
                }
            }
            if (form.status == CartStatus.INACTIVE) {
                Text("Managers can't add revenue or expenses to an inactive cart.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            form.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            Button(
                onClick = viewModel::save,
                enabled = !form.saving,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(12.dp)
            ) { Text(if (form.saving) "Saving…" else if (form.existing == null) "Create cart" else "Save changes") }
        }
    }
}
