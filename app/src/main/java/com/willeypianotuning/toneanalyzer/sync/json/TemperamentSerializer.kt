package com.willeypianotuning.toneanalyzer.sync.json

import com.willeypianotuning.toneanalyzer.store.db.temperaments.Temperament

import org.json.JSONException
import org.json.JSONObject
import javax.inject.Inject

class TemperamentSerializer @Inject constructor() : ObjectSerializer<Temperament>() {

    @Throws(JSONException::class)
    override fun fromJson(json: JSONObject): Temperament {
        val id = json.getString("id")
        val name = json.getString("name")
        val year = json.getString("year")
        var category: String? = null
        if (json.has("category")) {
            category = json.getString("category")
        }
        val comma = json.getString("comma")
        if ("PC" != comma && "SC" != comma) {
            throw JSONException("Unsupported comma value for temperament: $comma")
        }
        val offsets = parseOffsets(json.getJSONObject("offsets"))
        return Temperament(id, name, year, category, comma, offsets, false)
    }

    @Throws(JSONException::class)
    private fun parseOffsets(data: JSONObject): DoubleArray {
        val offsets = DoubleArray(12)
        offsets[0] = if (data.has("A")) data.getDouble("A") else 0.0
        offsets[1] = if (data.has("A#")) data.getDouble("A#") else 0.0
        offsets[2] = if (data.has("B")) data.getDouble("B") else 0.0
        offsets[3] = if (data.has("C")) data.getDouble("C") else 0.0
        offsets[4] = if (data.has("C#")) data.getDouble("C#") else 0.0
        offsets[5] = if (data.has("D")) data.getDouble("D") else 0.0
        offsets[6] = if (data.has("D#")) data.getDouble("D#") else 0.0
        offsets[7] = if (data.has("E")) data.getDouble("E") else 0.0
        offsets[8] = if (data.has("F")) data.getDouble("F") else 0.0
        offsets[9] = if (data.has("F#")) data.getDouble("F#") else 0.0
        offsets[10] = if (data.has("G")) data.getDouble("G") else 0.0
        offsets[11] = if (data.has("G#")) data.getDouble("G#") else 0.0
        return offsets
    }

    @Throws(JSONException::class)
    override fun toJson(obj: Temperament): JSONObject {
        val json = JSONObject()
        json.put("id", obj.id)
        json.put("name", obj.name)
        json.put("year", obj.year)
        json.put("category", obj.category)
        json.put("comma", obj.comma)
        val offsetsJson = JSONObject()
        val offsets = obj.offsets
        offsetsJson.put("A", offsets[0])
        offsetsJson.put("A#", offsets[1])
        offsetsJson.put("B", offsets[2])
        offsetsJson.put("C", offsets[3])
        offsetsJson.put("C#", offsets[4])
        offsetsJson.put("D", offsets[5])
        offsetsJson.put("D#", offsets[6])
        offsetsJson.put("E", offsets[7])
        offsetsJson.put("F", offsets[8])
        offsetsJson.put("F#", offsets[9])
        offsetsJson.put("G", offsets[10])
        offsetsJson.put("G#", offsets[11])
        json.put("offsets", offsetsJson)
        return json
    }

}
