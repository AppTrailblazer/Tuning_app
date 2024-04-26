package com.willeypianotuning.toneanalyzer.extensions

import kotlin.math.floor
import kotlin.math.roundToInt

fun Int.clamp(min: Int, max: Int): Int {
    require(max >= min)
    return minOf(maxOf(this, min), max)
}

fun Double.clamp(min: Double, max: Double): Double {
    require(max >= min)
    return minOf(maxOf(this, min), max)
}

fun Double.round(places: Int): Double {
    require(places >= 0)
    var multiplier = 1.0
    repeat(places) { multiplier *= 10 }
    return (this * multiplier).roundToInt() / multiplier
}

fun Double.fraction(maxDenominator: Int): Pair<Int, Int> {
    var input = this
    var p0 = 1
    var q0 = 0
    var p1 = floor(input).toInt()
    var q1 = 1
    var p2: Int
    var q2: Int
    var r = input - p1
    var nextCf: Double
    val limit = 1.0 / maxDenominator
    while (true) {
        r = 1.0 / r
        nextCf = floor(r)
        p2 = (nextCf * p1 + p0).toInt()
        q2 = (nextCf * q1 + q0).toInt()

        // Limit the numerator and denominator to be 256 or less
        if (p2 > maxDenominator || q2 > maxDenominator) break

        // remember the last two fractions
        p0 = p1
        p1 = p2
        q0 = q1
        q1 = q2
        r -= nextCf
        if (r < limit) {
            break
        }
    }
    input = p1.toDouble() / q1
    // hard upper and lower bounds for ratio
    if (input > maxDenominator) {
        p1 = 256
        q1 = 1
    } else if (input < 1.0 / maxDenominator.toDouble()) {
        p1 = 1
        q1 = 256
    }
    return p1 to q1
}