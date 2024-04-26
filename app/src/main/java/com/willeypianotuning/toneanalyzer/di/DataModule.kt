package com.willeypianotuning.toneanalyzer.di

import android.content.Context
import androidx.room.Room
import com.willeypianotuning.toneanalyzer.AppSettings
import com.willeypianotuning.toneanalyzer.store.db.PianoTuningDatabase
import com.willeypianotuning.toneanalyzer.store.db.migrations.RoomMigration1to2
import com.willeypianotuning.toneanalyzer.store.db.migrations.RoomMigration2to3
import com.willeypianotuning.toneanalyzer.store.db.migrations.RoomMigration3to4
import com.willeypianotuning.toneanalyzer.store.db.migrations.RoomMigration4to5
import com.willeypianotuning.toneanalyzer.store.db.temperaments.TemperamentDao
import com.willeypianotuning.toneanalyzer.store.db.tuning_styles.TuningStyleDao
import com.willeypianotuning.toneanalyzer.store.db.tunings.PianoTuningDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DataModule {
    @Provides
    @Singleton
    fun provideAppSettings(@ApplicationContext context: Context): AppSettings {
        return AppSettings(context)
    }

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): PianoTuningDatabase {
        return Room.databaseBuilder(
            context,
            PianoTuningDatabase::class.java,
            "tunings.db"
        )
            .allowMainThreadQueries()
            .addMigrations(
                RoomMigration1to2(),
                RoomMigration2to3(),
                RoomMigration3to4(),
                RoomMigration4to5()
            )
            .build()
    }

    @Provides
    @Singleton
    fun providePianoTuningDao(db: PianoTuningDatabase): PianoTuningDao {
        return db.tuningsDao()
    }

    @Provides
    @Singleton
    fun providePianoTuningStyleDao(db: PianoTuningDatabase): TuningStyleDao {
        return db.tuningStyleDao()
    }

    @Provides
    @Singleton
    fun provideTemperamentDao(db: PianoTuningDatabase): TemperamentDao {
        return db.temperamentDao()
    }
}