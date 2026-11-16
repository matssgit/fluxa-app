package com.fluxa.personal.domain.model

import java.util.UUID

internal enum class InstallmentPaymentError {
    INSTALLMENT_NOT_FOUND,
    INSTALLMENT_CANCELLED,
    INSTALLMENT_NOT_PAID,
    PURCHASE_CANCELLED,
    ACCOUNT_NOT_FOUND,
    ACCOUNT_ARCHIVED,
    PAYMENT_DATE_IN_FUTURE,
    PAYMENT_BEFORE_ACCOUNT_OPENING,
    INVALID_PAYMENT_LINK,
    PAYMENT_CHANGED,
    INSUFFICIENT_LIMIT_TO_UNDO,
}

internal sealed interface InstallmentPaymentResult {
    data class Paid(val transaction: FinancialTransaction) : InstallmentPaymentResult
    data class AlreadyPaid(val transactionId: UUID) : InstallmentPaymentResult
    data class Undone(val transactionId: UUID) : InstallmentPaymentResult
    data class Rejected(val error: InstallmentPaymentError) : InstallmentPaymentResult
}
