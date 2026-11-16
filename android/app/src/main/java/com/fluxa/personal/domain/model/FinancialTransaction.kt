package com.fluxa.personal.domain.model

import com.fluxa.personal.core.money.Money
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

internal data class TransactionDraft(
    val direction: TransactionDirection,
    val accountId: UUID,
    val categoryId: UUID,
    val amount: Money,
    val occurredOn: LocalDate,
    val description: String? = null,
)

internal data class FinancialTransaction(
    val id: UUID,
    val details: TransactionDraft,
    val createdAt: Instant,
    val updatedAt: Instant,
    val cancelledAt: Instant? = null,
) {
    val status: TransactionStatus
        get() = if (cancelledAt == null) TransactionStatus.COMPLETED else TransactionStatus.CANCELLED
}

internal enum class TransactionError {
    DUPLICATE_ID,
    NOT_FOUND,
    CANCELLED,
    INVALID_AMOUNT,
    FUTURE_DATE,
    ACCOUNT_NOT_FOUND,
    ACCOUNT_ARCHIVED,
    CATEGORY_NOT_FOUND,
    CATEGORY_ARCHIVED,
    CATEGORY_DIRECTION_MISMATCH,
}

internal sealed interface TransactionResult {
    data class Saved(val transaction: FinancialTransaction) : TransactionResult
    data class Rejected(val error: TransactionError) : TransactionResult
}

internal data class TransactionHistoryFilter(
    val accountId: UUID? = null,
    val categoryId: UUID? = null,
    val fromInclusive: LocalDate? = null,
    val untilExclusive: LocalDate? = null,
    val includeCancelled: Boolean = true,
    val direction: TransactionDirection? = null,
) {
    init {
        require(fromInclusive == null || untilExclusive == null || fromInclusive <= untilExclusive) {
            "The end of the period must not precede the start."
        }
    }
}

internal data class TransactionHistoryPage(val limit: Int, val offset: Int = 0) {
    init {
        require(limit > 0) { "History page limit must be positive." }
        require(offset >= 0) { "History page offset cannot be negative." }
    }
}
