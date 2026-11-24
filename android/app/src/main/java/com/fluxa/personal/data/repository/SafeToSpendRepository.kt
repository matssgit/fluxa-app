package com.fluxa.personal.data.repository

import androidx.room.invalidationTrackerFlow
import androidx.room.withTransaction
import androidx.sqlite.db.SimpleSQLiteQuery
import com.fluxa.personal.core.date.FinancialCalendar
import com.fluxa.personal.core.money.Money
import com.fluxa.personal.data.database.FluxaDatabase
import com.fluxa.personal.data.database.dao.SubscriptionScheduleRow
import com.fluxa.personal.data.database.mapper.toDomain
import com.fluxa.personal.domain.model.ProjectedCashEvent
import com.fluxa.personal.domain.model.ProjectedCashEventType
import com.fluxa.personal.domain.model.ProjectedCashSource
import com.fluxa.personal.domain.model.SafeToSpendError
import com.fluxa.personal.domain.model.SafeToSpendState
import com.fluxa.personal.domain.model.SubscriptionOccurrence
import com.fluxa.personal.domain.model.SubscriptionSource
import com.fluxa.personal.domain.rules.CalculatedBalance
import com.fluxa.personal.domain.rules.SafeToSpendCalculator
import com.fluxa.personal.domain.rules.SubscriptionCalendar
import java.time.Clock
import java.time.LocalDate
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

internal class SafeToSpendRepository(
    private val database: FluxaDatabase,
    private val goals: GoalRepository,
    clock: Clock = Clock.systemUTC(),
    private val calendar: FinancialCalendar = FinancialCalendar(clock),
) {
    private val dao = database.safeToSpendDao()

    fun observeSafeToSpend(horizonDays: Int = 30): Flow<SafeToSpendState> {
        require(horizonDays > 0)
        return database.invalidationTrackerFlow(
            "accounts", "financial_transactions", "goals", "goal_movements",
            "subscriptions", "subscription_terms", "subscription_resolutions",
            "credit_cards", "credit_purchases", "credit_installments",
        ).map { read(calendar.today(), horizonDays) }.distinctUntilChanged()
    }

    // Shared transactional read contract: analytics must not rebuild the obligation/calendar algorithm.
    internal suspend fun read(today: LocalDate, horizonDays: Int): SafeToSpendState = database.withTransaction {
        val end = today.plusDays(horizonDays.toLong())
        val balances = goals.readReserveBalances()
        val available = balances.availableBalance
        if (available !is CalculatedBalance.Exact) {
            return@withTransaction SafeToSpendState.Unavailable(SafeToSpendError.OVERFLOW)
        }
        val events = dao.readPendingInstallments(end.toEpochDay()).map { row ->
            ProjectedCashEvent(
                LocalDate.ofEpochDay(row.dueDate), ProjectedCashEventType.OBLIGATION, Money(row.amountCents),
                ProjectedCashSource.Installment(UUID.fromString(row.installmentId), UUID.fromString(row.purchaseId), UUID.fromString(row.cardId)),
            )
        }.toMutableList()
        for (schedule in dao.readSubscriptionSchedules(end.toEpochDay())) {
            for (occurrence in openOccurrences(schedule, today, end)) {
                val source = occurrence.details.source
                if (source is SubscriptionSource.Account && balances.accounts[source.id]?.includedInAvailable != true) continue
                events.add(ProjectedCashEvent(
                    occurrence.key.occurrenceDate, ProjectedCashEventType.OBLIGATION, occurrence.details.amount,
                    ProjectedCashSource.Subscription(occurrence.key, source),
                ))
            }
        }
        SafeToSpendCalculator.calculate(available.amount, events, today, horizonDays)
    }

    private suspend fun openOccurrences(
        schedule: SubscriptionScheduleRow,
        today: LocalDate,
        end: LocalDate,
    ): List<SubscriptionOccurrence> {
        val subscription = schedule.subscription.toDomain()
        val id = subscription.id.toString()
        val reopened = dao.readReopenedOccurrences(id, end.toEpochDay()).map { it.toDomain() }
        val terms = schedule.terms.map { it.toDomain() }
        val open = mutableListOf<SubscriptionOccurrence>()
        var start = subscription.anchorDate
        while (start < end) {
            val windowEnd = minOf(end, start.plusMonths(24))
            val candidates = SubscriptionCalendar.agenda(subscription, terms, reopened, start, windowEnd, today)
            if (candidates.isNotEmpty()) {
                val placeholders = List(candidates.size) { "(?)" }.joinToString()
                val query = SimpleSQLiteQuery(
                    """
                    WITH candidates(occurrenceDate) AS (VALUES $placeholders)
                    SELECT c.occurrenceDate FROM candidates c
                    WHERE NOT EXISTS (
                        SELECT 1 FROM subscription_resolutions r
                        WHERE r.subscriptionId = ? AND r.occurrenceDate = c.occurrenceDate AND r.status <> 'PENDING'
                    )
                    """.trimIndent(),
                    (candidates.map { it.key.occurrenceDate.toEpochDay() } + id).toTypedArray(),
                )
                val openDates = dao.readOpenDates(query).map { it.occurrenceDate }.toHashSet()
                open.addAll(candidates.filter { it.key.occurrenceDate.toEpochDay() in openDates })
            }
            start = windowEnd
        }
        return open
    }
}
