package com.fluxa.personal.domain.rules

import com.fluxa.personal.core.money.Money
import com.fluxa.personal.domain.model.TransactionDirection
import java.math.BigInteger
import java.time.LocalDate
import java.util.UUID

internal sealed interface CalculatedBalance {
    data class Exact(val amount: Money) : CalculatedBalance
    data object Overflow : CalculatedBalance
}

internal data class BalanceSnapshot(
    val accounts: Map<UUID, CalculatedBalance>,
    val available: CalculatedBalance,
)

internal data class BalanceMovement(
    val direction: TransactionDirection,
    val amount: Money,
    val occurredOn: LocalDate,
)

internal data class AccountBalanceInput(
    val id: UUID,
    val openingBalance: Money,
    val openingDate: LocalDate,
    val includedAndActive: Boolean,
    val movements: List<BalanceMovement>,
)

internal object BalanceCalculator {
    fun calculate(accounts: List<AccountBalanceInput>): BalanceSnapshot {
        var available = BigInteger.ZERO
        val balances = accounts.associate { account ->
            val balance = balanceOf(account)
            if (account.includedAndActive) available = available.add(balance)
            account.id to balance.toCalculatedBalance()
        }
        return BalanceSnapshot(balances, available.toCalculatedBalance())
    }

    fun balanceOf(account: AccountBalanceInput): BigInteger =
        account.movements.fold(BigInteger.valueOf(account.openingBalance.cents)) { total, movement ->
            if (movement.occurredOn < account.openingDate) {
                total
            } else {
                val cents = BigInteger.valueOf(movement.amount.cents)
                when (movement.direction) {
                    TransactionDirection.INCOME -> total.add(cents)
                    TransactionDirection.EXPENSE -> total.subtract(cents)
                }
            }
        }
}

internal fun BigInteger.toCalculatedBalance(): CalculatedBalance =
    if (bitLength() <= Long.SIZE_BITS - 1) CalculatedBalance.Exact(Money(toLong()))
    else CalculatedBalance.Overflow
