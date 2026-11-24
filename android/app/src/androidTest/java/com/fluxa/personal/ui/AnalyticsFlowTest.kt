package com.fluxa.personal.ui

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasParent
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.fluxa.personal.FluxaApplication
import com.fluxa.personal.MainActivity
import com.fluxa.personal.core.date.FinancialCalendar
import com.fluxa.personal.core.money.Money
import com.fluxa.personal.domain.model.AccountDetails
import com.fluxa.personal.domain.model.AccountType
import com.fluxa.personal.core.date.BillingDay
import com.fluxa.personal.domain.model.CategoryDetails
import com.fluxa.personal.domain.model.CreditCardDetails
import com.fluxa.personal.domain.model.CreditResult
import com.fluxa.personal.domain.model.FinancialAnalyticsSnapshot
import com.fluxa.personal.domain.model.FinancialAnalyticsState
import com.fluxa.personal.domain.model.GoalDetails
import com.fluxa.personal.domain.model.GoalResult
import com.fluxa.personal.domain.model.PurchaseDraft
import com.fluxa.personal.domain.model.SubscriptionDetails
import com.fluxa.personal.domain.model.SubscriptionFrequency
import com.fluxa.personal.domain.model.SubscriptionResult
import com.fluxa.personal.domain.model.SubscriptionSource
import com.fluxa.personal.domain.model.SubscriptionUndoResult
import com.fluxa.personal.domain.model.TransactionDirection
import com.fluxa.personal.domain.model.TransactionDirection.EXPENSE
import com.fluxa.personal.domain.model.TransactionDirection.INCOME
import com.fluxa.personal.domain.model.TransactionDraft
import com.fluxa.personal.ui.designsystem.components.FluxaMoneyFormat
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Sprint 11-B: Análises de ponta a ponta sobre o Room real e os contratos da 11-A. */
@OptIn(ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
class AnalyticsFlowTest {
    @get:Rule
    val compose = createEmptyComposeRule()

    private val app get() = ApplicationProvider.getApplicationContext<FluxaApplication>()
    private val phase: String? get() = InstrumentationRegistry.getArguments().getString("analyticsPhase")
    private val today = FinancialCalendar().today()
    private val month = YearMonth.from(today)
    private lateinit var main: UUID
    private lateinit var food: UUID
    private lateinit var home: UUID
    private lateinit var salary: UUID

    @Before
    fun seed() = runBlocking {
        if (phase == "verify") return@runBlocking
        app.preferences.apply(app.preferences.read().copy(hideAmounts = false))
        app.database.clearAllTables()
        main = app.accounts.create(AccountDetails("Conta Principal", AccountType.CHECKING, Money(500_000), LocalDate.of(2020, 1, 1)))
        food = app.categories.create(CategoryDetails("Alimentação", EXPENSE, "food", "#B9684F"))
        home = app.categories.create(CategoryDetails("Casa", EXPENSE, "home", "#35695A"))
        salary = app.categories.create(CategoryDetails("Salário", INCOME, "work", "#829B88"))
    }

    @After
    fun resetPrivacy() = runBlocking { app.preferences.apply(app.preferences.read().copy(hideAmounts = false)) }

    // region Apoio

    private fun brl(cents: Long, signed: Boolean = false) = FluxaMoneyFormat.format(Money(cents), signed)
    private fun waitForTag(tag: String) = compose.waitUntilAtLeastOneExists(hasTestTag(tag), 10_000)
    private fun click(tag: String) { waitForTag(tag); compose.onNodeWithTag(tag).performClick() }

    private fun inList(list: String, tag: String) {
        waitForTag(list)
        compose.waitForIdle()
        compose.onNodeWithTag(list).performScrollToNode(hasTestTag(tag))
    }

    private fun tap(list: String, tag: String) {
        inList(list, tag)
        val node = compose.onNodeWithTag(tag)
        val target = if (SemanticsActions.OnClick in node.fetchSemanticsNode().config) node
        else compose.onNode(hasClickAction() and hasParent(hasTestTag(tag)))
        target.performSemanticsAction(SemanticsActions.OnClick)
    }

    private fun waitForText(tag: String, text: String) = compose.waitUntil(10_000) {
        val matcher = (hasText(text, substring = true) or hasContentDescription(text, substring = true)) and (hasTestTag(tag) or hasAnyAncestor(hasTestTag(tag)))
        compose.onAllNodes(matcher, useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()
    }

    /** Rola até o bloco e espera o valor. */
    private fun shows(tag: String, text: String) {
        waitForTag("screen-analytics")
        compose.waitUntil(10_000) { runCatching { inList("feature-list", tag) }.isSuccess }
        waitForText(tag, text)
    }

    private fun gone(tag: String) = compose.waitUntil(10_000) { compose.onAllNodes(hasTestTag(tag)).fetchSemanticsNodes().isEmpty() }

    private fun openFromMore() {
        waitForTag("tab-more")
        compose.onNodeWithTag("tab-more").performClick()
        click("more-analytics")
        waitForTag("screen-analytics")
    }

    private fun tx(direction: TransactionDirection, category: UUID, cents: Long, day: LocalDate, text: String): UUID = runBlocking {
        UUID.randomUUID().also { app.transactions.create(it, TransactionDraft(direction, main, category, Money(cents), day, text)) }
    }

    private fun snapshot(): FinancialAnalyticsSnapshot = runBlocking {
        (app.analytics.observeFinancialAnalytics().first() as FinancialAnalyticsState.Ready).snapshot
    }

    private fun register(kind: String, amount: String, category: String, description: String) {
        click("action-new-entry")
        click(kind)
        waitForTag("screen-transaction-form")
        typeInto("field-tx-amount", amount)
        inList("feature-list", category)
        compose.onNodeWithTag(category).performClick()
        typeInto("field-tx-description", description)
        compose.onNodeWithTag("action-save").performClick()
        compose.waitUntilDoesNotExist(hasTestTag("screen-transaction-form"), 10_000)
    }

    private fun typeInto(tag: String, text: String) {
        inList("feature-list", tag)
        compose.onNodeWithTag(tag).performTextClearance()
        compose.onNodeWithTag(tag).performTextInput(text)
    }

    // endregion

    @Test
    fun incomeAndExpensesCreatedInTheAppAppearInSummaryCategoriesAndEvolution() {
        ActivityScenario.launch(MainActivity::class.java).use {
            waitForTag("tab-dashboard")
            register("new-income", "6.200,00", "category-option-Salário", "Pagamento")
            register("new-expense", "300,00", "category-option-Alimentação", "Feira")
            register("new-expense", "100,00", "category-option-Casa", "Luz")

            // Atalho discreto do Início; a aba continua a mesma.
            compose.onNodeWithTag("tab-dashboard").performClick()
            tap("list-dashboard", "action-open-analytics")
            shows("analytics-summary", brl(620_000))
            waitForText("analytics-income", brl(620_000))
            waitForText("analytics-expense", brl(40_000))
            waitForText("analytics-net", brl(580_000, signed = true))
            waitForText("analytics-savings", "93,5%")
            shows("insight-TOP_CATEGORY", "Alimentação")
            shows("category-share-Alimentação", brl(30_000))
            waitForText("category-share-Alimentação", "75%")
            shows("category-share-Casa", brl(10_000))
            waitForText("category-share-Casa", "25%")
            shows("analytics-chart", "Gráfico de entradas e despesas")
            shows("evolution-$month", "Resultado " + brl(580_000, signed = true))
            shows("evolution-${month.minusMonths(5)}", "Entradas " + brl(0)) // mês sem dados vale zero
            compose.onNodeWithTag("action-back").performClick()
            waitForTag("list-dashboard")
        }
        val s = snapshot()
        assertEquals(Money(620_000), s.monthSummary.incomeRealized)
        assertEquals(Money(40_000), s.monthSummary.expenseRealized)
    }

    @Test
    fun cardPurchaseCountsOnceWhenPaidAndUndoRemovesIt() {
        val card = runBlocking { (app.credit.createCard(CreditCardDetails("Roxo", Money(500_000), BillingDay(10), BillingDay(17))) as CreditResult.Saved).value.id }
        val purchase = UUID.randomUUID()
        tx(EXPENSE, home, 10_000, today, "Luz")
        ActivityScenario.launch(MainActivity::class.java).use {
            openFromMore()
            shows("analytics-expense", brl(10_000))
            runBlocking { app.credit.createPurchase(purchase, PurchaseDraft(card, food, "Fone", Money(60_000), 2, today)) }
            compose.waitForIdle()
            shows("analytics-expense", brl(10_000)) // comprar não é despesa realizada
            val first = runBlocking { app.credit.observeInstallments(purchase).first().first() }
            runBlocking { app.credit.payInstallment(first.id, main, today) }
            shows("analytics-expense", brl(40_000)) // 100 + uma parcela de 300, uma única vez
            shows("category-share-Alimentação", brl(30_000))
            shows("projection-available", brl(460_000))
            val payment = runBlocking { app.credit.observeInstallmentPayment(first.id).first()!! }
            runBlocking { app.credit.undoInstallmentPayment(first.id, payment.id) }
            shows("analytics-expense", brl(10_000))
            shows("projection-available", brl(490_000))
            gone("category-share-Alimentação")
        }
        assertEquals(1L, snapshot().monthSummary.transactionCount)
    }

    @Test
    fun subscriptionResolutionUpdatesWithoutDuplicatingAndUndoReverts() {
        val id = runBlocking {
            (app.subscriptions.create(
                SubscriptionDetails("Música", Money(5_000), food, SubscriptionSource.Account(main)),
                SubscriptionFrequency.YEARLY, today, BillingDay(today.dayOfMonth),
            ) as SubscriptionResult.Saved).value.id
        }
        tx(INCOME, salary, 100_000, today, "Pagamento")
        ActivityScenario.launch(MainActivity::class.java).use {
            openFromMore()
            shows("analytics-expense", brl(0))
            shows("projection-obligations", brl(-5_000))
            shows("subscriptions-active", "1")
            shows("subscriptions-monthly", brl(417)) // 50/12 arredondado uma vez pelo contrato
            shows("subscriptions-annual", brl(5_000))
            runBlocking { app.subscriptions.paySubscriptionOccurrence(id, today, today) }
            shows("analytics-expense", brl(5_000))
            shows("projection-no-obligations", "Nenhuma obrigação")
            shows("projection-ending", brl(595_000))
            val result = runBlocking { app.subscriptions.observeOccurrenceDetail(id, today).first()!!.occurrence.resultId!! }
            assertTrue(runBlocking { app.subscriptions.undoSubscriptionOccurrence(id, today, result) } is SubscriptionUndoResult.Reopened)
            shows("analytics-expense", brl(0))
            shows("projection-obligations", brl(-5_000))
            tap("feature-list", "action-analytics-subscriptions")
            waitForTag("screen-subscriptions")
        }
    }

    @Test
    fun reservingChangesTheProjectionButNotTheRealizedMonth() {
        tx(INCOME, salary, 100_000, today, "Pagamento")
        tx(EXPENSE, food, 20_000, today, "Feira")
        val goal = runBlocking { (app.goals.createGoal(GoalDetails("Viagem", Money(300_000), main)) as GoalResult.Saved).value.id }
        ActivityScenario.launch(MainActivity::class.java).use {
            openFromMore()
            shows("projection-available", brl(580_000))
            runBlocking { app.goals.allocateToGoal(goal, Money(150_000)) }
            shows("projection-available", brl(430_000))
            shows("projection-ending", brl(430_000)) // sem segunda dedução da reserva
            shows("analytics-income", brl(100_000))
            waitForText("analytics-expense", brl(20_000))
            waitForText("analytics-net", brl(80_000, signed = true))
            runBlocking { app.goals.releaseFromGoal(goal, Money(150_000)) }
            shows("projection-available", brl(580_000))
            tap("feature-list", "action-analytics-safe")
            waitForTag("safe-hero")
            waitForText("safe-detail-amount", brl(580_000))
            compose.onNodeWithTag("action-back").performClick()
            waitForTag("screen-analytics")
        }
    }

    @Test
    fun monthsNavigateBackwardsAndStopAtTheCurrentMonth() {
        val previous = month.minusMonths(1)
        tx(EXPENSE, food, 11_800, previous.atDay(3), "Feira antiga")
        tx(EXPENSE, food, 10_000, previous.minusMonths(1).atDay(3), "Feira mais antiga")
        tx(INCOME, salary, 100, today, "Pagamento")
        ActivityScenario.launch(MainActivity::class.java).use {
            openFromMore()
            waitForTag("analytics-next")
            compose.onNodeWithTag("analytics-next").assertIsNotEnabled()
            shows("analytics-no-expenses", "Nenhuma despesa") // receitas sem despesas: conteúdo parcial
            inList("feature-list", "analytics-previous")
            compose.onNodeWithTag("analytics-previous").performClick()
            shows("analytics-expense", brl(11_800))
            waitForText("analytics-no-income", "Sem receita realizada")
            shows("insight-EXPENSES_INCREASED", "+18%")
            inList("feature-list", "analytics-next")
            compose.onNodeWithTag("analytics-next").assertIsEnabled().performClick()
            shows("analytics-income", brl(100))
            inList("feature-list", "analytics-next")
            compose.onNodeWithTag("analytics-next").assertIsNotEnabled()
        }
    }

    @Test
    fun monthWithoutMovementsDoesNotRepeatTheEmptyMessageOrLeaveAnOrphanHeading() {
        tx(EXPENSE, food, 10_000, month.minusMonths(2).atDay(3), "Feira antiga")
        ActivityScenario.launch(MainActivity::class.java).use {
            openFromMore()
            shows("analytics-summary", "Nenhuma movimentação neste mês")
            shows("analytics-evolution", "Entradas")
            compose.onNodeWithTag("analytics-categories").assertDoesNotExist()
            compose.onNodeWithTag("analytics-no-expenses").assertDoesNotExist()
            assertTrue(compose.onAllNodes(hasText("Gastos por categoria")).fetchSemanticsNodes().isEmpty())
            // Com receita e nenhuma despesa a seção volta, com a frase própria.
            tx(INCOME, salary, 1_000, today, "Pagamento")
            shows("analytics-no-expenses", "Nenhuma despesa realizada neste mês")
            tx(EXPENSE, food, 500, today, "Feira")
            shows("category-share-Alimentação", "100%")
        }
    }

    @Test
    fun moreListsOnlyWorkingDestinations() {
        ActivityScenario.launch(MainActivity::class.java).use {
            waitForTag("tab-more")
            compose.onNodeWithTag("tab-more").performClick()
            val expected = listOf("subscriptions", "goals", "analytics", "categories", "backup", "security")
            waitForTag("more-security")
            val rows = expected.map { compose.onNodeWithTag("more-$it").fetchSemanticsNode() }
            assertTrue(rows.all { SemanticsActions.OnClick in it.config })
            assertEquals(rows, rows.sortedBy { it.positionInRoot.y }) // ordem lógica = ordem visual
            for (text in listOf("Em breve", "Configurações")) {
                assertTrue(compose.onAllNodes(hasText(text, substring = true), useUnmergedTree = true).fetchSemanticsNodes().isEmpty())
            }
        }
    }

    @Test
    fun emptyDatabaseShowsAnInvitation() {
        runBlocking { app.database.clearAllTables() }
        ActivityScenario.launch(MainActivity::class.java).use {
            openFromMore()
            shows("analytics-empty", "Ainda não há movimentações suficientes")
            compose.onNodeWithTag("analytics-summary").assertDoesNotExist()
            compose.onNodeWithTag("analytics-chart").assertDoesNotExist()
        }
    }

    @Test
    fun hiddenAmountsLeaveNoMoneyInTheScreenOrItsSemantics() {
        tx(INCOME, salary, 100_000, today, "Pagamento")
        tx(EXPENSE, food, 250_000, today, "Reforma")
        runBlocking {
            app.subscriptions.create(
                SubscriptionDetails("Música", Money(5_000), food, SubscriptionSource.Account(main)),
                SubscriptionFrequency.MONTHLY, today, BillingDay(today.dayOfMonth),
            )
            app.preferences.apply(app.preferences.read().copy(hideAmounts = true))
        }
        val money = SemanticsMatcher("contém valor monetário") { node ->
            val texts = node.config.getOrNull(SemanticsProperties.Text).orEmpty().map { it.text } +
                node.config.getOrNull(SemanticsProperties.ContentDescription).orEmpty() +
                listOfNotNull(node.config.getOrNull(SemanticsProperties.StateDescription))
            texts.any { "R$" in it || "1.000" in it || "2.500" in it || "−150" in it }
        }
        ActivityScenario.launch(MainActivity::class.java).use {
            openFromMore()
            for (tag in listOf(
                "analytics-summary", "analytics-insights", "analytics-categories", "analytics-evolution",
                "evolution-${month.minusMonths(5)}", "analytics-projection", "projection-lowest", "analytics-subscriptions", "subscriptions-annual",
            )) {
                compose.waitUntil(10_000) { runCatching { inList("feature-list", tag) }.isSuccess }
                compose.waitForIdle()
                assertEquals("valor visível em $tag", 0, compose.onAllNodes(money, useUnmergedTree = true).fetchSemanticsNodes().size)
            }
            shows("analytics-chart-hidden", "Gráfico oculto")
            compose.onNodeWithTag("analytics-chart").assertDoesNotExist()
            shows("analytics-insights-hidden", "ocultos")
            compose.onNodeWithTag("insight-NEGATIVE_MONTH").assertDoesNotExist()
            shows("evolution-$month", "Valor oculto")
        }
    }

    /** Protocolo externo: `-e analyticsPhase seed`, `am force-stop`, `-e analyticsPhase verify`. */
    @Test
    fun analyticsSurviveForceStop() {
        assumeTrue("Run seed and verify in separate processes", phase == "seed" || phase == "verify")
        if (phase == "seed") {
            tx(INCOME, salary, 620_000, today, "Pagamento")
            tx(EXPENSE, food, 30_000, today, "Feira")
            tx(EXPENSE, home, 10_000, today, "Luz")
        }
        ActivityScenario.launch(MainActivity::class.java).use {
            openFromMore()
            shows("analytics-income", brl(620_000))
            waitForText("analytics-expense", brl(40_000))
            waitForText("analytics-net", brl(580_000, signed = true))
            shows("category-share-Alimentação", "75%")
            shows("evolution-$month", "Resultado " + brl(580_000, signed = true))
            shows("projection-available", brl(1_080_000))
        }
    }
}
