package com.fluxa.personal.backup

import android.content.ContentResolver
import android.net.Uri
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** SAF adapters. Password buffers are consumed on every path, including failures opening a URI. */
internal class BackupDocuments(
    private val resolver: ContentResolver,
    private val service: FluxaBackupService,
) {
    suspend fun export(password: CharArray, destination: Uri): BackupResult<BackupManifestV1> = try {
        withContext(Dispatchers.IO) {
            try {
                resolver.openOutputStream(destination, "wt")?.use { service.export(password, it) }
                    ?: BackupResult.Failure(BackupError.EXPORT_FAILED)
            } catch (_: IOException) {
                BackupResult.Failure(BackupError.EXPORT_FAILED)
            } catch (_: SecurityException) {
                BackupResult.Failure(BackupError.EXPORT_FAILED)
            }
        }
    } finally {
        password.fill('\u0000')
    }

    suspend fun validate(password: CharArray, source: Uri): BackupResult<PreparedFluxaBackup> = try {
        withContext(Dispatchers.IO) {
            try {
                resolver.openInputStream(source)?.use { service.validate(password, it) }
                    ?: BackupResult.Failure(BackupError.INVALID)
            } catch (_: IOException) {
                BackupResult.Failure(BackupError.CORRUPTED)
            } catch (_: SecurityException) {
                BackupResult.Failure(BackupError.INVALID)
            }
        }
    } finally {
        password.fill('\u0000')
    }

    suspend fun restore(
        prepared: PreparedFluxaBackup,
        preventivePassword: CharArray,
        emergencyDestination: Uri,
    ): BackupResult<RestoreReceipt> = try {
        withContext(Dispatchers.IO) {
            try {
                resolver.openOutputStream(emergencyDestination, "wt")?.use { service.restore(prepared, preventivePassword, it) }
                    ?: BackupResult.Failure(BackupError.RESTORE_FAILED)
            } catch (_: IOException) {
                BackupResult.Failure(BackupError.RESTORE_FAILED)
            } catch (_: SecurityException) {
                BackupResult.Failure(BackupError.RESTORE_FAILED)
            }
        }
    } finally {
        preventivePassword.fill('\u0000')
    }
}
