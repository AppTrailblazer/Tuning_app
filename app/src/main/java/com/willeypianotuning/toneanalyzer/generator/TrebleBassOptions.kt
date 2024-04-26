package com.willeypianotuning.toneanalyzer.generator

import android.util.Range

data class TrebleBassOptions(
    val trebleVolume: Float = DEFAULT_TREBLE_VOLUME,
    var trebleEdge: Short = DEFAULT_TREBLE_EDGE,
    var bassVolume: Float = DEFAULT_BASS_VOLUME,
    var bassEdge: Short = DEFAULT_BASS_EDGE,
) {
    companion object {
        const val DEFAULT_BASS_VOLUME: Float = 2.0f
        const val DEFAULT_BASS_EDGE: Short = 18
        const val DEFAULT_TREBLE_VOLUME: Float = 1.0f
        const val DEFAULT_TREBLE_EDGE: Short = 75

        val trebleVolumeRange = Range(0.0, 2.0)
        val trebleEdgeRange = Range(50, 87)
        val bassVolumeRange = Range(0.0, 10.0)
        val bassEdgeRange = Range(0, 36)
    }
}