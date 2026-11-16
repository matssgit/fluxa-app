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
import com.fluxa.personal.domain.model.CategoryDetails
import com.fluxa.personal.domain.model.CreditCardDetails
import com.fluxa.personal.domain.model.CreditResult
import com.fluxa.personal.domain.model.InstallmentStatus
import com.fluxa.personal.domain.model.PurchaseDraft
import com.fluxa.personal.domain.model.TransactionDirection.EXPENSE
import java.util.UUID
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Sprint 4-B: cartões, compras e parcelas de ponta a ponta sobre o Room v4 real. */
@OptIn(ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
class CreditFlowTest {
    @get:Rule
    val compose = createEmptyComposeRule()

    private val app get() = ApplicationProvider.getApplicationContext<FluxaApplication>()
    private val today = FinancialCalendar().today()
    private lateinit var market: UUID

    @Before
    fun seed() = runBlocking {
        app.database.clearAllTables()
        market = app.categories.create(CategoryDetails("Mercado", EXPENSE, "cart", "#B9684F"))
    }

    private fun createCard(name: String, limitCents: Long): UUID = runBlocking {
        (app.credit.createCard(CreditCardDetails(name, Money(limitCents), BillingDay(10), BillingDay(17))) as CreditResult.Saved).value.id
    }

    private fun waitForTag(tag: String) = compose.waitUntilAtLeastOneExists(hasTestTag(tag), 5_000)
    private fun waitForNoTag(tag: String) = compose.waitUntilDoesNotExist(hasTestTag(tag), 5_000)

    private fun inList(list: String, tag: String) {
        compose.waitForIdle()
        compose.onNodeWithTag(list).performScrollToNode(hasTestTag(tag))
    }

    /** Clique semântico (itens podem estar sob a barra flutuante; ver Sprint 3-B). */
    private fun tap(list: String, tag: String) {
        inList(list, tag)
        val node = compose.onNodeWithTag(tag)
        val target = if (SemanticsActions.OnClick in node.fetchSemanticsNode().config) node
        else compose.onNode(hasClickAction() and hasParent(hasTestTag(tag)))
        target.performSemanticsAction(SemanticsActions.OnClick)
    }

    private fun typeInto(tag: String, text: String) {
        inList("feature-list", tag)
        compose.onNodeWithTag(tag).performTextClearance()
        compose.onNodeWithTag(tag).performTextInput(text)
    }

    /** Texto no nó marcado ou em um descendente (árvore não mesclada: cartões clicáveis mesclam a semântica). */
    private fun waitForText(tag: String, text: String) = compose.waitUntil(5_000) {
        val matcher = hasText(text, substring = true) and (hasTestTag(tag) or hasAnyAncestor(hasTestTag(tag)))
        compose.onAllNodes(matcher, useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()
    }

    private fun openCardsTab() {
        waitForTag("tab-cards")
        compose.onNodeWithTag("tab-cards").performClick()
        waitForTag("screen-cards")
    }

    private fun fillPurchase(amount: String, installments: String, title: String) {
        waitForTag("screen-purchase-form")
        typeInto("field-purchase-amount", amount)
        typeInto("field-purchase-installments", installments)
        inList("feature-list", "purchase-category-Mercado")
        compose.onNodeWithTag("purchase-category-Mercado").performClick()
        typeInto("field-purchase-title", title)
        compose.onNodeWithTag("action-save").performClick()
    }

    @Test
    fun createCardAppearsWithItsLimit() {
        ActivityScenario.launch(MainActivity::class.java).use {
            openCardsTab()
            tap("list-cards", "action-new-card")
            waitForTag("screen-card-form")
            compose.onNodeWithTag("tab-cards").assertDoesNotExist()
            typeInto("field-card-name", "Cartão teste")
            typeInto("field-card-limit", "2.000,00")
            typeInto("field-card-closing", "10")
            typeInto("field-card-due", "17")
            compose.onNodeWithTag("action-save").performClick()
            waitForNoTag("screen-card-form")
            inList("list-cards", "card-Cartão teste")
            waitForText("card-available-Cartão teste", "2.000,00")
            waitForText("card-total-Cartão teste", "2.000,00")
            waitForText("card-committed-Cartão teste", "0,00")
        }
        val summary = runBlocking { app.credit.observeCards().first().single() }
        assertEquals(Money(200_000), summary.totalLimit)
        assertEquals(10, summary.card.details.closingDay.day)
        assertEquals(17, summary.card.details.dueDay.day)
    }

    @Test
    fun purchaseInFullCommitsLimitAndAppearsInTheCard() {
        val cardId = createCard("Principal", 100_000)
        ActivityScenario.launch(MainActivity::class.java).use {
            openCardsTab()
            tap("list-cards", "card-Principal")
            waitForTag("screen-card-detail")
            tap("feature-list", "action-card-new-purchase")
            fillPurchase("150,00", "1", "Livro")
            waitForTag("screen-purchase-detail")
            waitForTag("installment-list")
            compose.onNodeWithTag("action-back").performClick()
            waitForTag("screen-card-detail")
            inList("feature-list", "card-Principal")
            waitForText("card-committed-Principal", "150,00")
            waitForText("card-available-Principal", "850,00")
            inList("feature-list", "purchase-Livro")
        }
        val summary = runBlocking { app.credit.observeCard(cardId).first()!! }
        assertEquals(Money(15_000), summary.committedAmount)
        assertEquals(Money(85_000), summary.availableLimit)
    }

    @Test
    fun installmentPurchaseFromCentralButtonShowsPersistedSchedule() {
        val cardId = createCard("Principal", 500_000)
        ActivityScenario.launch(MainActivity::class.java).use {
            waitForTag("action-new-entry")
            compose.onNodeWithTag("action-new-entry").performClick()
            waitForTag("new-purchase")
            compose.onNodeWithTag("new-purchase").performClick()
            fillPurchase("1.000,00", "3", "Notebook")
            waitForTag("screen-purchase-detail")
            for (n in 1..3) inList("feature-list", "installment--$n")
            waitForText("installment--1", "1/3")
        }
        val purchase = runBlocking { app.credit.observePurchases(cardId).first().single() }
        val parts = runBlocking { app.credit.observeInstallments(purchase.id).first() }
        assertEquals(3, parts.size)
        assertEquals(purchase.details.total.cents, parts.sumOf { it.amount.cents }) // soma visual = total persistido
        assertTrue(parts.all { it.status == InstallmentStatus.PENDING })
    }

    @Test
    fun insufficientLimitIsRejected() {
        val cardId = createCard("Pequeno", 10_000)
        ActivityScenario.launch(MainActivity::class.java).use {
            openCardsTab()
            tap("list-cards", "card-Pequeno")
            waitForTag("screen-card-detail")
            tap("feature-list", "action-card-new-purchase")
            fillPurchase("150,00", "1", "TV")
            // Visível junto ao Salvar mesmo com o campo Valor fora da tela, e também no próprio campo.
            waitForText("purchase-form-error", "Limite disponível insuficiente para esta compra.")
            inList("feature-list", "field-purchase-amount")
            compose.waitUntilAtLeastOneExists(hasText("Limite disponível insuficiente para esta compra.") and hasTestTag("field-purchase-amount"), 5_000)
            compose.onNodeWithTag("screen-purchase-form").assertExists()
        }
        assertTrue(runBlocking { app.credit.observePurchases(cardId).first().isEmpty() })
    }

    @Test
    fun cancelReleasesLimitAndKeepsHistory() {
        val cardId = createCard("Principal", 100_000)
        val purchaseId = UUID.randomUUID()
        runBlocking { app.credit.createPurchase(purchaseId, PurchaseDraft(cardId, market, "Fone", Money(30_000), 3, today)) }
        ActivityScenario.launch(MainActivity::class.java).use {
            openCardsTab()
            tap("list-cards", "card-Principal")
            waitForTag("screen-card-detail")
            waitForText("card-committed-Principal", "300,00")
            tap("feature-list", "purchase-Fone")
            waitForTag("screen-purchase-detail")
            inList("feature-list", "action-cancel-purchase")
            compose.onNodeWithTag("action-cancel-purchase").performClick()
            waitForTag("action-confirm-archive")
            compose.onNodeWithTag("action-confirm-archive").performClick()
            // Em 320dp com fonte 1,3 a nota (topo da lista) sai da composição: espera o botão sumir e rola até ela.
            waitForNoTag("action-cancel-purchase")
            inList("feature-list", "purchase-cancelled-note")
            compose.onNodeWithTag("action-back").performClick()
            waitForTag("screen-card-detail")
            inList("feature-list", "card-Principal")
            waitForText("card-available-Principal", "1.000,00")
            inList("feature-list", "purchase-Fone")
            waitForText("purchase-Fone", "Cancelada")
        }
        assertEquals(Money(0), runBlocking { app.credit.observeCard(cardId).first()!!.committedAmount })
        assertNotNull(runBlocking { app.credit.observePurchases(cardId).first().single().cancelledAt })
    }

    @Test
    fun cardsAndPurchasesSurviveReopening() {
        createCard("Persistente", 100_000)
        ActivityScenario.launch(MainActivity::class.java).use {
            openCardsTab()
            tap("list-cards", "card-Persistente")
            waitForTag("screen-card-detail")
            tap("feature-list", "action-card-new-purchase")
            fillPurchase("40,00", "2", "Assinatura anual")
            waitForTag("screen-purchase-detail")
        }
        ActivityScenario.launch(MainActivity::class.java).use {
            openCardsTab()
            inList("list-cards", "card-Persistente")
            waitForText("card-committed-Persistente", "40,00")
            tap("list-cards", "card-Persistente")
            waitForTag("screen-card-detail")
            inList("feature-list", "purchase-Assinatura anual")
        }
    }
}
