package com.fluxa.personal.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.fluxa.personal.data.database.entity.AccountEntity
import kotlinx.coroutines.flow.Flow

@Dao
internal interface AccountDao {
    @Insert
    suspend fun insert(account: AccountEntity)

    @Query("SELECT * FROM accounts WHERE id = :id")
    fun observe(id: String): Flow<AccountEntity?>

    @Query(
        """
        SELECT * FROM accounts
        WHERE :includeArchived OR archivedAt IS NULL
        ORDER BY name COLLATE NOCASE, id
        """,
    )
    fun observeAll(includeArchived: Boolean): Flow<List<AccountEntity>>

    @Query(
        """
        UPDATE accounts
        SET name = :name, type = :type, openingBalanceCents = :openingBalanceCents,
            openingDate = :openingDate, includeInAvailable = :includeInAvailable,
            updatedAt = :updatedAt
        WHERE id = :id
        """,
    )
    suspend fun update(
        id: String,
        name: String,
        type: String,
        openingBalanceCents: Long,
        openingDate: String,
        includeInAvailable: Boolean,
        updatedAt: String,
    ): Int

    @Query(
        """
        UPDATE accounts SET archivedAt = :archivedAt, updatedAt = :archivedAt
        WHERE id = :id AND archivedAt IS NULL
        """,
    )
    suspend fun archive(id: String, archivedAt: String): Int
}
