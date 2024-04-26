package com.willeypianotuning.toneanalyzer.store.migrations

import android.content.Context
import com.willeypianotuning.toneanalyzer.AppSettings
import com.willeypianotuning.toneanalyzer.store.PianoTuningDataStore
import com.willeypianotuning.toneanalyzer.store.TemperamentDataStore
import com.willeypianotuning.toneanalyzer.store.TuningStyleDataStore
import com.willeypianotuning.toneanalyzer.sync.json.PianoTuningSerializer
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import timber.log.Timber
import java.io.File
import java.io.FileFilter
import java.util.*
import javax.inject.Inject

class TuningFilesToDatabaseMigration @Inject constructor(
    @ApplicationContext private val context: Context,
    private val appSettings: AppSettings,
    private val tuningSerializer: PianoTuningSerializer,
    private val tuningDataStore: PianoTuningDataStore,
    private val temperamentDataStore: TemperamentDataStore,
    private val tuningStyleDataStore: TuningStyleDataStore
) {

    private fun getTuningFilesDirectory(): File {
        return File(context.filesDir.parent, "tunings")
    }

    fun shouldRunMigration(): Boolean {
        return !appSettings.tuningFilesToDbMigrationDone
    }

    suspend fun migrate() {
        withContext(Dispatchers.IO) {
            if (!shouldRunMigration()) {
                return@withContext
            }

            try {
                val tuningsDir = getTuningFilesDirectory()
                if (!tuningsDir.exists()) {
                    return@withContext
                }
                val tuningFiles =
                    tuningsDir.listFiles(FileFilter {
                        "etf".equals(
                            it.extension,
                            ignoreCase = true
                        )
                    })
                        ?: emptyArray()

                if (tuningFiles.isEmpty()) {
                    return@withContext
                }

                val temperaments = temperamentDataStore.allTemperaments()
                val styles = tuningStyleDataStore.allStyles()

                for (file in tuningFiles) {
                    try {
                        file.inputStream().use {
                            val tuning =
                                tuningSerializer.fromJson(
                                    JSONObject(
                                        it.bufferedReader().readText()
                                    )
                                )
                            tuning.lastModified = Date(file.lastModified())
                            val temperament = tuning.temperament
                            val temperamentExists =
                                temperament != null && temperaments.any { it.id == temperament.id }
                            if (!temperamentExists) {
                                tuning.temperament = null
                            }
                            val style = tuning.tuningStyle
                            val styleExists = style != null && styles.any { it.id == style.id }
                            if (!styleExists) {
                                tuning.tuningStyle = null
                            }

                            tuningDataStore.addTuning(tuning, false)
                            @Suppress("DEPRECATION")
                            if (file.name == appSettings.currentFile()) {
                                appSettings.currentTuningId = tuning.id
                                @Suppress("DEPRECATION")
                                appSettings.clearCurrentFile()
                            }
                        }
                    } catch (e: Exception) {
                        Timber.w(e, "Cannot migrate tuning file ${file.name} to database")
                    }
                }
            } finally {
                appSettings.tuningFilesToDbMigrationDone = true
            }
        }
    }

}