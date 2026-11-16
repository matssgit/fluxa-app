package com.fluxa.personal.ui.feature

import com.fluxa.personal.core.money.Money
import com.fluxa.personal.data.repository.CategoryRepository
import com.fluxa.personal.domain.model.CreditCard
import com.fluxa.personal.domain.model.CreditCardDetails
import com.fluxa.personal.domain.model.CreditCardSummary
import com.fluxa.personal.domain.model.CreditError
import com.fluxa.personal.domain.model.CreditResult
import com.fluxa.personal.domain.model.Installment
import com.fluxa.personal.domain.model.InstallmentStatus
import com.fluxa.personal.domain.model.Purchase
import com.fluxa.personal.domain.model.PurchaseDraft
import com.fluxa.personal.domain.model.PurchaseStatus
import com.fluxa.personal.domain.rules.CreditLimit
import com.fluxa.personal.domain.rules.CreditPurchaseRules
import com.fluxa.personal.ui.feature.credit.CreditSource
import java.io.IOException
import java.time.Instant
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
internal class FakeCreditSource(private val categories: CategoryRepository, private val now: Instant) : CreditSource {
    private data class Store(
        val cards: Map<UUID, CreditCard> = emptyMap(),
        val purchases: Map<UUID, Purchase> = emptyMap(),
        val installments: Map<UUID, List<Installment>> = emptyMap(),
    )

    private val store = MutableStateFlow(Store())
    val createPurchaseIds = mutableListOf<UUID>()
    var failWrites = false
    var failAfterWrite = false

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
}
