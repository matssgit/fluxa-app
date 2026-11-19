package com.fluxa.personal.backup

import java.io.InputStream
import java.io.OutputStream
import java.nio.ByteBuffer
import java.nio.CharBuffer
import java.nio.charset.CodingErrorAction
import java.security.GeneralSecurityException
import java.security.NoSuchAlgorithmException
import java.security.SecureRandom
import javax.crypto.BadPaddingException
import javax.crypto.Cipher
import javax.crypto.NoSuchPaddingException
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/** Portable, authenticated container v1. No financial metadata is stored in its header. */
internal object BackupCrypto {
    const val ITERATIONS = 600_000
    const val MAX_ITERATIONS = 1_200_000
    const val HEADER_BYTES = 52
    const val TAG_BYTES = 16
    const val MAX_PAYLOAD_BYTES = BackupCodec.MAX_DATA_BYTES + 64 * 1024
    private const val BUFFER_BYTES = 64 * 1024
    private val magic = "FLUXAENC".toByteArray(Charsets.US_ASCII)
    private val random = SecureRandom()

    /** Caller owns password and payload buffers. The service clears them after this call. */
    fun encrypt(payload: ByteArray, password: CharArray, output: OutputStream) {
        validatePassword(password)
        if (payload.size > MAX_PAYLOAD_BYTES) throw BackupException(BackupError.TOO_LARGE)
        val salt = ByteArray(16).also(random::nextBytes)
        val nonce = ByteArray(12).also(random::nextBytes)
        val header = ByteBuffer.allocate(HEADER_BYTES)
            .put(magic).put(1.toByte()).put(1.toByte()).put(1.toByte()).put(0.toByte())
            .putInt(ITERATIONS).put(16.toByte()).put(12.toByte()).put(TAG_BYTES.toByte()).put(0.toByte())
            .putInt(payload.size + TAG_BYTES).put(salt).put(nonce).array()
        val inputBuffer = ByteBuffer.allocateDirect(payload.size)
        val encrypted = ByteBuffer.allocateDirect(payload.size + TAG_BYTES)
        val chunk = ByteArray(BUFFER_BYTES)
        try {
            val cipher = cipher(Cipher.ENCRYPT_MODE, password, salt, nonce, ITERATIONS)
            cipher.updateAAD(header)
            inputBuffer.put(payload).flip()
            // Conscrypt's ByteBuffer path avoids accumulating another full message on the Java heap.
            cipher.doFinal(inputBuffer, encrypted)
            encrypted.flip()
            output.write(header)
            while (encrypted.hasRemaining()) {
                val count = minOf(chunk.size, encrypted.remaining())
                encrypted.get(chunk, 0, count)
                output.write(chunk, 0, count)
            }
        } catch (error: GeneralSecurityException) {
            throw BackupException(cryptoError(error, BackupError.ENCRYPTION_FAILED))
        } finally {
            wipe(inputBuffer)
            wipe(encrypted)
            chunk.fill(0)
        }
    }

