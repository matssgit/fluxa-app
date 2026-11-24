package com.fluxa.personal.ui.feature.backup

/** Instrumentation-only picker replacement; absent from release. */
internal object BackupPickerTestHook {
    @Volatile var override: BackupPicker? = null
}
internal fun backupPicker(): BackupPicker = BackupPickerTestHook.override ?: SystemBackupPicker
