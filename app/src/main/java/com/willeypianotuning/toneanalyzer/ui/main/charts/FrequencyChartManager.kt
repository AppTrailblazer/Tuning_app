package com.willeypianotuning.toneanalyzer.ui.main.charts

import android.app.Activity
import android.util.TypedValue
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.Description
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.willeypianotuning.toneanalyzer.R
import com.willeypianotuning.toneanalyzer.ToneDetectorWrapper
import com.willeypianotuning.toneanalyzer.ui.colors.ColorScheme
import kotlin.math.log2

class FrequencyChartManager(viewGroup: ViewGroup) {
    private val chartView: LineChart
    private val freqRange: FloatArray

    var lineColor: Int = ColorScheme.Default.spectrumLine
    private val lineWidthDp: Float
        get() {
            val outValue = TypedValue()
            chartView.resources.getValue(R.dimen.graph_line_width, outValue, true)
            return outValue.float
        }

    init {
        val multiplier = ToneDetectorWrapper.SAMPLE_FREQ.toFloat() / ToneDetectorWrapper.FFT_SIZE
        freqRange = FloatArray(ToneDetectorWrapper.FFT_SIZE) { i -> log2(i * multiplier) }

        chartView = LineChart(viewGroup.context).apply {
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
            axisLeft.axisMinimum = 0f
            axisLeft.axisMaximum = 1f
            axisRight.isEnabled = false

            xAxis.isEnabled = false
            xAxis.axisMinimum = log2(26f)
            xAxis.axisMaximum = log2(4450f)

            setNoDataText("")

            val layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            viewGroup.addView(this, layoutParams)
        }
    }

    private fun repaint(frequencySeries: List<Entry>) {
        ChartUtils.runOnUiThreadAndWait(chartView.context as Activity) {
            val frequenciesDataSet = LineDataSet(frequencySeries, "Frequencies").apply {
                color = lineColor
                lineWidth = lineWidthDp
                isHighlightEnabled = false
                axisDependency = YAxis.AxisDependency.LEFT
                setDrawIcons(false)
                setDrawCircles(false)
                setDrawValues(false)
            }
            val chartLineData = LineData(frequenciesDataSet)
            chartView.data = chartLineData
            chartView.invalidate()
        }
    }

    fun update(fftRes: DoubleArray) {
        val frequencySeries = arrayListOf<Entry>()

        val min = requireNotNull(fftRes.minOrNull()).toFloat()
        val max = requireNotNull(fftRes.maxOrNull()).toFloat()
        val range = max - min
        for (i in fftRes.indices) {
            val valueNormalized = if (range != 0f)
                (fftRes[i].toFloat() - min) / (max - min)
            else
                fftRes[i].toFloat()
            frequencySeries.add(Entry(freqRange[i], valueNormalized))
        }

        repaint(frequencySeries)
    }
}