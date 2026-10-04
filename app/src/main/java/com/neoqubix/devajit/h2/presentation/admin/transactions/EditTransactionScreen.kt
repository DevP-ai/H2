package com.neoqubix.devajit.h2.presentation.admin.transactions

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.neoqubix.devajit.h2.domain.model.UserProfile
import com.neoqubix.devajit.h2.presentation.components.DateField
import com.neoqubix.devajit.h2.presentation.components.EmptyState
import com.neoqubix.devajit.h2.presentation.components.ExpenseCategorySelector
import com.neoqubix.devajit.h2.presentation.components.LoadingView
import com.neoqubix.devajit.h2.ui.theme.ExpenseRed
import com.neoqubix.devajit.h2.ui.theme.RevenueGreen
import com.neoqubix.devajit.h2.ui.theme.h2TopBarColors
import com.neoqubix.devajit.h2.utils.formatDate
import com.neoqubix.devajit.h2.utils.formatTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditTransactionScreen(user: UserProfile, onDone: () -> Unit, viewModel: EditTransactionViewModel = hiltViewModel()) {
    val form by viewModel.form.collectAsStateWithLifecycle()
    val people by viewModel.people.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val original = form.original

    LaunchedEffect(form.savedMessage) {
        form.savedMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            onDone()
        }
    }

    fun nameOf(uid: String?): String = when (uid) {
        null -> "Unknown"
        user.id -> "You"
        else -> people.find { it.id == uid }?.name?.ifBlank { null } ?: "Admin"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                colors = h2TopBarColors(),
                title = { Text(if (original?.isRevenue == false) "Edit Expense" else "Edit Revenue") },
                navigationIcon = { IconButton(onClick = onDone) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") } }
            )
        }
    ) { padding ->
        when {
            form.loading -> LoadingView(Modifier.padding(padding))
            original == null -> EmptyState(form.error ?: "Record not found.", Modifier.padding(padding))
            else -> Column(
                Modifier.fillMaxSize().padding(padding).imePadding().verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            if (original.isRevenue) "Revenue" else "Expense",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = if (original.isRevenue) RevenueGreen else ExpenseRed
                        )
                        Text("Cart: ${form.cart?.name ?: "Unknown cart"}", style = MaterialTheme.typography.bodyMedium)
                        val created = original.createdAt ?: original.date
                        Text(
                            "Added by ${nameOf(original.createdBy)} · ${formatDate(created)} ${formatTime(created)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (original.isEdited) {
                            val edited = original.updatedAt ?: created
                            Text(
                                "Last edited by ${nameOf(original.updatedBy)} · ${formatDate(edited)} ${formatTime(edited)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                if (!original.isRevenue) {
                    ExpenseCategorySelector(form.category, viewModel::onCategory, Modifier.fillMaxWidth())
                }
                OutlinedTextField(
                    value = form.amount,
                    onValueChange = viewModel::onAmount,
                    label = { Text("Amount") },
                    prefix = { Text("₹ ") },
                    singleLine = true,
                    isError = form.amountError != null,
                    supportingText = form.amountError?.let { { Text(it) } },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
                DateField("Date", form.date, viewModel::onDate, Modifier.fillMaxWidth())
                OutlinedTextField(
                    value = form.description,
                    onValueChange = viewModel::onDescription,
                    label = { Text("Description") },
                    minLines = 2,
                    maxLines = 4,
                    supportingText = { Text("${form.description.length}/200") },
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    "The record stays in its cart. Your name is saved as the last editor.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                form.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                Button(
                    onClick = { viewModel.save(user) },
                    enabled = !form.saving && viewModel.hasChanges,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(12.dp)
                ) { Text(if (form.saving) "Saving…" else "Save changes") }
            }
        }
    }
}
