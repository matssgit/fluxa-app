package com.fluxa.personal.core.date

import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Test

class FinancialCalendarTest {
    @Test
    fun usesCurrentDeviceZoneAtMidnightBoundary() {
        val clock = Clock.fixed(Instant.parse("2026-11-01T01:30:00Z"), ZoneOffset.UTC)
        var zone = ZoneId.of("America/Sao_Paulo")
        val calendar = FinancialCalendar(clock) { zone }

        assertEquals(LocalDate.of(2026, 10, 31), calendar.today())
        zone = ZoneId.of("Europe/Lisbon")
        assertEquals(LocalDate.of(2026, 11, 1), calendar.today())
    }
}
