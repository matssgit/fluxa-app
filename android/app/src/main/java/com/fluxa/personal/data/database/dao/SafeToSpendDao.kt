package com.fluxa.personal.data.database.dao

import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Query
import androidx.room.Relation
import androidx.room.RawQuery
import androidx.sqlite.db.SupportSQLiteQuery
import androidx.room.Transaction
import com.fluxa.personal.data.database.entity.SubscriptionEntity
import com.fluxa.personal.data.database.entity.SubscriptionResolutionEntity
import com.fluxa.personal.data.database.entity.SubscriptionTermsEntity

internal data class PendingInstallmentRow(
    val installmentId: String,
    val purchaseId: String,
    val cardId: String,
    val dueDate: Long,
    val amountCents: Long,
)

internal data class OpenOccurrenceDate(val occurrenceDate: Long)

internal data class SubscriptionScheduleRow(
    @Embedded val subscription: SubscriptionEntity,
    @Relation(parentColumn = "id", entityColumn = "subscriptionId")
    val terms: List<SubscriptionTermsEntity>,
)

@Dao
internal interface SafeToSpendDao {
    @Query("""
        SELECT i.id AS installmentId, i.purchaseId, p.cardId, i.dueDate, i.amountCents
        FROM credit_installments i
        JOIN credit_purchases p ON p.id = i.purchaseId
        WHERE i.status = 'PENDING' AND p.cancelledAt IS NULL AND i.dueDate < :endExclusive
        ORDER BY i.dueDate, i.id
    """)
    suspend fun readPendingInstallments(endExclusive: Long): List<PendingInstallmentRow>

    @Transaction
    @Query("""
        SELECT * FROM subscriptions
        WHERE anchorDate < :endExclusive
        ORDER BY id
    """)
    suspend fun readSubscriptionSchedules(endExclusive: Long): List<SubscriptionScheduleRow>

    @RawQuery
    suspend fun readOpenDates(query: SupportSQLiteQuery): List<OpenOccurrenceDate>

    @Query("""
        SELECT * FROM subscription_resolutions
        WHERE subscriptionId = :id AND status = 'PENDING' AND occurrenceDate < :endExclusive
    """)
    suspend fun readReopenedOccurrences(id: String, endExclusive: Long): List<SubscriptionResolutionEntity>
}
