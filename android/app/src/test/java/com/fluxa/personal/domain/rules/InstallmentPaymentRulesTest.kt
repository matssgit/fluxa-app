package com.fluxa.personal.domain.rules

import com.fluxa.personal.core.money.Money
import com.fluxa.personal.domain.model.AccountDetails
import com.fluxa.personal.domain.model.AccountType
import com.fluxa.personal.domain.model.FinancialAccount
import com.fluxa.personal.domain.model.Installment
import com.fluxa.personal.domain.model.InstallmentPaymentError
import com.fluxa.personal.domain.model.InstallmentPaymentResult.AlreadyPaid
import com.fluxa.personal.domain.model.InstallmentPaymentResult.Paid
import com.fluxa.personal.domain.model.InstallmentPaymentResult.Rejected
import com.fluxa.personal.domain.model.InstallmentStatus
import com.fluxa.personal.domain.model.Purchase
import com.fluxa.personal.domain.model.PurchaseDraft
import com.fluxa.personal.domain.model.TransactionDirection
import com.fluxa.personal.domain.model.TransactionError
import com.fluxa.personal.domain.model.TransactionResult
import com.fluxa.personal.domain.model.TransactionStatus
import java.time.Instant
import java.time.LocalDate
import java.util.UUID
import org.junit.Assert.assertEquals
import org.junit.Test

class InstallmentPaymentRulesTest {
    private val today = LocalDate.of(2026, 11, 16)
    private val now = Instant.parse("2026-11-16T14:00:00Z")
    private val due = today.minusDays(2)
    private val account = FinancialAccount(
        UUID(0, 1), AccountDetails("Banco", AccountType.CHECKING, Money(0), today.minusMonths(1)), now, now, null,
    )
    private val purchase = Purchase(
        UUID(0, 2),
        PurchaseDraft(UUID(0, 3), UUID(0, 4), "Fone", Money(2000), 2, today.minusDays(10)),
        due, now, now,
    )
    private val installment = Installment(UUID(0, 5), purchase.id, 2, Money(1000), due, InstallmentStatus.PENDING)
    private val rules = InstallmentPaymentRules(today, now)
    private val paymentId = UUID(0, 6)

    @Test
    fun createsAnExactCompletedExpenseUsingThePaymentDateBeforeOrAfterDue() {
        for (paidOn in listOf(due.minusDays(1), due, today)) {
            val result = rules.prepare(paymentId, installment, purchase, null, account.id, account, paidOn) as Paid
            val transaction = result.transaction
            assertEquals(paymentId, transaction.id)
            assertEquals(installment.id, transaction.installmentId)
            assertEquals(InstallmentStatus.PENDING, installment.status)
            assertEquals(due, installment.dueDate)
            assertEquals(TransactionStatus.COMPLETED, transaction.status)
            assertEquals(TransactionDirection.EXPENSE, transaction.details.direction)
            assertEquals(account.id, transaction.details.accountId)
            assertEquals(purchase.details.categoryId, transaction.details.categoryId)
            assertEquals(installment.amount, transaction.details.amount)
            assertEquals(paidOn, transaction.details.occurredOn)
            assertEquals("Pagamento • Fone • 2/2", transaction.details.description)
        }
    }

    @Test
    fun rejectsFutureDateBeforeOpeningAndArchivedOrMissingAccount() {
        assertEquals(
            Rejected(InstallmentPaymentError.PAYMENT_DATE_IN_FUTURE),
            rules.prepare(paymentId, installment, purchase, null, account.id, account, today.plusDays(1)),
        )
        assertEquals(
            Rejected(InstallmentPaymentError.PAYMENT_BEFORE_ACCOUNT_OPENING),
            rules.prepare(paymentId, installment, purchase, null, account.id, account, account.details.openingDate.minusDays(1)),
        )
        assertEquals(
            Rejected(InstallmentPaymentError.ACCOUNT_ARCHIVED),
            rules.prepare(paymentId, installment, purchase, null, account.id, account.copy(archivedAt = now), today),
        )
        assertEquals(
            Rejected(InstallmentPaymentError.ACCOUNT_NOT_FOUND),
            rules.prepare(paymentId, installment, purchase, null, account.id, null, today),
        )
    }

    @Test
    fun recognizesOnlyValidLinksForRetriesAndRejectsCancelledStates() {
        val transaction = (rules.prepare(paymentId, installment, purchase, null, account.id, account, today) as Paid).transaction
        val paid = installment.copy(status = InstallmentStatus.PAID)
        assertEquals(AlreadyPaid(paymentId), rules.prepare(UUID(0, 7), paid, purchase, transaction, account.id, null, today))
        assertEquals(
            Rejected(InstallmentPaymentError.INVALID_PAYMENT_LINK),
            rules.prepare(paymentId, paid, purchase, transaction.copy(installmentId = null), account.id, account, today),
        )
        assertEquals(
            Rejected(InstallmentPaymentError.PURCHASE_CANCELLED),
            rules.prepare(paymentId, installment, purchase.copy(cancelledAt = now), null, account.id, account, today),
        )
        assertEquals(
            Rejected(InstallmentPaymentError.INSTALLMENT_CANCELLED),
            rules.prepare(paymentId, installment.copy(status = InstallmentStatus.CANCELLED), purchase, null, account.id, account, today),
        )
    }

    @Test
    fun genericTransactionRulesCannotEditOrCancelManagedPayments() {
        val transaction = (rules.prepare(paymentId, installment, purchase, null, account.id, account, today) as Paid).transaction
        val transactions = TransactionRules(today, now)
        val expected = TransactionResult.Rejected(TransactionError.MANAGED_BY_INSTALLMENT)
        assertEquals(expected, transactions.edit(transaction, transaction.details, account, null))
        assertEquals(expected, transactions.cancel(transaction))
    }
}
