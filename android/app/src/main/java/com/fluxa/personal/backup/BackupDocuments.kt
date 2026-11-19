package com.fluxa.personal.backup

import android.content.ContentResolver
import android.net.Uri
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** SAF adapters. The UI owns ACTION_CREATE_DOCUMENT / ACTION_OPEN_DOCUMENT and URI permissions. */
internal class BackupDocuments(
    private val resolver: ContentResolver,
    private val service: FluxaBackupService,
) {
    suspend fun export(destination: Uri): BackupResult<BackupManifestV1> = withContext(Dispatchers.IO) {
        try {
            resolver.openOutputStream(destination, "wt")?.use { service.export(it) }
                ?: BackupResult.Failure(BackupError.EXPORT_FAILED)
        } catch (_: IOException) {
            BackupResult.Failure(BackupError.EXPORT_FAILED)
        } catch (_: SecurityException) {
            BackupResult.Failure(BackupError.EXPORT_FAILED)
        }
    }

    suspend fun validate(source: Uri): BackupResult<PreparedFluxaBackup> = withContext(Dispatchers.IO) {
        try {
            resolver.openInputStream(source)?.use { service.validate(it) }
                ?: BackupResult.Failure(BackupError.INVALID)
        } catch (_: IOException) {
            BackupResult.Failure(BackupError.CORRUPTED)
        } catch (_: SecurityException) {
            BackupResult.Failure(BackupError.INVALID)
        }
    }

    suspend fun restore(prepared: PreparedFluxaBackup, emergencyDestination: Uri): BackupResult<RestoreReceipt> =
        withContext(Dispatchers.IO) {
            try {
                resolver.openOutputStream(emergencyDestination, "wt")?.use { service.restore(prepared, it) }
                    ?: BackupResult.Failure(BackupError.RESTORE_FAILED)
            } catch (_: IOException) {
                BackupResult.Failure(BackupError.RESTORE_FAILED)
            } catch (_: SecurityException) {
                BackupResult.Failure(BackupError.RESTORE_FAILED)
            }
        }
}
