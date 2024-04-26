package com.willeypianotuning.toneanalyzer.sync

import com.willeypianotuning.toneanalyzer.store.db.temperaments.Temperament
import com.willeypianotuning.toneanalyzer.store.db.tuning_styles.TuningStyle
import com.willeypianotuning.toneanalyzer.store.db.tunings.PianoTuning
import com.willeypianotuning.toneanalyzer.sync.json.PianoTuningSerializer
import com.willeypianotuning.toneanalyzer.sync.json.TemperamentSerializer
import com.willeypianotuning.toneanalyzer.sync.json.TuningStyleSerializer
import org.json.JSONException
import timber.log.Timber
import java.io.*
import java.nio.charset.Charset
import java.text.SimpleDateFormat
import java.util.*
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class EtfzFileWriter(outputStream: OutputStream) : Closeable {
    private val temperamentSerializer = TemperamentSerializer()
    private val tuningStyleSerializer = TuningStyleSerializer()
    private val pianoTuningSerializer: PianoTuningSerializer = PianoTuningSerializer(
        temperamentSerializer, tuningStyleSerializer,
    )

    private val df = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US)

    private val zipOutputStream: ZipOutputStream = ZipOutputStream(outputStream)

    constructor(file: File) : this(FileOutputStream(file))

    private fun formatFileName(
        id: String,
        name: String,
        date: Date? = null,
        maxLength: Int = 30
    ): String {
        var nameCleared = name.trim()
            .replace("[^A-Za-z0-9_ ]+", "")
            .trim()
            .replace(" ", "_")
        if (nameCleared.isEmpty()) {
            nameCleared = id
        }
        val fullName = if (date == null) {
            nameCleared
        } else {
            val timestamp = df.format(date)
            "${nameCleared}_${timestamp}"
        }
        return fullName.take(maxLength)
    }

    fun writeTemperaments(temperaments: List<Temperament>) {
        val zipEntries: MutableSet<String> = HashSet()
        for (temperament in temperaments) {
            val baseName = formatFileName(temperament.id, temperament.name)
            val entryName = findZipEntryName(baseName, "tem", zipEntries)
            if (entryName == null) {
                Timber.w("Could not find valid entry name for $baseName")
                continue
            }
            val zipEntry = ZipEntry(entryName)
            zipOutputStream.putNextEntry(zipEntry)
            zipEntries.add(entryName)
            val data = temperamentSerializer.toJson(temperament).toString()
                .toByteArray(Charset.forName("utf-8"))
            zipOutputStream.write(data, 0, data.size)
        }
    }

    fun writeTuningStyles(tuningStyles: List<TuningStyle>) {
        val zipEntries: MutableSet<String> = HashSet()
        for (tuningStyle in tuningStyles) {
            val baseName = formatFileName(tuningStyle.id, tuningStyle.name)
            val entryName = findZipEntryName(baseName, "sty", zipEntries)
            if (entryName == null) {
                Timber.w("Could not find valid entry name for $baseName")
                continue
            }
            val zipEntry = ZipEntry(entryName)
            zipOutputStream.putNextEntry(zipEntry)
            zipEntries.add(entryName)
            val data = tuningStyleSerializer.toJson(tuningStyle).toString()
                .toByteArray(Charset.forName("utf-8"))
            zipOutputStream.write(data, 0, data.size)
        }
    }

    private fun findZipEntryName(
        baseName: String,
        extension: String,
        entries: Set<String>
    ): String? {
        var name = "$baseName.$extension"
        if (!entries.contains(name)) {
            return name
        }
        for (i in 2..99) {
            name = "$baseName ($i).$extension"
            if (!entries.contains(name)) {
                return name
            }
        }
        return null
    }

    @Throws(IOException::class, JSONException::class)
    fun writeTunings(tunings: List<PianoTuning>) {
        val zipEntries: MutableSet<String> = HashSet()
        for (file in tunings) {
            val baseName = formatFileName(
                file.id,
                "${file.make} ${file.model} ${file.name}",
                file.lastModified
            )
            val entryName = findZipEntryName(baseName, "etf", zipEntries)
            if (entryName == null) {
                Timber.w("Could not find valid entry name for $baseName")
                continue
            }
            val zipEntry = ZipEntry(entryName)
            zipOutputStream.putNextEntry(zipEntry)
            zipEntries.add(entryName)
            val data =
                pianoTuningSerializer.toJson(file).toString().toByteArray(Charset.forName("utf-8"))
            zipOutputStream.write(data, 0, data.size)
        }
    }

    override fun close() {
        zipOutputStream.close()
    }

}