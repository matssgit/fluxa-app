package com.fluxa.personal.data.database.mapper

import com.fluxa.personal.core.date.BillingDay
import com.fluxa.personal.core.money.Money
import com.fluxa.personal.data.database.dao.CreditCardSummaryRow
import com.fluxa.personal.data.database.entity.CreditCardEntity
import com.fluxa.personal.data.database.entity.InstallmentEntity
import com.fluxa.personal.data.database.entity.PurchaseEntity
import com.fluxa.personal.domain.model.CreditCard
import com.fluxa.personal.domain.model.CreditCardDetails
import com.fluxa.personal.domain.model.CreditCardSummary
import com.fluxa.personal.domain.model.Installment
import com.fluxa.personal.domain.model.InstallmentStatus
import com.fluxa.personal.domain.model.Purchase
import com.fluxa.personal.domain.model.PurchaseDraft
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

internal fun CreditCardSummaryRow.toDomain() = CreditCardSummary(
    card = card.toDomain(),
    committedAmount = Money(committedAmountCents),
)

private fun CreditCardEntity.toDomain() = CreditCard(
    id = UUID.fromString(id),
    details = CreditCardDetails(name, Money(totalLimitCents), BillingDay(closingDay), BillingDay(dueDay), color),
    archivedAt = archivedAt?.let(Instant::parse),
    createdAt = Instant.parse(createdAt),
    updatedAt = Instant.parse(updatedAt),
)

internal fun CreditCard.toEntity() = CreditCardEntity(
    id = id.toString(),
    name = details.name,
    totalLimitCents = details.totalLimit.cents,
    closingDay = details.closingDay.day,
    dueDay = details.dueDay.day,
    color = details.color,
    archivedAt = archivedAt?.toString(),
    createdAt = createdAt.toString(),
    updatedAt = updatedAt.toString(),
)

internal fun PurchaseEntity.toDomain() = Purchase(
    id = UUID.fromString(id),
    details = PurchaseDraft(
        cardId = UUID.fromString(cardId),
        categoryId = UUID.fromString(categoryId),
        title = title,
        total = Money(totalCents),
        installmentCount = installmentCount,
        purchaseDate = LocalDate.ofEpochDay(purchaseDate),
        merchant = merchant,
        notes = notes,
    ),
    firstDueDate = LocalDate.ofEpochDay(firstDueDate),
    createdAt = Instant.parse(createdAt),
    updatedAt = Instant.parse(updatedAt),
    cancelledAt = cancelledAt?.let(Instant::parse),
    subscriptionOccurrence = subscriptionKey(subscriptionId, occurrenceDate),
)

internal fun Purchase.toEntity() = PurchaseEntity(
    id = id.toString(),
    cardId = details.cardId.toString(),
    categoryId = details.categoryId.toString(),
    title = details.title,
    merchant = details.merchant,
    notes = details.notes,
    totalCents = details.total.cents,
    installmentCount = details.installmentCount,
    purchaseDate = details.purchaseDate.toEpochDay(),
    firstDueDate = firstDueDate.toEpochDay(),
    createdAt = createdAt.toString(),
    updatedAt = updatedAt.toString(),
    cancelledAt = cancelledAt?.toString(),
    subscriptionId = subscriptionOccurrence?.subscriptionId?.toString(),
    occurrenceDate = subscriptionOccurrence?.occurrenceDate?.toEpochDay(),
)

internal fun InstallmentEntity.toDomain() = Installment(
    id = UUID.fromString(id),
    purchaseId = UUID.fromString(purchaseId),
    number = number,
    amount = Money(amountCents),
    dueDate = LocalDate.ofEpochDay(dueDate),
    status = InstallmentStatus.valueOf(status),
)

internal fun Installment.toEntity() = InstallmentEntity(
    id = id.toString(),
    purchaseId = purchaseId.toString(),
    number = number,
    amountCents = amount.cents,
    dueDate = dueDate.toEpochDay(),
    status = status.name,
)
