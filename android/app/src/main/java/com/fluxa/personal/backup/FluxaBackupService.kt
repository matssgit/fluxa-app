package com.fluxa.personal.backup

import com.fluxa.personal.data.database.FluxaDatabase
import java.io.InputStream
import java.io.OutputStream
import java.time.Clock
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal interface BackupSettingsStore {
    suspend fun read(): PortableSettingsV1
    suspend fun apply(settings: PortableSettingsV1)
}

internal class FluxaBackupService(
    database: FluxaDatabase,
    private val settings: BackupSettingsStore,
    private val appVersion: String,
    private val clock: Clock = Clock.systemUTC(),
) {
    private val persistence = BackupPersistence(database)

    /** Consumes and clears password, including validation failure or cancellation. */
    suspend fun export(password: CharArray, output: OutputStream): BackupResult<BackupManifestV1> =
        passwordOperation(password, BackupError.EXPORT_FAILED) {
            val data = BackupDataV1(persistence.snapshot(), settings.read())
            val payload = BackupCodec.encode(data, clock.instant(), appVersion)
            try {
                val manifest = BackupCodec.decode(payload).manifest
                BackupCrypto.encrypt(payload, password, output)
                output.flush()
                manifest
            } finally {
                payload.fill(0)
            }
        }

    /** Authentication finishes before parsing the financial payload or preparing a restore. */
    suspend fun validate(password: CharArray, input: InputStream): BackupResult<PreparedFluxaBackup> =
        passwordOperation(password, BackupError.CORRUPTED) {
            val payload = BackupCrypto.decrypt(input, password)
            try {
                PreparedFluxaBackup(BackupCodec.decode(payload))
            } finally {
                payload.fill(0)
            }
        }

    /** REPLACE. A fresh password protects the preventive backup and is consumed by this call. */
    suspend fun restore(
        prepared: PreparedFluxaBackup,
        preventivePassword: CharArray,
        emergencyOutput: OutputStream,
    ): BackupResult<RestoreReceipt> = passwordOperation(preventivePassword, BackupError.RESTORE_FAILED) {
        val incoming = prepared.document.data
        BackupValidator.validateData(incoming)
        val previous = BackupDataV1(persistence.snapshot(), settings.read())
        val payload = BackupCodec.encode(previous, clock.instant(), appVersion)
        try {
            BackupCodec.decode(payload)
            emergencyOutput.use {
                BackupCrypto.encrypt(payload, preventivePassword, it)
                it.flush()
            }
        } finally {
            payload.fill(0)
        }
        // A concurrent financial edit after the preventive snapshot rejects the replacement.
        persistence.replace(incoming, previous.tables)
        val settingsApplied = try {
            settings.apply(incoming.settings)
            true
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            false
        }
        RestoreReceipt(settingsApplied)
    }

    private suspend fun <T> passwordOperation(
        password: CharArray,
        fallback: BackupError,
        block: suspend () -> T,
    ): BackupResult<T> = try {
        withContext(Dispatchers.IO) {
            try {
                BackupCrypto.validatePassword(password)
                BackupResult.Success(block())
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: BackupException) {
                BackupResult.Failure(error.error)
            } catch (_: java.time.DateTimeException) {
                BackupResult.Failure(BackupError.INVALID)
            } catch (_: IllegalArgumentException) {
                BackupResult.Failure(BackupError.INVALID)
            } catch (_: Exception) {
                BackupResult.Failure(fallback)
            }
        }
    } finally {
        password.fill('\u0000')
    }
}
