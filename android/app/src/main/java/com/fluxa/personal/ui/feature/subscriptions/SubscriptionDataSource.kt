package com.fluxa.personal.ui.feature.subscriptions

import com.fluxa.personal.core.date.BillingDay
import com.fluxa.personal.data.repository.SubscriptionRepository
import com.fluxa.personal.domain.model.Subscription
import com.fluxa.personal.domain.model.SubscriptionDetails
import com.fluxa.personal.domain.model.SubscriptionFrequency
import com.fluxa.personal.domain.model.SubscriptionOccurrence
import com.fluxa.personal.domain.model.SubscriptionResult
import java.time.LocalDate
import java.util.UUID
import kotlinx.coroutines.flow.Flow

/**
 * Recorte do [SubscriptionRepository] usado pela UI, só para os ViewModels serem testáveis na JVM (o repositório usa
 * `RoomDatabase.withTransaction`). Não acrescenta regra: calendário, termos, estados e efeitos financeiros são do Codex.
 */
internal interface SubscriptionDataSource {
    suspend fun create(details: SubscriptionDetails, frequency: SubscriptionFrequency, anchorDate: LocalDate, billingDay: BillingDay): SubscriptionResult<Subscription>
    suspend fun update(id: UUID, details: SubscriptionDetails): SubscriptionResult<Subscription>
    suspend fun pause(id: UUID): SubscriptionResult<Subscription>
    suspend fun resume(id: UUID): SubscriptionResult<Subscription>
    suspend fun cancel(id: UUID): SubscriptionResult<Subscription>
    suspend fun pay(subscriptionId: UUID, occurrenceDate: LocalDate, paidOn: LocalDate): SubscriptionResult<SubscriptionOccurrence>
    suspend fun skip(subscriptionId: UUID, occurrenceDate: LocalDate): SubscriptionResult<SubscriptionOccurrence>
    fun observeSubscriptions(): Flow<List<Subscription>>
    fun observeSubscription(id: UUID): Flow<Subscription?>
    fun observeOccurrence(id: UUID, date: LocalDate): Flow<SubscriptionOccurrence?>
    fun observeNextOccurrence(id: UUID): Flow<SubscriptionOccurrence?>
    fun observeOccurrences(fromInclusive: LocalDate, untilExclusive: LocalDate): Flow<List<SubscriptionOccurrence>>
    fun observeUpcomingOccurrences(days: Long): Flow<List<SubscriptionOccurrence>>
    fun observeOverdueOccurrences(): Flow<List<SubscriptionOccurrence>>
}

internal class RepositorySubscriptionDataSource(private val repository: SubscriptionRepository) : SubscriptionDataSource {
    override suspend fun create(details: SubscriptionDetails, frequency: SubscriptionFrequency, anchorDate: LocalDate, billingDay: BillingDay) =
        repository.create(details, frequency, anchorDate, billingDay)
    override suspend fun update(id: UUID, details: SubscriptionDetails) = repository.update(id, details)
    override suspend fun pause(id: UUID) = repository.pause(id)
    override suspend fun resume(id: UUID) = repository.resume(id)
    override suspend fun cancel(id: UUID) = repository.cancel(id)
    override suspend fun pay(subscriptionId: UUID, occurrenceDate: LocalDate, paidOn: LocalDate) =
        repository.paySubscriptionOccurrence(subscriptionId, occurrenceDate, paidOn)
    override suspend fun skip(subscriptionId: UUID, occurrenceDate: LocalDate) =
        repository.skipSubscriptionOccurrence(subscriptionId, occurrenceDate)
    override fun observeSubscriptions() = repository.observeSubscriptions()
    override fun observeSubscription(id: UUID) = repository.observeSubscription(id)
    override fun observeOccurrence(id: UUID, date: LocalDate) = repository.observeOccurrence(id, date)
    override fun observeNextOccurrence(id: UUID) = repository.observeNextOccurrence(id)
    override fun observeOccurrences(fromInclusive: LocalDate, untilExclusive: LocalDate) =
        repository.observeOccurrences(fromInclusive, untilExclusive)
    override fun observeUpcomingOccurrences(days: Long) = repository.observeUpcomingOccurrences(days)
    override fun observeOverdueOccurrences() = repository.observeOverdueOccurrences()
}
