package com.willeypianotuning.toneanalyzer.ui.main.charts

import android.app.Activity
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
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet
import com.github.mikephil.charting.interfaces.datasets.IScatterDataSet
import com.github.mikephil.charting.renderer.scatter.IShapeRenderer
import com.github.mikephil.charting.utils.ViewPortHandler
import com.willeypianotuning.toneanalyzer.R
import com.willeypianotuning.toneanalyzer.audio.enums.PitchRaiseMode
import com.willeypianotuning.toneanalyzer.extensions.dpToPx
import com.willeypianotuning.toneanalyzer.ui.colors.ColorScheme


class DeltaChartManager(viewGroup: ViewGroup) {
    companion object {
        private const val POINT_SIZE_COEF = 2.5f
    }

    private val chartView: CombinedChart = CombinedChart(viewGroup.context).apply {
        legend.isEnabled = false
        description = Description().apply {
            text = ""
            isEnabled = false
        }
        isDragEnabled = true
        isDragDecelerationEnabled = false
        setTouchEnabled(false)
        setScaleEnabled(false)
        setBorderWidth(0f)
        setViewPortOffsets(0f, 0f, 0f, 0f)
        setDrawGridBackground(false)

        axisLeft.isEnabled = true
        axisLeft.axisMinimum = -50.0f
        axisLeft.axisMaximum = 50.0f
        axisLeft.setDrawZeroLine(true)
        axisLeft.setDrawGridLines(false)
        axisLeft.setDrawAxisLine(false)

        axisRight.isEnabled = false

        xAxis.isEnabled = false
        xAxis.axisMinimum = 0f
        xAxis.axisMaximum = 89f

        setNoDataText("")

        val layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        viewGroup.addView(this, layoutParams)
    }

    var lineColor: Int = ColorScheme.Default.tuningCurveLine
    var dotColor: Int = ColorScheme.Default.tuningCurveDots
    private val lineWidthDp: Float
        get() {
            val outValue = TypedValue()
            chartView.resources.getValue(R.dimen.graph_line_width, outValue, true)
            return outValue.float
        }

    var pitchRaiseDelta: DoubleArray? = null

    var selectedNote = 0

    fun updateAxis(pitchRaiseMode: Int) {
        if (pitchRaiseMode == PitchRaiseMode.MEASUREMENT) {
            chartView.axisLeft.axisMinimum = -200.0f
            chartView.axisLeft.axisMaximum = 200.0f
        } else {
            chartView.axisLeft.axisMinimum = -50.0f
            chartView.axisLeft.axisMaximum = 50.0f
        }
        chartView.invalidate()
    }

