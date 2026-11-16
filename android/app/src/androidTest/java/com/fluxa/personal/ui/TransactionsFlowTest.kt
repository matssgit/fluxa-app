package com.fluxa.personal.ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasParent
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performScrollToNode
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
import com.fluxa.personal.domain.model.TransactionDirection.EXPENSE
import com.fluxa.personal.domain.model.TransactionDirection.INCOME
import com.fluxa.personal.domain.model.TransactionDraft
import com.fluxa.personal.domain.model.TransactionResult
import com.fluxa.personal.domain.rules.CalculatedBalance
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

/**
 * Lançamentos de ponta a ponta sobre o Room real: criar receita e despesa pelo botão central, editar, cancelar,
 * filtrar o histórico e reabrir o app. Os saldos conferidos vêm do repositório, não da UI.
 */
@OptIn(ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
class TransactionsFlowTest {
    @get:Rule
    val compose = createEmptyComposeRule()

    private val app get() = ApplicationProvider.getApplicationContext<FluxaApplication>()
    private val today = FinancialCalendar().today()
    private lateinit var account: UUID
    private lateinit var market: UUID
    private lateinit var salary: UUID

    @Before
    fun seed() = runBlocking {
        app.database.clearAllTables()
        account = app.accounts.create(AccountDetails("Conta teste", AccountType.CHECKING, Money(100_000), today.minusDays(60)))
        market = app.categories.create(CategoryDetails("Mercado", EXPENSE, "cart", "#B9684F"))
        salary = app.categories.create(CategoryDetails("Salário", INCOME, "work", "#35695A"))
    }

    private fun waitForTag(tag: String) = compose.waitUntilAtLeastOneExists(hasTestTag(tag), 5_000)
    private fun waitForNoTag(tag: String) = compose.waitUntilDoesNotExist(hasTestTag(tag), 5_000)

    private fun inForm(tag: String) {
        compose.waitForIdle()
        compose.onNodeWithTag("feature-list").performScrollToNode(hasTestTag(tag))
    }

    private fun inCash(tag: String) {
        compose.waitForIdle()
        compose.onNodeWithTag("list-cash").performScrollToNode(hasTestTag(tag))
    }

    /**
     * Itens da lista do Caixa podem ficar visíveis porém sob a barra flutuante (conteúdo rola por trás dela, com
     * folga inferior para o usuário). Um toque por coordenada cairia na barra; aqui acionamos o clique semântico
     * do próprio item, que é o que o TalkBack e o usuário ativam após rolar.
     */
    private fun tapInCash(tag: String) {
        inCash(tag)
        val node = compose.onNodeWithTag(tag)
        // FluxaCard leva a tag na superfície e o clique na coluna interna; demais itens têm o clique no próprio nó.
        val target = if (SemanticsActions.OnClick in node.fetchSemanticsNode().config) node
        else compose.onNode(hasClickAction() and hasParent(hasTestTag(tag)))
        target.performSemanticsAction(SemanticsActions.OnClick)
    }

    private fun typeInto(tag: String, text: String) {
        inForm(tag)
        compose.onNodeWithTag(tag).performTextClearance()
        compose.onNodeWithTag(tag).performTextInput(text)
    }

    private fun available(): CalculatedBalance = runBlocking { app.transactions.observeAvailableBalance().first() }

    private fun openCash() {
        waitForTag("tab-cash")
        compose.onNodeWithTag("tab-cash").performClick()
        waitForTag("available-balance")
    }

    private fun registerViaCentralButton(newTag: String, amount: String, categoryOption: String, description: String) {
        compose.onNodeWithTag("action-new-entry").performClick()
        waitForTag(newTag)
        compose.onNodeWithTag(newTag).performClick()
        waitForTag("screen-transaction-form")
        compose.onNodeWithTag("tab-cash").assertDoesNotExist() // barra flutuante oculta no formulário
        typeInto("field-tx-amount", amount)
        inForm(categoryOption)
        compose.onNodeWithTag(categoryOption).performClick()
        typeInto("field-tx-description", description)
        compose.onNodeWithTag("action-save").performClick()
        waitForNoTag("screen-transaction-form")
        waitForTag("feedback-banner")
    }

    @Test
    fun createExpenseAndIncomeFromCentralButtonAndBalancesFollow() {
        ActivityScenario.launch(MainActivity::class.java).use {
            openCash()
            compose.waitUntilAtLeastOneExists(hasText("1.000,00", substring = true), 5_000)

            registerViaCentralButton("new-expense", "250,50", "category-option-Mercado", "Feira")
            assertEquals(CalculatedBalance.Exact(Money(74_950)), available())
            compose.waitUntilAtLeastOneExists(hasText("749,50", substring = true), 5_000)
            inCash("tx-Feira")
            compose.onNodeWithTag("tx-Feira").assertIsDisplayed()

            registerViaCentralButton("new-income", "1.000,00", "category-option-Salário", "Pagamento")
            assertEquals(CalculatedBalance.Exact(Money(174_950)), available())
            compose.waitUntilAtLeastOneExists(hasText("1.749,50", substring = true), 5_000)
            inCash("balance-Conta teste")
            compose.onNodeWithTag("balance-Conta teste").assertIsDisplayed()

            val rows = runBlocking { app.transactions.observeHistory().first() }
            assertEquals(setOf(EXPENSE, INCOME), rows.map { it.details.direction }.toSet())
            assertTrue(rows.all { it.details.amount.cents > 0 })
        }
    }

    @Test
    fun centralSheetCanBeDismissedWithoutChangingDestination() {
        ActivityScenario.launch(MainActivity::class.java).use {
            openCash()
            compose.onNodeWithTag("action-new-entry").performClick()
            waitForTag("new-expense")
            compose.onNode(hasText("Fechar")).performClick()
            waitForNoTag("new-expense")
            compose.onNodeWithTag("screen-cash").assertIsDisplayed()
            assertEquals(0, runBlocking { app.transactions.observeHistory().first().size })
        }
    }

    @Test
    fun editThenCancelKeepsHistoryAndRestoresBalance() {
        val id = UUID.randomUUID()
        val created = runBlocking { app.transactions.create(id, TransactionDraft(EXPENSE, account, market, Money(10_000), today, "Lançado")) }
        assertTrue(created is TransactionResult.Saved)
        ActivityScenario.launch(MainActivity::class.java).use {
            openCash()
            tapInCash("tx-Lançado")
            waitForTag("screen-transaction-form")
            compose.waitUntilAtLeastOneExists(hasText("100,00"), 5_000)
            typeInto("field-tx-amount", "150,00")
            compose.onNodeWithTag("action-save").performClick()
            waitForTag("feedback-banner")
            assertEquals(CalculatedBalance.Exact(Money(85_000)), available())
            compose.waitUntilAtLeastOneExists(hasText("850,00", substring = true), 5_000)

            tapInCash("tx-Lançado")
            waitForTag("screen-transaction-form")
            inForm("action-cancel-transaction")
            compose.onNodeWithTag("action-cancel-transaction").performClick()
            waitForTag("action-confirm-archive")
            compose.onNodeWithTag("action-confirm-archive").performClick()
            waitForNoTag("screen-transaction-form")
            assertEquals(CalculatedBalance.Exact(Money(100_000)), available())

            inCash("tx-Lançado")
            compose.onNode(hasText("Cancelado")).assertIsDisplayed()
            val row = runBlocking { app.transactions.observe(id).first() }
            assertNotNull(row?.cancelledAt)

            // Cancelado abre só para leitura.
            tapInCash("tx-Lançado")
            waitForTag("cancelled-note")
            compose.onNodeWithTag("action-save").assertDoesNotExist()
        }
    }

    @Test
    fun historyFiltersByDirectionPeriodAndAccount() {
        runBlocking {
            val other = app.accounts.create(AccountDetails("Outra", AccountType.WALLET, Money(0), today.minusDays(400)))
            app.transactions.create(UUID.randomUUID(), TransactionDraft(EXPENSE, account, market, Money(1_000), today, "Mercadinho"))
            app.transactions.create(UUID.randomUUID(), TransactionDraft(INCOME, account, salary, Money(5_000), today, "Salário mês"))
            app.transactions.create(UUID.randomUUID(), TransactionDraft(EXPENSE, other, market, Money(700), today, "Na outra"))
            app.transactions.create(UUID.randomUUID(), TransactionDraft(EXPENSE, account, market, Money(300), today.withDayOfMonth(1).minusDays(3), "Mês passado"))
        }
        ActivityScenario.launch(MainActivity::class.java).use {
            openCash()
            inCash("history-count")
            compose.waitUntilAtLeastOneExists(hasText("3 movimentações"), 5_000)

            tapInCash("direction-filter-INCOME")
            compose.waitUntilAtLeastOneExists(hasText("1 movimentação"), 5_000)
            inCash("tx-Salário mês")
            compose.onNodeWithTag("tx-Mercadinho").assertDoesNotExist()

            tapInCash("direction-filter-ALL")
            tapInCash("balance-Outra")
            compose.waitUntilAtLeastOneExists(hasText("1 movimentação"), 5_000)
            inCash("tx-Na outra")
            tapInCash("clear-account-filter")

            tapInCash("period-LAST_MONTH")
            compose.waitUntilAtLeastOneExists(hasText("1 movimentação"), 5_000)
            inCash("tx-Mês passado")
            compose.onNodeWithTag("tx-Mês passado").assertIsDisplayed()
        }
    }

    @Test
    fun ledgerSurvivesClosingAndReopeningTheApp() {
        ActivityScenario.launch(MainActivity::class.java).use {
            openCash()
            registerViaCentralButton("new-expense", "42,00", "category-option-Mercado", "Persistente")
        }
        ActivityScenario.launch(MainActivity::class.java).use {
            waitForTag("screen-dashboard")
            compose.waitUntilAtLeastOneExists(hasText("958,00", substring = true), 5_000) // Início: saldo disponível real
            openCash()
            inCash("tx-Persistente")
            compose.onNodeWithTag("tx-Persistente").assertIsDisplayed()
        }
        assertEquals(CalculatedBalance.Exact(Money(95_800)), available())
    }
}
