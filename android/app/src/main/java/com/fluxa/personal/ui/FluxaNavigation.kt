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
import com.fluxa.personal.domain.model.TransactionDirection
import com.fluxa.personal.ui.common.FeedbackBanner
import com.fluxa.personal.ui.feature.cash.CashScreen
import com.fluxa.personal.ui.feature.cash.CashViewModel
import com.fluxa.personal.ui.feature.cash.DashboardScreen
import com.fluxa.personal.ui.feature.cash.DashboardViewModel
import com.fluxa.personal.ui.feature.transactions.NewEntrySheet
import com.fluxa.personal.ui.feature.transactions.RepositoryTransactionSource
import com.fluxa.personal.ui.feature.transactions.TransactionCompletion
import com.fluxa.personal.ui.feature.transactions.TransactionFormScreen
import com.fluxa.personal.ui.feature.transactions.TransactionFormViewModel
import com.fluxa.personal.ui.feature.transactions.TransactionRoutes
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
import com.fluxa.personal.ui.feature.credit.CardDetailScreen
import com.fluxa.personal.ui.feature.credit.CardDetailViewModel
import com.fluxa.personal.ui.feature.credit.CardFormScreen
import com.fluxa.personal.ui.feature.credit.CardFormViewModel
import com.fluxa.personal.ui.feature.credit.CardsScreen
import com.fluxa.personal.ui.feature.credit.CardsViewModel
import com.fluxa.personal.ui.feature.credit.CreditCompletion
import com.fluxa.personal.ui.feature.credit.CreditRoutes
import com.fluxa.personal.ui.feature.credit.PurchaseDetailScreen
import com.fluxa.personal.ui.feature.credit.PurchaseDetailViewModel
import com.fluxa.personal.ui.feature.credit.PurchaseFormScreen
import com.fluxa.personal.ui.feature.credit.PurchaseFormViewModel
import com.fluxa.personal.ui.feature.credit.RepositoryCreditSource
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
    val transactions = remember(app) { RepositoryTransactionSource(app.transactions) }
    val credit = remember(app) { RepositoryCreditSource(app.credit) }
    var feedback by rememberSaveable { mutableStateOf<Int?>(null) }
    val goToTab: (String) -> Unit = { route ->
        controller.navigate(route) {
            popUpTo(controller.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }
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
                composable(Destination.DASHBOARD.route) {
                    DashboardScreen(
                        viewModel = viewModel(factory = DashboardViewModel.factory(transactions, app.accounts, app.categories)),
                        onToggleAmounts = toggleAmounts,
                        onOpenCash = { goToTab(Destination.CASH.route) },
                        onCreateAccount = { controller.navigate(AccountRoutes.NEW) },
                        onOpenTransaction = { controller.navigate(TransactionRoutes.detail(it)) },
                    )
                }
                composable(Destination.CASH.route) {
                    CashScreen(
                        viewModel = viewModel(factory = CashViewModel.factory(transactions, app.accounts, app.categories)),
                        onToggleAmounts = toggleAmounts,
                        onManageAccounts = { controller.navigate(AccountRoutes.LIST) },
                        onCreateAccount = { controller.navigate(AccountRoutes.NEW) },
                        onOpenTransaction = { controller.navigate(TransactionRoutes.detail(it)) },
                    )
                }
                composable(Destination.CARDS.route) {
                    CardsScreen(
                        viewModel = viewModel(factory = CardsViewModel.factory(credit)),
                        onToggleAmounts = toggleAmounts,
                        onCreate = { controller.navigate(CreditRoutes.NEW_CARD) },
                        onOpen = { controller.navigate(CreditRoutes.detail(it)) },
                    )
                }
                composable(CreditRoutes.NEW_CARD) {
                    CardFormScreen(viewModel(factory = CardFormViewModel.factory(credit)), onDone = { completion ->
                        if (completion != null) feedback = R.string.feedback_card_saved
                        controller.popBackStack()
                    })
                }
                composable(CreditRoutes.EDIT_CARD, arguments = listOf(navArgument(CreditRoutes.ARG_CARD) { type = NavType.StringType })) {
                    CardFormScreen(viewModel(factory = CardFormViewModel.factory(credit)), onDone = { completion ->
                        feedback = when (completion) {
                            CreditCompletion.Archived -> R.string.feedback_card_archived
                            null -> null
                            else -> R.string.feedback_card_saved
                        }
                        controller.popBackStack()
                    })
                }
                composable(CreditRoutes.CARD_DETAIL, arguments = listOf(navArgument(CreditRoutes.ARG_CARD) { type = NavType.StringType })) {
                    CardDetailScreen(
                        viewModel = viewModel(factory = CardDetailViewModel.factory(credit, app.categories)),
                        onBack = { controller.popBackStack() },
                        onEdit = { controller.navigate(CreditRoutes.edit(it)) },
                        onNewPurchase = { controller.navigate(CreditRoutes.newPurchase(it)) },
                        onOpenPurchase = { card, purchase -> controller.navigate(CreditRoutes.purchase(card, purchase)) },
                    )
                }
                composable(
                    CreditRoutes.NEW_PURCHASE,
                    arguments = listOf(navArgument(CreditRoutes.ARG_CARD) { type = NavType.StringType; nullable = true; defaultValue = null }),
                ) {
                    PurchaseFormScreen(
                        viewModel = viewModel(factory = PurchaseFormViewModel.factory(credit, app.categories)),
                        onCreated = { created ->
                            feedback = R.string.feedback_purchase_created
                            // Abre o detalhe com as parcelas e vencimentos persistidos, sem empilhar o formulário.
                            controller.navigate(CreditRoutes.purchase(created.cardId, created.purchaseId)) {
                                popUpTo(CreditRoutes.NEW_PURCHASE) { inclusive = true }
                            }
                        },
                        onBack = { controller.popBackStack() },
                        onCreateCard = { controller.navigate(CreditRoutes.NEW_CARD) },
                        onCreateCategory = { controller.navigate(CategoryRoutes.new(TransactionDirection.EXPENSE)) },
                    )
                }
                composable(
                    CreditRoutes.PURCHASE_DETAIL,
                    arguments = listOf(
                        navArgument(CreditRoutes.ARG_CARD) { type = NavType.StringType },
                        navArgument(CreditRoutes.ARG_PURCHASE) { type = NavType.StringType },
                    ),
                ) {
                    PurchaseDetailScreen(
                        viewModel = viewModel(factory = PurchaseDetailViewModel.factory(credit, app.categories)),
                        onBack = { controller.popBackStack() },
                        onCancelled = { feedback = R.string.feedback_purchase_cancelled },
                    )
                }
                composable(Destination.MORE.route) {
                    MorePlaceholder(toggleAmounts, onOpenCategories = { controller.navigate(CategoryRoutes.LIST) })
                }

                composable(AccountRoutes.LIST) {
                    AccountsScreen(
                        viewModel = viewModel(factory = AccountsViewModel.factory(app.accounts, transactions)),
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

                val transactionForm: @Composable () -> Unit = {
                    TransactionFormScreen(
                        viewModel = viewModel(factory = TransactionFormViewModel.factory(transactions, app.accounts, app.categories)),
                        onDone = { completion, direction ->
                            feedback = when (completion) {
                                TransactionCompletion.Created ->
                                    if (direction == TransactionDirection.INCOME) R.string.feedback_income_created else R.string.feedback_expense_created
                                TransactionCompletion.Updated -> R.string.feedback_updated
                                TransactionCompletion.Cancelled -> R.string.feedback_cancelled
                                null -> null
                            }
                            controller.popBackStack()
                        },
                        onCreateAccount = { controller.navigate(AccountRoutes.NEW) },
                        onCreateCategory = { controller.navigate(CategoryRoutes.new(it)) },
                    )
                }
                composable(
                    TransactionRoutes.NEW,
                    arguments = listOf(navArgument(TransactionRoutes.ARG_DIRECTION) { type = NavType.StringType; nullable = true; defaultValue = null }),
                ) { transactionForm() }
                composable(TransactionRoutes.DETAIL, arguments = listOf(navArgument(TransactionRoutes.ARG_ID) { type = NavType.StringType })) {
                    transactionForm()
                }
            }

            FeedbackBanner(
                message = feedback?.let { stringResource(it) },
                onDismiss = { feedback = null },
                modifier = Modifier.align(Alignment.BottomCenter),
            )

            // A barra flutuante só aparece nas abas; telas de cadastro têm ação fixa no rodapé e não podem ser cobertas.
            if (currentRoute in tabRoutes) FluxaNavigationBar(
                items = items,
                selectedRoute = currentRoute,
                onItemSelected = { item -> goToTab(item.route) },
                primaryAction = FluxaNavAction(
                    contentDescription = stringResource(R.string.action_new_entry),
                    onClick = { showNewEntry = true },
                    testTag = "action-new-entry",
                ),
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }

        if (showNewEntry) {
            NewEntrySheet(
                onChoose = { direction ->
                    showNewEntry = false
                    controller.navigate(TransactionRoutes.new(direction))
                },
                onDismiss = { showNewEntry = false },
                onNewPurchase = {
                    showNewEntry = false
                    controller.navigate(CreditRoutes.newPurchase(null))
                },
            )
        }
    }
}

private fun <T> tweenSpec() = androidx.compose.animation.core.tween<T>(FluxaMotion.Short, easing = FluxaMotion.Standard)
private fun <T> snapSpec() = androidx.compose.animation.core.snap<T>()
