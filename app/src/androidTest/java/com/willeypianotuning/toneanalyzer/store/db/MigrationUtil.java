package com.willeypianotuning.toneanalyzer.store.db;

import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.room.testing.MigrationTestHelper;
import androidx.test.core.app.ApplicationProvider;

public class MigrationUtil {

    public static RoomDatabase getDatabaseAfterPerformingMigrations(MigrationTestHelper migrationTestHelper,
                                                                    Class<? extends RoomDatabase> databaseClass, String databaseName,
                                                                    Migration... migrations) {
        RoomDatabase roomDatabase = Room.databaseBuilder(ApplicationProvider.getApplicationContext(),
                databaseClass, databaseName)
                .addMigrations(migrations)
                .build();
        migrationTestHelper.closeWhenFinished(roomDatabase);
        return roomDatabase;

    }
}