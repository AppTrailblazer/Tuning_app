package com.willeypianotuning.toneanalyzer.store.db.temperaments

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import com.willeypianotuning.toneanalyzer.extensions.round
import kotlinx.parcelize.Parcelize

@Entity(tableName = "Temperament")
@Parcelize
data class Temperament @JvmOverloads constructor(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,
    @ColumnInfo(name = "name")
    val name: String,
    @ColumnInfo(name = "year")
    val year: String,
    @ColumnInfo(name = "category")
    val category: String?,
    @ColumnInfo(name = "comma")
    val comma: String,
    @ColumnInfo(name = "offsets")
    val offsets: DoubleArray,
    @Ignore
    val mutable: Boolean = false
) : Parcelable {

    val fileName: String
        get() = "$id.tem"

    val fifths: DoubleArray
        get() = doubleArrayOf(
            offsetFor("G") - offsetFor("C") - 1.955000865,
            offsetFor("D") - offsetFor("G") - 1.955000865,
            offsetFor("A") - offsetFor("D") - 1.955000865,
            offsetFor("E") - offsetFor("A") - 1.955000865,
            offsetFor("B") - offsetFor("E") - 1.955000865,
            offsetFor("F#") - offsetFor("B") - 1.955000865,
            offsetFor("C#") - offsetFor("F#") - 1.955000865,
            offsetFor("G#") - offsetFor("C#") - 1.955000865,
            offsetFor("D#") - offsetFor("G#") - 1.955000865,
            offsetFor("A#") - offsetFor("D#") - 1.955000865,
            offsetFor("F") - offsetFor("A#") - 1.955000865,
            offsetFor("C") - offsetFor("F") - 1.955000865
        )

    val thirds: DoubleArray
        get() {
            val fifths = fifths
            val thirds = DoubleArray(fifths.size)
            for (i in fifths.indices) {
                thirds[i] = fifths[shiftPosition(i, -1, fifths.size)] +
                        fifths[shiftPosition(i, -2, fifths.size)] +
                        fifths[i] +
                        fifths[shiftPosition(i, 1, fifths.size)] +
                        21.5062896
            }
            return thirds
        }

    val fractions: DoubleArray
        get() {
            val fifths = fifths
            val fractions = DoubleArray(fifths.size)
            for (i in fifths.indices) {
                fractions[i] = (fifths[i] / commaValue).round(6)
            }
            return fractions
        }

    val commaValue: Double
        get() {
            when (comma) {
                "PC" -> return 23.46001038
                "SC" -> return 21.5062896
            }
            return 0.0
        }

    @Ignore
    constructor(temperament: Temperament) : this(
        temperament.id,
        temperament.name,
        temperament.year,
        temperament.category,
        temperament.comma,
        temperament.offsets,
        temperament.mutable
    )

    fun hasOffsets(): Boolean {
        for (i in offsets.indices) {
            if (offsets[i] != 0.0) {
                return true
            }
        }
        return false
    }

    fun offsetFor(note: String): Double {
        when (note) {
            "A" -> return offsets[0]
            "A#" -> return offsets[1]
            "B" -> return offsets[2]
            "C" -> return offsets[3]
            "C#" -> return offsets[4]
            "D" -> return offsets[5]
            "D#" -> return offsets[6]
            "E" -> return offsets[7]
            "F" -> return offsets[8]
            "F#" -> return offsets[9]
            "G" -> return offsets[10]
            "G#" -> return offsets[11]
        }
        throw IllegalArgumentException("Unknown note key")
    }

    fun setOffsetFor(note: String, offset: Double) {
        setOffsetForNote(offsets, note, offset)
    }

    companion object {
        private fun shiftPosition(i: Int, shift: Int, max: Int): Int {
            var newPosition = i + shift
            if (newPosition < 0) {
                newPosition += max
            }
            if (newPosition >= max) {
                newPosition %= max
            }
            return newPosition
        }

        fun setOffsetForNote(offsets: DoubleArray, note: String, offset: Double) {
            when (note) {
                "A" -> offsets[0] = offset
                "A#" -> offsets[1] = offset
                "B" -> offsets[2] = offset
                "C" -> offsets[3] = offset
                "C#" -> offsets[4] = offset
                "D" -> offsets[5] = offset
                "D#" -> offsets[6] = offset
                "E" -> offsets[7] = offset
                "F" -> offsets[8] = offset
                "F#" -> offsets[9] = offset
                "G" -> offsets[10] = offset
                "G#" -> offsets[11] = offset
                else -> throw IllegalArgumentException("Unknown note key")
            }
        }

        val EQUAL = Temperament(
            "equal",
            "Equal",
            "1577",
            null,
            "PC",
            doubleArrayOf(0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0)
        )
    }
}