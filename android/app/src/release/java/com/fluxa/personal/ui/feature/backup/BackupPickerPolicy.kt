package com.fluxa.personal.ui.feature.backup

/** Release only ever launches the system Storage Access Framework. */
internal fun backupPicker(): BackupPicker = SystemBackupPicker
