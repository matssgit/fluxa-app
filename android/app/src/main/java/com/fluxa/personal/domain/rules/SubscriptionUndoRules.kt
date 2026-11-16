package com.fluxa.personal.domain.rules

import com.fluxa.personal.domain.model.FinancialTransaction
import com.fluxa.personal.domain.model.Installment
import com.fluxa.personal.domain.model.InstallmentStatus
import com.fluxa.personal.domain.model.Purchase
import com.fluxa.personal.domain.model.PurchaseStatus
import com.fluxa.personal.domain.model.SubscriptionOccurrence
import com.fluxa.personal.domain.model.SubscriptionOccurrenceStatus
import com.fluxa.personal.domain.model.SubscriptionSource
import com.fluxa.personal.domain.model.SubscriptionUndoError
import com.fluxa.personal.domain.model.TransactionDirection
import com.fluxa.personal.domain.model.TransactionStatus
import java.util.UUID

internal object SubscriptionUndoRules {
    fun validateOccurrence(occurrence: SubscriptionOccurrence, expectedId: UUID): SubscriptionUndoError? =
        when (occurrence.status) {
            SubscriptionOccurrenceStatus.PENDING, SubscriptionOccurrenceStatus.OVERDUE -> SubscriptionUndoError.NOT_RESOLVED
            SubscriptionOccurrenceStatus.SKIPPED -> SubscriptionUndoError.SKIPPED
            SubscriptionOccurrenceStatus.PAID, SubscriptionOccurrenceStatus.CHARGED ->
                SubscriptionUndoError.CONFLICT.takeIf { occurrence.resultId != expectedId }
        }

    fun validateAccount(occurrence: SubscriptionOccurrence, payment: FinancialTransaction?): SubscriptionUndoError? {
        val details = occurrence.details
        val source = details.source as? SubscriptionSource.Account
        val matches = occurrence.status == SubscriptionOccurrenceStatus.PAID && source != null &&
            payment != null && payment.id == occurrence.resultId && payment.subscriptionOccurrence == occurrence.key &&
            payment.status == TransactionStatus.COMPLETED && payment.installmentId == null &&
            payment.details.direction == TransactionDirection.EXPENSE && payment.details.accountId == source.id &&
            payment.details.categoryId == details.categoryId && payment.details.amount == details.amount
        return SubscriptionUndoError.CONFLICT.takeUnless { matches }
    }

    fun validateCard(
        occurrence: SubscriptionOccurrence,
        purchase: Purchase?,
        installment: Installment?,
        hasPayment: Boolean,
    ): SubscriptionUndoError? {
        val details = occurrence.details
        val source = details.source as? SubscriptionSource.Card
        if (occurrence.status != SubscriptionOccurrenceStatus.CHARGED || source == null ||
            purchase == null || purchase.id != occurrence.resultId || purchase.subscriptionOccurrence != occurrence.key ||
            purchase.details.cardId != source.id || purchase.details.categoryId != details.categoryId ||
            purchase.details.total != details.amount || purchase.details.installmentCount != 1 ||
            installment == null || installment.purchaseId != purchase.id || installment.number != 1 ||
            installment.amount != details.amount
        ) return SubscriptionUndoError.CONFLICT
        if (installment.status == InstallmentStatus.PAID) return SubscriptionUndoError.CARD_CHARGE_ALREADY_PAID
        val expectedStatus = when (purchase.status) {
            PurchaseStatus.ACTIVE -> InstallmentStatus.PENDING
            PurchaseStatus.CANCELLED -> InstallmentStatus.CANCELLED
        }
        return SubscriptionUndoError.CONFLICT.takeIf { hasPayment || installment.status != expectedStatus }
    }
}
