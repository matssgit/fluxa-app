package com.fluxa.personal.ui

import android.graphics.Bitmap
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasScrollToNodeAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.fluxa.personal.FluxaApplication
import com.fluxa.personal.MainActivity
import com.fluxa.personal.core.money.Money
import com.fluxa.personal.domain.model.AccountDetails
import com.fluxa.personal.domain.model.AccountType
import com.fluxa.personal.domain.model.CategoryDetails
import com.fluxa.personal.domain.model.TransactionDirection.EXPENSE
import com.fluxa.personal.domain.model.TransactionDirection.INCOME
import java.io.File
import java.time.LocalDate
import kotlinx.coroutines.runBlocking
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Captura de tela para revisão visual. Não roda no `connectedDebugAndroidTest` comum: só executa com
 * `-e fluxaCapture <sufixo>`, depois de configurar largura, tema e fonte do aparelho via adb.
 * Salva PNGs em /sdcard/Android/data/com.fluxa.personal/files/captures.
 */
@OptIn(ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
class VisualReviewCapture {
    @get:Rule
    val compose = createEmptyComposeRule()

    private val suffix: String? = InstrumentationRegistry.getArguments().getString("fluxaCapture")
    private val app get() = ApplicationProvider.getApplicationContext<FluxaApplication>()

    @Test
    fun captureAccountsAndCategories() {
        assumeTrue("Captura visual só roda sob demanda", suffix != null)
        seed()
        ActivityScenario.launch(MainActivity::class.java).use {
            wait("tab-cash")
            compose.onNodeWithTag("tab-cash").performClick()
            wait("screen-cash")
            shot("caixa")
            compose.onNodeWithTag("list-cash").performScrollToNode(hasTestTag("action-manage-accounts"))
            compose.onNodeWithTag("action-manage-accounts").performClick()
            wait("screen-accounts")
            scrollTo("toggle-archived-accounts")
            shot("contas")
            compose.onNodeWithTag("toggle-archived-accounts").performClick()
            scrollTo("account-Cheque especial")
            shot("contas-arquivadas")
            scrollTo("account-Conta principal")
            compose.onNodeWithTag("account-Conta principal").performClick()
            wait("screen-account-form")
            shot("conta-editar")
            compose.onNodeWithTag("action-back").performClick()
            compose.onNodeWithTag("action-new-account").performClick()
            wait("screen-account-form")
            compose.onNodeWithTag("action-save").performClick()
            shot("conta-nova-erro")
            compose.onNodeWithTag("action-back").performClick()
            wait("screen-accounts")
            compose.onNodeWithTag("action-back").performClick()

            compose.onNodeWithTag("tab-more").performClick()
            wait("more-categories")
            shot("mais")
            compose.onNodeWithTag("more-categories").performClick()
            wait("category-Mercado")
            shot("categorias-despesas")
            compose.onNodeWithTag("kind-INCOME").performClick()
            wait("category-Salário")
            shot("categorias-receitas")
            compose.onNodeWithTag("kind-EXPENSE").performClick()
            wait("category-Mercado")
            compose.onNodeWithTag("category-Mercado").performClick()
            wait("screen-category-form")
            shot("categoria-editar")
            scrollTo("action-archive")
            compose.onNodeWithTag("action-archive").performClick()
            wait("action-confirm-archive")
            shot("categoria-arquivar")
        }
    }

    @Test
    fun captureLedger() {
        assumeTrue("Captura visual só roda sob demanda", suffix != null)
        val cancelledId = seedLedger()
        ActivityScenario.launch(MainActivity::class.java).use {
            wait("screen-dashboard")
            wait("available-balance")
            shot("3b-inicio")
            compose.onNodeWithTag("tab-cash").performClick()
            wait("available-balance")
            shot("3b-caixa")
            scrollTo("tx-Projeto de site")
            shot("3b-caixa-historico")
            compose.onNodeWithTag("action-new-entry").performClick()
            wait("new-expense")
            shot("3b-sheet-novo")
            compose.onNodeWithTag("new-expense").performClick()
            wait("screen-transaction-form")
            shot("3b-despesa-nova")
            compose.onNodeWithTag("action-save").performClick()
            compose.waitForIdle()
            shot("3b-despesa-erros")
            compose.onNodeWithTag("action-back").performClick()
            wait("screen-cash")
            scrollTo("tx-Feira do sábado")
            clickSemantic("tx-Feira do sábado")
            wait("screen-transaction-form")
            compose.waitUntilAtLeastOneExists(androidx.compose.ui.test.hasText("182,40"), 5_000)
            shot("3b-lancamento-editar")
            scrollTo("action-cancel-transaction")
            compose.onNodeWithTag("action-cancel-transaction").performClick()
            wait("action-confirm-archive")
            shot("3b-lancamento-cancelar")
            compose.onNodeWithTag("action-confirm-archive").performClick()
            wait("feedback-banner")
            shot("3b-caixa-confirmacao")
            scrollTo("tx-Assinatura cancelada")
            clickSemantic("tx-Assinatura cancelada")
            wait("cancelled-note")
            shot("3b-lancamento-cancelado")
            compose.onNodeWithTag("action-back").performClick()
            wait("screen-cash")
            compose.onNodeWithTag("list-cash").performScrollToIndex(0)
            compose.onNodeWithTag("toggle-amounts").performClick()
            compose.waitForIdle()
            shot("3b-caixa-oculto")
            compose.onNodeWithTag("toggle-amounts").performClick()
        }
        check(cancelledId.toString().isNotEmpty())
    }

    private fun clickSemantic(tag: String) {
        compose.onNodeWithTag(tag).performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.OnClick)
    }

    private fun seedLedger(): java.util.UUID = runBlocking {
        app.database.clearAllTables()
        val today = com.fluxa.personal.core.date.FinancialCalendar().today()
        val accounts = app.accounts
        val main = accounts.create(AccountDetails("Conta principal", AccountType.CHECKING, Money(245_090), today.minusDays(40)))
        val wallet = accounts.create(AccountDetails("Carteira", AccountType.WALLET, Money(12_000), today.minusDays(40)))
        accounts.create(AccountDetails("Reserva", AccountType.SAVINGS, Money(800_000), today.minusDays(40), includeInAvailable = false))
        val categories = app.categories
        val market = categories.create(CategoryDetails("Mercado", EXPENSE, "cart", "#B9684F"))
        val home = categories.create(CategoryDetails("Casa", EXPENSE, "home", "#829B88"))
        val subs = categories.create(CategoryDetails("Assinaturas", EXPENSE, "subscription", "#8A6A84"))
        val salary = categories.create(CategoryDetails("Salário", INCOME, "work", "#35695A"))
        val freela = categories.create(CategoryDetails("Freelas", INCOME, "gift", "#C9A66B"))
        val tx = app.transactions
        fun draft(d: com.fluxa.personal.domain.model.TransactionDirection, acc: java.util.UUID, cat: java.util.UUID, cents: Long, daysAgo: Long, desc: String?) =
            com.fluxa.personal.domain.model.TransactionDraft(d, acc, cat, Money(cents), today.minusDays(daysAgo), desc)
        tx.create(java.util.UUID.randomUUID(), draft(INCOME, main, salary, 520_000, 0, "Salário"))
        tx.create(java.util.UUID.randomUUID(), draft(EXPENSE, main, market, 18_240, 0, "Feira do sábado"))
        tx.create(java.util.UUID.randomUUID(), draft(EXPENSE, wallet, market, 3_500, 1, "Padaria"))
        tx.create(java.util.UUID.randomUUID(), draft(EXPENSE, main, home, 145_000, 2, "Aluguel"))
        tx.create(java.util.UUID.randomUUID(), draft(INCOME, main, freela, 80_000, 3, "Projeto de site"))
        val cancelled = java.util.UUID.randomUUID()
        tx.create(cancelled, draft(EXPENSE, main, subs, 5_590, 3, "Assinatura cancelada"))
        tx.cancel(cancelled)
        cancelled
    }

    private fun wait(tag: String) = compose.waitUntilAtLeastOneExists(hasTestTag(tag), 5_000)

    /** Com fonte ampliada, itens podem estar fora da área composta da LazyColumn. */
    private fun scrollTo(tag: String) {
        compose.waitForIdle()
        compose.onNode(hasScrollToNodeAction()).performScrollToNode(hasTestTag(tag))
        wait(tag)
    }

    private fun shot(name: String) {
        compose.waitForIdle()
        Thread.sleep(600)
        val bitmap = InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
        val dir = File(app.getExternalFilesDir(null), "captures").apply { mkdirs() }
        File(dir, "$name-$suffix.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    private fun seed() = runBlocking {
        app.database.clearAllTables()
        val accounts = app.accounts
        val date = LocalDate.of(2026, 11, 1)
        accounts.create(AccountDetails("Conta principal", AccountType.CHECKING, Money(245_090), date))
        accounts.create(AccountDetails("Carteira", AccountType.WALLET, Money(0), date))
        accounts.create(AccountDetails("Reserva de emergência", AccountType.SAVINGS, Money(800_000), date, includeInAvailable = false))
        val overdraft = accounts.create(AccountDetails("Cheque especial", AccountType.CHECKING, Money(-32_015), date))
        accounts.archive(overdraft)

        val categories = app.categories
        categories.create(CategoryDetails("Mercado", EXPENSE, "cart", "#B9684F"))
        categories.create(CategoryDetails("Casa", EXPENSE, "home", "#829B88"))
        categories.create(CategoryDetails("Transporte", EXPENSE, "transport", "#4E8C86"))
        categories.create(CategoryDetails("Assinaturas", EXPENSE, "subscription", null))
        val gym = categories.create(CategoryDetails("Academia", EXPENSE, "health", "#8A6A84"))
        categories.archive(gym)
        categories.create(CategoryDetails("Salário", INCOME, "work", "#35695A"))
        categories.create(CategoryDetails("Freelas", INCOME, "gift", "#C9A66B"))
    }
}
