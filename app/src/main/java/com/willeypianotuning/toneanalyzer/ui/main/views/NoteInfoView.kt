package com.willeypianotuning.toneanalyzer.ui.main.views

import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import android.util.TypedValue
import android.view.LayoutInflater
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import com.willeypianotuning.toneanalyzer.R
import com.willeypianotuning.toneanalyzer.audio.note_names.NoteNames
import com.willeypianotuning.toneanalyzer.audio.note_names.NoteNamingConvention
import com.willeypianotuning.toneanalyzer.extensions.setDebounceOnClickListener
import com.willeypianotuning.toneanalyzer.ui.colors.ColorFilter
import java.util.Locale

class NoteInfoView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0, defStyleRes: Int = 0
) : RelativeLayout(context, attrs, defStyleAttr, defStyleRes) {
    private val indicator: ImageView by lazy { findViewById(R.id.indicator) }
    private val currentNoteNameTextView: TextView by lazy { findViewById(R.id.currentNoteNameTextView) }
    private val currentNoteOffsetTextView: TextView by lazy { findViewById(R.id.currentNoteOffsetTextView) }
    private val currentNoteOffsetLock: ImageButton by lazy { findViewById(R.id.currentNoteOffsetLock) }

    private var locked: Boolean = false

    private var noteNamingConvention: NoteNamingConvention = NoteNames.getNamingConvention(
        context, NoteNames.NAME_A0_B3_C4_C8
    )

    private var circleTxtSize = 0f

    init {
        LayoutInflater.from(context).inflate(R.layout.view_note_info, this, true)
        circleTxtSize = currentNoteNameTextView.textSize
    }

    fun setLocked(locked: Boolean) {
        currentNoteOffsetLock.isVisible = locked
        currentNoteOffsetTextView.isInvisible = locked
        this.locked = locked
    }

    fun onLockClicked(onClick: () -> Unit) {
        currentNoteOffsetLock.setDebounceOnClickListener { onClick() }
    }

    fun setTextColor(@ColorInt color: Int) {
        currentNoteOffsetTextView.setTextColor(color)
        currentNoteNameTextView.setTextColor(color)
    }

    override fun setBackgroundColor(@ColorInt color: Int) {
        indicator.background = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(color)
        }
    }

    fun setBorderColor(@ColorInt color: Int) {
        ColorFilter(color).applyTo(indicator.drawable)
    }

    fun setNoteOffset(offset: Float) {
        currentNoteOffsetTextView.text = when {
            !locked -> {
                val numf = (offset * 10.0f + 0.5f).toInt() / 10.0f
                String.format(Locale.getDefault(), "%.1f", numf)
            }

            else -> "-"
        }
    }

    fun setNote(note: Int) {
        val noteNameTextSizeCoefficient =
            if (noteNamingConvention.usesSolmizationSystem()) 0.9f else 1.0f
        currentNoteNameTextView.setTextSize(
            TypedValue.COMPLEX_UNIT_PX, noteNameTextSizeCoefficient * circleTxtSize
        )
        val noteName = noteNamingConvention.pianoNoteName(note - 1)
        if (noteName.toString() != currentNoteNameTextView.text.toString()) {
            currentNoteNameTextView.setText(noteName, TextView.BufferType.SPANNABLE)
        }
    }

    fun setNoteNaming(@NoteNames.NoteNaming naming: Int) {
        noteNamingConvention = NoteNames.getNamingConvention(context, naming)
    }

}