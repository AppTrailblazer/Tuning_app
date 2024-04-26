package com.willeypianotuning.toneanalyzer.generator

import android.content.Context
import com.willeypianotuning.toneanalyzer.audio.handler.WavOutput
import com.willeypianotuning.toneanalyzer.audio.handler.WriteToWavFileAudioHandler
import com.willeypianotuning.toneanalyzer.extensions.flatten
import com.willeypianotuning.toneanalyzer.utils.assertContentEquals
import org.junit.After
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import java.io.File
import kotlin.math.roundToInt
import kotlin.test.assertEquals

class ToneGeneratorWrapperTest {
    private lateinit var toneGenerator: ToneGeneratorWrapper

    @Before
    fun setUp() {
        toneGenerator = ToneGeneratorWrapper()
        toneGenerator.audioFrequency = 16000
    }

    @After
    fun tearDown() {
        toneGenerator.close()
    }

    private fun readTsvFloatArray(name: String): Array<FloatArray> {
        val content = this.javaClass.getResourceAsStream(name)!!.bufferedReader().readText()
        return content.split("\n").filter { it.isNotBlank() }.map { row ->
            row.split("\t").filter { it.isNotEmpty() }.map { it.toFloat() }.toFloatArray()
        }.toTypedArray()
    }

    private fun readTsvShortArray(name: String): Array<ShortArray> {
        val content = this.javaClass.getResourceAsStream(name)!!.bufferedReader().readText()
        return content.split("\n").filter { it.isNotBlank() }.map { row ->
            row.split("\t").filter { it.isNotEmpty() }.map { it.toShort() }.toShortArray()
        }.toTypedArray()
    }

    @Test
    fun `test volume multiplier initialized properly`() {
        val actual = toneGenerator.volumeMultiplier.roundTo(4)
        val expected = readTsvFloatArray("/generator/volumeMultiplier.tsv").flatten().roundTo(4)
        assertContentEquals(expected, actual, 1e-4f)
    }

    @Test
    fun `test playback partials initialized properly`() {
        val actual = toneGenerator.playbackPartials
        val expected = readTsvShortArray("/generator/playbackPartials.tsv")
        assertContentEquals(expected, actual)
    }

    @Test
    fun `test harmonic amplitudes initialized properly`() {
        val actual = toneGenerator.harmonicsAmplitudes.roundTo(8)
        val expected = readTsvFloatArray("/generator/harmonicAmplitudes.tsv").roundTo(8)
        assertContentEquals(expected, actual, 1e-4f)
    }

    @Test
    fun `test generate tone`() {
        val buffer = ShortArray((toneGenerator.audioFrequency * 0.5f).roundToInt())
        assertEquals(8000, buffer.size)
        toneGenerator.initTone(48)
        toneGenerator.generateTone(buffer)
        val expected = readTsvShortArray("/generator/tone_49.tsv").flatten()
        assertContentEquals(expected, buffer)
    }

    //    @Ignore
    @Test
    fun `generate tone and write to wav file`() {
        val buffer = ShortArray(toneGenerator.audioFrequency)
        toneGenerator.initTone(48)
        toneGenerator.generateTone(buffer)
        val handler = WriteToWavFileAudioHandler(WavOutput.WavFile(File("audio_49.wav")))
        handler.prepare()
        handler.handle(buffer, buffer.size)
        handler.finish()
    }

    private fun FloatArray.roundTo(digits: Int): FloatArray {
        return map { String.format("%.${digits}f", it).toFloat() }.toFloatArray()
    }

    private fun Array<FloatArray>.roundTo(digits: Int): Array<FloatArray> {
        return map { it.roundTo(digits) }.toTypedArray()
    }
}