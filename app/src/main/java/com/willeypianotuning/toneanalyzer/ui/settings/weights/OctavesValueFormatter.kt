package com.willeypianotuning.toneanalyzer.ui.settings.weights

class OctavesValueFormatter : WeightTuneView.ValueFormatter {
    companion object {
        private const val DIVIDER = 12
    }

    override fun parse(value: String): Float? {
        val valueOctaves = value.toFloatOrNull() ?: return null
        return valueOctaves * DIVIDER
    }

    override fun format(value: Float, precision: Int): String {
        val newValue = value / DIVIDER
        return if (precision > 0) {
            ("%.${precision}f").format(newValue, value.toInt())
        } else {
            "%d".format(newValue.toInt())
        }
    }

}