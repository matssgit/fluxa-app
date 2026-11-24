package com.fluxa.personal.ui

/** Mutable prompt control exists only in the debug APK used by instrumentation. */
internal object SecurityGateTestHook {
    @Volatile var autoPrompt: Boolean = true
}
internal fun autoPromptEnabled(): Boolean = SecurityGateTestHook.autoPrompt
