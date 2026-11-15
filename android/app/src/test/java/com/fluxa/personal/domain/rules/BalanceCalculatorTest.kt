package com.fluxa.personal.domain.rules

import com.fluxa.personal.core.money.Money
import com.fluxa.personal.domain.model.TransactionDirection.EXPENSE
import com.fluxa.personal.domain.model.TransactionDirection.INCOME
import com.fluxa.personal.domain.rules.CalculatedBalance.Exact
import com.fluxa.personal.domain.rules.CalculatedBalance.Overflow
import java.time.LocalDate
import java.util.UUID
import org.junit.Assert.assertEquals
import org.junit.Test

class BalanceCalculatorTest {
    private val date = LocalDate.of(2024, 2, 29)
    private val first = UUID(0, 1)
    private val second = UUID(0, 2)

    @Test
    fun incomeAndExpensesUseInclusiveOpeningDateAndExcludedAccountsKeepOwnBalance() {
        val accounts = listOf(
            AccountBalanceInput(
                first, Money(200), date, true,
                listOf(
                    BalanceMovement(INCOME, Money(300), date.minusDays(1)),
                    BalanceMovement(INCOME, Money(20), date),
                    BalanceMovement(EXPENSE, Money(350), date.plusDays(1)),
                ),
            ),
            AccountBalanceInput(second, Money(999), date, false, emptyList()),
        )

        val snapshot = BalanceCalculator.calculate(accounts)
        assertEquals(mapOf(first to Exact(Money(-130)), second to Exact(Money(999))), snapshot.accounts)
        assertEquals(Exact(Money(-130)), snapshot.available)
    }

    @Test
    fun exactTotalsNeverWrapAndIntermediateOverflowCanReconcile() {
        val income = BalanceMovement(INCOME, Money(Long.MAX_VALUE), date)
        val expense = BalanceMovement(EXPENSE, Money(Long.MAX_VALUE), date)
        val account = AccountBalanceInput(first, Money(1), date, true, listOf(income))

        assertEquals(Overflow, BalanceCalculator.calculate(listOf(account)).available)
        val offset = AccountBalanceInput(second, Money(-1), date, true, emptyList())
        val offsetSnapshot = BalanceCalculator.calculate(listOf(account, offset))
        assertEquals(Overflow, offsetSnapshot.accounts[first])
        assertEquals(Exact(Money(Long.MAX_VALUE)), offsetSnapshot.available)
        assertEquals(
            Exact(Money(1)),
            BalanceCalculator.calculate(listOf(account.copy(movements = listOf(income, expense)))).available,
        )
        assertEquals(
            Overflow,
            BalanceCalculator.calculate(listOf(account.copy(openingBalance = Money(Long.MIN_VALUE), movements = listOf(expense)))).available,
        )
        assertEquals(
            Exact(Money(Long.MIN_VALUE)),
            BalanceCalculator.calculate(listOf(account.copy(openingBalance = Money(Long.MIN_VALUE), movements = emptyList()))).available,
        )
        assertEquals(Exact(Money(0)), BalanceCalculator.calculate(emptyList()).available)
    }
}
