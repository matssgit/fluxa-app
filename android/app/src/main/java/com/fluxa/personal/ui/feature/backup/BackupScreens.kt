package com.fluxa.personal.ui.feature.backup

import android.content.ActivityNotFoundException
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.platform.LocalContext
import com.fluxa.personal.FluxaApplication
import com.fluxa.personal.security.TrustedExternalActivity
import com.fluxa.personal.R
import kotlinx.coroutines.delay
import com.fluxa.personal.backup.FLUXA_BACKUP_MIME
import com.fluxa.personal.ui.common.ErrorBanner
import com.fluxa.personal.ui.common.FeatureScreen
import com.fluxa.personal.ui.common.StatePill
import com.fluxa.personal.ui.common.formatLocal
import com.fluxa.personal.ui.designsystem.FluxaIcons
import com.fluxa.personal.ui.designsystem.FluxaTheme
import com.fluxa.personal.ui.designsystem.components.FluxaBottomSheet
import com.fluxa.personal.ui.designsystem.components.FluxaButton
import com.fluxa.personal.ui.designsystem.components.FluxaButtonVariant
import com.fluxa.personal.ui.designsystem.components.FluxaCard
import com.fluxa.personal.ui.designsystem.components.FluxaIconButton
import com.fluxa.personal.ui.designsystem.components.FluxaInput
import com.fluxa.personal.ui.feature.backup.BackupUiState as S

// region Seletor do Android (SAF)

/**
 * Abstração do Storage Access Framework. A implementação real usa a Activity Result API
 * (`ACTION_CREATE_DOCUMENT` / `ACTION_OPEN_DOCUMENT`), sem permissão de armazenamento amplo.
 * Os testes instrumentados fornecem outra implementação via [BackupPickerTestHook].
 */
internal interface BackupPicker {
    /** Retorna a função que abre o "Salvar como" com o nome sugerido. */
    @Composable
    fun rememberCreate(onResult: (DocumentRef?) -> Unit): (suggestedName: String) -> Unit

    @Composable
    fun rememberOpen(onResult: (DocumentRef?) -> Unit): () -> Unit
}

internal object SystemBackupPicker : BackupPicker {
    /**
     * Provedores locais costumam classificar extensões desconhecidas como `application/octet-stream`;
     * por isso o filtro inclui esse tipo além do MIME próprio do Fluxa.
     */
    private val openTypes = arrayOf(FLUXA_BACKUP_MIME, "application/octet-stream")

    @Composable
    override fun rememberCreate(onResult: (DocumentRef?) -> Unit): (String) -> Unit {
        val external = rememberTrustedBackupActivity()
        val callback by rememberUpdatedState(onResult)
        val launcher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument(FLUXA_BACKUP_MIME)) { uri ->
            external.finish()
            callback(uri?.let { DocumentRef(it.toString()) })
        }
        return remember(launcher, external) {
            { name ->
                try { if (!external.launch { launcher.launch(name) }) callback(null) }
                catch (_: ActivityNotFoundException) { callback(null) }
            }
        }
    }

    @Composable
    override fun rememberOpen(onResult: (DocumentRef?) -> Unit): () -> Unit {
        val external = rememberTrustedBackupActivity()
        val callback by rememberUpdatedState(onResult)
        val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            external.finish()
            callback(uri?.let { DocumentRef(it.toString()) })
        }
        return remember(launcher, external) {
            {
                try { if (!external.launch { launcher.launch(openTypes) }) callback(null) }
                catch (_: ActivityNotFoundException) { callback(null) }
            }
        }
    }
}

@Composable
private fun rememberTrustedBackupActivity(): TrustedExternalActivity {
    val app = LocalContext.current.applicationContext as FluxaApplication
    return viewModel(
        key = "backup-trusted-external-activity",
        factory = viewModelFactory {
            initializer { TrustedExternalActivity(app.security) }
        },
    )
}

// endregion

