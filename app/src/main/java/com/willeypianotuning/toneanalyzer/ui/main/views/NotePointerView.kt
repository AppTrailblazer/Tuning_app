package com.willeypianotuning.toneanalyzer.ui.main.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import com.willeypianotuning.toneanalyzer.R

class NotePointerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0
) : View(context, attrs, defStyleAttr, defStyleRes) {
    private var pointerPaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG)
        .apply {
            color = 0xFFFF7777.toInt()
        }

    private var note: Int = 0

    private val notePointerHelper = NotePointerHelper()
    private var pointerOffset: Float = 0f
    private var pointerWidth: Float = 0f

    var color: Int
        get() = pointerPaint.color
        set(value) {
            pointerPaint.color = value
            invalidate()
        }

    init {
        init(attrs)
    }

    private fun init(attrs: AttributeSet?) {
        val ta = context.obtainStyledAttributes(attrs, R.styleable.NotePointerView)
        note = ta.getInteger(R.styleable.NotePointerView_np_currentNote, note)
        val borderWidth = ta.getDimensionPixelSize(R.styleable.NotePointerView_np_borderWidth, -1)
        if (borderWidth != -1) {
            notePointerHelper.customBorderWidth = borderWidth.toFloat()
        }
        ta.recycle()
    }

    fun getNote(): Int {
        return note
    }

    fun setNote(note: Int) {
        this.note = note
        updatePointer(width, height)
        invalidate()
    }

    @Suppress("UnusedParameter")
    private fun updatePointer(w: Int, h: Int) {
        pointerOffset = notePointerHelper.notePointerOffsetLeft(w.toFloat(), note)
        pointerWidth = notePointerHelper.computeNotePointerWidth(w.toFloat())
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        updatePointer(w, h)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawRect(
            pointerOffset,
            0f,
            pointerOffset + pointerWidth,
            height.toFloat(),
            pointerPaint
        )
    }
}