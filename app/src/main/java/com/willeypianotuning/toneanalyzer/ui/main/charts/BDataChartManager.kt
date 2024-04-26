package com.willeypianotuning.toneanalyzer.ui.main.charts

import android.app.Activity
import android.util.TypedValue
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import com.github.mikephil.charting.charts.CombinedChart
import com.github.mikephil.charting.charts.ScatterChart
import com.github.mikephil.charting.components.Description
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.*
import com.willeypianotuning.toneanalyzer.R
import com.willeypianotuning.toneanalyzer.ui.colors.ColorScheme
import kotlin.math.exp
import kotlin.math.ln

class BDataChartManager(viewGroup: ViewGroup) {
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

        axisLeft.isEnabled = false
        axisLeft.axisMinimum = ln(0.00005).toFloat()
        axisLeft.axisMaximum = ln(0.02).toFloat()

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

    var lineColor: Int = ColorScheme.Default.inharmonicityLine
    var dotColor: Int = ColorScheme.Default.inharmonicityDots
    private val lineWidthDp: Float
        get() {
            val outValue = TypedValue()
            chartView.resources.getValue(R.dimen.graph_line_width, outValue, true)
            return outValue.float
        }

    private fun repaint(bxFitSeries: List<Entry>, bDataSeries: List<Entry>) {
        ChartUtils.runOnUiThreadAndWait(chartView.context as Activity) {
            val bxFitDataSet = LineDataSet(bxFitSeries, "B").apply {
                color = lineColor
                lineWidth = lineWidthDp
                isHighlightEnabled = false
                axisDependency = YAxis.AxisDependency.LEFT
                setDrawIcons(false)
                setDrawCircles(false)
                setDrawValues(false)
            }
            val chartLineData = LineData(bxFitDataSet)

            val bDataSet = ScatterDataSet(bDataSeries, "BxFit").apply {
                color = dotColor
                isHighlightEnabled = false
                setScatterShape(ScatterChart.ScatterShape.SQUARE)
                axisDependency = YAxis.AxisDependency.LEFT
                setDrawValues(false)
                setDrawIcons(false)
                scatterShapeSize = 2 * bxFitDataSet.lineWidth
            }
            val chartScatterData = ScatterData(bDataSet)

            chartView.data = CombinedData().apply {
                setData(chartLineData)
                setData(chartScatterData)
            }
            chartView.invalidate()
        }
    }

    fun update(bx: IntArray, by: DoubleArray, bxFit: DoubleArray) {
        val bxFitSeries = arrayListOf<Entry>()
        val bDataSeries = arrayListOf<Entry>()

        for (i in 0 until minOf(bx.size, by.size)) {
            if (by[i] > 0.00005 && by[i] < 0.02) {
                bDataSeries.add(Entry(bx[i].toFloat(), ln(by[i]).toFloat()))
            }
        }

        for (i in 1..88) {
            val v1 = bxFit[0] * exp(bxFit[1] * i)
            val v2 = bxFit[2] * exp(bxFit[3] * (i - 88))
            val v3 = v1 + v2
            if (v3 > 0.00005 && v3 < 0.02) {
                bxFitSeries.add(Entry(i.toFloat(), ln(v3).toFloat()))
            }
        }

        repaint(bxFitSeries, bDataSeries)
    }

}