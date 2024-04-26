package com.willeypianotuning.toneanalyzer.store.db

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PianoTuningDatabaseTest {

    @Rule
    @JvmField
    val migrationTestHelper = MigrationTestHelper(
            InstrumentationRegistry.getInstrumentation(),
            PianoTuningDatabase::class.java.canonicalName!!,
            FrameworkSQLiteOpenHelperFactory()
    )

    @Test
    fun testDatabaseMigrations() {
        val db = migrationTestHelper.createDatabase(TEST_DB_NAME, 1)
        db.close()

        val appDatabase = MigrationUtil.getDatabaseAfterPerformingMigrations(
                migrationTestHelper,
                PianoTuningDatabase::class.java,
                TEST_DB_NAME
        ) as PianoTuningDatabase

        appDatabase.close()
    }

    companion object {
        private const val TEST_DB_NAME = "TestAppDatabase.db"
    }
}