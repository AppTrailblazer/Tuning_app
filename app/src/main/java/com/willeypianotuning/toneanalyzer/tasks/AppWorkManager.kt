package com.willeypianotuning.toneanalyzer.tasks

import android.content.Context
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.WorkManager

object AppWorkManager {
    @JvmStatic
    fun initializeWorkManager(context: Context, workerFactory: HiltWorkerFactory) {
        WorkManager.initialize(
            context.applicationContext, Configuration.Builder()
                .setWorkerFactory(workerFactory)
                .build()
        )

        // removes outdated tasks
        WorkManager.getInstance(context.applicationContext)
            .cancelUniqueWork("pianometer.backups.external_storage")
    }
}