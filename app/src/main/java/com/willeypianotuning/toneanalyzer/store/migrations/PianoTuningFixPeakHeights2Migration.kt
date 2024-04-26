package com.willeypianotuning.toneanalyzer.store.migrations

import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.pow


class PianoTuningFixPeakHeights2Migration {
    private val peakHeightsGuess = Array(88) { DoubleArray(10) { 0.0 } }

    init {
        for (j in 0 until 88) {
            peakHeightsGuess[j][0] = (1.0 / (1.0 + exp(-0.1833 * (j + 1 - 37))))
            peakHeightsGuess[j][1] = (10 * exp(-(j + 1 - 37.0).pow(2) / 400.0)) / 50.0
            peakHeightsGuess[j][2] = (10 * exp(-(j + 1 - 35.0).pow(2) / 400.0)) / 50.0
            peakHeightsGuess[j][3] = (10 * exp(-(j + 1 - 33.0).pow(2) / 400.0)) / 50.0
            peakHeightsGuess[j][4] = (10 * exp(-(j + 1 - 27.0).pow(2) / 400.0)) / 50.0
            peakHeightsGuess[j][5] = (10 * exp(-(j + 1 - 20.0).pow(2) / 400.0)) / 50.0
            peakHeightsGuess[j][6] = (10 * exp(-(j + 1 - 12.0).pow(2) / 400.0)) / 50.0
            peakHeightsGuess[j][7] = (10 * exp(-(j + 1 - 7.0).pow(2) / 400.0)) / 50.0
            peakHeightsGuess[j][8] = (10 * exp(-(j + 1 - 4.0).pow(2) / 400.0)) / 50.0
            peakHeightsGuess[j][9] = (10 * exp(-(j + 1 - 1.0).pow(2) / 400.0)) / 50.0
        }
    }

    fun migrate(peakHeights: Array<DoubleArray>) {

        // Detect if the file needs to be fixed (is there data stored in the 10th column?)
        var peakHeights10Sum = 0.0
        for (i in 0 until 88) {
            peakHeights10Sum += peakHeights[i][10]
        }


        if (peakHeights10Sum <= 0.01) {
            return
        }

        val eps = 0.1
        // set column 0
        for (i in 0 until 88) {
            if (abs(peakHeights[i][1] - peakHeightsGuess[i][1]) < eps) {
                peakHeights[i][0] = peakHeightsGuess[i][0]
            } else {
                peakHeights[i][0] = 0.5 * (peakHeightsGuess[i][0] + peakHeights[i][1])
            }
        }
        // set columns 1-8 (0-indexed)
        for (i in 0 until 88) {
            for (j in 1 until 9) {
                if (abs(peakHeights[i][j + 1] - peakHeightsGuess[i][j + 1]) < eps) {
                    peakHeights[i][j] = peakHeightsGuess[i][j]
                } else {
                    peakHeights[i][j] = peakHeights[i][j + 1]
                }
            }
        }
        // set 9-th column (0-indexed)
        for (i in 0 until 88) {
            peakHeights[i][9] = peakHeightsGuess[i][9]
        }
        // reset peakHeights for notes A0-E1
        for (i in 0 until 8) {
            for (j in peakHeightsGuess[i].indices) {
                peakHeights[i][j] = peakHeightsGuess[i][j]
            }
        }
        // set columns 10-15 (0-indexed)
        for (i in 0 until 88) {
            for (j in 10 until peakHeights[i].size) {
                peakHeights[i][j] = 0.0
            }
        }
    }

}