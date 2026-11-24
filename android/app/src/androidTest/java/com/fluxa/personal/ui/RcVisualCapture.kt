package com.fluxa.personal.ui

import android.graphics.Bitmap
import android.util.Log
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasParent
import androidx.compose.ui.test.hasScrollToNodeAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.fluxa.personal.FluxaApplication
import com.fluxa.personal.MainActivity
import com.fluxa.personal.backup.PortableSettingsV1
import com.fluxa.personal.core.date.BillingDay
import com.fluxa.personal.core.date.FinancialCalendar
import com.fluxa.personal.core.money.Money
import com.fluxa.personal.domain.model.AccountDetails
import com.fluxa.personal.domain.model.AccountType
import com.fluxa.personal.domain.model.CategoryDetails
import com.fluxa.personal.domain.model.CreditCardDetails
import com.fluxa.personal.domain.model.CreditResult
import com.fluxa.personal.domain.model.GoalDetails
import com.fluxa.personal.domain.model.GoalResult
import com.fluxa.personal.domain.model.PurchaseDraft
import com.fluxa.personal.domain.model.SubscriptionDetails
import com.fluxa.personal.domain.model.SubscriptionFrequency
import com.fluxa.personal.domain.model.SubscriptionResult
import com.fluxa.personal.domain.model.SubscriptionSource
import com.fluxa.personal.domain.model.TransactionDirection
import com.fluxa.personal.domain.model.TransactionDirection.EXPENSE
import com.fluxa.personal.domain.model.TransactionDirection.INCOME
import com.fluxa.personal.domain.model.TransactionDraft
import com.fluxa.personal.security.SecuritySettings
import java.io.File
import java.time.LocalDate
import java.util.UUID
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Sprint 10-B: percorre o app inteiro com dados sintéticos e captura cada tela (topo e continuação).
 * Só roda com `-e fluxaCapture <sufixo>`; `-e fluxaTheme LIGHT|DARK`, `-e fluxaHide 1` e `-e fluxaEmpty 1` são opcionais.
 * PNGs em /sdcard/Android/data/com.fluxa.personal/files/captures.
 */
@RunWith(AndroidJUnit4::class)
class RcVisualCapture {
    @get:Rule val compose = createEmptyComposeRule()
    private val args get() = InstrumentationRegistry.getArguments()
    private val suffix: String? = args.getString("fluxaCapture")
    private val app get() = ApplicationProvider.getApplicationContext<FluxaApplication>()
    private val today = FinancialCalendar().today()
    private val failures = mutableListOf<String>()

    @After fun reset() {
        if (suffix != null) runBlocking { app.preferences.apply(PortableSettingsV1("SYSTEM", false)) }
    }

