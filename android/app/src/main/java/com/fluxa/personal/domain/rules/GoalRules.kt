package com.fluxa.personal.domain.rules

import com.fluxa.personal.core.money.Money
import com.fluxa.personal.domain.model.AccountReserveSummary
import com.fluxa.personal.domain.model.FinancialAccount
import com.fluxa.personal.domain.model.Goal
import com.fluxa.personal.domain.model.GoalDetails
import com.fluxa.personal.domain.model.GoalError
import com.fluxa.personal.domain.model.GoalMovement
import com.fluxa.personal.domain.model.GoalMovementType
import com.fluxa.personal.domain.model.GoalStatus
import com.fluxa.personal.domain.model.GoalSummary
import com.fluxa.personal.domain.model.ReserveBalanceSnapshot
import java.math.BigInteger
import java.util.UUID

internal object GoalRules {
    fun validateDetails(details: GoalDetails): GoalError? = when {
        details.name.isBlank() -> GoalError.INVALID_NAME
        details.targetAmount.cents <= 0 -> GoalError.INVALID_TARGET
        else -> null
    }

    fun validateUpdate(goal: Goal, details: GoalDetails, reserved: Money, hasMovements: Boolean): GoalError? = when {
        goal.status == GoalStatus.ARCHIVED -> GoalError.ARCHIVED
        details.targetAmount < reserved -> GoalError.TARGET_BELOW_RESERVED
        details.accountId != goal.details.accountId && hasMovements -> GoalError.ACCOUNT_CHANGE_NOT_ALLOWED
        else -> validateDetails(details)
    }

    fun validateAllocation(goal: Goal, reserved: Money, amount: Money, available: BigInteger): GoalError? = when {
        goal.status == GoalStatus.ARCHIVED -> GoalError.ARCHIVED
        goal.status == GoalStatus.COMPLETED -> GoalError.COMPLETED
        amount.cents <= 0 -> GoalError.INVALID_AMOUNT
        amount > goal.details.targetAmount - reserved -> GoalError.ALLOCATION_EXCEEDS_TARGET
        amount.wide() > available -> GoalError.INSUFFICIENT_AVAILABLE_FUNDS
        else -> null
    }

    fun validateRelease(goal: Goal, reserved: Money, amount: Money): GoalError? = when {
        goal.status == GoalStatus.ARCHIVED -> GoalError.ARCHIVED
        amount.cents <= 0 -> GoalError.INVALID_AMOUNT
        amount > reserved -> GoalError.RELEASE_EXCEEDS_RESERVED
        else -> null
    }

    fun validateCompletion(goal: Goal, reserved: Money): GoalError? = when {
        goal.status == GoalStatus.ARCHIVED -> GoalError.ARCHIVED
        goal.status == GoalStatus.COMPLETED -> GoalError.COMPLETED
        goal.details.targetAmount != reserved -> GoalError.TARGET_NOT_REACHED
        else -> null
    }

    fun validateArchive(goal: Goal, reserved: Money): GoalError? = when {
        goal.status == GoalStatus.ARCHIVED -> GoalError.ARCHIVED
        reserved.cents != 0L -> GoalError.HAS_RESERVED_FUNDS
        else -> null
    }

    fun reserved(movements: List<GoalMovement>): Money {
        val cents = movements.fold(BigInteger.ZERO) { total, movement ->
            when (movement.type) {
                GoalMovementType.ALLOCATE -> total + movement.amount.wide()
                GoalMovementType.RELEASE -> total - movement.amount.wide()
            }
        }
        check(cents.signum() >= 0 && cents.bitLength() < Long.SIZE_BITS) { "Invalid goal reserve balance." }
        return Money(cents.toLong())
    }

    fun summarize(goal: Goal, account: FinancialAccount, reserved: Money): GoalSummary {
        val target = goal.details.targetAmount
        val progress = (reserved.wide() * BigInteger.valueOf(10_000) / target.wide()).toInt()
        return GoalSummary(goal, account, reserved, Money(maxOf(0, (target - reserved).cents)), progress)
    }
}

internal object ReserveBalanceCalculator {
    fun calculate(accounts: List<AccountBalanceInput>, reserves: Map<UUID, BigInteger>): ReserveBalanceSnapshot {
        var totalBalance = BigInteger.ZERO
        var totalReserved = BigInteger.ZERO
        var available = BigInteger.ZERO
        val summaries = accounts.associate { account ->
            val balance = BalanceCalculator.balanceOf(account)
            val reserved = reserves[account.id] ?: BigInteger.ZERO
            val accountAvailable = balance - reserved
            totalBalance += balance
            totalReserved += reserved
            if (account.includedAndActive) available += accountAvailable
            account.id to AccountReserveSummary(
                account.id, account.includedAndActive, balance.toCalculatedBalance(),
                reserved.toCalculatedBalance(), accountAvailable.toCalculatedBalance(),
            )
        }
        return ReserveBalanceSnapshot(
            summaries, totalBalance.toCalculatedBalance(), totalReserved.toCalculatedBalance(), available.toCalculatedBalance(),
        )
    }
}

internal fun Money.wide(): BigInteger = BigInteger.valueOf(cents)
