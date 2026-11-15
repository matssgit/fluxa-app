package com.fluxa.personal.ui

import android.graphics.Bitmap
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasScrollToNodeAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.performScrollToNode
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
            wait("cash-accounts-card")
            shot("caixa")
            compose.onNodeWithTag("cash-accounts-card").performClick()
            wait("screen-accounts")
            scrollTo("toggle-archived-accounts")
            shot("contas")
            compose.onNodeWithTag("toggle-archived-accounts").performClick()
            scrollTo("account-Cheque especial")
            shot("contas-arquivadas")
            scrollTo("account-Conta principal")
            compose.onNodeWithTag("account-Conta principal").performClick()
            wait("action-archive")
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
            wait("action-archive")
            shot("categoria-editar")
            compose.onNodeWithTag("action-archive").performClick()
            wait("action-confirm-archive")
            shot("categoria-arquivar")
        }
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
