package com.willeypianotuning.toneanalyzer.sync

import com.willeypianotuning.toneanalyzer.store.PianoTuningDataStore
import com.willeypianotuning.toneanalyzer.store.TemperamentDataStore
import com.willeypianotuning.toneanalyzer.store.TuningStyleDataStore
import com.willeypianotuning.toneanalyzer.store.db.PianoTuningDatabase
import com.willeypianotuning.toneanalyzer.store.db.temperaments.Temperament
import com.willeypianotuning.toneanalyzer.store.db.tuning_styles.TuningStyle
import com.willeypianotuning.toneanalyzer.store.db.tunings.PianoTuning
import com.willeypianotuning.toneanalyzer.sync.json.PianoTuningSerializer
import com.willeypianotuning.toneanalyzer.sync.json.TemperamentSerializer
import com.willeypianotuning.toneanalyzer.sync.json.TuningStyleSerializer
import org.json.JSONException
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.util.zip.ZipInputStream
import javax.inject.Inject

class PianoMeterFilesImporter @Inject constructor(
    private val pianoTuningSerializer: PianoTuningSerializer,
    private val temperamentSerializer: TemperamentSerializer,
    private val tuningStyleSerializer: TuningStyleSerializer,
    private val pianoTuningDataStore: PianoTuningDataStore,
    private val temperamentDataStore: TemperamentDataStore,
    private val tuningStyleDataStore: TuningStyleDataStore,
    private val database: PianoTuningDatabase
) {

    private var temperaments: List<Temperament>? = null
    private var tuningStyles: List<TuningStyle>? = null

    suspend fun importSty(
        stream: InputStream,
        strategy: RestoreStrategy = RestoreStrategy.RESTORE_MISSING
    ) {
        importTuningStyle(stream, strategy)
    }

    suspend fun importTem(
        stream: InputStream,
        strategy: RestoreStrategy = RestoreStrategy.RESTORE_MISSING
    ) {
        importTemperament(stream, strategy)
    }

    suspend fun importEtf(
        stream: InputStream,
        strategy: RestoreStrategy = RestoreStrategy.RESTORE_MISSING
    ) {
        importTuning(stream, strategy)
    }

    @Throws(IOException::class, JSONException::class)
    suspend fun importEtfz(
        stream: InputStream,
        strategy: RestoreStrategy = RestoreStrategy.RESTORE_MISSING
    ) {
        if (strategy == RestoreStrategy.REPLACE_EXISTING) {
            database.tuningsDao().deleteAll()
        }
        ZipInputStream(stream).use { zis ->
            var zipEntry = zis.nextEntry
            while (zipEntry != null) {
                ByteArrayOutputStream().use { bos ->
                    val bytes = ByteArray(16384)
                    var length: Int
                    while (zis.read(bytes).also { length = it } > 0) {
                        bos.write(bytes, 0, length)
                    }
                    ByteArrayInputStream(bos.toByteArray()).use { bis ->
                        when {
                            zipEntry.name.endsWith(".etf") -> {
                                importTuning(bis, strategy)
                            }
                            zipEntry.name.endsWith(".tem") -> {
                                importTemperament(bis, strategy)
                            }
                            zipEntry.name.endsWith(".sty") -> {
                                importTuningStyle(bis, strategy)
                            }
                            else -> {
                                importTuning(bis, strategy)
                            }
                        }
                    }
                }
                zipEntry = zis.nextEntry
            }
        }
    }

    private suspend fun tuningExists(tuning: PianoTuning): Boolean {
        val list = pianoTuningDataStore.checkExisting(listOf(tuning))
        return list.firstOrNull { it.first.id == tuning.id }?.second ?: false
    }

    private suspend fun importTuning(stream: InputStream, strategy: RestoreStrategy) {
        val tuning = pianoTuningSerializer.fromStream(stream)
        if (tuningExists(tuning)) {
            if (strategy == RestoreStrategy.OVERWRITE_EXISTING) {
                pianoTuningDataStore.updateTuning(tuning, updateLastModified = false)
            }
        } else {
            pianoTuningDataStore.addTuning(tuning, updateLastModified = false)
        }
    }

    private suspend fun importTemperament(stream: InputStream, strategy: RestoreStrategy) {
        val temperaments = temperaments ?: temperamentDataStore.allTemperaments()
        this.temperaments = temperaments

        val temperament = temperamentSerializer.fromStream(stream)
        val existingTemperament = temperaments.firstOrNull { it.id == temperament.id }
        if (existingTemperament == null) {
            temperamentDataStore.addTemperament(temperament)
            return
        }
        if (!existingTemperament.mutable) {
            return
        }
        if (strategy == RestoreStrategy.OVERWRITE_EXISTING) {
            temperamentDataStore.addTemperament(temperament)
        }
    }

    private suspend fun importTuningStyle(stream: InputStream, strategy: RestoreStrategy) {
        val tuningStyles = tuningStyles ?: tuningStyleDataStore.allStyles()
        this.tuningStyles = tuningStyles

        val tuningStyle = tuningStyleSerializer.fromStream(stream)
        val existingStyle = tuningStyles.firstOrNull { it.id == tuningStyle.id }
        if (existingStyle == null) {
            tuningStyleDataStore.addStyle(tuningStyle)
            return
        }
        if (!existingStyle.mutable) {
            return
        }
        if (strategy == RestoreStrategy.OVERWRITE_EXISTING) {
            tuningStyleDataStore.addStyle(tuningStyle)
        }
    }

}