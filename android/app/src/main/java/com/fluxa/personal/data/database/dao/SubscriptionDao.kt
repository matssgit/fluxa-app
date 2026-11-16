package com.fluxa.personal.data.database.dao

import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Relation
import androidx.room.Transaction
import androidx.room.Update
import com.fluxa.personal.data.database.entity.SubscriptionEntity
import com.fluxa.personal.data.database.entity.SubscriptionResolutionEntity
import com.fluxa.personal.data.database.entity.SubscriptionTermsEntity
import kotlinx.coroutines.flow.Flow

internal data class SubscriptionAggregate(
    @Embedded val subscription: SubscriptionEntity,
    @Relation(parentColumn = "id", entityColumn = "subscriptionId")
    val terms: List<SubscriptionTermsEntity>,
    @Relation(parentColumn = "id", entityColumn = "subscriptionId")
    val resolutions: List<SubscriptionResolutionEntity>,
)

internal data class SubscriptionDetailRow(
    @Embedded val aggregate: SubscriptionAggregate,
    val transactionDate: Long?,
    val purchaseDate: Long?,
    val installmentId: String?,
)

@Dao
internal interface SubscriptionDao {
    @Insert suspend fun insert(subscription: SubscriptionEntity)
    @Update suspend fun update(subscription: SubscriptionEntity)
    @Insert suspend fun insertTerms(terms: SubscriptionTermsEntity)
    @Insert suspend fun resolve(resolution: SubscriptionResolutionEntity)

    @Update suspend fun updateResolution(resolution: SubscriptionResolutionEntity): Int

    @Query("DELETE FROM subscription_resolutions WHERE subscriptionId = :id AND occurrenceDate = :date AND status = 'PENDING'")
    suspend fun deleteReopened(id: String, date: Long): Int

    @Transaction
    @Query("""
        SELECT s.*, t.occurredOn AS transactionDate, p.purchaseDate, i.id AS installmentId
        FROM subscriptions s
        LEFT JOIN subscription_resolutions r ON r.subscriptionId = s.id AND r.occurrenceDate = :date
        LEFT JOIN financial_transactions t ON r.status = 'PAID' AND t.id = r.resultId
          AND t.subscriptionId = r.subscriptionId AND t.occurrenceDate = r.occurrenceDate AND t.cancelledAt IS NULL
        LEFT JOIN credit_purchases p ON r.status = 'CHARGED' AND p.id = r.resultId
          AND p.subscriptionId = r.subscriptionId AND p.occurrenceDate = r.occurrenceDate
        LEFT JOIN credit_installments i ON i.purchaseId = p.id AND i.number = 1
        WHERE s.id = :id
    """)
    fun observeDetail(id: String, date: Long): Flow<SubscriptionDetailRow?>

    @Query("DELETE FROM subscription_terms WHERE subscriptionId = :id AND effectiveOn >= :from")
    suspend fun deleteTermsFrom(id: String, from: Long)

    @Transaction
    @Query("SELECT * FROM subscriptions WHERE id = :id")
    suspend fun find(id: String): SubscriptionAggregate?

    @Transaction
    @Query("SELECT * FROM subscriptions WHERE id = :id")
    fun observe(id: String): Flow<SubscriptionAggregate?>

    @Transaction
    @Query("SELECT * FROM subscriptions ORDER BY name COLLATE NOCASE, id")
    fun observeAll(): Flow<List<SubscriptionAggregate>>
}
