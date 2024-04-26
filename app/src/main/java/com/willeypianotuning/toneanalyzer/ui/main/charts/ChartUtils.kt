package com.willeypianotuning.toneanalyzer.ui.main.charts

import android.app.Activity
import timber.log.Timber
import java.util.concurrent.ExecutionException
import java.util.concurrent.FutureTask

object ChartUtils {
    fun runOnUiThreadAndWait(activity: Activity, runnable: Runnable) {
        val task = FutureTask(runnable, Unit)
        activity.runOnUiThread(task)
        try {
            task.get()
        } catch (e: InterruptedException) {
            Timber.e("Cannot run on ui thread")
        } catch (e: ExecutionException) {
            Timber.e("Cannot run on ui thread")
        }
    }
}