    private fun repaint(
        pitchRaiseMode: Int,
        deltaSeries: List<Entry>,
        fxSeries: List<Entry>,
        pitchRaiseSeries: List<Entry>
    ) {
        ChartUtils.runOnUiThreadAndWait(chartView.context as Activity) {
            val lineDataSets = mutableListOf<ILineDataSet>()
            val scatterDataSets = mutableListOf<IScatterDataSet>()

            val deltaDataSet = LineDataSet(deltaSeries, "Delta").apply {
                color = lineColor
                lineWidth = lineWidthDp
                isHighlightEnabled = false
                axisDependency = YAxis.AxisDependency.LEFT
                setDrawIcons(false)
                setDrawCircles(false)
                setDrawValues(false)
            }
            lineDataSets.add(deltaDataSet)

            if (fxSeries.isNotEmpty()) {
                if (pitchRaiseMode == PitchRaiseMode.MEASUREMENT) {
                    val fxDataSet = LineDataSet(fxSeries, "Fx").apply {
                        color = dotColor
                        lineWidth = lineWidthDp
                        isHighlightEnabled = false
                        axisDependency = YAxis.AxisDependency.LEFT
                        setDrawIcons(false)
                        setDrawCircles(false)
                        setDrawValues(false)
                    }
                    lineDataSets.add(fxDataSet)
                } else {
                    val fxDataSet = ScatterDataSet(fxSeries, "Fx").apply {
                        color = dotColor
                        isHighlightEnabled = false
                        setScatterShape(ScatterChart.ScatterShape.SQUARE)
                        axisDependency = YAxis.AxisDependency.LEFT
                        setDrawValues(false)
                        setDrawIcons(false)
                        scatterShapeSize = POINT_SIZE_COEF * deltaDataSet.lineWidth
                    }
                    scatterDataSets.add(fxDataSet)
                }
            }

            if (pitchRaiseMode == PitchRaiseMode.TUNING && pitchRaiseSeries.isNotEmpty()) {
                val pitchRaiseDataSet = LineDataSet(pitchRaiseSeries, "Pitch Raise").apply {
                    color = Color.GREEN
                    lineWidth = lineWidthDp
                    isHighlightEnabled = false
                    axisDependency = YAxis.AxisDependency.LEFT
                    setDrawIcons(false)
                    setDrawCircles(false)
                    setDrawValues(false)
                }
                lineDataSets.add(pitchRaiseDataSet)
            }

            val chartLineData = LineData(lineDataSets)

            if (selectedNote != 0) {
                val deltaValue = deltaSeries.firstOrNull { it.x == selectedNote.toFloat() }

                if (deltaValue != null) {
                    val fxValue = fxSeries.firstOrNull { it.x == selectedNote.toFloat() }

                    val x = deltaValue.x
                    val y = maxOf(
                        minOf(deltaValue.y, chartView.axisLeft.mAxisMaximum),
                        chartView.axisLeft.axisMinimum
                    )

                    val noteInfoDataset = ScatterDataSet(listOf(Entry(x, y)), "Note Info").apply {
                        color = Color.WHITE
                        isHighlightEnabled = false
                        setDrawIcons(false)
                        setDrawValues(false)
                        axisDependency = YAxis.AxisDependency.LEFT
                        shapeRenderer = SelectedNoteLabelRenderer(
                            chartView.context,
                            deltaValue.y,
                            fxValue?.y,
                            Color.BLACK
                        )
                    }
                    scatterDataSets.add(noteInfoDataset)
                }
            }
            val chartScatterData = ScatterData(scatterDataSets)

            chartView.data = CombinedData().apply {
                setData(chartLineData)
                setData(chartScatterData)
            }
            chartView.invalidate()
        }
    }

    fun update(
        delta: DoubleArray,
        fx: DoubleArray,
        pitchRaiseKeys: BooleanArray,
        pitchRaiseMode: Int
    ) {
        val deltaSeries = arrayListOf<Entry>()
        val fxSeries = arrayListOf<Entry>()
        val pitchRaiseSeries = arrayListOf<Entry>()

        for (i in delta.indices) {
            if (delta[i] < -200 || delta[i] > 200) {
                delta[i] = 0.0
            }
            deltaSeries.add(Entry((i + 1).toFloat(), delta[i].toFloat()))
        }

        for (i in fx.indices) {
            if (fx[i] > -200 && fx[i] < 200 && fx[i] != 0.0 &&
                (pitchRaiseMode != PitchRaiseMode.MEASUREMENT || pitchRaiseKeys[i])
            ) {
                fxSeries.add(Entry((i + 1).toFloat(), (fx[i] + delta[i]).toFloat()))
            }
        }

        val pitchRaiseDelta = pitchRaiseDelta
        if (pitchRaiseMode == PitchRaiseMode.TUNING && pitchRaiseDelta != null) {
            for (i in pitchRaiseDelta.indices) {
                if (pitchRaiseDelta[i] < -200 || pitchRaiseDelta[i] > 200) {
                    pitchRaiseDelta[i] = 0.0
                }
                pitchRaiseSeries.add(Entry((i + 1).toFloat(), pitchRaiseDelta[i].toFloat()))
            }
        }
        repaint(pitchRaiseMode, deltaSeries, fxSeries, pitchRaiseSeries)
    }

