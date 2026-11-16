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
import com.fluxa.personal.core.date.BillingDay
import com.fluxa.personal.core.date.FinancialCalendar
import com.fluxa.personal.core.money.Money
import com.fluxa.personal.domain.model.AccountDetails
import com.fluxa.personal.domain.model.AccountType
import com.fluxa.personal.domain.model.CategoryDetails
import com.fluxa.personal.domain.model.CreditCardDetails
import com.fluxa.personal.domain.model.CreditResult
import com.fluxa.personal.domain.model.SubscriptionDetails
import com.fluxa.personal.domain.model.SubscriptionFrequency
import com.fluxa.personal.domain.model.SubscriptionOccurrenceStatus
import com.fluxa.personal.domain.model.SubscriptionResult
import com.fluxa.personal.domain.model.SubscriptionSource
import com.fluxa.personal.domain.model.SubscriptionStatus
import com.fluxa.personal.domain.model.TransactionDirection.EXPENSE
import com.fluxa.personal.domain.model.TransactionHistoryFilter
import com.fluxa.personal.domain.rules.CalculatedBalance
import java.time.LocalDate
import java.util.UUID
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Assinaturas de ponta a ponta sobre o Room v6 real. */
@OptIn(ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
class SubscriptionFlowTest {
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

    private fun card(limitCents: Long): UUID = runBlocking {
        (app.credit.createCard(CreditCardDetails("Cartão Roxo", Money(limitCents), BillingDay(10), BillingDay(17))) as CreditResult.Saved).value.id
    }

    private fun subscription(
        name: String, cents: Long, frequency: SubscriptionFrequency = SubscriptionFrequency.MONTHLY,
        anchor: LocalDate = today, source: SubscriptionSource = SubscriptionSource.Account(main),
    ): UUID = runBlocking {
        (app.subscriptions.create(SubscriptionDetails(name, Money(cents), streaming, source), frequency, anchor, BillingDay(anchor.dayOfMonth))
            as SubscriptionResult.Saved).value.id
    }

    private fun balance(): Money = runBlocking { (app.transactions.observeAccountBalance(main).first() as CalculatedBalance.Exact).amount }
    private fun occurrenceStatus(id: UUID, date: LocalDate) = runBlocking { app.subscriptions.observeOccurrence(id, date).first()?.status }
    private fun ledgerFor(id: UUID) = runBlocking {
        app.transactions.observeHistory(TransactionHistoryFilter()).first().filter { it.subscriptionOccurrence?.subscriptionId == id }
    }

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

    private fun openSubscriptions() {
        waitForTag("tab-more")
        compose.onNodeWithTag("tab-more").performClick()
        waitForTag("more-subscriptions")
        compose.onNodeWithTag("more-subscriptions").performClick()
        waitForTag("screen-subscriptions")
    }

    private fun openDetail(name: String) {
        openSubscriptions()
        tap("feature-list", "subscription-$name")
        waitForTag("screen-subscription-detail")
    }

    private fun settleNext() {
        tap("feature-list", "action-open-next")
        waitForTag("screen-occurrence")
        tap("feature-list", "action-settle-occurrence")
        waitForTag("sheet-settle-occurrence")
        compose.onNodeWithTag("action-confirm-settle").performClick()
        compose.waitUntilDoesNotExist(hasTestTag("sheet-settle-occurrence"), 5_000)
    }

    @Test
    fun monthlyAccountSubscriptionCreatedPaidAndShownInCash() {
        val before = balance()
        ActivityScenario.launch(MainActivity::class.java).use {
            openSubscriptions()
            tap("feature-list", "action-new-subscription")
            waitForTag("screen-subscription-form")
            typeInto("field-subscription-name", "Música")
            typeInto("field-subscription-amount", "21,90")
            inList("feature-list", "subscription-category-Streaming")
            compose.onNodeWithTag("subscription-category-Streaming").performClick()
            compose.onNodeWithTag("action-save").performClick()
            waitForTag("screen-subscription-detail")
            waitForText("subscription-frequency", "COBRANÇA MENSAL")
            waitForText("subscription-source", "Conta Principal")
            settleNext()
            waitForText("occurrence-status", "Paga")
            inList("feature-list", "action-view-transaction")
            compose.onNodeWithTag("action-view-transaction").performClick()
            waitForTag("managed-payment")
            waitForText("managed-payment", "Assinatura")
            compose.onNodeWithTag("action-save").assertDoesNotExist()
            inList("feature-list", "action-view-subscription")
        }
        val id = runBlocking { app.subscriptions.observeSubscriptions().first().single().id }
        assertEquals(SubscriptionOccurrenceStatus.PAID, occurrenceStatus(id, today))
        assertEquals(1, ledgerFor(id).size)
        assertEquals(before.cents - 2_190, balance().cents)
    }

    @Test
    fun yearlySubscriptionChargesTheFullAmountOnce() {
        val id = subscription("Antivírus", 60_000, SubscriptionFrequency.YEARLY)
        val before = balance()
        ActivityScenario.launch(MainActivity::class.java).use {
            openDetail("Antivírus")
            waitForText("subscription-frequency", "COBRANÇA ANUAL")
            settleNext()
            waitForText("occurrence-status", "Paga")
        }
        assertEquals(Money(60_000), ledgerFor(id).single().details.amount)
        assertEquals(before.cents - 60_000, balance().cents)
        val year = runBlocking { app.subscriptions.observeOccurrences(today, today.plusYears(1)).first().filter { it.key.subscriptionId == id } }
        assertEquals(1, year.size) // uma única ocorrência anual, sem parcelas mensais
    }

    @Test
    fun cardSubscriptionIsChargedAsPurchaseAndCommitsLimit() {
        val card = card(100_000)
        val id = subscription("Nuvem", 1_000, source = SubscriptionSource.Card(card))
        ActivityScenario.launch(MainActivity::class.java).use {
            openDetail("Nuvem")
            waitForText("subscription-source", "Cartão Roxo")
            settleNext()
            waitForText("occurrence-status", "Cobrada no cartão")
            inList("feature-list", "action-view-purchase")
            compose.onNodeWithTag("action-view-purchase").performClick()
            waitForTag("screen-purchase-detail")
            inList("feature-list", "purchase-from-subscription")
        }
        assertEquals(SubscriptionOccurrenceStatus.CHARGED, occurrenceStatus(id, today))
        assertEquals(Money(1_000), runBlocking { app.credit.observeCard(card).first()!!.committedAmount })
        assertEquals("Nuvem", runBlocking { app.credit.observePurchases(card).first().single().details.title })
        assertTrue(ledgerFor(id).isEmpty()) // no Caixa só quando a parcela for paga
    }

    @Test
    fun insufficientCardLimitKeepsTheOccurrencePending() {
        val card = card(500)
        val id = subscription("Curso", 80_000, source = SubscriptionSource.Card(card))
        ActivityScenario.launch(MainActivity::class.java).use {
            openDetail("Curso")
            tap("feature-list", "action-open-next")
            waitForTag("screen-occurrence")
            tap("feature-list", "action-settle-occurrence")
            waitForTag("sheet-settle-occurrence")
            compose.onNodeWithTag("action-confirm-settle").performClick()
            waitForText("settle-error", "Limite disponível insuficiente para lançar esta assinatura no cartão.")
        }
        assertEquals(SubscriptionOccurrenceStatus.PENDING, occurrenceStatus(id, today))
        assertTrue(runBlocking { app.credit.observePurchases(card).first().isEmpty() })
    }

    @Test
    fun skippingAnOverdueOccurrenceRemovesItFromOverdueAndKeepsHistory() {
        val past = today.minusMonths(1)
        val id = subscription("Jornal", 3_000, anchor = past)
        ActivityScenario.launch(MainActivity::class.java).use {
            openSubscriptions()
            tap("feature-list", "action-agenda")
            waitForTag("screen-subscription-agenda")
            inList("feature-list", "agenda-overdue")
            tap("feature-list", "occurrence-Jornal-$past")
            waitForTag("screen-occurrence")
            waitForText("occurrence-status", "Vencida")
            tap("feature-list", "action-skip-occurrence")
            waitForTag("action-confirm-archive")
            compose.onNodeWithTag("action-confirm-archive").performClick()
            waitForText("occurrence-status", "Pulada")
        }
        assertEquals(SubscriptionOccurrenceStatus.SKIPPED, occurrenceStatus(id, past))
        assertTrue(runBlocking { app.subscriptions.observeOverdueOccurrences().first().isEmpty() })
        assertTrue(ledgerFor(id).isEmpty())
    }

    @Test
    fun pauseAndResumeDoNotGenerateTheIntervalAndCancelIsTerminal() {
        val id = subscription("Música", 2_190)
        ActivityScenario.launch(MainActivity::class.java).use {
            openDetail("Música")
            tap("feature-list", "action-pause")
            waitForTag("action-confirm-archive")
            compose.onNodeWithTag("action-confirm-archive").performClick()
            waitForText("subscription-status", "Pausada")
            waitForText("next-Música", "Sem cobranças enquanto estiver pausada")
            tap("feature-list", "action-resume")
            waitForTag("action-confirm-archive")
            compose.onNodeWithTag("action-confirm-archive").performClick()
            waitForText("subscription-status", "Ativa")
        }
        // Pausada hoje e retomada a partir de amanhã: a cobrança de hoje não volta a existir.
        assertNull(occurrenceStatus(id, today))
        val next = runBlocking { app.subscriptions.observeNextOccurrence(id).first()!! }
        assertTrue(next.key.occurrenceDate > today)

        ActivityScenario.launch(MainActivity::class.java).use {
            openDetail("Música")
            tap("feature-list", "action-cancel-subscription")
            waitForTag("action-confirm-archive")
            compose.onNodeWithTag("action-confirm-archive").performClick()
            waitForText("subscription-status", "Cancelada")
            inList("feature-list", "subscription-cancelled-note")
            compose.onNodeWithTag("action-resume").assertDoesNotExist()
            compose.onNodeWithTag("action-edit-subscription").assertDoesNotExist()
        }
        val sub = runBlocking { app.subscriptions.observeSubscription(id).first()!! }
        assertEquals(SubscriptionStatus.CANCELLED, sub.status)
        assertNull(runBlocking { app.subscriptions.observeNextOccurrence(id).first() })
        assertTrue(runBlocking { app.subscriptions.observeUpcomingOccurrences(400).first().none { it.key.subscriptionId == id } })
    }

    @Test
    fun subscriptionsAndOutcomesSurviveReopening() {
        val id = subscription("Música", 2_190)
        val paused = subscription("Jornal", 3_000)
        runBlocking {
            app.subscriptions.paySubscriptionOccurrence(id, today, today)
            app.subscriptions.pause(paused)
        }
        ActivityScenario.launch(MainActivity::class.java).use { openSubscriptions() }
        ActivityScenario.launch(MainActivity::class.java).use {
            openDetail("Música")
            inList("feature-list", "subscription-history")
            waitForText("subscription-history", "Paga")
        }
        assertEquals(SubscriptionStatus.PAUSED, runBlocking { app.subscriptions.observeSubscription(paused).first()!!.status })
    }
    private fun undoOccurrence() {
        tap("feature-list", "action-undo-occurrence")
        waitForTag("action-confirm-archive")
        compose.onNodeWithTag("action-confirm-archive").performClick()
        compose.waitUntilDoesNotExist(hasTestTag("action-confirm-archive"), 5_000)
    }

    @Test
    fun accountUndoRemovesCashEntryAndReturnsToAgenda() {
        val id = subscription("Música", 2_190)
        val before = balance()
        ActivityScenario.launch(MainActivity::class.java).use {
            openDetail("Música")
            settleNext()
            waitForText("occurrence-status", "Paga")
            assertEquals(before.cents - 2_190, balance().cents)
            tap("feature-list", "action-view-transaction")
            waitForText("managed-payment", "Assinatura")
            tap("feature-list", "action-view-subscription")
            waitForTag("screen-subscription-detail")
            tap("feature-list", "occurrence-Música-$today")
            undoOccurrence()
            waitForText("occurrence-status", "Pendente")
            assertEquals(before, balance())
            assertTrue(ledgerFor(id).isEmpty())
            compose.onNodeWithTag("action-back").performClick()
            waitForTag("screen-subscription-detail")
            tap("feature-list", "action-subscription-agenda")
            waitForTag("screen-subscription-agenda")
            tap("feature-list", "occurrence-Música-$today")
            waitForText("occurrence-status", "Pendente")
            compose.onNodeWithTag("action-back").performClick()
            compose.onNodeWithTag("action-back").performClick()
            waitForTag("screen-subscription-detail")
            compose.onNodeWithTag("action-back").performClick()
            waitForTag("screen-occurrence")
        }
    }

    @Test
    fun cardUndoRemovesPurchaseAndRestoresLimit() {
        val card = card(100_000)
        val id = subscription("Nuvem", 1_000, source = SubscriptionSource.Card(card))
        ActivityScenario.launch(MainActivity::class.java).use {
            openDetail("Nuvem")
            settleNext()
            waitForText("occurrence-status", "Cobrada no cartão")
            assertEquals(Money(1_000), runBlocking { app.credit.observeCard(card).first()!!.committedAmount })
            undoOccurrence()
            waitForText("occurrence-status", "Pendente")
        }
        assertEquals(SubscriptionOccurrenceStatus.PENDING, occurrenceStatus(id, today))
        assertEquals(Money(0), runBlocking { app.credit.observeCard(card).first()!!.committedAmount })
        assertTrue(runBlocking { app.credit.observePurchases(card).first().isEmpty() })
    }

    @Test
    fun paidCardInstallmentMustBeUndoneSeparately() {
        val card = card(100_000)
        subscription("Nuvem", 1_000, source = SubscriptionSource.Card(card))
        ActivityScenario.launch(MainActivity::class.java).use {
            openDetail("Nuvem")
            settleNext()
            tap("feature-list", "action-view-installment")
            waitForTag("screen-installment-detail")
            tap("feature-list", "action-pay-installment")
            waitForTag("sheet-pay-installment")
            compose.onNodeWithTag("action-review-payment").performClick()
            waitForTag("action-confirm-payment")
            compose.onNodeWithTag("action-confirm-payment").performClick()
            waitForText("installment-status", "Paga")
            compose.onNodeWithTag("action-back").performClick()
            waitForTag("screen-occurrence")
            undoOccurrence()
            inList("feature-list", "occurrence-error")
            waitForText("occurrence-error", "Desfaça primeiro o pagamento da parcela")
            tap("feature-list", "action-view-installment")
            waitForTag("screen-installment-detail")
            tap("feature-list", "action-undo-payment")
            waitForTag("action-confirm-archive")
            compose.onNodeWithTag("action-confirm-archive").performClick()
            waitForText("installment-status", "Pendente")
            compose.onNodeWithTag("action-back").performClick()
            waitForTag("screen-occurrence")
            undoOccurrence()
            waitForText("occurrence-status", "Pendente")
        }
    }

    @Test
    fun pausedSubscriptionKeepsItsStatusAfterHistoricalUndo() = historicalUndo(SubscriptionStatus.PAUSED)

    @Test
    fun cancelledSubscriptionKeepsItsStatusAfterHistoricalUndo() = historicalUndo(SubscriptionStatus.CANCELLED)

    private fun historicalUndo(status: SubscriptionStatus) {
        val past = today.minusMonths(1)
        val id = subscription("Jornal", 3_000, anchor = past)
        runBlocking {
            assertTrue(app.subscriptions.paySubscriptionOccurrence(id, past, today) is SubscriptionResult.Saved)
            val changed = if (status == SubscriptionStatus.PAUSED) app.subscriptions.pause(id) else app.subscriptions.cancel(id)
            assertTrue(changed is SubscriptionResult.Saved)
        }
        ActivityScenario.launch(MainActivity::class.java).use {
            openDetail("Jornal")
            tap("feature-list", "occurrence-Jornal-$past")
            undoOccurrence()
            waitForText("occurrence-status", "Vencida")
            compose.onNodeWithTag("action-settle-occurrence").assertDoesNotExist()
        }
        assertEquals(status, runBlocking { app.subscriptions.observeSubscription(id).first()!!.status })
        assertEquals(SubscriptionOccurrenceStatus.OVERDUE, occurrenceStatus(id, past))
    }

}
