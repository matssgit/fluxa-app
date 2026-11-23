package com.fluxa.personal.backup

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import org.junit.Assert.*
import org.junit.Test

class BackupCryptoTest {
    private fun password() = "frase portátil com espaços 🔐".toCharArray()

    @Test
    fun authenticatesUnicodeRoundTripAndGeneratesFreshSaltAndNonce() {
        val payload = "fixture sintética privada".toByteArray(Charsets.UTF_8)
        val a = encrypt(payload)
        val b = encrypt(payload)
        try {
            assertTrue(decrypt(a).contentEquals(payload))
            assertFalse(a.contentEquals(b))
            assertFalse(a.copyOfRange(24, 40).contentEquals(b.copyOfRange(24, 40)))
            assertFalse(a.copyOfRange(40, 52).contentEquals(b.copyOfRange(40, 52)))
            assertEquals(600_000, ByteBuffer.wrap(a, 12, 4).int)
            assertEquals(payload.size + BackupCrypto.TAG_BYTES, ByteBuffer.wrap(a, 20, 4).int)
            assertEquals("FLUXAENC", a.copyOfRange(0, 8).toString(Charsets.US_ASCII))
        } finally {
            payload.fill(0)
        }
    }

    @Test
    fun opensAnIndependentDotNetUtf8Pbkdf2AndAesGcmFixture() {
        val fixture = javaClass.getResourceAsStream("/backup/unicode-v1.fluxa")!!.use { it.readBytes() }
        val secret = "café com açúcar 🔐".toCharArray()
        val plaintext = try { BackupCrypto.decrypt(ByteArrayInputStream(fixture), secret) }
        finally { secret.fill('\u0000') }
        try {
            assertTrue(plaintext.contentEquals("Interoperability fixture v1".toByteArray()))
        } finally {
            plaintext.fill(0)
        }
    }

    @Test
    fun wrongPasswordChangedCiphertextAndChangedTagShareOneAuthenticationError() {
        val encoded = encrypt(ByteArray(100) { it.toByte() })
        val wrong = "outra frase diferente".toCharArray()
        try {
            assertFailure(BackupError.INVALID_PASSWORD) { BackupCrypto.decrypt(ByteArrayInputStream(encoded), wrong) }
        } finally {
            wrong.fill('\u0000')
        }
        for (at in listOf(BackupCrypto.HEADER_BYTES, encoded.lastIndex, 24, 40)) {
            val modified = encoded.copyOf()
            modified[at] = (modified[at].toInt() xor 1).toByte()
            assertFailure(BackupError.INVALID_PASSWORD) { decrypt(modified) }
        }
    }

    @Test
    fun headerRejectsUnknownVersionsAlgorithmsAndInvalidSizesBeforeDerivation() {
        val encoded = encrypt(byteArrayOf(1))
        for (at in listOf(8, 9, 10)) {
            val changed = encoded.copyOf()
            changed[at] = 99
            assertFailure(if (at == 8) BackupError.CONTAINER_VERSION_UNSUPPORTED else BackupError.CRYPTO_UNSUPPORTED) { decrypt(changed) }
        }
        for (at in listOf(11, 16, 17, 18, 19)) {
            val changed = encoded.copyOf()
            changed[at] = 99
            assertFailure(BackupError.CORRUPTED) { decrypt(changed) }
        }
    }

    @Test
    fun maliciousIterationCountsAndLengthsAreBoundedBeforeCpuOrPayloadAllocation() {
        val encoded = encrypt(byteArrayOf(1))
        for (iterations in listOf(Int.MIN_VALUE, -1, 0, 599_999, 1_200_001, Int.MAX_VALUE)) {
            val changed = encoded.copyOf()
            ByteBuffer.wrap(changed, 12, 4).putInt(iterations)
            assertFailure(BackupError.CRYPTO_UNSUPPORTED) { decrypt(changed) }
        }
        for (size in listOf(-1, 0, 15)) {
            val changed = encoded.copyOf()
            ByteBuffer.wrap(changed, 20, 4).putInt(size)
            assertFailure(BackupError.CORRUPTED) { decrypt(changed) }
        }
        val tooLarge = encoded.copyOf()
        ByteBuffer.wrap(tooLarge, 20, 4).putInt(BackupCrypto.MAX_PAYLOAD_BYTES + BackupCrypto.TAG_BYTES + 1)
        assertFailure(BackupError.TOO_LARGE) { decrypt(tooLarge) }
    }

