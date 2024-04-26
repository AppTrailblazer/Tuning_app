package com.willeypianotuning.toneanalyzer.audio.source

import com.willeypianotuning.toneanalyzer.utils.Hardware
import timber.log.Timber
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * This class implements audio source which provides audio data from single channel wav file.
 * The primary purpose of this class is debugging.
 */
@Suppress("unused")
class WavAudioSource constructor(
    private val wavInputStream: InputStream,
    private val enforceSpeed: Boolean = true
) : AudioSource {

    constructor(file: File, enforceSpeed: Boolean = true) : this(
        FileInputStream(file),
        enforceSpeed = enforceSpeed
    )

    private var _channels = 1

    private var _samplingRate = 16000
    override val samplingRate: Int get() = _samplingRate

    private var _bufSizeSamples: Int = 0
    override val bufferSizeSamples: Int get() = _bufSizeSamples

    override fun start(): Boolean {
        val header = ByteArray(44)
        val bytesRead = wavInputStream.read(header)
        require(bytesRead == header.size) { "Malformed WAV file header. Only read $bytesRead bytes" }

        val channels =
            ByteBuffer.wrap(header.copyOfRange(22, 24)).order(ByteOrder.LITTLE_ENDIAN).short
        val sampleRate =
            ByteBuffer.wrap(header.copyOfRange(24, 28)).order(ByteOrder.LITTLE_ENDIAN).int
        val bitsPerSample =
            ByteBuffer.wrap(header.copyOfRange(34, 36)).order(ByteOrder.LITTLE_ENDIAN).short

        require(channels.toInt() > 0) { "Audio should have at least 1 channel" }
        require(sampleRate >= 16000) { "Sampling rate should be at least 16000. Got $sampleRate" }
        require(bitsPerSample.toInt() == 16) { "Should be AudioFormat.ENCODING_PCM_16BIT. Got $bitsPerSample" }

        if (channels.toInt() != 1) {
            Timber.w("WAV file contains multichannel audio. It will be converted to MONO using averaging")
        }

        _bufSizeSamples = Hardware.minBufferSizeForSampleRate(sampleRate)
        _channels = channels.toInt()
        _samplingRate = sampleRate

        return true
    }

    override fun read(audioData: ShortArray): Int {
        var readShorts = 0
        if (_channels == 1) {
            val data = ByteArray(2 * audioData.size)
            val readBytes = wavInputStream.read(data, 0, data.size)
            readShorts = readBytes / 2
            ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(audioData)
        } else {
            var readIndex = 0
            val buffer = ByteArray(2 * _channels)
            val shortBuffer = ShortArray(_channels)
            while (true) {
                val readBytes = wavInputStream.read(buffer, 0, buffer.size)
                if (readBytes == 0) {
                    break
                }

                require(readBytes == buffer.size) { "Malformed WAV file. Incomplete data sample" }

                ByteBuffer.wrap(buffer).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer()
                    .get(shortBuffer)
                audioData[readShorts++] = shortBuffer.average().toInt().toShort()

                readIndex += readBytes
                if (readShorts == audioData.size) {
                    break
                }
            }
        }

        if (enforceSpeed) {
            kotlin.runCatching {
                Thread.sleep((readShorts * 1000.0 / samplingRate).toLong())
            }
        }

        return readShorts
    }

    override fun stop() {
    }

}