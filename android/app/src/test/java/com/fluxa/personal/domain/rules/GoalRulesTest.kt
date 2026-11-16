package com.fluxa.personal.domain.rules

import com.fluxa.personal.core.money.Money
import com.fluxa.personal.domain.model.AccountDetails
import com.fluxa.personal.domain.model.AccountType
import com.fluxa.personal.domain.model.FinancialAccount
import com.fluxa.personal.domain.model.Goal
import com.fluxa.personal.domain.model.GoalDetails
import com.fluxa.personal.domain.model.GoalError
import com.fluxa.personal.domain.model.GoalMovement
import com.fluxa.personal.domain.model.GoalMovementType
import com.fluxa.personal.domain.model.GoalStatus
import com.fluxa.personal.domain.model.TransactionDirection
import com.fluxa.personal.domain.rules.CalculatedBalance.Exact
import java.math.BigInteger
import java.time.Instant
import java.time.LocalDate
import java.util.UUID
import org.junit.Assert.*
import org.junit.Test

class GoalRulesTest {
    private val accountId = UUID(0, 1)
    private val otherId = UUID(0, 2)
    private val date = LocalDate.of(2024, 2, 29)
    private val now = Instant.EPOCH
    private val details = GoalDetails("Viagem", Money(500_000), accountId)
    private val goal = Goal(UUID(0, 3), details, now, now)

    @Test
    fun validatesCreationAndEditsWithOptionalTargetDateAndImmutableHistoricalAccount() {
        assertNull(GoalRules.validateDetails(details))
        assertNull(GoalRules.validateDetails(details.copy(targetDate = date)))
        assertEquals(GoalError.INVALID_NAME, GoalRules.validateDetails(details.copy(name = " ")))
        assertEquals(GoalError.INVALID_TARGET, GoalRules.validateDetails(details.copy(targetAmount = Money(0))))
        assertNull(GoalRules.validateUpdate(goal, details.copy(targetAmount = Money(700_000)), Money(200_000), true))
        assertNull(GoalRules.validateUpdate(goal, details.copy(targetAmount = Money(200_000)), Money(200_000), true))
        assertEquals(GoalError.TARGET_BELOW_RESERVED,
            GoalRules.validateUpdate(goal, details.copy(targetAmount = Money(199_999)), Money(200_000), true))
        assertNull(GoalRules.validateUpdate(goal, details.copy(accountId = otherId), Money(0), false))
        assertEquals(GoalError.ACCOUNT_CHANGE_NOT_ALLOWED,
            GoalRules.validateUpdate(goal, details.copy(accountId = otherId), Money(0), true))
    }

    @Test
    fun allocationRespectsTargetAvailableAndPositiveAmounts() {
        val wide = BigInteger.valueOf(500_000)
        assertNull(GoalRules.validateAllocation(goal, Money(0), Money(150_000), wide))
        assertNull(GoalRules.validateAllocation(goal, Money(350_000), Money(150_000), wide))
        assertEquals(GoalError.ALLOCATION_EXCEEDS_TARGET, GoalRules.validateAllocation(goal, Money(350_000), Money(150_001), wide))
        assertEquals(GoalError.INSUFFICIENT_AVAILABLE_FUNDS,
            GoalRules.validateAllocation(goal, Money(0), Money(150_000), BigInteger.valueOf(149_999)))
        assertEquals(GoalError.INVALID_AMOUNT, GoalRules.validateAllocation(goal, Money(0), Money(0), wide))
    }

    @Test
    fun completionIsExplicitAndKeepsReleasesAvailable() {
        assertEquals(GoalError.TARGET_NOT_REACHED, GoalRules.validateCompletion(goal, Money(499_999)))
        assertNull(GoalRules.validateCompletion(goal, details.targetAmount))
        val completed = goal.copy(completedAt = now)
        assertEquals(GoalStatus.COMPLETED, completed.status)
        assertEquals(GoalError.COMPLETED, GoalRules.validateAllocation(completed, Money(250_000), Money(1), BigInteger.TEN))
        assertNull(GoalRules.validateRelease(completed, Money(500_000), Money(120_000)))
        assertNull(GoalRules.validateRelease(completed, Money(500_000), Money(500_000)))
        assertEquals(GoalError.RELEASE_EXCEEDS_RESERVED, GoalRules.validateRelease(completed, Money(500_000), Money(500_001)))
        assertEquals(GoalError.HAS_RESERVED_FUNDS, GoalRules.validateArchive(completed, Money(1)))
        assertNull(GoalRules.validateArchive(completed, Money(0)))
    }

    @Test
    fun derivesReserveProgressAndRemainingInIntegerCents() {
        val moves = listOf(
            GoalMovement(UUID(0, 4), goal.id, GoalMovementType.ALLOCATE, Money(200_000), date, now),
            GoalMovement(UUID(0, 5), goal.id, GoalMovementType.RELEASE, Money(50_000), date, now),
        )
        val account = FinancialAccount(accountId, AccountDetails("Conta", AccountType.CHECKING, Money(500_000), date), now, now, archivedAt = null)
        val summary = GoalRules.summarize(goal, account, GoalRules.reserved(moves))
        assertEquals(Money(150_000), summary.reservedAmount)
        assertEquals(Money(350_000), summary.remainingAmount)
        assertEquals(3000, summary.progressBasisPoints)
    }

    @Test
    fun reserveDoesNotReduceAssetsAndExcludedAccountsDoNotReduceAvailableTwice() {
        val main = AccountBalanceInput(accountId, Money(500_000), date, true, emptyList())
        val includedReserve = ReserveBalanceCalculator.calculate(listOf(main), mapOf(accountId to BigInteger.valueOf(150_000)))
        assertEquals(Exact(Money(500_000)), includedReserve.totalBalance)
        assertEquals(Exact(Money(150_000)), includedReserve.totalReserved)
        assertEquals(Exact(Money(350_000)), includedReserve.availableBalance)
        val excluded = AccountBalanceInput(otherId, Money(1_000_000), date, false, emptyList())
        val excludedReserve = ReserveBalanceCalculator.calculate(listOf(main, excluded), mapOf(otherId to BigInteger.valueOf(400_000)))
        assertEquals(Exact(Money(1_500_000)), excludedReserve.totalBalance)
        assertEquals(Exact(Money(400_000)), excludedReserve.totalReserved)
        assertEquals(Exact(Money(500_000)), excludedReserve.availableBalance)
        assertEquals(Exact(Money(600_000)), excludedReserve.accounts.getValue(otherId).availableAmount)
    }

    @Test
    fun subsequentExpenseCanMakeAvailabilityNegativeWithoutChangingReserve() {
        val expense = BalanceMovement(TransactionDirection.EXPENSE, Money(200_000), date)
        val account = AccountBalanceInput(accountId, Money(500_000), date, true, listOf(expense))
        val snapshot = ReserveBalanceCalculator.calculate(listOf(account), mapOf(accountId to BigInteger.valueOf(400_000)))
        assertEquals(Exact(Money(300_000)), snapshot.totalBalance)
        assertEquals(Exact(Money(400_000)), snapshot.totalReserved)
        assertEquals(Exact(Money(-100_000)), snapshot.availableBalance)
    }
}
