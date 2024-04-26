package com.willeypianotuning.toneanalyzer.tasks.backup

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.willeypianotuning.toneanalyzer.AppNotificationManager
import com.willeypianotuning.toneanalyzer.AppSettings
import com.willeypianotuning.toneanalyzer.R
import com.willeypianotuning.toneanalyzer.store.dropbox.DropboxClientFactory
import com.willeypianotuning.toneanalyzer.tasks.backup.target.BackupTarget
import com.willeypianotuning.toneanalyzer.tasks.backup.target.DropboxBackupTarget
import com.willeypianotuning.toneanalyzer.ui.main.MainActivity
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import timber.log.Timber
import java.util.*
import java.util.concurrent.TimeUnit

@HiltWorker
class BackupWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val backupManager: BackupManager,
    private val appSettings: AppSettings
) : CoroutineWorker(context, params) {

    private fun createBackupTargets(): List<BackupTarget> {
        val targets = arrayListOf<BackupTarget>()

        val dropBoxToken = DropboxClientFactory.getCredentials(appSettings)
        if (dropBoxToken != null) {
            targets.add(DropboxBackupTarget(dropBoxToken))
        }

        return targets
    }

    override suspend fun doWork(): Result {
        val notificationManager = AppNotificationManager(applicationContext)
        notificationManager.createBackupsNotificationChannel()

        val backupTargets = createBackupTargets()
        if (backupTargets.isEmpty()) {
            return Result.success()
        }

        val backupDate = Date()

        kotlin.runCatching {
            val filesToBackup =
                backupManager.getFilesForBackup(inputData.getBoolean(KEY_FORCE_BACKUP, false))
            if (filesToBackup.isEmpty()) {
                return Result.success()
            }

            val backupFile = backupManager.createBackupFileFrom(filesToBackup, backupDate)

            for (target in backupTargets) {
                target.backup(backupFile)
            }
        }
            .onFailure {
                Timber.e(it, "Failed to perform backup")
                showNotification(applicationContext.getString(R.string.backups_notification_backup_failed))
            }
            .onSuccess {
                appSettings.lastBackupDate = backupDate
            }

        return Result.success()
    }

    private fun showNotification(message: String) {
        val intent = Intent(applicationContext, MainActivity::class.java)
        var pendingIntentFlags = PendingIntent.FLAG_UPDATE_CURRENT
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            pendingIntentFlags = pendingIntentFlags or PendingIntent.FLAG_IMMUTABLE
        }
        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            0,
            intent,
            pendingIntentFlags
        )
        val notification = NotificationCompat.Builder(
            applicationContext,
            AppNotificationManager.BACKUPS_CHANNEL_ID
        )
            .setContentTitle(applicationContext.getString(R.string.app_name))
            .setContentText(message)
            .setColorized(true)
            .setContentIntent(pendingIntent)
            .setColor(ContextCompat.getColor(applicationContext, R.color.menu_background))
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setDefaults(Notification.DEFAULT_ALL)
            .setSmallIcon(R.drawable.ic_notification_icon)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()

        val manager =
            applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(BACKUP_NOTIFICATION_ID, notification)
    }

    companion object {
        private const val BACKUP_WORK_NAME = "pianometer.jobs.backup"
        private const val KEY_FORCE_BACKUP = "forceBackup"
        private const val BACKUP_NOTIFICATION_ID = 3

        @JvmStatic
        fun scheduleAutomaticBackup(context: Context, replace: Boolean = false) {
            val prefs = AppSettings(context.applicationContext)
            if (!prefs.automaticBackupEnabled()) {
                Timber.d("Backups are disabled")
                WorkManager.getInstance(context.applicationContext)
                    .cancelUniqueWork(BACKUP_WORK_NAME)
                return
            }

            val constraintsBuilder = Constraints.Builder()
                .setRequiresBatteryNotLow(true)
                .setRequiredNetworkType(NetworkType.CONNECTED)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                constraintsBuilder.setRequiresDeviceIdle(true)
            }
            val constraints = constraintsBuilder.build()

            val repeatInterval = maxOf(
                prefs.backupRepeatInterval(),
                PeriodicWorkRequest.MIN_PERIODIC_INTERVAL_MILLIS
            )
            val flexInterval =
                maxOf(repeatInterval / 24, PeriodicWorkRequest.MIN_PERIODIC_FLEX_MILLIS)
            val request = PeriodicWorkRequestBuilder<BackupWorker>(
                repeatInterval, TimeUnit.MILLISECONDS, flexInterval, TimeUnit.MILLISECONDS
            )
                .setConstraints(constraints)
                .build()

            val strategy =
                if (replace) ExistingPeriodicWorkPolicy.REPLACE else ExistingPeriodicWorkPolicy.KEEP
            WorkManager.getInstance(context.applicationContext)
                .enqueueUniquePeriodicWork(BACKUP_WORK_NAME, strategy, request)
        }
    }
}