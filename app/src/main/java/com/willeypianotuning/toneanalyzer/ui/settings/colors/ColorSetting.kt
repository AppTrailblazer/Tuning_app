package com.willeypianotuning.toneanalyzer.ui.settings.colors

import android.graphics.Color
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.willeypianotuning.toneanalyzer.ui.colors.ColorScheme

data class ColorSetting(
    @StringRes
    val title: Int,
    @DrawableRes
    val icon: Int,
    @ColorInt
    val color: Int,
    @ColorInt
    val defaultColor: Int,
    val alphaAllowed: Boolean = false,
    val update: (ColorScheme, Int) -> ColorScheme,
) {
    val isTransparent = Color.alpha(color) == 0
}