package com.willeypianotuning.toneanalyzer.spinners

import android.os.Handler
import android.os.Looper
import android.os.Message
import android.os.SystemClock
import androidx.lifecycle.MutableLiveData
import com.willeypianotuning.toneanalyzer.ToneDetectorWrapper
import com.willeypianotuning.toneanalyzer.ui.main.NoteLock
import com.willeypianotuning.toneanalyzer.ui.main.TuningPartials
import com.willeypianotuning.toneanalyzer.ui.main.states.SpinnerState
import com.willeypianotuning.toneanalyzer.ui.main.states.SpinnersState
import timber.log.Timber
import java.util.*

/**
 * Handler that updates the spinner views
 */
class TuningSpinnerHandler(
    private val noteLock: NoteLock,
    private val analyzerWrapper: ToneDetectorWrapper,
    spinnersCount: Int = Spinners.SPINNERS
) : Handler(Looper.getMainLooper()) {

    /**
     * Stores the time when the execution of the spinners update should happen
     */
    private var updateTime: Long = 0

    private val phase: FloatArray = FloatArray(spinnersCount) { Spinner.INACTIVE }
    private val alpha: FloatArray = FloatArray(spinnersCount) { 0.0F }
    private val label: Array<String> = Array(spinnersCount) { "" }
    private val divisions: IntArray = IntArray(spinnersCount)
    private val isEnabled: BooleanArray = BooleanArray(spinnersCount)

    private val updateInterval = Spinners.SPINNER_UPDATE_INTERVAL

    private val _spinnersStateLiveData = MutableLiveData<SpinnersState>()
    val spinnersStateLiveData get() = _spinnersStateLiveData

    private fun currentTimeMillis(): Long {
        return SystemClock.uptimeMillis()
    }

    private fun getSkippedFramesAndScheduleNextRun(): Int {
        val timeWhenTheExecutionShouldHappen = updateTime
        val now = currentTimeMillis()

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

    private fun computeNewSpinnersState(): SpinnersState {
        val skippedFrames = getSkippedFramesAndScheduleNextRun()
        // based on number of skipped frames
        // we remove those from the phase queue
        for (j in phase.indices) {
            analyzerWrapper.skipPhases(j, skippedFrames)
        }

        val currentNote = analyzerWrapper.currentNote

        // note is unlocked, let's fetch update spinners with actual data
        val targetFreq = analyzerWrapper.targetPeakFrequencies
        val partials = minOf(Spinners.SPINNERS, targetFreq.size)

        // fill the outer rings first
        var partial = 0
        var ring = phase.size - partials
        while (partial < partials) {
            val phaseAndAlpha = analyzerWrapper.getPhaseAndAlpha(partial)
            phase[ring] = phaseAndAlpha[0]
            alpha[ring] = phaseAndAlpha[1]
            label[ring] = String.format(
                Locale.getDefault(),
                "%d",
                TuningPartials.tuningPartials[partial][currentNote - 1]
            )
            divisions[ring] = TuningPartials.ringDivisions[currentNote - 1][ring]
            isEnabled[ring] = true && phase[ring] != -2.0f
            partial++
            ring++
        }
        // disable the rest of the spinners
        ring = 0
        while (ring < phase.size - partials) {
            label[ring] = ""
            isEnabled[ring] = false
            ring++
        }

        return SpinnersState(
            currentNote,
            List(Spinners.SPINNERS) {
                SpinnerState(phase[it], alpha[it], label[it], divisions[it], isEnabled[it])
            }
        )
    }


    override fun handleMessage(msg: Message) {
        if (msg.what == CANCEL) {
            // we received cancel message
            // we need to remove all scheduled updates
            this.removeMessages(REFRESH)
            return
        }

        var state = computeNewSpinnersState()
        val noteUnlocked = noteLock.isNoteUnlocked(state.note)
        state = state.copy(spinners = state.spinners.map {
            it.copy(
                enabled = it.enabled && noteUnlocked,
                label = if (noteUnlocked) it.label else ""
            )
        })
        _spinnersStateLiveData.postValue(state)
    }

    /**
     * Start updating the spinner views
     */
    fun start() {
        updateTime = currentTimeMillis()
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
