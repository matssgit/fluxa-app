package com.fluxa.personal.ui

import android.util.Log
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasParent
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.fluxa.personal.FluxaApplication
import com.fluxa.personal.MainActivity
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import org.junit.After
import com.fluxa.personal.data.database.FluxaDatabase
import com.fluxa.personal.data.repository.AccountRepository
import com.fluxa.personal.backup.BackupResult
import com.fluxa.personal.backup.PortableSettingsV1
import com.fluxa.personal.core.money.Money
import com.fluxa.personal.core.preferences.LocalSettings
import com.fluxa.personal.core.preferences.ThemePreference
import com.fluxa.personal.domain.model.AccountDetails
import com.fluxa.personal.domain.model.AccountType
import com.fluxa.personal.ui.designsystem.components.FluxaMoneyFormat
import com.fluxa.personal.ui.feature.backup.BackupPickerTestHook
import java.io.File
import java.time.LocalDate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
class BackupRestoreFlowTest {
    @get:Rule
    val compose = createEmptyComposeRule()

    private val app get() = ApplicationProvider.getApplicationContext<FluxaApplication>()
    private val picker = ScriptedPicker()
    private lateinit var dir: File
    private val password = "minha senha do backup"

    @Before
    fun seed() = runBlocking {
        app.database.clearAllTables()
        app.preferences.apply(PortableSettingsV1(ThemePreference.LIGHT.name, false))
        app.accounts.create(AccountDetails("Conta Principal", AccountType.CHECKING, Money(150_000), LocalDate.of(2020, 1, 1)))
        dir = File(app.cacheDir, "backup-flow").apply { deleteRecursively(); mkdirs() }
    }

    private var scenario: ActivityScenario<MainActivity>? = null

    /** `MainActivity` real; só o seletor do sistema é trocado pelo roteirizado. */
    private fun launch() {
        BackupPickerTestHook.override = picker
        scenario = ActivityScenario.launch(MainActivity::class.java)
    }

    @After
    fun tearDown() {
        scenario?.close()
        BackupPickerTestHook.override = null
    }

    private fun accountNames(): List<String> = runBlocking { app.accounts.observeAll(includeArchived = true).first().map { it.details.name }.sorted() }
    private fun settings(): LocalSettings = runBlocking { app.preferences.settings.first() }

