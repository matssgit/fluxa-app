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
import com.fluxa.personal.FluxaApplication
import com.fluxa.personal.R
import com.fluxa.personal.ui.feature.accounts.AccountFormScreen
import com.fluxa.personal.ui.feature.accounts.AccountFormViewModel
import com.fluxa.personal.ui.feature.accounts.AccountRoutes
import com.fluxa.personal.ui.feature.accounts.AccountsScreen
import com.fluxa.personal.ui.feature.accounts.AccountsViewModel
import com.fluxa.personal.ui.feature.categories.CategoriesScreen
import com.fluxa.personal.ui.feature.categories.CategoriesViewModel
import com.fluxa.personal.ui.feature.categories.CategoryFormScreen
import com.fluxa.personal.ui.feature.categories.CategoryFormViewModel
import com.fluxa.personal.ui.feature.categories.CategoryRoutes
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.navArgument
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
    val app = LocalContext.current.applicationContext as FluxaApplication
    val currentRoute = entry?.destination?.route
    val tabRoutes = remember { Destination.entries.map { it.route }.toSet() }
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
                composable(Destination.CASH.route) {
                    val accounts: AccountsViewModel = viewModel(factory = AccountsViewModel.factory(app.accounts))
                    val accountsState by accounts.uiState.collectAsStateWithLifecycle()
                    CashPlaceholder(
                        onToggleAmounts = toggleAmounts,
                        activeAccounts = if (accountsState.isLoading) null else accountsState.active.size,
                        onManageAccounts = { controller.navigate(AccountRoutes.LIST) },
                    )
                }
                composable(Destination.CARDS.route) { CardsPlaceholder(toggleAmounts) }
                composable(Destination.MORE.route) {
                    MorePlaceholder(toggleAmounts, onOpenCategories = { controller.navigate(CategoryRoutes.LIST) })
                }

                composable(AccountRoutes.LIST) {
                    AccountsScreen(
                        viewModel = viewModel(factory = AccountsViewModel.factory(app.accounts)),
                        onBack = { controller.popBackStack() },
                        onCreate = { controller.navigate(AccountRoutes.NEW) },
                        onEdit = { controller.navigate(AccountRoutes.edit(it)) },
                    )
                }
                composable(AccountRoutes.NEW) {
                    AccountFormScreen(viewModel(factory = AccountFormViewModel.factory(app.accounts)), onDone = { controller.popBackStack() })
                }
                composable(AccountRoutes.EDIT, arguments = listOf(navArgument(AccountRoutes.ARG_ID) { type = NavType.StringType })) {
                    AccountFormScreen(viewModel(factory = AccountFormViewModel.factory(app.accounts)), onDone = { controller.popBackStack() })
                }
                composable(CategoryRoutes.LIST) {
                    CategoriesScreen(
                        viewModel = viewModel(factory = CategoriesViewModel.factory(app.categories)),
                        onBack = { controller.popBackStack() },
                        onCreate = { controller.navigate(CategoryRoutes.new(it)) },
                        onEdit = { controller.navigate(CategoryRoutes.edit(it)) },
                    )
                }
                composable(
                    CategoryRoutes.NEW,
                    arguments = listOf(navArgument(CategoryRoutes.ARG_KIND) { type = NavType.StringType; nullable = true; defaultValue = null }),
                ) {
                    CategoryFormScreen(viewModel(factory = CategoryFormViewModel.factory(app.categories)), onDone = { controller.popBackStack() })
                }
                composable(CategoryRoutes.EDIT, arguments = listOf(navArgument(CategoryRoutes.ARG_ID) { type = NavType.StringType })) {
                    CategoryFormScreen(viewModel(factory = CategoryFormViewModel.factory(app.categories)), onDone = { controller.popBackStack() })
                }
            }

            // A barra flutuante só aparece nas abas; telas de cadastro têm ação fixa no rodapé e não podem ser cobertas.
            if (currentRoute in tabRoutes) FluxaNavigationBar(
                items = items,
                selectedRoute = currentRoute,
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
