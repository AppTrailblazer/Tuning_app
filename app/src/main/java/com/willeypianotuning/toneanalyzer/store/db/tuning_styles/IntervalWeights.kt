package com.willeypianotuning.toneanalyzer.store.db.tuning_styles

import android.os.Parcelable
import androidx.room.ColumnInfo
import kotlinx.parcelize.Parcelize

@Parcelize
data class IntervalWeights(
    @ColumnInfo(name = "octave")
    val octave: DoubleArray,
    @ColumnInfo(name = "twelfth")
    val twelfth: DoubleArray,
    @ColumnInfo(name = "double_octave")
    val doubleOctave: DoubleArray,
    @ColumnInfo(name = "nineteenth")
    val nineteenth: DoubleArray,
    @ColumnInfo(name = "triple_octave")
    val tripleOctave: DoubleArray,
    @ColumnInfo(name = "fifth")
    val fifth: DoubleArray,
    @ColumnInfo(name = "fourth")
    val fourth: DoubleArray,
    @ColumnInfo(name = "extra_treble_stretch")
    val extraTrebleStretch: DoubleArray,
    @ColumnInfo(name = "extra_bass_stretch")
    val extraBassStretch: DoubleArray
) : Parcelable {
    init {
        assert(octave.size == 5)
        assert(twelfth.size == 3)
        assert(doubleOctave.size == 2)
        assert(nineteenth.size == 1)
        assert(tripleOctave.size == 1)
        assert(fifth.size == 2)
        assert(fourth.size == 2)
        assert(extraTrebleStretch.size == 2)
        assert(extraBassStretch.size == 2)
    }

    fun joined(): DoubleArray {
        // NOTE: be careful with the order
        return octave +
                twelfth +
                doubleOctave +
                nineteenth +
                tripleOctave +
                fifth +
                fourth +
                extraTrebleStretch +
                extraBassStretch
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as IntervalWeights

        if (!octave.contentEquals(other.octave)) return false
        if (!twelfth.contentEquals(other.twelfth)) return false
        if (!doubleOctave.contentEquals(other.doubleOctave)) return false
        if (!nineteenth.contentEquals(other.nineteenth)) return false
        if (!tripleOctave.contentEquals(other.tripleOctave)) return false
        if (!fifth.contentEquals(other.fifth)) return false
        if (!fourth.contentEquals(other.fourth)) return false
        if (!extraTrebleStretch.contentEquals(other.extraTrebleStretch)) return false
        if (!extraBassStretch.contentEquals(other.extraBassStretch)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = octave.contentHashCode()
        result = 31 * result + twelfth.contentHashCode()
        result = 31 * result + doubleOctave.contentHashCode()
        result = 31 * result + nineteenth.contentHashCode()
        result = 31 * result + tripleOctave.contentHashCode()
        result = 31 * result + fifth.contentHashCode()
        result = 31 * result + fourth.contentHashCode()
        result = 31 * result + extraTrebleStretch.contentHashCode()
        result = 31 * result + extraBassStretch.contentHashCode()
        return result
    }

}