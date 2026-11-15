package com.fluxa.personal.domain.rules

import com.fluxa.personal.core.money.Money
import com.fluxa.personal.domain.model.AccountDetails
import com.fluxa.personal.domain.model.AccountType
import com.fluxa.personal.domain.model.Category
import com.fluxa.personal.domain.model.CategoryDetails
import com.fluxa.personal.domain.model.FinancialAccount
import com.fluxa.personal.domain.model.TransactionDirection
import com.fluxa.personal.domain.model.TransactionDraft
import com.fluxa.personal.domain.model.TransactionError
import com.fluxa.personal.domain.model.TransactionHistoryFilter
import com.fluxa.personal.domain.model.TransactionResult.Rejected
import com.fluxa.personal.domain.model.TransactionResult.Saved
import com.fluxa.personal.domain.model.TransactionStatus
import java.time.Instant
import java.time.LocalDate
import java.util.UUID
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class TransactionRulesTest {
    private val today = LocalDate.of(2024, 2, 29)
    private val now = Instant.parse("2024-02-29T12:00:00Z")
    private val rules = TransactionRules(today, now)
    private val id = UUID(0, 1)
    private val account = FinancialAccount(
        UUID(0, 2), AccountDetails("Banco", AccountType.CHECKING, Money(0), today), now, now, null,
    )
    private val category = Category(UUID(0, 3), CategoryDetails("Trabalho", TransactionDirection.INCOME), null)
    private val draft = TransactionDraft(
        TransactionDirection.INCOME, account.id, category.id, Money(100), today, "  Descrição  ",
    )

    @Test
    fun createsIncomeAndExpenseWithPositiveMagnitudeAndStableId() {
        val income = (rules.create(id, null, draft, account, category) as Saved).transaction
        assertEquals(id, income.id)
        assertEquals(draft.copy(description = "Descrição"), income.details)
        assertEquals(TransactionStatus.COMPLETED, income.status)
        assertEquals(now, income.createdAt)

        val expenseDraft = draft.copy(direction = TransactionDirection.EXPENSE, amount = Money(Long.MAX_VALUE))
        val expenseCategory = category.copy(details = CategoryDetails("Mercado", TransactionDirection.EXPENSE))
        val expense = (rules.create(id, null, expenseDraft, account, expenseCategory) as Saved).transaction
        assertEquals(Money(Long.MAX_VALUE), expense.details.amount)
        assertEquals(TransactionDirection.EXPENSE, expense.details.direction)
    }

    @Test
    fun rejectsInvalidAmountFutureDateAndIncompatibleReferences() {
        fun rejection(
            error: TransactionError,
            input: TransactionDraft = draft,
            source: FinancialAccount? = account,
            classification: Category? = category,
        ) = assertEquals(Rejected(error), rules.create(id, null, input, source, classification))

        rejection(TransactionError.INVALID_AMOUNT, draft.copy(amount = Money(0)))
        rejection(TransactionError.INVALID_AMOUNT, draft.copy(amount = Money(Long.MIN_VALUE)))
        rejection(TransactionError.FUTURE_DATE, draft.copy(occurredOn = today.plusDays(1)))
        rejection(TransactionError.ACCOUNT_NOT_FOUND, source = null)
        rejection(TransactionError.ACCOUNT_ARCHIVED, source = account.copy(archivedAt = now))
        rejection(TransactionError.CATEGORY_NOT_FOUND, classification = null)
        rejection(TransactionError.CATEGORY_ARCHIVED, classification = category.copy(archivedAt = now))
        rejection(TransactionError.CATEGORY_DIRECTION_MISMATCH, draft.copy(direction = TransactionDirection.EXPENSE))
    }

    @Test
    fun editCancelAndRetriesKeepIdentityAndOriginalCreationTime() {
        val initial = (rules.create(id, null, draft, account, category) as Saved).transaction
        val updatedAt = now.plusSeconds(1)
        val later = TransactionRules(today, updatedAt)
        val details = draft.copy(amount = Money(999), occurredOn = today.minusDays(1), description = "  ")
        val edited = (later.edit(initial, details, account, category) as Saved).transaction
        assertEquals(initial.copy(details = details.copy(description = null), updatedAt = updatedAt), edited)
        assertEquals(Rejected(TransactionError.DUPLICATE_ID), rules.create(id, edited, draft, account, category))
        assertEquals(Rejected(TransactionError.NOT_FOUND), later.edit(null, draft, account, category))

        val cancelled = (later.cancel(edited) as Saved).transaction
        assertEquals(edited.copy(cancelledAt = updatedAt), cancelled)
        assertEquals(TransactionStatus.CANCELLED, cancelled.status)
        assertEquals(Rejected(TransactionError.CANCELLED), later.cancel(cancelled))
        assertEquals(Rejected(TransactionError.CANCELLED), later.edit(cancelled, draft, account, category))
        assertEquals(Rejected(TransactionError.DUPLICATE_ID), later.create(id, cancelled, draft, account, category))
    }

    @Test
    fun historyAllowsEmptyPeriodButRejectsReversedDates() {
        TransactionHistoryFilter(fromInclusive = today, untilExclusive = today)
        assertThrows(IllegalArgumentException::class.java) {
            TransactionHistoryFilter(fromInclusive = today, untilExclusive = today.minusDays(1))
        }
    }
}
