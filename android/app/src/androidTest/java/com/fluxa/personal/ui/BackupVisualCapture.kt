package com.fluxa.personal.ui

import android.graphics.Bitmap
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.fluxa.personal.FluxaApplication
import com.fluxa.personal.MainActivity
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import org.junit.After
import com.fluxa.personal.backup.PortableSettingsV1
import com.fluxa.personal.core.money.Money
import com.fluxa.personal.domain.model.AccountDetails
import com.fluxa.personal.domain.model.AccountType
import com.fluxa.personal.ui.feature.backup.BackupPickerTestHook
import java.io.File
import java.time.LocalDate
import kotlinx.coroutines.runBlocking
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Capturas da Sprint 8-C para revisão visual. Só roda com `-e fluxaCapture <sufixo>`, depois de configurar
 * largura, tema e fonte via adb. Usa o backend real (KDF incluída) e o seletor roteirizado.
 * Salva PNGs em /sdcard/Android/data/com.fluxa.personal/files/captures.
 */
@OptIn(ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
class BackupVisualCapture {
    @get:Rule
    val compose = createEmptyComposeRule()

    private val suffix: String? = InstrumentationRegistry.getArguments().getString("fluxaCapture")
    private val app get() = ApplicationProvider.getApplicationContext<FluxaApplication>()
    private val picker = ScriptedPicker()
    private var scenario: ActivityScenario<MainActivity>? = null

    @After
    fun tearDown() {
        scenario?.close()
        BackupPickerTestHook.override = null
    }

    private fun exists(tag: String) = compose.onAllNodes(hasTestTag(tag), useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()
    private fun wait(tag: String, timeout: Long = 30_000) = compose.waitUntil(timeout) { exists(tag) }
    private fun waitText(text: String) = compose.waitUntil(10_000) {
        compose.onAllNodes(hasText(text, substring = true), useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()
    }

    private fun shot(name: String, idle: Boolean = true) {
        if (idle) compose.waitForIdle()
        Thread.sleep(if (idle) 600 else 250)
        val bitmap = InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
        val dir = File(app.getExternalFilesDir(null), "captures").apply { mkdirs() }
        File(dir, "$name-$suffix.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    @Test
    fun captureBackupAndRestore() {
        assumeTrue("Captura visual só roda sob demanda", suffix != null)
        val dir = File(app.cacheDir, "backup-capture").apply { deleteRecursively(); mkdirs() }
        val backup = File(dir, "Fluxa-backup-2026-11-19.fluxa")
        runBlocking {
            app.database.clearAllTables()
            app.preferences.apply(PortableSettingsV1("SYSTEM", false))
            app.accounts.create(AccountDetails("Conta principal", AccountType.CHECKING, Money(245_090), LocalDate.of(2026, 11, 1)))
            backup.outputStream().use { app.backups.export("senha do backup".toCharArray(), it) }
        }
        BackupPickerTestHook.override = picker
        scenario = ActivityScenario.launch(MainActivity::class.java)

        wait("tab-more")
        compose.onNodeWithTag("tab-more").performClick()
        wait("more-backup")
        shot("8c-mais")
        compose.onNodeWithTag("more-backup").performClick()
        wait("backup-security")
        shot("8c-inicio")

        // Exportação: erro de confirmação, processamento e sucesso.
        compose.onNodeWithTag("action-backup-export").performClick()
        wait("input-backup-password")
        shot("8c-export-form")
        compose.onNodeWithTag("input-backup-password").performTextInput("senha do backup")
        compose.onNodeWithTag("input-backup-password-confirm").performTextInput("senha diferente")
        compose.onNodeWithTag("action-backup-export-submit").performClick()
        waitText("As senhas não coincidem.")
        shot("8c-export-erro")
        // Voltar descarta o fluxo e limpa os campos; recomeça com as senhas iguais.
        compose.onNodeWithTag("action-back").performClick()
        picker.creates += File(dir, "novo.fluxa")
        wait("backup-security")
        compose.onNodeWithTag("action-backup-export").performClick()
        wait("input-backup-password")
        compose.onNodeWithTag("input-backup-password").performTextInput("senha do backup")
        compose.onNodeWithTag("input-backup-password-confirm").performTextInput("senha do backup")
        compose.onNodeWithTag("action-backup-export-submit").performClick()
        wait("backup-working", 5_000)
        shot("8c-export-processando", idle = false)
        wait("backup-export-success")
        shot("8c-export-sucesso")
        compose.onNodeWithTag("action-backup-done").performClick()

        // Restauração: senha errada, confirmação, processamento e sucesso.
        wait("backup-security")
        picker.opens += backup
        compose.onNodeWithTag("action-backup-restore").performClick()
        wait("input-backup-restore-password")
        compose.onNodeWithTag("input-backup-restore-password").performTextInput("senha errada 123")
        compose.onNodeWithTag("action-backup-restore-submit").performClick()
        wait("backup-working", 5_000)
        shot("8c-restore-abrindo", idle = false)
        waitText("Verifique a senha")
        compose.waitUntil(30_000) { !exists("backup-working") }
        shot("8c-restore-senha-errada")
        compose.onNodeWithTag("input-backup-restore-password").performTextInput("senha do backup")
        compose.onNodeWithTag("action-backup-restore-submit").performClick()
        wait("backup-confirm-sheet")
        shot("8c-restore-confirmacao")
        picker.creates += File(dir, "preventivo.fluxa")
        compose.onNodeWithTag("action-backup-confirm-restore").performClick()
        wait("backup-working", 5_000)
        shot("8c-restore-processando", idle = false)
        wait("backup-restore-success")
        shot("8c-restore-sucesso")
        compose.onNodeWithTag("action-backup-go-home").performClick()
        wait("tab-more")

        // Arquivo inválido.
        compose.onNodeWithTag("tab-more").performClick()
        wait("more-backup")
        compose.onNodeWithTag("more-backup").performClick()
        wait("backup-security")
        picker.opens += File(dir, "nota.fluxa").apply { writeText("não é um backup") }
        compose.onNodeWithTag("action-backup-restore").performClick()
        wait("input-backup-restore-password")
        compose.onNodeWithTag("input-backup-restore-password").performTextInput("senha do backup")
        compose.onNodeWithTag("action-backup-restore-submit").performClick()
        wait("backup-restore-error")
        shot("8c-restore-erro")
    }
}
