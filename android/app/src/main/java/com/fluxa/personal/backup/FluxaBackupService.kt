package com.fluxa.personal.backup

import com.fluxa.personal.data.database.FluxaDatabase
import java.io.ByteArrayInputStream
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

    suspend fun export(output: OutputStream): BackupResult<BackupManifestV1> = operation(BackupError.EXPORT_FAILED) {
        val data = BackupDataV1(persistence.snapshot(), settings.read())
        val bytes = BackupCodec.encode(data, clock.instant(), appVersion)
        val checked = BackupCodec.decode(ByteArrayInputStream(bytes))
        output.write(bytes)
        output.flush()
        checked.manifest
    }

    suspend fun validate(input: InputStream): BackupResult<PreparedFluxaBackup> = operation(BackupError.CORRUPTED) {
        PreparedFluxaBackup(BackupCodec.decode(input))
    }

    /** REPLACE. The caller supplies a preventive backup destination before any financial data is replaced. */
    suspend fun restore(
        prepared: PreparedFluxaBackup,
        emergencyOutput: OutputStream,
    ): BackupResult<RestoreReceipt> = operation(BackupError.RESTORE_FAILED) {
        val incoming = prepared.document.data
        BackupValidator.validateData(incoming)
        val previous = BackupDataV1(persistence.snapshot(), settings.read())
        val emergency = BackupCodec.encode(previous, clock.instant(), appVersion)
        BackupCodec.decode(ByteArrayInputStream(emergency))
        emergencyOutput.use {
            it.write(emergency)
            it.flush()
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

    private suspend fun <T> operation(fallback: BackupError, block: suspend () -> T): BackupResult<T> =
        withContext(Dispatchers.IO) {
            try {
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
}
