package com.fluxa.personal.ui.feature

import com.fluxa.personal.R
import com.fluxa.personal.backup.BackupDataV1
import com.fluxa.personal.backup.BackupDocumentV1
import com.fluxa.personal.backup.BackupError
import com.fluxa.personal.backup.BackupManifestV1
import com.fluxa.personal.backup.BackupResult
import com.fluxa.personal.backup.PortableSettingsV1
import com.fluxa.personal.backup.PreparedFluxaBackup
import com.fluxa.personal.backup.RestoreReceipt
import com.fluxa.personal.ui.feature.backup.BackupGateway
import com.fluxa.personal.ui.feature.backup.BackupRestoreViewModel
import com.fluxa.personal.ui.feature.backup.BackupUiState
import com.fluxa.personal.ui.feature.backup.BackupWork
import com.fluxa.personal.ui.feature.backup.DocumentRef
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

private val CREATED = Instant.parse("2026-11-19T17:20:00Z")

private fun manifest() = BackupManifestV1(1, CREATED, "1.0", 7, "BRL", "NONE", "sha", emptyMap())

private fun prepared() = PreparedFluxaBackup(BackupDocumentV1(manifest(), BackupDataV1(emptyMap(), PortableSettingsV1("DARK", true))))

/** Fake dos contratos 8-B: registra o que recebeu e, como o backend real, zera toda senha recebida. */
private class FakeBackupGateway : BackupGateway {
    var exportResult: BackupResult<BackupManifestV1> = BackupResult.Success(manifest())
    var validateResult: BackupResult<PreparedFluxaBackup> = BackupResult.Success(prepared())
    var restoreResult: BackupResult<RestoreReceipt> = BackupResult.Success(RestoreReceipt(true))
    var gate: CompletableDeferred<Unit>? = null

    val exported = mutableListOf<Pair<String, DocumentRef>>()
    val validated = mutableListOf<Pair<String, DocumentRef>>()
    val restored = mutableListOf<Pair<String, DocumentRef>>()
    val received = mutableListOf<CharArray>()
    val discarded = mutableListOf<DocumentRef>()

    private suspend fun consume(password: CharArray): String {
        received += password
        val text = String(password)
        gate?.await()
        password.fill('\u0000')
        return text
    }

    override suspend fun export(password: CharArray, destination: DocumentRef): BackupResult<BackupManifestV1> {
        exported += consume(password) to destination
        return exportResult
    }

    override suspend fun validate(password: CharArray, source: DocumentRef): BackupResult<PreparedFluxaBackup> {
        validated += consume(password) to source
        return validateResult
    }

    override suspend fun restore(prepared: PreparedFluxaBackup, preventivePassword: CharArray, emergency: DocumentRef): BackupResult<RestoreReceipt> {
        restored += consume(preventivePassword) to emergency
        return restoreResult
    }

