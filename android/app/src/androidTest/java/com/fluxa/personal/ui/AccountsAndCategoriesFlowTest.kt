package com.fluxa.personal.ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.fluxa.personal.FluxaApplication
import com.fluxa.personal.MainActivity
import com.fluxa.personal.core.money.Money
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Fluxos de ponta a ponta das telas de Contas e Categorias sobre o Room real do app.
 * Cada teste limpa o banco antes de abrir a atividade.
 */
@OptIn(ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
class AccountsAndCategoriesFlowTest {
    @get:Rule
    val compose = createEmptyComposeRule()

    private val app get() = ApplicationProvider.getApplicationContext<FluxaApplication>()

    @Before
    fun clearDatabase() {
        app.database.clearAllTables()
    }

    private fun waitForTag(tag: String) = compose.waitUntilAtLeastOneExists(hasTestTag(tag), 5_000)
    private fun waitForNoTag(tag: String) = compose.waitUntilDoesNotExist(hasTestTag(tag), 5_000)

    private fun openAccounts() {
        compose.onNodeWithTag("tab-cash").performClick()
        waitForTag("screen-cash")
        compose.onNodeWithTag("list-cash").performScrollToNode(hasTestTag("action-manage-accounts"))
        compose.onNodeWithTag("action-manage-accounts").performClick()
        waitForTag("screen-accounts")
    }

    /** Em telas baixas (320x640dp) com teclado aberto, itens da LazyColumn podem não estar compostos. */
    private fun reveal(tag: String) {
        compose.waitForIdle()
        compose.onNodeWithTag("feature-list").performScrollToNode(hasTestTag(tag))
    }

    private fun openCategories() {
        compose.onNodeWithTag("tab-more").performClick()
        waitForTag("more-categories")
        compose.onNodeWithTag("more-categories").performClick()
        waitForTag("screen-categories")
    }

    private fun typeInto(tag: String, text: String) {
        reveal(tag)
        compose.onNodeWithTag(tag).performTextClearance()
        compose.onNodeWithTag(tag).performTextInput(text)
    }

    @Test
    fun createEditAndArchiveAccount() {
        ActivityScenario.launch(MainActivity::class.java).use {
            openAccounts()
            compose.onNodeWithTag("action-new-account").performClick()
            waitForTag("screen-account-form")
            // A navegação flutuante não aparece sobre o formulário.
            compose.onNodeWithTag("tab-cash").assertDoesNotExist()

            typeInto("field-account-name", "Conta teste")
            reveal("type-SAVINGS")
            compose.onNodeWithTag("type-SAVINGS").performClick()
            typeInto("field-account-amount", "1.250,90")
            compose.onNodeWithTag("action-save").performClick()

            waitForTag("account-Conta teste")
            compose.onNode(hasText("1.250,90", substring = true)).assertIsDisplayed()
            val created = runBlocking { app.accounts.observeAll().first().single() }
            assertEquals(Money(125_090), created.details.openingBalance)

            compose.onNodeWithTag("account-Conta teste").performClick()
            waitForTag("screen-account-form")
            compose.waitUntilAtLeastOneExists(hasText("Conta teste"), 5_000)
            typeInto("field-account-name", "Conta editada")
            compose.onNodeWithTag("action-save").performClick()
            waitForTag("account-Conta editada")
            assertEquals(created.id, runBlocking { app.accounts.observeAll().first().single().id })

            compose.onNodeWithTag("account-Conta editada").performClick()
            waitForTag("screen-account-form")
            reveal("action-archive")
            compose.onNodeWithTag("action-archive").performClick()
            waitForTag("action-confirm-archive")
            compose.onNodeWithTag("action-confirm-archive").performClick()

            waitForTag("screen-accounts")
            waitForNoTag("account-Conta editada")
            compose.onNodeWithTag("toggle-archived-accounts").performClick()
            waitForTag("account-Conta editada")
            assertNotNull(runBlocking { app.accounts.observe(created.id).first()!!.archivedAt })
        }
    }

    @Test
    fun invalidAmountShowsErrorAndDoesNotSave() {
        ActivityScenario.launch(MainActivity::class.java).use {
            openAccounts()
            compose.onNodeWithTag("action-new-account").performClick()
            waitForTag("screen-account-form")
            typeInto("field-account-name", "Conta")
            typeInto("field-account-amount", "12,345")
            compose.onNodeWithTag("action-save").performClick()
            compose.waitUntilAtLeastOneExists(hasText("Use no máximo dois dígitos após a vírgula."), 5_000)
            compose.onNodeWithTag("screen-account-form").assertIsDisplayed()
            assertEquals(0, runBlocking { app.accounts.observeAll(includeArchived = true).first().size })
        }
    }

    @Test
    fun createEditArchiveAndReactivateCategories() {
        ActivityScenario.launch(MainActivity::class.java).use {
            openCategories()

            compose.onNodeWithTag("action-new-category").performClick()
            waitForTag("screen-category-form")
            typeInto("field-category-name", "Mercado teste")
            compose.onNodeWithTag("action-save").performClick()
            waitForTag("category-Mercado teste")

            compose.onNodeWithTag("kind-INCOME").performClick()
            compose.onNodeWithTag("action-new-category").performClick()
            waitForTag("screen-category-form")
            typeInto("field-category-name", "Salário teste")
            compose.onNodeWithTag("action-save").performClick()
            waitForTag("category-Salário teste")
            compose.onNodeWithTag("category-Mercado teste").assertDoesNotExist()

            compose.onNodeWithTag("category-Salário teste").performClick()
            waitForTag("screen-category-form")
            compose.waitUntilAtLeastOneExists(hasText("Salário teste"), 5_000)
            typeInto("field-category-name", "Salário editado")
            reveal("icon-work")
            compose.onNodeWithTag("icon-work").performClick()
            compose.onNodeWithTag("action-save").performClick()
            waitForTag("category-Salário editado")

            compose.onNodeWithTag("category-Salário editado").performClick()
            waitForTag("screen-category-form")
            reveal("action-archive")
            compose.onNodeWithTag("action-archive").performClick()
            waitForTag("action-confirm-archive")
            compose.onNodeWithTag("action-confirm-archive").performClick()
            waitForTag("reactivate-Salário editado")
            val archived = runBlocking { app.categories.observeAll(includeArchived = true).first().single { it.details.name == "Salário editado" } }
            assertNotNull(archived.archivedAt)
            assertEquals("work", archived.details.icon)

            compose.onNodeWithTag("reactivate-Salário editado").performClick()
            waitForNoTag("reactivate-Salário editado")
            waitForTag("category-Salário editado")
            assertNull(runBlocking { app.categories.observe(archived.id).first()!!.archivedAt })
        }
    }

    @Test
    fun dataSurvivesClosingAndReopeningTheApp() {
        ActivityScenario.launch(MainActivity::class.java).use {
            openCategories()
            compose.onNodeWithTag("action-new-category").performClick()
            waitForTag("screen-category-form")
            typeInto("field-category-name", "Persistente")
            compose.onNodeWithTag("action-save").performClick()
            waitForTag("category-Persistente")

            compose.onNodeWithTag("action-back").performClick()
            compose.onNodeWithTag("tab-cash").performClick()
            openAccounts()
            compose.onNodeWithTag("action-new-account").performClick()
            waitForTag("screen-account-form")
            typeInto("field-account-name", "Carteira persistente")
            compose.onNodeWithTag("action-save").performClick()
            waitForTag("account-Carteira persistente")
        }

        // Nova atividade, mesmo arquivo de banco.
        ActivityScenario.launch(MainActivity::class.java).use {
            openAccounts()
            waitForTag("account-Carteira persistente")
            compose.onNodeWithTag("action-back").performClick()
            openCategories()
            waitForTag("category-Persistente")
            compose.onNodeWithTag("category-Persistente").assertIsDisplayed()
        }
    }
}
