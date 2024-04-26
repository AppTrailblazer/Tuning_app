package com.willeypianotuning.toneanalyzer.ui.main.views

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.widget.RelativeLayout
import androidx.appcompat.widget.AppCompatImageView
import com.willeypianotuning.toneanalyzer.extensions.clamp
import timber.log.Timber

class ModeSlider @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AppCompatImageView(context, attrs, defStyleAttr) {
    fun interface OnValueChangeListener {
        fun onValueChanged(mode: Int, fromUser: Boolean)
    }

    private var maxY = 0
    private var minY = 0
    private var _yDelta = 0
    private var step = 1
    private var value = 0
    private var listener: OnValueChangeListener? = null

    fun initialize(minY: Int, maxY: Int) {
        this.minY = minY
        this.maxY = maxY
        step = (maxY - minY) / 2
    }

    fun setMode(_value: Int) {
        var newValue = _value - 1
        if (newValue < 0) newValue = 0
        if (newValue > 2) newValue = 2
        if (newValue != value) {
            value = newValue
        }
        updateTopMargin(minY + value * step)
    }

    fun setOnValueChangeListener(listener: OnValueChangeListener?) {
        this.listener = listener
    }

    private fun updateTopMargin(margin: Int) {
        val lParams = layoutParams as RelativeLayout.LayoutParams
        lParams.topMargin = maxOf(margin, 0)
        layoutParams = lParams
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val y = event.rawY.toInt()
        when (event.action and MotionEvent.ACTION_MASK) {
            MotionEvent.ACTION_DOWN -> {
                val lParams = layoutParams as RelativeLayout.LayoutParams
                _yDelta = y - lParams.topMargin
            }
            MotionEvent.ACTION_UP -> {
                var compY = y - _yDelta
                val newValue =
                    ((compY - minY).toFloat() / step.toFloat() + 0.5f).toInt().clamp(0, 2)
                if (newValue != value) {
                    value = newValue
                    listener?.onValueChanged(value + 1, true)
                }
                compY = minY + value * step
                updateTopMargin(compY)
            }
            MotionEvent.ACTION_MOVE -> {
                val compY = (y - _yDelta).clamp(minY, maxY)
                val newValue = ((compY - minY).toFloat() / step.toFloat() + 0.5f).toInt()
                if (newValue != value) {
                    value = newValue
                    listener?.onValueChanged(value + 1, true)
                }
                Timber.d("Compx:$compY ($minY/$maxY) value:$value")
                updateTopMargin(compY)
            }
        }
        return true
    }
}