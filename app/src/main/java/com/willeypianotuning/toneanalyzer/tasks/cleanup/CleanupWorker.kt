package com.willeypianotuning.toneanalyzer.tasks.cleanup

import android.content.Context
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import timber.log.Timber

class CleanupWorker(context: Context, params: WorkerParameters) : Worker(context, params) {
    /**
     * We generate tuning files for exporting in the cache directory
     */
    private fun clearCache() {
        val cacheFiles = applicationContext.cacheDir.listFiles() ?: return
        for (file in cacheFiles) {
            if (!file.isDirectory) {
                file.delete()
            }
        }
    }

    override fun doWork(): Result {
        kotlin.runCatching {
            clearCache()
            return Result.success()
        }.getOrElse {
            Timber.e(it, "Failed to run cleanup")
            return Result.failure()
        }
    }

    companion object {
        @JvmStatic
        fun scheduleCleanup(context: Context) {
            val request = OneTimeWorkRequestBuilder<CleanupWorker>()
                .build()
            WorkManager.getInstance(context.applicationContext).enqueue(request)
        }
    }
}