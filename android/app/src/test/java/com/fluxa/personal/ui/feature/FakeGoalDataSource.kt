package com.fluxa.personal.ui.feature

import com.fluxa.personal.core.money.Money
import com.fluxa.personal.data.repository.AccountRepository
import com.fluxa.personal.domain.model.AccountReserveSummary
import com.fluxa.personal.domain.model.Goal
import com.fluxa.personal.domain.model.GoalDetails
import com.fluxa.personal.domain.model.GoalError
import com.fluxa.personal.domain.model.GoalMovement
import com.fluxa.personal.domain.model.GoalMovementType
import com.fluxa.personal.domain.model.GoalResult
import com.fluxa.personal.domain.model.GoalSummary
import com.fluxa.personal.domain.model.ReserveBalanceSnapshot
import com.fluxa.personal.domain.rules.AccountBalanceInput
import com.fluxa.personal.domain.rules.BalanceCalculator
import com.fluxa.personal.domain.rules.BalanceMovement
import com.fluxa.personal.domain.rules.GoalRules
import com.fluxa.personal.domain.rules.ReserveBalanceCalculator
import com.fluxa.personal.domain.rules.wide
import com.fluxa.personal.ui.feature.goals.GoalDataSource
import java.io.IOException
import java.math.BigInteger
import java.time.Instant
import java.time.LocalDate
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/**
 * Metas em memória para testes JVM, com a mesma sequência do `GoalRepository`: validações do `GoalRules`, reserva
 * derivada dos movimentos, resumo do `GoalRules.summarize` e disponibilidade do `ReserveBalanceCalculator` sobre o
 * ledger do [FakeTransactionSource]. Nenhum movimento de meta entra no ledger.
 */
