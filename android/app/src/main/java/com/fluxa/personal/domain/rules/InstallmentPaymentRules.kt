package com.fluxa.personal.domain.rules

import com.fluxa.personal.domain.model.FinancialAccount
import com.fluxa.personal.domain.model.FinancialTransaction
import com.fluxa.personal.domain.model.Installment
import com.fluxa.personal.domain.model.InstallmentPaymentError
import com.fluxa.personal.domain.model.InstallmentPaymentResult
import com.fluxa.personal.domain.model.InstallmentPaymentResult.AlreadyPaid
import com.fluxa.personal.domain.model.InstallmentPaymentResult.Paid
import com.fluxa.personal.domain.model.InstallmentPaymentResult.Rejected
import com.fluxa.personal.domain.model.InstallmentStatus
import com.fluxa.personal.domain.model.Purchase
import com.fluxa.personal.domain.model.PurchaseStatus
import com.fluxa.personal.domain.model.TransactionDirection
import com.fluxa.personal.domain.model.TransactionDraft
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

internal class InstallmentPaymentRules(private val today: LocalDate, private val now: Instant) {
    fun prepare(
        transactionId: UUID,
        installment: Installment,
        purchase: Purchase,
        existing: FinancialTransaction?,
        accountId: UUID,
        account: FinancialAccount?,
        paidOn: LocalDate,
    ): InstallmentPaymentResult {
        if (purchase.status == PurchaseStatus.CANCELLED) return Rejected(InstallmentPaymentError.PURCHASE_CANCELLED)
        if (installment.status == InstallmentStatus.CANCELLED) return Rejected(InstallmentPaymentError.INSTALLMENT_CANCELLED)
        if (installment.status == InstallmentStatus.PAID) {
            return if (existing != null && existing.matchesPayment(installment, purchase)) {
                AlreadyPaid(existing.id)
            } else {
                Rejected(InstallmentPaymentError.INVALID_PAYMENT_LINK)
            }
        }
        if (existing != null) return Rejected(InstallmentPaymentError.INVALID_PAYMENT_LINK)
        if (account == null || account.id != accountId) return Rejected(InstallmentPaymentError.ACCOUNT_NOT_FOUND)
        if (account.archivedAt != null) return Rejected(InstallmentPaymentError.ACCOUNT_ARCHIVED)
        if (paidOn > today) return Rejected(InstallmentPaymentError.PAYMENT_DATE_IN_FUTURE)
        if (paidOn < account.details.openingDate) return Rejected(InstallmentPaymentError.PAYMENT_BEFORE_ACCOUNT_OPENING)

        return Paid(
            FinancialTransaction(
                id = transactionId,
                details = TransactionDraft(
                    direction = TransactionDirection.EXPENSE,
                    accountId = accountId,
                    categoryId = purchase.details.categoryId,
                    amount = installment.amount,
                    occurredOn = paidOn,
                    description = "Pagamento • ${purchase.details.title} • ${installment.number}/${purchase.details.installmentCount}",
                ),
                createdAt = now,
                updatedAt = now,
                installmentId = installment.id,
            ),
        )
    }
}

internal fun FinancialTransaction.matchesPayment(installment: Installment, purchase: Purchase): Boolean =
    installmentId == installment.id && cancelledAt == null &&
        details.direction == TransactionDirection.EXPENSE && details.amount == installment.amount &&
        details.categoryId == purchase.details.categoryId
