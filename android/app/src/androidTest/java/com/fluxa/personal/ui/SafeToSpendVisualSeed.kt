package com.fluxa.personal.ui

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.fluxa.personal.FluxaApplication
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
import com.fluxa.personal.domain.model.SubscriptionSource
import com.fluxa.personal.domain.model.TransactionDirection.EXPENSE
import java.time.LocalDate
import java.util.UUID
import kotlinx.coroutines.runBlocking
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Popula o limite seguro para a revisão visual. Só roda com `-e fluxaSeed 1` (cenário em `-e fluxaScenario`:
 * `positive` (padrão), `zero` ou `negative`); usa apenas contratos públicos.
 */
@RunWith(AndroidJUnit4::class)
class SafeToSpendVisualSeed {
    private val app get() = ApplicationProvider.getApplicationContext<FluxaApplication>()

    @Test
    fun seed() = runBlocking {
        val args = InstrumentationRegistry.getArguments()
        assumeTrue(args.getString("fluxaSeed") != null)
        val scenario = args.getString("fluxaScenario") ?: "positive"
        app.database.clearAllTables()
        val today = FinancialCalendar().today()
        val streaming = app.categories.create(CategoryDetails("Assinaturas", EXPENSE, "bolt", "#B9684F"))
        val market = app.categories.create(CategoryDetails("Compras", EXPENSE, "cart", "#4F6B8A"))
        val opening = when (scenario) { "zero" -> 150_000L; "negative" -> -40_000L; else -> 500_000L }
        val main = app.accounts.create(AccountDetails("Conta Principal", AccountType.CHECKING, Money(opening), LocalDate.of(2020, 1, 1)))
        fun sub(name: String, cents: Long, anchor: LocalDate, source: SubscriptionSource = SubscriptionSource.Account(main)) {
            runBlocking { app.subscriptions.create(SubscriptionDetails(name, Money(cents), streaming, source), SubscriptionFrequency.YEARLY, anchor, BillingDay(anchor.dayOfMonth)) }
        }
        if (scenario == "positive") {
            val trip = (app.goals.createGoal(GoalDetails("Viagem", Money(500_000), main)) as GoalResult.Saved).value.id
            app.goals.allocateToGoal(trip, Money(150_000))
            val card = (app.credit.createCard(CreditCardDetails("Cartão Roxo", Money(800_000), BillingDay(today.plusDays(5).dayOfMonth.coerceAtMost(28)), BillingDay(today.plusDays(12).dayOfMonth.coerceAtMost(28)))) as CreditResult.Saved).value.id
            app.credit.createPurchase(UUID.randomUUID(), PurchaseDraft(card, market, "Notebook", Money(360_000), 10, today.minusDays(2)))
            sub("Academia", 12_000, today.minusDays(3))
            sub("Netflix", 5_590, today.plusDays(2))
            sub("Seguro do carro", 98_000, today.plusDays(8))
            sub("Internet fibra", 11_990, today.plusDays(20))
        } else if (scenario == "zero") {
            sub("Aluguel", 150_000, today.plusDays(4))
        }
        Unit
    }
}
