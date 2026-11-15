package com.fluxa.personal.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.fluxa.personal.data.database.dao.AccountDao
import com.fluxa.personal.data.database.dao.CategoryDao
import com.fluxa.personal.data.database.dao.TransactionDao
import com.fluxa.personal.data.database.entity.AccountEntity
import com.fluxa.personal.data.database.entity.CategoryEntity
import com.fluxa.personal.data.database.entity.LocalSchemaMarker
import com.fluxa.personal.data.database.entity.TransactionEntity
import com.fluxa.personal.data.database.migration.MIGRATION_1_2
import com.fluxa.personal.data.database.migration.MIGRATION_2_3

@Database(
    entities = [LocalSchemaMarker::class, AccountEntity::class, CategoryEntity::class, TransactionEntity::class],
    version = 3,
    exportSchema = true,
)
internal abstract class FluxaDatabase : RoomDatabase() {
    abstract fun accountDao(): AccountDao
    abstract fun categoryDao(): CategoryDao
    abstract fun transactionDao(): TransactionDao

    companion object {
        fun create(context: Context, name: String = "fluxa.db"): FluxaDatabase =
            Room.databaseBuilder(context, FluxaDatabase::class.java, name)
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                .addCallback(object : Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        installCategoryKindGuard(db)
                    }

                    override fun onOpen(db: SupportSQLiteDatabase) {
                        db.setForeignKeyConstraintsEnabled(true)
                    }
                })
                .build()
    }
}
