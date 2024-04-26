package com.willeypianotuning.toneanalyzer.utils.decimal

import android.text.InputFilter
import android.text.Spanned

class DecimalInputFilter @JvmOverloads constructor(signed: Boolean = true, maxPrecision: Int = 2) :
    InputFilter {
    private val regex: Regex

    init {
        val signedFormat = if (signed) "-?" else ""
        val format = if (maxPrecision < 1) {
            "$signedFormat(([0-9]*)([\\.\\,])?)?"
        } else {
            "$signedFormat(([0-9]*)([\\.\\,])?)?([0-9]{0,$maxPrecision})?"
        }
        regex = format.toRegex()
    }

    fun isValid(input: String): Boolean {
        return input.matches(regex)
    }

    override fun filter(
        source: CharSequence,
        start: Int,
        end: Int,
        dest: Spanned,
        dstart: Int,
        dend: Int
    ): CharSequence? {
        val builder = StringBuilder(dest)
        builder.replace(dstart, dend, source.subSequence(start, end).toString())
        return if (!isValid(builder.toString())) {
            if (source.isEmpty())
                dest.subSequence(dstart, dend)
            else
                ""
        } else null
    }
}
