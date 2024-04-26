package com.willeypianotuning.toneanalyzer.store.db.tuning_styles

import android.os.Parcelable
import androidx.room.*
import com.willeypianotuning.toneanalyzer.ToneDetectorWrapper
import kotlinx.parcelize.Parcelize

@Entity(tableName = "TuningStyle")
@Parcelize
data class TuningStyle @JvmOverloads constructor(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,
    @ColumnInfo(name = "name")
    val name: String,
    @Embedded
    val intervalWeights: IntervalWeights,
    @Ignore
    val mutable: Boolean = false
) : Parcelable {
    companion object {
        @JvmStatic
        val DEFAULT: TuningStyle by lazy {
            val weights = ToneDetectorWrapper.newInstance().use {
                it.defaultIntervalWeights
            }
            return@lazy TuningStyle(
                id = "default",
                name = "Default",
                intervalWeights = weights,
                mutable = false
            )
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as TuningStyle

        if (id != other.id) return false
        if (name != other.name) return false
        if (mutable != other.mutable) return false
        if (intervalWeights != other.intervalWeights) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + name.hashCode()
        result = 31 * result + mutable.hashCode()
        result = 31 * result + intervalWeights.hashCode()
        return result
    }

}