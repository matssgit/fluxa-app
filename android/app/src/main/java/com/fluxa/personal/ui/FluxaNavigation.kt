package com.fluxa.personal.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.fluxa.personal.R

private enum class Destination(val route: String, @StringRes val label: Int) {
    DASHBOARD("dashboard", R.string.dashboard),
    CASH("cash", R.string.cash),
    CARDS("cards", R.string.cards),
    MORE("more", R.string.more),
}

@Composable
internal fun FluxaNavigation() {
    val controller = rememberNavController()
    val entry by controller.currentBackStackEntryAsState()
    Scaffold(
        bottomBar = {
            NavigationBar {
                Destination.entries.forEach { destination ->
                    NavigationBarItem(
                        modifier = Modifier.testTag("tab-${destination.route}"),
                        selected = entry?.destination?.route == destination.route,
                        icon = { Text(stringResource(destination.label)) },
                        onClick = {
                            controller.navigate(destination.route) {
                                popUpTo(controller.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                    )
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = controller,
            startDestination = Destination.DASHBOARD.route,
            modifier = Modifier.padding(padding),
        ) {
            Destination.entries.forEach { destination ->
                composable(destination.route) {
                    Box(
                        modifier = Modifier.fillMaxSize().testTag("screen-${destination.route}"),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(stringResource(destination.label), style = MaterialTheme.typography.headlineMedium)
                    }
                }
            }
        }
    }
}
