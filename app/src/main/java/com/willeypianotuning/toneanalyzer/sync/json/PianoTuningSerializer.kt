package com.willeypianotuning.toneanalyzer.sync.json

import com.willeypianotuning.toneanalyzer.store.db.temperaments.Temperament
import com.willeypianotuning.toneanalyzer.store.db.tuning_styles.TuningStyle
import com.willeypianotuning.toneanalyzer.store.db.tunings.PianoTuning
import com.willeypianotuning.toneanalyzer.store.db.tunings.PianoTuningMeasurements
import com.willeypianotuning.toneanalyzer.store.migrations.PianoTuningFixPeakHeights2Migration
import com.willeypianotuning.toneanalyzer.store.migrations.PianoTuningFixPeakHeightsMigration
import org.json.JSONException
import org.json.JSONObject
import java.util.*
import javax.inject.Inject

class PianoTuningSerializer @Inject constructor(
    private val temperamentSerializer: TemperamentSerializer,
    private val tuningStyleSerializer: TuningStyleSerializer,
) : ObjectSerializer<PianoTuning>() {
    private val peakHeightsFix = PianoTuningFixPeakHeightsMigration()
    private val peakHeightsFix2 = PianoTuningFixPeakHeights2Migration()

    @Throws(JSONException::class)
    override fun fromJson(json: JSONObject): PianoTuning {
        var id: String? = json.optString(JSON_KEY_ID)
        if (id == null || id.isEmpty()) {
            // backward compatibility
            id = json.optString(JSON_KEY_FILENAME).replace(".etf", "")
        }

        return PianoTuning(
            id = id,
            name = json.optString(JSON_KEY_NAME),
            make = json.optString(JSON_KEY_MAKE),
            model = json.optString(JSON_KEY_MODEL),
            serial = json.optString(JSON_KEY_SERIAL),
            notes = json.optString(JSON_KEY_NOTES),
            type = json.optInt(JSON_KEY_TYPE),
            lock = json.optBoolean(JSON_KEY_LOCK_MODE, false),
            tenorBreak = parseTenorBreak(json),
            temperament = parseTemperament(json),
            tuningStyle = parseTuningStyle(json),
            pitch = json.optDouble(JSON_KEY_PITCH),
            measurements = PianoTuningMeasurements(
                inharmonicity = parseInharmonicity(json),
                peakHeights = parsePeakHeights(json),
                harmonics = parseHarmonics(json),
                bxFit = parseBxFit(json),
                delta = parseDelta(json),
                fx = parseFx(json)
            ),
            forceRecalculateDelta = parseShouldRecalculateDelta(json),
            lastModified = parseLastModified(json)
        )
    }

    private fun parseLastModified(json: JSONObject): Date {
        if (json.has(JSON_KEY_LAST_MODIFIED)) {
            return Date(json.optLong(JSON_KEY_LAST_MODIFIED))
        }
        return Date()
    }

    private fun parseTemperament(json: JSONObject): Temperament? {
        if (json.has(JSON_KEY_TEMPERAMENT)) {
            return try {
                val temperamentJson = json.getJSONObject(JSON_KEY_TEMPERAMENT)
                temperamentSerializer.fromJson(temperamentJson)
            } catch (e: Exception) {
                null
            }
        }
        return null
    }

    private fun parseTuningStyle(json: JSONObject): TuningStyle? {
        if (json.has(JSON_KEY_TUNING_STYLE)) {
            return try {
                val styleJson = json.getJSONObject(JSON_KEY_TUNING_STYLE)
                tuningStyleSerializer.fromJson(styleJson)
            } catch (e: Exception) {
                null
            }
        }
        return null
    }

    private fun parseTenorBreak(json: JSONObject): Int {
        var tenorBreak = json.optInt(JSON_KEY_TENOR_2, -2)
        if (tenorBreak == -2) {
            // map old setting to new
            tenorBreak = json.optInt("tenorBreak", -1)
            if (tenorBreak in 0..14) {
                // A1 - A3, white keys
                val map = intArrayOf(13, 15, 16, 18, 20, 21, 23, 25, 27, 28, 30, 32, 33, 35, 37)
                tenorBreak = map[tenorBreak]
            } else {
                tenorBreak = -1
            }
        }
        return tenorBreak
    }

    private fun parseBxFit(json: JSONObject): DoubleArray {
        if (json.has(JSON_KEY_BX_FIT_2)) {
            return json.getJSONArray(JSON_KEY_BX_FIT_2).toDoubleArray()
        } else {
            val bxFitStr =
                json.optString("bxFit").split(",".toRegex()).dropLastWhile { it.isEmpty() }
                    .toTypedArray()
            val bxFit = DoubleArray(bxFitStr.size)
            for (i in bxFitStr.indices.reversed()) {
                bxFit[i] = java.lang.Double.parseDouble(bxFitStr[i])
            }
            return bxFit
        }
    }

    private fun parseDelta(json: JSONObject): DoubleArray {
        if (json.has(JSON_KEY_DELTA_2)) {
            return json.getJSONArray(JSON_KEY_DELTA_2).toDoubleArray()
        } else {
            val deltaStr = json.optString("delta")
            if (!deltaStr.equals("", ignoreCase = true) && !deltaStr.equals(
                    "null",
                    ignoreCase = true
                )
            ) {
                val deltaStrArr =
                    deltaStr.split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                val delta = DoubleArray(deltaStrArr.size)
                for (i in deltaStrArr.indices.reversed()) {
                    delta[i] = java.lang.Double.parseDouble(deltaStrArr[i])
                }
                return delta
            }
        }
        return DoubleArray(88)
    }

    private fun parseFx(json: JSONObject): DoubleArray {
        if (json.has(JSON_KEY_FX_2)) {
            return json.getJSONArray(JSON_KEY_FX_2).toDoubleArray()
        } else {
            val fxStr = json.optString("fx")
            if (!fxStr.equals("", ignoreCase = true) && !fxStr.equals("null", ignoreCase = true)) {
                val fxStrArr =
                    fxStr.split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                val fx = DoubleArray(fxStrArr.size)
                for (i in fxStrArr.indices.reversed()) {
                    fx[i] = java.lang.Double.parseDouble(fxStrArr[i])
                }
                return fx
            }
        }
        return DoubleArray(88)
    }

    private fun parseInharmonicity(json: JSONObject): Array<DoubleArray> {
        return if (json.has(JSON_KEY_INHARMONICITY_2)) {
            json.getJSONArray(JSON_KEY_INHARMONICITY_2).toDoubleMatrix()
        } else {
            parseInharmonicityOld(json)
        }
    }

    private fun parsePeakHeights(json: JSONObject): Array<DoubleArray> {
        if (json.has(JSON_KEY_PEAK_HEIGHTS_4)) {
            return json.getJSONArray(JSON_KEY_PEAK_HEIGHTS_4).toDoubleMatrix()
        }
        if (json.has(JSON_KEY_PEAK_HEIGHTS_3)) {
            val peakHeights = json.getJSONArray(JSON_KEY_PEAK_HEIGHTS_3).toDoubleMatrix()
            peakHeightsFix2.migrate(peakHeights)
            return peakHeights
        }
        val peakHeights = if (json.has(JSON_KEY_PEAK_HEIGHTS_2)) {
            json.getJSONArray(JSON_KEY_PEAK_HEIGHTS_2).toDoubleMatrix()
        } else {
            parsePeakHeightsOld(json)
        }
        peakHeightsFix.migrate(peakHeights)
        peakHeightsFix2.migrate(peakHeights)
        return peakHeights
    }

    private fun parseShouldRecalculateDelta(json: JSONObject): Boolean {
        return !json.has(JSON_KEY_PEAK_HEIGHTS_4)
    }

    private fun parseHarmonics(json: JSONObject): Array<DoubleArray> {
        if (json.has(JSON_KEY_HARMONICS_2)) {
            return json.getJSONArray(JSON_KEY_HARMONICS_2).toDoubleMatrix()
        } else {
            return parseHarmonicsOld(json)
        }
    }

    private fun parseInharmonicityOld(json: JSONObject): Array<DoubleArray> {
        val inharmonicity = Array(88) { DoubleArray(3) }
        for (i in 0..87) {
            val inharmonStr = json.optString("inharmonicity$i").split(",".toRegex())
                .dropLastWhile { it.isEmpty() }.toTypedArray()
            for (j in 0..2) {
                inharmonicity[i][j] = java.lang.Double.parseDouble(inharmonStr[j])
            }
        }
        return inharmonicity
    }

    private fun parsePeakHeightsOld(json: JSONObject): Array<DoubleArray> {
        val peakHeights = Array(88) { DoubleArray(16) }
        for (i in 0..87) {
            val peakHeightsStr =
                json.optString("peakHeights$i").split(",".toRegex()).dropLastWhile { it.isEmpty() }
                    .toTypedArray()
            for (j in 0..15) {
                peakHeights[i][j] = java.lang.Double.parseDouble(peakHeightsStr[j])
            }
        }
        return peakHeights
    }

    private fun parseHarmonicsOld(json: JSONObject): Array<DoubleArray> {
        val harmonics = Array(88) { DoubleArray(10) }
        for (i in 0..87) {
            val harmonicsStr =
                json.optString("harmonics$i").split(",".toRegex()).dropLastWhile { it.isEmpty() }
                    .toTypedArray()
            for (j in 0..9) {
                harmonics[i][j] = java.lang.Double.parseDouble(harmonicsStr[j])
            }
        }
        return harmonics
    }

    @Throws(JSONException::class)
    override fun toJson(obj: PianoTuning): JSONObject {
        val json = JSONObject()
        json.put(JSON_KEY_ID, obj.id)
        json.put(JSON_KEY_NAME, obj.name)
        json.put(JSON_KEY_MAKE, obj.make)
        json.put("protocolVersion", PROTOCOL_VERSION)
        json.put(JSON_KEY_MODEL, obj.model)
        json.put(JSON_KEY_SERIAL, obj.serial)
        json.put(JSON_KEY_NOTES, obj.notes)
        json.put(JSON_KEY_TYPE, obj.type)
        json.put(JSON_KEY_TENOR_2, obj.tenorBreak)
        json.put(JSON_KEY_PITCH, obj.pitch)
        json.put(JSON_KEY_LOCK_MODE, obj.lock)
        if (obj.temperament != null) {
            json.put(JSON_KEY_TEMPERAMENT, temperamentSerializer.toJson(obj.temperament!!))
        }
        if (obj.tuningStyle != null) {
            json.put(JSON_KEY_TUNING_STYLE, tuningStyleSerializer.toJson(obj.tuningStyle!!))
        }

        json.put(JSON_KEY_FX_2, obj.measurements.fx.toJsonArray())
        json.put(JSON_KEY_BX_FIT_2, obj.measurements.bxFit.toJsonArray())
        json.put(JSON_KEY_DELTA_2, obj.measurements.delta.toJsonArray())
        json.put(JSON_KEY_INHARMONICITY_2, obj.measurements.inharmonicity.toJsonArray())
        json.put(JSON_KEY_PEAK_HEIGHTS_4, obj.measurements.peakHeights.toJsonArray())
        json.put(JSON_KEY_HARMONICS_2, obj.measurements.harmonics.toJsonArray())
        json.put(JSON_KEY_LAST_MODIFIED, obj.lastModified.time)
        return json
    }

    companion object {
        private const val PROTOCOL_VERSION = 2

        private const val JSON_KEY_ID = "id"
        private const val JSON_KEY_FILENAME = "filename"
        private const val JSON_KEY_NAME = "name"
        private const val JSON_KEY_MAKE = "make"
        private const val JSON_KEY_MODEL = "model"
        private const val JSON_KEY_SERIAL = "serial"
        private const val JSON_KEY_NOTES = "notes"
        private const val JSON_KEY_TYPE = "type"
        private const val JSON_KEY_LOCK_MODE = "lockMode"
        private const val JSON_KEY_PITCH = "pitch"
        private const val JSON_KEY_TENOR_2 = "tenorBreak2"
        private const val JSON_KEY_FX_2 = "fx_2"
        private const val JSON_KEY_BX_FIT_2 = "bxFit_2"
        private const val JSON_KEY_DELTA_2 = "delta_2"
        private const val JSON_KEY_TUNING_STYLE = "tuningStyle"
        private const val JSON_KEY_TEMPERAMENT = "temperament"
        private const val JSON_KEY_LAST_MODIFIED = "lastModified"
        private const val JSON_KEY_HARMONICS_2 = "harmonics_2"
        private const val JSON_KEY_PEAK_HEIGHTS_2 = "peakHeights_2"
        private const val JSON_KEY_PEAK_HEIGHTS_3 = "peakHeights_3"
        private const val JSON_KEY_PEAK_HEIGHTS_4 = "peakHeights_4"
        private const val JSON_KEY_INHARMONICITY_2 = "inharmonicity_2"
    }
}
