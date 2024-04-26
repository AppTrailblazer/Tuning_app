package com.willeypianotuning.toneanalyzer.sync

import com.willeypianotuning.toneanalyzer.store.db.tunings.PianoTuning
import com.willeypianotuning.toneanalyzer.sync.json.PianoTuningSerializer
import com.willeypianotuning.toneanalyzer.sync.json.TemperamentSerializer
import com.willeypianotuning.toneanalyzer.sync.json.TuningStyleSerializer
import org.json.JSONException
import java.io.*
import java.nio.charset.Charset

class EtfFileWriter(private val outputStream: OutputStream) : Closeable {
    private val pianoTuningSerializer: PianoTuningSerializer = PianoTuningSerializer(
        TemperamentSerializer(), TuningStyleSerializer()
    )

    constructor(file: File) : this(FileOutputStream(file))

    @Throws(IOException::class, JSONException::class)
    fun writeTuning(tuning: PianoTuning) {
        val data =
            pianoTuningSerializer.toJson(tuning).toString().toByteArray(Charset.forName("utf-8"))
        outputStream.write(data)
    }

    override fun close() {
        outputStream.close()
    }

}