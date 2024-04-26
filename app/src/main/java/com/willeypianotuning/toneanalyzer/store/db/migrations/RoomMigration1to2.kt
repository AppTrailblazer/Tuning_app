package com.willeypianotuning.toneanalyzer.store.db.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.willeypianotuning.toneanalyzer.store.db.TypeSerializers
import com.willeypianotuning.toneanalyzer.store.migrations.PianoTuningFixPeakHeightsMigration
import timber.log.Timber

class RoomMigration1to2 : Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE Tunings ADD COLUMN recalculate_delta INTEGER NOT NULL DEFAULT 0")
        val typeSerializer = TypeSerializers()
        val migration = PianoTuningFixPeakHeightsMigration()
        val cursor = database.query("SELECT id, peak_heights FROM Tunings")
        cursor.use {
            while (it.moveToNext()) {
                val id = cursor.getString(0)
                val peakHeightsData = cursor.getString(1)
                val peakHeights = typeSerializer.stringToDoubleMatrix(peakHeightsData)
                migration.migrate(peakHeights)
                val fixedPeakHeightsData = typeSerializer.doubleMatrixToString(peakHeights)
                Timber.d("Running migration of peakHeights for tuning $id.")
                database.execSQL("UPDATE Tunings SET peak_heights = '$fixedPeakHeightsData', recalculate_delta = 1 WHERE id = '$id'")
            }
        }
    }
}