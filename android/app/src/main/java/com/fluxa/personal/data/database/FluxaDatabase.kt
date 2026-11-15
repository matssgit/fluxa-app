package com.fluxa.personal.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.fluxa.personal.data.database.dao.AccountDao
import com.fluxa.personal.data.database.dao.CategoryDao
import com.fluxa.personal.data.database.entity.AccountEntity
import com.fluxa.personal.data.database.entity.CategoryEntity
import com.fluxa.personal.data.database.entity.LocalSchemaMarker
import com.fluxa.personal.data.database.migration.MIGRATION_1_2

@Database(
    entities = [LocalSchemaMarker::class, AccountEntity::class, CategoryEntity::class],
    version = 2,
    exportSchema = true,
)
internal abstract class FluxaDatabase : RoomDatabase() {
    abstract fun accountDao(): AccountDao
    abstract fun categoryDao(): CategoryDao

    companion object {
        fun create(context: Context, name: String = "fluxa.db"): FluxaDatabase =
            Room.databaseBuilder(context, FluxaDatabase::class.java, name)
                .addMigrations(MIGRATION_1_2)
                .addCallback(object : Callback() {
                    override fun onOpen(db: SupportSQLiteDatabase) {
                        db.setForeignKeyConstraintsEnabled(true)
                    }
                })
                .build()
    }
}
