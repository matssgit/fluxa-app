package com.fluxa.personal.domain.rules

import com.fluxa.personal.core.date.BillingDay
import com.fluxa.personal.core.money.Money
import com.fluxa.personal.domain.model.*
import java.time.Instant
import java.time.LocalDate
import java.util.UUID
import org.junit.Assert.*
import org.junit.Test

class SubscriptionUndoRulesTest {
    private val id = UUID.randomUUID()
    private val source = UUID.randomUUID()
    private val category = UUID.randomUUID()
    private val date = LocalDate.of(2024, 2, 29)
    private val now = Instant.EPOCH
    private val money = Money(60000)
    private val key = SubscriptionOccurrenceKey(UUID.randomUUID(), date)

    @Test
    fun validatesOnlyTheManagedAccountPaymentAndRejectsAnOldUndoAfterRepayment() {
        val details = SubscriptionDetails("Anual", money, category, SubscriptionSource.Account(source))
        val paid = SubscriptionOccurrence(key, details, SubscriptionOccurrenceStatus.PAID, id)
        val payment = FinancialTransaction(
            id, TransactionDraft(TransactionDirection.EXPENSE, source, category, money, date),
            now, now, subscriptionOccurrence = key,
        )
        assertNull(SubscriptionUndoRules.validateOccurrence(paid, id))
        assertNull(SubscriptionUndoRules.validateAccount(paid, payment))
        assertEquals(SubscriptionUndoError.CONFLICT, SubscriptionUndoRules.validateAccount(paid, payment.copy(subscriptionOccurrence = null)))
        val open = paid.copy(status = SubscriptionOccurrenceStatus.OVERDUE, resultId = null)
        assertEquals(SubscriptionUndoError.NOT_RESOLVED, SubscriptionUndoRules.validateOccurrence(open, id))
        val newId = UUID.randomUUID()
        val repaid = open.copy(status = SubscriptionOccurrenceStatus.PAID, resultId = newId)
        assertEquals(SubscriptionUndoError.CONFLICT, SubscriptionUndoRules.validateOccurrence(repaid, id))
        assertNull(SubscriptionUndoRules.validateAccount(repaid, payment.copy(id = newId)))
    }

    @Test
    fun protectsPaidInstallmentsAndReconcilesOnlyASafePreviouslyCancelledCharge() {
        val details = SubscriptionDetails("Anual", money, category, SubscriptionSource.Card(source))
        val charged = SubscriptionOccurrence(key, details, SubscriptionOccurrenceStatus.CHARGED, id)
        val purchase = Purchase(id, PurchaseDraft(source, category, details.name, money, 1, date), date, now, now,
            subscriptionOccurrence = key)
        val part = Installment(UUID.randomUUID(), id, 1, money, date, InstallmentStatus.PENDING)
        assertNull(SubscriptionUndoRules.validateCard(charged, purchase, part, hasPayment = false))
        assertEquals(SubscriptionUndoError.CARD_CHARGE_ALREADY_PAID,
            SubscriptionUndoRules.validateCard(charged, purchase, part.copy(status = InstallmentStatus.PAID), hasPayment = true))
        assertEquals(SubscriptionUndoError.CONFLICT, SubscriptionUndoRules.validateCard(charged, purchase, part, hasPayment = true))
        val cancelled = purchase.copy(cancelledAt = now)
        assertEquals(SubscriptionUndoError.CONFLICT, SubscriptionUndoRules.validateCard(charged, cancelled, part, hasPayment = false))
        assertNull(SubscriptionUndoRules.validateCard(charged, cancelled, part.copy(status = InstallmentStatus.CANCELLED), hasPayment = false))
        val newId = UUID.randomUUID()
        val recharged = charged.copy(resultId = newId)
        assertEquals(SubscriptionUndoError.CONFLICT, SubscriptionUndoRules.validateOccurrence(recharged, id))
        assertNull(SubscriptionUndoRules.validateCard(recharged, purchase.copy(id = newId), part.copy(purchaseId = newId), false))
    }

    @Test
    fun skippedCannotBeUndoneAsAPayment() {
        val details = SubscriptionDetails("Anual", money, category, SubscriptionSource.Account(source))
        val skipped = SubscriptionOccurrence(key, details, SubscriptionOccurrenceStatus.SKIPPED)
        assertEquals(SubscriptionUndoError.SKIPPED, SubscriptionUndoRules.validateOccurrence(skipped, id))
    }

    @Test
    fun reopenedSnapshotRemainsVisibleOnTheDayTheScheduleIsDisabled() {
        val details = SubscriptionDetails("Anual", money, category, SubscriptionSource.Account(source))
        val reopened = SubscriptionOccurrence(key, details, SubscriptionOccurrenceStatus.PENDING)
        for (status in listOf(SubscriptionStatus.PAUSED, SubscriptionStatus.CANCELLED)) {
            val subscription = Subscription(
                key.subscriptionId, details.copy(amount = Money(90000)), SubscriptionFrequency.YEARLY,
                date, BillingDay(29), status, now, now,
            )
            val terms = listOf(SubscriptionTerms(date, subscription.details, enabled = false))
            assertEquals(reopened, SubscriptionCalendar.next(subscription, terms, listOf(reopened), date))
            val tomorrow = date.plusDays(1)
            val result = SubscriptionCalendar.agenda(subscription, terms, listOf(reopened), date, tomorrow, tomorrow)
            assertEquals(listOf(reopened.copy(status = SubscriptionOccurrenceStatus.OVERDUE)), result)
            assertNull(SubscriptionCalendar.next(subscription, terms, listOf(reopened), tomorrow))
            assertEquals(status, subscription.status)
        }
    }
}
