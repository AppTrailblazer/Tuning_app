package com.willeypianotuning.toneanalyzer.extensions

import android.app.Activity
import android.view.WindowManager

fun Activity.keepScreenOn(on: Boolean) {
    window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    if (on) {
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }
}