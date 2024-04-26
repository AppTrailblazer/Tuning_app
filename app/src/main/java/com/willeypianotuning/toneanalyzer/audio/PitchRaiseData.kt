package com.willeypianotuning.toneanalyzer.audio

import com.willeypianotuning.toneanalyzer.store.db.tunings.PianoTuning
import kotlin.math.pow

class PitchRaiseData {
    var maxOP: Double = 0.0
    var initialPitchCurve = DoubleArray(88) { 0.0 }
    var overpullTarget = DoubleArray(88) { 0.0 }
    var overpullCents = DoubleArray(88) { 0.0 }

    fun interpolate(options: PitchRaiseOptions, fx: DoubleArray): Int {
        val pitchRaiseKeys = options.raiseKeys
        val interpX = mutableListOf<Int>()
        val interpY = mutableListOf<Double>()

        for (i in fx.indices) {
            if (pitchRaiseKeys[i] && fx[i] != 0.0) {
                interpX.add(i + 1)
                interpY.add(fx[i])
            }
        }

        var lastIndex = -1
        var lastValue: Double = -1.0
        var firstIndex = -1
        for (i in 0 until 88) {
            for (k in 0 until interpX.size) {
                if (interpX[k] == i + 1) {
                    if (lastIndex >= 0) {
                        val interpValue = (interpY[k] - lastValue) / (i - lastIndex)
                        initialPitchCurve[lastIndex] = lastValue
                        for (j in (lastIndex + 1)..i) {
                            initialPitchCurve[j] = initialPitchCurve[j - 1] + interpValue
                        }
                        if (firstIndex < 0) {
                            firstIndex = lastIndex
                        }
                    }
                    lastIndex = i
                    lastValue = interpY[k]
                }
            }
        }

        for (i in firstIndex - 1 downTo 0) {
            initialPitchCurve[i] = initialPitchCurve[i + 1]
        }

        if (lastIndex >= 0) {
            for (i in (lastIndex + 1) until 88) {
                initialPitchCurve[i] = initialPitchCurve[i - 1]
            }
        }

        return lastIndex
    }

    fun updateOverpull(
        options: PitchRaiseOptions,
        tuning: PianoTuning,
        maxOverpull: Double,
        maxOverpullMultiplier: Double
    ) {
        val lowestUnwound = options.lowestUnwound.toDouble()
        val highestMidsection = options.highestMidsection.toDouble()
        val pianoType = options.pianoType

        for (i in 0 until 88) {
            // Calculate a line with max overpull at note 88
            val x = (i + 1).toDouble()
            val opLinear = ((maxOP * maxOverpullMultiplier - 5.0) / 88.0 + 0.05) * x + 5.0

            // Calculate "correction curves" that go to zero at 1 and 88 (no overpull on those notes)
            val correctionCurve1 = (x - 0.75) / (1.0 + (x - 0.75))
            val correctionCurve2 = (89.0 / 2.0 - x / 2.0) / (1.0 + (89.0 / 2.0 - x / 2.0))
            var correctionCurve3 = 0.0
            val cc3amp = 30.0

            if (pianoType in 3..6) {
                if (x in lowestUnwound..highestMidsection) {
                    correctionCurve3 = 1.0 / (highestMidsection + 1.0 - lowestUnwound) *
                            (x - lowestUnwound) *
                            (-cc3amp * 0.8 + (cc3amp * 0.8 / ((lowestUnwound - highestMidsection - 1.0) / 2.0).pow(
                                2.0
                            )) *
                                    (x - (lowestUnwound + highestMidsection + 1) / 2.0).pow(2.0))
                } else if (x >= highestMidsection + 1.0 && x <= 88.0) {
                    correctionCurve3 =
                        (1.0 - (1.0 / (88.0 - highestMidsection - 1.0) * (x - highestMidsection - 1.0))) *
                                (cc3amp - (cc3amp / ((highestMidsection + 1.0 - 88.0) / 2.0).pow(2.0)) *
                                        (x - (highestMidsection + 1.0 + 88.0) / 2.0).pow(2.0))
                }
            }

            // Multiply overpull by the correction curve to calculate the actual overpull percentage
            val overpullPercent =
                (opLinear + correctionCurve3) * correctionCurve1 * correctionCurve2
            val initialPitchDelta = initialPitchCurve[i]
            overpullCents[i] = minOf(-initialPitchDelta * overpullPercent * 0.01, maxOverpull)
            overpullTarget[i] = overpullCents[i] + tuning.measurements.delta[i]
        }
    }
}