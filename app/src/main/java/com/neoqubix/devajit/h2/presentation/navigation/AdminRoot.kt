package com.neoqubix.devajit.h2.presentation.navigation

import android.widget.Toast
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import com.neoqubix.devajit.h2.ui.theme.h2TopBarColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.neoqubix.devajit.h2.domain.model.Transaction
import com.neoqubix.devajit.h2.domain.model.UserProfile
import com.neoqubix.devajit.h2.presentation.admin.transactions.AdminAddTransactionRoute
import com.neoqubix.devajit.h2.presentation.admin.transactions.EditTransactionScreen
import com.neoqubix.devajit.h2.presentation.admin.AdminViewModel
import com.neoqubix.devajit.h2.presentation.admin.carts.CartDetailScreen
import com.neoqubix.devajit.h2.presentation.admin.carts.CartEditScreen
import com.neoqubix.devajit.h2.presentation.admin.carts.CartEditViewModel
import com.neoqubix.devajit.h2.presentation.admin.carts.CartsScreen
import com.neoqubix.devajit.h2.presentation.admin.dashboard.AdminDashboardScreen
import com.neoqubix.devajit.h2.presentation.admin.managers.ManagersScreen
import com.neoqubix.devajit.h2.presentation.admin.reports.ReportsScreen
import com.neoqubix.devajit.h2.presentation.common.ProfileScreen

private val adminTabs = listOf(
    TabItem("dashboard", "Dashboard", Icons.Outlined.Dashboard),
    TabItem("carts", "Carts", Icons.Outlined.Storefront),
    TabItem("managers", "Managers", Icons.Outlined.Group),
    TabItem("reports", "Reports", Icons.Outlined.BarChart),
    TabItem("profile", "Profile", Icons.Outlined.Person)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminRoot(user: UserProfile, onLogout: () -> Unit, onCheckForUpdates: () -> Unit) {
    val viewModel: AdminViewModel = hiltViewModel()
    LaunchedEffect(user) { viewModel.setUser(user) }
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        viewModel.messages.collect { Toast.makeText(context, it, Toast.LENGTH_SHORT).show() }
    }

    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val route = backStack?.destination?.route
    val onTab = adminTabs.any { it.route == route }
    val openCart: (String) -> Unit = { navController.navigate("cart/$it") }
    val addCart = { navController.navigate("cart_edit/${CartEditViewModel.NEW}") }
    val openTab: (String) -> Unit = { tab -> navController.navigateToTab(tab) }
    val openTransaction: (Transaction) -> Unit = { t -> navController.navigate("transaction/${t.type.name}/${t.id}") }

    // Insets come from the bars here; inner screens with their own Scaffold handle theirs
    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = { if (onTab) TopAppBar(title = { Text(adminTabs.first { it.route == route }.label) }, colors = h2TopBarColors()) },
        bottomBar = { if (onTab) BottomTabs(navController, adminTabs, route) },
        floatingActionButton = {
            if (route == "carts") {
                ExtendedFloatingActionButton(onClick = addCart, icon = { Icon(Icons.Filled.Add, contentDescription = null) }, text = { Text("Add cart") })
            }
        }
    ) { padding ->
        NavHost(navController, startDestination = "dashboard", modifier = Modifier.padding(padding)) {
            composable("dashboard") {
                AdminDashboardScreen(user, viewModel, onOpenCarts = { openTab("carts") }, onOpenManagers = { openTab("managers") }, onOpenTransaction = openTransaction)
            }
            composable("carts") { CartsScreen(viewModel, openCart, addCart) }
            composable("managers") { ManagersScreen(viewModel) }
            composable("reports") { ReportsScreen(user) }
            composable("profile") { ProfileScreen(user, cartName = null, onLogout = onLogout, onCheckForUpdates = onCheckForUpdates) }
            composable("cart/{cartId}") {
                CartDetailScreen(
                    user,
                    onBack = { navController.popBackStack() },
                    onEdit = { id -> navController.navigate("cart_edit/$id") },
                    onAddTransaction = { cartId, isExpense -> navController.navigate("cart/$cartId/add/${if (isExpense) "expense" else "revenue"}") },
                    onOpenTransaction = openTransaction
                )
            }
            // Admins add revenue/expenses to any active cart and correct any record
            composable("cart/{cartId}/add/{kind}") { entry ->
                AdminAddTransactionRoute(user, isExpense = entry.arguments?.getString("kind") == "expense", onDone = { navController.popBackStack() })
            }
            composable("transaction/{type}/{id}") {
                EditTransactionScreen(user, onDone = { navController.popBackStack() })
            }
            composable("cart_edit/{cartId}") { CartEditScreen(onDone = { navController.popBackStack() }) }
        }
    }
}
