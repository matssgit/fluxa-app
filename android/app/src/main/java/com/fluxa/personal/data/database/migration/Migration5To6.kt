package com.fluxa.personal.data.database.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.fluxa.personal.data.database.installCategoryKindGuard
import com.fluxa.personal.data.database.installSubscriptionGuards

internal val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE subscriptions (
                id TEXT NOT NULL PRIMARY KEY, name TEXT NOT NULL, amountCents INTEGER NOT NULL,
                categoryId TEXT NOT NULL REFERENCES categories(id) ON DELETE RESTRICT,
                accountId TEXT REFERENCES accounts(id) ON DELETE RESTRICT,
                cardId TEXT REFERENCES credit_cards(id) ON DELETE RESTRICT,
                frequency TEXT NOT NULL, anchorDate INTEGER NOT NULL, billingDay INTEGER NOT NULL,
                status TEXT NOT NULL, createdAt TEXT NOT NULL, updatedAt TEXT NOT NULL,
                pausedAt TEXT, cancelledAt TEXT
            )
            """.trimIndent(),
        )
        for (column in listOf("categoryId", "accountId", "cardId")) {
            db.execSQL("CREATE INDEX index_subscriptions_${column} ON subscriptions($column)")
        }
        db.execSQL(
            """
            CREATE TABLE subscription_terms (
                subscriptionId TEXT NOT NULL REFERENCES subscriptions(id) ON DELETE RESTRICT,
                effectiveOn INTEGER NOT NULL, name TEXT NOT NULL, amountCents INTEGER NOT NULL,
                categoryId TEXT NOT NULL, accountId TEXT, cardId TEXT, enabled INTEGER NOT NULL,
                PRIMARY KEY(subscriptionId, effectiveOn)
            )
            """.trimIndent(),
        )
        db.execSQL("CREATE INDEX index_subscription_terms_categoryId ON subscription_terms(categoryId)")
        db.execSQL(
            """
            CREATE TABLE subscription_resolutions (
                subscriptionId TEXT NOT NULL REFERENCES subscriptions(id) ON DELETE RESTRICT,
                occurrenceDate INTEGER NOT NULL, name TEXT NOT NULL, amountCents INTEGER NOT NULL,
                categoryId TEXT NOT NULL, accountId TEXT, cardId TEXT, status TEXT NOT NULL, resultId TEXT,
                PRIMARY KEY(subscriptionId, occurrenceDate)
            )
            """.trimIndent(),
        )
        for (table in listOf("financial_transactions", "credit_purchases")) {
            db.execSQL("ALTER TABLE $table ADD COLUMN subscriptionId TEXT REFERENCES subscriptions(id) ON DELETE RESTRICT")
            db.execSQL("ALTER TABLE $table ADD COLUMN occurrenceDate INTEGER")
            db.execSQL("CREATE UNIQUE INDEX index_${table}_subscriptionId_occurrenceDate ON $table(subscriptionId, occurrenceDate)")
        }
        db.execSQL("DROP TRIGGER IF EXISTS category_kind_guard")
        installCategoryKindGuard(db, includePurchases = true, includeSubscriptions = true)
        installSubscriptionGuards(db)
    }
}
