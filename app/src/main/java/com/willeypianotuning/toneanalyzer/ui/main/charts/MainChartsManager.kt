package com.willeypianotuning.toneanalyzer.ui.main.charts

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.LinearLayout
import androidx.viewpager.widget.PagerAdapter
import com.willeypianotuning.toneanalyzer.R
import com.willeypianotuning.toneanalyzer.ToneDetectorWrapper
import com.willeypianotuning.toneanalyzer.extensions.setDebounceOnClickListener
import com.willeypianotuning.toneanalyzer.ui.colors.ColorScheme
import com.willeypianotuning.toneanalyzer.ui.views.LockableViewPager
import timber.log.Timber

class MainChartsManager(chartsLayout: ViewGroup) {
    companion object {
        private const val EXTRA_CURRENT_CHART = "currentChart"
    }

    private val chartsViewPager: LockableViewPager = chartsLayout.findViewById(R.id.viewPager)

    private val fftLayout: LinearLayout = chartsLayout.findViewById(R.id.frequencyChart)
    private val deltaLayout: LinearLayout = chartsLayout.findViewById(R.id.deltaChart)
    private val bDataLayout: LinearLayout = chartsLayout.findViewById(R.id.bChart)

    private val charts = arrayOf(
        fftLayout,
        deltaLayout,
        bDataLayout
    )

    private val rightArrow: ImageButton = chartsLayout.findViewById(R.id.right_button)
    private val leftArrow: ImageButton = chartsLayout.findViewById(R.id.left_button)

    private val frequencySpectrumChartManager: FrequencyChartManager
    private val tuningCurveChartManager: DeltaChartManager
    private val inharmonicityChartManager: BDataChartManager

    private val arrowsHiderHandler = Handler(Looper.getMainLooper())
    private val hideArrowsRunnable = Runnable {
        leftArrow.alpha = 0.0f // hide buttons
        rightArrow.alpha = 0.0f // hide buttons
    }

    var chartsLocked: Boolean = false
        set(value) {
            field = value
            chartsViewPager.swipeLocked = value
            if (value) {
                arrowsHiderHandler.removeCallbacks(hideArrowsRunnable)
                hideArrowsRunnable.run()
            }
        }

    /**
     * an indicator to the chart appears on screen
     * 0 means FFT chart
     * 1 means bData chart
     * 2 means delta chart
     */
    val currentChart: Int get() = chartsViewPager.currentItem

    var onChartLongClickListener: OnChartLongClickListener? = null
    var onChartChangedListener: OnChartChangedListener? = null

    init {
        chartsViewPager.offscreenPageLimit = charts.size
        chartsViewPager.adapter = ChartsViewPagerAdapter()
        chartsViewPager.swipeLocked = chartsLocked

        for (chart in charts) {
            chart.setOnClickListener { showChartSwitchingButtons() }
            chart.setOnLongClickListener { onChartLongClicked(it) }
        }

        leftArrow.setDebounceOnClickListener { switchChart(-1) }
        rightArrow.setDebounceOnClickListener { switchChart(1) }

        frequencySpectrumChartManager = FrequencyChartManager(fftLayout)
        tuningCurveChartManager = DeltaChartManager(deltaLayout)
        inharmonicityChartManager = BDataChartManager(bDataLayout)
    }

    fun updateColors(colorScheme: ColorScheme) {
        frequencySpectrumChartManager.lineColor = colorScheme.spectrumLine
        tuningCurveChartManager.lineColor = colorScheme.tuningCurveLine
        tuningCurveChartManager.dotColor = colorScheme.tuningCurveDots
        inharmonicityChartManager.lineColor = colorScheme.inharmonicityLine
        inharmonicityChartManager.dotColor = colorScheme.inharmonicityDots
    }

    @JvmOverloads
    fun restoreState(savedState: Bundle?, defaultChart: Int = 1) {
        val currentChart = savedState?.getInt(EXTRA_CURRENT_CHART, defaultChart) ?: defaultChart
        setCurrentChart(currentChart, true)
    }

    fun saveState(outState: Bundle) {
        outState.putInt(EXTRA_CURRENT_CHART, currentChart)
    }

    private fun onChartLongClicked(v: View): Boolean {
        onChartLongClickListener?.onChartLongClick(charts.indexOf(v))
        return true
    }

    fun setCurrentChart(chartIndex: Int, smoothScroll: Boolean) {
        if (currentChart == chartIndex) {
            return
        }
        Timber.d("Changing current chart to $chartIndex")
        chartsViewPager.setCurrentItem(chartIndex, smoothScroll)
    }

    fun setCurrentNote(currentNote: Int) {
        tuningCurveChartManager.selectedNote = currentNote
    }

    fun updateAxis(pitchRaiseMode: Int) {
        tuningCurveChartManager.updateAxis(pitchRaiseMode)
    }

    fun updateCharts(toneAnalyzer: ToneDetectorWrapper) {
        when (chartsViewPager.currentItem) {
            0 -> frequencySpectrumChartManager.update(toneAnalyzer.fftResArray)
            1 -> {
                val pitchRaiseKeys = toneAnalyzer.pitchRaiseOptions?.raiseKeys ?: BooleanArray(88)
                tuningCurveChartManager.pitchRaiseDelta = toneAnalyzer.pitchRaiseData.overpullTarget
                tuningCurveChartManager.update(
                    toneAnalyzer.delta,
                    toneAnalyzer.fx,
                    pitchRaiseKeys,
                    toneAnalyzer.pitchRaiseMode
                )
            }
            2 -> inharmonicityChartManager.update(
                toneAnalyzer.bx,
                toneAnalyzer.by,
                toneAnalyzer.bxfit
            )
        }
    }

    fun chartName(chartIndex: Int): String {
        val chartNameRes = when (chartIndex) {
            0 -> R.string.activity_main_chart_fft_spectrum
            1 -> R.string.activity_main_chart_tuning_curve
            2 -> R.string.activity_main_chart_inharmonicity
            else -> return ""
        }
        return chartsViewPager.context.getString(chartNameRes)
    }

    private fun showChartSwitchingButtons() {
        if (chartsLocked) {
            return
        }

        leftArrow.alpha = 1.0f // show arrows
        rightArrow.alpha = 1.0f // show arrows
        arrowsHiderHandler.removeCallbacks(hideArrowsRunnable)
        arrowsHiderHandler.postDelayed(
            hideArrowsRunnable,
            5000
        ) // set the handler action to hide the arrows after 5 seconds
    }

    private fun switchChart(change: Int) {
        if (chartsLocked) {
            return
        }

        showChartSwitchingButtons()
        var newIndex: Int = chartsViewPager.currentItem + change
        if (newIndex > charts.lastIndex) {
            newIndex = 0
        }
        if (newIndex < 0) {
            newIndex = charts.lastIndex
        }
        chartsViewPager.setCurrentItem(newIndex, true)
        onChartChangedListener?.onChartChanged(newIndex)
    }

    fun onDestroy() {
        arrowsHiderHandler.removeCallbacks(hideArrowsRunnable)
    }

    private inner class ChartsViewPagerAdapter : PagerAdapter() {
        override fun instantiateItem(collection: ViewGroup, position: Int): Any {
            return charts[position]
        }

        override fun getCount(): Int {
            return charts.size
        }

        override fun isViewFromObject(arg0: View, arg1: Any): Boolean {
            return arg0 === arg1
        }
    }

    fun interface OnChartLongClickListener {
        fun onChartLongClick(chartIndex: Int)
    }

    fun interface OnChartChangedListener {
        fun onChartChanged(chartIndex: Int)
    }
}