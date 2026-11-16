package com.fluxa.personal.ui.feature

import com.fluxa.personal.core.money.Money
import com.fluxa.personal.data.repository.AccountRepository
import com.fluxa.personal.data.repository.CategoryRepository
import com.fluxa.personal.domain.model.CreditCard
import com.fluxa.personal.domain.model.CreditCardDetails
import com.fluxa.personal.domain.model.CreditCardSummary
import com.fluxa.personal.domain.model.CreditError
import com.fluxa.personal.domain.model.CreditResult
import com.fluxa.personal.domain.model.FinancialTransaction
import com.fluxa.personal.domain.model.Installment
import com.fluxa.personal.domain.model.InstallmentOrigin
import com.fluxa.personal.domain.model.InstallmentPaymentError
import com.fluxa.personal.domain.model.InstallmentPaymentResult
import com.fluxa.personal.domain.model.InstallmentStatus
import com.fluxa.personal.domain.model.Purchase
import com.fluxa.personal.domain.model.PurchaseDraft
import com.fluxa.personal.domain.model.PurchaseStatus
import com.fluxa.personal.domain.rules.CreditLimit
import com.fluxa.personal.domain.rules.CreditPurchaseRules
import com.fluxa.personal.domain.rules.InstallmentPaymentRules
import com.fluxa.personal.domain.rules.matchesPayment
import com.fluxa.personal.ui.feature.credit.CreditSource
import java.io.IOException
import java.time.Instant
import java.time.LocalDate
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/**
 * Crédito em memória para testes JVM. Decisões de negócio vêm do domínio do Codex: `CreditPurchaseRules`
 * (validação, limite, calendário de fechamento e `InstallmentSplitter`) e `CreditLimit`. A soma do comprometido
 * substitui o `SUM` do SQLite (parcelas pendentes de compras ativas).
 */
