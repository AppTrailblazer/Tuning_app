package com.willeypianotuning.toneanalyzer.store.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.willeypianotuning.toneanalyzer.store.db.temperaments.Temperament
import com.willeypianotuning.toneanalyzer.store.db.temperaments.TemperamentDao
import com.willeypianotuning.toneanalyzer.store.db.tuning_styles.TuningStyle
import com.willeypianotuning.toneanalyzer.store.db.tuning_styles.TuningStyleDao
import com.willeypianotuning.toneanalyzer.store.db.tunings.PianoTuning
import com.willeypianotuning.toneanalyzer.store.db.tunings.PianoTuningDao

@Database(
    entities = [
        PianoTuning::class, Temperament::class, TuningStyle::class
    ],
    version = 5,
    exportSchema = true
)
@TypeConverters(TypeSerializers::class)
abstract class PianoTuningDatabase : RoomDatabase() {

    abstract fun tuningsDao(): PianoTuningDao

    abstract fun temperamentDao(): TemperamentDao

    abstract fun tuningStyleDao(): TuningStyleDao

}