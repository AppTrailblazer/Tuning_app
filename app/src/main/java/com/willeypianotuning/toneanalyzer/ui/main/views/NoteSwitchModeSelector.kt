package com.willeypianotuning.toneanalyzer.ui.main.views

import android.content.Context
import android.graphics.PorterDuff
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.willeypianotuning.toneanalyzer.R
import com.willeypianotuning.toneanalyzer.audio.enums.NoteChangeMode
import com.willeypianotuning.toneanalyzer.extensions.setDebounceOnClickListener

class NoteSwitchModeSelector @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0
) : RelativeLayout(context, attrs, defStyleAttr, defStyleRes) {
    private val modeSlider: ModeSlider by lazy { findViewById(R.id.modeSlider) }
    private val noteSwitchModeAuto: View by lazy { findViewById(R.id.noteSwitchModeAuto) }
    private val noteSwitchModeStep: View by lazy { findViewById(R.id.noteSwitchModeStep) }
    private val noteSwitchModeLock: View by lazy { findViewById(R.id.noteSwitchModeLock) }
    private val noteSwitchModeAutoText: TextView by lazy { findViewById(R.id.noteSwitchModeAutoText) }
    private val noteSwitchModeStepText: TextView by lazy { findViewById(R.id.noteSwitchModeStepText) }
    private val noteSwitchModeLockText: TextView by lazy { findViewById(R.id.noteSwitchModeLockText) }
    private val noteSwitchModeAutoIcon: ImageView by lazy { findViewById(R.id.noteSwitchModeAutoIcon) }
    private val noteSwitchModeStepIcon: ImageView by lazy { findViewById(R.id.noteSwitchModeStepIcon) }
    private val noteSwitchModeLockIcon: ImageView by lazy { findViewById(R.id.noteSwitchModeLockIcon) }

    private val noteSwitchModeTexts: List<TextView>
        get() = listOf(
            noteSwitchModeAutoText,
            noteSwitchModeStepText,
            noteSwitchModeLockText,
        )

    private val noteSwitchModeIcons: List<ImageView>
        get() = listOf(
            noteSwitchModeAutoIcon,
            noteSwitchModeStepIcon,
            noteSwitchModeLockIcon,
        )

    var onValueChangeListener: ((Int) -> Unit)? = null

    init {
        LayoutInflater.from(context).inflate(R.layout.view_note_switch_mode_selector, this, true)

        if (!isInEditMode) {
            val imageHeight = resources.getDimensionPixelSize(R.dimen.modes_seekbar_height)
            val minY = imageHeight / 2
            val maxY = resources.getDimensionPixelSize(R.dimen.modes_height) - imageHeight / 2
            modeSlider.initialize(minY, maxY)
            modeSlider.setOnValueChangeListener { mode: Int, _: Boolean ->
                onValueChangeListener?.invoke(mode)
            }

            noteSwitchModeAuto.setDebounceOnClickListener { onOnChangeModeClicked(NoteChangeMode.AUTO) }
            noteSwitchModeStep.setDebounceOnClickListener { onOnChangeModeClicked(NoteChangeMode.STEP) }
            noteSwitchModeLock.setDebounceOnClickListener { onOnChangeModeClicked(NoteChangeMode.LOCK) }
        }

        context.obtainStyledAttributes(attrs, R.styleable.NoteSwitchModeSelector).let {
            setTextVisible(it.getBoolean(R.styleable.NoteSwitchModeSelector_ns_showLabel, true))
            setColor(
                it.getColor(
                    R.styleable.NoteSwitchModeSelector_android_tint,
                    ContextCompat.getColor(context, R.color.note_switching_color)
                )
            )
            it.recycle()
        }
    }

    private fun onOnChangeModeClicked(noteChangeMode: Int) {
        modeSlider.setMode(noteChangeMode)
        onValueChangeListener?.invoke(noteChangeMode)
    }

    fun setMode(mode: Int) {
        modeSlider.setMode(mode)
    }

    fun setTextVisible(visible: Boolean) {
        noteSwitchModeTexts.forEach { it.isVisible = visible }
    }

    fun setColor(@ColorInt color: Int) {
        noteSwitchModeTexts.forEach { it.setTextColor(color) }
        noteSwitchModeIcons.forEach { it.setColorFilter(color, PorterDuff.Mode.SRC_IN) }
    }
}