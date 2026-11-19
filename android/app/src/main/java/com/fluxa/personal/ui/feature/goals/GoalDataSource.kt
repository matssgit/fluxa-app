package com.fluxa.personal.ui.feature.goals

import com.fluxa.personal.core.money.Money
import com.fluxa.personal.data.repository.GoalRepository
import com.fluxa.personal.domain.model.AccountReserveSummary
import com.fluxa.personal.domain.model.Goal
import com.fluxa.personal.domain.model.GoalDetails
import com.fluxa.personal.domain.model.GoalMovement
import com.fluxa.personal.domain.model.GoalResult
import com.fluxa.personal.domain.model.GoalSummary
import com.fluxa.personal.domain.model.ReserveBalanceSnapshot
import java.util.UUID
import kotlinx.coroutines.flow.Flow

/**
 * Recorte do [GoalRepository] usado pela UI, só para os ViewModels serem testáveis na JVM (o repositório usa
 * `RoomDatabase.withTransaction`). Não acrescenta regra: reserva, restante, progresso e disponível vêm do Codex.
 */
internal interface GoalDataSource {
    suspend fun createGoal(details: GoalDetails): GoalResult<Goal>
    suspend fun updateGoal(id: UUID, details: GoalDetails): GoalResult<Goal>
    suspend fun allocateToGoal(id: UUID, amount: Money): GoalResult<GoalMovement>
    suspend fun releaseFromGoal(id: UUID, amount: Money): GoalResult<GoalMovement>
    suspend fun completeGoal(id: UUID): GoalResult<Goal>
    suspend fun archiveGoal(id: UUID): GoalResult<Goal>
    fun observeGoals(includeArchived: Boolean = false): Flow<List<GoalSummary>>
    fun observeGoal(id: UUID): Flow<GoalSummary?>
    fun observeMovements(id: UUID): Flow<List<GoalMovement>>
    fun observeAccountReserves(accountId: UUID): Flow<AccountReserveSummary?>
    fun observeReserveBalances(): Flow<ReserveBalanceSnapshot>
}

internal class RepositoryGoalDataSource(private val repository: GoalRepository) : GoalDataSource {
    override suspend fun createGoal(details: GoalDetails) = repository.createGoal(details)
    override suspend fun updateGoal(id: UUID, details: GoalDetails) = repository.updateGoal(id, details)
    override suspend fun allocateToGoal(id: UUID, amount: Money) = repository.allocateToGoal(id, amount)
    override suspend fun releaseFromGoal(id: UUID, amount: Money) = repository.releaseFromGoal(id, amount)
    override suspend fun completeGoal(id: UUID) = repository.completeGoal(id)
    override suspend fun archiveGoal(id: UUID) = repository.archiveGoal(id)
    override fun observeGoals(includeArchived: Boolean) = repository.observeGoals(includeArchived)
    override fun observeGoal(id: UUID) = repository.observeGoal(id)
    override fun observeMovements(id: UUID) = repository.observeMovements(id)
    override fun observeAccountReserves(accountId: UUID) = repository.observeAccountReserves(accountId)
    override fun observeReserveBalances() = repository.observeReserveBalances()
}
