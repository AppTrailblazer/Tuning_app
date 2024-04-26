package com.willeypianotuning.toneanalyzer.store.migrations

import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.pow


class PianoTuningFixPeakHeightsMigration {

    private val peakHeightsGuessOld = Array(88) { DoubleArray(10) { 0.0 } }
    private val peakHeightsGuess = Array(88) { DoubleArray(10) { 0.0 } }

    init {
        for (j in 0 until 88) {
            peakHeightsGuessOld[j][0] = (10 * exp(-(j + 1 - 83.0).pow(2) / 2500.0)) / 50.0
            peakHeightsGuessOld[j][1] = (10 * exp(-(j + 1 - 37.0).pow(2) / 400.0)) / 50.0
            peakHeightsGuessOld[j][2] = (10 * exp(-(j + 1 - 35.0).pow(2) / 400.0)) / 50.0
            peakHeightsGuessOld[j][3] = (10 * exp(-(j + 1 - 33.0).pow(2) / 400.0)) / 50.0
            peakHeightsGuessOld[j][4] = (10 * exp(-(j + 1 - 27.0).pow(2) / 400.0)) / 50.0
            peakHeightsGuessOld[j][5] = (10 * exp(-(j + 1 - 20.0).pow(2) / 400.0)) / 50.0
            peakHeightsGuessOld[j][6] = (10 * exp(-(j + 1 - 12.0).pow(2) / 400.0)) / 50.0
            peakHeightsGuessOld[j][7] = (10 * exp(-(j + 1 - 7.0).pow(2) / 400.0)) / 50.0
            peakHeightsGuessOld[j][8] = (10 * exp(-(j + 1 - 4.0).pow(2) / 400.0)) / 50.0
            peakHeightsGuessOld[j][9] = (10 * exp(-(j + 1 - 1.0).pow(2) / 400.0)) / 50.0

            peakHeightsGuess[j][0] = (1.0 / (1.0 + exp(-0.1833 * (j + 1 - 37))))
            peakHeightsGuess[j][1] = peakHeightsGuessOld[j][1]
            peakHeightsGuess[j][2] = peakHeightsGuessOld[j][2]
            peakHeightsGuess[j][3] = peakHeightsGuessOld[j][3]
            peakHeightsGuess[j][4] = peakHeightsGuessOld[j][4]
            peakHeightsGuess[j][5] = peakHeightsGuessOld[j][5]
            peakHeightsGuess[j][6] = peakHeightsGuessOld[j][6]
            peakHeightsGuess[j][7] = peakHeightsGuessOld[j][7]
            peakHeightsGuess[j][8] = peakHeightsGuessOld[j][8]
            peakHeightsGuess[j][9] = peakHeightsGuessOld[j][9]
        }
    }

    fun migrate(peakHeights: Array<DoubleArray>) {
        // Temporarily replace all "default" values with zeros
        for (i in 0 until 88) {
            for (j in 0 until 10) {
                if (abs(peakHeights[i][j] - peakHeightsGuessOld[i][j]) < 0.01) {
                    peakHeights[i][j] = 0.0
                }
            }
        }

        for (i in 0 until 88) {
            for (j in 10 until peakHeights[i].size) {
                peakHeights[i][j] = 0.0
            }
        }

        // Shift right from 1st harmonic: notes 1 through 20
        for (i in 0 until 20) {
            if (peakHeights[i][0] != 0.0) {
                for (j in 8 downTo 0) {
                    peakHeights[i][j + 1] = peakHeights[i][j]
                }
                peakHeights[i][0] = 0.0
            }
        }
        // Shift right from 2nd harmonic: notes 1:8
        for (i in 0 until 8) {
            if (peakHeights[i][1] != 0.0) {
                for (j in 8 downTo 1) {
                    peakHeights[i][j + 1] = peakHeights[i][j]
                }
                peakHeights[i][1] = 0.0
            }
        }
        // Shift right from 3rd harmonic: note 1
        if (peakHeights[0][2] != 0.0) {
            for (j in 8 downTo 2) {
                peakHeights[0][j + 1] = peakHeights[0][j]
            }
            peakHeights[0][2] = 0.0
        }
        // Replace all zeros with new default guesses
        for (i in 0 until 88) {
            for (j in 0 until 10) {
                if (peakHeights[i][j] == 0.0) {
                    peakHeights[i][j] = peakHeightsGuess[i][j]
                }
            }
        }
    }

}