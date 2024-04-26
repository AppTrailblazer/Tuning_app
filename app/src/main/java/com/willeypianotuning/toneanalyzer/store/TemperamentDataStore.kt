package com.willeypianotuning.toneanalyzer.store

import com.willeypianotuning.toneanalyzer.store.db.PianoTuningDatabase
import com.willeypianotuning.toneanalyzer.store.db.temperaments.Temperament
import javax.inject.Inject

class TemperamentDataStore @Inject constructor(
    private val preloadedDs: PreloadedTemperamentDataStore,
    private val db: PianoTuningDatabase
) {
    suspend fun allTemperaments(): List<Temperament> {
        return preloadedTemperaments() + customTemperaments()
    }

    fun preloadedTemperaments(): List<Temperament> {
        return preloadedDs.getAll().map { it.copy(mutable = false) }
    }

    suspend fun customTemperaments(): List<Temperament> {
        return db.temperamentDao().getTemperaments().map { it.copy(mutable = true) }
    }

    suspend fun addTemperament(temperament: Temperament): Temperament {
        db.temperamentDao().insert(temperament)
        return temperament
    }

    suspend fun deleteTemperament(temperament: Temperament): Int {
        db.temperamentDao().delete(temperament)
        return 1
    }
}