internal class FakeCreditSource(
    private val categories: CategoryRepository,
    private val now: Instant,
    /** Contas e ledger para a baixa de parcelas (Sprint 4-D). A saída gerada vai para [ledger].rows. */
    private val accounts: AccountRepository? = null,
    private val ledger: FakeTransactionSource? = null,
    private val today: LocalDate = LocalDate.of(2026, 11, 16),
) : CreditSource {
    private data class Store(
        val cards: Map<UUID, CreditCard> = emptyMap(),
        val purchases: Map<UUID, Purchase> = emptyMap(),
        val installments: Map<UUID, List<Installment>> = emptyMap(),
    )

    private val store = MutableStateFlow(Store())
    val createPurchaseIds = mutableListOf<UUID>()
    var failWrites = false
    var failAfterWrite = false
    /** Lança exceção nas chamadas de pagamento/estorno (falha inesperada, sem resposta tipada). */
    var failPayments = false
    val payCalls = mutableListOf<Triple<UUID, UUID, LocalDate>>()
    /** Força a próxima baixa a ser rejeitada com este erro (ex.: conta arquivada entre a tela e o comando). */
    var rejectNextPayment: InstallmentPaymentError? = null

    private fun summaryOf(s: Store, card: CreditCard): CreditCardSummary {
        val committed = s.purchases.values
            .filter { it.details.cardId == card.id && it.cancelledAt == null }
            .flatMap { s.installments[it.id].orEmpty() }
            .filter { it.status == InstallmentStatus.PENDING }
            .sumOf { it.amount.cents }
        return CreditCardSummary(card, Money(committed))
    }

    /** Marca uma parcela como paga (o fluxo de baixa não existe na UI; usado para testar o bloqueio). */
    fun markPaid(purchaseId: UUID, number: Int) = store.update { s ->
        s.copy(installments = s.installments + (purchaseId to s.installments[purchaseId].orEmpty().map {
            if (it.number == number) it.copy(status = InstallmentStatus.PAID) else it
        }))
    }

    override suspend fun createCard(details: CreditCardDetails): CreditResult<CreditCard> {
        if (failWrites) throw IOException("disk full")
        if (details.name.isBlank()) return CreditResult.Rejected(CreditError.INVALID_NAME)
        if (details.totalLimit.cents < 0) return CreditResult.Rejected(CreditError.INVALID_LIMIT)
        val card = CreditCard(UUID.randomUUID(), details.copy(name = details.name.trim()), null, now, now)
        store.update { it.copy(cards = it.cards + (card.id to card)) }
        return CreditResult.Saved(card)
    }

    override suspend fun updateCard(id: UUID, details: CreditCardDetails): CreditResult<CreditCard> {
        if (failWrites) throw IOException("disk full")
        val s = store.value
        val card = s.cards[id] ?: return CreditResult.Rejected(CreditError.CARD_NOT_FOUND)
        val summary = summaryOf(s, card)
        if (!CreditLimit(summary.totalLimit, summary.committedAmount).canSetTotal(details.totalLimit)) {
            return CreditResult.Rejected(CreditError.LIMIT_BELOW_COMMITTED)
        }
        val updated = card.copy(details = details.copy(name = details.name.trim()), updatedAt = now)
        store.update { it.copy(cards = it.cards + (id to updated)) }
        return CreditResult.Saved(updated)
    }

    override suspend fun archiveCard(id: UUID): Boolean {
        val card = store.value.cards[id] ?: return false
        if (card.archivedAt != null) return false
        store.update { it.copy(cards = it.cards + (id to card.copy(archivedAt = now))) }
        return true
    }

    override suspend fun createPurchase(id: UUID, draft: PurchaseDraft): CreditResult<Purchase> {
        if (failWrites) throw IOException("disk full")
        createPurchaseIds += id
        val s = store.value
        if (s.purchases.containsKey(id)) return CreditResult.Rejected(CreditError.DUPLICATE_PURCHASE_ID)
        val summary = s.cards[draft.cardId]?.let { summaryOf(s, it) }
        val category = categories.observe(draft.categoryId).first()
        return when (val schedule = CreditPurchaseRules.schedule(draft, summary, category)) {
            is CreditResult.Rejected -> schedule
            is CreditResult.Saved -> {
                val purchase = Purchase(id, draft.copy(title = draft.title.trim()), schedule.value.first().dueDate, now, now)
                val parts = schedule.value.map {
                    Installment(UUID.randomUUID(), id, it.number, it.amount, it.dueDate, InstallmentStatus.PENDING)
                }
                store.update { it.copy(purchases = it.purchases + (id to purchase), installments = it.installments + (id to parts)) }
                if (failAfterWrite) {
                    failAfterWrite = false
                    throw IOException("connection reset after write")
                }
                CreditResult.Saved(purchase)
            }
        }
    }

    override suspend fun cancelPurchase(id: UUID): CreditResult<Purchase> {
        if (failWrites) throw IOException("disk full")
        val s = store.value
        val purchase = s.purchases[id] ?: return CreditResult.Rejected(CreditError.PURCHASE_NOT_FOUND)
        if (purchase.status == PurchaseStatus.CANCELLED) return CreditResult.Rejected(CreditError.PURCHASE_CANCELLED)
        if (s.installments[id].orEmpty().any { it.status == InstallmentStatus.PAID }) return CreditResult.Rejected(CreditError.HAS_PAID_INSTALLMENT)
        val cancelled = purchase.copy(cancelledAt = now, updatedAt = now)
        store.update {
            it.copy(
                purchases = it.purchases + (id to cancelled),
                installments = it.installments + (id to it.installments[id].orEmpty().map { p ->
                    if (p.status == InstallmentStatus.PENDING) p.copy(status = InstallmentStatus.CANCELLED) else p
                }),
            )
        }
        return CreditResult.Saved(cancelled)
    }

    override fun observeCards(includeArchived: Boolean): Flow<List<CreditCardSummary>> = store.map { s ->
        s.cards.values.filter { includeArchived || it.archivedAt == null }
            .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { c: CreditCard -> c.details.name }.thenBy { it.id.toString() })
            .map { summaryOf(s, it) }
    }

    override fun observeCard(id: UUID): Flow<CreditCardSummary?> = store.map { s -> s.cards[id]?.let { summaryOf(s, it) } }

    override fun observePurchases(cardId: UUID, status: PurchaseStatus?): Flow<List<Purchase>> = store.map { s ->
        s.purchases.values.filter { it.details.cardId == cardId && (status == null || it.status == status) }
            .sortedWith(compareByDescending<Purchase> { it.details.purchaseDate }.thenBy { it.id.toString() })
    }

    override fun observeInstallments(purchaseId: UUID): Flow<List<Installment>> =
        store.map { s -> s.installments[purchaseId].orEmpty().sortedBy { it.number } }

    // region Sprint 4-C: baixa e estorno (mesma sequência do CreditRepository, com as regras do domínio)

    private fun installmentById(id: UUID): Installment? = store.value.installments.values.flatten().firstOrNull { it.id == id }

    private fun setStatus(id: UUID, status: InstallmentStatus) = store.update { s ->
        s.copy(installments = s.installments.mapValues { (_, list) -> list.map { if (it.id == id) it.copy(status = status) else it } })
    }

    private fun paymentOf(installmentId: UUID): FinancialTransaction? =
        ledger?.rows?.value?.values?.firstOrNull { it.installmentId == installmentId }

    override suspend fun payInstallment(installmentId: UUID, accountId: UUID, paidOn: LocalDate): InstallmentPaymentResult {
        if (failPayments) throw IOException("disk full")
        payCalls += Triple(installmentId, accountId, paidOn)
        rejectNextPayment?.let { rejectNextPayment = null; return InstallmentPaymentResult.Rejected(it) }
        val installment = installmentById(installmentId)
            ?: return InstallmentPaymentResult.Rejected(InstallmentPaymentError.INSTALLMENT_NOT_FOUND)
        val purchase = store.value.purchases[installment.purchaseId]
            ?: return InstallmentPaymentResult.Rejected(InstallmentPaymentError.INVALID_PAYMENT_LINK)
        val account = if (installment.status == InstallmentStatus.PENDING) accounts?.observe(accountId)?.first() else null
        val result = InstallmentPaymentRules(today, now).prepare(
            UUID.randomUUID(), installment, purchase, paymentOf(installmentId), accountId, account, paidOn,
        )
        if (result is InstallmentPaymentResult.Paid) {
            setStatus(installmentId, InstallmentStatus.PAID)
            ledger!!.rows.update { it + (result.transaction.id to result.transaction) }
        }
        return result
    }

    override suspend fun undoInstallmentPayment(installmentId: UUID, expectedTransactionId: UUID): InstallmentPaymentResult {
        if (failPayments) throw IOException("disk full")
        val installment = installmentById(installmentId)
            ?: return InstallmentPaymentResult.Rejected(InstallmentPaymentError.INSTALLMENT_NOT_FOUND)
        if (installment.status == InstallmentStatus.CANCELLED) return InstallmentPaymentResult.Rejected(InstallmentPaymentError.INSTALLMENT_CANCELLED)
        if (installment.status != InstallmentStatus.PAID) return InstallmentPaymentResult.Rejected(InstallmentPaymentError.INSTALLMENT_NOT_PAID)
        val purchase = store.value.purchases[installment.purchaseId]!!
        if (purchase.status != PurchaseStatus.ACTIVE) return InstallmentPaymentResult.Rejected(InstallmentPaymentError.PURCHASE_CANCELLED)
        val payment = paymentOf(installmentId)
        if (payment == null || !payment.matchesPayment(installment, purchase)) {
            return InstallmentPaymentResult.Rejected(InstallmentPaymentError.INVALID_PAYMENT_LINK)
        }
        if (payment.id != expectedTransactionId) return InstallmentPaymentResult.Rejected(InstallmentPaymentError.PAYMENT_CHANGED)
        val summary = summaryOf(store.value, store.value.cards.getValue(purchase.details.cardId))
        if (!CreditLimit(summary.totalLimit, summary.committedAmount).canCommit(installment.amount)) {
            return InstallmentPaymentResult.Rejected(InstallmentPaymentError.INSUFFICIENT_LIMIT_TO_UNDO)
        }
        setStatus(installmentId, InstallmentStatus.PENDING)
        ledger!!.rows.update { it - payment.id }
        return InstallmentPaymentResult.Undone(payment.id)
    }

    override fun observeInstallmentPayment(installmentId: UUID): Flow<FinancialTransaction?> =
        ledger!!.rows.map { rows -> rows.values.firstOrNull { it.installmentId == installmentId && it.cancelledAt == null } }

    // endregion

    override fun observeInstallmentOrigin(installmentId: UUID): Flow<InstallmentOrigin?> = store.map { s ->
        s.installments.values.flatten().firstOrNull { it.id == installmentId }?.let { i ->
            s.purchases[i.purchaseId]?.let { p -> InstallmentOrigin(i.id, p.id, p.details.cardId) }
        }
    }
}
