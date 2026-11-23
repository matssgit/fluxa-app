package com.fluxa.personal.ui

import android.net.Uri
import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import com.fluxa.personal.ui.feature.backup.BackupPicker
import com.fluxa.personal.ui.feature.backup.DocumentRef
import java.io.File

/**
 * Seletor SAF roteirizado: devolve, de forma assíncrona como o sistema, documentos `file://` no cache do app.
 * O resto do caminho é o real: `BackupDocuments` → `ContentResolver` → contratos criptografados da 8-B → Room/DataStore.
 */
internal class ScriptedPicker : BackupPicker {
    val creates = ArrayDeque<File?>()
    val opens = ArrayDeque<File?>()
    val requestedNames = mutableListOf<String>()
    private val main = Handler(Looper.getMainLooper())

    private fun File?.ref() = this?.let { DocumentRef(Uri.fromFile(it).toString()) }

    @Composable
    override fun rememberCreate(onResult: (DocumentRef?) -> Unit): (String) -> Unit {
        val callback by rememberUpdatedState(onResult)
        return remember { { name -> requestedNames += name; val file = creates.removeFirst(); main.post { callback(file.ref()) } } }
    }

    @Composable
    override fun rememberOpen(onResult: (DocumentRef?) -> Unit): () -> Unit {
        val callback by rememberUpdatedState(onResult)
        return remember { { val file = opens.removeFirst(); main.post { callback(file.ref()) } } }
    }
}

