package com.fluxa.personal.domain.model

import com.fluxa.personal.core.money.Money
import com.fluxa.personal.domain.rules.CalculatedBalance
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

internal enum class GoalStatus { ACTIVE, COMPLETED, ARCHIVED }
internal enum class GoalMovementType { ALLOCATE, RELEASE }

internal data class GoalDetails(
    val name: String,
    val targetAmount: Money,
    val accountId: UUID,
    val targetDate: LocalDate? = null,
)

internal data class Goal(
    val id: UUID,
    val details: GoalDetails,
    val createdAt: Instant,
    val updatedAt: Instant,
    val completedAt: Instant? = null,
    val archivedAt: Instant? = null,
) {
    val status: GoalStatus
        get() = when {
            archivedAt != null -> GoalStatus.ARCHIVED
            completedAt != null -> GoalStatus.COMPLETED
            else -> GoalStatus.ACTIVE
        }
}

internal data class GoalMovement(
    val id: UUID,
    val goalId: UUID,
    val type: GoalMovementType,
    val amount: Money,
    val occurredOn: LocalDate,
    val createdAt: Instant,
)

internal data class GoalSummary(
    val goal: Goal,
    val account: FinancialAccount,
    val reservedAmount: Money,
    val remainingAmount: Money,
    val progressBasisPoints: Int,
) {
    val targetAmount: Money get() = goal.details.targetAmount
}

internal data class AccountReserveSummary(
    val accountId: UUID,
    val includedInAvailable: Boolean,
    val accountBalance: CalculatedBalance,
    val reservedAmount: CalculatedBalance,
    val availableAmount: CalculatedBalance,
)

internal data class ReserveBalanceSnapshot(
    val accounts: Map<UUID, AccountReserveSummary>,
    val totalBalance: CalculatedBalance,
    val totalReserved: CalculatedBalance,
    val availableBalance: CalculatedBalance,
)

internal enum class GoalError {
    NOT_FOUND, ARCHIVED, COMPLETED, INVALID_NAME, INVALID_TARGET, INVALID_AMOUNT,
    ACCOUNT_NOT_FOUND, ACCOUNT_ARCHIVED, ACCOUNT_CHANGE_NOT_ALLOWED, TARGET_BELOW_RESERVED,
    ALLOCATION_EXCEEDS_TARGET, INSUFFICIENT_AVAILABLE_FUNDS, RELEASE_EXCEEDS_RESERVED,
    TARGET_NOT_REACHED, HAS_RESERVED_FUNDS,
}

internal sealed interface GoalResult<out T> {
    data class Saved<T>(val value: T) : GoalResult<T>
    data class Rejected(val error: GoalError) : GoalResult<Nothing>
}
