package com.willeypianotuning.toneanalyzer.store.db.tunings

import androidx.room.ColumnInfo
import com.willeypianotuning.toneanalyzer.audio.enums.PianoType
import com.willeypianotuning.toneanalyzer.audio.enums.PianoTypeEnum
import java.util.*

data class PianoTuningInfo(
    @ColumnInfo(name = "id")
    var id: String = "",

    @ColumnInfo(name = "name")
    var name: String = "",

    @ColumnInfo(name = "make")
    var make: String = "",

    @ColumnInfo(name = "model")
    var model: String = "",

    @ColumnInfo(name = "type")
    @PianoTypeEnum
    var type: Int = PianoType.UNSPECIFIED,

    @ColumnInfo(name = "last_modified")
    var lastModified: Date = Date()
)