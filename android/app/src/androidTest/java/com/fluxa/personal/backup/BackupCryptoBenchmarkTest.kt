package com.fluxa.personal.backup

import android.os.SystemClock
import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BackupCryptoBenchmarkTest {
    @Test
    fun androidDecryptsTheIndependentUtf8Fixture() {
        val fixture = InstrumentationRegistry.getInstrumentation().context.assets.open("backup/unicode-v1.fluxa")
        val secret = "café com açúcar 🔐".toCharArray()
        val plaintext = try { fixture.use { BackupCrypto.decrypt(it, secret) } }
        finally { secret.fill('\u0000') }
        try {
            assertTrue(plaintext.contentEquals("Interoperability fixture v1".toByteArray()))
        } finally {
            plaintext.fill(0)
        }
    }

    @Test
    fun measuresSmallAndMaximumContainersWithTheFixedNativeKdf() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        for (size in listOf(1024, BackupCrypto.MAX_PAYLOAD_BYTES)) {
            val secret = "frase sintética do benchmark 🔐".toCharArray()
            val salt = ByteArray(16).also(SecureRandom()::nextBytes)
            val spec = PBEKeySpec(secret, salt, BackupCrypto.ITERATIONS, 256)
            val startKdf = SystemClock.elapsedRealtimeNanos()
            try {
                val key = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
                key.fill(0)
            } finally {
                spec.clearPassword()
            }
            val kdfMs = (SystemClock.elapsedRealtimeNanos() - startKdf) / 1_000_000
            // Only ciphertext is persisted, in private cache, and it is removed in finally.
            val file = File.createTempFile("crypto-benchmark-", ".fluxa", context.cacheDir)
            try {
                val encryptMs = encryptSynthetic(file, size, secret)
                val startDecrypt = SystemClock.elapsedRealtimeNanos()
                val decoded = file.inputStream().use { BackupCrypto.decrypt(it, secret) }
                val decryptMs = (SystemClock.elapsedRealtimeNanos() - startDecrypt) / 1_000_000
                try {
                    assertEquals(size, decoded.size)
                    assertTrue("Synthetic payload must round-trip", decoded.indices.all { decoded[it] == (it % 251).toByte() })
                    assertEquals(size.toLong() + BackupCrypto.HEADER_BYTES + BackupCrypto.TAG_BYTES, file.length())
                    // Synthetic size and elapsed times only. No passwords, keys or backup contents.
                    Log.i("FluxaBackupBenchmark", "bytes=$size kdfMs=$kdfMs encryptTotalMs=$encryptMs decryptTotalMs=$decryptMs")
                } finally {
                    decoded.fill(0)
                }
            } finally {
                secret.fill('\u0000')
                assertTrue("Encrypted benchmark file must be removed", file.delete())
            }
        }
    }

    private fun encryptSynthetic(file: File, size: Int, password: CharArray): Long {
        val payload = ByteArray(size) { (it % 251).toByte() }
        val start = SystemClock.elapsedRealtimeNanos()
        try {
            file.outputStream().use { BackupCrypto.encrypt(payload, password, it) }
            return (SystemClock.elapsedRealtimeNanos() - start) / 1_000_000
        } finally {
            payload.fill(0)
        }
    }
}
