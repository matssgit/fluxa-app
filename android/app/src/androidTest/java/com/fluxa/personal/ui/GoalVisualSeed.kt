package com.fluxa.personal.ui

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.fluxa.personal.FluxaApplication
import com.fluxa.personal.core.date.FinancialCalendar
import com.fluxa.personal.core.money.Money
import com.fluxa.personal.domain.model.AccountDetails
import com.fluxa.personal.domain.model.AccountType
import com.fluxa.personal.domain.model.CategoryDetails
import com.fluxa.personal.domain.model.GoalDetails
import com.fluxa.personal.domain.model.GoalResult
import com.fluxa.personal.domain.model.TransactionDirection.EXPENSE
import com.fluxa.personal.domain.model.TransactionDirection.INCOME
import com.fluxa.personal.domain.model.TransactionDraft
import java.time.LocalDate
import java.util.UUID
import kotlinx.coroutines.runBlocking
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith

/** Popula metas para a revisão visual. Só roda com `-e fluxaSeed 1`; usa apenas contratos públicos. */
@RunWith(AndroidJUnit4::class)
class GoalVisualSeed {
    private val app get() = ApplicationProvider.getApplicationContext<FluxaApplication>()

    @Test
    fun seed() = runBlocking {
        assumeTrue(InstrumentationRegistry.getArguments().getString("fluxaSeed") != null)
        app.database.clearAllTables()
        val today = FinancialCalendar().today()
        val main = app.accounts.create(AccountDetails("Conta Principal", AccountType.CHECKING, Money(500_000), LocalDate.of(2020, 1, 1)))
        val savings = app.accounts.create(AccountDetails("Poupança", AccountType.SAVINGS, Money(300_000), LocalDate.of(2020, 1, 1)))
        val market = app.categories.create(CategoryDetails("Mercado", EXPENSE, "cart", "#B9684F"))
        val salary = app.categories.create(CategoryDetails("Salário", INCOME, "briefcase", "#4F8A6B"))
        app.transactions.create(UUID.randomUUID(), TransactionDraft(INCOME, main, salary, Money(120_000), today.minusDays(6), "Salário"))
        fun goal(name: String, cents: Long, account: UUID, date: LocalDate? = null): UUID =
            runBlocking { (app.goals.createGoal(GoalDetails(name, Money(cents), account, date)) as GoalResult.Saved).value.id }

        val trip = goal("Viagem de férias em família", 500_000, main, today.plusMonths(8))
        app.goals.allocateToGoal(trip, Money(150_000))
        app.goals.allocateToGoal(trip, Money(60_000))
        app.goals.releaseFromGoal(trip, Money(10_000))
        val laptop = goal("Notebook", 120_000, main)
        app.goals.allocateToGoal(laptop, Money(120_000))
        app.goals.completeGoal(laptop)
        val emergency = goal("Reserva de emergência", 600_000, savings)
        app.goals.allocateToGoal(emergency, Money(280_000))
        val old = goal("Curso antigo", 50_000, main)
        app.goals.archiveGoal(old)
        // Despesa real na poupança depois da reserva: disponível dessa conta fica negativo.
        app.transactions.create(UUID.randomUUID(), TransactionDraft(EXPENSE, savings, market, Money(60_000), today, "Conserto do carro"))
        Unit
    }
}