    private fun exists(tag: String) = compose.onAllNodes(hasTestTag(tag), useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()
    private fun wait(tag: String) = compose.waitUntil(10_000) { exists(tag) }

    private fun tap(tag: String) {
        compose.waitForIdle()
        if (!exists(tag)) {
            try { compose.waitUntil(2_000) { exists(tag) } } catch (_: Throwable) {
                compose.onAllNodes(hasScrollToNodeAction()).onFirst().performScrollToNode(hasTestTag(tag))
            }
        }
        wait(tag)
        val node = compose.onNodeWithTag(tag)
        val target = if (SemanticsActions.OnClick in node.fetchSemanticsNode().config) node
        else compose.onNode(hasClickAction() and hasParent(hasTestTag(tag)))
        target.performSemanticsAction(SemanticsActions.OnClick)
        compose.waitForIdle()
    }

    private fun openNewEntry() {
        compose.onAllNodes(hasContentDescription("Novo lançamento")).onFirst().performSemanticsAction(SemanticsActions.OnClick)
        wait("new-expense")
    }

    private fun shot(name: String) {
        compose.waitForIdle()
        Thread.sleep(550)
        val bitmap = InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot() ?: return
        val dir = File(app.getExternalFilesDir(null), "captures").apply { mkdirs() }
        File(dir, "$name-$suffix.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    /** Topo e mais [pages] telas para baixo. */
    private fun page(name: String, pages: Int = 1) {
        shot("$name-0")
        repeat(pages) { i ->
            val nodes = compose.onAllNodes(hasScrollToNodeAction())
            if (nodes.fetchSemanticsNodes().isEmpty()) return
            nodes.onFirst().performTouchInput { swipeUp(startY = height * 0.82f, endY = height * 0.12f, durationMillis = 900) }
            shot("$name-${i + 1}")
        }
    }

    private fun section(name: String, body: () -> Unit) {
        try {
            ActivityScenario.launch(MainActivity::class.java).use {
                wait("tab-dashboard")
                body()
            }
        } catch (e: Throwable) {
            Log.w("RcCapture", "section $name failed", e)
            failures += "$name: ${e.message?.take(160)}"
        }
    }

    private fun more(tag: String, screen: String) { tap("tab-more"); tap(tag); wait(screen) }
    private fun openCard() { tap("tab-cards"); tap("card-Cartão Roxo"); wait("screen-card-detail") }
    private fun openNotebook() { openCard(); tap("purchase-Notebook"); wait("screen-purchase-detail") }

    private fun seed() = runBlocking {
        app.database.clearAllTables()
        val market = app.categories.create(CategoryDetails("Mercado", EXPENSE, "cart", "#4F6B8A"))
        val home = app.categories.create(CategoryDetails("Casa", EXPENSE, "home", "#B9684F"))
        val stream = app.categories.create(CategoryDetails("Assinaturas", EXPENSE, "subscription", "#6B5B8A"))
        val salary = app.categories.create(CategoryDetails("Salário", INCOME, "work", "#3F7A5E"))
        val opening = LocalDate.of(2020, 1, 1)
        val main = app.accounts.create(AccountDetails("Conta Principal", AccountType.CHECKING, Money(420_000), opening))
        val savings = app.accounts.create(AccountDetails("Poupança da família", AccountType.SAVINGS, Money(90_000), opening))
        app.accounts.create(AccountDetails("Carteira", AccountType.WALLET, Money(18_550), opening, includeInAvailable = false))
        app.accounts.archive(app.accounts.create(AccountDetails("Conta antiga", AccountType.CHECKING, Money(0), opening)))
        fun tx(d: TransactionDirection, acc: UUID, cat: UUID, cents: Long, day: LocalDate, text: String): UUID {
            val id = UUID.randomUUID()
            runBlocking { app.transactions.create(id, TransactionDraft(d, acc, cat, Money(cents), day, text)) }
            return id
        }
        tx(INCOME, main, salary, 650_000, today.minusDays(4), "Salário")
        tx(EXPENSE, main, market, 28_790, today, "Feira do sábado")
        tx(EXPENSE, main, home, 18_640, today.minusDays(1), "Conta de luz")
        app.transactions.cancel(tx(EXPENSE, main, market, 9_900, today.minusDays(2), "Compra duplicada"))
        repeat(6) { tx(EXPENSE, main, market, 4_300L + it * 731, today.minusDays(3L + it), "Padaria") }
        fun goal(name: String, cents: Long, account: UUID, date: LocalDate? = null): UUID = runBlocking {
            (app.goals.createGoal(GoalDetails(name, Money(cents), account, date)) as GoalResult.Saved).value.id
        }
        val trip = goal("Viagem em família", 500_000, main, today.plusMonths(8))
        app.goals.allocateToGoal(trip, Money(150_000))
        app.goals.releaseFromGoal(trip, Money(20_000))
        val fridge = goal("Geladeira nova", 60_000, savings)
        app.goals.allocateToGoal(fridge, Money(60_000))
        val done = goal("Reserva de emergência", 20_000, main)
        app.goals.allocateToGoal(done, Money(20_000))
        app.goals.completeGoal(done)
        tx(EXPENSE, savings, home, 45_000, today, "Conserto do carro") // poupança: reserva acima do saldo
        val closing = today.plusDays(5).dayOfMonth.coerceAtMost(28)
        val due = today.plusDays(12).dayOfMonth.coerceAtMost(28)
        val purple = (app.credit.createCard(CreditCardDetails("Cartão Roxo", Money(800_000), BillingDay(closing), BillingDay(due), "#6B5B8A")) as CreditResult.Saved).value.id
        app.credit.createCard(CreditCardDetails("Cartão do mercado", Money(120_000), BillingDay(3), BillingDay(10)))
        val note = UUID.randomUUID()
        app.credit.createPurchase(note, PurchaseDraft(purple, market, "Notebook", Money(360_000), 10, today.minusDays(40), merchant = "Loja de informática"))
        val firstInstallment = app.credit.observeInstallments(note).first().first()
        app.credit.payInstallment(firstInstallment.id, main, today.minusDays(1))
        val shoes = UUID.randomUUID()
        app.credit.createPurchase(shoes, PurchaseDraft(purple, market, "Tênis", Money(39_990), 3, today.minusDays(6)))
        app.credit.cancelPurchase(shoes)
        fun sub(name: String, cents: Long, anchor: LocalDate, src: SubscriptionSource = SubscriptionSource.Account(main), f: SubscriptionFrequency = SubscriptionFrequency.MONTHLY): UUID =
            runBlocking { (app.subscriptions.create(SubscriptionDetails(name, Money(cents), stream, src), f, anchor, BillingDay(anchor.dayOfMonth)) as SubscriptionResult.Saved).value.id }
        val music = sub("Música Premium Família", 3_490, today.minusMonths(2).plusDays(3))
        app.subscriptions.paySubscriptionOccurrence(music, today.minusMonths(2).plusDays(3), today.minusMonths(2).plusDays(3))
        app.subscriptions.skipSubscriptionOccurrence(music, today.minusMonths(1).plusDays(3))
        sub("Academia", 12_000, today.minusDays(3)) // vencida
        sub("Streaming", 5_590, today.plusDays(2))
        sub("Nuvem", 1_000, today.minusDays(10), SubscriptionSource.Card(purple))
        sub("Seguro do carro", 198_000, today.plusDays(8), f = SubscriptionFrequency.YEARLY)
        app.subscriptions.pause(sub("Jornal digital", 2_990, today.plusDays(5)))
        app.subscriptions.cancel(sub("Curso de idiomas", 8_900, today.plusDays(6)))
        Unit
    }

    @Test fun walk() {
        assumeTrue("Captura visual só roda sob demanda", suffix != null)
        SecurityGateTestHook.autoPrompt = false
        runBlocking {
            app.preferences.writeSecuritySettings(SecuritySettings())
            app.preferences.apply(PortableSettingsV1(args.getString("fluxaTheme") ?: "SYSTEM", args.getString("fluxaHide") == "1"))
        }
        if (args.getString("fluxaEmpty") == "1") {
            runBlocking { app.database.clearAllTables() }
            empty()
        } else {
            seed()
            populated()
        }
        SecurityGateTestHook.autoPrompt = true
        assertTrue(failures.joinToString("\n"), failures.isEmpty())
    }

    private fun empty() {
        section("e-tabs") { page("e-inicio", 0); tap("tab-cash"); page("e-caixa", 0); tap("tab-cards"); page("e-cartoes", 0) }
        section("e-subs") { more("more-subscriptions", "screen-subscriptions"); page("e-assinaturas", 0); tap("action-agenda"); page("e-agenda", 0) }
        section("e-goals") { more("more-goals", "screen-goals"); page("e-metas", 0); tap("action-new-goal"); page("e-meta-form", 1) }
        section("e-cats") { more("more-categories", "screen-categories"); page("e-categorias", 0) }
        section("e-acc") { tap("tab-cash"); tap("action-create-account"); wait("screen-account-form"); tap("action-save"); page("e-conta-form", 1) }
        section("e-card") { tap("tab-cards"); tap("action-new-card"); wait("screen-card-form"); tap("action-save"); page("e-cartao-form", 1) }
        section("e-entry") { openNewEntry(); shot("e-sheet-mais"); tap("new-expense"); wait("screen-transaction-form"); page("e-lancamento-form", 1) }
    }

    private fun populated() {
        section("inicio") { page("inicio", 2) }
        section("limite") { tap("action-safe-details"); wait("safe-hero"); page("limite", 3) }
        section("caixa") { tap("tab-cash"); page("caixa", 3) }
        section("contas") {
            tap("tab-cash"); tap("action-manage-accounts"); wait("screen-accounts"); page("contas", 1)
            tap("account-Conta Principal"); wait("screen-account-form"); page("conta-form", 1); tap("action-archive"); shot("conta-arquivar")
        }
        section("lancamento") {
            openNewEntry(); shot("sheet-mais"); tap("new-expense"); wait("screen-transaction-form"); page("lancamento", 1)
            tap("action-save"); page("lancamento-erro", 1)
        }
        section("lancamento-detalhe") {
            tap("tab-cash"); tap("tx-Feira do sábado"); wait("screen-transaction-form"); page("lancamento-editar", 1)
            tap("action-cancel-transaction"); shot("lancamento-cancelar")
        }
        section("pagamento-detalhe") { tap("tab-cash"); tap("tx-Pagamento • Notebook • 1/10"); wait("managed-payment"); page("lancamento-gerenciado", 0) }
        section("cartoes") { tap("tab-cards"); page("cartoes", 1); openCard(); page("cartao", 2) }
        section("cartao-form") { openCard(); tap("action-edit-card"); wait("screen-card-form"); page("cartao-form", 1) }
        section("compra-form") { openCard(); tap("action-card-new-purchase"); wait("screen-purchase-form"); page("compra-form", 2) }
        section("compra") { openNotebook(); page("compra", 1); tap("action-back"); tap("purchase-Tênis"); wait("screen-purchase-detail"); page("compra-cancelada", 0) }
        section("parcela") {
            openNotebook(); tap("installment--2"); wait("screen-installment-detail"); page("parcela", 0)
            tap("action-pay-installment"); wait("sheet-pay-installment"); shot("parcela-pagar")
        }
        section("parcela-paga") { openNotebook(); tap("installment--1"); wait("screen-installment-detail"); page("parcela-paga", 0); tap("action-undo-payment"); shot("parcela-desfazer") }
        section("assinaturas") { more("more-subscriptions", "screen-subscriptions"); page("assinaturas", 2) }
        section("assinatura") {
            more("more-subscriptions", "screen-subscriptions"); tap("subscription-Música Premium Família"); wait("screen-subscription-detail")
            page("assinatura", 2); tap("action-cancel-subscription"); shot("assinatura-cancelar")
        }
        section("agenda") {
            more("more-subscriptions", "screen-subscriptions"); tap("action-agenda"); wait("screen-subscription-agenda"); page("agenda", 1)
            tap("occurrence-Academia-${today.minusDays(3)}"); wait("screen-occurrence"); page("cobranca", 0)
            tap("action-settle-occurrence"); wait("sheet-settle-occurrence"); shot("cobranca-pagar")
        }
        section("assinatura-form") { more("more-subscriptions", "screen-subscriptions"); tap("action-new-subscription"); wait("screen-subscription-form"); page("assinatura-form", 2) }
        section("metas") {
            more("more-goals", "screen-goals"); page("metas", 1); tap("goal-Viagem em família"); wait("screen-goal-detail"); page("meta", 2)
            tap("action-allocate"); wait("sheet-goal-movement"); shot("meta-reservar")
        }
        section("meta-negativa") { more("more-goals", "screen-goals"); tap("goal-Geladeira nova"); wait("screen-goal-detail"); page("meta-negativa", 1); tap("action-complete-goal"); shot("meta-concluir") }
        section("meta-form") { more("more-goals", "screen-goals"); tap("action-new-goal"); wait("screen-goal-form"); page("meta-form", 1) }
        section("categorias") { more("more-categories", "screen-categories"); page("categorias", 0); tap("category-Mercado"); wait("screen-category-form"); page("categoria-form", 1) }
        section("mais") { tap("tab-more"); page("mais", 0); tap("more-backup"); wait("backup-security"); page("backup", 0); tap("action-backup-export"); wait("input-backup-password"); page("backup-form", 1) }
        section("seguranca") { more("more-security", "screen-security"); page("seguranca", 1) }
    }
}
