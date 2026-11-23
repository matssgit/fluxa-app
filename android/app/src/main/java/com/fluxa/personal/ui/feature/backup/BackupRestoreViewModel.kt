package com.fluxa.personal.ui.feature.backup

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.fluxa.personal.R
import com.fluxa.personal.backup.BackupError
import com.fluxa.personal.backup.BackupResult
import com.fluxa.personal.backup.FLUXA_BACKUP_EXTENSION
import com.fluxa.personal.backup.PreparedFluxaBackup
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Trabalho em andamento. Cada valor corresponde a uma única chamada do backend, que não expõe progresso:
 * - EXPORTING = captura + cifragem (KDF) + escrita do arquivo (`export`);
 * - UNLOCKING = leitura + autenticação/decifragem (KDF) + validação completa (`validate`);
 * - RESTORING = backup preventivo (KDF) + substituição REPLACE + aplicação das preferências (`restore`).
 */
internal enum class BackupWork { EXPORTING, UNLOCKING, RESTORING }

internal sealed interface BackupUiState {
    data object Home : BackupUiState

    /** [attempt] muda quando os campos precisam ser limpos (ex.: depois de uma falha). */
    data class ExportPassword(
        val attempt: Int,
        @StringRes val passwordError: Int? = null,
        @StringRes val confirmationError: Int? = null,
        @StringRes val banner: Int? = null,
    ) : BackupUiState

    /** Seletor "Salvar como" aberto. A senha validada está retida só no ViewModel. */
    data class ChoosingDestination(val attempt: Int) : BackupUiState
    data class Working(val work: BackupWork) : BackupUiState
    data class ExportSuccess(val fileName: String?, val createdAt: Instant) : BackupUiState

    data object ChoosingFile : BackupUiState
    data class RestorePassword(val fileName: String?, val attempt: Int, @StringRes val error: Int? = null) : BackupUiState

    /** Backup aberto e validado; aguardando a confirmação de substituição. */
    data class RestoreConfirm(val fileName: String?, val createdAt: Instant, @StringRes val error: Int? = null) : BackupUiState
    data class ChoosingPreventiveDestination(val fileName: String?, val createdAt: Instant) : BackupUiState
    data class RestoreSuccess(val settingsApplied: Boolean) : BackupUiState

    /** Falha definitiva do arquivo ou da restauração. O banco atual nunca é alterado nesses casos. */
    data class RestoreError(@StringRes val message: Int) : BackupUiState
}

internal val BackupUiState.isWorking get() = this is BackupUiState.Working

/**
 * Fluxos de exportação e restauração. Senhas chegam como `CharArray` (cópias feitas pela UI), ficam só em memória,
 * nunca em `SavedStateHandle`, e são zeradas em sucesso, cancelamento, erro definitivo e em [onCleared].
 * Se o processo morrer no meio do fluxo, o ViewModel recomeça em [BackupUiState.Home] e a senha precisa ser informada de novo.
 */
