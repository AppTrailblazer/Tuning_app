package com.willeypianotuning.toneanalyzer.ui.main

import android.graphics.drawable.ShapeDrawable
import android.graphics.drawable.shapes.RoundRectShape
import androidx.annotation.ColorInt

class ChartBackgroundDrawable(
    radius: Float,
    @ColorInt
    color: Int,
) : ShapeDrawable(
    RoundRectShape(
        floatArrayOf(radius, radius, radius, radius, 0f, 0f, 0f, 0f),
        null,
        null
    )
) {
    init {
        paint.color = color
    }

    var color: Int
        get() = paint.color
        set(value) {
            paint.color = value
        }
}