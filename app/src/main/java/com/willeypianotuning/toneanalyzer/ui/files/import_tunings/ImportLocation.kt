package com.willeypianotuning.toneanalyzer.ui.files.import_tunings

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import timber.log.Timber
import java.io.InputStream
import java.util.*

interface ImportLocation {
    fun name(): String

    suspend fun withInputStream(consumer: suspend ((InputStream) -> Unit))
}

class AndroidUriImportLocation(
    private val context: Context,
    private val uri: Uri
) : ImportLocation {
    override fun name(): String {
        return when (uri.scheme) {
            "content" -> {
                context.applicationContext.contentResolver.query(uri, null, null, null, null)?.use {
                    if (it.moveToFirst()) {
                        val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        it.getString(nameIndex) ?: ""
                    } else {
                        ""
                    }
                } ?: ""
            }
            "file" -> (uri.path ?: "").lowercase(Locale.getDefault())
            else -> {
                Timber.w("Unknown URI scheme detected: ${uri.scheme}")
                ""
            }
        }
    }

    override suspend fun withInputStream(consumer: suspend (InputStream) -> Unit) {
        context.applicationContext.contentResolver.openInputStream(uri)?.use { stream ->
            consumer.invoke(stream)
        }
    }
}