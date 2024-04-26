package com.willeypianotuning.toneanalyzer.ui.settings.weights.chart

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.util.TypedValue
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import com.github.mikephil.charting.charts.CombinedChart
import com.github.mikephil.charting.charts.ScatterChart
import com.github.mikephil.charting.components.Description
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.LegendEntry
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.CombinedData
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.data.ScatterData
import com.github.mikephil.charting.data.ScatterDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet
import com.github.mikephil.charting.interfaces.datasets.IScatterDataSet
import com.github.mikephil.charting.renderer.XAxisRenderer
import com.github.mikephil.charting.renderer.scatter.IShapeRenderer
import com.github.mikephil.charting.utils.Transformer
import com.github.mikephil.charting.utils.ViewPortHandler
import com.willeypianotuning.toneanalyzer.AppSettings
import com.willeypianotuning.toneanalyzer.R
import com.willeypianotuning.toneanalyzer.audio.IntervalWidth
import com.willeypianotuning.toneanalyzer.audio.note_names.NoteNames
import com.willeypianotuning.toneanalyzer.extensions.dpToPx
import timber.log.Timber

class TuningCurveChartManager(viewGroup: ViewGroup, private val appSettings: AppSettings) {
    companion object {
        private val OCTAVE_DARK = Color.argb(255, 30, 30, 255)
        private val FIFTH_DARK = Color.argb(255, 230, 25, 25)
        private val FOURTH_DARK = Color.argb(255, 60, 180, 75)
        private val TWELFTH_DARK = Color.argb(255, 85, 85, 85)
        private val DOUBLE_OCTAVE_DARK = Color.argb(255, 237, 125, 49)
        private val TRIPLE_OCTAVE_DARK = Color.argb(255, 240, 210, 0)
        private val NINETEENTH_DARK = Color.argb(255, 199, 109, 255)
    }

    private val originalTuningCurve = arrayListOf<Entry>()
    private val newTuningCurve = arrayListOf<Entry>()
    private val frequencies = arrayListOf<Entry>()
    private val widths = arrayListOf<LineSpecification>()

    var intervalWidth: IntervalWidth? = null
        set(value) {
            field = value
            updateIntervalWidths()
        }

    private var drawOctaves: Boolean = false
    private var drawDoubleOctaves: Boolean = false
    private var drawTripleOctaves: Boolean = false
    private var drawFifth: Boolean = false
    private var drawFourth: Boolean = false
    private var drawTwelfth: Boolean = false
    private var drawNineteenth: Boolean = false

    private var defaultCurveColor: Int = Color.argb(255, 192, 128, 0)
    private var currentCurveColor: Int = Color.argb(255, 0, 0, 128)

    private val context = viewGroup.context
    private val chartView: CombinedChart

    private val lineWidthDp: Float
        get() {
            val outValue = TypedValue()
            context.resources.getValue(R.dimen.graph_line_width, outValue, true)
            return outValue.float
        }

    private val noteNamingConvention by lazy {
        NoteNames.getNamingConvention(viewGroup.context, appSettings.noteNames)
    }

    var originalTuningCurveDashed: Boolean = false
        set(value) {
            if (field != value) {
                field = value
                repaint()
            }
        }

