package com.fluxa.personal.ui.feature.backup

import android.content.ContentResolver
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.OpenableColumns
import com.fluxa.personal.backup.BackupDocuments
import com.fluxa.personal.backup.BackupManifestV1
import com.fluxa.personal.backup.BackupResult
import com.fluxa.personal.backup.PreparedFluxaBackup
import com.fluxa.personal.backup.RestoreReceipt
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal object BackupRoutes {
    const val HOME = "backup"
}

/** Documento escolhido no seletor do Android (SAF). Opaco para o ViewModel: só a camada Android o interpreta. */
@JvmInline
internal value class DocumentRef(val value: String)

/**
 * Recorte dos contratos da Sprint 8-B usado pela UI. Toda senha recebida é consumida (zerada) pelo backend;
 * o ViewModel sempre entrega uma cópia nova e limpa a sua.
 */
internal interface BackupGateway {
    suspend fun export(password: CharArray, destination: DocumentRef): BackupResult<BackupManifestV1>
    suspend fun validate(password: CharArray, source: DocumentRef): BackupResult<PreparedFluxaBackup>
    suspend fun restore(prepared: PreparedFluxaBackup, preventivePassword: CharArray, emergency: DocumentRef): BackupResult<RestoreReceipt>

    /** Nome exibível do documento, quando o provedor informa. Nunca é um caminho absoluto. */
    suspend fun displayName(document: DocumentRef): String?

    /** Remove, se possível, o documento que acabou de ser criado e ficou incompleto após uma falha de exportação. */
    suspend fun discard(document: DocumentRef)
}

internal class RepositoryBackupGateway(
    private val documents: BackupDocuments,
    private val resolver: ContentResolver,
) : BackupGateway {
    override suspend fun export(password: CharArray, destination: DocumentRef) =
        documents.export(password, Uri.parse(destination.value))

    override suspend fun validate(password: CharArray, source: DocumentRef) =
        documents.validate(password, Uri.parse(source.value))

    override suspend fun restore(prepared: PreparedFluxaBackup, preventivePassword: CharArray, emergency: DocumentRef) =
        documents.restore(prepared, preventivePassword, Uri.parse(emergency.value))

    override suspend fun displayName(document: DocumentRef): String? = withContext(Dispatchers.IO) {
        val uri = Uri.parse(document.value)
        if (uri.scheme == ContentResolver.SCHEME_FILE) return@withContext uri.lastPathSegment
        runCatching {
            resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) cursor.getString(0) else null
            }
        }.getOrNull()
    }

    override suspend fun discard(document: DocumentRef) {
        withContext(Dispatchers.IO) {
            val uri = Uri.parse(document.value)
            runCatching {
                if (uri.scheme == ContentResolver.SCHEME_FILE) uri.path?.let { File(it).delete() }
                else DocumentsContract.deleteDocument(resolver, uri)
            }
        }
    }
}

/** Regra de senha do backend (8 pontos de código Unicode, sem regras de composição). A UI não a reimplementa. */
internal fun backupPasswordError(password: CharArray): com.fluxa.personal.backup.BackupError? = try {
    com.fluxa.personal.backup.BackupCrypto.validatePassword(password)
    null
} catch (error: com.fluxa.personal.backup.BackupException) {
    error.error
}
