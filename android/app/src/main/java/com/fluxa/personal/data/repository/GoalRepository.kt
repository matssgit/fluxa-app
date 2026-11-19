package com.fluxa.personal.data.repository

import androidx.room.invalidationTrackerFlow
import androidx.room.withTransaction
import com.fluxa.personal.core.date.FinancialCalendar
import com.fluxa.personal.core.money.Money
import com.fluxa.personal.data.database.FluxaDatabase
import com.fluxa.personal.data.database.dao.WideCashTotal
import com.fluxa.personal.data.database.mapper.reserved
import com.fluxa.personal.data.database.mapper.toBalanceInputs
import com.fluxa.personal.data.database.mapper.toDomain
import com.fluxa.personal.data.database.mapper.toEntity
import com.fluxa.personal.data.database.mapper.toSummary
import com.fluxa.personal.domain.model.Goal
import com.fluxa.personal.domain.model.GoalDetails
import com.fluxa.personal.domain.model.GoalError
import com.fluxa.personal.domain.model.GoalMovement
import com.fluxa.personal.domain.model.GoalMovementType
import com.fluxa.personal.domain.model.GoalResult
import com.fluxa.personal.domain.model.GoalResult.Rejected
import com.fluxa.personal.domain.model.GoalResult.Saved
import com.fluxa.personal.domain.rules.AccountReserveInput
import com.fluxa.personal.domain.rules.BalanceCalculator
import com.fluxa.personal.domain.rules.GoalRules
import com.fluxa.personal.domain.rules.ReserveBalanceCalculator
import com.fluxa.personal.domain.rules.wide
import java.math.BigInteger
import java.time.Clock
import java.time.LocalDate
import java.util.UUID
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