internal class BackupRestoreViewModel(
    private val gateway: BackupGateway,
    private val today: () -> LocalDate = { LocalDate.now() },
    private val passwordRule: (CharArray) -> BackupError? = ::backupPasswordError,
) : ViewModel() {
    private val _state = MutableStateFlow<BackupUiState>(BackupUiState.Home)
    val uiState: StateFlow<BackupUiState> = _state.asStateFlow()

    private var attempt = 0
    private var heldPassword: CharArray? = null
    private var source: DocumentRef? = null
    private var sourceName: String? = null
    private var prepared: PreparedFluxaBackup? = null

    val suggestedExportName: String get() = "Fluxa-backup-${today()}$FLUXA_BACKUP_EXTENSION"
    val suggestedPreventiveName: String get() = "Fluxa-antes-da-restauracao-${today()}$FLUXA_BACKUP_EXTENSION"

    // region Exportação

    fun startExport() {
        if (_state.value != BackupUiState.Home) return
        clearSecrets()
        _state.value = BackupUiState.ExportPassword(attempt)
    }

    /**
     * Valida as duas senhas (a igualdade aqui; o mínimo pela regra do backend) e assume a posse dos dois arrays.
     * Retorna `true` quando a UI deve abrir o seletor "Salvar como".
     */
    fun submitExport(password: CharArray, confirmation: CharArray): Boolean {
        try {
            if (_state.value !is BackupUiState.ExportPassword) return false
            val passwordError = passwordRule(password)?.let(::passwordMessage)
            val confirmationError = if (passwordError == null && !password.contentEquals(confirmation)) R.string.backup_password_mismatch else null
            if (passwordError != null || confirmationError != null) {
                _state.value = BackupUiState.ExportPassword(attempt, passwordError, confirmationError)
                return false
            }
            replaceHeldPassword(password.copyOf())
            _state.value = BackupUiState.ChoosingDestination(attempt)
            return true
        } finally {
            password.fill('\u0000')
            confirmation.fill('\u0000')
        }
    }

    /** Resultado do seletor. `null` = usuário cancelou: não é erro, volta ao formulário com os campos preenchidos. */
    fun onExportDestination(destination: DocumentRef?) {
        if (_state.value !is BackupUiState.ChoosingDestination) return
        val password = heldPassword
        if (destination == null || password == null) {
            clearSecrets()
            _state.value = BackupUiState.ExportPassword(attempt)
            return
        }
        _state.value = BackupUiState.Working(BackupWork.EXPORTING)
        viewModelScope.launch {
            val result = try {
                gateway.export(password.copyOf(), destination)
            } finally {
                clearSecrets()
            }
            _state.value = when (result) {
                is BackupResult.Success -> BackupUiState.ExportSuccess(gateway.displayName(destination), result.value.createdAt)
                is BackupResult.Failure -> {
                    gateway.discard(destination)
                    attempt++
                    BackupUiState.ExportPassword(attempt, banner = exportMessage(result.error))
                }
            }
        }
    }

    // endregion

    // region Restauração

    /** Retorna `true` quando a UI deve abrir o seletor de arquivo. */
    fun startRestore(): Boolean {
        if (_state.value != BackupUiState.Home) return false
        clearSecrets()
        _state.value = BackupUiState.ChoosingFile
        return true
    }

    fun onRestoreFile(document: DocumentRef?) {
        if (_state.value != BackupUiState.ChoosingFile) return
        if (document == null) {
            _state.value = BackupUiState.Home
            return
        }
        source = document
        sourceName = null
        _state.value = BackupUiState.RestorePassword(null, attempt)
        viewModelScope.launch {
            val name = gateway.displayName(document)
            val current = _state.value
            if (source == document && current is BackupUiState.RestorePassword && current.fileName == null) {
                sourceName = name
                _state.value = current.copy(fileName = name)
            }
        }
    }

    /** Abre e valida o arquivo. A senha fica retida (só em memória) para proteger o backup preventivo. */
    fun submitRestorePassword(password: CharArray) {
        try {
            val current = _state.value as? BackupUiState.RestorePassword ?: return
            val file = source ?: return
            passwordRule(password)?.let {
                _state.value = current.copy(error = restorePasswordMessage(it))
                return
            }
            replaceHeldPassword(password.copyOf())
            val forValidation = password.copyOf()
            _state.value = BackupUiState.Working(BackupWork.UNLOCKING)
            viewModelScope.launch {
                when (val result = gateway.validate(forValidation, file)) {
                    is BackupResult.Success -> {
                        prepared = result.value
                        _state.value = BackupUiState.RestoreConfirm(sourceName, result.value.manifest.createdAt)
                    }
                    is BackupResult.Failure -> {
                        clearSecrets(keepSource = true)
                        val passwordMessage = when (result.error) {
                            BackupError.INVALID_PASSWORD, BackupError.PASSWORD_REQUIRED,
                            BackupError.PASSWORD_TOO_SHORT, BackupError.PASSWORD_INVALID_ENCODING -> restorePasswordMessage(result.error)
                            else -> null
                        }
                        if (passwordMessage != null) {
                            attempt++
                            _state.value = BackupUiState.RestorePassword(sourceName, attempt, passwordMessage)
                        } else {
                            clearSecrets()
                            _state.value = BackupUiState.RestoreError(fileMessage(result.error))
                        }
                    }
                }
            }
        } finally {
            password.fill('\u0000')
        }
    }

    /** "Restaurar backup" na confirmação. Retorna `true` quando a UI deve abrir o seletor do backup preventivo. */
    fun confirmRestore(): Boolean {
        val current = _state.value as? BackupUiState.RestoreConfirm ?: return false
        if (prepared == null || heldPassword == null) return false
        _state.value = BackupUiState.ChoosingPreventiveDestination(current.fileName, current.createdAt)
        return true
    }

    /** Destino do backup preventivo. Cancelar o seletor volta à confirmação, sem substituir nada. */
    fun onPreventiveDestination(destination: DocumentRef?) {
        val current = _state.value as? BackupUiState.ChoosingPreventiveDestination ?: return
        val backup = prepared
        val password = heldPassword
        if (backup == null || password == null) {
            clearSecrets()
            _state.value = BackupUiState.Home
            return
        }
        if (destination == null) {
            _state.value = BackupUiState.RestoreConfirm(current.fileName, current.createdAt)
            return
        }
        _state.value = BackupUiState.Working(BackupWork.RESTORING)
        viewModelScope.launch {
            when (val result = gateway.restore(backup, password.copyOf(), destination)) {
                is BackupResult.Success -> {
                    clearSecrets()
                    _state.value = BackupUiState.RestoreSuccess(result.value.settingsApplied)
                }
                is BackupResult.Failure -> if (result.error == BackupError.RESTORE_CONFLICT) {
                    // Nada foi substituído; o arquivo continua válido. Nova confirmação e novo destino preventivo.
                    _state.value = BackupUiState.RestoreConfirm(current.fileName, current.createdAt, R.string.backup_error_conflict)
                } else {
                    clearSecrets()
                    _state.value = BackupUiState.RestoreError(R.string.backup_error_restore_failed)
                }
            }
        }
    }

    // endregion

    fun dismissBanner() {
        val current = _state.value
        if (current is BackupUiState.ExportPassword && current.banner != null) _state.value = current.copy(banner = null)
        if (current is BackupUiState.RestoreConfirm && current.error != null) _state.value = current.copy(error = null)
    }

    /** Voltar/cancelar: descarta o fluxo e limpa as senhas. Ignorado enquanto há trabalho em andamento. */
    fun cancel() {
        if (_state.value.isWorking) return
        clearSecrets()
        attempt++
        _state.value = BackupUiState.Home
    }

    override fun onCleared() {
        clearSecrets()
    }

    private fun replaceHeldPassword(password: CharArray) {
        heldPassword?.fill('\u0000')
        heldPassword = password
    }

    private fun clearSecrets(keepSource: Boolean = false) {
        heldPassword?.fill('\u0000')
        heldPassword = null
        prepared = null
        if (!keepSource) {
            source = null
            sourceName = null
        }
    }

    /** Somente para testes: indica se ainda há senha retida. */
    internal val holdsPassword: Boolean get() = heldPassword != null

    companion object {
        fun factory(gateway: BackupGateway): ViewModelProvider.Factory = viewModelFactory {
            initializer { BackupRestoreViewModel(gateway) }
        }

        @StringRes
        internal fun passwordMessage(error: BackupError): Int = when (error) {
            BackupError.PASSWORD_REQUIRED -> R.string.backup_password_required
            BackupError.PASSWORD_INVALID_ENCODING -> R.string.backup_password_encoding
            else -> R.string.backup_password_too_short
        }

        @StringRes
        internal fun restorePasswordMessage(error: BackupError): Int = when (error) {
            BackupError.INVALID_PASSWORD -> R.string.backup_error_wrong_password
            else -> passwordMessage(error)
        }

        @StringRes
        internal fun exportMessage(error: BackupError): Int = when (error) {
            BackupError.TOO_LARGE -> R.string.backup_error_export_too_large
            else -> R.string.backup_error_export_failed
        }

        /** O contrato não distingue "não é um arquivo do Fluxa" de "envelope de versão futura" (`CRYPTO_UNSUPPORTED`). */
        @StringRes
        internal fun fileMessage(error: BackupError): Int = when (error) {
            BackupError.VERSION_UNSUPPORTED -> R.string.backup_error_version
            BackupError.TOO_LARGE -> R.string.backup_error_file_too_large
            BackupError.DECRYPTION_FAILED -> R.string.backup_error_open_failed
            else -> R.string.backup_error_invalid_file
        }
    }
}
