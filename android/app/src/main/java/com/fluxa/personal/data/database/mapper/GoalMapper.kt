package com.fluxa.personal.data.database.mapper

import com.fluxa.personal.core.money.Money
import com.fluxa.personal.data.database.dao.GoalWithAccount
import com.fluxa.personal.data.database.dao.GoalWithMovements
import com.fluxa.personal.data.database.entity.GoalEntity
import com.fluxa.personal.data.database.entity.GoalMovementEntity
import com.fluxa.personal.domain.model.Goal
import com.fluxa.personal.domain.model.GoalDetails
import com.fluxa.personal.domain.model.GoalMovement
import com.fluxa.personal.domain.model.GoalMovementType
import com.fluxa.personal.domain.rules.GoalRules
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

internal fun GoalEntity.toDomain() = Goal(
    UUID.fromString(id),
    GoalDetails(name, Money(targetAmountCents), UUID.fromString(accountId), targetDate?.let(LocalDate::ofEpochDay)),
    Instant.parse(createdAt), Instant.parse(updatedAt), completedAt?.let(Instant::parse), archivedAt?.let(Instant::parse),
)

internal fun Goal.toEntity() = GoalEntity(
    id.toString(), details.name, details.targetAmount.cents, details.targetDate?.toEpochDay(), details.accountId.toString(),
    createdAt.toString(), updatedAt.toString(), completedAt?.toString(), archivedAt?.toString(),
)

internal fun GoalMovementEntity.toDomain() = GoalMovement(
    UUID.fromString(id), UUID.fromString(goalId), GoalMovementType.valueOf(type), Money(amountCents),
    LocalDate.ofEpochDay(occurredOn), Instant.parse(createdAt),
)

internal fun GoalMovement.toEntity() = GoalMovementEntity(
    id.toString(), goalId.toString(), type.name, amount.cents, occurredOn.toEpochDay(), createdAt.toString(),
)

internal fun GoalWithMovements.reserved() = GoalRules.reserved(movements.map { it.toDomain() })

internal fun GoalWithAccount.toSummary() = GoalRules.summarize(row.goal.toDomain(), account.toDomain(), row.reserved())
