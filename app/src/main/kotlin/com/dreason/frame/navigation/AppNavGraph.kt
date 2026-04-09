package com.dreason.frame.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Rule
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.dreason.frame.ui.home.HomeScreen
import com.dreason.frame.ui.logs.LogScreen
import com.dreason.frame.ui.rules.RuleListScreen
import com.dreason.frame.ui.servers.ServerListScreen

enum class TopLevelRoute(
    val route: String,
    val label: String,
    val icon: ImageVector,
) {
    HOME("home", "首頁", Icons.Default.Home),
    SERVERS("servers", "伺服器", Icons.Default.Storage),
    RULES("rules", "規則", Icons.Default.Rule),
    LOGS("logs", "日誌", Icons.Default.List),
}

@Composable
fun AppNavGraph() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    Scaffold(
        bottomBar = {
            NavigationBar {
                TopLevelRoute.entries.forEach { route ->
                    NavigationBarItem(
                        icon = { Icon(route.icon, contentDescription = route.label) },
                        label = { Text(route.label) },
                        selected = currentDestination?.hierarchy?.any { it.route == route.route } == true,
                        onClick = {
                            navController.navigate(route.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = TopLevelRoute.HOME.route,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable(TopLevelRoute.HOME.route) { HomeScreen() }
            composable(TopLevelRoute.SERVERS.route) { ServerListScreen() }
            composable(TopLevelRoute.RULES.route) { RuleListScreen() }
            composable(TopLevelRoute.LOGS.route) { LogScreen() }
        }
    }
}
