package com.fluxa.personal.ui

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assert
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
import com.fluxa.personal.domain.model.PeriodSummary
import com.fluxa.personal.domain.model.TransactionDirection.EXPENSE
import com.fluxa.personal.domain.model.TransactionDirection.INCOME
import com.fluxa.personal.domain.model.TransactionDraft
import com.fluxa.personal.domain.rules.CalculatedBalance
import java.util.UUID
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Sprint 3-D: Início e Caixa sobre os read models do Codex (resumo do período, recentes e histórico paginado
 * com filtros combinados), no Room real. Os valores esperados também são conferidos no repositório.
 */
@OptIn(ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
class ReadModelsFlowTest {
    @get:Rule
    val compose = createEmptyComposeRule()

    private val app get() = ApplicationProvider.getApplicationContext<FluxaApplication>()
    private val today = FinancialCalendar().today()
    private val monthStart = today.withDayOfMonth(1)
    private lateinit var main: UUID
    private lateinit var wallet: UUID
    private lateinit var salary: UUID
    private lateinit var market: UUID

    @Before
    fun seed() = runBlocking {
        app.database.clearAllTables()
        main = app.accounts.create(AccountDetails("Conta teste", AccountType.CHECKING, Money(100_000), monthStart.minusMonths(3)))
        wallet = app.accounts.create(AccountDetails("Carteira teste", AccountType.WALLET, Money(0), monthStart.minusMonths(3)))
        salary = app.categories.create(CategoryDetails("Salário", INCOME, "work", "#35695A"))
        market = app.categories.create(CategoryDetails("Mercado", EXPENSE, "cart", "#B9684F"))
    }

    private fun waitForTag(tag: String) = compose.waitUntilAtLeastOneExists(hasTestTag(tag), 5_000)
    private fun waitForNoTag(tag: String) = compose.waitUntilDoesNotExist(hasTestTag(tag), 5_000)

    private fun inList(list: String, tag: String) {
        compose.waitForIdle()
        compose.onNodeWithTag(list).performScrollToNode(hasTestTag(tag))
    }

    /** Itens podem estar visíveis sob a barra flutuante; aciona o clique semântico do item (ver Sprint 3-B). */
    private fun tap(list: String, tag: String) {
        inList(list, tag)
        val node = compose.onNodeWithTag(tag)
        val target = if (SemanticsActions.OnClick in node.fetchSemanticsNode().config) node
        else compose.onNode(hasClickAction() and hasParent(hasTestTag(tag)))
        target.performSemanticsAction(SemanticsActions.OnClick)
    }

    private fun waitForText(tag: String, text: String) =
        compose.waitUntil(5_000) {
            runCatching { compose.onNodeWithTag(tag).assert(hasText(text, substring = true)); true }.getOrDefault(false)
        }

    private fun register(newTag: String, amount: String, categoryOption: String, description: String) {
        compose.onNodeWithTag("action-new-entry").performClick()
        waitForTag(newTag)
        compose.onNodeWithTag(newTag).performClick()
        waitForTag("screen-transaction-form")
        inList("feature-list", "field-tx-amount")
        compose.onNodeWithTag("field-tx-amount").performTextClearance()
        compose.onNodeWithTag("field-tx-amount").performTextInput(amount)
        inList("feature-list", "account-option-Conta teste")
        compose.onNodeWithTag("account-option-Conta teste").performClick()
        inList("feature-list", categoryOption)
        compose.onNodeWithTag(categoryOption).performClick()
        inList("feature-list", "field-tx-description")
        compose.onNodeWithTag("field-tx-description").performTextInput(description)
        compose.onNodeWithTag("action-save").performClick()
        waitForNoTag("screen-transaction-form")
    }

    private fun summary(): PeriodSummary = runBlocking { app.transactions.observePeriodSummary(monthStart, monthStart.plusMonths(1)).first() }

    @Test
    fun dashboardShowsMonthSummaryAndRecentAndUpdatesWithoutReload() {
        ActivityScenario.launch(MainActivity::class.java).use {
            waitForTag("month-summary")
            inList("list-dashboard", "summary-net")
            waitForText("summary-income", "0,00")
            waitForText("summary-expenses", "0,00")
            waitForText("summary-net", "Neutro")

            // Lançados a partir do Início: o formulário volta para o Início, que se atualiza pelos Flows.
            register("new-income", "1.000,00", "category-option-Salário", "Salário UI")
            register("new-expense", "250,50", "category-option-Mercado", "Feira UI")
            waitForTag("screen-dashboard")

            assertEquals(
                PeriodSummary(CalculatedBalance.Exact(Money(100_000)), CalculatedBalance.Exact(Money(25_050)), CalculatedBalance.Exact(Money(74_950))),
                summary(),
            )
            compose.waitUntilAtLeastOneExists(hasText("1.749,50", substring = true), 5_000) // saldo disponível
            inList("list-dashboard", "summary-net")
            waitForText("summary-income", "1.000,00")
            waitForText("summary-expenses", "250,50")
            waitForText("summary-net", "749,50")
            waitForText("summary-net", "Positivo")
            inList("list-dashboard", "tx-Feira UI")
            inList("list-dashboard", "tx-Salário UI")
        }
    }

    @Test
    fun negativeMonthIsShownWithSignAndText() {
        runBlocking {
            app.transactions.create(UUID.randomUUID(), TransactionDraft(INCOME, main, salary, Money(10_000), today, "Pouco"))
            app.transactions.create(UUID.randomUUID(), TransactionDraft(EXPENSE, main, market, Money(30_000), today, "Muito"))
        }
        ActivityScenario.launch(MainActivity::class.java).use {
            waitForTag("month-summary")
            inList("list-dashboard", "summary-net")
            waitForText("summary-net", "200,00")
            waitForText("summary-net", "Negativo")
        }
    }

    @Test
    fun cashCombinesDirectionAccountAndPeriodThroughTheQuery() {
        runBlocking {
            fun draft(d: com.fluxa.personal.domain.model.TransactionDirection, acc: UUID, cents: Long, day: java.time.LocalDate, desc: String) =
                TransactionDraft(d, acc, if (d == INCOME) salary else market, Money(cents), day, desc)
            app.transactions.create(UUID.randomUUID(), draft(INCOME, main, 300_000, today, "Pagamento"))
            app.transactions.create(UUID.randomUUID(), draft(EXPENSE, main, 20_000, today, "Feira"))
            app.transactions.create(UUID.randomUUID(), draft(EXPENSE, wallet, 1_500, today, "Padaria"))
            app.transactions.create(UUID.randomUUID(), draft(INCOME, wallet, 2_000, today, "Troco"))
            app.transactions.create(UUID.randomUUID(), draft(INCOME, main, 7_000, monthStart.minusDays(5), "Bônus anterior"))
            app.transactions.create(UUID.randomUUID(), draft(EXPENSE, main, 5_000, monthStart.minusDays(6), "Gasto anterior"))
        }
        ActivityScenario.launch(MainActivity::class.java).use {
            waitForTag("tab-cash")
            compose.onNodeWithTag("tab-cash").performClick()
            waitForTag("available-balance")
            inList("list-cash", "history-count")
            compose.waitUntilAtLeastOneExists(hasText("4 movimentações"), 5_000)

            tap("list-cash", "direction-filter-INCOME")
            compose.waitUntilAtLeastOneExists(hasText("2 movimentações"), 5_000)
            inList("list-cash", "tx-Pagamento")
            inList("list-cash", "tx-Troco")
            compose.onNodeWithTag("tx-Feira").assertDoesNotExist()

            tap("list-cash", "direction-filter-EXPENSE")
            tap("list-cash", "balance-Carteira teste")
            compose.waitUntilAtLeastOneExists(hasText("1 movimentação"), 5_000)
            inList("list-cash", "tx-Padaria")
            compose.onNodeWithTag("tx-Feira").assertDoesNotExist()

            tap("list-cash", "clear-account-filter")
            tap("list-cash", "direction-filter-INCOME")
            tap("list-cash", "period-LAST_MONTH")
            compose.waitUntilAtLeastOneExists(hasText("1 movimentação"), 5_000)
            inList("list-cash", "tx-Bônus anterior")
            compose.onNodeWithTag("tx-Gasto anterior").assertDoesNotExist()
        }
    }

    @Test
    fun cashAndDashboardReactToANewEntryWithoutReload() {
        ActivityScenario.launch(MainActivity::class.java).use {
            waitForTag("tab-cash")
            compose.onNodeWithTag("tab-cash").performClick()
            waitForTag("available-balance")
            register("new-expense", "42,00", "category-option-Mercado", "Reativo")
            waitForTag("screen-cash")
            compose.waitUntilAtLeastOneExists(hasText("958,00", substring = true), 5_000)
            inList("list-cash", "tx-Reativo")

            compose.onNodeWithTag("tab-dashboard").performClick()
            waitForTag("month-summary")
            inList("list-dashboard", "summary-expenses")
            waitForText("summary-expenses", "42,00")
            inList("list-dashboard", "tx-Reativo")
        }
    }
}
