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
import com.fluxa.personal.domain.model.CreditCardDetails
import com.fluxa.personal.domain.model.CreditResult
import com.fluxa.personal.domain.model.InstallmentPaymentResult
import com.fluxa.personal.domain.model.InstallmentStatus
import com.fluxa.personal.domain.model.PurchaseDraft
import com.fluxa.personal.domain.model.TransactionDirection.EXPENSE
import com.fluxa.personal.domain.model.TransactionHistoryFilter
import com.fluxa.personal.domain.rules.CalculatedBalance
import java.time.LocalDate
import java.util.UUID
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Sprint 4-D: pagar, desfazer e ver a baixa no Caixa, sobre o Room v5 real. */
@OptIn(ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
class InstallmentPaymentFlowTest {
    @get:Rule
    val compose = createEmptyComposeRule()

    private val app get() = ApplicationProvider.getApplicationContext<FluxaApplication>()
    private val today = FinancialCalendar().today()
    private lateinit var market: UUID
    private lateinit var main: UUID
    private lateinit var card: UUID
    private lateinit var purchase: UUID
    private lateinit var installments: List<UUID>

    @Before
    fun seed() = runBlocking {
        app.database.clearAllTables()
        market = app.categories.create(CategoryDetails("Mercado", EXPENSE, "cart", "#B9684F"))
        main = app.accounts.create(AccountDetails("Conta Principal", AccountType.CHECKING, Money(100_000), LocalDate.of(2020, 1, 1)))
    }

    private fun seedPurchase(limitCents: Long = 500_000, totalCents: Long = 30_000, count: Int = 3) = runBlocking {
        card = (app.credit.createCard(CreditCardDetails("Principal", Money(limitCents), BillingDay(10), BillingDay(17))) as CreditResult.Saved).value.id
        purchase = UUID.randomUUID()
        app.credit.createPurchase(purchase, PurchaseDraft(card, market, "Fone", Money(totalCents), count, today))
        installments = app.credit.observeInstallments(purchase).first().map { it.id }
    }

    private fun payDirectly(index: Int = 0) = runBlocking {
        app.credit.payInstallment(installments[index], main, today) as InstallmentPaymentResult.Paid
    }

    private fun balance(): Money = runBlocking { (app.transactions.observeAccountBalance(main).first() as CalculatedBalance.Exact).amount }
    private fun committed(): Money = runBlocking { app.credit.observeCard(card).first()!!.committedAmount }
    private fun status(index: Int): InstallmentStatus = runBlocking { app.credit.observeInstallments(purchase).first()[index].status }
    private fun paymentRows() = runBlocking { app.transactions.observeHistory(TransactionHistoryFilter()).first().filter { it.installmentId != null } }

    private fun waitForTag(tag: String) = compose.waitUntilAtLeastOneExists(hasTestTag(tag), 5_000)
    private fun waitForNoTag(tag: String) = compose.waitUntilDoesNotExist(hasTestTag(tag), 5_000)

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

    private fun openInstallment(number: Int) {
        waitForTag("tab-cards")
        compose.onNodeWithTag("tab-cards").performClick()
        tap("list-cards", "card-Principal")
        waitForTag("screen-card-detail")
        tap("feature-list", "purchase-Fone")
        waitForTag("screen-purchase-detail")
        tap("feature-list", "installment--$number")
        waitForTag("screen-installment-detail")
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
    fun payingAnInstallmentUpdatesStatusBalanceLimitCashAndDashboard() {
        seedPurchase()
        val before = balance()
        ActivityScenario.launch(MainActivity::class.java).use {
            openInstallment(1)
            waitForText("installment-status", "Pendente")
            tap("feature-list", "action-pay-installment")
            waitForTag("sheet-pay-installment")
            compose.onNodeWithTag("payment-account-Conta Principal").performClick() // única conta já vem escolhida; tocar mantém
            compose.onNodeWithTag("action-review-payment").performClick()
            waitForTag("payment-confirm-summary")
            waitForText("payment-confirm-summary", "Conta Principal")
            compose.onNodeWithTag("action-confirm-payment").performClick()
            waitForNoTag("sheet-pay-installment")
            waitForText("installment-status", "Paga")
            inList("feature-list", "payment-info")
            waitForText("payment-account", "Conta Principal")
            waitForTag("payment-date")
            inList("feature-list", "action-undo-payment")

            backToTab("cash")
            inList("list-cash", "tx-Pagamento • Fone • 1/3")
            waitForText("tx-Pagamento • Fone • 1/3", "Cartão") // marca discreta de origem (árvore não mesclada)
            backToTab("dashboard")
            inList("list-dashboard", "recent-list")
            waitForText("recent-list", "Pagamento • Fone • 1/3")
        }
        assertEquals(InstallmentStatus.PAID, status(0))
        assertEquals(before.cents - 10_000, balance().cents)
        assertEquals(Money(20_000), committed())
        assertEquals(1, paymentRows().size)
    }

    @Test
    fun repeatedPaymentIsIdempotentAndDoesNotDuplicateTheExpense() {
        seedPurchase()
        ActivityScenario.launch(MainActivity::class.java).use {
            openInstallment(1)
            tap("feature-list", "action-pay-installment")
            waitForTag("sheet-pay-installment")
            compose.onNodeWithTag("action-review-payment").performClick()
            waitForTag("action-confirm-payment")
            payDirectly(0) // outra tentativa já registrou a baixa (reenvio/concorrência)
            compose.onNodeWithTag("action-confirm-payment").performClick()
            waitForNoTag("sheet-pay-installment")
            waitForText("installment-status", "Paga")
            compose.onAllNodes(hasTestTag("payment-error")).fetchSemanticsNodes().let { assertTrue(it.isEmpty()) }
        }
        assertEquals(1, paymentRows().size)
        assertEquals(Money(20_000), committed())
    }

    @Test
    fun undoingAPaymentReturnsInstallmentToPendingAndRemovesTheExpense() {
        seedPurchase()
        val before = balance()
        payDirectly(0)
        ActivityScenario.launch(MainActivity::class.java).use {
            openInstallment(1)
            waitForText("installment-status", "Paga")
            tap("feature-list", "action-undo-payment")
            waitForTag("action-confirm-archive")
            compose.onNodeWithTag("action-confirm-archive").performClick()
            waitForText("installment-status", "Pendente")
            inList("feature-list", "action-pay-installment")
            backToTab("cash")
            compose.waitUntilDoesNotExist(hasTestTag("tx-Pagamento • Fone • 1/3"), 5_000)
        }
        assertEquals(InstallmentStatus.PENDING, status(0))
        assertEquals(before, balance())
        assertEquals(Money(30_000), committed())
        assertTrue(paymentRows().isEmpty())
    }

    @Test
    fun undoIsBlockedWhenTheReleasedLimitWasReused() {
        seedPurchase(limitCents = 30_000, totalCents = 30_000, count = 1)
        payDirectly(0)
        runBlocking { app.credit.createPurchase(UUID.randomUUID(), PurchaseDraft(card, market, "Outra", Money(30_000), 1, today)) }
        ActivityScenario.launch(MainActivity::class.java).use {
            openInstallment(1)
            tap("feature-list", "action-undo-payment")
            waitForTag("action-confirm-archive")
            compose.onNodeWithTag("action-confirm-archive").performClick()
            inList("feature-list", "undo-error")
            waitForText("undo-error", "o limite liberado já foi utilizado por outras compras")
            waitForText("installment-status", "Paga")
        }
        assertEquals(InstallmentStatus.PAID, status(0))
        assertEquals(1, paymentRows().size)
    }

    @Test
    fun generatedExpenseOpensReadOnlyFromCash() {
        seedPurchase()
        payDirectly(0)
        ActivityScenario.launch(MainActivity::class.java).use {
            waitForTag("tab-cash")
            compose.onNodeWithTag("tab-cash").performClick()
            tap("list-cash", "tx-Pagamento • Fone • 1/3")
            waitForTag("managed-payment")
            waitForText("managed-payment", "Conta Principal")
            waitForText("managed-payment", "Parcela de cartão de crédito")
            inList("feature-list", "managed-payment-note")
            compose.onNodeWithTag("action-save").assertDoesNotExist()
            compose.onNodeWithTag("action-cancel-transaction").assertDoesNotExist()
        }
        assertNull(paymentRows().single().cancelledAt)
    }

    @Test
    fun paymentAndUndoSurviveReopening() {
        seedPurchase()
        ActivityScenario.launch(MainActivity::class.java).use {
            openInstallment(2)
            tap("feature-list", "action-pay-installment")
            waitForTag("sheet-pay-installment")
            compose.onNodeWithTag("action-review-payment").performClick()
            waitForTag("action-confirm-payment")
            compose.onNodeWithTag("action-confirm-payment").performClick()
            waitForText("installment-status", "Paga")
        }
        ActivityScenario.launch(MainActivity::class.java).use {
            openInstallment(2)
            waitForText("installment-status", "Paga")
            tap("feature-list", "action-undo-payment")
            waitForTag("action-confirm-archive")
            compose.onNodeWithTag("action-confirm-archive").performClick()
            waitForText("installment-status", "Pendente")
        }
        ActivityScenario.launch(MainActivity::class.java).use {
            openInstallment(2)
            waitForText("installment-status", "Pendente")
        }
        assertTrue(paymentRows().isEmpty())
        assertNotNull(installments[1])
    }
}
