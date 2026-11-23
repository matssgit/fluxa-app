package com.fluxa.personal.security

import androidx.lifecycle.ViewModel

/** ActivityResult callbacks survive Activity recreation; this lease must do so in memory as well. */
internal class TrustedExternalActivity(private val manager: LocalSecurityManager) : ViewModel() {
    private var lease: ExternalActivityLease? = null

    fun launch(block: () -> Unit): Boolean {
        if (lease != null) return false
        lease = manager.beginExternalActivity() ?: return false
        try { block() } catch (failure: Throwable) {
            finish()
            throw failure
        }
        return true
    }

    fun finish() {
        lease?.let(manager::finishExternalActivity)
        lease = null
    }

    override fun onCleared() {
        lease?.let(manager::abandonExternalActivity)
        lease = null
    }
}
