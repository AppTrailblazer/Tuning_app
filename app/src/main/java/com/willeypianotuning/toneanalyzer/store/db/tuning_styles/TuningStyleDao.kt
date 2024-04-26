package com.willeypianotuning.toneanalyzer.store.db.tuning_styles

import androidx.room.*

@Dao
interface TuningStyleDao {

    @Query("SELECT * FROM TuningStyle")
    suspend fun getStyles(): List<TuningStyle>

    @Query("SELECT * FROM TuningStyle WHERE id = :id")
    suspend fun getStyle(id: String): TuningStyle?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: TuningStyle): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(items: List<TuningStyle>)

    @Update(onConflict = OnConflictStrategy.REPLACE)
    suspend fun update(item: TuningStyle)

    @Delete
    suspend fun delete(item: TuningStyle)

}