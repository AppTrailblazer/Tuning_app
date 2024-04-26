package com.willeypianotuning.toneanalyzer.store.db.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.willeypianotuning.toneanalyzer.store.db.TypeSerializers
import com.willeypianotuning.toneanalyzer.store.migrations.PianoTuningFixPeakHeights2Migration
import timber.log.Timber

class RoomMigration4to5 : Migration(4, 5) {
    override fun migrate(database: SupportSQLiteDatabase) {
        val typeSerializer = TypeSerializers()
        val migration = PianoTuningFixPeakHeights2Migration()
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