    init {
        chartView = CombinedChart(viewGroup.context).apply {
            legend.isEnabled = true
            legend.setCustom(listOf(
                LegendEntry(
                    context.getString(R.string.tuning_style_chart_current),
                    Legend.LegendForm.LINE,
                    2 * lineWidthDp.dpToPx(context.resources),
                    lineWidthDp,
                    null,
                    currentCurveColor,
                ),
                LegendEntry(
                    context.getString(R.string.tuning_style_chart_default),
                    Legend.LegendForm.LINE,
                    2 *lineWidthDp.dpToPx(context.resources),
                     lineWidthDp,
                    null,
                    defaultCurveColor,
                ),
            ))
            legend.horizontalAlignment = Legend.LegendHorizontalAlignment.CENTER
            description = Description().apply {
                text = ""
                isEnabled = false
            }
            isDragEnabled = true
            isDragDecelerationEnabled = false
            setTouchEnabled(true)
            setScaleEnabled(true)
            setDrawGridBackground(true)

            axisLeft.isEnabled = true
            axisLeft.setDrawZeroLine(true)
            axisLeft.zeroLineWidth = lineWidthDp * 0.8F
            axisLeft.zeroLineColor = Color.BLACK
            axisLeft.setDrawLabels(true)
            axisLeft.setLabelCount(10, false)
            axisLeft.axisMinimum = -10f
            axisLeft.axisMaximum = 10f

            axisRight.isEnabled = true
            axisRight.setDrawLabels(true)
            axisRight.setLabelCount(10, false)
            axisRight.axisMinimum = -50f
            axisRight.axisMaximum = 50f

            xAxis.isEnabled = true
            xAxis.position = XAxis.XAxisPosition.BOTTOM
            xAxis.valueFormatter = object : ValueFormatter() {
                override fun getFormattedValue(value: Float): String {
                    val noteIndex = value.toInt()
                    if (noteIndex < 1 || noteIndex > 89) {
                        return ""
                    }
                    return noteNamingConvention.pianoNoteName(noteIndex - 1).toString()
                }
            }

            setXAxisRenderer(
                CustomXAxisRenderer(
                    viewPortHandler,
                    xAxis,
                    getTransformer(YAxis.AxisDependency.LEFT)
                )
            )

            viewGroup.addView(this)
        }
    }

    private fun createLine(
        name: String,
        mainColor: Int,
        values: DoubleArray,
        strengths: DoubleArray
    ): LineSpecification? {
        val points = arrayListOf<Entry>()
        val colors = arrayListOf<Int>()
        val min = requireNotNull(strengths.minOrNull())
        val max = requireNotNull(strengths.maxOrNull())
        for (j in 0 until 88) {
            if (values[j] != 0.0) {
                points.add(Entry((j + 1).toFloat(), values[j].toFloat()))
                val alpha = (255 * (0.1 + 0.9 * ((strengths[j] - min) / (max - min)))).toInt()
                colors.add(
                    Color.argb(
                        alpha,
                        Color.red(mainColor),
                        Color.green(mainColor),
                        Color.blue(mainColor)
                    )
                )
            }
        }
        if (points.size < 2) {
            return null
        }
        val maxIdx = strengths.indexOfFirst { it == max }
        val labelPosition = Entry((maxIdx + 1).toFloat(), values[maxIdx].toFloat())
        return LineSpecification(name, labelPosition, mainColor, colors, points)
    }

    fun drawIntervalWidth(
        octave: Boolean = false,
        twelfth: Boolean = false,
        fifth: Boolean = false,
        fourth: Boolean = false,
        doubleOctave: Boolean = false,
        tripleOctave: Boolean = false,
        nineteenth: Boolean = false
    ) {
        drawOctaves = octave
        drawDoubleOctaves = doubleOctave
        drawTripleOctaves = tripleOctave
        drawFifth = fifth
        drawFourth = fourth
        drawTwelfth = twelfth
        drawNineteenth = nineteenth
        val showLabels =
            octave || doubleOctave || tripleOctave || fifth || fourth || twelfth || nineteenth
        chartView.axisLeft.textColor = when {
            showLabels -> Color.BLACK
            else -> ContextCompat.getColor(context, R.color.tuning_chart_bg)
        }
        updateIntervalWidths()
    }

