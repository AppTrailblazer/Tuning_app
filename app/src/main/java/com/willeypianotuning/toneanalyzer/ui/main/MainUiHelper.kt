package com.willeypianotuning.toneanalyzer.ui.main

import android.app.Activity
import android.graphics.Matrix
import android.graphics.Point
import android.os.Build
import android.util.DisplayMetrics
import android.widget.ImageView
import com.willeypianotuning.toneanalyzer.R
import com.willeypianotuning.toneanalyzer.utils.BitmapUtils

object MainUiHelper {
    private fun getScreenSize(activity: Activity): Point {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val bounds = activity.windowManager.currentWindowMetrics.bounds
            Point(bounds.width(), bounds.height())
        } else {
            val displayMetrics = DisplayMetrics()
            @Suppress("DEPRECATION")
            activity.windowManager.defaultDisplay.getMetrics(displayMetrics)
            Point(displayMetrics.widthPixels, displayMetrics.heightPixels)
        }
    }

    @JvmStatic
    fun configurePanels(
        activity: Activity,
        backPanel: ImageView,
        topPanel: ImageView,
        nightMode: Boolean
    ) {
        val size = getScreenSize(activity)
        val width = (0.8 * size.x).toInt()
        val height = (0.8 * size.y).toInt()

        if (nightMode) {
            // reduces GPU overdraw, by removing window background
            // which is overdrawn by custom layout background anyway, so not visible
            activity.window.setBackgroundDrawableResource(R.color.black)
            backPanel.setImageDrawable(null)
        } else {
            // reduces GPU overdraw, by removing window background
            // which is overdrawn by custom layout background anyway, so not visible
            activity.window.setBackgroundDrawable(null)
            backPanel.setImageResource(R.drawable.back_panel)
        }

        val utils = BitmapUtils(activity)
        val drawableRes: Int = if (nightMode) R.drawable.top_panel_night else R.drawable.top_panel
        topPanel.setImageBitmap(utils.decodeBitmap(drawableRes, width, height))
        topPanel.post { prepareTopPanelImage(topPanel) }
        activity.window.decorView.setOnSystemUiVisibilityChangeListener { visibility: Int ->
            prepareTopPanelImage(topPanel)
        }
    }

    @JvmStatic
    fun prepareTopPanelImage(imageView: ImageView) {
        if (imageView.width == 0 || imageView.height == 0) {
            return
        }
        val drawable = imageView.drawable ?: return
        val centerX = imageView.width / 2f
        val scaleX = imageView.width * 1f / drawable.intrinsicWidth
        val scaleY = imageView.height * 1f / drawable.intrinsicHeight
        val scale = maxOf(scaleX, scaleY)
        val offsetX = (imageView.width - drawable.intrinsicWidth) / 2f
        val matrix = Matrix()
        matrix.setScale(scale, scale, centerX, 0f)
        matrix.preTranslate(offsetX, 0f)
        imageView.scaleType = ImageView.ScaleType.MATRIX
        imageView.imageMatrix = matrix
        imageView.setImageDrawable(null)
        imageView.setImageDrawable(drawable)
    }
}