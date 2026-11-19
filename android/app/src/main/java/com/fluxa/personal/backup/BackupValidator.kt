package com.fluxa.personal.backup

import com.google.gson.JsonObject
import com.fluxa.personal.core.date.BillingDay
import com.fluxa.personal.domain.model.OccurrenceFrequency
import com.fluxa.personal.domain.model.OccurrenceSchedule
import com.fluxa.personal.domain.rules.OccurrenceCalendar
import java.math.BigInteger
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

internal object BackupValidator {
    fun validateData(data: BackupDataV1) {
        if (data.tables.keys != BackupFormatV1.tables.map { it.name }.toSet()) invalid()
        if (data.settings.theme !in setOf("SYSTEM", "LIGHT", "DARK")) invalid()
        if (data.tables.values.sumOf { it.size.toLong() } > 100_000) throw BackupException(BackupError.TOO_LARGE)
        val rows = data.tables.mapValues { (_, records) -> records.map { it.fields } }
        val indexes = BackupFormatV1.tables.associate { table ->
            val tableRows = rows.getValue(table.name)
            val keys = HashSet<List<String>>()
            for (row in tableRows) {
                requireKeys(row, table.fields.map { it.name }.toSet())
                for (field in table.fields) validateField(row, field)
                if (!keys.add(table.keys.map { row.get(it).asString })) invalid()
                validateAmounts(row)
            }
            table.name to tableRows.associateBy { it.get("id")?.asString }
        }
        for (table in BackupFormatV1.tables) {
            for (row in rows.getValue(table.name)) {
                for (reference in table.references) {
                    val id = row.optional(reference.field) ?: continue
                    if (indexes.getValue(reference.table)[id] == null) relation()
                }
            }
        }
        val categories = indexes.getValue("categories")
        for (row in rows.getValue("financial_transactions")) {
            if (categories.getValue(row.text("categoryId")).text("kind") != row.text("direction")) relation()
        }
        for (table in listOf("credit_purchases", "subscriptions", "subscription_terms")) {
            for (row in rows.getValue(table)) {
                if (categories[row.text("categoryId")]?.text("kind") != "EXPENSE") relation()
            }
        }
        val accounts = indexes.getValue("accounts")
        val cards = indexes.getValue("credit_cards")
        for (table in listOf("subscriptions", "subscription_terms", "subscription_resolutions")) {
            for (row in rows.getValue(table)) {
                if (row.text("categoryId") !in categories) relation()
                val account = row.optional("accountId")
                val card = row.optional("cardId")
                if ((account == null) == (card == null)) relation()
                if (account != null && account !in accounts || card != null && card !in cards) relation()
            }
        }
        for (subscription in rows.getValue("subscriptions")) {
            if (subscription.long("billingDay") !in 1..31) invalid()
            if (subscription.text("frequency") == "YEARLY" &&
                LocalDate.parse(subscription.text("anchorDate")).dayOfMonth.toLong() != subscription.long("billingDay")) invalid()
            when (subscription.text("status")) {
                "ACTIVE" -> if (subscription.optional("cancelledAt") != null || subscription.optional("pausedAt") != null) invalid()
                "PAUSED" -> if (subscription.optional("pausedAt") == null || subscription.optional("cancelledAt") != null) invalid()
                "CANCELLED" -> if (subscription.optional("cancelledAt") == null) invalid()
            }
        }
        for (card in rows.getValue("credit_cards")) {
            if (card.long("closingDay") !in 1..31 || card.long("dueDay") !in 1..31) invalid()
        }
        validateCredit(rows, indexes)
        validateSubscriptions(rows)
        validateGoals(rows)
    }

    private fun validateField(row: JsonObject, field: FieldV1) {
        val value = row.get(field.name)
        if (value.isJsonNull) {
            if (!field.nullable) invalid()
            return
        }
        when (field.type) {
            BackupFieldType.LONG -> row.long(field.name)
            BackupFieldType.BOOLEAN -> row.bool(field.name)
            BackupFieldType.TEXT -> row.text(field.name)
            BackupFieldType.DATE -> {
                val text = row.text(field.name)
                if (LocalDate.parse(text).toString() != text) invalid()
            }
            BackupFieldType.INSTANT -> {
                val text = row.text(field.name)
                if (Instant.parse(text).toString() != text) invalid()
            }
            BackupFieldType.UUID -> {
                val text = row.text(field.name)
                if (UUID.fromString(text).toString() != text) invalid()
            }
            BackupFieldType.ENUM -> if (row.text(field.name) !in field.options) invalid()
        }
    }

    private fun validateAmounts(row: JsonObject) {
        for (name in listOf("name", "title")) if (row.has(name) && row.text(name).isBlank()) invalid()
        for (name in listOf("amountCents", "totalCents", "targetAmountCents")) {
            if (row.has(name) && row.long(name) <= 0) invalid()
        }
        if (row.has("totalLimitCents") && row.long("totalLimitCents") < 0) invalid()
        for (name in listOf("number", "installmentCount")) {
            if (row.has(name) && row.long(name) !in 1..Int.MAX_VALUE.toLong()) invalid()
        }
    }

