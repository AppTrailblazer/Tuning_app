package com.willeypianotuning.toneanalyzer.extensions

/**
 * Converts 2D matrix into 1D array
 */
fun Array<DoubleArray>.flatten(): DoubleArray {
    val array = DoubleArray(this.size * this[0].size)
    for (i in this.indices) {
        for (j in this[i].indices) {
            array[i * this[i].size + j] = this[i][j]
        }
    }
    return array
}

fun Array<ShortArray>.flatten(): ShortArray {
    val array = ShortArray(this.size * this[0].size)
    for (i in this.indices) {
        for (j in this[i].indices) {
            array[i * this[i].size + j] = this[i][j]
        }
    }
    return array
}

fun Array<FloatArray>.flatten(): FloatArray {
    val array = FloatArray(this.size * this[0].size)
    for (i in this.indices) {
        for (j in this[i].indices) {
            array[i * this[i].size + j] = this[i][j]
        }
    }
    return array
}

/**
 * Converts 1D array into 2D matrix
 */
fun DoubleArray.reshape(rows: Int, columns: Int): Array<DoubleArray> {
    require(this.size == rows * columns) { "Array length should be equal the product of rows by columns" }
    val matrix = Array(rows) { DoubleArray(columns) }
    for (i in 0 until rows) {
        for (j in 0 until columns) {
            matrix[i][j] = this[i * columns + j]
        }
    }
    return matrix
}

fun FloatArray.reshape(rows: Int, columns: Int): Array<FloatArray> {
    require(this.size == rows * columns) { "Array length should be equal the product of rows by columns" }
    val matrix = Array(rows) { FloatArray(columns) }
    for (i in 0 until rows) {
        for (j in 0 until columns) {
            matrix[i][j] = this[i * columns + j]
        }
    }
    return matrix
}

fun ShortArray.reshape(rows: Int, columns: Int): Array<ShortArray> {
    require(this.size == rows * columns) { "Array length should be equal the product of rows by columns" }
    val matrix = Array(rows) { ShortArray(columns) }
    for (i in 0 until rows) {
        for (j in 0 until columns) {
            matrix[i][j] = this[i * columns + j]
        }
    }
    return matrix
}