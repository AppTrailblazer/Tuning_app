package com.willeypianotuning.toneanalyzer.audio

import com.willeypianotuning.toneanalyzer.audio.enums.PianoType
import com.willeypianotuning.toneanalyzer.audio.enums.PitchRaiseMode
import com.willeypianotuning.toneanalyzer.store.db.tunings.PianoTuning

data class PitchRaiseOptions(
    var pianoType: Int = PianoType.UNSPECIFIED,
    var lowestUnwound: Int = LOWEST_UNWOUND_DEFAULT,
    var highestMidsection: Int = HIGHEST_MIDSECTION_DEFAULT,
    var notesToRaise: MutableList<Int> = mutableListOf<Int>(),
    var mode: Int = PitchRaiseMode.OFF,
    var measurement: MeasurementData? = null
) {
    constructor(tuning: PianoTuning) : this(
        pianoType = tuning.type,
        lowestUnwound = if (tuning.tenorBreak < 0) LOWEST_UNWOUND_DEFAULT else tuning.tenorBreak
    )

    val keys: IntArray
        get() = IntArray(12) { i -> if (notesToRaise.contains(i % 12)) 1 else 0 }

    val raiseKeys: BooleanArray
        get() = BooleanArray(88) { i -> notesToRaise.contains(i % 12) }

    companion object {
        const val TENOR_BREAK_START = 18 // D2
        const val TENOR_BREAK_END = 42 // D4
        const val TENOR_BREAK_LENGTH = TENOR_BREAK_END - TENOR_BREAK_START + 1

        const val HIGHEST_MIDSECTION_START = 49 // A4
        const val HIGHEST_MIDSECTION_LENGTH = 13

        const val LOWEST_UNWOUND_DEFAULT = 33 // F3
        const val HIGHEST_MIDSECTION_DEFAULT = 57 // F5
    }
}