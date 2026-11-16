package com.fluxa.personal.data.repository

import androidx.room.withTransaction
import com.fluxa.personal.core.date.BillingDay
import com.fluxa.personal.core.date.FinancialCalendar
import com.fluxa.personal.data.database.FluxaDatabase
import com.fluxa.personal.data.database.dao.SubscriptionAggregate
import com.fluxa.personal.data.database.entity.SubscriptionTermsEntity
import com.fluxa.personal.data.database.mapper.toColumns
import com.fluxa.personal.data.database.mapper.toDomain
import com.fluxa.personal.data.database.mapper.toEntity
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
import com.fluxa.personal.domain.model.SubscriptionResult.Rejected
import com.fluxa.personal.domain.model.SubscriptionResult.Saved
import com.fluxa.personal.domain.model.SubscriptionSource
import com.fluxa.personal.domain.model.SubscriptionStatus
import com.fluxa.personal.domain.model.TransactionDirection.EXPENSE
import com.fluxa.personal.domain.model.TransactionDraft
import com.fluxa.personal.domain.rules.SubscriptionCalendar
import java.time.Clock
import java.time.LocalDate
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

internal class SubscriptionRepository(
    private val database: FluxaDatabase,
    private val credit: CreditRepository,
    private val clock: Clock = Clock.systemUTC(),
    private val calendar: FinancialCalendar = FinancialCalendar(clock),
) {
    private val dao = database.subscriptionDao()
    private val transactions = database.transactionDao()

    suspend fun create(
        details: SubscriptionDetails,
        frequency: SubscriptionFrequency,
        anchorDate: LocalDate,
        billingDay: BillingDay,
    ): SubscriptionResult<Subscription> = database.withTransaction {
        validate(details)?.let { return@withTransaction Rejected(it) }
        if (frequency == SubscriptionFrequency.YEARLY && billingDay.day != anchorDate.dayOfMonth) {
            return@withTransaction Rejected(SubscriptionError.INVALID_ANCHOR)
        }
        val now = clock.instant()
        val subscription = Subscription(
            UUID.randomUUID(), details.normalized(), frequency, anchorDate, billingDay, SubscriptionStatus.ACTIVE, now, now,
        )
        dao.insert(subscription.toEntity())
        replaceTermsFrom(subscription, anchorDate, enabled = true)
        Saved(subscription)
    }

    suspend fun update(id: UUID, details: SubscriptionDetails): SubscriptionResult<Subscription> =
        database.withTransaction {
            val existing = dao.find(id.toString())?.subscription?.toDomain()
                ?: return@withTransaction Rejected(SubscriptionError.NOT_FOUND)
            if (existing.status == SubscriptionStatus.CANCELLED) return@withTransaction Rejected(SubscriptionError.CANCELLED)
            validate(details)?.let { return@withTransaction Rejected(it) }
            val updated = existing.copy(details = details.normalized(), updatedAt = clock.instant())
            dao.update(updated.toEntity())
            if (updated.status == SubscriptionStatus.ACTIVE) replaceTermsFrom(updated, calendar.today().plusDays(1), true)
            Saved(updated)
        }

    suspend fun pause(id: UUID) = changeStatus(id, SubscriptionStatus.PAUSED)
    suspend fun resume(id: UUID) = changeStatus(id, SubscriptionStatus.ACTIVE)
    suspend fun cancel(id: UUID) = changeStatus(id, SubscriptionStatus.CANCELLED)

    private suspend fun changeStatus(id: UUID, target: SubscriptionStatus): SubscriptionResult<Subscription> =
        database.withTransaction {
            val existing = dao.find(id.toString())?.subscription?.toDomain()
                ?: return@withTransaction Rejected(SubscriptionError.NOT_FOUND)
            if (existing.status == target) return@withTransaction Saved(existing)
            if (existing.status == SubscriptionStatus.CANCELLED) return@withTransaction Rejected(SubscriptionError.CANCELLED)
            if (target == SubscriptionStatus.ACTIVE) validate(existing.details)?.let { return@withTransaction Rejected(it) }
            val now = clock.instant()
            val updated = existing.copy(
                status = target, updatedAt = now,
                pausedAt = if (target == SubscriptionStatus.PAUSED) now else existing.pausedAt.takeIf { target == SubscriptionStatus.CANCELLED },
                cancelledAt = now.takeIf { target == SubscriptionStatus.CANCELLED },
            )
            dao.update(updated.toEntity())
            val today = calendar.today()
            val resumes = target == SubscriptionStatus.ACTIVE
            replaceTermsFrom(updated, if (resumes) today.plusDays(1) else today, resumes)
            Saved(updated)
        }

    suspend fun paySubscriptionOccurrence(subscriptionId: UUID, occurrenceDate: LocalDate, paidOn: LocalDate) =
        resolve(SubscriptionOccurrenceKey(subscriptionId, occurrenceDate), paidOn)

    suspend fun skipSubscriptionOccurrence(subscriptionId: UUID, occurrenceDate: LocalDate) =
        resolve(SubscriptionOccurrenceKey(subscriptionId, occurrenceDate), paidOn = null)

    private suspend fun resolve(key: SubscriptionOccurrenceKey, paidOn: LocalDate?): SubscriptionResult<SubscriptionOccurrence> =
        database.withTransaction {
            val data = dao.find(key.subscriptionId.toString()) ?: return@withTransaction Rejected(SubscriptionError.NOT_FOUND)
            val resolved = data.resolutions.firstOrNull { it.occurrenceDate == key.occurrenceDate.toEpochDay() }
            if (resolved != null) {
                return@withTransaction Rejected(
                    if (resolved.status == SubscriptionOccurrenceStatus.SKIPPED.name) SubscriptionError.SKIPPED
                    else SubscriptionError.ALREADY_RESOLVED,
                )
            }
            val today = calendar.today()
            val date = key.occurrenceDate
            if (paidOn != null) {
                when (data.subscription.toDomain().status) {
                    SubscriptionStatus.PAUSED -> return@withTransaction Rejected(SubscriptionError.PAUSED)
                    SubscriptionStatus.CANCELLED -> return@withTransaction Rejected(SubscriptionError.CANCELLED)
                    SubscriptionStatus.ACTIVE -> Unit
                }
                if (paidOn > today || date > today) return@withTransaction Rejected(SubscriptionError.FUTURE_DATE)
            }
            val pending = data.onDate(date, today) ?: return@withTransaction Rejected(SubscriptionError.INVALID_OCCURRENCE)
            val outcome = if (paidOn == null) {
                Saved(pending.copy(status = SubscriptionOccurrenceStatus.SKIPPED))
            } else {
                settle(pending, paidOn)
            }
            if (outcome is Saved) dao.resolve(outcome.value.toEntity())
            outcome
        }

    private suspend fun settle(occurrence: SubscriptionOccurrence, paidOn: LocalDate): SubscriptionResult<SubscriptionOccurrence> {
        val details = occurrence.details
        validate(details)?.let { return Rejected(it) }
        val id = UUID.randomUUID()
        return when (val source = details.source) {
            is SubscriptionSource.Account -> {
                val account = checkNotNull(transactions.findAccount(source.id.toString())).toDomain()
                if (paidOn < account.details.openingDate) return Rejected(SubscriptionError.BEFORE_ACCOUNT_OPENING)
                val now = clock.instant()
                val transaction = FinancialTransaction(
                    id, TransactionDraft(EXPENSE, source.id, details.categoryId, details.amount, paidOn, "Assinatura • ${details.name}"),
                    now, now, subscriptionOccurrence = occurrence.key,
                )
                transactions.insert(transaction.toEntity())
                Saved(occurrence.copy(status = SubscriptionOccurrenceStatus.PAID, resultId = id))
            }
            is SubscriptionSource.Card -> {
                val draft = PurchaseDraft(source.id, details.categoryId, details.name, details.amount, 1, paidOn)
                when (val result = credit.createSubscriptionPurchase(id, draft, occurrence.key)) {
                    is CreditResult.Saved -> Saved(occurrence.copy(status = SubscriptionOccurrenceStatus.CHARGED, resultId = id))
                    is CreditResult.Rejected -> Rejected(
                        when (result.error) {
                            CreditError.INSUFFICIENT_LIMIT -> SubscriptionError.INSUFFICIENT_CARD_LIMIT
                            CreditError.INVALID_SCHEDULE_DATE -> SubscriptionError.INVALID_CARD_SCHEDULE
                            else -> SubscriptionError.INVALID_SOURCE
                        },
                    )
                }
            }
        }
    }

    fun observeSubscriptions(): Flow<List<Subscription>> =
        dao.observeAll().map { rows -> rows.map { it.subscription.toDomain() } }.distinctUntilChanged()

    fun observeSubscription(id: UUID): Flow<Subscription?> =
        dao.observe(id.toString()).map { it?.subscription?.toDomain() }.distinctUntilChanged()

    fun observeOccurrence(id: UUID, date: LocalDate): Flow<SubscriptionOccurrence?> =
        dao.observe(id.toString()).map { it?.onDate(date, calendar.today()) }.distinctUntilChanged()

    fun observeNextOccurrence(id: UUID): Flow<SubscriptionOccurrence?> =
        dao.observe(id.toString()).map { row ->
            row?.let { SubscriptionCalendar.next(it.subscription.toDomain(), it.terms.map { term -> term.toDomain() },
                it.resolutions.map { resolution -> resolution.toDomain() }, calendar.today()) }
        }.distinctUntilChanged()

    fun observeOccurrences(fromInclusive: LocalDate, untilExclusive: LocalDate): Flow<List<SubscriptionOccurrence>> {
        require(fromInclusive <= untilExclusive)
        return dao.observeAll().map { rows ->
            val today = calendar.today()
            rows.flatMap { it.agenda(fromInclusive, untilExclusive, today) }.ordered()
        }.distinctUntilChanged()
    }

    fun observeUpcomingOccurrences(days: Long): Flow<List<SubscriptionOccurrence>> {
        require(days > 0)
        return dao.observeAll().map { rows ->
            val today = calendar.today()
            rows.flatMap { it.agenda(today, today.plusDays(days), today) }
                .filter { it.status == SubscriptionOccurrenceStatus.PENDING }.ordered()
        }.distinctUntilChanged()
    }

    fun observeOverdueOccurrences(): Flow<List<SubscriptionOccurrence>> =
        dao.observeAll().map { rows ->
            val today = calendar.today()
            rows.flatMap {
                val anchor = LocalDate.ofEpochDay(it.subscription.anchorDate)
                if (anchor < today) it.agenda(anchor, today, today) else emptyList()
            }.filter { it.status == SubscriptionOccurrenceStatus.OVERDUE }.ordered()
        }.distinctUntilChanged()

    private suspend fun replaceTermsFrom(subscription: Subscription, day: LocalDate, enabled: Boolean) {
        val id = subscription.id.toString()
        dao.deleteTermsFrom(id, day.toEpochDay())
        dao.insertTerms(SubscriptionTermsEntity(id, day.toEpochDay(), subscription.details.toColumns(), enabled))
    }

    private suspend fun validate(details: SubscriptionDetails): SubscriptionError? {
        if (details.name.isBlank() || details.amount.cents <= 0) return SubscriptionError.INVALID_DETAILS
        val category = transactions.findCategory(details.categoryId.toString())
        if (category == null || category.archivedAt != null || category.kind != EXPENSE.name) return SubscriptionError.INVALID_CATEGORY
        val activeSource = when (val source = details.source) {
            is SubscriptionSource.Account -> transactions.findAccount(source.id.toString())?.let { it.archivedAt == null } == true
            is SubscriptionSource.Card -> database.creditDao().findCardSummary(source.id.toString())?.let { it.card.archivedAt == null } == true
        }
        return if (activeSource) null else SubscriptionError.INVALID_SOURCE
    }

    private fun SubscriptionAggregate.agenda(start: LocalDate, end: LocalDate, today: LocalDate) =
        SubscriptionCalendar.agenda(subscription.toDomain(), terms.map { it.toDomain() }, resolutions.map { it.toDomain() }, start, end, today)

    private fun SubscriptionAggregate.onDate(date: LocalDate, today: LocalDate) =
        if (date == LocalDate.MAX) null else agenda(date, date.plusDays(1), today).singleOrNull()

    private fun List<SubscriptionOccurrence>.ordered() =
        sortedWith(compareBy({ it.key.occurrenceDate }, { it.key.subscriptionId }))

    private fun SubscriptionDetails.normalized() = copy(name = name.trim())
}
