package com.fluxa.personal.domain.model

import com.fluxa.personal.core.date.BillingDay
import com.fluxa.personal.core.money.Money
import com.fluxa.personal.domain.rules.CreditLimit
import java.time.Instant
import java.util.UUID

internal data class CreditCardDetails(
    val name: String,
    val totalLimit: Money,
    val closingDay: BillingDay,
    val dueDay: BillingDay,
    val color: String? = null,
)

internal data class CreditCard(
    val id: UUID,
    val details: CreditCardDetails,
    val archivedAt: Instant?,
    val createdAt: Instant,
    val updatedAt: Instant,
)

internal data class CreditCardSummary(val card: CreditCard, val committedAmount: Money) {
    val totalLimit: Money get() = card.details.totalLimit
    val availableLimit: Money get() = CreditLimit(totalLimit, committedAmount).available
}
