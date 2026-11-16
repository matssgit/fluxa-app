package com.fluxa.personal.domain.rules

import com.fluxa.personal.domain.model.Category
import com.fluxa.personal.domain.model.FinancialAccount
import com.fluxa.personal.domain.model.FinancialTransaction
import com.fluxa.personal.domain.model.TransactionDraft
import com.fluxa.personal.domain.model.TransactionError
import com.fluxa.personal.domain.model.TransactionResult
import com.fluxa.personal.domain.model.TransactionResult.Rejected
import com.fluxa.personal.domain.model.TransactionResult.Saved
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

internal class TransactionRules(private val today: LocalDate, private val now: Instant) {
    fun create(
        id: UUID,
        existing: FinancialTransaction?,
        draft: TransactionDraft,
        account: FinancialAccount?,
        category: Category?,
    ): TransactionResult {
        if (existing != null) return Rejected(TransactionError.DUPLICATE_ID)
        validate(draft, account, category)?.let { return Rejected(it) }
        return Saved(FinancialTransaction(id, draft.normalized(), now, now))
    }

    fun edit(
        existing: FinancialTransaction?,
        draft: TransactionDraft,
        account: FinancialAccount?,
        category: Category?,
    ): TransactionResult {
        if (existing == null) return Rejected(TransactionError.NOT_FOUND)
        if (existing.installmentId != null) return Rejected(TransactionError.MANAGED_BY_INSTALLMENT)
        if (existing.cancelledAt != null) return Rejected(TransactionError.CANCELLED)
        validate(draft, account, category)?.let { return Rejected(it) }
        return Saved(existing.copy(details = draft.normalized(), updatedAt = now))
    }

    fun cancel(existing: FinancialTransaction?): TransactionResult {
        if (existing == null) return Rejected(TransactionError.NOT_FOUND)
        if (existing.installmentId != null) return Rejected(TransactionError.MANAGED_BY_INSTALLMENT)
        if (existing.cancelledAt != null) return Rejected(TransactionError.CANCELLED)
        return Saved(existing.copy(updatedAt = now, cancelledAt = now))
    }

    private fun validate(
        draft: TransactionDraft,
        account: FinancialAccount?,
        category: Category?,
    ): TransactionError? = when {
        draft.amount.cents <= 0 -> TransactionError.INVALID_AMOUNT
        draft.occurredOn > today -> TransactionError.FUTURE_DATE
        account == null || account.id != draft.accountId -> TransactionError.ACCOUNT_NOT_FOUND
        account.archivedAt != null -> TransactionError.ACCOUNT_ARCHIVED
        category == null || category.id != draft.categoryId -> TransactionError.CATEGORY_NOT_FOUND
        category.archivedAt != null -> TransactionError.CATEGORY_ARCHIVED
        category.details.kind != draft.direction -> TransactionError.CATEGORY_DIRECTION_MISMATCH
        else -> null
    }

    private fun TransactionDraft.normalized() = copy(description = description?.trim()?.ifEmpty { null })
}
