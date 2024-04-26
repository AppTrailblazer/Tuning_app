package com.willeypianotuning.toneanalyzer.ui.tuning.temperament

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.util.AttributeSet
import android.util.DisplayMetrics
import android.view.View
import com.willeypianotuning.toneanalyzer.R
import com.willeypianotuning.toneanalyzer.extensions.fraction
import com.willeypianotuning.toneanalyzer.extensions.round
import com.willeypianotuning.toneanalyzer.utils.decimal.AppInputDecimalFormat
import kotlin.math.abs
import kotlin.math.absoluteValue
import kotlin.math.cos
import kotlin.math.sin

class TemperamentChartView : View {

    private val circleInnerPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val circleBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val linePaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val noteTextPaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textHighPaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val commaTextPaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG)

    private var noteNameTextColor = Color.WHITE
    private var noteCircleColor = Color.DKGRAY
    private var lineColor = Color.LTGRAY
    private var offsetTextColor = Color.WHITE
    private var offsetHighTextColor = Color.RED
    private var lineWidth = 6
    private var drawInnerLines = true

    private var noteTextSize = 50
    private var offsetTextSize = 35

    private var noteCircleRadius = 50
    private val startAngle = -Math.PI / 2f

    // all of the data is stored as strings to avoid memory allocation during onDraw
    private val notes = arrayOf("C", "G", "D", "A", "E", "B", "F#", "C#", "G#", "D#", "A#", "F")

    private var fifths: Array<FormattedDouble>? = null
    private var fractions: Array<FormattedDouble>? = null
    private var thirds: Array<FormattedDouble>? = null
    private var comma: String? = null

    // this fields are used in onDraw method
    private val textBounds = Rect()
    private val xs = FloatArray(notes.size)
    private val ys = FloatArray(notes.size)

    constructor(context: Context) : super(context) {
        init(null)
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        init(attrs)
    }

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        init(attrs)
    }

    private fun init(attrs: AttributeSet?) {
        lineWidth = 2 * context.resources.displayMetrics.densityDpi / DisplayMetrics.DENSITY_DEFAULT
        if (attrs == null) {
            return
        }

        val ta = context.obtainStyledAttributes(attrs, R.styleable.TemperamentChartView)
        noteNameTextColor =
            ta.getColor(R.styleable.TemperamentChartView_noteNameTextColor, noteNameTextColor)
        noteCircleColor =
            ta.getColor(R.styleable.TemperamentChartView_noteCircleColor, noteCircleColor)
        lineColor = ta.getColor(R.styleable.TemperamentChartView_lineColor, lineColor)
        offsetTextColor =
            ta.getColor(R.styleable.TemperamentChartView_offsetTextColor, offsetTextColor)
        offsetHighTextColor =
            ta.getColor(R.styleable.TemperamentChartView_offsetHighTextColor, offsetHighTextColor)

        lineWidth = ta.getDimensionPixelSize(R.styleable.TemperamentChartView_lineWidth, lineWidth)

        noteTextSize =
            ta.getDimensionPixelSize(R.styleable.TemperamentChartView_noteTextSize, noteTextSize)
        offsetTextSize = ta.getDimensionPixelSize(
            R.styleable.TemperamentChartView_offsetTextSize,
            offsetTextSize
        )
        noteCircleRadius = ta.getDimensionPixelSize(
            R.styleable.TemperamentChartView_noteCircleRadius,
            noteCircleRadius
        )
        drawInnerLines =
            ta.getBoolean(R.styleable.TemperamentChartView_drawInnerLines, drawInnerLines)

        ta.getResourceId(R.styleable.TemperamentChartView_innerData, 0).takeUnless { it == 0 }
            ?.let { resourceId ->
                val values = resources.getStringArray(resourceId)
                this.thirds =
                    values.map { FormattedDouble(AppInputDecimalFormat.parseDouble(it)!!, it) }
                        .toTypedArray()
            }

        ta.getResourceId(R.styleable.TemperamentChartView_middleData, 0).takeUnless { it == 0 }
            ?.let { resourceId ->
                val values = resources.getStringArray(resourceId)
                this.fractions =
                    values.map { FormattedDouble(AppInputDecimalFormat.parseDouble(it)!!, it) }
                        .toTypedArray()
            }

        ta.getResourceId(R.styleable.TemperamentChartView_outerData, 0).takeUnless { it == 0 }
            ?.let { resourceId ->
                val values = resources.getStringArray(resourceId)
                this.fifths =
                    values.map { FormattedDouble(AppInputDecimalFormat.parseDouble(it)!!, it) }
                        .toTypedArray()
            }

        ta.recycle()
    }

    fun setFifths(data: DoubleArray?) {
        if (data == null) {
            fifths = null
            return
        }

        fifths = data.take(notes.size).map { FormattedDouble(it, "%.2f".format(it.round(2))) }
            .toTypedArray()
        invalidate()
    }

    fun setThirds(data: DoubleArray?) {
        if (data == null) {
            thirds = null
            return
        }

        thirds = data.take(notes.size).map { FormattedDouble(it, "%.1f".format(it.round(1))) }
            .toTypedArray()
        invalidate()
    }

    fun setFractions(data: DoubleArray?) {
        if (data == null) {
            fractions = null
            return
        }

        fractions = data.take(notes.size).map {
            val fraction = it.round(2)
            val isInt = fraction == fraction.toInt().toDouble()
            var fractionValue =
                if (isInt) "%d".format(fraction.toInt()) else "%.2f".format(fraction)

            if (abs(it) < 0.667 && fraction != 0.0) {
                val numDenom = abs(it).fraction(FRACTION_MAX_DENOMINATOR)
                fractionValue =
                    (if (fraction < 0) "-" else "") + "%d".format(numDenom.first) + "/" + "%d".format(
                        numDenom.second
                    )
            }
            return@map FormattedDouble(it, fractionValue)
        }.toTypedArray()
        invalidate()
    }

    fun setComma(comma: String) {
        this.comma = comma
        invalidate()
    }

    fun setDrawInnerLines(drawInnerLines: Boolean) {
        this.drawInnerLines = drawInnerLines
        invalidate()
    }

    override fun onFinishInflate() {
        super.onFinishInflate()

        circleInnerPaint.color = noteCircleColor

        commaTextPaint.textSize = 1.5f * noteTextSize
        commaTextPaint.color = offsetTextColor

        circleBorderPaint.color = lineColor
        circleBorderPaint.strokeWidth = lineWidth.toFloat()
        circleBorderPaint.style = Paint.Style.STROKE

        linePaint.color = lineColor
        linePaint.strokeWidth = lineWidth.toFloat()

        noteTextPaint.color = noteNameTextColor
        noteTextPaint.textSize = noteTextSize.toFloat()

        textPaint.color = offsetTextColor
        textPaint.textSize = offsetTextSize.toFloat()

        textHighPaint.color = offsetHighTextColor
        textHighPaint.textSize = offsetTextSize.toFloat()
    }

    private fun computeX(centerX: Int, position: Int, maxPosition: Int, radius: Float): Int {
        val angle: Double = if (position == 0) {
            startAngle
        } else {
            startAngle + position * (2 * Math.PI / maxPosition)
        }
        return (centerX + cos(angle) * radius).toInt()
    }

    private fun computeY(centerY: Int, position: Int, maxPosition: Int, radius: Float): Int {
        val angle: Double = if (position == 0) {
            startAngle
        } else {
            startAngle + position * (2 * Math.PI / maxPosition)
        }
        return (centerY + sin(angle) * radius).toInt()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)

        val width = measuredWidth
        val height = measuredHeight
        val squareLen = maxOf(width, height)

        super.onMeasure(
            MeasureSpec.makeMeasureSpec(squareLen, MeasureSpec.EXACTLY),
            MeasureSpec.makeMeasureSpec(squareLen, MeasureSpec.EXACTLY)
        )
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val centerX = width / 2
        val centerY = height / 2

        val outerTextRadius = minOf(centerX - offsetTextSize, centerX - noteCircleRadius).toFloat()
        val radius = outerTextRadius - offsetTextSize
        val middleTextRadius = radius - 1.7f * offsetTextSize
        val innerTextRadius = radius / 2.5f

        for (i in notes.indices) {
            xs[i] = computeX(centerX, i, notes.size, radius).toFloat()
            ys[i] = computeY(centerY, i, notes.size, radius).toFloat()
        }

        for (i in notes.indices) {
            canvas.drawLine(
                xs[i],
                ys[i],
                xs[(i + 1) % notes.size],
                ys[(i + 1) % notes.size],
                linePaint
            )
            if (drawInnerLines) {
                canvas.drawLine(
                    xs[i],
                    ys[i],
                    xs[(i + 4) % notes.size],
                    ys[(i + 4) % notes.size],
                    linePaint
                )
            }
        }

        for (i in notes.indices) {
            canvas.drawCircle(xs[i], ys[i], noteCircleRadius.toFloat(), circleInnerPaint)
            canvas.drawCircle(xs[i], ys[i], noteCircleRadius.toFloat(), circleBorderPaint)

            noteTextPaint.getTextBounds(notes[i], 0, notes[i].length, textBounds)
            canvas.drawText(
                notes[i],
                xs[i] - textBounds.width() / 2f,
                ys[i] + textBounds.height() / 2f,
                noteTextPaint
            )
        }

        comma?.let {
            commaTextPaint.getTextBounds(it, 0, it.length, textBounds)

            val x = centerX - textBounds.width() / 2f
            val y = centerY + textBounds.height() / 2f

            canvas.drawText(it, x, y, commaTextPaint)
        }

        fifths?.let {
            for (i in notes.indices) {
                val paint =
                    if (it[i].value.absoluteValue > HIGH_FIFTH_LIMIT) textHighPaint else textPaint
                paint.getTextBounds(it[i].stringValue, 0, it[i].stringValue.length, textBounds)

                var x = computeX(centerX, 2 * i + 1, 2 * notes.size, outerTextRadius)
                var y = computeY(centerY, 2 * i + 1, 2 * notes.size, outerTextRadius)

                x -= (textBounds.width() / 2f).toInt()
                y += (textBounds.height() / 2f).toInt()

                canvas.drawText(it[i].stringValue, x.toFloat(), y.toFloat(), paint)
            }
        }

        thirds?.let {
            for (i in notes.indices) {
                val paint =
                    if (it[i].value.absoluteValue > HIGH_THIRD_LIMIT) textHighPaint else textPaint
                paint.getTextBounds(it[i].stringValue, 0, it[i].stringValue.length, textBounds)

                var x = computeX(centerX, i, notes.size, innerTextRadius)
                var y = computeY(centerY, i, notes.size, innerTextRadius)

                x -= (textBounds.width() / 2f).toInt()
                y += (textBounds.height() / 2f).toInt()

                canvas.drawText(it[i].stringValue, x.toFloat(), y.toFloat(), paint)
            }
        }

        fractions?.let {
            for (i in notes.indices) {
                textPaint.getTextBounds(it[i].stringValue, 0, it[i].stringValue.length, textBounds)

                var x = computeX(centerX, 2 * i + 1, 2 * notes.size, middleTextRadius)
                var y = computeY(centerY, 2 * i + 1, 2 * notes.size, middleTextRadius)

                x -= (textBounds.width() / 2f).toInt()
                y += (textBounds.height() / 2f).toInt()

                canvas.drawText(it[i].stringValue, x.toFloat(), y.toFloat(), textPaint)
            }
        }
    }

    private data class FormattedDouble(val value: Double, val stringValue: String)

    companion object {
        private const val HIGH_FIFTH_LIMIT = 13.0
        private const val HIGH_THIRD_LIMIT = 31.0
        private const val FRACTION_MAX_DENOMINATOR = 88
    }

}