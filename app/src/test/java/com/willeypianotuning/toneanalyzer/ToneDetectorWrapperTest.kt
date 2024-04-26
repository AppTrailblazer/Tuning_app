package com.willeypianotuning.toneanalyzer

import com.willeypianotuning.toneanalyzer.sync.json.toJsonArray
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals

internal class ToneDetectorWrapperTest {

    private lateinit var toneAnalyzer: ToneDetectorWrapper

    @Before
    fun setUp() {
        toneAnalyzer = ToneDetectorWrapper.newInstance()
    }

    @After
    fun tearDown() {
        toneAnalyzer.close()
    }

    @Test
    fun `test fx returned by the native code is the same as passed`() {
        val expected = DoubleArray(88) { i -> i.toDouble() }
        toneAnalyzer.fx = expected
        val actual = toneAnalyzer.fx
        assertEquals(expected.toJsonArray().toString(), actual.toJsonArray().toString())
    }

    @Test
    fun `test harmonics returned by the native code is the same as passed`() {
        val expected = Array(88) { i -> DoubleArray(10) { j -> (i * 10 + j).toDouble() } }
        toneAnalyzer.harmonics = expected
        val actual = toneAnalyzer.harmonics
        assertEquals(expected.toJsonArray().toString(), actual.toJsonArray().toString())
    }

    @Test
    fun `test inharmonicity returned by the native code is the same as passed`() {
        val expected = Array(88) { i -> DoubleArray(3) { j -> (i * 3 + j).toDouble() } }
        toneAnalyzer.inharmonicity = expected
        val actual = toneAnalyzer.inharmonicity
        assertEquals(expected.toJsonArray().toString(), actual.toJsonArray().toString())
    }
}