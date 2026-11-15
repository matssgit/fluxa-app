package com.fluxa.personal.ui.common

import kotlin.coroutines.cancellation.CancellationException

/** Como [runCatching], mas deixa o cancelamento de corrotina se propagar. */
internal inline fun <T> runCatchingNonCancellation(block: () -> T): Result<T> = try {
    Result.success(block())
} catch (cancellation: CancellationException) {
    throw cancellation
} catch (error: Exception) {
    Result.failure(error)
}
