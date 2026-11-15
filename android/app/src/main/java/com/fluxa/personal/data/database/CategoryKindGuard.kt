package com.fluxa.personal.data.database

import androidx.sqlite.db.SupportSQLiteDatabase

internal const val CATEGORY_KIND_IN_USE = "CATEGORY_KIND_IN_USE"

internal fun installCategoryKindGuard(db: SupportSQLiteDatabase) {
    db.execSQL(
        """
        CREATE TRIGGER IF NOT EXISTS category_kind_guard
        BEFORE UPDATE OF kind ON categories
        WHEN OLD.kind <> NEW.kind AND EXISTS (
            SELECT 1 FROM financial_transactions WHERE categoryId = OLD.id
        )
        BEGIN
            SELECT RAISE(ABORT, '$CATEGORY_KIND_IN_USE');
        END
        """.trimIndent(),
    )
}