@Composable
internal fun BackupScreen(
    viewModel: BackupRestoreViewModel,
    onBack: () -> Unit,
    onGoHome: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val picker = remember { backupPicker() }
    val createExport = picker.rememberCreate(viewModel::onExportDestination)
    val createPreventive = picker.rememberCreate(viewModel::onPreventiveDestination)
    val openFile = picker.rememberOpen(viewModel::onRestoreFile)

    val back: () -> Unit = {
        when (state) {
            is S.Working -> Unit // bloqueado: a operação não pode ser interrompida pela navegação
            S.Home -> onBack()
            is S.RestoreSuccess -> onGoHome()
            else -> viewModel.cancel()
        }
    }
    BackHandler(enabled = state != S.Home, onBack = back)

    val restoring = state is S.ChoosingFile || state is S.RestorePassword || state is S.RestoreConfirm ||
        state is S.ChoosingPreventiveDestination || state is S.RestoreSuccess || state is S.RestoreError ||
        (state as? S.Working)?.work?.let { it != BackupWork.EXPORTING } == true
    val exporting = state is S.ExportPassword || state is S.ChoosingDestination || state is S.ExportSuccess ||
        (state as? S.Working)?.work == BackupWork.EXPORTING
    val title = stringResource(
        when {
            exporting -> R.string.backup_export_title
            restoring && state !is S.ChoosingFile -> R.string.backup_restore_title
            else -> R.string.backup_title
        },
    )

    // Campos de senha só em memória da composição (nunca rememberSaveable): não sobrevivem à morte do processo.
    var exportPassword by remember { mutableStateOf("") }
    var exportConfirmation by remember { mutableStateOf("") }
    var restorePassword by remember { mutableStateOf("") }
    val attempt = when (val s = state) {
        is S.ExportPassword -> s.attempt
        is S.ChoosingDestination -> s.attempt
        is S.RestorePassword -> s.attempt
        else -> null
    }
    // Sucesso, erro definitivo, cancelamento (novo attempt) ou saída do formulário: limpa os campos.
    LaunchedEffect(attempt) {
        exportPassword = ""
        exportConfirmation = ""
        restorePassword = ""
    }

    // Enviar fecha o teclado: o formulário inteiro (com a mensagem de erro, se houver) fica visível.
    val keyboard = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val dismissKeyboard = {
        focusManager.clearFocus()
        keyboard?.hide()
    }
    val submitExport = {
        dismissKeyboard()
        if (viewModel.submitExport(exportPassword.toCharArray(), exportConfirmation.toCharArray())) {
            createExport(viewModel.suggestedExportName)
        }
    }
    val submitRestore = {
        dismissKeyboard()
        viewModel.submitRestorePassword(restorePassword.toCharArray())
    }

    FeatureScreen(
        title = title,
        overline = stringResource(R.string.backup_overline),
        onBack = back,
        backEnabled = !state.isWorking,
        modifier = Modifier.testTag("screen-backup"),
        bottomAction = bottomActionFor(
            state = state,
            onSubmitExport = submitExport,
            onSubmitRestore = submitRestore,
            onFinish = viewModel::cancel,
            onGoHome = onGoHome,
            onChooseOther = {
                viewModel.cancel()
                if (viewModel.startRestore()) openFile()
            },
        ),
    ) {
        when (val s = state) {
            S.Home, S.ChoosingFile -> homeContent(
                enabled = s == S.Home,
                onExport = viewModel::startExport,
                onRestore = { if (viewModel.startRestore()) openFile() },
            )
            is S.ExportPassword, is S.ChoosingDestination -> item(key = "export-form") {
                ExportForm(
                    password = exportPassword,
                    onPassword = { exportPassword = it },
                    confirmation = exportConfirmation,
                    onConfirmation = { exportConfirmation = it },
                    state = s,
                    onSubmit = submitExport,
                    onDismissBanner = viewModel::dismissBanner,
                )
            }
            is S.Working -> item(key = "working") { WorkingCard(s.work) }
            is S.ExportSuccess -> item(key = "export-success") { ExportSuccessCard(s) }
            is S.RestorePassword -> item(key = "restore-form") {
                RestorePasswordForm(password = restorePassword, onPassword = { restorePassword = it }, state = s, onSubmit = submitRestore)
            }
            is S.RestoreConfirm -> item(key = "restore-ready") { VerifiedBackupCard(s.fileName, s.createdAt) }
            is S.ChoosingPreventiveDestination -> item(key = "restore-ready") { VerifiedBackupCard(s.fileName, s.createdAt) }
            is S.RestoreSuccess -> item(key = "restore-success") { RestoreSuccessCard(s.settingsApplied) }
            is S.RestoreError -> item(key = "restore-error") { RestoreErrorCard(s.message) }
        }
    }

    (state as? S.RestoreConfirm)?.let { confirm ->
        ReplaceConfirmationSheet(
            state = confirm,
            onConfirm = { if (viewModel.confirmRestore()) createPreventive(viewModel.suggestedPreventiveName) },
            onDismiss = viewModel::cancel,
        )
    }
}

