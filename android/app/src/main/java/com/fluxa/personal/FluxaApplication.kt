package com.fluxa.personal

import android.app.Application
import com.fluxa.personal.core.preferences.LocalPreferences
import com.fluxa.personal.data.database.FluxaDatabase

internal class FluxaApplication : Application() {
    val database by lazy { FluxaDatabase.create(this) }
    val preferences by lazy { LocalPreferences(this) }
}