internal class GoalRepository(
    private val database: FluxaDatabase,
    private val clock: Clock = Clock.systemUTC(),
    private val calendar: FinancialCalendar = FinancialCalendar(clock),
) {
    private val dao = database.goalDao()
    private val transactions = database.transactionDao()

    suspend fun createGoal(details: GoalDetails): GoalResult<Goal> = database.withTransaction {
        GoalRules.validateDetails(details)?.let { return@withTransaction Rejected(it) }
        validateAccount(details.accountId)?.let { return@withTransaction Rejected(it) }
        val now = clock.instant()
        val goal = Goal(UUID.randomUUID(), details.normalized(), now, now)
        dao.insert(goal.toEntity())
        Saved(goal)
    }

    suspend fun updateGoal(id: UUID, details: GoalDetails): GoalResult<Goal> = database.withTransaction {
        val row = dao.find(id.toString()) ?: return@withTransaction Rejected(GoalError.NOT_FOUND)
        val goal = row.goal.toDomain()
        GoalRules.validateUpdate(goal, details, row.reserved(), row.movements.isNotEmpty())?.let {
            return@withTransaction Rejected(it)
        }
        if (details.accountId != goal.details.accountId) {
            validateAccount(details.accountId)?.let { return@withTransaction Rejected(it) }
        }
        save(goal.copy(details = details.normalized(), updatedAt = clock.instant()))
    }

    suspend fun allocateToGoal(id: UUID, amount: Money): GoalResult<GoalMovement> = database.withTransaction {
        val row = dao.find(id.toString()) ?: return@withTransaction Rejected(GoalError.NOT_FOUND)
        val goal = row.goal.toDomain()
        val accountId = goal.details.accountId
        validateAccount(accountId)?.let { return@withTransaction Rejected(it) }
        val balance = BalanceCalculator.balanceOf(transactions.readBalanceRows(accountId.toString()).toBalanceInputs().single())
        val accountReserved = dao.readGoals(accountId.toString()).fold(BigInteger.ZERO) { total, accountGoal ->
            total + accountGoal.reserved().wide()
        }
        GoalRules.validateAllocation(goal, row.reserved(), amount, balance - accountReserved)?.let {
            return@withTransaction Rejected(it)
        }
        record(goal, GoalMovementType.ALLOCATE, amount)
    }

    suspend fun releaseFromGoal(id: UUID, amount: Money): GoalResult<GoalMovement> = database.withTransaction {
        val row = dao.find(id.toString()) ?: return@withTransaction Rejected(GoalError.NOT_FOUND)
        val goal = row.goal.toDomain()
        GoalRules.validateRelease(goal, row.reserved(), amount)?.let { return@withTransaction Rejected(it) }
        record(goal, GoalMovementType.RELEASE, amount)
    }

    suspend fun completeGoal(id: UUID): GoalResult<Goal> = database.withTransaction {
        val row = dao.find(id.toString()) ?: return@withTransaction Rejected(GoalError.NOT_FOUND)
        val goal = row.goal.toDomain()
        GoalRules.validateCompletion(goal, row.reserved())?.let { return@withTransaction Rejected(it) }
        val now = clock.instant()
        save(goal.copy(completedAt = now, updatedAt = now))
    }

    suspend fun archiveGoal(id: UUID): GoalResult<Goal> = database.withTransaction {
        val row = dao.find(id.toString()) ?: return@withTransaction Rejected(GoalError.NOT_FOUND)
        val goal = row.goal.toDomain()
        GoalRules.validateArchive(goal, row.reserved())?.let { return@withTransaction Rejected(it) }
        val now = clock.instant()
        save(goal.copy(archivedAt = now, updatedAt = now))
    }

    fun observeGoals(includeArchived: Boolean = false) = dao.observeGoals(includeArchived)
        .map { rows -> rows.map { it.toSummary() } }.distinctUntilChanged()

    fun observeGoal(id: UUID) = dao.observeGoal(id.toString())
        .map { it?.toSummary() }.distinctUntilChanged()

    fun observeMovements(id: UUID) = dao.observeMovements(id.toString())
        .map { rows -> rows.map { it.toDomain() } }.distinctUntilChanged()

    fun observeReserveBalances() = database.invalidationTrackerFlow(
        "accounts", "financial_transactions", "goals", "goal_movements",
    ).map { readReserveBalances() }.distinctUntilChanged()

    internal suspend fun readReserveBalances() = database.withTransaction {
        val inputs = dao.readAccounts().map { account ->
            val net = dao.readWideNet(account.id, LocalDate.parse(account.openingDate).toEpochDay()).toCents()
            AccountReserveInput(
                UUID.fromString(account.id), BigInteger.valueOf(account.openingBalanceCents) + net,
                dao.readWideReserve(account.id).toCents(), account.includeInAvailable && account.archivedAt == null,
            )
        }
        ReserveBalanceCalculator.calculateWide(inputs)
    }

    fun observeAccountReserves(id: UUID) = observeReserveBalances()
        .map { it.accounts[id] }.distinctUntilChanged()

    private suspend fun validateAccount(id: UUID): GoalError? {
        val account = transactions.findAccount(id.toString()) ?: return GoalError.ACCOUNT_NOT_FOUND
        return GoalError.ACCOUNT_ARCHIVED.takeIf { account.archivedAt != null }
    }

    private suspend fun save(goal: Goal): GoalResult<Goal> {
        dao.update(goal.toEntity())
        return Saved(goal)
    }

    private suspend fun record(goal: Goal, type: GoalMovementType, amount: Money): GoalResult<GoalMovement> {
        val now = clock.instant()
        val movement = GoalMovement(UUID.randomUUID(), goal.id, type, amount, calendar.today(), now)
        dao.insertMovement(movement.toEntity())
        dao.update(goal.copy(updatedAt = now).toEntity())
        return Saved(movement)
    }

    private fun WideCashTotal.toCents(): BigInteger =
        BigInteger.valueOf(high).shiftLeft(32) + BigInteger.valueOf(low)

    private fun GoalDetails.normalized() = copy(name = name.trim())
}