    private fun updateIntervalWidths() {
        widths.clear()
        if (intervalWidth == null) {
            repaint()
            return
        }
        val intervalWidth = this.intervalWidth!!

        if (drawOctaves) {
            createLine(
                "2:1",
                OCTAVE_DARK,
                intervalWidth.octave.beatrate[0],
                intervalWidth.octave.strength[0]
            )?.let { widths.add(it) }
            createLine(
                "4:2",
                OCTAVE_DARK,
                intervalWidth.octave.beatrate[1],
                intervalWidth.octave.strength[1]
            )?.let { widths.add(it) }
            createLine(
                "6:3",
                OCTAVE_DARK,
                intervalWidth.octave.beatrate[2],
                intervalWidth.octave.strength[2]
            )?.let { widths.add(it) }
            createLine(
                "8:4",
                OCTAVE_DARK,
                intervalWidth.octave.beatrate[3],
                intervalWidth.octave.strength[3]
            )?.let { widths.add(it) }
            createLine(
                "10:5",
                OCTAVE_DARK,
                intervalWidth.octave.beatrate[4],
                intervalWidth.octave.strength[4]
            )?.let { widths.add(it) }
        }

        if (drawTwelfth) {
            createLine(
                "3:1",
                TWELFTH_DARK,
                intervalWidth.twelfth.beatrate[0],
                intervalWidth.twelfth.strength[0]
            )?.let { widths.add(it) }
            createLine(
                "6:2",
                TWELFTH_DARK,
                intervalWidth.twelfth.beatrate[1],
                intervalWidth.twelfth.strength[1]
            )?.let { widths.add(it) }
            createLine(
                "9:3",
                TWELFTH_DARK,
                intervalWidth.twelfth.beatrate[2],
                intervalWidth.twelfth.strength[2]
            )?.let { widths.add(it) }
        }

        if (drawDoubleOctaves) {
            createLine(
                "4:1",
                DOUBLE_OCTAVE_DARK,
                intervalWidth.doubleOctave.beatrate[0],
                intervalWidth.doubleOctave.strength[0]
            )?.let { widths.add(it) }
            createLine(
                "8:2",
                DOUBLE_OCTAVE_DARK,
                intervalWidth.doubleOctave.beatrate[1],
                intervalWidth.doubleOctave.strength[1]
            )?.let { widths.add(it) }
        }

        if (drawNineteenth) {
            createLine(
                "6:1",
                NINETEENTH_DARK,
                intervalWidth.nineteenth.beatrate[0],
                intervalWidth.nineteenth.strength[0]
            )?.let { widths.add(it) }
        }

        if (drawTripleOctaves) {
            createLine(
                "8:1",
                TRIPLE_OCTAVE_DARK,
                intervalWidth.tripleOctave.beatrate[0],
                intervalWidth.tripleOctave.strength[0]
            )?.let { widths.add(it) }
        }

        if (drawFifth) {
            createLine(
                "3:2",
                FIFTH_DARK,
                intervalWidth.fifth.beatrate[0],
                intervalWidth.fifth.strength[0]
            )?.let { widths.add(it) }
            createLine(
                "6:4",
                FIFTH_DARK,
                intervalWidth.fifth.beatrate[1],
                intervalWidth.fifth.strength[1]
            )?.let { widths.add(it) }
        }

        if (drawFourth) {
            createLine(
                "4:3",
                FOURTH_DARK,
                intervalWidth.fourth.beatrate[0],
                intervalWidth.fourth.strength[0]
            )?.let { widths.add(it) }
            createLine(
                "8:6",
                FOURTH_DARK,
                intervalWidth.fourth.beatrate[1],
                intervalWidth.fourth.strength[0]
            )?.let { widths.add(it) }
        }

        fixLabelPositionsToAvoidOverlaps()

        Timber.d("Prepared %d lines: %s", widths.size, widths.joinToString(",") { it.name })

        repaint()
    }

    private fun causesOverlap(existing: List<Entry>, newPosition: Entry): Boolean {
        val xExpand = 5f
        val yExpand = 2f
        return existing.isNotEmpty() && existing.any {
            it.x < newPosition.x + xExpand
                    && it.x > newPosition.x - xExpand
                    && it.y < newPosition.y + yExpand
                    && it.y > newPosition.y - yExpand
        }
    }

    private fun validPosition(existing: List<Entry>, newPosition: Entry): Boolean {
        // label should not be out of the x range
        if (newPosition.x < 1 || newPosition.x > 87) {
            return false
        }

        // label should not be out of the y range
        if (newPosition.y < -9 || newPosition.y > 9) {
            return false
        }

        return !causesOverlap(existing, newPosition)
    }

