package com.neoqubix.devajit.h2.presentation.manager

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import com.neoqubix.devajit.h2.ui.theme.h2TopBarColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.neoqubix.devajit.h2.domain.model.Cart
import com.neoqubix.devajit.h2.domain.model.UserProfile
import com.neoqubix.devajit.h2.presentation.components.DateField
import com.neoqubix.devajit.h2.presentation.components.ExpenseCategorySelector
import com.neoqubix.devajit.h2.ui.theme.ExpenseRed
import com.neoqubix.devajit.h2.ui.theme.RevenueGreen

// Add Revenue (isExpense = false) or Add Expense for one cart: a manager's own cart, or the cart an admin opened.
// There is no cart field, so a manager can't pick another cart.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionScreen(
    user: UserProfile,
    cart: Cart,
    isExpense: Boolean,
    onDone: () -> Unit,
    viewModel: AddTransactionViewModel = hiltViewModel()
) {
    val form by viewModel.form.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val color = if (isExpense) ExpenseRed else RevenueGreen

    LaunchedEffect(form.savedMessage) {
        form.savedMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            onDone()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                colors = h2TopBarColors(),
                title = { Text(if (isExpense) "Add Expense" else "Add Revenue") },
                navigationIcon = {
                    IconButton(onClick = onDone) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
                }
            )
        }
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).imePadding().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text("For ${cart.name}", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (isExpense) {
                ExpenseCategorySelector(form.category, viewModel::onCategory, Modifier.fillMaxWidth(), isError = form.categoryError)
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
                label = { Text(if (isExpense) "Description (optional)" else "Description (e.g. Daily sales)") },
                minLines = 2,
                maxLines = 4,
                supportingText = { Text("${form.description.length}/200") },
                modifier = Modifier.fillMaxWidth()
            )
            form.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            Button(
                onClick = { viewModel.save(user, cart.id, isExpense) },
                enabled = !form.saving && cart.isActive,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = color)
            ) {
                if (form.saving) CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                else Text(if (isExpense) "Save Expense" else "Save Revenue")
            }
            if (!cart.isActive) {
                Text("This cart is inactive, so nothing can be added.", color = MaterialTheme.colorScheme.error)
            }
        }
    }
}
