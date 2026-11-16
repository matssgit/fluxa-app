package com.fluxa.personal.data.database.dao

import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.fluxa.personal.data.database.entity.CategoryEntity
import com.fluxa.personal.data.database.entity.CreditCardEntity
import com.fluxa.personal.data.database.entity.InstallmentEntity
import com.fluxa.personal.data.database.entity.PurchaseEntity
import kotlinx.coroutines.flow.Flow

private const val CARD_SUMMARIES = """
    SELECT c.*, COALESCE((
        SELECT SUM(i.amountCents)
        FROM credit_purchases p
        JOIN credit_installments i ON i.purchaseId = p.id
        WHERE p.cardId = c.id AND p.cancelledAt IS NULL AND i.status = 'PENDING'
    ), 0) AS committedAmountCents
    FROM credit_cards c
"""

internal data class CreditCardSummaryRow(
    @Embedded val card: CreditCardEntity,
    val committedAmountCents: Long,
)

internal data class InstallmentOriginRow(val installmentId: String, val purchaseId: String, val cardId: String)

@Dao
internal interface CreditDao {
    @Insert
    suspend fun insertCard(card: CreditCardEntity)

    @Update
    suspend fun updateCard(card: CreditCardEntity)

    @Query("UPDATE credit_cards SET archivedAt = :now, updatedAt = :now WHERE id = :id AND archivedAt IS NULL")
    suspend fun archiveCard(id: String, now: String): Int

    @Query(CARD_SUMMARIES + " WHERE c.id = :id")
    suspend fun findCardSummary(id: String): CreditCardSummaryRow?

    @Query(CARD_SUMMARIES + " WHERE c.id = :id")
    fun observeCard(id: String): Flow<CreditCardSummaryRow?>

    @Query(CARD_SUMMARIES + " WHERE (:includeArchived OR c.archivedAt IS NULL) ORDER BY c.name COLLATE NOCASE, c.id")
    fun observeCards(includeArchived: Boolean): Flow<List<CreditCardSummaryRow>>

    @Query("SELECT * FROM categories WHERE id = :id")
    suspend fun findCategory(id: String): CategoryEntity?

    @Query("SELECT * FROM credit_purchases WHERE id = :id")
    suspend fun findPurchase(id: String): PurchaseEntity?

    @Query(
        """
        SELECT * FROM credit_purchases WHERE cardId = :cardId
          AND (:cancelled IS NULL OR (cancelledAt IS NOT NULL) = :cancelled)
        ORDER BY purchaseDate DESC, id
        """,
    )
    fun observePurchases(cardId: String, cancelled: Boolean?): Flow<List<PurchaseEntity>>

    @Query("SELECT * FROM credit_installments WHERE purchaseId = :purchaseId ORDER BY number")
    fun observeInstallments(purchaseId: String): Flow<List<InstallmentEntity>>

    @Insert
    suspend fun insertPurchase(purchase: PurchaseEntity)

    @Insert
    suspend fun insertInstallments(installments: List<InstallmentEntity>)

    @Query(
        """
        SELECT i.id AS installmentId, i.purchaseId, p.cardId FROM credit_installments i
        JOIN credit_purchases p ON p.id = i.purchaseId WHERE i.id = :id
        """,
    )
    fun observeInstallmentOrigin(id: String): Flow<InstallmentOriginRow?>

    @Query("SELECT * FROM credit_installments WHERE id = :id")
    suspend fun findInstallment(id: String): InstallmentEntity?

    @Query("UPDATE credit_installments SET status = 'PAID' WHERE id = :id AND status = 'PENDING'")
    suspend fun markInstallmentPaid(id: String): Int

    @Query("UPDATE credit_installments SET status = 'PENDING' WHERE id = :id AND status = 'PAID'")
    suspend fun reopenPaidInstallment(id: String): Int

    @Query("SELECT EXISTS(SELECT 1 FROM credit_installments WHERE purchaseId = :purchaseId AND status = 'PAID')")
    suspend fun hasPaidInstallments(purchaseId: String): Boolean

    @Update
    suspend fun updatePurchase(purchase: PurchaseEntity)

    @Query("SELECT * FROM credit_installments WHERE purchaseId = :purchaseId ORDER BY number")
    suspend fun findPurchaseInstallments(purchaseId: String): List<InstallmentEntity>

    @Query("DELETE FROM credit_installments WHERE purchaseId = :purchaseId AND status = 'CANCELLED'")
    suspend fun deleteCancelledInstallments(purchaseId: String): Int

    @Query("""
        DELETE FROM credit_purchases WHERE id = :id AND subscriptionId = :subscriptionId
          AND occurrenceDate = :date AND cancelledAt IS NOT NULL
    """)
    suspend fun deleteCancelledSubscriptionPurchase(id: String, subscriptionId: String, date: Long): Int

    @Query("UPDATE credit_installments SET status = 'CANCELLED' WHERE purchaseId = :purchaseId AND status = 'PENDING'")
    suspend fun cancelPendingInstallments(purchaseId: String)
}