private fun bottomActionFor(
    state: S,
    onSubmitExport: () -> Unit,
    onSubmitRestore: () -> Unit,
    onFinish: () -> Unit,
    onGoHome: () -> Unit,
    onChooseOther: () -> Unit,
): (@Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit)? = when (state) {
    is S.ExportPassword, is S.ChoosingDestination -> {
        {
            FluxaButton(
                text = stringResource(R.string.backup_export_action),
                onClick = onSubmitExport,
                enabled = state is S.ExportPassword,
                leadingIcon = FluxaIcons.Download,
                modifier = Modifier.fillMaxWidth().testTag("action-backup-export-submit"),
            )
        }
    }
    is S.RestorePassword -> {
        {
            FluxaButton(
                text = stringResource(R.string.backup_continue),
                onClick = onSubmitRestore,
                modifier = Modifier.fillMaxWidth().testTag("action-backup-restore-submit"),
            )
        }
    }
    is S.ExportSuccess -> {
        {
            FluxaButton(
                text = stringResource(R.string.backup_done),
                onClick = onFinish,
                modifier = Modifier.fillMaxWidth().testTag("action-backup-done"),
            )
        }
    }
    is S.RestoreSuccess -> {
        {
            FluxaButton(
                text = stringResource(R.string.backup_go_home),
                onClick = onGoHome,
                leadingIcon = FluxaIcons.Home,
                modifier = Modifier.fillMaxWidth().testTag("action-backup-go-home"),
            )
        }
    }
    is S.RestoreError -> {
        {
            FluxaButton(
                text = stringResource(R.string.backup_choose_other),
                onClick = onChooseOther,
                modifier = Modifier.fillMaxWidth().testTag("action-backup-choose-other"),
            )
            FluxaButton(
                text = stringResource(R.string.action_back),
                onClick = onFinish,
                variant = FluxaButtonVariant.Secondary,
                modifier = Modifier.fillMaxWidth().testTag("action-backup-error-back"),
            )
        }
    }
    else -> null
}

// region Início da tela

private fun androidx.compose.foundation.lazy.LazyListScope.homeContent(enabled: Boolean, onExport: () -> Unit, onRestore: () -> Unit) {
    item(key = "security") { SecurityCard() }
    item(key = "actions") {
        FluxaCard(modifier = Modifier.fillMaxWidth(), contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 4.dp)) {
            ActionRow(
                icon = FluxaIcons.Download,
                title = stringResource(R.string.backup_export_title),
                body = stringResource(R.string.backup_export_body),
                enabled = enabled,
                onClick = onExport,
                modifier = Modifier.testTag("action-backup-export"),
            )
            Box(Modifier.padding(start = 72.dp).fillMaxWidth().heightIn(min = 1.dp, max = 1.dp).background(FluxaTheme.colors.border))
            ActionRow(
                icon = FluxaIcons.Restore,
                title = stringResource(R.string.backup_restore_title),
                body = stringResource(R.string.backup_restore_body),
                enabled = enabled,
                onClick = onRestore,
                modifier = Modifier.testTag("action-backup-restore"),
            )
        }
    }
    item(key = "manual") {
        Text(
            stringResource(R.string.backup_manual_note),
            style = FluxaTheme.typography.caption,
            color = FluxaTheme.colors.textMuted,
            modifier = Modifier.padding(horizontal = 4.dp),
        )
    }
}

