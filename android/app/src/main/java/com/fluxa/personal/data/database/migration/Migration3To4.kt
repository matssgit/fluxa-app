package com.fluxa.personal.data.database.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.fluxa.personal.data.database.installCategoryKindGuard

internal val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS credit_cards (
                id TEXT NOT NULL PRIMARY KEY, name TEXT NOT NULL, totalLimitCents INTEGER NOT NULL,
                closingDay INTEGER NOT NULL, dueDay INTEGER NOT NULL, color TEXT, archivedAt TEXT,
                createdAt TEXT NOT NULL, updatedAt TEXT NOT NULL
            )
            """.trimIndent(),
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS credit_purchases (
                id TEXT NOT NULL PRIMARY KEY, cardId TEXT NOT NULL, categoryId TEXT NOT NULL,
                title TEXT NOT NULL, merchant TEXT, notes TEXT, totalCents INTEGER NOT NULL,
                installmentCount INTEGER NOT NULL, purchaseDate INTEGER NOT NULL, firstDueDate INTEGER NOT NULL,
                createdAt TEXT NOT NULL, updatedAt TEXT NOT NULL, cancelledAt TEXT,
                FOREIGN KEY (cardId) REFERENCES credit_cards(id) ON DELETE RESTRICT,
                FOREIGN KEY (categoryId) REFERENCES categories(id) ON DELETE RESTRICT
            )
            """.trimIndent(),
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS credit_installments (
                id TEXT NOT NULL PRIMARY KEY, purchaseId TEXT NOT NULL, number INTEGER NOT NULL,
                amountCents INTEGER NOT NULL, dueDate INTEGER NOT NULL, status TEXT NOT NULL,
                FOREIGN KEY (purchaseId) REFERENCES credit_purchases(id) ON DELETE RESTRICT
            )
            """.trimIndent(),
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS index_credit_purchases_cardId_purchaseDate ON credit_purchases(cardId, purchaseDate)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_credit_purchases_categoryId ON credit_purchases(categoryId)")
        db.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS index_credit_installments_purchaseId_number ON credit_installments(purchaseId, number)",
        )

        db.execSQL("DROP TRIGGER IF EXISTS category_kind_guard")
        installCategoryKindGuard(db, includePurchases = true)
    }
}
