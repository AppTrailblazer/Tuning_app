package com.willeypianotuning.toneanalyzer.store

import com.willeypianotuning.toneanalyzer.store.db.PianoTuningDatabase
import com.willeypianotuning.toneanalyzer.store.db.tuning_styles.TuningStyle
import javax.inject.Inject

class TuningStyleDataStore @Inject constructor(
    private val preloadedDs: PreloadedTuningStyleDataStore,
    private val db: PianoTuningDatabase,
) {

    suspend fun allStyles(): List<TuningStyle> {
        return preloadedStyles() + customStyles()
    }

    fun preloadedStyles(): List<TuningStyle> {
        return preloadedDs.getAll().map { it.copy(mutable = false) }
    }

    suspend fun customStyles(): List<TuningStyle> {
        return db.tuningStyleDao().getStyles().map { it.copy(mutable = true) }
    }

    suspend fun getById(id: String): TuningStyle? {
        if (id == TuningStyle.DEFAULT.id) {
            return TuningStyle.DEFAULT
        }

        return preloadedStyles().firstOrNull { it.id == id }
            ?: customStyles().firstOrNull { it.id == id }
    }

    suspend fun addStyle(style: TuningStyle): TuningStyle {
        db.tuningStyleDao().insert(style)
        return style
    }

    suspend fun deleteStyle(style: TuningStyle): Int {
        db.tuningStyleDao().delete(style)
        return 1
    }

}