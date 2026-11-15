package com.fluxa.personal.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.fluxa.personal.BuildConfig
import com.fluxa.personal.data.database.entity.LocalSchemaMarker

@Database(entities = [LocalSchemaMarker::class], version = 1, exportSchema = true)
internal abstract class FluxaDatabase : RoomDatabase() {
    companion object {
        fun create(context: Context): FluxaDatabase {
            val builder = Room.databaseBuilder(context, FluxaDatabase::class.java, "fluxa.db")
                .addCallback(object : Callback() {
                    override fun onOpen(db: SupportSQLiteDatabase) {
                        db.setForeignKeyConstraintsEnabled(true)
                    }
                })

            // Remove before v1 is frozen. Release always requires a migration path.
            if (BuildConfig.DEBUG) builder.fallbackToDestructiveMigration()
            return builder.build()
        }
    }
}
