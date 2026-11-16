package com.fluxa.personal.domain.rules

import com.fluxa.personal.domain.model.Category
import com.fluxa.personal.domain.model.CreditCardSummary
import com.fluxa.personal.domain.model.CreditError
import com.fluxa.personal.domain.model.CreditResult
import com.fluxa.personal.domain.model.PurchaseDraft
import com.fluxa.personal.domain.model.ScheduledInstallment
import com.fluxa.personal.domain.model.TransactionDirection
import java.time.DateTimeException
import java.time.YearMonth

internal object CreditPurchaseRules {
    fun schedule(
        draft: PurchaseDraft,
        summary: CreditCardSummary?,
        category: Category?,
    ): CreditResult<List<ScheduledInstallment>> {
        val card = summary?.card
        val error = when {
            draft.title.isBlank() -> CreditError.INVALID_TITLE
            draft.total.cents <= 0 -> CreditError.INVALID_AMOUNT
            draft.installmentCount <= 0 || draft.installmentCount.toLong() > draft.total.cents ->
                CreditError.INVALID_INSTALLMENT_COUNT
            summary == null || card == null || card.id != draft.cardId -> CreditError.CARD_NOT_FOUND
            card.archivedAt != null -> CreditError.CARD_ARCHIVED
            category == null || category.id != draft.categoryId -> CreditError.CATEGORY_NOT_FOUND
            category.archivedAt != null -> CreditError.CATEGORY_ARCHIVED
            category.details.kind != TransactionDirection.EXPENSE -> CreditError.CATEGORY_NOT_EXPENSE
            !CreditLimit(summary.totalLimit, summary.committedAmount).canCommit(draft.total) ->
                CreditError.INSUFFICIENT_LIMIT
            else -> null
        }
        if (error != null) return CreditResult.Rejected(error)

        val details = checkNotNull(card).details
        return try {
            val firstDue = CreditBillingCalendar.firstDueDate(draft.purchaseDate, details.closingDay, details.dueDay)
            val firstMonth = YearMonth.from(firstDue)
            // Check the end of the calendar range before materializing the plan.
            firstMonth.plusMonths(draft.installmentCount.toLong() - 1)
            CreditResult.Saved(
                InstallmentSplitter.split(draft.total, draft.installmentCount, firstMonth, details.dueDay),
            )
        } catch (_: DateTimeException) {
            CreditResult.Rejected(CreditError.INVALID_SCHEDULE_DATE)
        }
    }
}
