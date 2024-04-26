package com.willeypianotuning.toneanalyzer.audio.source

import kotlin.math.PI
import kotlin.math.sin

/**
 * This class implements audio source which generates a pure sin-wave tone of given frequency.
 * The primary purpose of this class is debugging.
 */
@Suppress("unused")
class ArtificialToneSource(
    var frequency: Float = 440.5f,
    private val enforceSpeed: Boolean = true
) : AudioSource {
    private val sinWave = ShortArray(samplingRate) { i ->
        (Short.MIN_VALUE * sin(2F * PI.toFloat() * i.toFloat() / samplingRate.toFloat())).toInt()
            .toShort()
    }

    override val samplingRate: Int
        get() = 16000

    override val bufferSizeSamples: Int
        get() = 2048
    var phase = 0.0

    override fun start(): Boolean {
        return true
    }

    override fun read(audioData: ShortArray): Int {
        for (i in audioData.indices) {
            phase %= sinWave.size
            audioData[i] = sinWave[phase.toInt()]
            phase += frequency
        }
        if (enforceSpeed) {
            kotlin.runCatching {
                Thread.sleep((audioData.size * 1000.0 / samplingRate).toLong())
            }
        }
        return audioData.size
    }

    override fun stop() {

    }

}