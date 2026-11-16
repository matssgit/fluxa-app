package com.fluxa.personal.ui.common

import com.fluxa.personal.core.date.FinancialCalendar
import java.time.Duration
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow

/**
 * Dia financeiro observável. Emite o dia atual e acorda só perto da meia-noite seguinte (nunca dorme mais de
 * [MAX_SLEEP], para absorver mudanças manuais de relógio ou fuso). Não é polling: em um dia típico acorda ~24 vezes
 * no pior caso e só emite quando a data muda. Os ViewModels usam a mudança para **recriar** as observações dos
 * repositórios, que calculam "hoje" dentro do `map` — como recomenda o handoff 5-A.
 */
internal fun financialDays(
    calendar: FinancialCalendar = FinancialCalendar(),
    zone: () -> ZoneId = ZoneId::systemDefault,
): Flow<LocalDate> = flow {
    while (true) {
        val today = calendar.today()
        emit(today)
        val nextMidnight = today.plusDays(1).atStartOfDay(zone())
        val untilMidnight = Duration.between(ZonedDateTime.now(zone()), nextMidnight).plusSeconds(1)
        delay(untilMidnight.coerceIn(Duration.ofSeconds(1), MAX_SLEEP).toMillis())
    }
}.distinctUntilChanged()

private val MAX_SLEEP: Duration = Duration.ofHours(1)
