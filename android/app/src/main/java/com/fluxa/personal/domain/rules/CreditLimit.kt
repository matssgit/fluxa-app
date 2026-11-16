package com.fluxa.personal.domain.rules

import com.fluxa.personal.core.money.Money

internal data class CreditLimit(val total: Money, val committed: Money) {
    init {
        require(committed.cents >= 0 && total >= committed) { "Invalid credit limit or commitments." }
    }

    val available: Money get() = total - committed

    fun canCommit(amount: Money): Boolean = amount.cents > 0 && amount <= available

    fun canSetTotal(newTotal: Money): Boolean = newTotal >= committed
}
