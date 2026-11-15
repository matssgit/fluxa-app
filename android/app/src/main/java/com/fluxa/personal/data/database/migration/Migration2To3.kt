package com.fluxa.personal.data.database.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.fluxa.personal.data.database.installCategoryKindGuard

internal val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS financial_transactions (
                id TEXT NOT NULL,
                accountId TEXT NOT NULL,
                categoryId TEXT NOT NULL,
                direction TEXT NOT NULL,
                amountCents INTEGER NOT NULL,
                occurredOn INTEGER NOT NULL,
                description TEXT,
                createdAt TEXT NOT NULL,
                updatedAt TEXT NOT NULL,
                cancelledAt TEXT,
                PRIMARY KEY(id),
                FOREIGN KEY(accountId) REFERENCES accounts(id) ON UPDATE RESTRICT ON DELETE RESTRICT,
                FOREIGN KEY(categoryId) REFERENCES categories(id) ON UPDATE RESTRICT ON DELETE RESTRICT
            )
            """.trimIndent(),
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS index_financial_transactions_accountId_occurredOn ON financial_transactions (accountId, occurredOn)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_financial_transactions_categoryId_occurredOn ON financial_transactions (categoryId, occurredOn)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_financial_transactions_occurredOn ON financial_transactions (occurredOn)")
        installCategoryKindGuard(db)
    }
}
