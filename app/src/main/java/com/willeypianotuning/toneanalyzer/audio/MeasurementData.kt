package com.willeypianotuning.toneanalyzer.audio

data class MeasurementData(
    val fx: DoubleArray,
    val harmonics: Array<DoubleArray>
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is MeasurementData) return false

        if (!fx.contentEquals(other.fx)) return false
        if (!harmonics.contentDeepEquals(other.harmonics)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = fx.contentHashCode()
        result = 31 * result + harmonics.contentDeepHashCode()
        return result
    }
}