package com.neoqubix.devajit.h2.presentation.navigation

import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController

/*
 * Switches bottom-bar tabs, keeping each tab's own state. Every jump to a tab (bar, "See all", dashboard
 * shortcuts) must go through here: a plain navigate() stacks the tab on top of Home, and Home's saved stack then
 * keeps bringing that screen back.
 */
fun NavHostController.navigateToTab(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

data class TabItem(val route: String, val label: String, val icon: ImageVector)

@Composable
fun BottomTabs(navController: NavHostController, tabs: List<TabItem>, currentRoute: String?) {
    NavigationBar {
        tabs.forEach { tab ->
            NavigationBarItem(
                selected = currentRoute == tab.route,
                onClick = { navController.navigateToTab(tab.route) },
                icon = { Icon(tab.icon, contentDescription = null) },
                label = { Text(tab.label) }
            )
        }
    }
}