    @Test
    fun everyAcceptedHeaderParameterIsBoundToTheAuthenticationTag() {
        val encoded = encrypt(byteArrayOf(1, 2, 3))
        val changed = encoded.copyOf()
        ByteBuffer.wrap(changed, 12, 4).putInt(BackupCrypto.ITERATIONS + 1)
        assertFailure(BackupError.INVALID_PASSWORD) { decrypt(changed) }
    }

    @Test
    fun truncatedAndTrailingDataAreRejected() {
        val encoded = encrypt(ByteArray(128))
        for (size in listOf(0, 4, 51, encoded.size - 1)) {
            assertFailure(if (size == 0) BackupError.NOT_FLUXA else BackupError.CORRUPTED) { decrypt(encoded.copyOf(size)) }
        }
        assertFailure(BackupError.CORRUPTED) { decrypt(encoded + 0.toByte()) }
        assertFailure(BackupError.NOT_FLUXA) { decrypt(ByteArray(BackupCrypto.HEADER_BYTES)) }
    }

    @Test
    fun validatesEightUnicodeCodePointsAndRejectsMalformedUtf16WithoutNormalization() {
        assertFailure(BackupError.PASSWORD_REQUIRED) { BackupCrypto.validatePassword(charArrayOf()) }
        assertFailure(BackupError.PASSWORD_TOO_SHORT) { BackupCrypto.validatePassword("curta".toCharArray()) }
        assertFailure(BackupError.PASSWORD_TOO_SHORT) { BackupCrypto.validatePassword("🔐🔐🔐🔐".toCharArray()) }
        assertFailure(BackupError.PASSWORD_INVALID_ENCODING) {
            BackupCrypto.validatePassword(charArrayOf('a', 'a', 'a', 'a', 'a', 'a', 'a', '\uD800'))
        }
        val canonical = "café com açúcar".toCharArray()
        val decomposed = "cafe\u0301 com ac\u0327u\u0301car".toCharArray()
        try {
            val output = ByteArrayOutputStream()
            BackupCrypto.encrypt(byteArrayOf(1), canonical, output)
            assertFailure(BackupError.INVALID_PASSWORD) { BackupCrypto.decrypt(ByteArrayInputStream(output.toByteArray()), decomposed) }
        } finally {
            canonical.fill('\u0000')
            decomposed.fill('\u0000')
        }
    }

    @Test
    fun allowsLongPassphrasesAndAuthenticatesEmptyPlaintext() {
        val secret = ("uma frase longa com espaços ".repeat(100)).toCharArray()
        try {
            val out = ByteArrayOutputStream()
            BackupCrypto.encrypt(byteArrayOf(), secret, out)
            val result = BackupCrypto.decrypt(ByteArrayInputStream(out.toByteArray()), secret)
            assertEquals(0, result.size)
        } finally {
            secret.fill('\u0000')
        }
    }

    @Test
    fun encryptedPayloadCannotExceedTheExistingArchiveBudget() {
        assertFailure(BackupError.TOO_LARGE) { encrypt(ByteArray(BackupCrypto.MAX_PAYLOAD_BYTES + 1)) }
    }

    @Test
    fun distinguishesForeignFilesFutureContainersAndTruncatedRecognizedHeaders() {
        assertFailure(BackupError.NOT_FLUXA) { decrypt("other file".toByteArray()) }
        assertFailure(BackupError.NOT_FLUXA) { decrypt(byteArrayOf()) }
        assertFailure(BackupError.CORRUPTED) { decrypt("FLUXA".toByteArray()) }
        assertFailure(BackupError.CORRUPTED) { decrypt("FLUXAENC".toByteArray()) }
        assertFailure(BackupError.CONTAINER_VERSION_UNSUPPORTED) { decrypt("FLUXAENC".toByteArray() + 2.toByte()) }
    }

    private fun encrypt(payload: ByteArray): ByteArray {
        val output = ByteArrayOutputStream()
        val secret = password()
        try { BackupCrypto.encrypt(payload, secret, output) }
        finally { secret.fill('\u0000') }
        return output.toByteArray()
    }

    private fun decrypt(encoded: ByteArray): ByteArray {
        val secret = password()
        return try { BackupCrypto.decrypt(ByteArrayInputStream(encoded), secret) }
        finally { secret.fill('\u0000') }
    }

    private fun assertFailure(expected: BackupError, block: () -> Unit) {
        val error = runCatching(block).exceptionOrNull()
        assertTrue("Expected a typed backup error", error is BackupException)
        assertEquals(expected, (error as BackupException).error)
    }
}
