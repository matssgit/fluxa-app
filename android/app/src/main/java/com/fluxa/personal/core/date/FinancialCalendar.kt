package com.fluxa.personal.core.date

import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId

internal class FinancialCalendar(
    private val clock: Clock = Clock.systemUTC(),
    private val zone: () -> ZoneId = ZoneId::systemDefault,
) {
    fun today(): LocalDate = LocalDate.now(clock.withZone(zone()))
}
