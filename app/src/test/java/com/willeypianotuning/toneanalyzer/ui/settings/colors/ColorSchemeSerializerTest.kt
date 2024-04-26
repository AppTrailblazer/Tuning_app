package com.willeypianotuning.toneanalyzer.ui.settings.colors

import com.willeypianotuning.toneanalyzer.ui.colors.ColorScheme
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals

class ColorSchemeSerializerTest {
    private lateinit var serializer: ColorSchemeSerializer

    @Before
    fun setUp() {
        serializer = ColorSchemeSerializer()
    }

    @Test
    fun testToJson() {
        val colorScheme = ColorScheme(
            noteName = 0xFFFFFFFF.toInt(),
            noteNameBackground = 0xFF4a3636.toInt(),
            innerAndOuterRings = 0x00000000.toInt(),
            strobeWheels = 0xFF000000.toInt(),
            strobeBackground = 0xFFf6f6e8.toInt(),
            dialMarkings = 0x00000000.toInt(),
            needle = 0x00000000.toInt(),
            graphBackground = 0xFFece9e1.toInt(),
            tuningCurveLine = 0xFF000000.toInt(),
            tuningCurveDots = 0xFF0000ff.toInt(),
            inharmonicityLine = 0xFF0000ff.toInt(),
            inharmonicityDots = 0xFF0000ff.toInt(),
            spectrumLine = 0xFF000000.toInt(),
            currentNoteIndicator = 0xFFff0000.toInt(),
            menuPrimary = 0xFF443930.toInt(),
            menuTextPrimary = 0xFFffffff.toInt(),
            backPanel = 0xFF47382e.toInt(),
            topPanel = 0x00000000.toInt(),
            autoStepLock = 0xFF46413e.toInt(),
            autoStepLockLand = 0xFFfafaec.toInt(),
        )

        val serialized = serializer.toJson(colorScheme).toString(2)
        val expected = this.javaClass.getResourceAsStream("/serialization/color_scheme_serialized.json")!!.bufferedReader().readText()
        assertEquals(expected, serialized)
    }
}