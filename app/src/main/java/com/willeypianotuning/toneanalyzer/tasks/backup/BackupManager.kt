package com.willeypianotuning.toneanalyzer.tasks.backup

import android.content.Context
import com.willeypianotuning.toneanalyzer.AppSettings
import com.willeypianotuning.toneanalyzer.store.PianoTuningDataStore
import com.willeypianotuning.toneanalyzer.store.TemperamentDataStore
import com.willeypianotuning.toneanalyzer.store.TuningStyleDataStore
import com.willeypianotuning.toneanalyzer.store.db.tunings.PianoTuning
import com.willeypianotuning.toneanalyzer.sync.EtfzFileWriter
import com.willeypianotuning.toneanalyzer.utils.Hardware
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

class BackupManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val appSettings: AppSettings,
    private val tuningDs: PianoTuningDataStore,
    private val temperamentDs: TemperamentDataStore,
    private val tuningStyleDs: TuningStyleDataStore
) {
    suspend fun getFilesForBackup(force: Boolean = false): List<PianoTuning> {
        val tunings = tuningDs.getCompleteTuningsList()
        if (tunings.isEmpty()) {
            Timber.d("No tunings. Skipping backup")
            return emptyList()
        }

        if (force) {
            return tunings
        }

        val tuningsChangedAfterLastBackup =
            tunings.filter { it.lastModified.time > (appSettings.lastBackupDate?.time ?: 0) }
        if (tuningsChangedAfterLastBackup.isEmpty()) {
            Timber.d("No changes in tunings since last backup. Skipping backup")
            return emptyList()
        }

        return tunings
    }

    suspend fun createBackupFileFrom(tunings: List<PianoTuning>, timestamp: Date): File {
        val df = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US)
        val outputDir = context.cacheDir
        if (!outputDir.exists()) {
            outputDir.mkdirs()
        }
        val deviceName = Hardware.deviceName.replace(Regex("\\s+"), "_")
        val outputFile = File(outputDir, "backup_${df.format(timestamp)}_$deviceName.etfz")
        EtfzFileWriter(outputFile).use {
            it.writeTunings(tunings)
            it.writeTemperaments(temperamentDs.customTemperaments())
            it.writeTuningStyles(tuningStyleDs.customStyles())
        }
        outputFile.deleteOnExit()
        return outputFile
    }


}