    override suspend fun displayName(document: DocumentRef) = document.value.substringAfterLast('/')
    override suspend fun discard(document: DocumentRef) {
        discarded += document
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class BackupRestoreViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private lateinit var gateway: FakeBackupGateway
    private lateinit var vm: BackupRestoreViewModel

    private val destination = DocumentRef("content://docs/Fluxa-backup-2026-11-19.fluxa")
    private val source = DocumentRef("content://docs/antigo.fluxa")
    private val preventive = DocumentRef("content://docs/Fluxa-antes-da-restauracao-2026-11-19.fluxa")

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        gateway = FakeBackupGateway()
        vm = BackupRestoreViewModel(gateway, today = { LocalDate.of(2026, 11, 19) })
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    private val state get() = vm.uiState.value
    private fun chars(text: String) = text.toCharArray()
    private fun CharArray.isWiped() = all { it == '\u0000' }

    // region Exportação

    @Test
    fun shortPasswordIsRejectedByTheBackendRuleWithoutOpeningThePicker() {
        vm.startExport()
        val password = chars("1234567")
        val confirmation = chars("1234567")
        assertFalse(vm.submitExport(password, confirmation))
        assertEquals(R.string.backup_password_too_short, (state as BackupUiState.ExportPassword).passwordError)
        assertTrue(password.isWiped() && confirmation.isWiped())
        assertFalse(vm.holdsPassword)
    }

    @Test
    fun emptyPasswordAsksForOne() {
        vm.startExport()
        assertFalse(vm.submitExport(chars(""), chars("")))
        assertEquals(R.string.backup_password_required, (state as BackupUiState.ExportPassword).passwordError)
    }

    @Test
    fun eightCodePointsWithAccentsSpacesAndEmojiAreEnoughWithoutCompositionRules() {
        vm.startExport()
        // O emoji ocupa 2 chars: "é ã 😀 a" tem 8 chars mas só 7 pontos de código, e é recusada.
        val seven = "é ã 😀 a"
        assertEquals(8, seven.length)
        assertEquals(7, seven.codePointCount(0, seven.length))
        assertFalse(vm.submitExport(chars(seven), chars(seven)))
        assertEquals(R.string.backup_password_too_short, (state as BackupUiState.ExportPassword).passwordError)
        // Com mais um ponto de código passa. Sem maiúscula, número ou símbolo obrigatório.
        val text = "é ã 😀 ab"
        assertEquals(8, text.codePointCount(0, text.length))
        assertTrue(vm.submitExport(chars(text), chars(text)))
        assertTrue(state is BackupUiState.ChoosingDestination)
    }

    @Test
    fun differentConfirmationIsAFieldErrorOnTheConfirmation() {
        vm.startExport()
        assertFalse(vm.submitExport(chars("senha segura"), chars("senha segurA")))
        val form = state as BackupUiState.ExportPassword
        assertEquals(R.string.backup_password_mismatch, form.confirmationError)
        assertEquals(null, form.passwordError)
        assertFalse(vm.holdsPassword)
    }

    @Test
    fun cancellingThePickerIsNotAnErrorAndKeepsTheFormAttempt() {
        vm.startExport()
        val before = (state as BackupUiState.ExportPassword).attempt
        assertTrue(vm.submitExport(chars("senha segura"), chars("senha segura")))
        assertTrue(vm.holdsPassword)
        vm.onExportDestination(null)
        val form = state as BackupUiState.ExportPassword
        assertEquals(before, form.attempt) // mesmos campos preenchidos
        assertEquals(null, form.banner)
        assertFalse(vm.holdsPassword)
        assertTrue(gateway.exported.isEmpty())
    }

    @Test
    fun exportSuccessShowsNameAndDateAndClearsThePassword() = runTest(dispatcher) {
        vm.startExport()
        assertEquals("Fluxa-backup-2026-11-19.fluxa", vm.suggestedExportName)
        assertTrue(vm.submitExport(chars("senha segura"), chars("senha segura")))
        vm.onExportDestination(destination)
        assertEquals(BackupUiState.Working(BackupWork.EXPORTING), state)
        advanceUntilIdle()
        assertEquals(BackupUiState.ExportSuccess("Fluxa-backup-2026-11-19.fluxa", CREATED), state)
        assertEquals(listOf("senha segura" to destination), gateway.exported)
        assertFalse(vm.holdsPassword)
        assertTrue(gateway.received.all { it.isWiped() })
    }

    @Test
    fun exportFailureShowsMessageClearsFieldsAndDiscardsTheIncompleteFile() = runTest(dispatcher) {
        gateway.exportResult = BackupResult.Failure(BackupError.EXPORT_FAILED)
        vm.startExport()
        val before = (state as BackupUiState.ExportPassword).attempt
        vm.submitExport(chars("senha segura"), chars("senha segura"))
        vm.onExportDestination(destination)
        advanceUntilIdle()
        val form = state as BackupUiState.ExportPassword
        assertEquals(R.string.backup_error_export_failed, form.banner)
        assertTrue(form.attempt != before)
        assertEquals(listOf(destination), gateway.discarded)
        assertFalse(vm.holdsPassword)
    }

    @Test
    fun exportTooLargeHasItsOwnMessage() = runTest(dispatcher) {
        gateway.exportResult = BackupResult.Failure(BackupError.TOO_LARGE)
        vm.startExport()
        vm.submitExport(chars("senha segura"), chars("senha segura"))
        vm.onExportDestination(destination)
        advanceUntilIdle()
        assertEquals(R.string.backup_error_export_too_large, (state as BackupUiState.ExportPassword).banner)
    }

    @Test
    fun repeatedSubmitAndBackAreIgnoredWhileExporting() = runTest(dispatcher) {
        gateway.gate = CompletableDeferred()
        vm.startExport()
        vm.submitExport(chars("senha segura"), chars("senha segura"))
        vm.onExportDestination(destination)
        advanceUntilIdle()
        assertFalse(vm.submitExport(chars("senha segura"), chars("senha segura")))
        vm.onExportDestination(destination)
        vm.cancel()
        assertEquals(BackupUiState.Working(BackupWork.EXPORTING), state)
        gateway.gate!!.complete(Unit)
        advanceUntilIdle()
        assertEquals(1, gateway.exported.size)
        assertTrue(state is BackupUiState.ExportSuccess)
    }

    // endregion

    // region Restauração

    private fun openFileAndPassword(password: String = "senha segura") {
        assertTrue(vm.startRestore())
        vm.onRestoreFile(source)
        dispatcher.scheduler.advanceUntilIdle()
        vm.submitRestorePassword(chars(password))
    }

    @Test
    fun cancellingTheFilePickerReturnsHome() {
        assertTrue(vm.startRestore())
        vm.onRestoreFile(null)
        assertEquals(BackupUiState.Home, state)
    }

    @Test
    fun choosingAFileAsksForThePasswordBeforeAnything() = runTest(dispatcher) {
        vm.startRestore()
        vm.onRestoreFile(source)
        advanceUntilIdle()
        val form = state as BackupUiState.RestorePassword
        assertEquals("antigo.fluxa", form.fileName)
        assertTrue(gateway.validated.isEmpty())
        assertTrue(gateway.restored.isEmpty())
    }

    @Test
    fun correctPasswordUnlocksAndWaitsForConfirmationWithoutRestoring() = runTest(dispatcher) {
        openFileAndPassword()
        assertEquals(BackupUiState.Working(BackupWork.UNLOCKING), state)
        advanceUntilIdle()
        assertEquals(BackupUiState.RestoreConfirm("antigo.fluxa", CREATED), state)
        assertEquals(listOf("senha segura" to source), gateway.validated)
        assertTrue(gateway.restored.isEmpty())
        assertTrue(vm.holdsPassword) // retida para proteger o backup preventivo
    }

    @Test
    fun confirmationUsesTheSamePasswordForThePreventiveBackupAndReportsSuccess() = runTest(dispatcher) {
        openFileAndPassword()
        advanceUntilIdle()
        assertEquals("Fluxa-antes-da-restauracao-2026-11-19.fluxa", vm.suggestedPreventiveName)
        assertTrue(vm.confirmRestore())
        vm.onPreventiveDestination(preventive)
        assertEquals(BackupUiState.Working(BackupWork.RESTORING), state)
        advanceUntilIdle()
        assertEquals(BackupUiState.RestoreSuccess(settingsApplied = true), state)
        assertEquals(listOf("senha segura" to preventive), gateway.restored)
        assertFalse(vm.holdsPassword)
        assertTrue(gateway.received.all { it.isWiped() })
    }

    @Test
    fun cancellingTheConfirmationNeverRestoresAndClearsThePassword() = runTest(dispatcher) {
        openFileAndPassword()
        advanceUntilIdle()
        vm.cancel()
        assertEquals(BackupUiState.Home, state)
        assertFalse(vm.holdsPassword)
        assertTrue(gateway.restored.isEmpty())
    }

    @Test
    fun cancellingThePreventivePickerReturnsToTheConfirmation() = runTest(dispatcher) {
        openFileAndPassword()
        advanceUntilIdle()
        vm.confirmRestore()
        vm.onPreventiveDestination(null)
        assertEquals(BackupUiState.RestoreConfirm("antigo.fluxa", CREATED), state)
        assertTrue(gateway.restored.isEmpty())
        assertTrue(vm.holdsPassword)
    }

    @Test
    fun wrongPasswordStaysOnTheFormWithAFriendlyMessageAndNewAttempt() = runTest(dispatcher) {
        gateway.validateResult = BackupResult.Failure(BackupError.INVALID_PASSWORD)
        vm.startRestore()
        vm.onRestoreFile(source)
        advanceUntilIdle()
        val before = (state as BackupUiState.RestorePassword).attempt
        vm.submitRestorePassword(chars("senha errada"))
        advanceUntilIdle()
        val form = state as BackupUiState.RestorePassword
        assertEquals(R.string.backup_error_wrong_password, form.error)
        assertTrue(form.attempt != before) // campo limpo
        assertFalse(vm.holdsPassword)
        // Tenta de novo com o mesmo arquivo.
        gateway.validateResult = BackupResult.Success(prepared())
        vm.submitRestorePassword(chars("senha segura"))
        advanceUntilIdle()
        assertTrue(state is BackupUiState.RestoreConfirm)
        assertEquals(listOf("senha errada", "senha segura"), gateway.validated.map { it.first })
    }

    @Test
    fun shortRestorePasswordIsRejectedBeforeTheSlowKdf() {
        vm.startRestore()
        vm.onRestoreFile(source)
        vm.submitRestorePassword(chars("curta"))
        assertEquals(R.string.backup_password_too_short, (state as BackupUiState.RestorePassword).error)
        assertTrue(gateway.validated.isEmpty())
    }

    @Test
    fun invalidFileIsADefinitiveErrorWithoutRestore() = runTest(dispatcher) {
        for (error in listOf(BackupError.CORRUPTED, BackupError.INVALID, BackupError.INTEGRITY_FAILED, BackupError.RELATION_INVALID)) {
            vm.cancel()
            gateway.validateResult = BackupResult.Failure(error)
            openFileAndPassword()
            advanceUntilIdle()
            assertEquals(error.name, BackupUiState.RestoreError(R.string.backup_error_invalid_file), state)
            assertFalse(vm.holdsPassword)
        }
        assertTrue(gateway.restored.isEmpty())
    }

    @Test
    fun unsupportedVersionHasItsOwnMessage() = runTest(dispatcher) {
        for (error in listOf(BackupError.VERSION_UNSUPPORTED, BackupError.CONTAINER_VERSION_UNSUPPORTED)) {
            vm.cancel()
            gateway.validateResult = BackupResult.Failure(error)
            openFileAndPassword()
            advanceUntilIdle()
            assertEquals(BackupUiState.RestoreError(R.string.backup_error_version), state)
            assertTrue(gateway.restored.isEmpty())
        }
    }

    @Test
    fun foreignFileAndUnsupportedCryptoHaveDistinctMessages() = runTest(dispatcher) {
        for ((error, message) in listOf(
            BackupError.NOT_FLUXA to R.string.backup_error_not_fluxa,
            BackupError.CRYPTO_UNSUPPORTED to R.string.backup_error_crypto_unsupported,
        )) {
            vm.cancel()
            gateway.validateResult = BackupResult.Failure(error)
            openFileAndPassword()
            advanceUntilIdle()
            assertEquals(BackupUiState.RestoreError(message), state)
            assertTrue(gateway.restored.isEmpty())
        }
    }

    @Test
    fun failedPreventiveCleanupCanBeExplainedWithoutChangingTheOriginalRestoreError() = runTest(dispatcher) {
        gateway.restoreResult = BackupResult.Failure(
            BackupError.RESTORE_FAILED, cleanup = com.fluxa.personal.backup.BackupCleanup.FAILED,
        )
        openFileAndPassword()
        advanceUntilIdle()
        vm.confirmRestore()
        vm.onPreventiveDestination(preventive)
        advanceUntilIdle()
        assertEquals(BackupUiState.RestoreError(R.string.backup_error_restore_cleanup_failed), state)
        assertFalse(vm.holdsPassword)
        assertTrue(gateway.discarded.isEmpty())
    }

    @Test
    fun roomRestoredButSettingsFailedIsStillASuccessWithAWarning() = runTest(dispatcher) {
        gateway.restoreResult = BackupResult.Success(RestoreReceipt(settingsApplied = false))
        openFileAndPassword()
        advanceUntilIdle()
        vm.confirmRestore()
        vm.onPreventiveDestination(preventive)
        advanceUntilIdle()
        assertEquals(BackupUiState.RestoreSuccess(settingsApplied = false), state)
    }

    @Test
    fun restoreFailureSaysCurrentDataWasNotChanged() = runTest(dispatcher) {
        gateway.restoreResult = BackupResult.Failure(BackupError.RESTORE_FAILED)
        openFileAndPassword()
        advanceUntilIdle()
        vm.confirmRestore()
        vm.onPreventiveDestination(preventive)
        advanceUntilIdle()
        assertEquals(BackupUiState.RestoreError(R.string.backup_error_restore_failed), state)
        assertFalse(vm.holdsPassword)
    }

    @Test
    fun concurrentChangeReturnsToConfirmationToTryAgain() = runTest(dispatcher) {
        gateway.restoreResult = BackupResult.Failure(BackupError.RESTORE_CONFLICT)
        openFileAndPassword()
        advanceUntilIdle()
        vm.confirmRestore()
        vm.onPreventiveDestination(preventive)
        advanceUntilIdle()
        assertEquals(BackupUiState.RestoreConfirm("antigo.fluxa", CREATED, R.string.backup_error_conflict), state)
        assertTrue(vm.holdsPassword)
        gateway.restoreResult = BackupResult.Success(RestoreReceipt(true))
        assertTrue(vm.confirmRestore())
        vm.onPreventiveDestination(preventive)
        advanceUntilIdle()
        assertEquals(BackupUiState.RestoreSuccess(true), state)
        assertEquals(2, gateway.restored.size)
    }

    @Test
    fun doubleTapOnContinueStartsASingleUnlock() = runTest(dispatcher) {
        gateway.gate = CompletableDeferred()
        openFileAndPassword()
        vm.submitRestorePassword(chars("senha segura"))
        vm.cancel()
        advanceUntilIdle()
        assertEquals(BackupUiState.Working(BackupWork.UNLOCKING), state)
        gateway.gate!!.complete(Unit)
        advanceUntilIdle()
        assertEquals(1, gateway.validated.size)
    }

    // endregion

    // region Senha

    @Test
    fun everyPasswordHandedToTheUiOrBackendEndsWiped() = runTest(dispatcher) {
        val inputs = mutableListOf<CharArray>()
        fun typed(text: String) = chars(text).also { inputs += it }
        vm.startExport()
        vm.submitExport(typed("senha segura"), typed("senha segura"))
        vm.onExportDestination(destination)
        advanceUntilIdle()
        vm.cancel()
        vm.startRestore()
        vm.onRestoreFile(source)
        advanceUntilIdle()
        vm.submitRestorePassword(typed("senha segura"))
        advanceUntilIdle()
        vm.confirmRestore()
        vm.onPreventiveDestination(preventive)
        advanceUntilIdle()
        assertTrue(inputs.all { it.isWiped() })
        assertTrue(gateway.received.all { it.isWiped() })
        assertFalse(vm.holdsPassword)
    }

    @Test
    fun leavingTheScreenClearsAHeldPassword() = runTest(dispatcher) {
        openFileAndPassword()
        advanceUntilIdle()
        assertTrue(vm.holdsPassword)
        val clear = androidx.lifecycle.ViewModel::class.java.getDeclaredMethod("onCleared").apply { isAccessible = true }
        clear.invoke(vm)
        assertFalse(vm.holdsPassword)
    }

    // endregion
}
