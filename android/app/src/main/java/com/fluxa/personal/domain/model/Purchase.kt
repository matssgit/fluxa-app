package com.fluxa.personal.domain.model

import com.fluxa.personal.core.money.Money
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

internal data class PurchaseDraft(
    val cardId: UUID,
    val categoryId: UUID,
    val title: String,
    val total: Money,
    val installmentCount: Int,
    val purchaseDate: LocalDate,
    val merchant: String? = null,
    val notes: String? = null,
)

internal enum class PurchaseStatus { ACTIVE, CANCELLED }
internal enum class InstallmentStatus { PENDING, PAID, CANCELLED }

internal data class Purchase(
    val id: UUID,
    val details: PurchaseDraft,
    val firstDueDate: LocalDate,
    val createdAt: Instant,
    val updatedAt: Instant,
    val cancelledAt: Instant? = null,
    val subscriptionOccurrence: SubscriptionOccurrenceKey? = null,
) {
    val status: PurchaseStatus
        get() = if (cancelledAt == null) PurchaseStatus.ACTIVE else PurchaseStatus.CANCELLED
}

internal data class Installment(
    val id: UUID,
    val purchaseId: UUID,
    val number: Int,
    val amount: Money,
    val dueDate: LocalDate,
    val status: InstallmentStatus,
)

internal enum class CreditError {
    INVALID_NAME,
    INVALID_LIMIT,
    LIMIT_BELOW_COMMITTED,
    CARD_NOT_FOUND,
    CARD_ARCHIVED,
    CATEGORY_NOT_FOUND,
    CATEGORY_ARCHIVED,
    CATEGORY_NOT_EXPENSE,
    DUPLICATE_PURCHASE_ID,
    INVALID_TITLE,
    INVALID_AMOUNT,
    INVALID_INSTALLMENT_COUNT,
    INVALID_SCHEDULE_DATE,
    INSUFFICIENT_LIMIT,
    PURCHASE_NOT_FOUND,
    PURCHASE_CANCELLED,
    HAS_PAID_INSTALLMENT,
    MANAGED_BY_SUBSCRIPTION,
}

internal sealed interface CreditResult<out T> {
    data class Saved<T>(val value: T) : CreditResult<T>
    data class Rejected(val error: CreditError) : CreditResult<Nothing>
}
