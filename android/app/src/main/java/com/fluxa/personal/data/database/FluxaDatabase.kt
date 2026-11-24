package com.fluxa.personal.data.database

import android.content.Context
import androidx.room.Database
import com.fluxa.personal.data.database.dao.SafeToSpendDao
import com.fluxa.personal.data.database.dao.GoalDao
import com.fluxa.personal.data.database.entity.GoalEntity
import com.fluxa.personal.data.database.entity.GoalMovementEntity
import com.fluxa.personal.data.database.migration.MIGRATION_6_7
import com.fluxa.personal.data.database.migration.installGoalGuards
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.fluxa.personal.data.database.dao.AccountDao
import com.fluxa.personal.data.database.dao.CategoryDao
import com.fluxa.personal.data.database.dao.CreditDao
import com.fluxa.personal.data.database.dao.TransactionDao
import com.fluxa.personal.data.database.dao.SubscriptionDao
import com.fluxa.personal.data.database.entity.AccountEntity
import com.fluxa.personal.data.database.entity.CategoryEntity
import com.fluxa.personal.data.database.entity.CreditCardEntity
import com.fluxa.personal.data.database.entity.InstallmentEntity
import com.fluxa.personal.data.database.entity.LocalSchemaMarker
import com.fluxa.personal.data.database.entity.PurchaseEntity
import com.fluxa.personal.data.database.entity.TransactionEntity
import com.fluxa.personal.data.database.entity.SubscriptionEntity
import com.fluxa.personal.data.database.entity.SubscriptionTermsEntity
import com.fluxa.personal.data.database.entity.SubscriptionResolutionEntity
import com.fluxa.personal.data.database.migration.MIGRATION_1_2
import com.fluxa.personal.data.database.migration.MIGRATION_2_3
import com.fluxa.personal.data.database.migration.MIGRATION_3_4
import com.fluxa.personal.data.database.migration.MIGRATION_4_5
import com.fluxa.personal.data.database.migration.MIGRATION_5_6

@Database(
    entities = [
        LocalSchemaMarker::class, AccountEntity::class, CategoryEntity::class, TransactionEntity::class,
        CreditCardEntity::class, PurchaseEntity::class, InstallmentEntity::class,
        SubscriptionEntity::class, SubscriptionTermsEntity::class, SubscriptionResolutionEntity::class,
        GoalEntity::class, GoalMovementEntity::class,
    ],
    version = 7,
    exportSchema = true,
)
internal abstract class FluxaDatabase : RoomDatabase() {
    abstract fun accountDao(): AccountDao
    abstract fun categoryDao(): CategoryDao
    abstract fun transactionDao(): TransactionDao
    abstract fun creditDao(): CreditDao
    abstract fun subscriptionDao(): SubscriptionDao
    abstract fun goalDao(): GoalDao
    abstract fun analyticsDao(): com.fluxa.personal.data.database.dao.FinancialAnalyticsDao
    abstract fun safeToSpendDao(): SafeToSpendDao

    companion object {
        fun create(context: Context, name: String = "fluxa.db"): FluxaDatabase =
            Room.databaseBuilder(context, FluxaDatabase::class.java, name)
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7)
                .addCallback(object : Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        installCategoryKindGuard(db, includePurchases = true, includeSubscriptions = true)
                        installSubscriptionGuards(db)
                        installGoalGuards(db)
                    }

                    override fun onOpen(db: SupportSQLiteDatabase) {
                        db.setForeignKeyConstraintsEnabled(true)
                    }
                })
                .build()
    }
}
