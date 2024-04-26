package com.willeypianotuning.toneanalyzer.store.db.tunings

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface PianoTuningDao {

    @Query("SELECT id, name, make, model, type, last_modified FROM Tunings")
    fun getTuningsList(): Flow<List<PianoTuningInfo>>

    @Query("SELECT * FROM Tunings")
    suspend fun getCompleteTuningsLists(): List<PianoTuning>

    @Query("SELECT * FROM Tunings WHERE id = :id")
    suspend fun getTuning(id: String): PianoTuning?

    @Query("SELECT * FROM Tunings WHERE id IN (:ids)")
    suspend fun getManyTunings(ids: List<String>): List<PianoTuning>

    @Query("SELECT id FROM Tunings WHERE id IN (:ids)")
    suspend fun checkExistingIds(ids: List<String>): List<String>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addOrUpdateTuning(tuning: PianoTuning)

    @Delete
    suspend fun deleteTuning(tuning: PianoTuning)

    @Query("DELETE FROM Tunings WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM Tunings WHERE id IN (:ids)")
    suspend fun deleteMany(ids: List<String>)

    @Query("DELETE FROM Tunings")
    suspend fun deleteAll()

}