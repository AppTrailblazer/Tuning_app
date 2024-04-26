package com.willeypianotuning.toneanalyzer

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.graphics.Color
import android.os.Build
import com.willeypianotuning.toneanalyzer.extensions.hasNotificationsPermission
import timber.log.Timber

class AppNotificationManager(private val context: Context) {

    companion object {
        const val BACKUPS_CHANNEL_ID = BuildConfig.APPLICATION_ID + ".BACKUP_RESTORE"
        private const val BACKUPS_CHANNEL_NAME = "PianoMeter Backup/Restore Notifications"
        private const val BACKUPS_CHANNEL_DESCRIPTION =
            "Notifications about status of backup/restore process"
    }

    fun createChannels() {
        createBackupsNotificationChannel()
    }

    fun createBackupsNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return
        }
        if (!context.hasNotificationsPermission()) {
            Timber.d("No permission to send notifications")
            return
        }

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (notificationManager.notificationChannels.any { it.id == BACKUPS_CHANNEL_ID }) {
            Timber.d("Channel already exists")
            return
        }

        val channel = NotificationChannel(
            BACKUPS_CHANNEL_ID,
            BACKUPS_CHANNEL_NAME,
            NotificationManager.IMPORTANCE_DEFAULT
        )
        channel.description = BACKUPS_CHANNEL_DESCRIPTION
        channel.enableLights(true)
        channel.lightColor = Color.GREEN
        channel.enableVibration(true)

        notificationManager.createNotificationChannel(channel)
    }
}

