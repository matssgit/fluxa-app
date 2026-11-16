package com.fluxa.personal.data.database.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

internal val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            ALTER TABLE financial_transactions ADD COLUMN installmentId TEXT
            REFERENCES credit_installments(id) ON UPDATE RESTRICT ON DELETE RESTRICT
            """.trimIndent(),
        )
        db.execSQL(
            "CREATE UNIQUE INDEX index_financial_transactions_installmentId ON financial_transactions(installmentId)",
        )
    }
}