@Composable
private fun SecurityCard() {
    val colors = FluxaTheme.colors
    FluxaCard(modifier = Modifier.fillMaxWidth().testTag("backup-security")) {
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.Top) {
            IconBadge(FluxaIcons.Lock)
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(stringResource(R.string.backup_security_title), style = FluxaTheme.typography.titleSmall, color = colors.textPrimary, modifier = Modifier.semantics { heading() })
                Text(stringResource(R.string.backup_security_body), style = FluxaTheme.typography.body, color = colors.textSecondary)
                Text(stringResource(R.string.backup_security_note), style = FluxaTheme.typography.label, color = colors.textPrimary, modifier = Modifier.padding(top = 4.dp))
            }
        }
    }
}

@Composable
private fun ActionRow(icon: ImageVector, title: String, body: String, enabled: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = FluxaTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .heightIn(min = 72.dp)
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .semantics(mergeDescendants = true) {},
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        IconBadge(icon)
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = FluxaTheme.typography.bodyStrong, color = colors.textPrimary)
            Text(body, style = FluxaTheme.typography.caption, color = colors.textSecondary)
            StatePill(stringResource(R.string.backup_file_type), icon = FluxaIcons.Lock, modifier = Modifier.padding(top = 4.dp))
        }
        Icon(FluxaIcons.ChevronRight, contentDescription = null, tint = colors.textMuted, modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun IconBadge(icon: ImageVector, background: Color = FluxaTheme.colors.brandTonal, tint: Color = FluxaTheme.colors.onBrandTonal) {
    Box(modifier = Modifier.size(40.dp).background(background, FluxaTheme.shapes.pill), contentAlignment = Alignment.Center) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
    }
}

// endregion

// region Exportação

@Composable
private fun ExportForm(
    password: String,
    onPassword: (String) -> Unit,
    confirmation: String,
    onConfirmation: (String) -> Unit,
    state: S,
    onSubmit: () -> Unit,
    onDismissBanner: () -> Unit,
) {
    val form = state as? S.ExportPassword
    val enabled = form != null
    val passwordFocus = remember { FocusRequester() }
    val confirmationFocus = remember { FocusRequester() }
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        form?.banner?.let { ErrorBanner(stringResource(it), onDismiss = onDismissBanner) }
        Text(stringResource(R.string.backup_export_intro), style = FluxaTheme.typography.body, color = FluxaTheme.colors.textSecondary)
        PasswordWarning()
        PasswordField(
            value = password,
            onValueChange = onPassword,
            label = stringResource(R.string.backup_password_label),
            tag = "input-backup-password",
            errorText = form?.passwordError?.let { stringResource(it) },
            supportingText = stringResource(R.string.backup_password_supporting),
            enabled = enabled,
            imeAction = ImeAction.Next,
            onIme = { confirmationFocus.requestFocus() },
            focusRequester = passwordFocus,
        )
        PasswordField(
            value = confirmation,
            onValueChange = onConfirmation,
            label = stringResource(R.string.backup_password_confirm_label),
            tag = "input-backup-password-confirm",
            errorText = form?.confirmationError?.let { stringResource(it) },
            supportingText = null,
            enabled = enabled,
            imeAction = ImeAction.Done,
            onIme = onSubmit,
            focusRequester = confirmationFocus,
        )
        Text(stringResource(R.string.backup_export_destination_note), style = FluxaTheme.typography.caption, color = FluxaTheme.colors.textMuted)
    }
}

