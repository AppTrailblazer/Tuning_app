package com.willeypianotuning.toneanalyzer.store

import android.content.Context
import com.willeypianotuning.toneanalyzer.store.db.tuning_styles.TuningStyle
import com.willeypianotuning.toneanalyzer.sync.json.TuningStyleSerializer
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import java.io.IOException
import javax.inject.Inject

class PreloadedTuningStyleDataStore @Inject constructor(
    @ApplicationContext private val context: Context,
    private val serializer: TuningStyleSerializer
) {
    fun getAll(): List<TuningStyle> {
        val styleFiles = predefinedStyles()
        val styles = ArrayList<TuningStyle>()
        for (file in styleFiles) {
            val style = loadPreloadedStyle(file)
            if (style != null) {
                styles.add(style)
            }
        }
        return styles
    }

    private fun predefinedStyles(): List<String> {
        try {
            val list = context.assets.list(DIRECTORY_NAME) ?: return emptyList()
            val styles = ArrayList<String>()
            for (file in list) {
                val extensionIndex = file.lastIndexOf(".")
                if (extensionIndex == -1) {
                    continue
                }
                val name = file.substring(0, extensionIndex)
                styles.add(name)
            }
            return styles
        } catch (e: IOException) {
            Timber.e(e, "Cannot load temperaments files")
        }

        return emptyList()
    }

    private fun loadPreloadedStyle(name: String): TuningStyle? {
        try {
            return context.assets.open("${DIRECTORY_NAME}/$name.${FILE_EXTENSION}")
                .use { serializer.fromStream(it) }
        } catch (e: Exception) {
            Timber.e(e, "Cannot open temperament file")
        }

        return null
    }

    companion object {
        const val FILE_EXTENSION = "sty"
        const val DIRECTORY_NAME = "styles"
    }
}