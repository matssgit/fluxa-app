package com.fluxa.personal.data.database

import androidx.sqlite.db.SupportSQLiteDatabase

internal const val CATEGORY_KIND_IN_USE = "CATEGORY_KIND_IN_USE"

internal fun installCategoryKindGuard(db: SupportSQLiteDatabase, includePurchases: Boolean = false) {
    val purchaseCheck = if (includePurchases) {
        "OR EXISTS (SELECT 1 FROM credit_purchases WHERE categoryId = OLD.id)"
    } else {
        ""
    }
    db.execSQL(
        """
        CREATE TRIGGER IF NOT EXISTS category_kind_guard
        BEFORE UPDATE OF kind ON categories
        WHEN OLD.kind <> NEW.kind AND (
            EXISTS (SELECT 1 FROM financial_transactions WHERE categoryId = OLD.id)
            $purchaseCheck
        )
        BEGIN
            SELECT RAISE(ABORT, '$CATEGORY_KIND_IN_USE');
        END
        """.trimIndent(),
    )
}
