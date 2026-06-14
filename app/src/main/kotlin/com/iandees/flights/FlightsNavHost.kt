package com.iandees.flights

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FlightTakeoff
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*
import com.iandees.flights.feature.flightlist.FlightListScreen
import com.iandees.flights.feature.flightdetail.FlightDetailScreen
import com.iandees.flights.feature.addedit.AddEditFlightScreen
import com.iandees.flights.feature.map.MapScreen
import com.iandees.flights.feature.stats.StatsScreen
import com.iandees.flights.feature.importcsv.ImportCsvScreen

sealed class Screen(val route: String) {
    // Bottom nav destinations
    object FlightList : Screen("flight_list")
    object Map        : Screen("map")
    object Stats      : Screen("stats")

    // Detail / edit (not in bottom nav)
    object FlightDetail : Screen("flight_detail/{flightId}") {
        fun createRoute(flightId: Long) = "flight_detail/$flightId"
    }
    object AddFlight  : Screen("add_flight")
    object EditFlight : Screen("edit_flight/{flightId}") {
        fun createRoute(flightId: Long) = "edit_flight/$flightId"
    }
    object ImportCsv  : Screen("import_csv")
}

private data class BottomNavItem(
    val screen: Screen,
    val label: String,
    val icon: ImageVector,
)

private val bottomNavItems = listOf(
    BottomNavItem(Screen.FlightList, "Flights",  Icons.Default.FlightTakeoff),
    BottomNavItem(Screen.Map,        "Map",      Icons.Default.Map),
    BottomNavItem(Screen.Stats,      "Stats",    Icons.Default.BarChart),
)

@Composable
fun FlightsNavHost() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val showBottomBar = bottomNavItems.any { it.screen.route == currentDestination?.route }

    Scaffold(
        contentWindowInsets = WindowInsets(0),  // let each screen manage its own insets
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    bottomNavItems.forEach { item ->
                        NavigationBarItem(
                            icon = { Icon(item.icon, contentDescription = item.label) },
                            label = { Text(item.label) },
                            selected = currentDestination?.hierarchy?.any { it.route == item.screen.route } == true,
                            onClick = {
                                navController.navigate(item.screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.FlightList.route,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable(Screen.FlightList.route) {
                FlightListScreen(
                    onFlightClick  = { id -> navController.navigate(Screen.FlightDetail.createRoute(id)) },
                    onAddFlight    = { navController.navigate(Screen.AddFlight.route) },
                    onImportCsv    = { navController.navigate(Screen.ImportCsv.route) },
                )
            }
            composable(Screen.Map.route) {
                MapScreen()
            }
            composable(Screen.Stats.route) {
                StatsScreen()
            }
            composable(Screen.FlightDetail.route) { backStack ->
                val flightId = backStack.arguments?.getString("flightId")?.toLongOrNull() ?: return@composable
                FlightDetailScreen(
                    flightId   = flightId,
                    onEdit     = { navController.navigate(Screen.EditFlight.createRoute(flightId)) },
                    onBack     = { navController.popBackStack() },
                )
            }
            composable(Screen.AddFlight.route) {
                AddEditFlightScreen(
                    flightId = null,
                    onBack   = { navController.popBackStack() },
                )
            }
            composable(Screen.EditFlight.route) { backStack ->
                val flightId = backStack.arguments?.getString("flightId")?.toLongOrNull() ?: return@composable
                AddEditFlightScreen(
                    flightId = flightId,
                    onBack   = { navController.popBackStack() },
                )
            }
            composable(Screen.ImportCsv.route) {
                ImportCsvScreen(onBack = { navController.popBackStack() })
            }
        }
    }
}