    private fun fixLabelPositionsToAvoidOverlaps() {
        val usedPositions = arrayListOf<Entry>()
        for (line in widths) {
            if (validPosition(usedPositions, line.labelPosition)) {
                usedPositions.add(line.labelPosition)
                continue
            }

            val pointIndex = line.points.indexOfFirst { it.x == line.labelPosition.x }
            var distance = 1
            while (pointIndex - distance > 0 || pointIndex + distance < 89) {
                val leftPosition = pointIndex - distance
                val rightPosition = pointIndex + distance
                if (leftPosition >= 0) {
                    if (validPosition(usedPositions, line.points[leftPosition])) {
                        line.labelPosition = line.points[leftPosition]
                        break
                    }
                }
                if (rightPosition < line.points.size) {
                    if (validPosition(usedPositions, line.points[rightPosition])) {
                        line.labelPosition = line.points[rightPosition]
                        break
                    }
                }
                distance++
            }

            usedPositions.add(line.labelPosition)
        }
    }

    fun setOriginalTuningCurve(delta: DoubleArray) {
        originalTuningCurve.clear()
        for (i in delta.indices) {
            if (delta[i] < -200 || delta[i] > 200)
                delta[i] = 0.0
            originalTuningCurve.add(Entry((i + 1).toFloat(), delta[i].toFloat()))
        }
        repaint()
    }

    fun setNewTuningCurve(delta: DoubleArray) {
        newTuningCurve.clear()
        for (i in delta.indices) {
            if (delta[i] < -200 || delta[i] > 200)
                delta[i] = 0.0
            newTuningCurve.add(Entry((i + 1).toFloat(), delta[i].toFloat()))
        }
        repaint()
    }

    fun setFrequencies(fx: DoubleArray, delta: DoubleArray) {
        frequencies.clear()
        for (i in fx.indices) {
            if (fx[i] > -200 && fx[i] < 200 && fx[i] != 0.0) {
                frequencies.add(Entry((i + 1).toFloat(), (fx[i] + delta[i]).toFloat()))
            }
        }
        repaint()
    }

    private fun repaint() {
        val originalTuningCurveLineDataSet =
            LineDataSet(originalTuningCurve, context.getString(R.string.tuning_style_chart_default)).apply {
                form = Legend.LegendForm.LINE
                lineWidth = lineWidthDp
                isHighlightEnabled = false
                axisDependency = YAxis.AxisDependency.RIGHT
                setDrawIcons(false)
                setDrawCircles(false)
                setDrawValues(false)
                if (originalTuningCurveDashed) {
                    val dashColor = defaultCurveColor
                    val spaceColor = Color.argb(0, 192, 128, 0)
                    colors =
                        Array(originalTuningCurve.size) { i -> if (i % 2 == 0) dashColor else spaceColor }.toList()
                } else {
                    color = defaultCurveColor
                }
            }
        val newTuningCurveLineDataSet = LineDataSet(newTuningCurve, context.getString(R.string.tuning_style_chart_current)).apply {
            form = Legend.LegendForm.LINE
            lineWidth = lineWidthDp
            isHighlightEnabled = false
            color = currentCurveColor
            axisDependency = YAxis.AxisDependency.RIGHT
            setDrawIcons(false)
            setDrawCircles(false)
            setDrawValues(false)
        }
        var lineDatasets = mutableListOf<ILineDataSet>(
            originalTuningCurveLineDataSet,
            newTuningCurveLineDataSet
        )
        if (originalTuningCurveDashed) {
            lineDatasets = lineDatasets.asReversed()
        }
        for (line in widths) {
            val dataset = LineDataSet(line.points, null).apply {
                form = Legend.LegendForm.NONE
                lineWidth = lineWidthDp
                isHighlightEnabled = false
                colors = line.colors
                axisDependency = YAxis.AxisDependency.LEFT
                setDrawIcons(false)
                setDrawCircles(false)
                setDrawValues(false)
            }
            lineDatasets.add(dataset)
        }
        val chartLineData = LineData(lineDatasets)

        val frequenciesLineDataSet = ScatterDataSet(frequencies, null).apply {
            form = Legend.LegendForm.NONE
            color = Color.BLUE
            isHighlightEnabled = false
            setScatterShape(ScatterChart.ScatterShape.SQUARE)
            axisDependency = YAxis.AxisDependency.RIGHT
            setDrawValues(false)
            setDrawIcons(false)
            scatterShapeSize = 2 * originalTuningCurveLineDataSet.lineWidth
        }
        val scatterDatasets = arrayListOf<IScatterDataSet>(
            frequenciesLineDataSet
        )

        for (line in widths) {
            val labelDataSet = ScatterDataSet(arrayListOf(line.labelPosition), null).apply {
                form = Legend.LegendForm.NONE
                color = Color.WHITE
                isHighlightEnabled = false
                axisDependency = YAxis.AxisDependency.LEFT
                shapeRenderer =
                    WidthLineLabelRenderer(chartView.context, line.name, line.primaryColor)
            }
            scatterDatasets.add(labelDataSet)
        }
        if (widths.isNotEmpty()) {
            scatterDatasets.add(
                ScatterDataSet(
                    arrayListOf(
                        Entry(2f, 9f, context.getString(R.string.interval_weights_wide_intervals)),
                    ),
                    null
                ).apply {
                    form = Legend.LegendForm.NONE
                    isHighlightEnabled = false
                    axisDependency = YAxis.AxisDependency.LEFT
                    shapeRenderer = IntervalsLabelRenderer(
                        chartView.context,
                        textSizeDp = 6 * lineWidthDp
                    )
                }
            )
            scatterDatasets.add(
                ScatterDataSet(
                    arrayListOf(
                        Entry(2f, -9f, context.getString(R.string.interval_weights_narrow_intervals)),
                    ),
                    null
                ).apply {
                    form = Legend.LegendForm.NONE
                    isHighlightEnabled = false
                    axisDependency = YAxis.AxisDependency.LEFT
                    shapeRenderer = IntervalsLabelRenderer(
                        chartView.context,
                        textSizeDp = 6 * lineWidthDp
                    )
                }
            )
        }
        val chartScatterData = ScatterData(scatterDatasets)

        chartView.data = CombinedData().apply {
            setData(chartLineData)
            setData(chartScatterData)
        }
        chartView.invalidate()
    }

