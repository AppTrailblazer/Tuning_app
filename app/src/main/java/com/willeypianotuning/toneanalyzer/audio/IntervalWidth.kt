package com.willeypianotuning.toneanalyzer.audio

data class IntervalWidthData(
    val beatrate: Array<DoubleArray>,
    val strength: Array<DoubleArray>
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is IntervalWidthData) return false

        if (!beatrate.contentDeepEquals(other.beatrate)) return false
        if (!strength.contentDeepEquals(other.strength)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = beatrate.contentDeepHashCode()
        result = 31 * result + strength.contentDeepHashCode()
        return result
    }
}

data class IntervalWidth(
    val octave: IntervalWidthData,
    val fifth: IntervalWidthData,
    val fourth: IntervalWidthData,
    val twelfth: IntervalWidthData,
    val doubleOctave: IntervalWidthData,
    val tripleOctave: IntervalWidthData,
    val nineteenth: IntervalWidthData
)