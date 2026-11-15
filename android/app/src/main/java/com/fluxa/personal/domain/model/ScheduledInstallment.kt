package com.fluxa.personal.domain.model

import com.fluxa.personal.core.money.Money
import java.time.LocalDate

data class ScheduledInstallment internal constructor(
    val number: Int,
    val totalCount: Int,
    val amount: Money,
    val dueDate: LocalDate,
)
