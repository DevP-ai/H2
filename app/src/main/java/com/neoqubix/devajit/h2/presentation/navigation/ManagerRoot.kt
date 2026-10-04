package com.neoqubix.devajit.h2.presentation.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import com.neoqubix.devajit.h2.ui.theme.h2TopBarColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.neoqubix.devajit.h2.domain.model.Cart
import com.neoqubix.devajit.h2.domain.model.UserProfile
import com.neoqubix.devajit.h2.presentation.admin.reports.ReportsScreen
import com.neoqubix.devajit.h2.presentation.auth.BrandLogo
import com.neoqubix.devajit.h2.presentation.common.ProfileScreen
import com.neoqubix.devajit.h2.presentation.common.UiState
import com.neoqubix.devajit.h2.presentation.components.ErrorView
import com.neoqubix.devajit.h2.presentation.components.LoadingView
import com.neoqubix.devajit.h2.presentation.manager.AddTransactionScreen
import com.neoqubix.devajit.h2.presentation.manager.ManagerViewModel
import com.neoqubix.devajit.h2.presentation.manager.dashboard.ManagerHomeScreen
import com.neoqubix.devajit.h2.presentation.manager.transactions.TransactionHistoryList

private val managerTabs = listOf(
    TabItem("home", "Home", Icons.Outlined.Home),
    TabItem("transactions", "Transactions", Icons.AutoMirrored.Outlined.ReceiptLong),
    TabItem("reports", "Reports", Icons.Outlined.BarChart),
    TabItem("profile", "Profile", Icons.Outlined.Person)
)

// Everything a manager sees, always limited to the cart in their profile
@Composable
fun ManagerRoot(user: UserProfile, onLogout: () -> Unit, onCheckForUpdates: () -> Unit) {
    val viewModel: ManagerViewModel = hiltViewModel()
    LaunchedEffect(user) { viewModel.setUser(user) }
    val cartState by viewModel.cart.collectAsStateWithLifecycle()

    if (user.cartId == null) {
        NoCartScreen(
            title = "Your account has not been assigned to a food cart yet.",
            message = "Please contact the administrator.",
            onLogout = onLogout
        )
        return
    }
    when (val s = cartState) {
        is UiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { LoadingView(message = "Loading your cart…") }
        is UiState.Error -> Box(Modifier.fillMaxSize().systemBarsPadding(), contentAlignment = Alignment.Center) { ErrorView(s.message) }
        is UiState.Success -> {
            val cart = s.data
            if (cart == null) {
                NoCartScreen(
                    title = "Your assigned food cart could not be found.",
                    message = "Please contact the administrator.",
                    onLogout = onLogout
                )
            } else {
                ManagerShell(user, cart, viewModel, onLogout, onCheckForUpdates)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ManagerShell(user: UserProfile, cart: Cart, viewModel: ManagerViewModel, onLogout: () -> Unit, onCheckForUpdates: () -> Unit) {
    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val route = backStack?.destination?.route
    val onTab = managerTabs.any { it.route == route }
    val openRevenue = { navController.navigate("add_revenue") }
    val openExpense = { navController.navigate("add_expense") }

    // Insets come from the bars here; inner screens with their own Scaffold handle theirs
    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            if (onTab) TopAppBar(
                title = { Text(managerTabs.first { it.route == route }.label.let { if (it == "Home") cart.name else it }) },
                colors = h2TopBarColors()
            )
        },
        bottomBar = { if (onTab) BottomTabs(navController, managerTabs, route) },
        floatingActionButton = {
            if (route == "transactions" && cart.isActive) AddFab(openRevenue, openExpense)
        }
    ) { padding ->
        NavHost(navController, startDestination = "home", modifier = Modifier.padding(padding)) {
            composable("home") {
                ManagerHomeScreen(user, cart, viewModel, openRevenue, openExpense, onSeeAll = { navController.navigateToTab("transactions") })
            }
            composable("transactions") {
                val selection by viewModel.historySelection.collectAsStateWithLifecycle()
                val history by viewModel.history.collectAsStateWithLifecycle()
                val canLoadMore by viewModel.canLoadMore.collectAsStateWithLifecycle()
                TransactionHistoryList(selection, viewModel::setHistorySelection, history, canLoadMore, viewModel::loadMore)
            }
            composable("reports") { ReportsScreen(user) }
            composable("profile") { ProfileScreen(user, cart.name, onLogout, onCheckForUpdates) }
            composable("add_revenue") { AddTransactionScreen(user, cart, isExpense = false, onDone = { navController.popBackStack() }) }
            composable("add_expense") { AddTransactionScreen(user, cart, isExpense = true, onDone = { navController.popBackStack() }) }
        }
    }
}

@Composable
private fun AddFab(onAddRevenue: () -> Unit, onAddExpense: () -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        FloatingActionButton(onClick = { open = true }) { Icon(Icons.Filled.Add, contentDescription = "Add") }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            DropdownMenuItem(text = { Text("+ Add Revenue") }, onClick = { open = false; onAddRevenue() })
            DropdownMenuItem(text = { Text("- Add Expense") }, onClick = { open = false; onAddExpense() })
        }
    }
}

// A manager without a cart sees no cart data at all and can't pick one
@Composable
fun NoCartScreen(title: String, message: String, onLogout: () -> Unit) {
    Column(
        Modifier.fillMaxSize().systemBarsPadding().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        BrandLogo(Modifier.size(120.dp))
        Spacer(Modifier.height(24.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        Spacer(Modifier.height(24.dp))
        OutlinedButton(onClick = onLogout) { Text("Log out") }
    }
}
