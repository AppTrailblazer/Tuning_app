package com.willeypianotuning.toneanalyzer.store.db.temperaments

import androidx.room.*

@Dao
interface TemperamentDao {

    @Query("SELECT * FROM Temperament")
    suspend fun getTemperaments(): List<Temperament>

    @Query("SELECT * FROM Temperament WHERE id = :id")
    suspend fun getTemperament(id: String): Temperament?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: Temperament): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(items: List<Temperament>)

    @Update(onConflict = OnConflictStrategy.REPLACE)
    suspend fun update(item: Temperament)

    @Delete
    suspend fun delete(item: Temperament)

}