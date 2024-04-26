package com.willeypianotuning.toneanalyzer.ui.views

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.DisplayMetrics
import android.view.View
import androidx.annotation.ColorInt
import com.willeypianotuning.toneanalyzer.R
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

class RingView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0
) : View(context, attrs, defStyleAttr, defStyleRes) {
    private lateinit var ringPaint: Paint
    private lateinit var summaryPaint: Paint
    private lateinit var summaryTextPaint: Paint

    private val labelBackgroundPosition = RectF()
    private val labelTextStart = Point()
    private val ringCircleStart = Point()
    private var strokeWidth = 0
    private var strokeColor = Color.argb(0, 0, 0, 0)
    private var numberOfDashes = 4
    private var radius = 50
    var ringAlpha = 0f
        set(alpha) {
            if (ringAlpha != alpha) {
                field = alpha
                val alphaInt = (255 * alpha).roundToInt()
                strokeColor = Color.argb(
                    alphaInt,
                    Color.red(strokeColor),
                    Color.green(strokeColor),
                    Color.blue(strokeColor)
                )
                ringPaint.color = strokeColor
            }
        }
    private var ringPhase = 0f
    var isShowRing = true
    private var showLabel = true
    private var labelStartAngle = 12f
    private var ringFillAngle = 60f
    private var labelTextSize = 0
    private var labelTextColor = Color.BLACK
    private var labelBackgroundColor = Color.WHITE
    var label = ""

    // we add 1dp on each side of the label stroke, so it would be a bit wider than rings
    private val summaryStrokeWidth: Int get() = (strokeWidth + 2 * pixelsPerDp()).toInt()

    private fun pixelsPerDp(): Float {
        return context.resources.displayMetrics.densityDpi * 1f / DisplayMetrics.DENSITY_DEFAULT
    }

    private fun init(attrs: AttributeSet?) {
        strokeWidth = (12 * pixelsPerDp()).toInt()
        labelTextSize = (9 * pixelsPerDp()).toInt()
        if (attrs == null) {
            return
        }
        val ta = context.obtainStyledAttributes(attrs, R.styleable.RingView)
        strokeColor = ta.getColor(R.styleable.RingView_strokeColor, strokeColor)
        strokeWidth = ta.getDimensionPixelSize(R.styleable.RingView_strokeWidth, strokeWidth)
        radius = ta.getDimensionPixelSize(R.styleable.RingView_circleRadius, radius)
        numberOfDashes = ta.getInt(R.styleable.RingView_numberOfDashes, numberOfDashes)
        labelTextColor = ta.getColor(R.styleable.RingView_labelTextColor, labelTextColor)
        labelBackgroundColor =
            ta.getColor(R.styleable.RingView_labelBackgroundColor, labelBackgroundColor)
        labelStartAngle = ta.getFloat(R.styleable.RingView_labelStartAngle, labelStartAngle)
        ringFillAngle = ta.getFloat(R.styleable.RingView_ringFillAngle, ringFillAngle)
        isShowRing = ta.getBoolean(R.styleable.RingView_showRing, isShowRing)
        showLabel = ta.getBoolean(R.styleable.RingView_showLabel, showLabel)
        labelTextSize = ta.getDimensionPixelSize(R.styleable.RingView_labelTextSize, labelTextSize)
        val resourceLabel = ta.getString(R.styleable.RingView_label)
        if (resourceLabel != null) {
            label = resourceLabel
        }
        ta.recycle()
    }

    fun setNumberOfDashes(numberOfDashes: Int) {
        if (this.numberOfDashes != numberOfDashes) {
            this.numberOfDashes = numberOfDashes
            updateRingPaint()
        }
    }

    fun setColor(@ColorInt color: Int) {
        val alphaInt = (255 * ringAlpha).roundToInt()
        strokeColor = Color.argb(alphaInt, Color.red(color), Color.green(color), Color.blue(color))
        ringPaint.color = strokeColor
        invalidate()
    }

    fun setLabelColor(@ColorInt color: Int) {
        labelBackgroundColor = color
        summaryPaint.color = color
        invalidate()
    }

    fun setLabelTextColor(@ColorInt color: Int) {
        labelTextColor = color
        summaryTextPaint.color = color
        invalidate()
    }

    /**
     * The purpose of this method is to reduce the number of recreations of the DashPathEffect
     */
    fun setNumberOfDashesAndRingPhase(numberOfDashes: Int, ringPhase: Float) {
        var changed = false
        if (this.numberOfDashes != numberOfDashes) {
            this.numberOfDashes = numberOfDashes
            changed = true
        }
        if (this.ringPhase != ringPhase) {
            this.ringPhase = ringPhase
            changed = true
        }
        if (changed) {
            updateRingPaint()
        }
    }

    fun getNumberOfDashes(): Int {
        return numberOfDashes
    }

    fun setRingPhase(ringPhase: Float) {
        if (this.ringPhase != ringPhase) {
            this.ringPhase = ringPhase
            updateRingPaint()
        }
    }

    fun getRingPhase(): Float {
        return ringPhase
    }

    override fun onFinishInflate() {
        super.onFinishInflate()
        ringPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        summaryPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        summaryTextPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        // This flag is important. For some reason, the HARDWARE layer doesn't work well
        // with the DashPathEffect during animations and causes some flickering.
        // I was able to reproduce the issue on Nexus 4 emulator running Android API 19.
        setLayerType(LAYER_TYPE_SOFTWARE, null)
        updatePaint()
    }

    private fun updateRingPaint() {
        val circleLength = 2 * Math.PI * radius
        var dashLength = 1f
        if (numberOfDashes > 0) {
            dashLength = (circleLength / (2 * numberOfDashes)).toFloat()
        }
        val dashPath = DashPathEffect(
            floatArrayOf(dashLength, dashLength),
            dashLength * (2 * (1 - ringPhase) + INITIAL_RING_PHASE)
        )
        ringPaint.pathEffect = dashPath
    }

    private fun updatePaint() {
        ringPaint.color = strokeColor
        ringPaint.strokeWidth = strokeWidth.toFloat()
        ringPaint.style = Paint.Style.STROKE
        updateRingPaint()
        summaryPaint.color = labelBackgroundColor
        summaryPaint.strokeWidth = summaryStrokeWidth.toFloat()
        summaryPaint.style = Paint.Style.STROKE
        summaryPaint.strokeJoin = Paint.Join.ROUND
        summaryPaint.strokeCap = Paint.Cap.ROUND
        summaryTextPaint.color = labelTextColor
        summaryTextPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        summaryTextPaint.textSize = labelTextSize.toFloat()
        summaryTextPaint.textAlign = Paint.Align.CENTER
        labelBackgroundPosition.top = strokeWidth / 2f
        labelBackgroundPosition.left = strokeWidth / 2f
        labelBackgroundPosition.bottom = 2 * radius + strokeWidth / 2f
        labelBackgroundPosition.right = 2 * radius + strokeWidth / 2f
        // we need to add a small extra angle to text, so it will be shifted on background
        val textAngleShift = 3.0
        labelTextStart.x =
            (radius + strokeWidth / 2f + (radius * cos(Math.toRadians(labelStartAngle + textAngleShift)))).toInt()
        labelTextStart.y =
            (radius + strokeWidth / 2f + (radius * sin(Math.toRadians(labelStartAngle + textAngleShift)))).toInt()
        ringCircleStart.x = (radius + strokeWidth / 2f).toInt()
        ringCircleStart.y = (radius + strokeWidth / 2f).toInt()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val desiredWidth = 2 * radius + summaryStrokeWidth
        val desiredHeight = 2 * radius + summaryStrokeWidth
        val widthMode = MeasureSpec.getMode(widthMeasureSpec)
        val widthSize = MeasureSpec.getSize(widthMeasureSpec)
        val heightMode = MeasureSpec.getMode(heightMeasureSpec)
        val heightSize = MeasureSpec.getSize(heightMeasureSpec)
        val width: Int = when (widthMode) {
            MeasureSpec.EXACTLY -> {
                widthSize
            }
            MeasureSpec.AT_MOST -> {
                minOf(desiredWidth, widthSize)
            }
            else -> {
                desiredWidth
            }
        }
        val height: Int = when (heightMode) {
            MeasureSpec.EXACTLY -> {
                heightSize
            }
            MeasureSpec.AT_MOST -> {
                minOf(desiredHeight, heightSize)
            }
            else -> {
                desiredHeight
            }
        }
        setMeasuredDimension(width, height)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (numberOfDashes > 0 && isShowRing) {
            canvas.drawCircle(
                ringCircleStart.x.toFloat(),
                ringCircleStart.y.toFloat(),
                radius.toFloat(),
                ringPaint
            )
        }
        if (showLabel) {
            canvas.drawArc(
                labelBackgroundPosition,
                labelStartAngle,
                ringFillAngle,
                false,
                summaryPaint
            )
            if (label.isNotEmpty()) {
                canvas.drawText(
                    label,
                    labelTextStart.x.toFloat(),
                    labelTextStart.y.toFloat(),
                    summaryTextPaint
                )
            }
        }
    }

    companion object {
        private const val INITIAL_RING_PHASE = 1f
    }

    init {
        init(attrs)
    }
}