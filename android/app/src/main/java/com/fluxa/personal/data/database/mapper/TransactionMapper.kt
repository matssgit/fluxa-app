package com.fluxa.personal.data.database.mapper

import com.fluxa.personal.core.money.Money
import com.fluxa.personal.data.database.dao.BalanceRow
import com.fluxa.personal.data.database.entity.TransactionEntity
import com.fluxa.personal.domain.model.FinancialTransaction
import com.fluxa.personal.domain.model.TransactionDirection
import com.fluxa.personal.domain.model.TransactionDraft
import com.fluxa.personal.domain.rules.AccountBalanceInput
import com.fluxa.personal.domain.rules.BalanceMovement
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

internal fun TransactionEntity.toDomain() = FinancialTransaction(
    id = UUID.fromString(id),
    details = TransactionDraft(
        direction = TransactionDirection.valueOf(direction),
        accountId = UUID.fromString(accountId),
        categoryId = UUID.fromString(categoryId),
        amount = Money(amountCents),
        occurredOn = LocalDate.ofEpochDay(occurredOn),
        description = description,
    ),
    createdAt = Instant.parse(createdAt),
    updatedAt = Instant.parse(updatedAt),
    cancelledAt = cancelledAt?.let(Instant::parse),
    installmentId = installmentId?.let(UUID::fromString),
    subscriptionOccurrence = subscriptionKey(subscriptionId, occurrenceDate),
)

internal fun FinancialTransaction.toEntity() = TransactionEntity(
    id = id.toString(),
    accountId = details.accountId.toString(),
    categoryId = details.categoryId.toString(),
    direction = details.direction.name,
    amountCents = details.amount.cents,
    occurredOn = details.occurredOn.toEpochDay(),
    description = details.description,
    createdAt = createdAt.toString(),
    updatedAt = updatedAt.toString(),
    cancelledAt = cancelledAt?.toString(),
    installmentId = installmentId?.toString(),
    subscriptionId = subscriptionOccurrence?.subscriptionId?.toString(),
    occurrenceDate = subscriptionOccurrence?.occurrenceDate?.toEpochDay(),
)

internal fun List<BalanceRow>.toBalanceInputs(): List<AccountBalanceInput> =
    groupBy { it.accountId }.map { (accountId, rows) ->
        val account = rows.first()
        AccountBalanceInput(
            id = UUID.fromString(accountId),
            openingBalance = Money(account.openingBalanceCents),
            openingDate = LocalDate.parse(account.openingDate),
            includedAndActive = account.includeInAvailable && account.archivedAt == null,
            movements = rows.mapNotNull { row ->
                row.amountCents?.let { amount ->
                    BalanceMovement(
                        direction = TransactionDirection.valueOf(requireNotNull(row.direction)),
                        amount = Money(amount),
                        occurredOn = LocalDate.ofEpochDay(requireNotNull(row.occurredOn)),
                    )
                }
            },
        )
    }
