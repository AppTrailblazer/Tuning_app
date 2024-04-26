package com.willeypianotuning.toneanalyzer.store

import android.content.Context
import com.willeypianotuning.toneanalyzer.store.db.temperaments.Temperament
import com.willeypianotuning.toneanalyzer.sync.json.TemperamentSerializer
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import java.io.IOException
import javax.inject.Inject

class PreloadedTemperamentDataStore @Inject constructor(
    @ApplicationContext private val context: Context,
    private val serializer: TemperamentSerializer
) {

    private fun predefinedTemperaments(): List<String> {
        try {
            val list = context.assets.list(DIRECTORY_NAME) ?: emptyArray()
            val temperaments = ArrayList<String>()
            for (file in list) {
                val extensionIndex = file.lastIndexOf(".")
                if (extensionIndex == -1) {
                    continue
                }
                val name = file.substring(0, extensionIndex)
                temperaments.add(name)
            }
            return temperaments
        } catch (e: IOException) {
            Timber.e(e, "Cannot load temperaments files")
        }

        return emptyList()
    }

    private fun loadPreloadedTemperament(name: String): Temperament? {
        try {
            return context.assets.open("${DIRECTORY_NAME}/$name.${FILE_EXTENSION}")
                .use { serializer.fromStream(it) }
        } catch (e: Exception) {
            Timber.e(e, "Cannot open temperament file")
        }

        return null
    }

    fun getAll(): List<Temperament> {
        val temperamentFiles = predefinedTemperaments()
        val temperaments = ArrayList<Temperament>()
        for (file in temperamentFiles) {
            val temperament = loadPreloadedTemperament(file)
            if (temperament != null) {
                temperaments.add(temperament)
            }
        }
        return temperaments
    }

    companion object {
        const val FILE_EXTENSION = "tem"
        const val DIRECTORY_NAME = "temperaments"
    }
}