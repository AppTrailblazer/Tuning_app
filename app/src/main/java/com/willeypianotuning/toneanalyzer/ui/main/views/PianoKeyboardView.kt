package com.willeypianotuning.toneanalyzer.ui.main.views

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import com.willeypianotuning.toneanalyzer.R
import timber.log.Timber

fun interface DisabledKeyProvider {
    fun isKeyDisabled(key: Int): Boolean
}

class PianoKeyboardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0
) : View(context, attrs, defStyleAttr, defStyleRes) {
    companion object {
        private const val KEY_RADIUS_PROPORTION_TO_KEY_WIDTH = 0.14f
        private const val HEIGHT_PROPORTION_BLACK_KEY_TO_WHITE_KEY = 0.6f
    }

    private val whiteKeys = arrayListOf<PianoKey>()
    private val blackKeys = arrayListOf<PianoKey>()

    private val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val whiteKeyPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val whitePressedKeyPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val whiteDisabledKeyPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val blackKeyPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val blackPressedKeyPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val blackDisabledKeyPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val selectedKeyPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    private val extendBlackKeysTouchAreaOverWhiteKeys = true

    private val notePointerHelper = NotePointerHelper()
    private var bitmapCache: Bitmap? = null

    var selectedKeyColor: Int
        get() = selectedKeyPaint.color
        set(value) {
            selectedKeyPaint.color = value
            invalidate()
        }

    var selectedKey: Int? = null
        set(value) {
            if (field != value) {
                field = value
                invalidateCache()
                invalidate()
            }
        }
    var onKeyPressedListener: PianoKeyClickListener? = null
    var disabledKeysProvider: DisabledKeyProvider? = null
        set(value) {
            field = value
            invalidateCache()
            invalidate()
        }

    private fun init(attrs: AttributeSet?) {
        val ta = context.obtainStyledAttributes(attrs, R.styleable.PianoKeyboardView)
        val key = ta.getInteger(R.styleable.PianoKeyboardView_pv_currentNote, -1)
        if (key > 0) {
            selectedKey = key
        }
        val borderWidth = ta.getDimensionPixelSize(R.styleable.PianoKeyboardView_pv_borderWidth, -1)
        if (borderWidth != -1) {
            notePointerHelper.customBorderWidth = borderWidth.toFloat()
        }
        ta.recycle()
    }

    override fun onFinishInflate() {
        super.onFinishInflate()
        backgroundPaint.color = Color.BLACK
        whiteKeyPaint.color = Color.WHITE
        whitePressedKeyPaint.color = Color.LTGRAY
        whiteDisabledKeyPaint.color = 0xFFCCCCCC.toInt()
        blackKeyPaint.color = Color.BLACK
        blackPressedKeyPaint.color = Color.DKGRAY
        blackDisabledKeyPaint.color = 0xFF555555.toInt()
        selectedKeyPaint.color = Color.RED
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val action = event.action
        val isDownAction = action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_MOVE

        val x = event.getX(0)
        val y = event.getY(0)

        val key = blackKeys.firstOrNull {
            x >= it.left && x <= it.right && (extendBlackKeysTouchAreaOverWhiteKeys || (y >= it.top && y <= it.bottom))
        }
            ?: whiteKeys.firstOrNull { x >= it.left && x <= it.right && y >= it.top && y <= it.bottom }

        whiteKeys.forEach { it.pressed = false }
        blackKeys.forEach { it.pressed = false }

        if (key != null) {
            key.pressed = isDownAction
            if (isDownAction) {
                onKeyPressedListener?.onKeyClicked(key.index)
            }
            invalidateCache()
            invalidate()

            return true
        }
        return super.onTouchEvent(event)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)

        val localBorder = notePointerHelper.borderWidth(w.toFloat())
        val whiteKeyHeight = h.toFloat() - localBorder
        val blackKeyHeight = HEIGHT_PROPORTION_BLACK_KEY_TO_WHITE_KEY * whiteKeyHeight

        whiteKeys.clear()
        blackKeys.clear()
        var keyIndex = 1
        for (i in 0 until NotePointerHelper.WHITE_KEYS) {
            if (i != 0 && (i % 7 == 1 || i % 7 == 3 || i % 7 == 4 || i % 7 == 6 || i % 7 == 0)) {
                val position = notePointerHelper.noteOffset(w.toFloat(), keyIndex)
                blackKeys.add(
                    PianoKey(
                        keyIndex++,
                        position.x,
                        0f,
                        position.y,
                        blackKeyHeight,
                        false
                    )
                )
            }

            val position = notePointerHelper.noteOffset(w.toFloat(), keyIndex)
            whiteKeys.add(PianoKey(keyIndex++, position.x, 0f, position.y, whiteKeyHeight, true))
        }

        invalidateCache()
    }

    fun invalidateCache() {
        val currentWidth = width
        val currentHeight = height
        if (currentWidth <= 0 || currentHeight <= 0) {
            Timber.w(
                "View width and height should be larger than 0. Got %d %d",
                currentWidth,
                currentHeight
            )
            return
        }

        var cache = bitmapCache
        if (cache == null || cache.width != currentWidth || cache.height != currentHeight || cache.isRecycled) {
            cache?.recycle()
            cache = Bitmap.createBitmap(currentWidth, currentHeight, Bitmap.Config.ARGB_8888)
        }
        cache ?: return

        if (cache.isRecycled) {
            Timber.w("View cache is recycled")
            return
        }

        drawOnCanvas(Canvas(cache))
        bitmapCache = cache
    }

    private fun drawOnCanvas(canvas: Canvas) {
        val borderRadius = notePointerHelper.borderWidth(width.toFloat())
        canvas.drawRoundedRect(
            0f,
            0f,
            width.toFloat(),
            height.toFloat(),
            borderRadius,
            backgroundPaint
        )

        for (key in whiteKeys) {
            val paint = providePaint(
                selectedKey != null && key.index == selectedKey,
                key.pressed,
                disabledKeysProvider?.isKeyDisabled(key.index)
                    ?: false,
                key.isWhile
            )
            val keyRadius = (key.right - key.left) * KEY_RADIUS_PROPORTION_TO_KEY_WIDTH
            canvas.drawRoundedRect(key.left, key.top, key.right, key.bottom, keyRadius, paint)
        }

        for (key in blackKeys) {
            val paint = providePaint(
                selectedKey != null && key.index == selectedKey,
                key.pressed,
                disabledKeysProvider?.isKeyDisabled(key.index)
                    ?: false,
                key.isWhile
            )
            val keyRadius = (key.right - key.left) * KEY_RADIUS_PROPORTION_TO_KEY_WIDTH
            canvas.drawRoundedRect(key.left, key.top, key.right, key.bottom, keyRadius, paint)
        }
    }

    private fun providePaint(
        selected: Boolean,
        pressed: Boolean,
        disabled: Boolean,
        white: Boolean
    ): Paint {
        return if (selected) {
            selectedKeyPaint
        } else if (disabled) {
            if (white) {
                whiteDisabledKeyPaint
            } else {
                blackDisabledKeyPaint
            }
        } else if (pressed) {
            if (white) {
                whitePressedKeyPaint
            } else {
                blackPressedKeyPaint
            }
        } else if (white) {
            whiteKeyPaint
        } else {
            blackKeyPaint
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val cache = bitmapCache
        if (cache == null || cache.isRecycled) {
            return
        }
        canvas.drawBitmap(cache, 0F, 0F, null)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        bitmapCache?.recycle()
    }

    private var rectF = RectF()

    private fun Canvas.drawRoundedRect(
        left: Float,
        top: Float,
        right: Float,
        bottom: Float,
        radius: Float,
        paint: Paint
    ) {
        this.drawRect(left, top, right, bottom - radius, paint)
        this.drawRect(left + radius, bottom - radius, right - radius, bottom, paint)
        rectF.left = left
        rectF.top = bottom - 2 * radius
        rectF.right = left + 2 * radius
        rectF.bottom = bottom
        this.drawArc(rectF, 90F, 180F, true, paint)
        rectF.left = right - 2 * radius
        rectF.top = bottom - 2 * radius
        rectF.right = right
        rectF.bottom = bottom
        this.drawArc(rectF, 0F, 90F, true, paint)
    }

    fun interface PianoKeyClickListener {
        fun onKeyClicked(index: Int)
    }

    private data class PianoKey(
        val index: Int,
        val left: Float,
        val top: Float,
        val right: Float,
        val bottom: Float,
        val isWhile: Boolean,
        var pressed: Boolean = false
    )

    init {
        init(attrs)
    }
}
