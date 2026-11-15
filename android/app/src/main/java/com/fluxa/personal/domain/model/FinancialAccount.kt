package com.fluxa.personal.domain.model

import com.fluxa.personal.core.money.Money
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

internal enum class AccountType {
    CHECKING,
    WALLET,
    SAVINGS,
}

internal data class AccountDetails(
    val name: String,
    val type: AccountType,
    val openingBalance: Money,
    val openingDate: LocalDate,
    val includeInAvailable: Boolean = true,
) {
    init {
        require(name.isNotBlank()) { "Account name must not be blank." }
    }
}

internal data class FinancialAccount(
    val id: UUID,
    val details: AccountDetails,
    val createdAt: Instant,
    val updatedAt: Instant,
    val archivedAt: Instant?,
)
