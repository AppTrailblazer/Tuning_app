package com.willeypianotuning.toneanalyzer.utils

import android.util.Log
import com.google.firebase.crashlytics.FirebaseCrashlytics
import timber.log.Timber

class CrashReportingTree : Timber.Tree() {
    private val crashlytics = FirebaseCrashlytics.getInstance()

    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        if (priority == Log.VERBOSE) {
            return
        }
        logWithCrashlytics(priority, tag, message, t)
    }

    private fun priorityLabel(priority: Int): String {
        return when (priority) {
            Log.VERBOSE -> "V"
            Log.DEBUG -> "D"
            Log.INFO -> "I"
            Log.WARN -> "W"
            Log.ERROR -> "E"
            Log.ASSERT -> "A"
            else -> ""
        }
    }

    private fun logWithCrashlytics(priority: Int, tag: String?, message: String, t: Throwable?) {
        crashlytics.log(priorityLabel(priority) + "/" + tag + ": " + message)
        if (t != null) {
            crashlytics.recordException(t)
        }
    }
}