package com.fluxa.personal.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.fluxa.personal.data.database.entity.AccountEntity
import com.fluxa.personal.data.database.entity.CategoryEntity
import com.fluxa.personal.data.database.entity.TransactionEntity
import kotlinx.coroutines.flow.Flow

internal const val PERIOD_TOTAL_RADIX = 4294967296L

internal data class PeriodDirectionTotal(val direction: String, val cents: Long)

internal data class WidePeriodDirectionTotal(val direction: String, val high: Long, val low: Long)

internal data class BalanceRow(
    val accountId: String,
    val openingBalanceCents: Long,
    val openingDate: String,
    val includeInAvailable: Boolean,
    val archivedAt: String?,
    val direction: String?,
    val amountCents: Long?,
    val occurredOn: Long?,
)

@Dao
internal interface TransactionDao {
    @Insert
    suspend fun insert(transaction: TransactionEntity)

    @Update
    suspend fun update(transaction: TransactionEntity)

    @Query("SELECT * FROM financial_transactions WHERE id = :id")
    suspend fun find(id: String): TransactionEntity?

    @Query("SELECT * FROM accounts WHERE id = :id")
    suspend fun findAccount(id: String): AccountEntity?

    @Query("SELECT * FROM categories WHERE id = :id")
    suspend fun findCategory(id: String): CategoryEntity?

    @Query("SELECT * FROM financial_transactions WHERE id = :id")
    fun observe(id: String): Flow<TransactionEntity?>

    @Query(
        """
        SELECT * FROM financial_transactions
        WHERE (:accountId IS NULL OR accountId = :accountId)
          AND (:categoryId IS NULL OR categoryId = :categoryId)
          AND occurredOn >= :fromInclusive
          AND occurredOn < :untilExclusive
          AND (:includeCancelled OR cancelledAt IS NULL)
          AND (:direction IS NULL OR direction = :direction)
        ORDER BY occurredOn DESC, id
        LIMIT :limit OFFSET :offset
        """,
    )
    fun observeHistory(
        accountId: String?,
        categoryId: String?,
        fromInclusive: Long,
        untilExclusive: Long,
        includeCancelled: Boolean,
        direction: String?,
        limit: Int,
        offset: Int,
    ): Flow<List<TransactionEntity>>

    @Query(
        """
        SELECT * FROM financial_transactions
        WHERE cancelledAt IS NULL
        ORDER BY occurredOn DESC, id
        LIMIT :limit
        """,
    )
    fun observeRecent(limit: Int): Flow<List<TransactionEntity>>

    @Query(
        """
        SELECT direction, SUM(amountCents) AS cents
        FROM financial_transactions
        WHERE cancelledAt IS NULL AND occurredOn >= :startInclusive AND occurredOn < :endExclusive
        GROUP BY direction
        """,
    )
    suspend fun readPeriodTotals(startInclusive: Long, endExclusive: Long): List<PeriodDirectionTotal>

    // If SUM overflows, aggregate two smaller integer parts instead of reading the ledger into Kotlin.
    @Query(
        """
        SELECT direction, SUM(amountCents / $PERIOD_TOTAL_RADIX) AS high,
               SUM(amountCents % $PERIOD_TOTAL_RADIX) AS low
        FROM financial_transactions
        WHERE cancelledAt IS NULL AND occurredOn >= :startInclusive AND occurredOn < :endExclusive
        GROUP BY direction
        """,
    )
    suspend fun readWidePeriodTotals(startInclusive: Long, endExclusive: Long): List<WidePeriodDirectionTotal>

    @Query(
        """
        SELECT a.id AS accountId, a.openingBalanceCents, a.openingDate, a.includeInAvailable,
               a.archivedAt, t.direction, t.amountCents, t.occurredOn
        FROM accounts a
        LEFT JOIN financial_transactions t ON t.accountId = a.id AND t.cancelledAt IS NULL
        """,
    )
    fun observeBalanceRows(): Flow<List<BalanceRow>>
}
