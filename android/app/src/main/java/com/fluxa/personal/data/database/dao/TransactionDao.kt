package com.fluxa.personal.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.fluxa.personal.data.database.entity.AccountEntity
import com.fluxa.personal.data.database.entity.CategoryEntity
import com.fluxa.personal.data.database.entity.TransactionEntity
import kotlinx.coroutines.flow.Flow

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
          AND (:fromInclusive IS NULL OR occurredOn >= :fromInclusive)
          AND (:untilExclusive IS NULL OR occurredOn < :untilExclusive)
          AND (:includeCancelled OR cancelledAt IS NULL)
        ORDER BY occurredOn DESC, id
        """,
    )
    fun observeHistory(
        accountId: String?,
        categoryId: String?,
        fromInclusive: Long?,
        untilExclusive: Long?,
        includeCancelled: Boolean,
    ): Flow<List<TransactionEntity>>

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
