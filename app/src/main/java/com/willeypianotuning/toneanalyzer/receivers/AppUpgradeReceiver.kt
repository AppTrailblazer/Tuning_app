package com.willeypianotuning.toneanalyzer.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.willeypianotuning.toneanalyzer.tasks.backup.BackupWorker
import timber.log.Timber

class AppUpgradeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (Intent.ACTION_MY_PACKAGE_REPLACED != intent?.action) {
            return
        }
        // on app update we replace the worker job
        // as configuration of the job might have changed
        Timber.d("App has been updated. Rescheduling background tasks")
        BackupWorker.scheduleAutomaticBackup(context, true)
    }

}