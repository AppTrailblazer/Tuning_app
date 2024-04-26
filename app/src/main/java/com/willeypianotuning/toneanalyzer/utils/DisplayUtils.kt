package com.willeypianotuning.toneanalyzer.utils

import android.app.Activity
import android.graphics.Point
import android.os.Build

object DisplayUtils {

    @Suppress("deprecation")
    private fun getDisplaySizeOldWay(activity: Activity): Point {
        val size = Point()
        activity.windowManager.defaultDisplay.getSize(size)
        return size
    }

    fun screenSize(activity: Activity): Point {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val bounds = activity.windowManager.currentWindowMetrics.bounds
            Point(bounds.width(), bounds.height())
        } else {
            getDisplaySizeOldWay(activity)
        }
    }
}