    /** Returns plaintext only after GCM authentication succeeds. */
    fun decrypt(input: InputStream, password: CharArray): ByteArray {
        validatePassword(password)
        val header = ByteArray(HEADER_BYTES)
        readFully(input, header, 0, HEADER_BYTES)
        if (!header.copyOfRange(0, 8).contentEquals(magic)) throw BackupException(BackupError.CRYPTO_UNSUPPORTED)
        if (header[8].toInt() != 1 || header[9].toInt() != 1 || header[10].toInt() != 1) {
            throw BackupException(BackupError.CRYPTO_UNSUPPORTED)
        }
        if (header[11].toInt() != 0 || header[16].toInt() != 16 || header[17].toInt() != 12 ||
            header[18].toInt() != TAG_BYTES || header[19].toInt() != 0
        ) throw BackupException(BackupError.CORRUPTED)
        val iterations = ByteBuffer.wrap(header, 12, 4).int
        if (iterations !in ITERATIONS..MAX_ITERATIONS) throw BackupException(BackupError.CRYPTO_UNSUPPORTED)
        val encryptedSize = ByteBuffer.wrap(header, 20, 4).int
        if (encryptedSize < TAG_BYTES) throw BackupException(BackupError.CORRUPTED)
        if (encryptedSize.toLong() > MAX_PAYLOAD_BYTES.toLong() + TAG_BYTES) throw BackupException(BackupError.TOO_LARGE)
        val encrypted = ByteBuffer.allocateDirect(encryptedSize)
        val plaintext = ByteBuffer.allocateDirect(encryptedSize - TAG_BYTES)
        val chunk = ByteArray(BUFFER_BYTES)
        try {
            var remaining = encryptedSize
            while (remaining > 0) {
                val count = minOf(chunk.size, remaining)
                readFully(input, chunk, 0, count)
                encrypted.put(chunk, 0, count)
                remaining -= count
            }
            if (input.read() != -1) throw BackupException(BackupError.CORRUPTED)
            val cipher = cipher(Cipher.DECRYPT_MODE, password, header.copyOfRange(24, 40), header.copyOfRange(40, 52), iterations)
            cipher.updateAAD(header)
            encrypted.flip()
            cipher.doFinal(encrypted, plaintext)
            plaintext.flip()
            if (plaintext.remaining() != encryptedSize - TAG_BYTES) throw BackupException(BackupError.DECRYPTION_FAILED)
            return ByteArray(plaintext.remaining()).also { plaintext.get(it) }
        } catch (_: BadPaddingException) {
            // Wrong passwords and changed authenticated bytes are indistinguishable to GCM.
            throw BackupException(BackupError.INVALID_PASSWORD)
        } catch (error: GeneralSecurityException) {
            throw BackupException(cryptoError(error, BackupError.DECRYPTION_FAILED))
        } finally {
            wipe(encrypted)
            wipe(plaintext)
            chunk.fill(0)
        }
    }

    private fun wipe(buffer: ByteBuffer) {
        buffer.clear()
        val zeroes = ByteArray(BUFFER_BYTES)
        while (buffer.hasRemaining()) buffer.put(zeroes, 0, minOf(zeroes.size, buffer.remaining()))
    }

    fun validatePassword(password: CharArray) {
        if (password.isEmpty()) throw BackupException(BackupError.PASSWORD_REQUIRED)
        if (Character.codePointCount(password, 0, password.size) < 8) throw BackupException(BackupError.PASSWORD_TOO_SHORT)
        val encoder = Charsets.UTF_8.newEncoder().onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT)
        try {
            val encoded = encoder.encode(CharBuffer.wrap(password))
            if (encoded.hasArray()) encoded.array().fill(0)
        } catch (_: java.nio.charset.CharacterCodingException) {
            throw BackupException(BackupError.PASSWORD_INVALID_ENCODING)
        }
    }

    private fun cipher(mode: Int, password: CharArray, salt: ByteArray, nonce: ByteArray, iterations: Int): Cipher {
        val spec = PBEKeySpec(password, salt, iterations, 256)
        var encoded: ByteArray? = null
        try {
            // PBKDF2WithHmacSHA256 uses UTF-8 on the supported JVM and Android providers.
            // A cross-provider Unicode fixture locks down this wire encoding.
            val secret = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec)
            encoded = secret.encoded
            val key = SecretKeySpec(encoded, "AES")
            return Cipher.getInstance("AES/GCM/NoPadding").apply { init(mode, key, GCMParameterSpec(128, nonce)) }
        } finally {
            spec.clearPassword()
            encoded?.fill(0)
        }
    }

    private fun cryptoError(error: GeneralSecurityException, fallback: BackupError) =
        if (error is NoSuchAlgorithmException || error is NoSuchPaddingException) BackupError.CRYPTO_UNSUPPORTED else fallback

    private fun readFully(input: InputStream, buffer: ByteArray, offset: Int, length: Int) {
        var read = 0
        while (read < length) {
            val count = input.read(buffer, offset + read, length - read)
            if (count < 0) throw BackupException(BackupError.CORRUPTED)
            if (count == 0) {
                val next = input.read()
                if (next < 0) throw BackupException(BackupError.CORRUPTED)
                buffer[offset + read++] = next.toByte()
            } else read += count
        }
    }
}