internal class FakeGoalDataSource(
    private val accounts: AccountRepository,
    private val ledger: FakeTransactionSource,
    private val now: Instant,
    private val today: LocalDate,
) : GoalDataSource {
    private data class Row(val goal: Goal, val movements: List<GoalMovement>)

    private val store = MutableStateFlow<Map<UUID, Row>>(emptyMap())
    var failWrites = false
    val allocateCalls = mutableListOf<Money>()

    private fun Row.reserved() = GoalRules.reserved(movements)

    private suspend fun accountError(id: UUID): GoalError? {
        val account = accounts.observe(id).first() ?: return GoalError.ACCOUNT_NOT_FOUND
        return GoalError.ACCOUNT_ARCHIVED.takeIf { account.archivedAt != null }
    }

    private fun balanceInputs(list: List<com.fluxa.personal.domain.model.FinancialAccount>, txs: Collection<com.fluxa.personal.domain.model.FinancialTransaction>) =
        list.map { a ->
            AccountBalanceInput(
                a.id, a.details.openingBalance, a.details.openingDate, a.details.includeInAvailable && a.archivedAt == null,
                txs.filter { it.details.accountId == a.id && it.cancelledAt == null }.map { BalanceMovement(it.details.direction, it.details.amount, it.details.occurredOn) },
            )
        }

    override suspend fun createGoal(details: GoalDetails): GoalResult<Goal> {
        if (failWrites) throw IOException("disk full")
        GoalRules.validateDetails(details)?.let { return GoalResult.Rejected(it) }
        accountError(details.accountId)?.let { return GoalResult.Rejected(it) }
        val goal = Goal(UUID.randomUUID(), details.copy(name = details.name.trim()), now, now)
        store.update { it + (goal.id to Row(goal, emptyList())) }
        return GoalResult.Saved(goal)
    }

    override suspend fun updateGoal(id: UUID, details: GoalDetails): GoalResult<Goal> {
        val row = store.value[id] ?: return GoalResult.Rejected(GoalError.NOT_FOUND)
        GoalRules.validateUpdate(row.goal, details, row.reserved(), row.movements.isNotEmpty())?.let { return GoalResult.Rejected(it) }
        if (details.accountId != row.goal.details.accountId) accountError(details.accountId)?.let { return GoalResult.Rejected(it) }
        val updated = row.goal.copy(details = details.copy(name = details.name.trim()))
        store.update { it + (id to row.copy(goal = updated)) }
        return GoalResult.Saved(updated)
    }

    override suspend fun allocateToGoal(id: UUID, amount: Money): GoalResult<GoalMovement> {
        if (failWrites) throw IOException("disk full")
        allocateCalls += amount
        val row = store.value[id] ?: return GoalResult.Rejected(GoalError.NOT_FOUND)
        val accountId = row.goal.details.accountId
        accountError(accountId)?.let { return GoalResult.Rejected(it) }
        val account = accounts.observe(accountId).first()!!
        val balance = BalanceCalculator.balanceOf(balanceInputs(listOf(account), ledger.rows.value.values).single())
        val accountReserved = store.value.values.filter { it.goal.details.accountId == accountId }.fold(BigInteger.ZERO) { t, r -> t + r.reserved().wide() }
        GoalRules.validateAllocation(row.goal, row.reserved(), amount, balance - accountReserved)?.let { return GoalResult.Rejected(it) }
        return record(row, GoalMovementType.ALLOCATE, amount)
    }

    override suspend fun releaseFromGoal(id: UUID, amount: Money): GoalResult<GoalMovement> {
        val row = store.value[id] ?: return GoalResult.Rejected(GoalError.NOT_FOUND)
        GoalRules.validateRelease(row.goal, row.reserved(), amount)?.let { return GoalResult.Rejected(it) }
        return record(row, GoalMovementType.RELEASE, amount)
    }

    private fun record(row: Row, type: GoalMovementType, amount: Money): GoalResult<GoalMovement> {
        val m = GoalMovement(UUID.randomUUID(), row.goal.id, type, amount, today, now.plusSeconds(row.movements.size.toLong()))
        store.update { it + (row.goal.id to row.copy(movements = row.movements + m)) }
        return GoalResult.Saved(m)
    }

    override suspend fun completeGoal(id: UUID): GoalResult<Goal> {
        val row = store.value[id] ?: return GoalResult.Rejected(GoalError.NOT_FOUND)
        GoalRules.validateCompletion(row.goal, row.reserved())?.let { return GoalResult.Rejected(it) }
        val updated = row.goal.copy(completedAt = now)
        store.update { it + (id to row.copy(goal = updated)) }
        return GoalResult.Saved(updated)
    }

    override suspend fun archiveGoal(id: UUID): GoalResult<Goal> {
        val row = store.value[id] ?: return GoalResult.Rejected(GoalError.NOT_FOUND)
        GoalRules.validateArchive(row.goal, row.reserved())?.let { return GoalResult.Rejected(it) }
        val updated = row.goal.copy(archivedAt = now)
        store.update { it + (id to row.copy(goal = updated)) }
        return GoalResult.Saved(updated)
    }

    private fun summaries(rows: Map<UUID, Row>, all: List<com.fluxa.personal.domain.model.FinancialAccount>): List<GoalSummary> =
        rows.values.mapNotNull { r -> all.firstOrNull { it.id == r.goal.details.accountId }?.let { GoalRules.summarize(r.goal, it, r.reserved()) } }

    override fun observeGoals(includeArchived: Boolean): Flow<List<GoalSummary>> =
        combine(store, accounts.observeAll(includeArchived = true)) { s, a -> summaries(s, a).filter { includeArchived || it.goal.archivedAt == null } }

    override fun observeGoal(id: UUID): Flow<GoalSummary?> =
        combine(store, accounts.observeAll(includeArchived = true)) { s, a -> summaries(s, a).firstOrNull { it.goal.id == id } }

    override fun observeMovements(id: UUID): Flow<List<GoalMovement>> =
        store.map { s -> s[id]?.movements.orEmpty().sortedWith(compareByDescending<GoalMovement> { it.occurredOn }.thenByDescending { it.createdAt }) }

    override fun observeReserveBalances(): Flow<ReserveBalanceSnapshot> =
        combine(store, accounts.observeAll(includeArchived = true), ledger.rows) { s, a, txs ->
            val reserves = s.values.groupBy { it.goal.details.accountId }.mapValues { (_, rows) -> rows.fold(BigInteger.ZERO) { t, r -> t + r.reserved().wide() } }
            ReserveBalanceCalculator.calculate(balanceInputs(a, txs.values), reserves)
        }

    override fun observeAccountReserves(accountId: UUID): Flow<AccountReserveSummary?> = observeReserveBalances().map { it.accounts[accountId] }
}
