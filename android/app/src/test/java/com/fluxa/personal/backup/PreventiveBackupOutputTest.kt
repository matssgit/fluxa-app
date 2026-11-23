package com.fluxa.personal.backup

import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.OutputStream
import org.junit.Assert.*
import org.junit.Test

class PreventiveBackupOutputTest {
    @Test
    fun completionIsRecordedOnlyAfterFlushAndCloseAndValidOutputIsRetained() {
        val destination = FaultDestination()
        val writer = PreventiveBackupOutput(destination)
        writer.write {
            assertFalse(writer.created)
            assertEquals(0, destination.closes)
            it.write(ByteArray(80))
        }
        assertTrue(writer.created)
        assertEquals(1, destination.closes)
        assertEquals(BackupCleanup.NOT_NEEDED, writer.cleanupIncomplete())
        assertEquals(0, destination.deletes)
    }

    @Test
    fun openHeaderCiphertextFlushAndCloseFailuresKeepOutputIncompleteAndTryCleanupOnce() {
        for (fault in listOf("open", "header", "ciphertext", "flush", "close")) {
            val destination = FaultDestination(fault)
            val writer = PreventiveBackupOutput(destination)
            val failure = runCatching { writer.write { it.write(ByteArray(256)) } }.exceptionOrNull()
            assertTrue("An injected output failure must propagate", failure is IOException)
            assertFalse(writer.created)
            assertEquals(BackupCleanup.DELETED, writer.cleanupIncomplete())
            assertEquals(BackupCleanup.DELETED, writer.cleanupIncomplete())
            assertEquals(1, destination.deletes)
            assertEquals(if (fault == "open") 0 else 1, destination.closes)
        }
    }

    @Test
    fun aRefusedOrThrowingCleanupKeepsTheOriginalWriteFailure() {
        for (throwsOnDelete in listOf(false, true)) {
            val destination = FaultDestination("ciphertext", acceptsDelete = false, throwsOnDelete = throwsOnDelete)
            val writer = PreventiveBackupOutput(destination)
            val original = runCatching { writer.write { it.write(ByteArray(256)) } }.exceptionOrNull()
            assertTrue(original is IOException)
            assertEquals("injected-write", original?.message)
            assertEquals(BackupCleanup.FAILED, writer.cleanupIncomplete())
            assertEquals(1, destination.deletes)
        }
    }

    @Test
    fun laterRestoreFailureCannotDeleteACompletedPreventiveDocument() {
        val destination = FaultDestination()
        val writer = PreventiveBackupOutput(destination)
        writer.write { it.write(ByteArray(256)) }
        val error = BackupResult.Failure(
            BackupError.RESTORE_CONFLICT, preventiveBackupCreated = writer.created,
            cleanup = writer.cleanupIncomplete(),
        )
        assertEquals(BackupError.RESTORE_CONFLICT, error.error)
        assertTrue(error.preventiveBackupCreated)
        assertEquals(BackupCleanup.NOT_NEEDED, error.cleanup)
        assertEquals(0, destination.deletes)
    }

    private class FaultDestination(
        private val fault: String = "",
        private val acceptsDelete: Boolean = true,
        private val throwsOnDelete: Boolean = false,
    ) : BackupDestination {
        var deletes = 0
        var closes = 0
        override fun open(): OutputStream {
            if (fault == "open") throw IOException("injected-open")
            return object : ByteArrayOutputStream() {
                override fun write(bytes: ByteArray, offset: Int, length: Int) {
                    val limit = when (fault) {
                        "header" -> 52
                        "ciphertext" -> 128
                        else -> Int.MAX_VALUE
                    }
                    if (size() + length > limit) {
                        super.write(bytes, offset, maxOf(0, limit - size()))
                        throw IOException("injected-write")
                    }
                    super.write(bytes, offset, length)
                }
                override fun flush() {
                    if (fault == "flush") throw IOException("injected-flush")
                }
                override fun close() {
                    closes++
                    if (fault == "close") throw IOException("injected-close")
                }
            }
        }
        override fun delete(): Boolean {
            deletes++
            if (throwsOnDelete) throw SecurityException("provider denied cleanup")
            return acceptsDelete
        }
    }
}
