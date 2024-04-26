package com.willeypianotuning.toneanalyzer.utils.decimal

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DecimalInputFilterTest {
    @Test
    fun filterWithDot() {
        val df = DecimalInputFilter(signed = true, maxPrecision = 2)
        Assert.assertTrue(df.isValid("1"))
        Assert.assertTrue(df.isValid("-1"))
        Assert.assertTrue(df.isValid(".2"))
        Assert.assertTrue(df.isValid("-.2"))
        Assert.assertTrue(df.isValid("1.2"))
        Assert.assertTrue(df.isValid("-1.2"))
        Assert.assertTrue(df.isValid("1.23"))
        Assert.assertTrue(df.isValid("-1.23"))
        Assert.assertFalse(df.isValid("1.234"))
        Assert.assertFalse(df.isValid("-1.234"))
    }

    @Test
    fun filterWithComma() {
        val df = DecimalInputFilter(signed = true, maxPrecision = 2)
        Assert.assertTrue(df.isValid("1"))
        Assert.assertTrue(df.isValid("-1"))
        Assert.assertTrue(df.isValid(",2"))
        Assert.assertTrue(df.isValid("-,2"))
        Assert.assertTrue(df.isValid("1,2"))
        Assert.assertTrue(df.isValid("-1,2"))
        Assert.assertTrue(df.isValid("1,23"))
        Assert.assertTrue(df.isValid("-1,23"))
        Assert.assertFalse(df.isValid("1,234"))
        Assert.assertFalse(df.isValid("-1,234"))
    }

    @Test
    fun filterInvalid() {
        val df = DecimalInputFilter(signed = true, maxPrecision = 2)
        Assert.assertFalse(df.isValid("1,2.2"))
        Assert.assertFalse(df.isValid("1.2,2"))
        Assert.assertFalse(df.isValid("a.3"))
        Assert.assertFalse(df.isValid("3.o"))
    }
}