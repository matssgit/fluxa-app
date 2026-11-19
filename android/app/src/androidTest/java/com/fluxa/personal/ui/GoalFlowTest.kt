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
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.fluxa.personal.FluxaApplication
import com.fluxa.personal.MainActivity
import com.fluxa.personal.core.date.FinancialCalendar
import com.fluxa.personal.core.money.Money
import com.fluxa.personal.domain.model.AccountDetails
import com.fluxa.personal.domain.model.AccountType
import com.fluxa.personal.domain.model.CategoryDetails
import com.fluxa.personal.domain.model.GoalDetails
import com.fluxa.personal.domain.model.GoalResult
import com.fluxa.personal.domain.model.GoalStatus
import com.fluxa.personal.domain.model.TransactionDirection.EXPENSE
import com.fluxa.personal.domain.model.TransactionDraft
import com.fluxa.personal.domain.model.TransactionHistoryFilter
import com.fluxa.personal.domain.rules.CalculatedBalance
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

/** Sprint 6-B: metas e reservas de ponta a ponta sobre o Room v7 real. */
@OptIn(ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
class GoalFlowTest {
    @get:Rule
    val compose = createEmptyComposeRule()

    private val app get() = ApplicationProvider.getApplicationContext<FluxaApplication>()
    private val today = FinancialCalendar().today()
    private lateinit var main: UUID

    @Before
    fun seed() = runBlocking {
        app.database.clearAllTables()
        main = app.accounts.create(AccountDetails("Conta Principal", AccountType.CHECKING, Money(500_000), LocalDate.of(2020, 1, 1)))
    }

    private fun goal(name: String, targetCents: Long, account: UUID = main): UUID = runBlocking {
        (app.goals.createGoal(GoalDetails(name, Money(targetCents), account)) as GoalResult.Saved).value.id
    }

    private fun balanceOfMain(): Money = runBlocking { (app.transactions.observeAccountBalance(main).first() as CalculatedBalance.Exact).amount }
    private fun reserved(id: UUID): Money = runBlocking { app.goals.observeGoal(id).first()!!.reservedAmount }
    private fun ledgerSize() = runBlocking { app.transactions.observeHistory(TransactionHistoryFilter()).first().size }

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

    private fun typeInto(tag: String, text: String) {
        inList("feature-list", tag)
        compose.onNodeWithTag(tag).performTextClearance()
        compose.onNodeWithTag(tag).performTextInput(text)
    }

    private fun openGoals() {
        waitForTag("tab-more")
        compose.onNodeWithTag("tab-more").performClick()
        waitForTag("more-goals")
        compose.onNodeWithTag("more-goals").performClick()
        waitForTag("screen-goals")
    }

    private fun openGoal(name: String) {
        openGoals()
        tap("feature-list", "goal-$name")
        waitForTag("screen-goal-detail")
    }

    private fun move(action: String, amount: String) {
        tap("feature-list", action)
        waitForTag("sheet-goal-movement")
        compose.onNodeWithTag("field-goal-amount").performTextClearance()
        compose.onNodeWithTag("field-goal-amount").performTextInput(amount)
        compose.onNodeWithTag("action-confirm-movement").performClick()
    }

    private fun backToTab(tab: String) {
        while (compose.onAllNodes(hasTestTag("tab-$tab")).fetchSemanticsNodes().isEmpty()) {
            compose.onNodeWithTag("action-back").performClick()
            compose.waitForIdle()
        }
        compose.onNodeWithTag("tab-$tab").performClick()
        waitForTag("list-$tab")
    }

    @Test
    fun reservingAndReleasingMoveOnlyReservedAndAvailable() {
        ActivityScenario.launch(MainActivity::class.java).use {
            openGoals()
            tap("feature-list", "action-new-goal")
            waitForTag("screen-goal-form")
            typeInto("field-goal-name", "Viagem")
            typeInto("field-goal-target", "5.000,00")
            inList("feature-list", "goal-account-explainer")
            compose.onNodeWithTag("action-save").performClick()
            waitForTag("screen-goal-detail")

            move("action-allocate", "2.000,00")
            compose.waitUntilDoesNotExist(hasTestTag("sheet-goal-movement"), 5_000)
            inList("feature-list", "goal-summary")
            waitForText("goal-percent", "40%")
            waitForText("goal-reserved", "2.000,00")
            inList("feature-list", "goal-account-reserves")
            waitForText("reserve-real", "5.000,00")
            waitForText("reserve-reserved", "2.000,00")
            waitForText("reserve-available", "3.000,00")

            backToTab("dashboard")
            inList("list-dashboard", "available-balance")
            waitForText("available-balance", "3.000,00")
            waitForText("reserve-total", "5.000,00")
            waitForText("reserve-reserved-total", "2.000,00")

            openGoal("Viagem")
            move("action-release", "500,00")
            compose.waitUntilDoesNotExist(hasTestTag("sheet-goal-movement"), 5_000)
            inList("feature-list", "goal-summary") // em 320dp o cartão do topo sai da composição após rolar
            waitForText("goal-reserved", "1.500,00")
            inList("feature-list", "goal-history")
            backToTab("dashboard")
            inList("list-dashboard", "available-balance")
            waitForText("available-balance", "3.500,00")
            waitForText("reserve-total", "5.000,00")
        }
        val id = runBlocking { app.goals.observeGoals().first().single().goal.id }
        assertEquals(Money(150_000), reserved(id))
        assertEquals(Money(500_000), balanceOfMain()) // saldo real intacto
        assertEquals(0, ledgerSize()) // nada entra no Caixa
    }

    @Test
    fun completingAndArchivingFollowTheContract() {
        val id = goal("Fone", 100_000)
        runBlocking { app.goals.allocateToGoal(id, Money(100_000)) }
        ActivityScenario.launch(MainActivity::class.java).use {
            openGoal("Fone")
            tap("feature-list", "action-archive-goal")
            waitForTag("action-confirm-archive")
            compose.onNodeWithTag("action-confirm-archive").performClick()
            inList("feature-list", "goal-action-error")
            waitForText("goal-action-error", "Libere todo o valor reservado antes de arquivar esta meta.")
            tap("feature-list", "action-complete-goal")
            waitForTag("action-confirm-archive")
            compose.onNodeWithTag("action-confirm-archive").performClick()
            inList("feature-list", "goal-summary")
            waitForText("goal-status", "Concluída")
            inList("feature-list", "goal-completed-note")
            compose.onNodeWithTag("action-allocate").assertDoesNotExist()
            move("action-release", "1.000,00")
            compose.waitUntilDoesNotExist(hasTestTag("sheet-goal-movement"), 5_000)
            tap("feature-list", "action-archive-goal")
            waitForTag("action-confirm-archive")
            compose.onNodeWithTag("action-confirm-archive").performClick()
            inList("feature-list", "goal-summary")
            waitForText("goal-status", "Arquivada")
        }
        val g = runBlocking { app.goals.observeGoal(id).first()!! }
        assertEquals(GoalStatus.ARCHIVED, g.goal.status)
        assertEquals(2, runBlocking { app.goals.observeMovements(id).first().size })
    }

    @Test
    fun allocationAboveTargetOrAvailableIsRejected() {
        val small = runBlocking { app.accounts.create(AccountDetails("Carteira", AccountType.WALLET, Money(10_000), LocalDate.of(2020, 1, 1))) }
        goal("Fone", 30_000)
        val wallet = goal("Bike", 500_000, small)
        ActivityScenario.launch(MainActivity::class.java).use {
            openGoal("Fone")
            move("action-allocate", "400,00")
            waitForText("sheet-goal-movement", "Essa reserva ultrapassa o valor objetivo da meta.")
        }
        ActivityScenario.launch(MainActivity::class.java).use {
            openGoal("Bike")
            move("action-allocate", "200,00")
            waitForText("sheet-goal-movement", "Esta conta não possui valor disponível suficiente para essa reserva.")
        }
        assertEquals(Money(0), reserved(wallet))
    }

    @Test
    fun negativeAvailabilityAfterRealExpenseIsShownAndReserveKept() {
        val id = goal("Reserva", 500_000)
        runBlocking {
            app.goals.allocateToGoal(id, Money(400_000))
            val market = app.categories.create(CategoryDetails("Mercado", EXPENSE))
            app.transactions.create(UUID.randomUUID(), TransactionDraft(EXPENSE, main, market, Money(200_000), today, "Conserto"))
        }
        ActivityScenario.launch(MainActivity::class.java).use {
            openGoal("Reserva")
            inList("feature-list", "goal-account-reserves")
            waitForText("reserve-available", "1.000,00") // −R$ 1.000,00 (negativo, não zerado)
            waitForText("goal-account-reserves", "O valor reservado está acima do saldo atual desta conta.")
            backToTab("cash")
            inList("list-cash", "available-balance")
            waitForText("available-balance", "1.000,00")
            waitForText("available-balance", "O valor reservado está acima do saldo disponível das contas.")
        }
        assertEquals(Money(400_000), reserved(id))
        assertEquals(Money(300_000), balanceOfMain())
    }

    @Test
    fun goalsAndReservesSurviveReopening() {
        val id = goal("Viagem", 500_000)
        runBlocking { app.goals.allocateToGoal(id, Money(100_000)) }
        ActivityScenario.launch(MainActivity::class.java).use { openGoals() }
        ActivityScenario.launch(MainActivity::class.java).use {
            openGoal("Viagem")
            waitForText("goal-percent", "20%")
            inList("feature-list", "goal-history")
            backToTab("dashboard")
            waitForText("available-balance", "4.000,00")
        }
        assertTrue(runBlocking { app.goals.observeMovements(id).first().isNotEmpty() })
    }
}
