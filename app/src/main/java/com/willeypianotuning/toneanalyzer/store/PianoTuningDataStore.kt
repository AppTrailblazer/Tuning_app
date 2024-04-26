package com.willeypianotuning.toneanalyzer.store

import com.willeypianotuning.toneanalyzer.store.db.PianoTuningDatabase
import com.willeypianotuning.toneanalyzer.store.db.tunings.PianoTuning
import com.willeypianotuning.toneanalyzer.store.db.tunings.PianoTuningInfo
import kotlinx.coroutines.flow.Flow
import java.util.*
import javax.inject.Inject

class PianoTuningDataStore @Inject constructor(
    private val db: PianoTuningDatabase
) {
    fun getTuningsList(): Flow<List<PianoTuningInfo>> {
        return db.tuningsDao().getTuningsList()
    }

    suspend fun getCompleteTuningsList(): List<PianoTuning> {
        return db.tuningsDao().getCompleteTuningsLists()
    }

    suspend fun getMany(ids: Array<String>): List<PianoTuning> {
        return db.tuningsDao().getManyTunings(ids.toList())
    }

    suspend fun getTuning(id: String): PianoTuning {
        return db.tuningsDao().getTuning(id)
            ?: throw IllegalStateException("Tuning not found")
    }

    @JvmOverloads
    suspend fun addTuning(tuning: PianoTuning, updateLastModified: Boolean = true): PianoTuning {
        if (tuning.id == PianoTuning.NO_ID) {
            tuning.id = PianoTuning.generateId()
        }
        if (updateLastModified) {
            tuning.lastModified = Date()
        }
        db.tuningsDao().addOrUpdateTuning(tuning)
        return tuning
    }

    suspend fun checkExisting(tunings: List<PianoTuning>): List<Pair<PianoTuning, Boolean>> {
        val ids = tunings.map { it.id }.toList()
        val idsInDb = db.tuningsDao().checkExistingIds(ids)
        return tunings.map { it to idsInDb.contains(it.id) }
    }

    suspend fun copyTuning(id: String): PianoTuning {
        val tuning = getTuning(id)
        val copy = tuning.copy(id = PianoTuning.generateId(), name = tuning.name + " (copy)")
        copy.lastModified = Date()
        db.tuningsDao().addOrUpdateTuning(copy)
        return copy
    }

    suspend fun updateTuningName(id: String, name: String): PianoTuning {
        val tuning = getTuning(id)
        tuning.name = name
        tuning.lastModified = Date()
        db.tuningsDao().addOrUpdateTuning(tuning)
        return tuning
    }

    suspend fun updateTuning(tuning: PianoTuning, updateLastModified: Boolean = true): PianoTuning {
        if (updateLastModified) {
            tuning.lastModified = Date()
        }
        db.tuningsDao().addOrUpdateTuning(tuning)
        return tuning
    }

    suspend fun deleteTuning(tuning: PianoTuning): Int {
        db.tuningsDao().deleteTuning(tuning)
        return 1
    }

    suspend fun deleteById(id: String): Int {
        db.tuningsDao().deleteById(id)
        return 1
    }

    suspend fun deleteManyByIds(ids: Array<String>): Int {
        db.tuningsDao().deleteMany(ids.toList())
        return ids.size
    }

}