    private inner class SelectedNoteLabelRenderer(
        context: Context,
        private val delta: Float,
        private val fx: Float?,
        private val textColor: Int
    ) : IShapeRenderer {
        private val paddingHorizontal = (lineWidthDp * 3f).dpToPx(context.resources)
        private val paddingVertical = (lineWidthDp * 1.5f).dpToPx(context.resources)
        private val lineWidth = lineWidthDp.dpToPx(context.resources)
        private val strokeWidth = POINT_SIZE_COEF * lineWidth
        private val iconWidth = 2 * strokeWidth

        private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = (8f * lineWidthDp).dpToPx(context.resources)
            color = textColor
        }
        private val lineShapePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            strokeWidth = lineWidth
            color = lineColor
        }
        private val pointShapePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            strokeWidth = 0f
            color = dotColor
        }

        private val deltaTextBounds = Rect()
        private val fxTextBounds = Rect()
        private val linePoints = FloatArray(12)

        override fun renderShape(
            c: Canvas,
            dataSet: IScatterDataSet,
            viewPortHandler: ViewPortHandler,
            posX: Float,
            posY: Float,
            renderPaint: Paint
        ) {
            renderPaint.style = Paint.Style.FILL
            val deltaText = String.format("%+.1f", delta)
            textPaint.getTextBounds(deltaText, 0, deltaText.length, deltaTextBounds)
            val fxText = if (fx != null) String.format("%+.1f", fx - delta) else ""
            textPaint.getTextBounds(fxText, 0, fxText.length, fxTextBounds)

            val rectWidth = maxOf(
                deltaTextBounds.width(),
                fxTextBounds.width()
            ) + 3 * paddingHorizontal + iconWidth
            val rectHeight =
                deltaTextBounds.height() + fxTextBounds.height() + (if (fxText.isNotEmpty()) 3 else 2) * paddingVertical

            val left =
                if (posX + rectWidth > 0.95 * c.width) posX - rectWidth - paddingHorizontal else posX + paddingHorizontal
            val top = if (posX > c.width / 3) c.height / 3f * 2 else c.height / 6f

            c.drawRect(
                left,
                top,
                left + rectWidth,
                top + rectHeight,
                renderPaint
            )

            var i = 0
            linePoints[i++] = left + paddingHorizontal
            linePoints[i++] = top + paddingVertical + (deltaTextBounds.height() * 3 / 4)
            linePoints[i++] = left + paddingHorizontal + (iconWidth / 4)
            linePoints[i++] = top + paddingVertical + (deltaTextBounds.height() / 2)
            linePoints[i++] = left + paddingHorizontal + (iconWidth / 4)
            linePoints[i++] = top + paddingVertical + (deltaTextBounds.height() / 2)
            linePoints[i++] = left + paddingHorizontal + (iconWidth * 3 / 4)
            linePoints[i++] = top + paddingVertical + (deltaTextBounds.height() / 2)
            linePoints[i++] = left + paddingHorizontal + (iconWidth * 3 / 4)
            linePoints[i++] = top + paddingVertical + (deltaTextBounds.height() / 2)
            linePoints[i++] = left + paddingHorizontal + iconWidth
            linePoints[i++] = top + paddingVertical + (deltaTextBounds.height() / 4)

            c.drawLines(linePoints, lineShapePaint)

            if (fxText.isNotEmpty()) {
                c.drawRect(
                    left + paddingHorizontal + ((iconWidth - strokeWidth) / 2),
                    top + rectHeight - paddingVertical - (fxTextBounds.height() / 2) - (strokeWidth / 2),
                    left + paddingHorizontal + ((iconWidth - strokeWidth) / 2) + strokeWidth,
                    top + rectHeight - paddingVertical - (fxTextBounds.height() / 2) + (strokeWidth / 2),
                    pointShapePaint
                )
            }

            c.drawText(
                deltaText,
                left + 2 * paddingHorizontal + iconWidth,
                top + paddingVertical + deltaTextBounds.height(),
                textPaint
            )
            if (fxText.isNotEmpty()) {
                c.drawText(
                    fxText,
                    left + 2 * paddingHorizontal + iconWidth,
                    top + rectHeight - paddingVertical,
                    textPaint
                )
            }
        }
    }

}