package com.willeypianotuning.toneanalyzer.audio

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import com.willeypianotuning.toneanalyzer.ToneDetectorWrapper
import com.willeypianotuning.toneanalyzer.audio.note_names.English1NoteNamingConvention
import com.willeypianotuning.toneanalyzer.audio.source.ArtificialToneSource
import com.willeypianotuning.toneanalyzer.audio.source.AudioSource
import com.willeypianotuning.toneanalyzer.audio.source.WavAudioSource
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import timber.log.Timber

@LargeTest
@RunWith(AndroidJUnit4::class)
class NotesDetectionTest {
    private lateinit var toneDetector: ToneDetectorWrapper

    @Before
    fun setUp() {
        if (Timber.forest().isEmpty()) {
            Timber.plant(Timber.DebugTree())
        }

        toneDetector = ToneDetectorWrapper.newInstance()
    }

    @After
    fun tearDown() {
        toneDetector.close()
    }

    private fun audioSourceFromAssetWavFile(filePath: String): AudioSource {
        val context = InstrumentationRegistry.getInstrumentation().context
        val inputStream = context.assets.open(filePath)
        val audioSource = WavAudioSource(inputStream, enforceSpeed = false)
        require(audioSource.start()) { "Failed to load audio" }
        Timber.i("Audio loaded. Sample Rate = ${audioSource.samplingRate}. Buffer Size = ${audioSource.bufferSizeSamples}")
        return audioSource
    }

    private fun audioSourceFromFrequency(frequency: Float): AudioSource {
        val audioSource = ArtificialToneSource(frequency = frequency, enforceSpeed = false)
        require(audioSource.start()) { "Failed to load audio" }
        Timber.i("Audio loaded. Sample Rate = ${audioSource.samplingRate}. Buffer Size = ${audioSource.bufferSizeSamples}")
        return audioSource
    }

    private fun supplyAudioFrames(audioSource: AudioSource, numberOfFrames: Int) {
        toneDetector.setInputSamplingRate(audioSource.samplingRate.toDouble())

        val readBuffer = ShortArray(audioSource.bufferSizeSamples)
        for (i in 0 until numberOfFrames) {
            val samplesRead = audioSource.read(readBuffer)
            if (samplesRead == 0) {
                Timber.w("Read 0 samples. Audio record finished")
                break
            }
            require(samplesRead == readBuffer.size) { "Failed to read enough data" }

            Timber.i("Iteration ${i + 1}: Supplying $samplesRead samples into ToneDetector")
            toneDetector.addData(readBuffer, samplesRead)
            toneDetector.processFrame()
            toneDetector.processZeroCrossing()
            toneDetector.detectNotes()
        }
    }

    private fun noteName(noteIndex: Int): String {
        return English1NoteNamingConvention().pianoNoteName(noteIndex - 1).toString()
    }

    @Test
    fun testNotesDetection_artificialTone() {
        val audioSource = audioSourceFromFrequency(311f)

        supplyAudioFrames(audioSource, 10)

        val currentNote = toneDetector.currentNote
        Assert.assertEquals("Wrong note detected ($currentNote)", "D#4", noteName(currentNote))
    }

    @Test
    fun testNotesDetection_wavFile() {
        val audioSource = audioSourceFromAssetWavFile("audio/A4.wav")

        supplyAudioFrames(audioSource, 10)

        val currentNote = toneDetector.currentNote
        Assert.assertEquals("Wrong note detected ($currentNote)", "A4", noteName(currentNote))
    }
}