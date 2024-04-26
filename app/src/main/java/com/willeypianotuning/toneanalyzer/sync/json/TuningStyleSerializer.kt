package com.willeypianotuning.toneanalyzer.sync.json

import com.willeypianotuning.toneanalyzer.store.db.tuning_styles.IntervalWeights
import com.willeypianotuning.toneanalyzer.store.db.tuning_styles.TuningStyle
import org.json.JSONObject
import javax.inject.Inject

class TuningStyleSerializer @Inject constructor() : ObjectSerializer<TuningStyle>() {
    override fun fromJson(json: JSONObject): TuningStyle {
        val defaultWeights = TuningStyle.DEFAULT

        val id = json.getString(KEY_ID)
        val name = json.getString(KEY_NAME)
        val octave = json.getJSONArray(KEY_OCTAVE_WEIGHTS).toDoubleArray()
        var twelfth = json.getJSONArray(KEY_TWELFTH_WEIGHTS).toDoubleArray()
        if (twelfth.size < defaultWeights.intervalWeights.twelfth.size) {
            val fixedTwelfth = twelfth.copyOf(defaultWeights.intervalWeights.twelfth.size)
            for (i in twelfth.size until (defaultWeights.intervalWeights.twelfth.size)) {
                fixedTwelfth[i] = defaultWeights.intervalWeights.twelfth[i]
            }
            twelfth = fixedTwelfth
        }
        val doubleOctave = json.getJSONArray(KEY_DOUBLE_OCTAVE_WEIGHTS).toDoubleArray()
        val nineteenth = if (json.has(KEY_NINETEENTH_WEIGHTS)) {
            json.getJSONArray(KEY_NINETEENTH_WEIGHTS).toDoubleArray()
        } else {
            defaultWeights.intervalWeights.nineteenth.copyOf()
        }
        val tripleOctave = json.getJSONArray(KEY_TRIPLE_OCTAVE_WEIGHTS).toDoubleArray()
        val fifth = json.getJSONArray(KEY_FIFTH_WEIGHTS).toDoubleArray()
        val fourth = json.getJSONArray(KEY_FOURTH_WEIGHTS).toDoubleArray()
        val extraTrebleStretch = if (json.has(KEY_EXTRA_TREBLE_STRETCH)) {
            json.getJSONArray(KEY_EXTRA_TREBLE_STRETCH).toDoubleArray()
        } else {
            defaultWeights.intervalWeights.extraTrebleStretch.copyOf()
        }
        val extraBassStretch = if (json.has(KEY_EXTRA_BASS_STRETCH)) {
            json.getJSONArray(KEY_EXTRA_BASS_STRETCH).toDoubleArray()
        } else {
            defaultWeights.intervalWeights.extraBassStretch.copyOf()
        }
        return TuningStyle(
            id = id,
            name = name,
            intervalWeights = IntervalWeights(
                octave,
                twelfth,
                doubleOctave,
                nineteenth,
                tripleOctave,
                fifth,
                fourth,
                extraTrebleStretch,
                extraBassStretch
            ),
            mutable = true
        )
    }

    override fun toJson(obj: TuningStyle): JSONObject {
        val json = JSONObject()
        json.put(KEY_ID, obj.id)
        json.put(KEY_NAME, obj.name)
        json.put(KEY_OCTAVE_WEIGHTS, obj.intervalWeights.octave.toJsonArray())
        json.put(KEY_TWELFTH_WEIGHTS, obj.intervalWeights.twelfth.toJsonArray())
        json.put(KEY_DOUBLE_OCTAVE_WEIGHTS, obj.intervalWeights.doubleOctave.toJsonArray())
        json.put(KEY_NINETEENTH_WEIGHTS, obj.intervalWeights.nineteenth.toJsonArray())
        json.put(KEY_TRIPLE_OCTAVE_WEIGHTS, obj.intervalWeights.tripleOctave.toJsonArray())
        json.put(KEY_FIFTH_WEIGHTS, obj.intervalWeights.fifth.toJsonArray())
        json.put(KEY_FOURTH_WEIGHTS, obj.intervalWeights.fourth.toJsonArray())
        json.put(KEY_EXTRA_TREBLE_STRETCH, obj.intervalWeights.extraTrebleStretch.toJsonArray())
        json.put(KEY_EXTRA_BASS_STRETCH, obj.intervalWeights.extraBassStretch.toJsonArray())
        return json
    }

    companion object {
        private const val KEY_ID = "id"
        private const val KEY_NAME = "name"
        private const val KEY_OCTAVE_WEIGHTS = "octave"
        private const val KEY_TWELFTH_WEIGHTS = "twelfth"
        private const val KEY_DOUBLE_OCTAVE_WEIGHTS = "double_octave"
        private const val KEY_NINETEENTH_WEIGHTS = "nineteenth"
        private const val KEY_TRIPLE_OCTAVE_WEIGHTS = "triple_octave"
        private const val KEY_FIFTH_WEIGHTS = "fifth"
        private const val KEY_FOURTH_WEIGHTS = "fourth"
        private const val KEY_THIRD_WEIGHTS = "third"
        private const val KEY_EXTRA_TREBLE_STRETCH = "extra_treble_stretch"
        private const val KEY_EXTRA_BASS_STRETCH = "extra_bass_stretch"
    }
}