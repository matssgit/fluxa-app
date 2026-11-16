package com.fluxa.personal.ui.feature

import com.fluxa.personal.core.date.BillingDay
import com.fluxa.personal.data.repository.AccountRepository
import com.fluxa.personal.data.repository.CategoryRepository
import com.fluxa.personal.domain.model.CreditError
import com.fluxa.personal.domain.model.CreditResult
import com.fluxa.personal.domain.model.FinancialTransaction
import com.fluxa.personal.domain.model.PurchaseDraft
import com.fluxa.personal.domain.model.Subscription
import com.fluxa.personal.domain.model.SubscriptionDetails
import com.fluxa.personal.domain.model.SubscriptionError
import com.fluxa.personal.domain.model.SubscriptionFrequency
import com.fluxa.personal.domain.model.SubscriptionOccurrence
import com.fluxa.personal.domain.model.SubscriptionOccurrenceKey
import com.fluxa.personal.domain.model.SubscriptionOccurrenceStatus
import com.fluxa.personal.domain.model.SubscriptionResult
import com.fluxa.personal.domain.model.SubscriptionSource
import com.fluxa.personal.domain.model.SubscriptionStatus
import com.fluxa.personal.domain.model.SubscriptionTerms
import com.fluxa.personal.domain.model.TransactionDirection.EXPENSE
import com.fluxa.personal.domain.model.TransactionDraft
import com.fluxa.personal.domain.rules.SubscriptionCalendar
import com.fluxa.personal.ui.feature.subscriptions.SubscriptionDataSource
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
 * Assinaturas em memória para testes JVM, com a mesma sequência do `SubscriptionRepository` do Codex: calendário,
 * agenda e "próxima" vêm do `SubscriptionCalendar` (domínio); termos com vigência; desfechos com snapshot;
 * pagamento em conta grava no ledger do [FakeTransactionSource]; cobrança no cartão usa o [FakeCreditSource].
 * [today] é mutável para simular a virada do dia.
 */
