package com.fluxa.personal.data.database.mapper

import com.fluxa.personal.core.money.Money
import com.fluxa.personal.data.database.entity.AccountEntity
import com.fluxa.personal.domain.model.AccountDetails
import com.fluxa.personal.domain.model.AccountType
import com.fluxa.personal.domain.model.FinancialAccount
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

internal fun AccountEntity.toDomain(): FinancialAccount = FinancialAccount(
    id = UUID.fromString(id),
    details = AccountDetails(
        name = name,
        type = AccountType.valueOf(type),
        openingBalance = Money(openingBalanceCents),
        openingDate = LocalDate.parse(openingDate),
        includeInAvailable = includeInAvailable,
    ),
    createdAt = Instant.parse(createdAt),
    updatedAt = Instant.parse(updatedAt),
    archivedAt = archivedAt?.let(Instant::parse),
)

internal fun FinancialAccount.toEntity(): AccountEntity = AccountEntity(
    id = id.toString(),
    name = details.name.trim(),
    type = details.type.name,
    openingBalanceCents = details.openingBalance.cents,
    openingDate = details.openingDate.toString(),
    includeInAvailable = details.includeInAvailable,
    createdAt = createdAt.toString(),
    updatedAt = updatedAt.toString(),
    archivedAt = archivedAt?.toString(),
)
