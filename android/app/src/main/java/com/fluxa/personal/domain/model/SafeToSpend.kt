package com.fluxa.personal.domain.model

import com.fluxa.personal.core.money.Money
import java.time.LocalDate
import java.util.UUID

internal enum class ProjectedCashEventType { OBLIGATION, CONFIRMED_INCOME }

internal sealed interface ProjectedCashSource {
    data class Installment(val installmentId: UUID, val purchaseId: UUID, val cardId: UUID) : ProjectedCashSource
    data class Subscription(val key: SubscriptionOccurrenceKey, val paymentSource: SubscriptionSource) : ProjectedCashSource
    /** An explicit income commitment supplied by a future domain contract. */
    data class ConfirmedIncome(val id: UUID) : ProjectedCashSource
}

internal data class ProjectedCashEvent(
    val date: LocalDate,
    val type: ProjectedCashEventType,
    val amount: Money,
    val source: ProjectedCashSource,
) {
    init {
        require(amount.cents > 0) { "A projected cash event must have a positive amount." }
    }
}

internal data class SafeToSpendResult(
    val safeAmount: Money,
    val availableNow: Money,
    val safetyMargin: Money,
    val lowestProjectedBalance: Money,
    val lowestSlackDate: LocalDate,
    val horizonStart: LocalDate,
    val horizonEndExclusive: LocalDate,
    val obligationsTotal: Money,
    val confirmedIncomeTotal: Money,
    /** Effective dates, ordered through the precise minimum, before any later same-day income. */
    val breakdown: List<ProjectedCashEvent>,
)

internal enum class SafeToSpendError { OVERFLOW, CONFLICTING_EVENTS }

internal sealed interface SafeToSpendState {
    data class Ready(val result: SafeToSpendResult) : SafeToSpendState
    data class Unavailable(val error: SafeToSpendError) : SafeToSpendState
}