internal class FakeSubscriptionDataSource(
    private val accounts: AccountRepository,
    private val categories: CategoryRepository,
    private val credit: FakeCreditSource,
    private val ledger: FakeTransactionSource,
    private val now: Instant,
    var today: LocalDate,
) : SubscriptionDataSource {
    private data class Row(val subscription: Subscription, val terms: List<SubscriptionTerms>, val resolutions: List<SubscriptionOccurrence>)

    private val store = MutableStateFlow<Map<UUID, Row>>(emptyMap())
    var failWrites = false
    val payCalls = mutableListOf<Pair<SubscriptionOccurrenceKey, LocalDate>>()

    private fun Row.agenda(from: LocalDate, until: LocalDate) =
        SubscriptionCalendar.agenda(subscription, terms, resolutions, from, until, today)

    private fun Row.onDate(date: LocalDate) = agenda(date, date.plusDays(1)).singleOrNull()

    private suspend fun validate(details: SubscriptionDetails): SubscriptionError? {
        if (details.name.isBlank() || details.amount.cents <= 0) return SubscriptionError.INVALID_DETAILS
        val category = categories.observe(details.categoryId).first()
        if (category == null || category.archivedAt != null || category.details.kind != EXPENSE) return SubscriptionError.INVALID_CATEGORY
        val active = when (val s = details.source) {
            is SubscriptionSource.Account -> accounts.observe(s.id).first()?.archivedAt == null && accounts.observe(s.id).first() != null
            is SubscriptionSource.Card -> credit.observeCard(s.id).first()?.card?.archivedAt == null && credit.observeCard(s.id).first() != null
        }
        return if (active) null else SubscriptionError.INVALID_SOURCE
    }

    private fun replaceTermsFrom(row: Row, day: LocalDate, enabled: Boolean, details: SubscriptionDetails) =
        row.copy(terms = row.terms.filter { it.effectiveOn < day } + SubscriptionTerms(day, details, enabled))

    override suspend fun create(details: SubscriptionDetails, frequency: SubscriptionFrequency, anchorDate: LocalDate, billingDay: BillingDay): SubscriptionResult<Subscription> {
        if (failWrites) throw IOException("disk full")
        validate(details)?.let { return SubscriptionResult.Rejected(it) }
        if (frequency == SubscriptionFrequency.YEARLY && billingDay.day != anchorDate.dayOfMonth) return SubscriptionResult.Rejected(SubscriptionError.INVALID_ANCHOR)
        val normalized = details.copy(name = details.name.trim())
        val sub = Subscription(UUID.randomUUID(), normalized, frequency, anchorDate, billingDay, SubscriptionStatus.ACTIVE, now, now)
        store.update { it + (sub.id to Row(sub, listOf(SubscriptionTerms(anchorDate, normalized, true)), emptyList())) }
        return SubscriptionResult.Saved(sub)
    }

    override suspend fun update(id: UUID, details: SubscriptionDetails): SubscriptionResult<Subscription> {
        if (failWrites) throw IOException("disk full")
        val row = store.value[id] ?: return SubscriptionResult.Rejected(SubscriptionError.NOT_FOUND)
        if (row.subscription.status == SubscriptionStatus.CANCELLED) return SubscriptionResult.Rejected(SubscriptionError.CANCELLED)
        validate(details)?.let { return SubscriptionResult.Rejected(it) }
        val updated = row.subscription.copy(details = details.copy(name = details.name.trim()))
        var next = row.copy(subscription = updated)
        if (updated.status == SubscriptionStatus.ACTIVE) next = replaceTermsFrom(next, today.plusDays(1), true, updated.details)
        store.update { it + (id to next) }
        return SubscriptionResult.Saved(updated)
    }

    override suspend fun pause(id: UUID) = change(id, SubscriptionStatus.PAUSED)
    override suspend fun resume(id: UUID) = change(id, SubscriptionStatus.ACTIVE)
    override suspend fun cancel(id: UUID) = change(id, SubscriptionStatus.CANCELLED)

    private suspend fun change(id: UUID, target: SubscriptionStatus): SubscriptionResult<Subscription> {
        if (failWrites) throw IOException("disk full")
        val row = store.value[id] ?: return SubscriptionResult.Rejected(SubscriptionError.NOT_FOUND)
        if (row.subscription.status == target) return SubscriptionResult.Saved(row.subscription)
        if (row.subscription.status == SubscriptionStatus.CANCELLED) return SubscriptionResult.Rejected(SubscriptionError.CANCELLED)
        if (target == SubscriptionStatus.ACTIVE) validate(row.subscription.details)?.let { return SubscriptionResult.Rejected(it) }
        val updated = row.subscription.copy(status = target)
        val resumes = target == SubscriptionStatus.ACTIVE
        store.update { it + (id to replaceTermsFrom(row.copy(subscription = updated), if (resumes) today.plusDays(1) else today, resumes, updated.details)) }
        return SubscriptionResult.Saved(updated)
    }

    override suspend fun pay(subscriptionId: UUID, occurrenceDate: LocalDate, paidOn: LocalDate) =
        resolve(SubscriptionOccurrenceKey(subscriptionId, occurrenceDate), paidOn)

    override suspend fun skip(subscriptionId: UUID, occurrenceDate: LocalDate) =
        resolve(SubscriptionOccurrenceKey(subscriptionId, occurrenceDate), null)

    private suspend fun resolve(key: SubscriptionOccurrenceKey, paidOn: LocalDate?): SubscriptionResult<SubscriptionOccurrence> {
        if (failWrites) throw IOException("disk full")
        if (paidOn != null) payCalls += key to paidOn
        val row = store.value[key.subscriptionId] ?: return SubscriptionResult.Rejected(SubscriptionError.NOT_FOUND)
        row.resolutions.firstOrNull { it.key == key }?.let {
            return SubscriptionResult.Rejected(if (it.status == SubscriptionOccurrenceStatus.SKIPPED) SubscriptionError.SKIPPED else SubscriptionError.ALREADY_RESOLVED)
        }
        if (paidOn != null) {
            when (row.subscription.status) {
                SubscriptionStatus.PAUSED -> return SubscriptionResult.Rejected(SubscriptionError.PAUSED)
                SubscriptionStatus.CANCELLED -> return SubscriptionResult.Rejected(SubscriptionError.CANCELLED)
                SubscriptionStatus.ACTIVE -> Unit
            }
            if (paidOn > today || key.occurrenceDate > today) return SubscriptionResult.Rejected(SubscriptionError.FUTURE_DATE)
        }
        val pending = row.onDate(key.occurrenceDate) ?: return SubscriptionResult.Rejected(SubscriptionError.INVALID_OCCURRENCE)
        val outcome: SubscriptionResult<SubscriptionOccurrence> =
            if (paidOn == null) SubscriptionResult.Saved(pending.copy(status = SubscriptionOccurrenceStatus.SKIPPED)) else settle(pending, paidOn)
        if (outcome is SubscriptionResult.Saved) {
            store.update { s -> s + (key.subscriptionId to s.getValue(key.subscriptionId).let { it.copy(resolutions = it.resolutions + outcome.value) }) }
        }
        return outcome
    }

    private suspend fun settle(occurrence: SubscriptionOccurrence, paidOn: LocalDate): SubscriptionResult<SubscriptionOccurrence> {
        val d = occurrence.details
        validate(d)?.let { return SubscriptionResult.Rejected(it) }
        val id = UUID.randomUUID()
        return when (val src = d.source) {
            is SubscriptionSource.Account -> {
                val account = accounts.observe(src.id).first()!!
                if (paidOn < account.details.openingDate) return SubscriptionResult.Rejected(SubscriptionError.BEFORE_ACCOUNT_OPENING)
                val tx = FinancialTransaction(
                    id, TransactionDraft(EXPENSE, src.id, d.categoryId, d.amount, paidOn, "Assinatura • ${d.name}"), now, now,
                    subscriptionOccurrence = occurrence.key,
                )
                ledger.rows.update { it + (id to tx) }
                SubscriptionResult.Saved(occurrence.copy(status = SubscriptionOccurrenceStatus.PAID, resultId = id))
            }
            is SubscriptionSource.Card -> when (val r = credit.createPurchase(id, PurchaseDraft(src.id, d.categoryId, d.name, d.amount, 1, paidOn))) {
                is CreditResult.Saved -> SubscriptionResult.Saved(occurrence.copy(status = SubscriptionOccurrenceStatus.CHARGED, resultId = id))
                is CreditResult.Rejected -> SubscriptionResult.Rejected(
                    when (r.error) {
                        CreditError.INSUFFICIENT_LIMIT -> SubscriptionError.INSUFFICIENT_CARD_LIMIT
                        CreditError.INVALID_SCHEDULE_DATE -> SubscriptionError.INVALID_CARD_SCHEDULE
                        else -> SubscriptionError.INVALID_SOURCE
                    },
                )
            }
        }
    }

    override fun observeSubscriptions(): Flow<List<Subscription>> = store.map { s -> s.values.map { it.subscription }.sortedBy { it.details.name } }
    override fun observeSubscription(id: UUID): Flow<Subscription?> = store.map { it[id]?.subscription }
    override fun observeOccurrence(id: UUID, date: LocalDate): Flow<SubscriptionOccurrence?> = store.map { it[id]?.onDate(date) }
    override fun observeNextOccurrence(id: UUID): Flow<SubscriptionOccurrence?> =
        store.map { s -> s[id]?.let { SubscriptionCalendar.next(it.subscription, it.terms, it.resolutions, today) } }

    private fun List<SubscriptionOccurrence>.ordered() = sortedWith(compareBy({ it.key.occurrenceDate }, { it.key.subscriptionId }))

    override fun observeOccurrences(fromInclusive: LocalDate, untilExclusive: LocalDate): Flow<List<SubscriptionOccurrence>> =
        store.map { s -> s.values.flatMap { it.agenda(fromInclusive, untilExclusive) }.ordered() }

    override fun observeUpcomingOccurrences(days: Long): Flow<List<SubscriptionOccurrence>> =
        store.map { s -> s.values.flatMap { it.agenda(today, today.plusDays(days)) }.filter { it.status == SubscriptionOccurrenceStatus.PENDING }.ordered() }

    override fun observeOverdueOccurrences(): Flow<List<SubscriptionOccurrence>> = store.map { s ->
        s.values.flatMap { if (it.subscription.anchorDate < today) it.agenda(it.subscription.anchorDate, today) else emptyList() }
            .filter { it.status == SubscriptionOccurrenceStatus.OVERDUE }.ordered()
    }
}
