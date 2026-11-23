package com.fluxa.personal.backup

import java.io.OutputStream

/** A newly selected output document. Financial code does not depend on Android URI APIs. */
internal interface BackupDestination {
    fun open(): OutputStream
    fun delete(): Boolean
}

internal enum class BackupCleanup { NOT_NEEDED, DELETED, FAILED }

/** Owns closing the stream once. A completed document is retained even if restore subsequently fails. */
internal class PreventiveBackupOutput(private val destination: BackupDestination) {
    var created: Boolean = false
        private set
    private var cleanupResult: BackupCleanup? = null

    fun write(write: (OutputStream) -> Unit) {
        check(!created && cleanupResult == null) { "Preventive output has already been finalized." }
        destination.open().use {
            write(it)
            it.flush()
        }
        created = true
    }

    fun cleanupIncomplete(): BackupCleanup {
        if (created) return BackupCleanup.NOT_NEEDED
        cleanupResult?.let { return it }
        val deleted = try { destination.delete() } catch (_: Exception) { false }
        return (if (deleted) BackupCleanup.DELETED else BackupCleanup.FAILED).also { cleanupResult = it }
    }
}
