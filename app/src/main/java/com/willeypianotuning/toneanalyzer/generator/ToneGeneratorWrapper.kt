package com.willeypianotuning.toneanalyzer.generator

import androidx.annotation.VisibleForTesting
import com.willeypianotuning.toneanalyzer.extensions.reshape
import androidx.annotation.IntRange
import timber.log.Timber

class ToneGeneratorWrapper(pianoKeyFrequenciesPtr: Long = 0) : ToneGenerator, AutoCloseable {
    private var nativePtr: Long = createNative(pianoKeyFrequenciesPtr)

    override var audioFrequency: Int = 48000
        set(value) {
            field = value
            setAudioFrequency(nativePtr, value)
        }

    @VisibleForTesting
    val volumeMultiplier: FloatArray
        get() {
            val buffer = FloatArray(88)
            getVolumeMultiplier(nativePtr, buffer)
            return buffer
        }

    @VisibleForTesting
    val playbackPartials: Array<ShortArray>
        get() {
            val buffer = ShortArray(88 * 8)
            getPlaybackPartials(nativePtr, buffer)
            return buffer.reshape(8, 88)
        }

    @VisibleForTesting
    val harmonicsAmplitudes: Array<FloatArray>
        get() {
            val buffer = FloatArray(88 * 10)
            getHarmonicsAmplitudes(nativePtr, buffer)
            return buffer.reshape(88, 10)
        }

    override var trebleBassOptions: TrebleBassOptions = TrebleBassOptions()
        set(value) {
            field = value
            setTrebleBassOptions(
                nativePtr,
                value.trebleVolume,
                value.trebleEdge,
                value.bassVolume,
                value.bassEdge
            )
        }

    override fun initTone(@IntRange(0, 87) note: Int) {
        assert(note in 0..87) { "Note should be 0 indexed "}
        initTone(nativePtr, note)
    }

    override fun generateTone(buffer: ShortArray) {
        generateTone(nativePtr, buffer, buffer.size)
    }

    override fun close() {
        destroyNative(nativePtr)
        nativePtr = 0L
    }

    private external fun createNative(pianoKeyFrequenciesPtr: Long): Long
    private external fun setAudioFrequency(instance: Long, frequency: Int)
    private external fun setTrebleBassOptions(
        instance: Long,
        trebleVolume: Float,
        trebleEdge: Short,
        bassVolume: Float,
        bassEdge: Short
    )

    private external fun initTone(instance: Long, @IntRange(0, 87) noteZeroIndexed: Int)
    private external fun generateTone(instance: Long, buffer: ShortArray, bufferLen: Int)
    private external fun getVolumeMultiplier(instance: Long, buffer: FloatArray)
    private external fun getPlaybackPartials(instance: Long, buffer: ShortArray)
    private external fun getHarmonicsAmplitudes(instance: Long, buffer: FloatArray)

    private external fun destroyNative(instance: Long)

    companion object {
        init {
            System.loadLibrary("WilleyToneAnalyzerLib")
        }
    }
}