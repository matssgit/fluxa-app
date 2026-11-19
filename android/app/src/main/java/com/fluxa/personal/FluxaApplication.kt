package com.fluxa.personal

import android.app.Application
import com.fluxa.personal.backup.FluxaBackupService
import com.fluxa.personal.backup.BackupDocuments
import com.fluxa.personal.core.preferences.LocalPreferences
import com.fluxa.personal.data.database.FluxaDatabase
import com.fluxa.personal.data.repository.AccountRepository
import com.fluxa.personal.data.repository.GoalRepository
import com.fluxa.personal.data.repository.SafeToSpendRepository
import com.fluxa.personal.data.repository.CategoryRepository
import com.fluxa.personal.data.repository.CreditRepository
import com.fluxa.personal.data.repository.TransactionRepository
import com.fluxa.personal.data.repository.SubscriptionRepository

internal class FluxaApplication : Application() {
    val database by lazy { FluxaDatabase.create(this) }
    val preferences by lazy { LocalPreferences(this) }
    val backups by lazy { FluxaBackupService(database, preferences, BuildConfig.VERSION_NAME) }
    val backupDocuments by lazy { BackupDocuments(contentResolver, backups) }
    val accounts by lazy { AccountRepository(database.accountDao()) }
    val categories by lazy { CategoryRepository(database.categoryDao()) }
    val transactions by lazy { TransactionRepository(database) }
    val credit by lazy { CreditRepository(database) }
    val subscriptions by lazy { SubscriptionRepository(database, credit) }
    val goals by lazy { GoalRepository(database) }
    val safeToSpend by lazy { SafeToSpendRepository(database, goals) }
}
