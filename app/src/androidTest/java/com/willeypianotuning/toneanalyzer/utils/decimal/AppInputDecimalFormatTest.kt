package com.willeypianotuning.toneanalyzer.utils.decimal

import org.junit.Assert
import org.junit.Test

class AppInputDecimalFormatTest {

    private fun assertEqualsDouble(expected: Double?, actual: Double?) {
        if (expected == null) {
            Assert.assertNull(actual)
            return
        }
        Assert.assertNotNull(actual)
        Assert.assertEquals(expected, requireNotNull(actual), 1e-6)
    }

    @Test
    fun parseDouble() {
        assertEqualsDouble(null, AppInputDecimalFormat.parseDouble("1.3.3"))
        assertEqualsDouble(null, AppInputDecimalFormat.parseDouble("1,3.2"))
        assertEqualsDouble(0.3, AppInputDecimalFormat.parseDouble(".3"))
        assertEqualsDouble(-0.3, AppInputDecimalFormat.parseDouble("-.3"))
        assertEqualsDouble(1.3, AppInputDecimalFormat.parseDouble("1,3"))
        assertEqualsDouble(-1.3, AppInputDecimalFormat.parseDouble("-1.3"))
        assertEqualsDouble(-1.3, AppInputDecimalFormat.parseDouble("-1,3"))
    }
}