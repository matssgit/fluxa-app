package com.fluxa.personal.domain.model

import com.fluxa.personal.core.date.BillingDay
import com.fluxa.personal.core.money.Money
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

internal sealed interface SubscriptionSource {
    data class Account(val id: UUID) : SubscriptionSource
    data class Card(val id: UUID) : SubscriptionSource
}

internal enum class SubscriptionFrequency { MONTHLY, YEARLY }
internal enum class SubscriptionStatus { ACTIVE, PAUSED, CANCELLED }

internal data class SubscriptionDetails(
    val name: String,
    val amount: Money,
    val categoryId: UUID,
    val source: SubscriptionSource,
)

internal data class Subscription(
    val id: UUID,
    val details: SubscriptionDetails,
    val frequency: SubscriptionFrequency,
    val anchorDate: LocalDate,
    val billingDay: BillingDay,
    val status: SubscriptionStatus,
    val createdAt: Instant,
    val updatedAt: Instant,
    val pausedAt: Instant? = null,
    val cancelledAt: Instant? = null,
)

internal data class SubscriptionOccurrenceKey(val subscriptionId: UUID, val occurrenceDate: LocalDate)

internal enum class SubscriptionOccurrenceStatus { PENDING, OVERDUE, PAID, CHARGED, SKIPPED }

internal data class SubscriptionOccurrence(
    val key: SubscriptionOccurrenceKey,
    val details: SubscriptionDetails,
    val status: SubscriptionOccurrenceStatus,
    val resultId: UUID? = null,
)

internal data class SubscriptionTerms(
    val effectiveOn: LocalDate,
    val details: SubscriptionDetails,
    val enabled: Boolean,
)

internal enum class SubscriptionError {
    NOT_FOUND, PAUSED, CANCELLED, INVALID_DETAILS, INVALID_ANCHOR,
    INVALID_CATEGORY, INVALID_SOURCE, INVALID_OCCURRENCE, FUTURE_DATE,
    BEFORE_ACCOUNT_OPENING, ALREADY_RESOLVED, SKIPPED, INSUFFICIENT_CARD_LIMIT, INVALID_CARD_SCHEDULE,
}

internal sealed interface SubscriptionResult<out T> {
    data class Saved<T>(val value: T) : SubscriptionResult<T>
    data class Rejected(val error: SubscriptionError) : SubscriptionResult<Nothing>
}
