package com.willeypianotuning.toneanalyzer.spinners

import android.os.Handler
import android.os.Looper
import android.os.Message
import android.os.SystemClock
import com.willeypianotuning.toneanalyzer.ui.views.RingView
import timber.log.Timber

/**
 * Handler that updates the spinner views
 */
class CalibrationSpinnerHandler(
    private val spinners: Spinners,
    private val spinnerViews: Array<RingView>
) : Handler(Looper.getMainLooper()) {
    private var updateTime: Long = 0

    private val phase: FloatArray = FloatArray(spinnerViews.size) { Spinner.INACTIVE }
    private val label: Array<String> = Array(spinnerViews.size) { "" }
    private val divisions: IntArray = IntArray(spinnerViews.size)
    private val isEnabled: BooleanArray = BooleanArray(spinnerViews.size)

    private fun getSkippedFramesAndScheduleNextRun(): Int {
        val timeWhenTheExecutionShouldHappen = updateTime
        val now = SystemClock.uptimeMillis()
        val updateInterval = Spinners.SPINNER_UPDATE_INTERVAL
        // here we compute the delay between the time when the function was scheduled to run
        // and the time when it is actually running
        val executionDelay = now - timeWhenTheExecutionShouldHappen

        // if the execution delay is more than the update interval
        // then the UI thread was busy and wasn't able to execute the spinner update
        // which means we have to skip some frames so we show up to date data
        val skippedFrames = (executionDelay - 1) / updateInterval
        if (skippedFrames > 0) {
            Timber.tag("SpinnerHandler").v("Skipping %d phases", skippedFrames)
        }

        // now lets compute the time when the next execution should happen
        updateTime += updateInterval * skippedFrames
        updateTime += updateInterval.toLong()

        // here we schedule next execution
        this.sendEmptyMessageAtTime(REFRESH, updateTime)

        // returning the number of skipped frames
        return skippedFrames.toInt()
    }

    override fun handleMessage(msg: Message) {
        if (msg.what == CANCEL) {
            this.removeMessages(REFRESH)
            return
        }

        val skippedFrames = getSkippedFramesAndScheduleNextRun()
        spinners.fill(skippedFrames, phase, label, divisions, isEnabled)

        // update spinner rotation and label
        for (i in spinnerViews.indices) {
            var changed = false
            if (isEnabled[i]) {
                // update ring if the number of divisions changed
                if (divisions[i] != spinnerViews[i].getNumberOfDashes()) {
                    spinnerViews[i].setNumberOfDashes(divisions[i])
                    changed = true
                }
                if (phase[i] >= 0.0f && spinnerViews[i].getRingPhase() != phase[i]) {
                    spinnerViews[i].setRingPhase(phase[i])
                    changed = true
                }
            }
            val visible =
                isEnabled[i] && (phase[i] >= 0.0f || phase[i] == Spinner.EMPTY && spinnerViews[i].isShowRing)
            if (spinnerViews[i].label != label[i]) {
                spinnerViews[i].label = label[i]
                changed = true
            }
            if (spinnerViews[i].isShowRing != visible) {
                spinnerViews[i].isShowRing = visible
                changed = true
            }
            if (changed) {
                spinnerViews[i].invalidate()
            }
        }
    }

    /**
     * Start updating the spinner views
     */
    fun start() {
        updateTime = System.currentTimeMillis()
        this.sendEmptyMessage(REFRESH)
    }

    /**
     * Stop updating the spinner views
     */
    fun cancel() {
        this.sendEmptyMessage(CANCEL)
    }

    companion object {
        // Message.what types
        private const val REFRESH = 0
        private const val CANCEL = -1
    }
}
