package com.fluxa.personal.data.repository

import androidx.room.withTransaction
import com.fluxa.personal.data.database.FluxaDatabase
import com.fluxa.personal.data.database.mapper.toDomain
import com.fluxa.personal.data.database.mapper.toEntity
import com.fluxa.personal.domain.model.CreditCard
import com.fluxa.personal.domain.model.CreditCardDetails
import com.fluxa.personal.domain.model.CreditCardSummary
import com.fluxa.personal.domain.model.CreditError
import com.fluxa.personal.domain.model.CreditResult
import com.fluxa.personal.domain.model.CreditResult.Rejected
import com.fluxa.personal.domain.model.CreditResult.Saved
import com.fluxa.personal.domain.model.Installment
import com.fluxa.personal.domain.model.InstallmentStatus
import com.fluxa.personal.domain.model.Purchase
import com.fluxa.personal.domain.model.PurchaseDraft
import com.fluxa.personal.domain.model.PurchaseStatus
import com.fluxa.personal.domain.rules.CreditLimit
import com.fluxa.personal.domain.rules.CreditPurchaseRules
import java.time.Clock
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

internal class CreditRepository(
    private val database: FluxaDatabase,
    private val clock: Clock = Clock.systemUTC(),
) {
    private val dao = database.creditDao()

    suspend fun createCard(details: CreditCardDetails): CreditResult<CreditCard> {
        validateCard(details)?.let { return Rejected(it) }
        val now = clock.instant()
        val card = CreditCard(UUID.randomUUID(), details.normalized(), null, now, now)
        dao.insertCard(card.toEntity())
        return Saved(card)
    }

    suspend fun updateCard(id: UUID, details: CreditCardDetails): CreditResult<CreditCard> = database.withTransaction {
        validateCard(details)?.let { return@withTransaction Rejected(it) }
        val summary = dao.findCardSummary(id.toString())?.toDomain()
            ?: return@withTransaction Rejected(CreditError.CARD_NOT_FOUND)
        if (!CreditLimit(summary.totalLimit, summary.committedAmount).canSetTotal(details.totalLimit)) {
            return@withTransaction Rejected(CreditError.LIMIT_BELOW_COMMITTED)
        }
        val card = summary.card.copy(details = details.normalized(), updatedAt = clock.instant())
        dao.updateCard(card.toEntity())
        Saved(card)
    }

    suspend fun archiveCard(id: UUID): Boolean = dao.archiveCard(id.toString(), clock.instant().toString()) == 1

    suspend fun createPurchase(id: UUID, draft: PurchaseDraft): CreditResult<Purchase> = database.withTransaction {
        if (dao.findPurchase(id.toString()) != null) return@withTransaction Rejected(CreditError.DUPLICATE_PURCHASE_ID)
        val schedule = CreditPurchaseRules.schedule(
            draft,
            dao.findCardSummary(draft.cardId.toString())?.toDomain(),
            dao.findCategory(draft.categoryId.toString())?.toDomain(),
        )
        when (schedule) {
            is Rejected -> schedule
            is Saved -> {
                val now = clock.instant()
                val purchase = Purchase(id, draft.normalized(), schedule.value.first().dueDate, now, now)
                dao.insertPurchase(purchase.toEntity())
                dao.insertInstallments(schedule.value.map { part ->
                    Installment(UUID.randomUUID(), id, part.number, part.amount, part.dueDate, InstallmentStatus.PENDING)
                        .toEntity()
                })
                Saved(purchase)
            }
        }
    }

    suspend fun cancelPurchase(id: UUID): CreditResult<Purchase> = database.withTransaction {
        val purchase = dao.findPurchase(id.toString())?.toDomain()
            ?: return@withTransaction Rejected(CreditError.PURCHASE_NOT_FOUND)
        if (purchase.status == PurchaseStatus.CANCELLED) return@withTransaction Rejected(CreditError.PURCHASE_CANCELLED)
        if (dao.hasPaidInstallments(id.toString())) return@withTransaction Rejected(CreditError.HAS_PAID_INSTALLMENT)

        val now = clock.instant()
        val cancelled = purchase.copy(cancelledAt = now, updatedAt = now)
        dao.updatePurchase(cancelled.toEntity())
        dao.cancelPendingInstallments(id.toString())
        Saved(cancelled)
    }

    fun observeCards(includeArchived: Boolean = false): Flow<List<CreditCardSummary>> =
        dao.observeCards(includeArchived).map { rows -> rows.map { it.toDomain() } }.distinctUntilChanged()

    fun observeCard(id: UUID): Flow<CreditCardSummary?> =
        dao.observeCard(id.toString()).map { it?.toDomain() }.distinctUntilChanged()

    fun observePurchases(cardId: UUID, status: PurchaseStatus? = null): Flow<List<Purchase>> =
        dao.observePurchases(cardId.toString(), status?.let { it == PurchaseStatus.CANCELLED })
            .map { rows -> rows.map { it.toDomain() } }.distinctUntilChanged()

    fun observeInstallments(purchaseId: UUID): Flow<List<Installment>> =
        dao.observeInstallments(purchaseId.toString()).map { rows -> rows.map { it.toDomain() } }.distinctUntilChanged()

    private fun validateCard(details: CreditCardDetails): CreditError? = when {
        details.name.isBlank() -> CreditError.INVALID_NAME
        details.totalLimit.cents < 0 -> CreditError.INVALID_LIMIT
        else -> null
    }

    private fun CreditCardDetails.normalized() = copy(name = name.trim(), color = color?.trim()?.ifEmpty { null })

    private fun PurchaseDraft.normalized() = copy(
        title = title.trim(),
        merchant = merchant?.trim()?.ifEmpty { null },
        notes = notes?.trim()?.ifEmpty { null },
    )
}
