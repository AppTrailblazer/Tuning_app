package com.willeypianotuning.toneanalyzer.ui.views

import android.content.Context
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.annotation.ColorInt
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.ContextCompat
import com.willeypianotuning.toneanalyzer.R
import com.willeypianotuning.toneanalyzer.ui.colors.ColorFilter

class DialView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0
) : FrameLayout(context, attrs, defStyleAttr, defStyleRes) {
    private val dial: ImageView
    private val rim: ImageView

    var dialAngle: Float
        get() = -dial.rotation
        set(value) {
            dial.rotation = -value
        }

    private val dialBg: GradientDrawable = GradientDrawable().apply {
        shape = GradientDrawable.OVAL
        setColor(ContextCompat.getColor(context, R.color.dial_background))
    }
    private val dialMarkings: Drawable =
        AppCompatResources.getDrawable(context, R.drawable.notes_dial_markings)!!

    private val finalDrawable = LayerDrawable(arrayOf(dialBg, dialMarkings)).apply {
        setLayerInset(0, 0, 0, 0, 0)
        setLayerInset(1, 0, 0, 0, 0)
    }

    private val outerRing: Drawable =
        AppCompatResources.getDrawable(context, R.drawable.dial_outer_rim)!!

    init {
        LayoutInflater.from(context).inflate(R.layout.view_dial, this, true)
        dial = findViewById(R.id.dialscreen)
        dial.setImageDrawable(finalDrawable)
        rim = findViewById(R.id.outerRim)
        rim.setImageDrawable(outerRing)
    }

    fun setDialColor(@ColorInt color: Int) {
        dialBg.setColor(color)
    }

    fun setDialMarkingsColor(@ColorInt color: Int) {
        ColorFilter(color).applyTo(dialMarkings)
    }

    fun setOuterRingColor(@ColorInt color: Int) {
        ColorFilter(color).applyTo(outerRing)
    }
}