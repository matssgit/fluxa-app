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
import com.fluxa.personal.domain.model.InstallmentStatus
import com.fluxa.personal.domain.model.SafeToSpendState
import com.fluxa.personal.domain.model.SubscriptionOccurrenceStatus
import com.fluxa.personal.domain.model.TransactionDirection.EXPENSE
import com.fluxa.personal.domain.model.TransactionDirection.INCOME
import com.fluxa.personal.domain.rules.CalculatedBalance
import com.fluxa.personal.ui.designsystem.components.FluxaMoneyFormat
import java.time.LocalDate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Sprint 10-B: jornada financeira completa feita só pela interface, num único processo, conferindo que Início, Caixa,
 * Cartões, Assinaturas, Metas e Limite seguro contam a mesma história a cada passo.
 */
@OptIn(ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
class RcFinancialSmokeTest {
    @get:Rule
    val compose = createEmptyComposeRule()

    private val app get() = ApplicationProvider.getApplicationContext<FluxaApplication>()
    private val today = FinancialCalendar().today()

    @Before
    fun seed() = runBlocking {
        app.preferences.apply(app.preferences.read().copy(hideAmounts = false))
        app.database.clearAllTables()
        app.accounts.create(AccountDetails("Conta Principal", AccountType.CHECKING, Money(200_000), LocalDate.of(2020, 1, 1)))
        app.categories.create(CategoryDetails("Mercado", EXPENSE, "cart", "#B9684F"))
        app.categories.create(CategoryDetails("Salário", INCOME, "work", "#35695A"))
        Unit
    }

    private fun brl(cents: Long) = FluxaMoneyFormat.format(Money(cents))
    private fun waitForTag(tag: String) = compose.waitUntilAtLeastOneExists(hasTestTag(tag), 10_000)
    private fun waitForNoTag(tag: String) = compose.waitUntilDoesNotExist(hasTestTag(tag), 10_000)
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
        val matcher = hasText(text, substring = true) and (hasTestTag(tag) or hasAnyAncestor(hasTestTag(tag)))
        compose.onAllNodes(matcher, useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()
    }

    private fun typeInto(tag: String, text: String) {
        inList("feature-list", tag)
        compose.onNodeWithTag(tag).performTextClearance()
        compose.onNodeWithTag(tag).performTextInput(text)
    }

    private fun toTab(tab: String) {
        while (compose.onAllNodes(hasTestTag("tab-$tab")).fetchSemanticsNodes().isEmpty()) {
            compose.onNodeWithTag("action-back").performClick()
            compose.waitForIdle()
        }
        compose.onNodeWithTag("tab-$tab").performClick()
        waitForTag("screen-$tab")
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
        waitForNoTag("screen-transaction-form")
    }

    /** Disponível no Início e no Caixa, e o limite seguro no Início, no detalhe e no contrato. */
    private fun coherent(available: Long) {
        // Sem receitas previstas, o limite é o disponível menos tudo que vence no horizonte (nunca abaixo de zero).
        val horizonEnd = today.plusDays(30)
        val installments = runBlocking {
            app.credit.observeCards().first().flatMap { c -> app.credit.observePurchases(c.card.id).first() }
                .flatMap { app.credit.observeInstallments(it.id).first() }
                .filter { it.status == InstallmentStatus.PENDING && it.dueDate < horizonEnd }.sumOf { it.amount.cents }
        }
        val subscriptions = runBlocking {
            app.subscriptions.observeOccurrences(today, horizonEnd).first()
                .filter { it.status == SubscriptionOccurrenceStatus.PENDING || it.status == SubscriptionOccurrenceStatus.OVERDUE }
                .sumOf { it.details.amount.cents }
        }
        val safe = (available - installments - subscriptions).coerceAtLeast(0)
        val contract = runBlocking { (app.safeToSpend.observeSafeToSpend(30).first() as SafeToSpendState.Ready).result }
        assertEquals(available, contract.availableNow.cents)
        assertEquals(safe, contract.safeAmount.cents)

        toTab("cash")
        inList("list-cash", "available-balance")
        waitForText("available-balance", brl(available))
        toTab("dashboard")
        inList("list-dashboard", "available-balance")
        waitForText("available-balance", brl(available))
        inList("list-dashboard", "safe-to-spend-card")
        waitForText("safe-amount", brl(safe))
        tap("list-dashboard", "action-safe-details")
        waitForTag("safe-hero")
        waitForText("safe-detail-amount", brl(safe))
        inList("feature-list", "safe-how")
        waitForText("safe-available-now", brl(available))
        compose.onNodeWithTag("action-back").performClick()
        waitForTag("list-dashboard")
    }

    @Test
    fun wholeJourneyStaysCoherentAcrossScreens() {
        ActivityScenario.launch(MainActivity::class.java).use {
            waitForTag("tab-dashboard")
            coherent(200_000)

            // Receita e despesa.
            register("new-income", "1.500,00", "category-option-Salário", "Pagamento")
            coherent(350_000)
            register("new-expense", "300,00", "category-option-Mercado", "Feira")
            coherent(320_000)
            toTab("dashboard")
            inList("list-dashboard", "month-summary")
            waitForText("summary-income", brl(150_000))
            waitForText("summary-net", brl(120_000))

            // Compra no cartão: compromete limite, não mexe no saldo.
            toTab("cards")
            tap("list-cards", "action-new-card")
            waitForTag("screen-card-form")
            typeInto("field-card-name", "Roxo")
            typeInto("field-card-limit", "2.000,00")
            typeInto("field-card-closing", "10")
            typeInto("field-card-due", "17")
            compose.onNodeWithTag("action-save").performClick()
            waitForNoTag("screen-card-form")
            tap("list-cards", "card-Roxo")
            waitForTag("screen-card-detail")
            tap("feature-list", "action-card-new-purchase")
            waitForTag("screen-purchase-form")
            typeInto("field-purchase-amount", "600,00")
            typeInto("field-purchase-installments", "2")
            inList("feature-list", "purchase-category-Mercado")
            compose.onNodeWithTag("purchase-category-Mercado").performClick()
            typeInto("field-purchase-title", "Fone")
            compose.onNodeWithTag("action-save").performClick()
            waitForTag("screen-purchase-detail")
            coherent(320_000)
            toTab("cards")
            inList("list-cards", "card-Roxo")
            waitForText("card-committed-Roxo", brl(60_000))

            // Pagar a primeira parcela: sai da conta e libera limite.
            tap("list-cards", "card-Roxo")
            tap("feature-list", "purchase-Fone")
            waitForTag("screen-purchase-detail")
            tap("feature-list", "installment--1")
            waitForTag("screen-installment-detail")
            tap("feature-list", "action-pay-installment")
            click("payment-account-Conta Principal")
            click("action-review-payment")
            click("action-confirm-payment")
            waitForNoTag("sheet-pay-installment")
            coherent(290_000)
            toTab("cards")
            inList("list-cards", "card-Roxo")
            waitForText("card-committed-Roxo", brl(30_000))

            // Assinatura criada e paga.
            toTab("more")
            click("more-subscriptions")
            click("action-new-subscription")
            waitForTag("screen-subscription-form")
            typeInto("field-subscription-name", "Música")
            typeInto("field-subscription-amount", "50,00")
            inList("feature-list", "subscription-category-Mercado")
            compose.onNodeWithTag("subscription-category-Mercado").performClick()
            compose.onNodeWithTag("action-save").performClick()
            waitForTag("screen-subscription-detail")
            coherent(290_000) // criada, ainda não paga: só reduz o limite
            toTab("more")
            click("more-subscriptions")
            tap("feature-list", "subscription-Música")
            waitForTag("screen-subscription-detail")
            tap("feature-list", "action-open-next")
            tap("feature-list", "action-settle-occurrence")
            click("action-confirm-settle")
            waitForNoTag("sheet-settle-occurrence")
            coherent(285_000)

            // Meta com reserva: o saldo total não muda, o disponível e o limite diminuem.
            toTab("more")
            click("more-goals")
            click("action-new-goal")
            waitForTag("screen-goal-form")
            typeInto("field-goal-name", "Viagem")
            typeInto("field-goal-target", "1.000,00")
            compose.onNodeWithTag("action-save").performClick()
            waitForTag("screen-goal-detail")
            tap("feature-list", "action-allocate")
            waitForTag("sheet-goal-movement")
            compose.onNodeWithTag("field-goal-amount").performTextInput("400,00")
            compose.onNodeWithTag("action-confirm-movement").performClick()
            waitForNoTag("sheet-goal-movement")
            inList("feature-list", "goal-summary")
            waitForText("goal-percent", "40%")
            coherent(245_000)
            toTab("dashboard")
            inList("list-dashboard", "available-balance")
            waitForText("reserve-total", brl(285_000))
            waitForText("reserve-reserved-total", brl(40_000))
        }
        val total = runBlocking { app.accounts.observeAll().first().single() }
            .let { runBlocking { app.transactions.observeAccountBalance(it.id).first() as CalculatedBalance.Exact } }
        assertEquals(285_000, total.amount.cents)
    }
}
