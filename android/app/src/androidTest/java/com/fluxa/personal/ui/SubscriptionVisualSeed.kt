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
import com.fluxa.personal.domain.model.SubscriptionDetails
import com.fluxa.personal.domain.model.SubscriptionFrequency
import com.fluxa.personal.domain.model.SubscriptionResult
import com.fluxa.personal.domain.model.SubscriptionSource
import com.fluxa.personal.domain.model.TransactionDirection.EXPENSE
import java.time.LocalDate
import java.util.UUID
import kotlinx.coroutines.runBlocking
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Popula o banco para a revisão visual das assinaturas. Só roda com `-e fluxaSeed 1` (ignorado na suíte normal).
 * Usa apenas os contratos públicos dos repositórios.
 */
@RunWith(AndroidJUnit4::class)
class SubscriptionVisualSeed {
    private val app get() = ApplicationProvider.getApplicationContext<FluxaApplication>()

    @Test
    fun seed() = runBlocking {
        assumeTrue(InstrumentationRegistry.getArguments().getString("fluxaSeed") != null)
        app.database.clearAllTables()
        val today = FinancialCalendar().today()
        val streaming = app.categories.create(CategoryDetails("Streaming", EXPENSE, "bolt", "#B9684F"))
        val software = app.categories.create(CategoryDetails("Software", EXPENSE, "briefcase", "#4F7AB9"))
        val main = app.accounts.create(AccountDetails("Conta Principal", AccountType.CHECKING, Money(250_000), LocalDate.of(2020, 1, 1)))
        val card = (app.credit.createCard(CreditCardDetails("Cartão Roxo", Money(300_000), BillingDay(10), BillingDay(17))) as CreditResult.Saved).value.id
        val subs = app.subscriptions
        fun create(name: String, cents: Long, f: SubscriptionFrequency, anchor: LocalDate, source: SubscriptionSource, category: UUID = streaming): UUID =
            runBlocking { (subs.create(SubscriptionDetails(name, Money(cents), category, source), f, anchor, BillingDay(anchor.dayOfMonth)) as SubscriptionResult.Saved).value.id }

        val music = create("Música Premium Família", 3_490, SubscriptionFrequency.MONTHLY, today.minusMonths(2).plusDays(3), SubscriptionSource.Account(main))
        val cloud = create("Armazenamento em nuvem", 1_000, SubscriptionFrequency.MONTHLY, today.minusDays(10), SubscriptionSource.Card(card), software)
        create("Antivírus", 60_000, SubscriptionFrequency.YEARLY, today.plusDays(12), SubscriptionSource.Account(main), software)
        val news = create("Jornal digital", 2_990, SubscriptionFrequency.MONTHLY, today.minusMonths(1).plusDays(5), SubscriptionSource.Account(main))
        create("Revista de culinária", 1_990, SubscriptionFrequency.MONTHLY, today.minusDays(3), SubscriptionSource.Account(main)) // vencida
        val old = create("Curso de idiomas", 8_900, SubscriptionFrequency.MONTHLY, today.minusMonths(1).plusDays(1), SubscriptionSource.Account(main))

        // Histórico: uma paga, uma pulada, uma cobrada no cartão; uma vencida em aberto (música, mês atual).
        subs.paySubscriptionOccurrence(music, today.minusMonths(2).plusDays(3), today.minusMonths(2).plusDays(3))
        subs.skipSubscriptionOccurrence(music, today.minusMonths(1).plusDays(3))
        subs.paySubscriptionOccurrence(cloud, today.minusDays(10), today.minusDays(9))
        subs.paySubscriptionOccurrence(news, today.minusMonths(1).plusDays(5), today.minusMonths(1).plusDays(5))
        subs.pause(news)
        subs.paySubscriptionOccurrence(old, today.minusMonths(1).plusDays(1), today.minusMonths(1).plusDays(1))
        subs.cancel(old)
        Unit
    }
}
