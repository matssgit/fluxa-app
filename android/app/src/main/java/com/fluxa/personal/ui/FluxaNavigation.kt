package com.fluxa.personal.ui

import androidx.annotation.StringRes
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.fluxa.personal.R
import com.fluxa.personal.ui.designsystem.FluxaIcons
import com.fluxa.personal.ui.designsystem.FluxaMotion
import com.fluxa.personal.ui.designsystem.FluxaTheme
import com.fluxa.personal.ui.designsystem.LocalAmountsHidden
import com.fluxa.personal.ui.designsystem.LocalReducedMotion
import com.fluxa.personal.ui.designsystem.components.FluxaBottomSheet
import com.fluxa.personal.ui.designsystem.components.FluxaButton
import com.fluxa.personal.ui.designsystem.components.FluxaButtonVariant
import com.fluxa.personal.ui.designsystem.components.FluxaNavAction
import com.fluxa.personal.ui.designsystem.components.FluxaNavItem
import com.fluxa.personal.ui.designsystem.components.FluxaNavigationBar
import com.fluxa.personal.ui.placeholder.CardsPlaceholder
import com.fluxa.personal.ui.placeholder.CashPlaceholder
import com.fluxa.personal.ui.placeholder.DashboardPlaceholder
import com.fluxa.personal.ui.placeholder.MorePlaceholder

private enum class Destination(
    val route: String,
    @StringRes val label: Int,
    val icon: ImageVector,
    val selectedIcon: ImageVector,
) {
    DASHBOARD("dashboard", R.string.dashboard, FluxaIcons.Home, FluxaIcons.HomeFilled),
    CASH("cash", R.string.cash, FluxaIcons.Cash, FluxaIcons.CashFilled),
    CARDS("cards", R.string.cards, FluxaIcons.Card, FluxaIcons.CardFilled),
    MORE("more", R.string.more, FluxaIcons.More, FluxaIcons.MoreFilled),
}

@Composable
internal fun FluxaNavigation() {
    val controller = rememberNavController()
    val entry by controller.currentBackStackEntryAsState()
    var showNewEntry by rememberSaveable { mutableStateOf(false) }

    // Ocultação de valores: parte da preferência persistida e pode ser alternada na sessão.
    // A gravação da preferência fica com a camada de dados (LocalPreferences ainda não expõe escrita).
    var amountsOverride by rememberSaveable { mutableStateOf<Boolean?>(null) }
    val amountsHidden = amountsOverride ?: LocalAmountsHidden.current
    val toggleAmounts = { amountsOverride = !amountsHidden }

    val reducedMotion = LocalReducedMotion.current
    val items = Destination.entries.map {
        FluxaNavItem(
            route = it.route,
            label = stringResource(it.label),
            icon = it.icon,
            selectedIcon = it.selectedIcon,
            testTag = "tab-${it.route}",
        )
    }

    CompositionLocalProvider(LocalAmountsHidden provides amountsHidden) {
        Box(modifier = Modifier.fillMaxSize().background(FluxaTheme.colors.background)) {
            NavHost(
                navController = controller,
                startDestination = Destination.DASHBOARD.route,
                modifier = Modifier.fillMaxSize(),
                enterTransition = { if (reducedMotion) fadeIn(snapSpec()) else fadeIn(tweenSpec()) },
                exitTransition = { if (reducedMotion) fadeOut(snapSpec()) else fadeOut(tweenSpec()) },
            ) {
                composable(Destination.DASHBOARD.route) { DashboardPlaceholder(toggleAmounts) }
                composable(Destination.CASH.route) { CashPlaceholder(toggleAmounts) }
                composable(Destination.CARDS.route) { CardsPlaceholder(toggleAmounts) }
                composable(Destination.MORE.route) { MorePlaceholder(toggleAmounts) }
            }

            FluxaNavigationBar(
                items = items,
                selectedRoute = entry?.destination?.route,
                onItemSelected = { item ->
                    controller.navigate(item.route) {
                        popUpTo(controller.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                primaryAction = FluxaNavAction(
                    contentDescription = stringResource(R.string.action_new_entry),
                    onClick = { showNewEntry = true },
                    testTag = "action-new-entry",
                ),
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }

        if (showNewEntry) {
            // Placeholder visual do padrão de sheet. O fluxo de lançamento não faz parte do Sprint 0.
            FluxaBottomSheet(
                title = stringResource(R.string.action_new_entry),
                onDismissRequest = { showNewEntry = false },
                closeDescription = stringResource(R.string.action_close),
                footer = {
                    FluxaButton(
                        text = stringResource(R.string.action_close),
                        onClick = { showNewEntry = false },
                        variant = FluxaButtonVariant.Secondary,
                        modifier = Modifier.weight(1f),
                    )
                },
            ) {
                Text(
                    stringResource(R.string.placeholder_sheet_body),
                    style = FluxaTheme.typography.body,
                    color = FluxaTheme.colors.textSecondary,
                )
            }
        }
    }
}

private fun <T> tweenSpec() = androidx.compose.animation.core.tween<T>(FluxaMotion.Short, easing = FluxaMotion.Standard)
private fun <T> snapSpec() = androidx.compose.animation.core.snap<T>()
