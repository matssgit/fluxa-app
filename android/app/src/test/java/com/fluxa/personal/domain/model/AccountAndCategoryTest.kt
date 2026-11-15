package com.fluxa.personal.domain.model

import com.fluxa.personal.core.money.Money
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class AccountAndCategoryTest {
    private val referenceDate = LocalDate.of(2024, 2, 29)

    @Test
    fun accountAcceptsSignedBrlOpeningBalancesAndFinancialDate() {
        for (cents in listOf(-1055L, 0L, 1055L)) {
            val details = AccountDetails("Conta", AccountType.CHECKING, Money(cents), referenceDate)
            assertEquals(Money(cents), details.openingBalance)
            assertEquals(referenceDate, details.openingDate)
            assertTrue(details.includeInAvailable)
        }
        assertEquals(setOf("CHECKING", "WALLET", "SAVINGS"), AccountType.entries.map { it.name }.toSet())
    }

    @Test
    fun namesCannotBeBlank() {
        assertThrows(IllegalArgumentException::class.java) {
            AccountDetails(" \t", AccountType.WALLET, Money(0), referenceDate)
        }
        assertThrows(IllegalArgumentException::class.java) {
            CategoryDetails(" \n", TransactionDirection.EXPENSE)
        }
    }

    @Test
    fun categoriesUseSharedFinancialDirectionAndOptionalAppearance() {
        val income = CategoryDetails("Outros", TransactionDirection.INCOME)
        val expense = CategoryDetails("Outros", TransactionDirection.EXPENSE, "tag", "#53654D")
        assertEquals(TransactionDirection.INCOME, income.kind)
        assertEquals(null, income.icon)
        assertEquals(null, income.color)
        assertEquals(TransactionDirection.EXPENSE, expense.kind)
        assertEquals("tag", expense.icon)
        assertEquals("#53654D", expense.color)
    }
}
