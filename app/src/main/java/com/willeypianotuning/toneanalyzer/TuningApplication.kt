package com.willeypianotuning.toneanalyzer

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import androidx.hilt.work.HiltWorkerFactory
import com.github.mikephil.charting.utils.Utils
import com.google.firebase.FirebaseApp
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import com.willeypianotuning.toneanalyzer.tasks.AppWorkManager.initializeWorkManager
import com.willeypianotuning.toneanalyzer.tasks.backup.BackupWorker.Companion.scheduleAutomaticBackup
import com.willeypianotuning.toneanalyzer.tasks.cleanup.CleanupWorker.Companion.scheduleCleanup
import com.willeypianotuning.toneanalyzer.utils.CrashReportingTree
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber
import timber.log.Timber.DebugTree
import javax.inject.Inject

@HiltAndroidApp
class TuningApplication : Application() {
    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var appSettings: AppSettings

    override fun onCreate() {
        super.onCreate()
        initializeFirebase()
        initializeLogging()
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true)
        AppCompatDelegate.setDefaultNightMode(appSettings.appearance)
        initializeWorkManager(this, workerFactory)
        scheduleCleanup(this)
        scheduleAutomaticBackup(this, false)
        Utils.init(this)
    }

    private fun initializeFirebase() {
        // although Firebase app should get initialized by the FirebaseInitProvider (ContentProvider)
        // for some reason we were getting reporting from Redmi K20 that the FirebaseApp is not initialized
        FirebaseApp.initializeApp(this)
        FirebaseCrashlytics.getInstance()
            .setCrashlyticsCollectionEnabled(!BuildConfig.DEBUG)
        val remoteConfig = FirebaseRemoteConfig.getInstance()
        val configSettings = FirebaseRemoteConfigSettings.Builder()
            .setMinimumFetchIntervalInSeconds(if (BuildConfig.DEBUG) 10 else 3600.toLong())
            .build()
        remoteConfig.setConfigSettingsAsync(configSettings)
        remoteConfig.fetchAndActivate()
    }

    private fun initializeLogging() {
        if (BuildConfig.DEBUG) {
            Timber.plant(DebugTree())
        } else {
            Timber.plant(CrashReportingTree())
        }
    }
}