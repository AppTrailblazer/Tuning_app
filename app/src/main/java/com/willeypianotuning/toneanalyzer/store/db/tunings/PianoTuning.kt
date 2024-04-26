package com.willeypianotuning.toneanalyzer.store.db.tunings

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.willeypianotuning.toneanalyzer.audio.enums.PianoType
import com.willeypianotuning.toneanalyzer.audio.enums.PianoTypeEnum
import com.willeypianotuning.toneanalyzer.store.db.temperaments.Temperament
import com.willeypianotuning.toneanalyzer.store.db.tuning_styles.TuningStyle
import kotlinx.parcelize.Parcelize
import java.text.SimpleDateFormat
import java.util.*

@Entity(tableName = "Tunings")
@Parcelize
data class PianoTuning constructor(
    @PrimaryKey
    @ColumnInfo(name = "id")
    var id: String = NO_ID,

    @ColumnInfo(name = "name")
    var name: String = generateName(),

    @ColumnInfo(name = "make")
    var make: String = "",

    @ColumnInfo(name = "model")
    var model: String = "",

    @ColumnInfo(name = "serial")
    var serial: String = "",

    @ColumnInfo(name = "notes")
    var notes: String = "",

    @Embedded
    var measurements: PianoTuningMeasurements = PianoTuningMeasurements.empty(),

    @ColumnInfo(name = "temperament")
    var temperament: Temperament? = null,

    @ColumnInfo(name = "tuning_style")
    var tuningStyle: TuningStyle? = null,

    @ColumnInfo(name = "type")
    @PianoTypeEnum
    var type: Int = PianoType.UNSPECIFIED,

    @ColumnInfo(name = "tenorBreak")
    var tenorBreak: Int = -1,

    @ColumnInfo(name = "pitch")
    var pitch: Double = 440.0,

    @ColumnInfo(name = "lock")
    var lock: Boolean = false,

    @ColumnInfo(name = "last_modified")
    var lastModified: Date = Date(),

    @ColumnInfo(name = "recalculate_delta")
    var forceRecalculateDelta: Boolean = false
) : Parcelable {

    val isNewTuning: Boolean get() = id == NO_ID

    val customTemperamentOrDefault: Temperament
        get() = temperament ?: Temperament.EQUAL

    companion object {
        @JvmField
        val NO_ID = UUID(0, 0).toString()

        @JvmStatic
        fun generateId(): String {
            return UUID.randomUUID().toString()
        }

        @JvmStatic
        fun generateName(): String {
            val df = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)
            return df.format(Date())
        }
    }
}
