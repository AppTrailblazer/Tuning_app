package com.willeypianotuning.toneanalyzer.ui.colors

import android.graphics.BlendMode
import android.graphics.BlendModeColorFilter
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.drawable.Drawable
import android.os.Build
import android.widget.ImageView
import androidx.annotation.FloatRange
import androidx.core.graphics.ColorUtils

data class ColorScheme constructor(
    val noteName: Int,
    val noteNameBackground: Int,
    val innerAndOuterRings: Int,
    val strobeWheels: Int,
    val strobeBackground: Int,
    val dialMarkings: Int,
    val needle: Int,
    val graphBackground: Int,
    val tuningCurveLine: Int,
    val tuningCurveDots: Int,
    val inharmonicityLine: Int,
    val inharmonicityDots: Int,
    val spectrumLine: Int,
    val currentNoteIndicator: Int,
    val menuPrimary: Int,
    val menuTextPrimary: Int,
    val backPanel: Int,
    val topPanel: Int,
    val autoStepLock: Int,
    val autoStepLockLand: Int,
) {
    companion object {
        val Default = ColorScheme(
            noteName = 0xFFFFFFFF.toInt(),
            noteNameBackground = 0xFF4a3636.toInt(),
            innerAndOuterRings = 0x00000000.toInt(),
            strobeWheels = 0xFF000000.toInt(),
            strobeBackground = 0xFFf6f6e8.toInt(),
            dialMarkings = 0x00000000.toInt(),
            needle = 0x00000000.toInt(),
            graphBackground = 0xFFece9e1.toInt(),
            tuningCurveLine = 0xFF000000.toInt(),
            tuningCurveDots = 0xFF0000ff.toInt(),
            inharmonicityLine = 0xFF0000ff.toInt(),
            inharmonicityDots = 0xFF0000ff.toInt(),
            spectrumLine = 0xFF000000.toInt(),
            currentNoteIndicator = 0xFFff0000.toInt(),
            menuPrimary = 0xFF443930.toInt(),
            menuTextPrimary = 0xFFffffff.toInt(),
            backPanel = 0x0047382E.toInt(),
            topPanel = 0x00000000.toInt(),
            autoStepLock = 0xFF46413e.toInt(),
            autoStepLockLand = 0xFFfafaec.toInt(),
        )

        val Dark = ColorScheme(
            noteName = 0xFFFFFFFF.toInt(),
            noteNameBackground = 0xFF000000.toInt(),
            innerAndOuterRings = 0x00000000.toInt(),
            strobeWheels = 0xFFFFFFFF.toInt(),
            strobeBackground = 0xFF121206.toInt(),
            dialMarkings = 0xFFFFFFFF.toInt(),
            needle = 0x00000000.toInt(),
            graphBackground = 0xFF000000.toInt(),
            tuningCurveLine = 0xFFF2F2F2.toInt(),
            tuningCurveDots = 0xFFA3DEFF.toInt(),
            inharmonicityLine = 0xFFA3DEFF.toInt(),
            inharmonicityDots = 0xFFA3DEFF.toInt(),
            spectrumLine = 0xFFF2F2F2.toInt(),
            currentNoteIndicator = 0xFFff0000.toInt(),
            menuPrimary = 0xFF443930.toInt(),
            menuTextPrimary = 0xFFffffff.toInt(),
            backPanel = 0xFF000000.toInt(),
            topPanel = 0x00000000.toInt(),
            autoStepLock = 0xFFEDEDDF.toInt(),
            autoStepLockLand = 0xFFEDEDDF.toInt(),
        )

        private fun isDark(color: Int): Boolean {
            return ColorUtils.calculateLuminance(color) < 0.5
        }

        private fun makeDarker(color: Int, @FloatRange(from = 0.0, to = 1.0) percent: Float): Int {
            return ColorUtils.blendARGB(color, Color.BLACK, percent)
        }

        private fun makeLighter(color: Int, @FloatRange(from = 0.0, to = 1.0) percent: Float): Int {
            return ColorUtils.blendARGB(color, Color.WHITE, percent)
        }
    }

    val ringLabelTextColor: Int = strobeWheels
    val ringLabelColor: Int = makeDarker(strobeBackground, 0.1f)

    val menuShadow: Int = ColorUtils.setAlphaComponent(
        makeDarker(menuPrimary, 0.75f),
        170
    )
    val menuTextSecondary: Int = makeDarker(menuTextPrimary, 0.25f)

    val toolbarColor: Int = makeDarker(menuPrimary, 0.5f)
}

class ColorFilter(private val color: Int) {
    private val isTransparent: Boolean get() = Color.alpha(color) == 0

    fun applyTo(view: ImageView) {
        if (isTransparent) {
            // transparent
            view.clearColorFilter()
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            view.colorFilter = BlendModeColorFilter(color, BlendMode.SRC_IN)
        } else {
            @Suppress("deprecation")
            view.setColorFilter(color, PorterDuff.Mode.SRC_IN)
        }
    }

    fun applyTo(drawable: Drawable) {
        if (color == Color.TRANSPARENT) {
            drawable.clearColorFilter()
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            drawable.colorFilter = BlendModeColorFilter(color, BlendMode.SRC_IN)
        } else {
            @Suppress("deprecation")
            drawable.setColorFilter(color, PorterDuff.Mode.SRC_IN)
        }
    }
}