/** Aviso crítico, sempre visível no formulário (não depende de tooltip). */
@Composable
private fun PasswordWarning() {
    val colors = FluxaTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.warningContainer, FluxaTheme.shapes.medium)
            .padding(16.dp)
            .semantics(mergeDescendants = true) {}
            .testTag("backup-password-warning"),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(FluxaIcons.Alert, contentDescription = null, tint = colors.warning, modifier = Modifier.size(20.dp))
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(stringResource(R.string.backup_warning_title), style = FluxaTheme.typography.bodyStrong, color = colors.textPrimary)
            Text(stringResource(R.string.backup_warning_body), style = FluxaTheme.typography.body, color = colors.textPrimary)
        }
    }
}

/**
 * Traz o bloco inteiro (campo + mensagem de erro) para a área visível quando um erro aparece, inclusive em telas
 * baixas com fonte ampliada, e o anuncia. Reinicia a cada passo da animação do teclado para agir depois dela.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun RevealOnError(key: Any?, announce: Boolean = true, content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    val requester = remember { BringIntoViewRequester() }
    // Reinicia a cada passo da animação do teclado: só revela quando a altura do teclado estabiliza.
    val imeBottom = WindowInsets.ime.getBottom(LocalDensity.current)
    LaunchedEffect(key, imeBottom) {
        if (key != null) {
            delay(120)
            requester.bringIntoView()
        }
    }
    Column(
        modifier = Modifier
            .bringIntoViewRequester(requester)
            // Com erro, o bloco é uma região viva: o TalkBack anuncia a mensagem quando ela aparece.
            .semantics { if (key != null && announce) liveRegion = LiveRegionMode.Assertive },
        verticalArrangement = Arrangement.spacedBy(12.dp),
        content = content,
    )
}

@Composable
private fun PasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    tag: String,
    errorText: String?,
    supportingText: String?,
    enabled: Boolean,
    imeAction: ImeAction,
    onIme: () -> Unit,
    focusRequester: FocusRequester,
) {
    // Mostrar/ocultar é só um estado visual: o texto continua sem ir para o SavedState.
    var visible by rememberSaveable { mutableStateOf(false) }
    RevealOnError(errorText) {
        FluxaInput(
            value = value,
            onValueChange = onValueChange,
            label = label,
            modifier = Modifier.focusRequester(focusRequester).testTag(tag),
            supportingText = supportingText,
            errorText = errorText,
            leadingIcon = FluxaIcons.Lock,
            enabled = enabled,
            trailing = {
                FluxaIconButton(
                    icon = if (visible) FluxaIcons.EyeOff else FluxaIcons.Eye,
                    contentDescription = stringResource(if (visible) R.string.backup_password_hide else R.string.backup_password_show),
                    onClick = { visible = !visible },
                    enabled = enabled,
                    modifier = Modifier.testTag("$tag-visibility"),
                )
            },
            // Teclado de senha: sem sugestões nem aprendizado. Nenhum contentType de autofill é declarado.
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = imeAction, autoCorrect = false),
            keyboardActions = KeyboardActions(onNext = { onIme() }, onDone = { onIme() }),
            visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        )
    }
}

@Composable
private fun ExportSuccessCard(state: S.ExportSuccess) {
    val colors = FluxaTheme.colors
    ResultCard(
        icon = FluxaIcons.Check,
        badge = colors.successContainer,
        tint = colors.success,
        title = stringResource(R.string.backup_export_success_title),
        tag = "backup-export-success",
    ) {
        state.fileName?.let {
            Text(stringResource(R.string.backup_export_success_file, it), style = FluxaTheme.typography.label, color = colors.textPrimary, modifier = Modifier.testTag("backup-export-file"))
        }
        Text(stringResource(R.string.backup_export_success_date, state.createdAt.formatLocal()), style = FluxaTheme.typography.body, color = colors.textSecondary)
        Text(stringResource(R.string.backup_export_success_note), style = FluxaTheme.typography.caption, color = colors.textMuted, modifier = Modifier.padding(top = 4.dp))
    }
}

// endregion

// region Restauração

@Composable
private fun RestorePasswordForm(password: String, onPassword: (String) -> Unit, state: S.RestorePassword, onSubmit: () -> Unit) {
    val focus = remember { FocusRequester() }
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        FileLine(state.fileName)
        Text(stringResource(R.string.backup_restore_password_intro), style = FluxaTheme.typography.body, color = FluxaTheme.colors.textSecondary)
        val wrongPassword = state.error == R.string.backup_error_wrong_password
        RevealOnError(if (wrongPassword) state.attempt else null, announce = false) {
            // Acima do campo: com o teclado aberto, o bloco é alinhado pelo topo e a mensagem fica visível.
            // Anunciada pelo TalkBack (liveRegion do ErrorBanner).
            if (wrongPassword) ErrorBanner(stringResource(R.string.backup_error_wrong_password), onDismiss = null)
            PasswordField(
                value = password,
                onValueChange = onPassword,
                label = stringResource(R.string.backup_restore_password_title),
                tag = "input-backup-restore-password",
                errorText = state.error?.takeIf { !wrongPassword }?.let { stringResource(it) },
                supportingText = null,
                enabled = true,
                imeAction = ImeAction.Done,
                onIme = onSubmit,
                focusRequester = focus,
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Top) {
            Icon(FluxaIcons.Lock, contentDescription = null, tint = FluxaTheme.colors.textMuted, modifier = Modifier.size(16.dp).padding(top = 2.dp))
            Text(stringResource(R.string.backup_restore_password_note), style = FluxaTheme.typography.caption, color = FluxaTheme.colors.textMuted)
        }
    }
}

@Composable
private fun FileLine(fileName: String?) {
    val colors = FluxaTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.surfaceSunken, FluxaTheme.shapes.medium)
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .semantics(mergeDescendants = true) {}
            .testTag("backup-file"),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(FluxaIcons.Lock, contentDescription = null, tint = colors.textSecondary, modifier = Modifier.size(20.dp))
        Text(
            if (fileName != null) stringResource(R.string.backup_restore_file, fileName) else stringResource(R.string.backup_restore_file_unknown),
            style = FluxaTheme.typography.label,
            color = colors.textPrimary,
        )
    }
}

@Composable
private fun VerifiedBackupCard(fileName: String?, createdAt: java.time.Instant) {
    val colors = FluxaTheme.colors
    ResultCard(
        icon = FluxaIcons.Check,
        badge = colors.brandTonal,
        tint = colors.onBrandTonal,
        title = stringResource(R.string.backup_confirm_ready),
        tag = "backup-verified",
        announce = false,
    ) {
        if (fileName != null) Text(fileName, style = FluxaTheme.typography.label, color = colors.textPrimary)
        Text(stringResource(R.string.backup_confirm_created, createdAt.formatLocal()), style = FluxaTheme.typography.body, color = colors.textSecondary)
    }
}

/** Confirmação destrutiva (padrão de sheet do DS). Só REPLACE: não há mesclar nem importar junto. */
@Composable
private fun ReplaceConfirmationSheet(state: S.RestoreConfirm, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    val colors = FluxaTheme.colors
    FluxaBottomSheet(title = stringResource(R.string.backup_confirm_title), onDismissRequest = onDismiss, modifier = Modifier.testTag("backup-confirm-sheet")) {
        Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            state.error?.let { ErrorBanner(stringResource(it), onDismiss = null) }
            Text(stringResource(R.string.backup_confirm_body), style = FluxaTheme.typography.body, color = colors.textPrimary)
            Text(stringResource(R.string.backup_confirm_preventive), style = FluxaTheme.typography.bodyStrong, color = colors.textPrimary)
            Text(stringResource(R.string.backup_confirm_preventive_detail), style = FluxaTheme.typography.caption, color = colors.textSecondary)
            FluxaButton(
                text = stringResource(R.string.backup_confirm_action),
                onClick = onConfirm,
                variant = FluxaButtonVariant.Destructive,
                leadingIcon = FluxaIcons.Restore,
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp).testTag("action-backup-confirm-restore"),
            )
            FluxaButton(
                text = stringResource(R.string.action_cancel),
                onClick = onDismiss,
                variant = FluxaButtonVariant.Secondary,
                modifier = Modifier.fillMaxWidth().testTag("action-backup-confirm-cancel"),
            )
        }
    }
}

