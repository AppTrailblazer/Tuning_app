package com.willeypianotuning.toneanalyzer.spinners

import com.willeypianotuning.toneanalyzer.AppSettings
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.pow

@Singleton
class Spinners @Inject constructor(
    private val appSettings: AppSettings
) {
    companion object {
        const val SPINNER_UPDATE_INTERVAL = 33 // ~30 FPS
        const val SPINNERS = 4
    }

    private val spinners = arrayOfNulls<Spinner>(SPINNERS)
    var processSpinners: Boolean = false

    fun configure(sampleRate: Int) {
        synchronized(spinners) {
            for (i in 0 until SPINNERS) {
                // make the update interval a bit longer (+1 ms) so the queue will be short
                spinners[i] = Spinner(i, sampleRate, SPINNER_UPDATE_INTERVAL + 1, 0.07)
            }
        }
    }

    fun processFrame(buffer: ShortArray) {
        if (!processSpinners) {
            return
        }

        val calibration = 2.0.pow(-appSettings.pitchOffset / 1200.0)
        synchronized(spinners) {
            spinners[0]?.let {
                it.setFrequency(calibration * appSettings.pitchOffsetTargetFreq)
                it.setDetectionParam(0.0)  // spinner is always active
                it.divisions = 4
                it.process(buffer)
                it.isEnabled = true
            }
            // disable the rest of the spinners
            for (i in 1 until spinners.size) {
                spinners[i]?.isEnabled = false
            }
        }
    }

    fun fill(
        skippedFrames: Int,
        phase: FloatArray,
        label: Array<String>,
        divisions: IntArray,
        isEnabled: BooleanArray
    ) {
        synchronized(spinners) {
            for (i in 0 until minOf(spinners.size, label.size)) {
                val spinner = spinners[i] ?: continue

                spinner.skip(skippedFrames)
                phase[i] = spinner.phase
                label[i] = spinner.label
                divisions[i] = spinner.divisions
                isEnabled[i] = spinner.isEnabled
            }
        }
    }
}