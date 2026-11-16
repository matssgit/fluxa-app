package com.fluxa.personal.data.repository

import androidx.room.withTransaction
import com.fluxa.personal.core.date.FinancialCalendar
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
import com.fluxa.personal.domain.model.FinancialTransaction
import com.fluxa.personal.domain.model.InstallmentPaymentError
import com.fluxa.personal.domain.model.InstallmentPaymentResult
import com.fluxa.personal.domain.model.InstallmentPaymentResult.Rejected as PaymentRejected
import com.fluxa.personal.domain.model.InstallmentOrigin
import com.fluxa.personal.domain.model.InstallmentStatus
import com.fluxa.personal.domain.model.Purchase
import com.fluxa.personal.domain.model.PurchaseDraft
import com.fluxa.personal.domain.model.PurchaseStatus
import com.fluxa.personal.domain.model.SubscriptionOccurrenceKey
import com.fluxa.personal.domain.rules.CreditLimit
import com.fluxa.personal.domain.rules.CreditPurchaseRules
import com.fluxa.personal.domain.rules.InstallmentPaymentRules
import com.fluxa.personal.domain.rules.matchesPayment
import java.time.Clock
import java.time.LocalDate
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

internal class CreditRepository(
    private val database: FluxaDatabase,
    private val clock: Clock = Clock.systemUTC(),
    private val calendar: FinancialCalendar = FinancialCalendar(clock),
) {
    private val dao = database.creditDao()
    private val transactions = database.transactionDao()

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

    suspend fun createPurchase(id: UUID, draft: PurchaseDraft): CreditResult<Purchase> =
        createPurchase(id, draft, subscriptionOccurrence = null)

    internal suspend fun createSubscriptionPurchase(id: UUID, draft: PurchaseDraft, key: SubscriptionOccurrenceKey) =
        createPurchase(id, draft, subscriptionOccurrence = key)

    private suspend fun createPurchase(
        id: UUID,
        draft: PurchaseDraft,
        subscriptionOccurrence: SubscriptionOccurrenceKey?,
    ): CreditResult<Purchase> = database.withTransaction {
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
                val purchase = Purchase(
                    id, draft.normalized(), schedule.value.first().dueDate, now, now,
                    subscriptionOccurrence = subscriptionOccurrence,
                )
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
        if (purchase.subscriptionOccurrence != null) return@withTransaction Rejected(CreditError.MANAGED_BY_SUBSCRIPTION)
        cancelActivePurchase(purchase)
    }

    internal suspend fun removeSubscriptionCharge(purchase: Purchase) {
        val key = requireNotNull(purchase.subscriptionOccurrence)
        val id = purchase.id.toString()
        if (purchase.status == PurchaseStatus.ACTIVE) check(cancelActivePurchase(purchase) is Saved)
        check(dao.deleteCancelledInstallments(id) == 1) { "Subscription installment changed during undo." }
        check(dao.deleteCancelledSubscriptionPurchase(id, key.subscriptionId.toString(), key.occurrenceDate.toEpochDay()) == 1) {
            "Subscription purchase changed during undo."
        }
    }

    private suspend fun cancelActivePurchase(purchase: Purchase): CreditResult<Purchase> {
        val id = purchase.id
        if (dao.hasPaidInstallments(id.toString())) return Rejected(CreditError.HAS_PAID_INSTALLMENT)
        val now = clock.instant()
        val cancelled = purchase.copy(cancelledAt = now, updatedAt = now)
        dao.updatePurchase(cancelled.toEntity())
        dao.cancelPendingInstallments(id.toString())
        return Saved(cancelled)
    }

    suspend fun payInstallment(
        installmentId: UUID,
        accountId: UUID,
        paidOn: LocalDate,
    ): InstallmentPaymentResult = database.withTransaction {
        val key = installmentId.toString()
        val installment = dao.findInstallment(key)?.toDomain()
            ?: return@withTransaction PaymentRejected(InstallmentPaymentError.INSTALLMENT_NOT_FOUND)
        val purchase = dao.findPurchase(installment.purchaseId.toString())?.toDomain()
            ?: return@withTransaction PaymentRejected(InstallmentPaymentError.INVALID_PAYMENT_LINK)
        val existing = transactions.findInstallmentPayment(key)?.toDomain()
        val account = if (installment.status == InstallmentStatus.PENDING) {
            transactions.findAccount(accountId.toString())?.toDomain()
        } else {
            null
        }
        val result = InstallmentPaymentRules(calendar.today(), clock.instant()).prepare(
            UUID.randomUUID(), installment, purchase, existing, accountId, account, paidOn,
        )
        if (result is InstallmentPaymentResult.Paid) {
            if (dao.markInstallmentPaid(key) != 1) {
                val current = dao.findInstallment(key)?.toDomain()
                val payment = transactions.findInstallmentPayment(key)?.toDomain()
                return@withTransaction when {
                    current == null -> PaymentRejected(InstallmentPaymentError.INSTALLMENT_NOT_FOUND)
                    current.status == InstallmentStatus.CANCELLED ->
                        PaymentRejected(InstallmentPaymentError.INSTALLMENT_CANCELLED)
                    current.status == InstallmentStatus.PAID && payment != null && payment.matchesPayment(current, purchase) ->
                        InstallmentPaymentResult.AlreadyPaid(payment.id)
                    else -> PaymentRejected(InstallmentPaymentError.PAYMENT_CHANGED)
                }
            }
            transactions.insert(result.transaction.toEntity())
        }
        result
    }

    suspend fun undoInstallmentPayment(
        installmentId: UUID,
        expectedTransactionId: UUID,
    ): InstallmentPaymentResult = database.withTransaction {
        val key = installmentId.toString()
        val installment = dao.findInstallment(key)?.toDomain()
            ?: return@withTransaction PaymentRejected(InstallmentPaymentError.INSTALLMENT_NOT_FOUND)
        if (installment.status == InstallmentStatus.CANCELLED) {
            return@withTransaction PaymentRejected(InstallmentPaymentError.INSTALLMENT_CANCELLED)
        }
        if (installment.status != InstallmentStatus.PAID) {
            return@withTransaction PaymentRejected(InstallmentPaymentError.INSTALLMENT_NOT_PAID)
        }
        val purchase = dao.findPurchase(installment.purchaseId.toString())?.toDomain()
            ?: return@withTransaction PaymentRejected(InstallmentPaymentError.INVALID_PAYMENT_LINK)
        if (purchase.status != PurchaseStatus.ACTIVE) {
            return@withTransaction PaymentRejected(InstallmentPaymentError.PURCHASE_CANCELLED)
        }
        val payment = transactions.findInstallmentPayment(key)?.toDomain()
        if (payment == null || !payment.matchesPayment(installment, purchase)) {
            return@withTransaction PaymentRejected(InstallmentPaymentError.INVALID_PAYMENT_LINK)
        }
        if (payment.id != expectedTransactionId) {
            return@withTransaction PaymentRejected(InstallmentPaymentError.PAYMENT_CHANGED)
        }
        val card = dao.findCardSummary(purchase.details.cardId.toString())?.toDomain()
            ?: return@withTransaction PaymentRejected(InstallmentPaymentError.INVALID_PAYMENT_LINK)
        if (!CreditLimit(card.totalLimit, card.committedAmount).canCommit(installment.amount)) {
            return@withTransaction PaymentRejected(InstallmentPaymentError.INSUFFICIENT_LIMIT_TO_UNDO)
        }
        if (dao.reopenPaidInstallment(key) != 1) {
            return@withTransaction PaymentRejected(InstallmentPaymentError.PAYMENT_CHANGED)
        }
        check(transactions.deleteInstallmentPayment(payment.id.toString(), key) == 1) { "Payment changed during undo." }
        InstallmentPaymentResult.Undone(payment.id)
    }

    fun observeInstallmentOrigin(installmentId: UUID): Flow<InstallmentOrigin?> =
        dao.observeInstallmentOrigin(installmentId.toString()).map { row ->
            row?.let { InstallmentOrigin(UUID.fromString(it.installmentId), UUID.fromString(it.purchaseId), UUID.fromString(it.cardId)) }
        }.distinctUntilChanged()

    fun observeInstallmentPayment(installmentId: UUID): Flow<FinancialTransaction?> =
        transactions.observeInstallmentPayment(installmentId.toString())
            .map { it?.toDomain() }
            .distinctUntilChanged()


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
