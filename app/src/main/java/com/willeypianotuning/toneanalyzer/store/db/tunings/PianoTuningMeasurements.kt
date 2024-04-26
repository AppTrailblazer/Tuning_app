package com.willeypianotuning.toneanalyzer.store.db.tunings

import android.os.Parcelable
import androidx.room.ColumnInfo
import kotlinx.parcelize.Parcelize
import java.util.*

@Parcelize
data class PianoTuningMeasurements(
    @ColumnInfo(name = "inharmonicity")
    val inharmonicity: Array<DoubleArray>,

    @ColumnInfo(name = "peak_heights")
    val peakHeights: Array<DoubleArray>,

    @ColumnInfo(name = "harmonics")
    val harmonics: Array<DoubleArray>,

    @ColumnInfo(name = "bx_fit")
    val bxFit: DoubleArray,

    @ColumnInfo(name = "delta")
    val delta: DoubleArray,

    @ColumnInfo(name = "fx")
    val fx: DoubleArray,
) : Parcelable {

    fun resetHarmonics(): PianoTuningMeasurements {
        return copy(harmonics = Array(88) { DoubleArray(10) })
    }

    fun resetInharmonicity(note: Int): PianoTuningMeasurements {
        val inharmonicity = this.inharmonicity.copyOf()
        Arrays.fill(inharmonicity[note], 0.0)
        return copy(inharmonicity = inharmonicity)
    }

    private fun validate(): List<String> {
        val messages = arrayListOf<String>()
        if (inharmonicity.size != 88) {
            messages.add("Inharmonicity should have 88 items")
        }
        inharmonicity.forEachIndexed { idx, item ->
            if (item.size != 3) {
                messages.add("Inharmonicity $idx should have 3 items")
            }
        }
        if (peakHeights.size != 88) {
            messages.add("peakHeights should have 88 items")
        }
        peakHeights.forEachIndexed { idx, item ->
            if (item.size != 16) {
                messages.add("PeakHeights $idx should have 16 items")
            }
        }
        if (harmonics.size != 88) {
            messages.add("Harmonics should have 88 items")
        }
        harmonics.forEachIndexed { idx, item ->
            if (item.size != 10) {
                messages.add("Harmonics $idx should have 10 items")
            }
        }

        if (bxFit.size != 88) {
            messages.add("BxFit should have 88 items")
        }

        if (delta.size != 88) {
            messages.add("Delta should have 88 items")
        }

        if (fx.size != 88) {
            messages.add("Fx should have 88 items")
        }
        return messages
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PianoTuningMeasurements) return false

        if (!inharmonicity.contentDeepEquals(other.inharmonicity)) return false
        if (!peakHeights.contentDeepEquals(other.peakHeights)) return false
        if (!harmonics.contentDeepEquals(other.harmonics)) return false
        if (!bxFit.contentEquals(other.bxFit)) return false
        if (!delta.contentEquals(other.delta)) return false
        if (!fx.contentEquals(other.fx)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = inharmonicity.contentDeepHashCode()
        result = 31 * result + peakHeights.contentDeepHashCode()
        result = 31 * result + harmonics.contentDeepHashCode()
        result = 31 * result + bxFit.contentHashCode()
        result = 31 * result + delta.contentHashCode()
        result = 31 * result + fx.contentHashCode()
        return result
    }

    companion object {
        fun empty(): PianoTuningMeasurements {
            return PianoTuningMeasurements(
                inharmonicity = Array(88) { DoubleArray(3) },
                peakHeights = Array(88) { DoubleArray(16) },
                harmonics = Array(88) { DoubleArray(10) },
                bxFit = DoubleArray(88),
                delta = DoubleArray(88),
                fx = DoubleArray(88),
            )
        }
    }
}
