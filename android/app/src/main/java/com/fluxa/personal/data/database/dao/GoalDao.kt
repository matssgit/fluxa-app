package com.fluxa.personal.data.database.dao

import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Relation
import androidx.room.Transaction
import androidx.room.Update
import com.fluxa.personal.data.database.entity.AccountEntity
import com.fluxa.personal.data.database.entity.GoalEntity
import com.fluxa.personal.data.database.entity.GoalMovementEntity
import kotlinx.coroutines.flow.Flow

internal data class GoalWithMovements(
    @Embedded val goal: GoalEntity,
    @Relation(parentColumn = "id", entityColumn = "goalId")
    val movements: List<GoalMovementEntity>,
)

internal data class GoalWithAccount(
    @Embedded val row: GoalWithMovements,
    @Relation(parentColumn = "accountId", entityColumn = "id")
    val account: AccountEntity,
)

internal data class WideCashTotal(val high: Long, val low: Long)

@Dao
internal interface GoalDao {
    @Query("SELECT * FROM accounts ORDER BY id")
    suspend fun readAccounts(): List<AccountEntity>

    @Query("""
        SELECT COALESCE(SUM(CASE direction WHEN 'INCOME' THEN amountCents / 4294967296 ELSE -(amountCents / 4294967296) END), 0) AS high,
               COALESCE(SUM(CASE direction WHEN 'INCOME' THEN amountCents % 4294967296 ELSE -(amountCents % 4294967296) END), 0) AS low
        FROM financial_transactions
        WHERE accountId = :id AND cancelledAt IS NULL AND occurredOn >= :openingDate
    """)
    suspend fun readWideNet(id: String, openingDate: Long): WideCashTotal

    @Query("""
        SELECT COALESCE(SUM(CASE m.type WHEN 'ALLOCATE' THEN m.amountCents / 4294967296 ELSE -(m.amountCents / 4294967296) END), 0) AS high,
               COALESCE(SUM(CASE m.type WHEN 'ALLOCATE' THEN m.amountCents % 4294967296 ELSE -(m.amountCents % 4294967296) END), 0) AS low
        FROM goal_movements m JOIN goals g ON g.id = m.goalId WHERE g.accountId = :id
    """)
    suspend fun readWideReserve(id: String): WideCashTotal

    @Insert suspend fun insert(goal: GoalEntity)
    @Update suspend fun update(goal: GoalEntity)
    @Insert suspend fun insertMovement(movement: GoalMovementEntity)

    @Transaction
    @Query("SELECT * FROM goals WHERE id = :id")
    suspend fun find(id: String): GoalWithMovements?

    @Transaction
    @Query("SELECT * FROM goals WHERE :accountId IS NULL OR accountId = :accountId")
    suspend fun readGoals(accountId: String? = null): List<GoalWithMovements>

    @Transaction
    @Query("SELECT * FROM goals WHERE :includeArchived OR archivedAt IS NULL ORDER BY name COLLATE NOCASE, id")
    fun observeGoals(includeArchived: Boolean): Flow<List<GoalWithAccount>>

    @Transaction
    @Query("SELECT * FROM goals WHERE id = :id")
    fun observeGoal(id: String): Flow<GoalWithAccount?>

    @Query("SELECT * FROM goal_movements WHERE goalId = :goalId ORDER BY occurredOn DESC, createdAt DESC, id DESC")
    fun observeMovements(goalId: String): Flow<List<GoalMovementEntity>>
}
