package com.willeypianotuning.toneanalyzer.ui.files.export_tunings

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

interface ExportLocation {
    fun withOutputStream(consumer: ((OutputStream) -> Unit))
    fun uriString(): String
}

class AndroidFileExportLocation(
    private val context: Context,
    private val file: File
) : ExportLocation {
    override fun withOutputStream(consumer: ((OutputStream) -> Unit)) {
        file.outputStream().use { outputStream ->
            consumer.invoke(outputStream)
        }
    }

    override fun uriString(): String {
        val uri = FileProvider.getUriForFile(
            context,
            "com.willeypianotuning.toneanalyzer.fileprovider",
            file
        )
        return uri.toString()
    }
}

class AndroidUriExportLocation(
    private val context: Context,
    private val uri: Uri
) : ExportLocation {
    override fun withOutputStream(consumer: ((OutputStream) -> Unit)) {
        context.contentResolver.openFileDescriptor(uri, "w")?.use {
            FileOutputStream(it.fileDescriptor).use { outputStream ->
                consumer.invoke(outputStream)
            }
        }
    }

    override fun uriString(): String {
        return uri.toString()
    }
}