    private class CustomXAxisRenderer(
        viewPortHandler: ViewPortHandler,
        xAxis: XAxis,
        trans: Transformer
    ) : XAxisRenderer(viewPortHandler, xAxis, trans) {
        override fun computeAxisValues(min: Float, max: Float) {
            val entries = if (max.toInt() - min.toInt() > 10) {
                floatArrayOf(
                    1f, 13f, 25f, 37f, 49f, 61f, 73f, 85f
                )
            } else {
                FloatArray(max.toInt() - min.toInt()) { i -> min + i }
            }
            mXAxis.mEntries = entries
            mXAxis.mCenteredEntries = entries
            mAxis.mEntryCount = entries.count()
            computeSize()
        }
    }

    private inner class WidthLineLabelRenderer(
        context: Context,
        private val label: String,
        private val textColor: Int
    ) : IShapeRenderer {
        private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = (8 * lineWidthDp).dpToPx(context.resources)
            color = textColor
        }
        private val padding = lineWidthDp.dpToPx(context.resources)
        private val textBounds = Rect()

        override fun renderShape(
            c: Canvas,
            dataSet: IScatterDataSet,
            viewPortHandler: ViewPortHandler,
            posX: Float,
            posY: Float,
            renderPaint: Paint
        ) {
            renderPaint.style = Paint.Style.FILL

            textPaint.getTextBounds(label, 0, label.length, textBounds)
            val rectWidth = textBounds.width() + 2 * padding
            val rectHeight = textBounds.height() + 2 * padding
            val halfWidth = rectWidth / 2
            val halfHeight = rectHeight / 2

            c.drawRect(
                posX - halfWidth,
                posY - halfHeight,
                posX + halfWidth,
                posY + halfHeight,
                renderPaint
            )

            c.drawText(
                label,
                posX - textBounds.width() / 2,
                posY + textBounds.height() / 2,
                textPaint
            )
        }

    }

    data class LineSpecification(
        val name: String,
        var labelPosition: Entry,
        val primaryColor: Int,
        val colors: List<Int>,
        val points: List<Entry>
    )
}
