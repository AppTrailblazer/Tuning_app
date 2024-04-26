package com.willeypianotuning.toneanalyzer.extensions

import android.content.res.Resources
import android.util.DisplayMetrics

fun Int.dpToPx(resources: Resources = Resources.getSystem()): Float {
    return this.toFloat().dpToPx(resources)
}

fun Float.dpToPx(resources: Resources = Resources.getSystem()): Float {
    return this * (resources.displayMetrics.densityDpi.toFloat() / DisplayMetrics.DENSITY_DEFAULT)
}