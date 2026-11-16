package com.fluxa.personal.data.database

import androidx.sqlite.db.SupportSQLiteDatabase

internal fun installSubscriptionGuards(db: SupportSQLiteDatabase) {
    for (table in listOf("subscriptions", "subscription_terms", "subscription_resolutions")) {
        val changes = if (table == "subscriptions") listOf("INSERT", "UPDATE") else listOf("INSERT")
        for (change in changes) {
            db.execSQL(
                """
                CREATE TRIGGER ${table}_source_${change.lowercase()}
                BEFORE $change ON $table
                WHEN (NEW.accountId IS NULL) = (NEW.cardId IS NULL)
                BEGIN SELECT RAISE(ABORT, 'INVALID_SUBSCRIPTION_SOURCE'); END
                """.trimIndent(),
            )
        }
    }
    for ((table, other) in listOf(
        "financial_transactions" to "credit_purchases", "credit_purchases" to "financial_transactions",
    )) {
        db.execSQL(
            """
            CREATE TRIGGER ${table}_subscription_insert BEFORE INSERT ON $table
            WHEN (NEW.subscriptionId IS NULL) <> (NEW.occurrenceDate IS NULL) OR (
                NEW.subscriptionId IS NOT NULL AND (
                    EXISTS (SELECT 1 FROM $other
                            WHERE subscriptionId = NEW.subscriptionId AND occurrenceDate = NEW.occurrenceDate)
                    OR EXISTS (SELECT 1 FROM subscription_resolutions
                               WHERE subscriptionId = NEW.subscriptionId AND occurrenceDate = NEW.occurrenceDate)
                )
            )
            BEGIN SELECT RAISE(ABORT, 'INVALID_OR_RESOLVED_SUBSCRIPTION_OCCURRENCE'); END
            """.trimIndent(),
        )
        db.execSQL(
            """
            CREATE TRIGGER ${table}_subscription_identity BEFORE UPDATE OF subscriptionId, occurrenceDate ON $table
            WHEN OLD.subscriptionId IS NOT NEW.subscriptionId OR OLD.occurrenceDate IS NOT NEW.occurrenceDate
            BEGIN SELECT RAISE(ABORT, 'IMMUTABLE_SUBSCRIPTION_OCCURRENCE'); END
            """.trimIndent(),
        )
    }
}
