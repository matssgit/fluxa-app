package com.fluxa.personal

import android.app.Application
import com.fluxa.personal.core.preferences.LocalPreferences
import com.fluxa.personal.data.database.FluxaDatabase
import com.fluxa.personal.data.repository.AccountRepository
import com.fluxa.personal.data.repository.CategoryRepository

internal class FluxaApplication : Application() {
    val database by lazy { FluxaDatabase.create(this) }
    val preferences by lazy { LocalPreferences(this) }
    val accounts by lazy { AccountRepository(database.accountDao()) }
    val categories by lazy { CategoryRepository(database.categoryDao()) }
}
