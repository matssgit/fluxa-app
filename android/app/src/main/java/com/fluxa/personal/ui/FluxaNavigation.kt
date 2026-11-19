package com.fluxa.personal.ui

import java.util.UUID
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
import com.fluxa.personal.ui.feature.credit.InstallmentDetailScreen
import com.fluxa.personal.ui.feature.credit.InstallmentDetailViewModel
import com.fluxa.personal.ui.feature.credit.InstallmentPaymentViewModel
import com.fluxa.personal.ui.feature.credit.PaymentOutcome
import com.fluxa.personal.ui.feature.credit.PurchaseDetailScreen
import com.fluxa.personal.ui.feature.credit.PurchaseDetailViewModel
import com.fluxa.personal.ui.feature.credit.PurchaseFormScreen
import com.fluxa.personal.ui.feature.credit.PurchaseFormViewModel
import com.fluxa.personal.ui.feature.credit.RepositoryCreditSource
import com.fluxa.personal.ui.feature.safe.RepositoryCashEventLabels
import com.fluxa.personal.ui.feature.safe.RepositorySafeToSpendDataSource
import com.fluxa.personal.ui.feature.safe.SafeRoutes
import com.fluxa.personal.ui.feature.safe.SafeToSpendDetailScreen
import com.fluxa.personal.ui.feature.safe.SafeToSpendViewModel
import com.fluxa.personal.ui.feature.subscriptions.RepositorySubscriptionDataSource
import com.fluxa.personal.ui.feature.subscriptions.SubscriptionAgendaScreen
import com.fluxa.personal.ui.feature.subscriptions.SubscriptionAgendaViewModel
import com.fluxa.personal.ui.feature.subscriptions.SubscriptionCompletion
import com.fluxa.personal.ui.feature.subscriptions.SubscriptionDetailScreen
import com.fluxa.personal.ui.feature.subscriptions.SubscriptionDetailViewModel
import com.fluxa.personal.ui.feature.subscriptions.SubscriptionFormScreen
import com.fluxa.personal.ui.feature.subscriptions.SubscriptionFormViewModel
import com.fluxa.personal.ui.feature.subscriptions.SubscriptionOccurrenceScreen
import com.fluxa.personal.ui.feature.subscriptions.SubscriptionOccurrenceViewModel
import com.fluxa.personal.ui.feature.subscriptions.SubscriptionRoutes
import com.fluxa.personal.ui.feature.subscriptions.SubscriptionsScreen
import com.fluxa.personal.ui.feature.subscriptions.SubscriptionsViewModel
import com.fluxa.personal.ui.placeholder.MorePlaceholder
import com.fluxa.personal.ui.feature.goals.GoalAllocationViewModel
import com.fluxa.personal.ui.feature.goals.GoalCompletion
import com.fluxa.personal.ui.feature.goals.GoalDetailScreen
import com.fluxa.personal.ui.feature.goals.GoalDetailViewModel
import com.fluxa.personal.ui.feature.goals.GoalFormScreen
import com.fluxa.personal.ui.feature.goals.GoalFormViewModel
import com.fluxa.personal.ui.feature.goals.GoalRoutes
import com.fluxa.personal.ui.feature.goals.GoalsScreen
import com.fluxa.personal.ui.feature.goals.GoalsViewModel
import com.fluxa.personal.ui.feature.goals.RepositoryGoalDataSource

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
    val subscriptions = remember(app) { RepositorySubscriptionDataSource(app.subscriptions) }
    var feedback by rememberSaveable { mutableStateOf<Int?>(null) }
    /** Feedback com valor formatado (ex.: "R$ 500,00 foram reservados…"). */
    var feedbackText by rememberSaveable { mutableStateOf<String?>(null) }
    val goals = remember(app) { RepositoryGoalDataSource(app.goals) }
    val safeSource = remember(app) { RepositorySafeToSpendDataSource(app.safeToSpend) }
    val cashLabels = remember(app) { RepositoryCashEventLabels(subscriptions, credit) }
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
                        viewModel = viewModel(factory = DashboardViewModel.factory(transactions, app.accounts, app.categories, goals.observeReserveBalances())),
                        onToggleAmounts = toggleAmounts,
                        onOpenCash = { goToTab(Destination.CASH.route) },
                        onCreateAccount = { controller.navigate(AccountRoutes.NEW) },
                        onOpenTransaction = { controller.navigate(TransactionRoutes.detail(it)) },
                        safeToSpend = viewModel(factory = SafeToSpendViewModel.factory(safeSource, cashLabels)),
                        onOpenSafeToSpend = { controller.navigate(SafeRoutes.DETAIL) },
                    )
                }
                composable(SafeRoutes.DETAIL) {
                    SafeToSpendDetailScreen(
                        viewModel = viewModel(factory = SafeToSpendViewModel.factory(safeSource, cashLabels)),
                        onBack = { controller.popBackStack() },
                        onOpenSubscription = { id, date -> controller.navigate(SubscriptionRoutes.occurrence(id, date)) },
                        onOpenInstallment = { card, purchase, installment -> controller.navigate(CreditRoutes.installment(card, purchase, installment)) },
                    )
                }
                composable(Destination.CASH.route) {
                    CashScreen(
                        viewModel = viewModel(factory = CashViewModel.factory(transactions, app.accounts, app.categories, goals.observeReserveBalances())),
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
                        onOpenInstallment = { card, purchase, installment ->
                            controller.navigate(CreditRoutes.installment(card, purchase, installment))
                        },
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
                ) { backStack ->
                    val cardArg = backStack.arguments?.getString(CreditRoutes.ARG_CARD)
                    val purchaseArg = backStack.arguments?.getString(CreditRoutes.ARG_PURCHASE)
                    PurchaseDetailScreen(
                        viewModel = viewModel(factory = PurchaseDetailViewModel.factory(credit, app.categories)),
                        onBack = { controller.popBackStack() },
                        onCancelled = { feedback = R.string.feedback_purchase_cancelled },
                        onOpenSubscription = { controller.navigate(SubscriptionRoutes.detail(it)) },
                        onOpenInstallment = { installment ->
                            if (cardArg != null && purchaseArg != null) {
                                controller.navigate(CreditRoutes.installment(UUID.fromString(cardArg), UUID.fromString(purchaseArg), installment))
                            }
                        },
                    )
                }
                composable(
                    CreditRoutes.INSTALLMENT_DETAIL,
                    arguments = listOf(
                        navArgument(CreditRoutes.ARG_CARD) { type = NavType.StringType },
                        navArgument(CreditRoutes.ARG_PURCHASE) { type = NavType.StringType },
                        navArgument(CreditRoutes.ARG_INSTALLMENT) { type = NavType.StringType },
                    ),
                ) {
                    InstallmentDetailScreen(
                        viewModel = viewModel(factory = InstallmentDetailViewModel.factory(credit, app.accounts)),
                        paymentViewModel = viewModel(
                            factory = InstallmentPaymentViewModel.factory(credit, app.accounts, transactions.observeBalances()),
                        ),
                        onBack = { controller.popBackStack() },
                        onPaid = { outcome ->
                            feedback = if (outcome == PaymentOutcome.AlreadyPaid) R.string.feedback_installment_already_paid
                            else R.string.feedback_installment_paid
                        },
                        onUndone = { feedback = R.string.feedback_installment_undone },
                        onCreateAccount = { controller.navigate(AccountRoutes.NEW) },
                    )
                }
                // Sprint 5-B: assinaturas
                composable(SubscriptionRoutes.LIST) {
                    SubscriptionsScreen(
                        viewModel = viewModel(factory = SubscriptionsViewModel.factory(subscriptions, app.accounts, credit)),
                        onBack = { controller.popBackStack() },
                        onNew = { controller.navigate(SubscriptionRoutes.NEW) },
                        onOpen = { controller.navigate(SubscriptionRoutes.detail(it)) },
                        onAgenda = { controller.navigate(SubscriptionRoutes.AGENDA) },
                    )
                }
                composable(SubscriptionRoutes.AGENDA) {
                    SubscriptionAgendaScreen(
                        viewModel = viewModel(factory = SubscriptionAgendaViewModel.factory(subscriptions, app.accounts, credit)),
                        onBack = { controller.popBackStack() },
                        onOpenOccurrence = { id, date -> controller.navigate(SubscriptionRoutes.occurrence(id, date)) },
                    )
                }
                val subscriptionForm: @Composable () -> Unit = {
                    SubscriptionFormScreen(
                        viewModel = viewModel(factory = SubscriptionFormViewModel.factory(subscriptions, app.categories, app.accounts, credit)),
                        onDone = { completion ->
                            if (completion != null) feedback = R.string.feedback_subscription_saved
                            if (completion is SubscriptionCompletion.Created) {
                                controller.navigate(SubscriptionRoutes.detail(completion.id)) {
                                    popUpTo(SubscriptionRoutes.NEW) { inclusive = true }
                                }
                            } else {
                                controller.popBackStack()
                            }
                        },
                        onCreateCategory = { controller.navigate(CategoryRoutes.new(TransactionDirection.EXPENSE)) },
                        onCreateAccount = { controller.navigate(AccountRoutes.NEW) },
                        onCreateCard = { controller.navigate(CreditRoutes.NEW_CARD) },
                    )
                }
                composable(SubscriptionRoutes.NEW) { subscriptionForm() }
                composable(SubscriptionRoutes.EDIT, arguments = listOf(navArgument(SubscriptionRoutes.ARG_ID) { type = NavType.StringType })) {
                    subscriptionForm()
                }
                composable(SubscriptionRoutes.DETAIL, arguments = listOf(navArgument(SubscriptionRoutes.ARG_ID) { type = NavType.StringType })) {
                    SubscriptionDetailScreen(
                        viewModel = viewModel(factory = SubscriptionDetailViewModel.factory(subscriptions, app.accounts, credit, app.categories)),
                        onBack = { controller.popBackStack() },
                        onEdit = { controller.navigate(SubscriptionRoutes.edit(it)) },
                        onOpenOccurrence = { id, date -> controller.navigate(SubscriptionRoutes.occurrence(id, date)) },
                        onAgenda = { controller.navigate(SubscriptionRoutes.AGENDA) },
                        onFeedback = { feedback = it },
                    )
                }
                composable(
                    SubscriptionRoutes.OCCURRENCE,
                    arguments = listOf(
                        navArgument(SubscriptionRoutes.ARG_ID) { type = NavType.StringType },
                        navArgument(SubscriptionRoutes.ARG_DATE) { type = NavType.StringType },
                    ),
                ) {
                    SubscriptionOccurrenceScreen(
                        viewModel = viewModel(factory = SubscriptionOccurrenceViewModel.factory(subscriptions, app.accounts, credit)),
                        onBack = { controller.popBackStack() },
                        onFeedback = { feedback = it },
                        onOpenTransaction = { controller.navigate(TransactionRoutes.detail(it)) },
                        onOpenPurchase = { card, purchase -> controller.navigate(CreditRoutes.purchase(card, purchase)) },
                        onOpenInstallment = { card, purchase, installment -> controller.navigate(CreditRoutes.installment(card, purchase, installment)) },
                    )
                }
                composable(Destination.MORE.route) {
                    MorePlaceholder(
                        toggleAmounts,
                        onOpenCategories = { controller.navigate(CategoryRoutes.LIST) },
                        onOpenSubscriptions = { controller.navigate(SubscriptionRoutes.LIST) },
                        onOpenGoals = { controller.navigate(GoalRoutes.LIST) },
                    )
                }
                // Sprint 6-B: metas e reservas
                composable(GoalRoutes.LIST) {
                    GoalsScreen(
                        viewModel = viewModel(factory = GoalsViewModel.factory(goals)),
                        onBack = { controller.popBackStack() },
                        onNew = { controller.navigate(GoalRoutes.NEW) },
                        onOpen = { controller.navigate(GoalRoutes.detail(it)) },
                    )
                }
                val goalForm: @Composable () -> Unit = {
                    GoalFormScreen(
                        viewModel = viewModel(factory = GoalFormViewModel.factory(goals, app.accounts)),
                        onDone = { completion ->
                            if (completion != null) feedback = R.string.feedback_goal_saved
                            if (completion is GoalCompletion.Created) {
                                controller.navigate(GoalRoutes.detail(completion.id)) { popUpTo(GoalRoutes.NEW) { inclusive = true } }
                            } else controller.popBackStack()
                        },
                        onCreateAccount = { controller.navigate(AccountRoutes.NEW) },
                    )
                }
                composable(GoalRoutes.NEW) { goalForm() }
                composable(GoalRoutes.EDIT, arguments = listOf(navArgument(GoalRoutes.ARG_ID) { type = NavType.StringType })) { goalForm() }
                composable(GoalRoutes.DETAIL, arguments = listOf(navArgument(GoalRoutes.ARG_ID) { type = NavType.StringType })) {
                    GoalDetailScreen(
                        viewModel = viewModel(factory = GoalDetailViewModel.factory(goals)),
                        allocation = viewModel(factory = GoalAllocationViewModel.factory(goals)),
                        onBack = { controller.popBackStack() },
                        onEdit = { controller.navigate(GoalRoutes.edit(it)) },
                        onFeedback = { feedback = it },
                        onFeedbackText = { feedbackText = it },
                    )
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
                        viewModel = viewModel(
                            factory = TransactionFormViewModel.factory(transactions, app.accounts, app.categories, app.credit::observeInstallmentOrigin),
                        ),
                        onOpenSubscription = { controller.navigate(SubscriptionRoutes.detail(it)) },
                        onOpenInstallment = { card, purchase, installment ->
                            controller.navigate(CreditRoutes.installment(card, purchase, installment))
                        },
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
                message = feedbackText ?: feedback?.let { stringResource(it) },
                onDismiss = { feedback = null; feedbackText = null },
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
