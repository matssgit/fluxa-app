package com.fluxa.personal.data.database

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.fluxa.personal.core.date.FinancialCalendar
import com.fluxa.personal.core.money.Money
import com.fluxa.personal.data.repository.AccountRepository
import com.fluxa.personal.data.repository.CategoryRepository
import com.fluxa.personal.data.repository.GoalRepository
import com.fluxa.personal.data.repository.TransactionRepository
import com.fluxa.personal.domain.model.*
import com.fluxa.personal.domain.model.GoalResult.Rejected
import com.fluxa.personal.domain.model.GoalResult.Saved
import com.fluxa.personal.domain.rules.CalculatedBalance.Exact
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.UUID
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class GoalPersistenceTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val name = "goals-${UUID.randomUUID()}"
    private val now = Instant.parse("2024-03-01T02:30:00Z")
    private val clock = Clock.fixed(now, ZoneOffset.UTC)
    private val calendar = FinancialCalendar(clock) { ZoneOffset.ofHours(-3) }
    private val date = LocalDate.of(2024, 2, 29)
    private lateinit var db: FluxaDatabase
    private lateinit var goals: GoalRepository
    private lateinit var accounts: AccountRepository

    @Before
    fun setup() {
        db = FluxaDatabase.create(context, name)
        goals = GoalRepository(db, clock, calendar)
        accounts = AccountRepository(db.accountDao(), clock)
    }

    @After
    fun cleanup() {
        db.close()
        context.deleteDatabase(name)
    }

    @Test
    fun combinesMultipleGoalsAndExcludedAccountsWithoutChangingCash() = runBlocking {
        val main = account("Principal", 500_000)
        val excluded = account("Investimentos", 1_000_000, included = false)
        val excludedGoal = goal(excluded, "Longo prazo", 400_000)
        assertTrue(goals.allocateToGoal(excludedGoal.id, Money(400_000)) is Saved)
        assertEquals(Exact(Money(500_000)), goals.observeReserveBalances().first().availableBalance)

        val goalsOnMain = listOf(100_000L, 150_000L, 50_000L).mapIndexed { index, amount ->
            val goal = goal(main, "Meta $index", amount)
            assertTrue(goals.allocateToGoal(goal.id, Money(amount)) is Saved)
            goal
        }
        val snapshot = goals.observeReserveBalances().first()
        assertEquals(Exact(Money(1_500_000)), snapshot.totalBalance)
        assertEquals(Exact(Money(700_000)), snapshot.totalReserved)
        assertEquals(Exact(Money(200_000)), snapshot.availableBalance)
        assertEquals(Exact(Money(500_000)), snapshot.accounts.getValue(main).accountBalance)
        assertEquals(Exact(Money(300_000)), snapshot.accounts.getValue(main).reservedAmount)
        assertEquals(Exact(Money(600_000)), snapshot.accounts.getValue(excluded).availableAmount)
        val exact = goals.observeGoal(goalsOnMain[1].id).first()!!
        assertEquals(GoalStatus.ACTIVE, exact.goal.status)
        assertEquals(main, exact.account.id)
        assertEquals(Money(0), exact.remainingAmount)
        assertEquals(10_000, exact.progressBasisPoints)
        assertEquals(date, goals.observeMovements(exact.goal.id).first().single().occurredOn)
        assertTrue(TransactionRepository(db).observeHistory().first().isEmpty())
    }

    @Test
    fun reserveFlowReactsToAllocationsLedgerExpensesAndReleases() = runBlocking {
        val account = account("Banco", 500_000)
        val goal = goal(account, "Reserva", 400_000)
        val category = CategoryRepository(db.categoryDao(), clock)
            .create(CategoryDetails("Despesa", TransactionDirection.EXPENSE))
        val events = List(4) { CompletableDeferred<Unit>() }
        val snapshots = mutableListOf<ReserveBalanceSnapshot>()
        val collecting = launch {
            withTimeout(10_000) {
                goals.observeReserveBalances().take(4).collect {
                    snapshots.add(it)
                    events[snapshots.lastIndex].complete(Unit)
                }
            }
        }
        withTimeout(10_000) {
            events[0].await()
            assertTrue(goals.allocateToGoal(goal.id, Money(400_000)) is Saved)
            events[1].await()
            val ledger = TransactionRepository(db, clock, calendar)
            assertTrue(ledger.create(UUID.randomUUID(), TransactionDraft(
                TransactionDirection.EXPENSE, account, category, Money(200_000), date,
            )) is TransactionResult.Saved)
            events[2].await()
            assertTrue(goals.releaseFromGoal(goal.id, Money(50_000)) is Saved)
            events[3].await()
        }
        collecting.join()
        assertEquals(listOf(500_000L, 100_000L, -100_000L, -50_000L).map { Exact(Money(it)) },
            snapshots.map { it.availableBalance })
        assertEquals(Exact(Money(300_000)), snapshots[2].totalBalance)
        assertEquals(Exact(Money(400_000)), snapshots[2].totalReserved)
    }

    @Test
    fun allocationsAcrossTwoGoalsShareOneAtomicAvailabilityCheck() = runBlocking {
        val account = account("Concorrência", 100_000)
        val a = goal(account, "A", 100_000)
        val b = goal(account, "B", 100_000)
        val start = CompletableDeferred<Unit>()
        val attempts = listOf(a, b).map { goal ->
            async(Dispatchers.Default) { start.await(); goals.allocateToGoal(goal.id, Money(70_000)) }
        }
        start.complete(Unit)
        val results = attempts.awaitAll()
        assertEquals(1, results.count { it is Saved })
        assertEquals(1, results.count { it == Rejected(GoalError.INSUFFICIENT_AVAILABLE_FUNDS) })
        assertEquals(Exact(Money(70_000)), goals.observeReserveBalances().first().totalReserved)
        assertEquals(Exact(Money(30_000)), goals.observeAccountReserves(account).first()?.availableAmount)
    }

    @Test
    fun completesExplicitlyArchivesSafelyAndPreservesHistoryAfterAccountArchive() = runBlocking {
        val origin = account("Origem", 500_000)
        val other = account("Outra", 100_000)
        val created = goal(other, "Reserva", 100_000)
        val changed = (goals.updateGoal(created.id, created.details.copy(accountId = origin, targetDate = date.plusYears(1))) as Saved).value
        assertEquals(origin, changed.details.accountId)
        assertEquals(date.plusYears(1), goals.observeGoal(changed.id).first()?.goal?.details?.targetDate)
        assertEquals(Rejected(GoalError.TARGET_NOT_REACHED), goals.completeGoal(changed.id))
        assertTrue(goals.allocateToGoal(changed.id, Money(100_000)) is Saved)
        assertEquals(Rejected(GoalError.ALLOCATION_EXCEEDS_TARGET), goals.allocateToGoal(changed.id, Money(1)))
        assertEquals(Rejected(GoalError.TARGET_BELOW_RESERVED),
            goals.updateGoal(changed.id, changed.details.copy(targetAmount = Money(99_999))))
        assertEquals(Rejected(GoalError.HAS_RESERVED_FUNDS), goals.archiveGoal(changed.id))

        val empty = goal(origin, "Ainda ativa", 100_000)
        assertTrue(goals.completeGoal(changed.id) is Saved)
        assertEquals(Rejected(GoalError.COMPLETED), goals.allocateToGoal(changed.id, Money(1)))
        assertTrue(accounts.archive(origin))
        assertEquals(Rejected(GoalError.ACCOUNT_ARCHIVED), goals.allocateToGoal(empty.id, Money(1)))
        assertEquals(Rejected(GoalError.ACCOUNT_ARCHIVED), goals.createGoal(changed.details))
        val snapshot = goals.observeReserveBalances().first()
        assertEquals(Exact(Money(100_000)), snapshot.availableBalance)
        assertEquals(Exact(Money(100_000)), snapshot.totalReserved)
        assertEquals(Exact(Money(400_000)), snapshot.accounts.getValue(origin).availableAmount)
        assertEquals(Rejected(GoalError.RELEASE_EXCEEDS_RESERVED), goals.releaseFromGoal(changed.id, Money(100_001)))
        assertTrue(goals.releaseFromGoal(changed.id, Money(100_000)) is Saved)
        assertEquals(Rejected(GoalError.ACCOUNT_CHANGE_NOT_ALLOWED),
            goals.updateGoal(changed.id, changed.details.copy(accountId = other)))
        assertTrue(goals.archiveGoal(changed.id) is Saved)
        assertEquals(GoalStatus.ARCHIVED, goals.observeGoal(changed.id).first()?.goal?.status)
        assertEquals(now, goals.observeGoal(changed.id).first()?.goal?.completedAt)
        assertEquals(setOf(GoalMovementType.ALLOCATE, GoalMovementType.RELEASE),
            goals.observeMovements(changed.id).first().map { it.type }.toSet())
        assertEquals(listOf(empty.id), goals.observeGoals().first().map { it.goal.id })
        assertEquals(2, goals.observeGoals(includeArchived = true).first().size)
    }

    private suspend fun account(name: String, cents: Long, included: Boolean = true): UUID =
        accounts.create(AccountDetails(name, AccountType.CHECKING, Money(cents), date, included))

    private suspend fun goal(account: UUID, title: String, target: Long): Goal =
        (goals.createGoal(GoalDetails(title, Money(target), account)) as Saved).value
}