    private fun exists(tag: String) = compose.onAllNodes(hasTestTag(tag), useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()
    private fun waitFor(tag: String, timeout: Long = 30_000) = compose.waitUntil(timeout) { exists(tag) }
    private fun waitForText(text: String, timeout: Long = 30_000) = compose.waitUntil(timeout) {
        compose.onAllNodes(hasText(text, substring = true), useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()
    }

    private fun tap(tag: String) {
        compose.waitForIdle()
        val node = compose.onNodeWithTag(tag)
        val target = if (SemanticsActions.OnClick in node.fetchSemanticsNode().config) node
        else compose.onNode(hasClickAction() and hasParent(hasTestTag(tag)))
        target.performSemanticsAction(SemanticsActions.OnClick)
    }

    private fun openBackupScreen() {
        waitFor("tab-more")
        compose.onNodeWithTag("tab-more").performClick()
        waitFor("more-backup")
        tap("more-backup")
        waitFor("backup-security")
    }

    private fun exportWithBackend(file: File, secret: String = password) = runBlocking {
        val result = file.outputStream().use { app.backups.export(secret.toCharArray(), it) }
        assertTrue(result is BackupResult.Success)
    }

    private fun validates(file: File, secret: String = password) = runBlocking {
        file.inputStream().use { app.backups.validate(secret.toCharArray(), it) }
    }

    /** Abre o arquivo, informa a senha e espera o resultado da validação (lenta: KDF). */
    private fun unlock(file: File, secret: String, slow: Boolean = true): Long {
        picker.opens += file
        tap("action-backup-restore")
        waitFor("input-backup-restore-password")
        compose.onNodeWithTag("input-backup-restore-password").performTextInput(secret)
        val start = System.nanoTime()
        compose.onNodeWithTag("action-backup-restore-submit").performClick()
        if (slow) {
            // Feedback imediato com texto, sem porcentagem, e o botão some (sem duplo envio).
            waitForText("Abrindo backup protegido…", 5_000)
            assertTrue(!exists("action-backup-restore-submit"))
        }
        compose.waitUntil(30_000) { !exists("backup-working") }
        return (System.nanoTime() - start) / 1_000_000
    }

    @Test
    fun exportCreatesAnEncryptedBackupOfCurrentData() {
        launch()
        openBackupScreen()
        tap("action-backup-export")
        waitFor("backup-password-warning")
        waitForText("O Fluxa não armazena nem consegue recuperar a senha do backup")
        compose.onNodeWithTag("input-backup-password").performTextInput(password)
        compose.onNodeWithTag("input-backup-password-confirm").performTextInput(password)
        val file = File(dir, "Fluxa-backup.fluxa")
        picker.creates += file
        val start = System.nanoTime()
        compose.onNodeWithTag("action-backup-export-submit").performClick()
        waitForText("Protegendo seu backup…", 5_000)
        waitFor("backup-export-success")
        Log.i("FluxaBackupUx", "export UI ms=${(System.nanoTime() - start) / 1_000_000}")

        assertEquals(listOf("Fluxa-backup-${LocalDate.now()}.fluxa"), picker.requestedNames)
        assertTrue(compose.onAllNodes(hasText(password, substring = true), useUnmergedTree = true).fetchSemanticsNodes().isEmpty())
        val header = file.readBytes().copyOfRange(0, 8).toString(Charsets.US_ASCII)
        assertEquals("FLUXAENC", header)
        val checked = validates(file)
        assertTrue(checked is BackupResult.Success)
        assertEquals(1, (checked as BackupResult.Success).value.manifest.recordCounts["accounts"])
        assertEquals(listOf("Conta Principal"), accountNames())
    }

    @Test
    fun exportMismatchAndCancelledPickerAreHandledWithoutWriting() {
        launch()
        openBackupScreen()
        tap("action-backup-export")
        waitFor("input-backup-password")
        compose.onNodeWithTag("input-backup-password").performTextInput(password)
        compose.onNodeWithTag("input-backup-password-confirm").performTextInput("outra senha qualquer")
        compose.onNodeWithTag("action-backup-export-submit").performClick()
        waitForText("As senhas não coincidem.", 5_000)
        assertTrue(picker.requestedNames.isEmpty())
        // Mostrar/ocultar senha tem descrição acessível.
        compose.onNode(hasTestTag("input-backup-password-visibility") and hasContentDescription("Mostrar senha")).assertExists()
        compose.onNodeWithTag("input-backup-password-visibility").performClick()
        compose.onNode(hasTestTag("input-backup-password-visibility") and hasContentDescription("Ocultar senha")).assertExists()
    }

    @Test
    fun restoreReplacesCurrentDataAndCreatesThePreventiveBackup() {
        val backup = File(dir, "antigo.fluxa")
        exportWithBackend(backup)
        runBlocking { app.accounts.create(AccountDetails("Conta Nova", AccountType.CHECKING, Money(1_000), LocalDate.of(2021, 1, 1))) }
        assertEquals(listOf("Conta Nova", "Conta Principal"), accountNames())

        launch()
        openBackupScreen()
        val unlockMs = unlock(backup, password)
        waitFor("backup-confirm-sheet")
        waitForText("Substituir dados atuais?")
        waitForText("Um backup preventivo será criado antes da substituição.")
        assertEquals(listOf("Conta Nova", "Conta Principal"), accountNames()) // nada substituído antes de confirmar

        val preventive = File(dir, "preventivo.fluxa")
        picker.creates += preventive
        val start = System.nanoTime()
        tap("action-backup-confirm-restore")
        waitForText("Restaurando seus dados…", 5_000)
        waitFor("backup-restore-success")
        Log.i("FluxaBackupUx", "unlock UI ms=$unlockMs restore UI ms=${(System.nanoTime() - start) / 1_000_000}")
        waitForText("Seus dados foram restaurados com sucesso.")
        assertTrue(!exists("backup-settings-warning"))

        assertEquals(listOf("Conta Principal"), accountNames())
        assertTrue(picker.requestedNames.single().startsWith("Fluxa-antes-da-restauracao-"))
        // O preventivo guarda o estado substituído e abre com a mesma senha.
        val saved = validates(preventive)
        assertEquals(2, (saved as BackupResult.Success).value.manifest.recordCounts["accounts"])

        tap("action-backup-go-home")
        waitFor("list-dashboard")
        assertTrue(!exists("screen-backup"))
    }

    @Test
    fun cancellingTheConfirmationKeepsCurrentData() {
        val backup = File(dir, "antigo.fluxa")
        exportWithBackend(backup)
        runBlocking { app.accounts.create(AccountDetails("Conta Nova", AccountType.CHECKING, Money(1_000), LocalDate.of(2021, 1, 1))) }
        launch()
        openBackupScreen()
        unlock(backup, password)
        waitFor("backup-confirm-sheet")
        tap("action-backup-confirm-cancel")
        waitFor("backup-security")
        assertTrue(picker.requestedNames.isEmpty())
        assertEquals(listOf("Conta Nova", "Conta Principal"), accountNames())
    }

    @Test
    fun wrongPasswordDoesNotTouchTheDatabase() {
        val backup = File(dir, "antigo.fluxa")
        exportWithBackend(backup)
        runBlocking { app.accounts.create(AccountDetails("Conta Nova", AccountType.CHECKING, Money(1_000), LocalDate.of(2021, 1, 1))) }
        launch()
        openBackupScreen()
        unlock(backup, "senha errada qualquer")
        waitForText("Não foi possível abrir o backup. Verifique a senha e tente novamente.")
        waitFor("input-backup-restore-password")
        assertTrue(picker.requestedNames.isEmpty())
        assertEquals(listOf("Conta Nova", "Conta Principal"), accountNames())
        assertEquals(ThemePreference.LIGHT, settings().theme)
    }

    @Test
    fun corruptedFilesDoNotTouchTheDatabase() {
        val good = File(dir, "antigo.fluxa")
        exportWithBackend(good)
        val bytes = good.readBytes()
        runBlocking { app.accounts.create(AccountDetails("Conta Nova", AccountType.CHECKING, Money(1_000), LocalDate.of(2021, 1, 1))) }
        val notFluxa = File(dir, "nota.fluxa").apply { writeText("isto não é um backup do Fluxa") }
        val truncated = File(dir, "cortado.fluxa").apply { writeBytes(bytes.copyOf(bytes.size - 40)) }
        val tampered = File(dir, "alterado.fluxa").apply { writeBytes(bytes.copyOf().also { it[it.size - 30] = (it[it.size - 30].toInt() xor 1).toByte() }) }
        launch()
        openBackupScreen()
        for (file in listOf(notFluxa, truncated)) {
            unlock(file, password, slow = false) // rejeitado no cabeçalho, sem KDF
            waitFor("backup-restore-error")
            waitForText(if (file == notFluxa) "Este arquivo não é um backup válido do Fluxa."
                else "Este arquivo não pôde ser validado como um backup válido do Fluxa.")
            tap("action-backup-error-back")
            waitFor("backup-security")
        }
        // Alteração autenticada: indistinguível de senha errada pelo contrato (GCM).
        unlock(tampered, password)
        waitForText("Verifique a senha e tente novamente.")
        assertTrue(picker.requestedNames.isEmpty())
        assertEquals(listOf("Conta Nova", "Conta Principal"), accountNames())
    }

    @Test
    fun restoredPreferencesApplyAfterGoingHome() {
        runBlocking { app.preferences.apply(PortableSettingsV1(ThemePreference.DARK.name, true)) }
        val backup = File(dir, "antigo.fluxa")
        exportWithBackend(backup)
        runBlocking { app.preferences.apply(PortableSettingsV1(ThemePreference.LIGHT.name, false)) }
        launch()
        openBackupScreen()
        unlock(backup, password)
        waitFor("backup-confirm-sheet")
        picker.creates += File(dir, "preventivo.fluxa")
        tap("action-backup-confirm-restore")
        waitFor("backup-restore-success")
        assertEquals(LocalSettings(ThemePreference.DARK, hideAmounts = true), settings())
        tap("action-backup-go-home")
        waitFor("list-dashboard")
        // Shell recriado: a privacidade restaurada vale no Início (sem a escolha da sessão anterior).
        waitForText(FluxaMoneyFormat.MASK)
        assertTrue(compose.onAllNodes(hasText("R$", substring = true), useUnmergedTree = true).fetchSemanticsNodes().isEmpty())
    }

    @Test
    fun restoredDataSurvivesReopeningTheApp() {
        val backup = File(dir, "antigo.fluxa")
        exportWithBackend(backup)
        runBlocking { app.accounts.create(AccountDetails("Conta Nova", AccountType.CHECKING, Money(1_000), LocalDate.of(2021, 1, 1))) }
        launch()
        openBackupScreen()
        unlock(backup, password)
        waitFor("backup-confirm-sheet")
        picker.creates += File(dir, "preventivo.fluxa")
        tap("action-backup-confirm-restore")
        waitFor("backup-restore-success")
        // Uma conexão Room nova, sobre o mesmo arquivo, lê o estado restaurado já gravado em disco.
        val fresh = FluxaDatabase.create(app)
        try {
            val names = runBlocking { AccountRepository(fresh.accountDao()).observeAll(includeArchived = true).first().map { it.details.name } }
            assertEquals(listOf("Conta Principal"), names)
        } finally {
            fresh.close()
        }
    }
}
