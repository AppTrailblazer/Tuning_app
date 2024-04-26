package com.willeypianotuning.toneanalyzer.ui.main.views

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.animation.RotateAnimation
import android.widget.ImageView
import android.widget.RelativeLayout
import androidx.annotation.ColorInt
import androidx.core.view.isVisible
import com.willeypianotuning.toneanalyzer.R
import com.willeypianotuning.toneanalyzer.ui.colors.ColorFilter
import com.willeypianotuning.toneanalyzer.ui.views.DialView
import com.willeypianotuning.toneanalyzer.ui.views.RingView

class TuningWheelView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0
) : RelativeLayout(context, attrs, defStyleAttr, defStyleRes) {
    private val dialView: DialView by lazy { findViewById(R.id.dialView) }
    private val ring1: RingView by lazy { findViewById(R.id.ring1) }
    private val ring2: RingView by lazy { findViewById(R.id.ring2) }
    private val ring3: RingView by lazy { findViewById(R.id.ring3) }
    private val ring4: RingView by lazy { findViewById(R.id.ring4) }
    private val arrow: ImageView by lazy { findViewById(R.id.arrow) }

    private var prevAngle: Float = 0.0f

    val ringViews: List<RingView> by lazy {
        listOf(
            ring4,
            ring3,
            ring2,
            ring1
        )
    }

    init {
        LayoutInflater.from(context).inflate(R.layout.view_tuning_wheel, this, true)
    }

    fun setDialAngle(angle: Float) {
        dialView.dialAngle = angle
    }

    fun setRingColor(@ColorInt color: Int) {
        ringViews.forEach { it.setColor(color) }
    }

    fun setRingLabelColor(@ColorInt color: Int) {
        ringViews.forEach { it.setLabelColor(color) }
    }

    fun setRingLabelTextColor(@ColorInt color: Int) {
        ringViews.forEach { it.setLabelTextColor(color) }
    }

    fun setDialColor(@ColorInt color: Int) {
        dialView.setDialColor(color)
    }

    fun setDialMarkingsColor(@ColorInt color: Int) {
        dialView.setDialMarkingsColor(color)
    }

    fun setOuterRingColor(@ColorInt color: Int) {
        dialView.setOuterRingColor(color)
    }

    fun setArrowColor(@ColorInt color: Int) {
        ColorFilter(color).applyTo(arrow)
    }

    fun setArrowAngle(needleAngle: Float) {
        arrow.clearAnimation()
        val newAngle: Float = needleAngle - dialView.dialAngle
        val needleAnimation = RotateAnimation(
            prevAngle,
            newAngle,
            RotateAnimation.RELATIVE_TO_SELF,
            0.5f,
            RotateAnimation.RELATIVE_TO_SELF,
            1.0f
        )
        needleAnimation.fillAfter = true
        needleAnimation.duration = 100

        arrow.isVisible = true
        arrow.setLayerType(LAYER_TYPE_SOFTWARE, null)
        arrow.startAnimation(needleAnimation)
        prevAngle = newAngle
    }

    fun hideNeedle() {
        arrow.clearAnimation()
        arrow.isVisible = false
    }

}