    private fun validateCredit(rows: Map<String, List<JsonObject>>, indexes: Map<String, Map<String?, JsonObject>>) {
        val parts = rows.getValue("credit_installments").groupBy { it.text("purchaseId") }
        val payments = rows.getValue("financial_transactions").filter { it.optional("installmentId") != null }
            .groupBy { it.text("installmentId") }
        for (purchase in rows.getValue("credit_purchases")) {
            val installments = parts[purchase.text("id")].orEmpty()
            if (installments.size.toLong() != purchase.long("installmentCount")) relation()
            if (installments.map { it.long("number") }.toSet() != (1L..installments.size.toLong()).toSet()) relation()
            if (installments.fold(BigInteger.ZERO) { sum, part -> sum + part.wide("amountCents") } != purchase.wide("totalCents")) relation()
            if (installments.singleOrNull { it.long("number") == 1L }?.text("dueDate") != purchase.text("firstDueDate")) relation()
            for (part in installments) {
                val payment = payments[part.text("id")].orEmpty()
                if (part.text("status") == "PAID") {
                    val tx = payment.singleOrNull() ?: relation()
                    if (tx.optional("cancelledAt") != null || tx.text("direction") != "EXPENSE" ||
                        tx.long("amountCents") != part.long("amountCents") || tx.text("categoryId") != purchase.text("categoryId")) relation()
                } else if (payment.isNotEmpty()) relation()
                val cancelled = purchase.optional("cancelledAt") != null
                if (cancelled != (part.text("status") == "CANCELLED")) relation()
            }
        }
        if (payments.keys.any { it !in indexes.getValue("credit_installments") }) relation()
    }

    private fun validateSubscriptions(rows: Map<String, List<JsonObject>>) {
        val subscriptions = rows.getValue("subscriptions").associateBy { it.text("id") }
        val outcomes = rows.getValue("subscription_resolutions").associateBy { it.occurrenceKey() }
        for (outcome in outcomes.values) {
            val subscription = subscriptions[outcome.text("subscriptionId")] ?: relation()
            val date = LocalDate.parse(outcome.text("occurrenceDate"))
            if (date == LocalDate.MAX) relation()
            val schedule = OccurrenceSchedule(
                subscription.text("id"), LocalDate.parse(subscription.text("anchorDate")),
                OccurrenceFrequency.valueOf(subscription.text("frequency")), BillingDay(subscription.long("billingDay").toInt()),
            )
            if (OccurrenceCalendar.between(schedule, date, date.plusDays(1)).singleOrNull()?.dueDate != date) relation()
        }
        val finance = (rows.getValue("financial_transactions") + rows.getValue("credit_purchases"))
        val linked = finance.filter {
            if ((it.optional("subscriptionId") == null) != (it.optional("occurrenceDate") == null)) relation()
            it.optional("subscriptionId") != null
        }.groupBy { it.occurrenceKey() }
        if (linked.values.any { it.size != 1 }) relation()
        for ((key, effects) in linked) {
            val row = effects.single()
            val outcome = outcomes[key] ?: relation()
            val purchase = row.has("cardId")
            if (outcome.text("status") != if (purchase) "CHARGED" else "PAID") relation()
            if (outcome.optional("resultId") != row.text("id")) relation()
            if (purchase) {
                if (outcome.optional("cardId") != row.text("cardId") || row.long("installmentCount") != 1L ||
                    outcome.long("amountCents") != row.long("totalCents")) relation()
            } else {
                if (row.optional("installmentId") != null || row.optional("cancelledAt") != null ||
                    row.text("direction") != "EXPENSE" || outcome.optional("accountId") != row.text("accountId") ||
                    outcome.long("amountCents") != row.long("amountCents")) relation()
            }
            if (outcome.text("categoryId") != row.text("categoryId")) relation()
        }
        for ((key, outcome) in outcomes) {
            when (outcome.text("status")) {
                "PAID", "CHARGED" -> if (linked[key] == null || outcome.optional("resultId") == null) relation()
                "PENDING", "SKIPPED" -> if (linked[key] != null || outcome.optional("resultId") != null) relation()
            }
        }
        val terms = rows.getValue("subscription_terms").groupBy { it.text("subscriptionId") }
        for (subscription in rows.getValue("subscriptions")) {
            if (terms[subscription.text("id")].isNullOrEmpty()) relation()
        }
    }

    private fun validateGoals(rows: Map<String, List<JsonObject>>) {
        val movements = rows.getValue("goal_movements").groupBy { it.text("goalId") }
        for (goal in rows.getValue("goals")) {
            val reserved = movements[goal.text("id")].orEmpty().fold(BigInteger.ZERO) { sum, movement ->
                if (movement.text("type") == "ALLOCATE") sum + movement.wide("amountCents")
                else sum - movement.wide("amountCents")
            }
            if (reserved.signum() < 0 || reserved > goal.wide("targetAmountCents")) relation()
            if (goal.optional("archivedAt") != null && reserved.signum() != 0) relation()
        }
    }

    private fun JsonObject.wide(name: String): BigInteger = BigInteger.valueOf(long(name))
    private fun JsonObject.occurrenceKey() = text("subscriptionId") to text("occurrenceDate")
    private fun invalid(): Nothing = throw BackupException(BackupError.INVALID)
    private fun relation(): Nothing = throw BackupException(BackupError.RELATION_INVALID)
}

internal fun JsonObject.optional(name: String): String? =
    get(name)?.takeUnless { it.isJsonNull }?.asString
