package com.fluxa.personal.data.database.mapper

import com.fluxa.personal.core.date.BillingDay
import com.fluxa.personal.core.money.Money
import com.fluxa.personal.data.database.entity.SubscriptionColumns
import com.fluxa.personal.data.database.entity.SubscriptionEntity
import com.fluxa.personal.data.database.entity.SubscriptionResolutionEntity
import com.fluxa.personal.data.database.entity.SubscriptionTermsEntity
import com.fluxa.personal.domain.model.Subscription
import com.fluxa.personal.domain.model.SubscriptionDetails
import com.fluxa.personal.domain.model.SubscriptionFrequency
import com.fluxa.personal.domain.model.SubscriptionOccurrence
import com.fluxa.personal.domain.model.SubscriptionOccurrenceKey
import com.fluxa.personal.domain.model.SubscriptionOccurrenceStatus
import com.fluxa.personal.domain.model.SubscriptionSource
import com.fluxa.personal.domain.model.SubscriptionStatus
import com.fluxa.personal.domain.model.SubscriptionTerms
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

internal fun subscriptionKey(id: String?, on: Long?): SubscriptionOccurrenceKey? =
    id?.let { SubscriptionOccurrenceKey(UUID.fromString(it), LocalDate.ofEpochDay(requireNotNull(on))) }

internal fun SubscriptionDetails.toColumns() = SubscriptionColumns(
    name, amount.cents, categoryId.toString(),
    (source as? SubscriptionSource.Account)?.id?.toString(),
    (source as? SubscriptionSource.Card)?.id?.toString(),
)

private fun SubscriptionColumns.toDomain(): SubscriptionDetails {
    check((accountId == null) != (cardId == null)) { "Invalid subscription payment source." }
    return SubscriptionDetails(
        name, Money(amountCents), UUID.fromString(categoryId),
        if (accountId != null) SubscriptionSource.Account(UUID.fromString(accountId))
        else SubscriptionSource.Card(UUID.fromString(requireNotNull(cardId))),
    )
}

internal fun Subscription.toEntity() = SubscriptionEntity(
    id.toString(), details.toColumns(), frequency.name, anchorDate.toEpochDay(), billingDay.day,
    status.name, createdAt.toString(), updatedAt.toString(), pausedAt?.toString(), cancelledAt?.toString(),
)

internal fun SubscriptionEntity.toDomain() = Subscription(
    UUID.fromString(id), details.toDomain(), SubscriptionFrequency.valueOf(frequency),
    LocalDate.ofEpochDay(anchorDate), BillingDay(billingDay), SubscriptionStatus.valueOf(status),
    Instant.parse(createdAt), Instant.parse(updatedAt), pausedAt?.let(Instant::parse), cancelledAt?.let(Instant::parse),
)

internal fun SubscriptionTermsEntity.toDomain() =
    SubscriptionTerms(LocalDate.ofEpochDay(effectiveOn), details.toDomain(), enabled)

internal fun SubscriptionResolutionEntity.toDomain() = SubscriptionOccurrence(
    SubscriptionOccurrenceKey(UUID.fromString(subscriptionId), LocalDate.ofEpochDay(occurrenceDate)),
    details.toDomain(), SubscriptionOccurrenceStatus.valueOf(status), resultId?.let(UUID::fromString),
)

internal fun SubscriptionOccurrence.toEntity() = SubscriptionResolutionEntity(
    key.subscriptionId.toString(), key.occurrenceDate.toEpochDay(), details.toColumns(), status.name, resultId?.toString(),
)