@Composable
private fun RestoreSuccessCard(settingsApplied: Boolean) {
    val colors = FluxaTheme.colors
    ResultCard(
        icon = FluxaIcons.Check,
        badge = colors.successContainer,
        tint = colors.success,
        title = stringResource(R.string.backup_restore_success_title),
        tag = "backup-restore-success",
    ) {
        Text(stringResource(R.string.backup_restore_success_body), style = FluxaTheme.typography.body, color = colors.textSecondary)
        if (!settingsApplied) {
            Row(
                modifier = Modifier.fillMaxWidth().background(colors.warningContainer, FluxaTheme.shapes.medium).padding(12.dp).testTag("backup-settings-warning"),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Icon(FluxaIcons.Alert, contentDescription = null, tint = colors.warning, modifier = Modifier.size(18.dp))
                Text(stringResource(R.string.backup_restore_settings_warning), style = FluxaTheme.typography.label, color = colors.textPrimary)
            }
        }
    }
}

@Composable
private fun RestoreErrorCard(message: Int) {
    val colors = FluxaTheme.colors
    ResultCard(
        icon = FluxaIcons.Alert,
        badge = colors.expenseContainer,
        tint = colors.expense,
        title = stringResource(R.string.backup_error_title),
        tag = "backup-restore-error",
        assertive = true,
    ) {
        Text(stringResource(message), style = FluxaTheme.typography.body, color = colors.textPrimary)
    }
}

