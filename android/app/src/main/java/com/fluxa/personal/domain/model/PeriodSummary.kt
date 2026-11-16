package com.fluxa.personal.domain.model

import com.fluxa.personal.domain.rules.CalculatedBalance
import com.fluxa.personal.domain.rules.toCalculatedBalance
import java.math.BigInteger

internal data class PeriodSummary(
    val income: CalculatedBalance,
    val expenses: CalculatedBalance,
    val net: CalculatedBalance,
) {
    companion object {
        internal fun fromCents(income: BigInteger, expenses: BigInteger) = PeriodSummary(
            income = income.toCalculatedBalance(),
            expenses = expenses.toCalculatedBalance(),
            net = income.subtract(expenses).toCalculatedBalance(),
        )

        internal val Overflow = PeriodSummary(
            CalculatedBalance.Overflow, CalculatedBalance.Overflow, CalculatedBalance.Overflow,
        )
    }
}
