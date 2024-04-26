package com.willeypianotuning.toneanalyzer.store.db.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

class RoomMigration3to4 : Migration(3, 4) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("CREATE TABLE IF NOT EXISTS `Temperament` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, `year` TEXT NOT NULL, `category` TEXT, `comma` TEXT NOT NULL, `offsets` TEXT NOT NULL, PRIMARY KEY(`id`))")
        database.execSQL("CREATE TABLE IF NOT EXISTS `TuningStyle` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, `octave` TEXT NOT NULL, `twelfth` TEXT NOT NULL, `double_octave` TEXT NOT NULL, `nineteenth` TEXT NOT NULL, `triple_octave` TEXT NOT NULL, `fifth` TEXT NOT NULL, `fourth` TEXT NOT NULL, `extra_treble_stretch` TEXT NOT NULL, `extra_bass_stretch` TEXT NOT NULL, PRIMARY KEY(`id`))")
    }
}