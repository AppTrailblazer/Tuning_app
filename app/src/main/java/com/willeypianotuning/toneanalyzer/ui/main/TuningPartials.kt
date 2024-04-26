package com.willeypianotuning.toneanalyzer.ui.main

object TuningPartials {
    val tuningPartials: Array<IntArray> = run {
        val array = Array(5) { IntArray(88) }
        for (i in 0..87) {
            when {
                i < 6 -> {
                    array[0][i] = 4
                    array[1][i] = 5
                    array[2][i] = 6
                    array[3][i] = 8
                    array[4][i] = 10
                }
                i < 12 -> {
                    array[0][i] = 3
                    array[1][i] = 4
                    array[2][i] = 5
                    array[3][i] = 6
                    array[4][i] = 8
                }
                i < 24 -> {
                    array[0][i] = 2
                    array[1][i] = 3
                    array[2][i] = 4
                    array[3][i] = 5
                    array[4][i] = 6
                }
                i < 33 -> {
                    array[0][i] = 2
                    array[1][i] = 3
                    array[2][i] = 4
                    array[3][i] = 5
                    array[4][i] = 1
                }
                i < 48 -> {
                    array[0][i] = 1
                    array[1][i] = 2
                    array[2][i] = 3
                    array[3][i] = 4
                }
                i < 61 -> {
                    array[0][i] = 1
                    array[1][i] = 2
                    array[2][i] = 3
                }
                i < 73 -> {
                    array[0][i] = 1
                    array[1][i] = 2
                    array[2][i] = 0
                }
                else -> {
                    array[0][i] = 1
                    array[1][i] = 0
                    array[2][i] = 0
                }
            }
        }
        array
    }

    val ringDivisions: Array<IntArray> = arrayOf(
        intArrayOf(4, 5, 6, 8),
        intArrayOf(4, 5, 6, 8),
        intArrayOf(4, 5, 6, 8),
        intArrayOf(4, 5, 6, 8),
        intArrayOf(6, 8, 9, 12),
        intArrayOf(6, 8, 9, 12),
        intArrayOf(6, 8, 10, 12),
        intArrayOf(6, 8, 10, 12),
        intArrayOf(6, 8, 10, 12),
        intArrayOf(6, 8, 10, 12),
        intArrayOf(6, 8, 10, 12),
        intArrayOf(6, 8, 10, 12),
        intArrayOf(4, 6, 8, 10),
        intArrayOf(6, 9, 12, 15),
        intArrayOf(6, 9, 12, 15),
        intArrayOf(6, 9, 12, 15),
        intArrayOf(6, 9, 12, 15),
        intArrayOf(6, 9, 12, 15),
        intArrayOf(6, 9, 12, 15),
        intArrayOf(6, 9, 12, 15),
        intArrayOf(6, 9, 12, 15),
        intArrayOf(6, 9, 12, 15),
        intArrayOf(6, 9, 12, 15),
        intArrayOf(8, 12, 16, 20),
        intArrayOf(8, 12, 16, 20),
        intArrayOf(8, 12, 16, 20),
        intArrayOf(8, 12, 16, 20),
        intArrayOf(8, 12, 16, 20),
        intArrayOf(10, 15, 20, 25),
        intArrayOf(10, 15, 20, 25),
        intArrayOf(10, 15, 20, 25),
        intArrayOf(10, 15, 20, 25),
        intArrayOf(12, 18, 24, 30),
        intArrayOf(6, 12, 18, 24),
        intArrayOf(6, 12, 18, 24),
        intArrayOf(6, 12, 18, 24),
        intArrayOf(8, 16, 24, 32),
        intArrayOf(8, 16, 24, 32),
        intArrayOf(8, 16, 24, 32),
        intArrayOf(8, 16, 24, 32),
        intArrayOf(10, 20, 30, 40),
        intArrayOf(10, 20, 30, 40),
        intArrayOf(10, 20, 30, 40),
        intArrayOf(10, 20, 30, 40),
        intArrayOf(12, 24, 36, 48),
        intArrayOf(12, 24, 36, 48),
        intArrayOf(12, 24, 36, 48),
        intArrayOf(12, 24, 36, 48),
        intArrayOf(0, 14, 28, 42),
        intArrayOf(0, 14, 28, 42),
        intArrayOf(0, 16, 32, 48),
        intArrayOf(0, 16, 32, 48),
        intArrayOf(0, 18, 36, 54),
        intArrayOf(0, 18, 36, 54),
        intArrayOf(0, 20, 40, 60),
        intArrayOf(0, 20, 40, 60),
        intArrayOf(0, 20, 40, 60),
        intArrayOf(0, 24, 48, 72),
        intArrayOf(0, 24, 48, 72),
        intArrayOf(0, 24, 48, 72),
        intArrayOf(0, 24, 48, 72),
        intArrayOf(0, 0, 28, 56),
        intArrayOf(0, 0, 28, 56),
        intArrayOf(0, 0, 28, 56),
        intArrayOf(0, 0, 28, 56),
        intArrayOf(0, 0, 32, 64),
        intArrayOf(0, 0, 32, 64),
        intArrayOf(0, 0, 32, 64),
        intArrayOf(0, 0, 32, 64),
        intArrayOf(0, 0, 32, 64),
        intArrayOf(0, 0, 32, 64),
        intArrayOf(0, 0, 36, 72),
        intArrayOf(0, 0, 36, 72),
        intArrayOf(0, 0, 0, 36),
        intArrayOf(0, 0, 0, 40),
        intArrayOf(0, 0, 0, 40),
        intArrayOf(0, 0, 0, 40),
        intArrayOf(0, 0, 0, 48),
        intArrayOf(0, 0, 0, 48),
        intArrayOf(0, 0, 0, 48),
        intArrayOf(0, 0, 0, 48),
        intArrayOf(0, 0, 0, 48),
        intArrayOf(0, 0, 0, 56),
        intArrayOf(0, 0, 0, 56),
        intArrayOf(0, 0, 0, 56),
        intArrayOf(0, 0, 0, 64),
        intArrayOf(0, 0, 0, 64),
        intArrayOf(0, 0, 0, 64)
    )
}