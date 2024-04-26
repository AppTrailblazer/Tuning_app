package com.willeypianotuning.toneanalyzer.utils.decimal

import java.text.DecimalFormat
import java.text.NumberFormat
import java.util.*

object AppInputDecimalFormat {
    private val locale: Locale get() = Locale.ROOT
    private val df: NumberFormat get() = DecimalFormat.getInstance(locale)

    fun parseDouble(source: String): Double? {
        return kotlin.runCatching {
            val input = source.trim().replace(",", ".")
            if (input.count { it == '.' } > 1) {
                return@runCatching null
            }
            return@runCatching df.parse(input)?.toDouble()
        }.getOrNull()
    }

    fun formatDouble(value: Double): String {
        return df.format(value)
    }
}