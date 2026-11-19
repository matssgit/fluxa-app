package com.fluxa.personal.ui

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasParent
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performSemanticsAction
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.fluxa.personal.FluxaApplication
import com.fluxa.personal.MainActivity
import com.fluxa.personal.core.date.BillingDay
import com.fluxa.personal.core.date.FinancialCalendar
import com.fluxa.personal.core.money.Money
import com.fluxa.personal.domain.model.AccountDetails
import com.fluxa.personal.domain.model.AccountType
import com.fluxa.personal.domain.model.CategoryDetails
import com.fluxa.personal.domain.model.GoalDetails
import com.fluxa.personal.domain.model.GoalResult
import com.fluxa.personal.domain.model.SafeToSpendState
import com.fluxa.personal.domain.model.SubscriptionDetails
import com.fluxa.personal.domain.model.SubscriptionFrequency
import com.fluxa.personal.domain.model.SubscriptionResult
import com.fluxa.personal.domain.model.SubscriptionSource
import com.fluxa.personal.domain.model.TransactionDirection.EXPENSE
import com.fluxa.personal.ui.designsystem.components.FluxaMoneyFormat
import java.time.LocalDate
import java.util.UUID
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Sprint 7-B: limite seguro de ponta a ponta sobre o Room v7 real; a UI mostra o que `observeSafeToSpend(30)` dá. */
@OptIn(ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
class SafeToSpendFlowTest {
    @get:Rule
    val compose = createEmptyComposeRule()

    private val app get() = ApplicationProvider.getApplicationContext<FluxaApplication>()
    private val today = FinancialCalendar().today()
    private lateinit var streaming: UUID
    private lateinit var main: UUID

    @Before
    fun seed() = runBlocking {
        app.database.clearAllTables()
        streaming = app.categories.create(CategoryDetails("Streaming", EXPENSE, "bolt", "#B9684F"))
        main = app.accounts.create(AccountDetails("Conta Principal", AccountType.CHECKING, Money(100_000), LocalDate.of(2020, 1, 1)))
    }

    /** Anual: uma única ocorrência no horizonte de 30 dias, independente da data do aparelho. */
    private fun subscription(name: String, cents: Long, anchor: LocalDate): UUID = runBlocking {
        (app.subscriptions.create(
            SubscriptionDetails(name, Money(cents), streaming, SubscriptionSource.Account(main)),
            SubscriptionFrequency.YEARLY, anchor, BillingDay(anchor.dayOfMonth),
        ) as SubscriptionResult.Saved).value.id
    }

    private fun contractSafe(): Money = runBlocking {
        (app.safeToSpend.observeSafeToSpend(30).first() as SafeToSpendState.Ready).result.safeAmount
    }

    private fun brl(cents: Long) = FluxaMoneyFormat.format(Money(cents))

    private fun waitForTag(tag: String) = compose.waitUntilAtLeastOneExists(hasTestTag(tag), 5_000)

    private fun inList(list: String, tag: String) {
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

    private fun waitForText(tag: String, text: String) = compose.waitUntil(5_000) {
        val matcher = hasText(text, substring = true) and (hasTestTag(tag) or hasAnyAncestor(hasTestTag(tag)))
        compose.onAllNodes(matcher, useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()
    }

    private fun noText(tag: String, text: String): Boolean {
        val matcher = hasText(text, substring = true) and (hasTestTag(tag) or hasAnyAncestor(hasTestTag(tag)))
        return compose.onAllNodes(matcher, useUnmergedTree = true).fetchSemanticsNodes().isEmpty()
    }

    /** O card no Início com o valor esperado (rolando até ele em telas pequenas). */
    private fun cardShows(cents: Long) {
        waitForTag("list-dashboard")
        inList("list-dashboard", "safe-to-spend-card")
        waitForText("safe-amount", brl(cents))
    }

    private fun openDetails() {
        tap("list-dashboard", "action-safe-details")
        waitForTag("screen-safe-to-spend")
        waitForTag("safe-hero") // o detalhe tem seu próprio ViewModel: espera sair do Loading antes de rolar
    }

    @Test
    fun obligationReducesTheLimitAndPayingUpdatesIt() {
        ActivityScenario.launch(MainActivity::class.java).use {
            cardShows(100_000)
            waitForText("safe-situation", "Nenhuma obrigação conhecida")
            val id = subscription("Seguro", 30_000, today)
            cardShows(70_000)
            assertEquals(Money(70_000), contractSafe())
            waitForText("safe-lowest", "hoje")
            // Pagar: a obrigação sai da projeção e o lançamento real reduz o disponível; limite = novo disponível.
            // (Pagamento antecipado é rejeitado pelo contrato com FUTURE_DATE; paga-se a ocorrência de hoje.)
            runBlocking { app.subscriptions.paySubscriptionOccurrence(id, today, today) }
            cardShows(70_000)
            waitForText("safe-situation", "Nenhuma obrigação conhecida")
            assertEquals(Money(70_000), contractSafe())
        }
    }

    @Test
    fun reservingAndReleasingMoveAvailableAndLimit() {
        val goal = runBlocking { (app.goals.createGoal(GoalDetails("Viagem", Money(500_000), main)) as GoalResult.Saved).value.id }
        ActivityScenario.launch(MainActivity::class.java).use {
            cardShows(100_000)
            runBlocking { app.goals.allocateToGoal(goal, Money(40_000)) }
            // Disponível já líquido: 1.000 − 400 = 600. Limite 600, nunca 200 (sem dupla dedução).
            cardShows(60_000)
            assertTrue(noText("safe-amount", brl(20_000)))
            openDetails()
            inList("feature-list", "safe-available-now")
            waitForText("safe-available-now", brl(60_000))
            inList("feature-list", "safe-note-reserves")
            waitForText("safe-note-reserves", "já foram descontados")
            runBlocking { app.goals.releaseFromGoal(goal, Money(40_000)) }
            inList("feature-list", "safe-detail-amount")
            waitForText("safe-detail-amount", brl(100_000))
        }
    }

    @Test
    fun obligationsUsingEverythingShowZeroAndExplainIt() {
        subscription("Aluguel", 100_000, today.plusDays(3))
        ActivityScenario.launch(MainActivity::class.java).use {
            cardShows(0)
            waitForText("safe-situation", "já utilizam todo o valor disponível")
            waitForText("safe-lowest", "Menor folga em")
            openDetails()
            inList("feature-list", "safe-breakdown")
            waitForText("safe-event-Aluguel", "Aluguel")
            waitForText("safe-event-Aluguel", "Assinatura")
            waitForText("safe-event-Aluguel", brl(100_000))
            inList("feature-list", "safe-lowest-balance")
            waitForText("safe-lowest-balance", brl(0))
        }
    }

    @Test
    fun overdueObligationIsAppliedTodayAndShowsItsOriginalDate() {
        subscription("Academia", 12_000, today.minusDays(3))
        ActivityScenario.launch(MainActivity::class.java).use {
            cardShows(88_000)
            waitForText("safe-lowest", "hoje")
            openDetails()
            inList("feature-list", "safe-breakdown")
            waitForText("safe-event-Academia", "Hoje")
            waitForText("safe-event-Academia", "Vencida em")
        }
    }

    @Test
    fun negativeAvailableShowsZeroAndTheNegativeMessage() {
        runBlocking {
            app.accounts.create(AccountDetails("Conta Negativa", AccountType.CHECKING, Money(-250_000), LocalDate.of(2020, 1, 1)))
        }
        ActivityScenario.launch(MainActivity::class.java).use {
            cardShows(0)
            waitForText("safe-situation", "já está negativo")
            openDetails()
            inList("feature-list", "safe-available-now")
            waitForText("safe-available-now", "−")
            assertTrue(noText("feature-list", "comprometidos"))
        }
    }

    @Test
    fun hiddenAmountsMaskTheLimitAndItsDetails() {
        subscription("Seguro", 30_000, today.plusDays(2))
        ActivityScenario.launch(MainActivity::class.java).use {
            cardShows(70_000)
            compose.onNodeWithTag("toggle-amounts").performClick()
            waitForText("safe-amount", FluxaMoneyFormat.MASK)
            assertTrue(noText("safe-to-spend-card", "R$"))
            openDetails()
            inList("feature-list", "safe-breakdown")
            waitForText("safe-event-Seguro", FluxaMoneyFormat.MASK)
            assertTrue(noText("safe-event-Seguro", "R$"))
            assertTrue(noText("safe-hero", "R$"))
        }
    }

    @Test
    fun limitSurvivesReopening() {
        subscription("Seguro", 30_000, today.plusDays(2))
        ActivityScenario.launch(MainActivity::class.java).use { cardShows(70_000) }
        ActivityScenario.launch(MainActivity::class.java).use {
            cardShows(70_000)
            openDetails()
            inList("feature-list", "safe-breakdown")
            waitForText("safe-event-Seguro", "Seguro")
        }
    }
}
