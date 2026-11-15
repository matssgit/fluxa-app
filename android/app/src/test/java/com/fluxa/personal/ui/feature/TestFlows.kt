package com.fluxa.personal.ui.feature

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

internal suspend fun <T> Flow<T>.firstValue(): T = first()