// endregion

@Composable
private fun WorkingCard(work: BackupWork) {
    val colors = FluxaTheme.colors
    val (title, body) = when (work) {
        BackupWork.EXPORTING -> R.string.backup_working_export to R.string.backup_working_export_body
        BackupWork.UNLOCKING -> R.string.backup_working_unlock to R.string.backup_working_unlock_body
        BackupWork.RESTORING -> R.string.backup_working_restore to R.string.backup_working_restore_body
    }
    // Indeterminado: o backend não informa progresso real, então não há porcentagem.
    FluxaCard(
        modifier = Modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite }
            .testTag("backup-working"),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            CircularProgressIndicator(color = colors.brand, modifier = Modifier.size(36.dp))
            Text(stringResource(title), style = FluxaTheme.typography.titleSmall, color = colors.textPrimary, textAlign = TextAlign.Center, modifier = Modifier.testTag("backup-working-title"))
            Text(stringResource(body), style = FluxaTheme.typography.body, color = colors.textSecondary, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun ResultCard(
    icon: ImageVector,
    badge: Color,
    tint: Color,
    title: String,
    tag: String,
    announce: Boolean = true,
    assertive: Boolean = false,
    content: @Composable () -> Unit,
) {
    FluxaCard(
        modifier = Modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {
                if (announce) liveRegion = if (assertive) LiveRegionMode.Assertive else LiveRegionMode.Polite
            }
            .testTag(tag),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.Top) {
            IconBadge(icon, background = badge, tint = tint)
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(title, style = FluxaTheme.typography.titleSmall, color = FluxaTheme.colors.textPrimary, modifier = Modifier.semantics { heading() })
                content()
            }
        }
    }
}
