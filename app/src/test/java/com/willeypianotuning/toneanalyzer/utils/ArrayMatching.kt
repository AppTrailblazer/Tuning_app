package com.willeypianotuning.toneanalyzer.utils

import kotlin.test.assertEquals
import kotlin.test.fail

fun <T> Array<T>.toCsv(): String {
    val builder = StringBuilder()
    for (item in this) {
        when {
            item is Array<*> -> builder.append(item.toCsv()).append("\n")
            item is Collection<*> -> builder.append(item.toTypedArray().toCsv()).append("\n")
            item is FloatArray -> builder.append(item.toTypedArray().toCsv()).append("\n")
            item is DoubleArray -> builder.append(item.toTypedArray().toCsv()).append("\n")
            item is IntArray -> builder.append(item.toTypedArray().toCsv()).append("\n")
            item is LongArray -> builder.append(item.toTypedArray().toCsv()).append("\n")
            item is ByteArray -> builder.append(item.toTypedArray().toCsv()).append("\n")
            item is ShortArray -> builder.append(item.toTypedArray().toCsv()).append("\n")
            item is CharArray -> builder.append(item.toTypedArray().toCsv()).append("\n")
            item is BooleanArray -> builder.append(item.toTypedArray().toCsv()).append("\n")
            else -> builder.append(item.toString()).append(";")
        }
    }
    return builder.toString()
}

fun assertContentEquals(expected: ShortArray, actual: ShortArray) {
    if (expected.size != actual.size) {
        fail("Size of expected array (${expected.size}) differs from actual array (${actual.size})")
    }

    for (i in expected.indices) {
        assertEquals(expected[i], actual[i], "Element $i is different")
    }
}

fun assertContentEquals(expected: FloatArray, actual: FloatArray, epsilon: Float) {
    if (expected.size != actual.size) {
        fail("Size of expected array (${expected.size}) differs from actual array (${actual.size})")
    }

    for (i in expected.indices) {
        assertEquals(expected[i], actual[i], epsilon)
    }
}

fun assertContentEquals(
    expected: Array<ShortArray>,
    actual: Array<ShortArray>
) {
    if (expected.size != actual.size) {
        fail("Size of expected array (${expected.size}) differs from actual array (${actual.size})")
    }

    for (i in expected.indices) {
        val expectedRow = expected[i]
        val actualRow = actual[i]
        assertContentEquals(expectedRow, actualRow)
    }
}

fun assertContentEquals(
    expected: Array<FloatArray>,
    actual: Array<FloatArray>,
    epsilon: Float
) {
    if (expected.size != actual.size) {
        fail("Size of expected array (${expected.size}) differs from actual array (${actual.size})")
    }

    for (i in expected.indices) {
        val expectedRow = expected[i]
        val actualRow = actual[i]
        assertContentEquals(expectedRow, actualRow, epsilon)
    }
}