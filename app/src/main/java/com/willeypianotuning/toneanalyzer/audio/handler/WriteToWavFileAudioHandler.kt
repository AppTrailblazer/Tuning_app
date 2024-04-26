package com.willeypianotuning.toneanalyzer.audio.handler

import android.content.ContentValues
import android.content.Context
import android.media.AudioFormat
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.annotation.Keep
import androidx.core.util.Consumer
import timber.log.Timber
import java.io.*
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.text.SimpleDateFormat
import java.util.*

/**
 * Writes audio data into a buffer in memory. Once `finish` method is called,
 * the audio data is written to a WAV file
 */
@Keep
@Suppress("unused")
class WriteToWavFileAudioHandler(private val output: WavOutput) : AudioHandler {
    private var outputStream: ByteArrayOutputStream? = null

    var channelConfig = AudioFormat.CHANNEL_IN_MONO
    var audioFormat = AudioFormat.ENCODING_PCM_16BIT
    var sampleRate = 16000

    constructor(context: Context) : this(wavOutputForSystem(context))

    private val channels: Short
        get() {
            return if (channelConfig == AudioFormat.CHANNEL_IN_MONO) {
                1
            } else {
                2
            }
        }

    private val bitsPerSample: Short
        get() {
            return when (audioFormat) {
                AudioFormat.ENCODING_PCM_16BIT -> 16
                else -> 8
            }
        }

    override fun prepare() {
        outputStream = ByteArrayOutputStream()
    }

    override fun handle(audioData: ShortArray, read: Int) {
        val buffer = ByteBuffer.allocate(2 * read)
        buffer.order(ByteOrder.LITTLE_ENDIAN)
        for (i in 0 until read) {
            buffer.putShort(audioData[i])
        }
        outputStream?.write(buffer.array())
    }

    override fun finish() {
        val audioData = outputStream?.toByteArray() ?: return

        val mySubChunk1Size = 16
        val myFormat: Short = 1
        val myByteRate = sampleRate * channels * bitsPerSample / 8
        val myBlockAlign: Short = (channels * bitsPerSample / 8).toShort()
        val myDataSize = audioData.size
        val myChunk2Size = myDataSize * channels * bitsPerSample / 8
        val myChunkSize = 36 + myChunk2Size

        val header = ByteBuffer.allocate(44)
        header.order(ByteOrder.LITTLE_ENDIAN)
        header.put('R'.code.toByte())
        header.put('I'.code.toByte())
        header.put('F'.code.toByte())
        header.put('F'.code.toByte())
        header.putInt(myChunkSize)
        header.put('W'.code.toByte())
        header.put('A'.code.toByte())
        header.put('V'.code.toByte())
        header.put('E'.code.toByte())
        header.put('f'.code.toByte())
        header.put('m'.code.toByte())
        header.put('t'.code.toByte())
        header.put(' '.code.toByte())
        header.putInt(mySubChunk1Size)
        header.putShort(myFormat)
        header.putShort(channels)
        header.putInt(sampleRate)
        header.putInt(myByteRate)
        header.putShort(myBlockAlign)
        header.putShort(bitsPerSample)
        header.put('d'.code.toByte())
        header.put('a'.code.toByte())
        header.put('t'.code.toByte())
        header.put('a'.code.toByte())
        header.putInt(myDataSize)

        output.write {
            it.write(header.array())
            it.write(audioData)
        }
    }

    companion object {
        private fun wavOutputForSystem(context: Context): WavOutput {
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                WavOutput.MediaCollection(context, "audio_$timestamp.wav")
            } else {
                WavOutput.WavFile(
                    File(
                        context.getExternalFilesDir(Environment.DIRECTORY_PODCASTS),
                        "audio_$timestamp.wav"
                    )
                )
            }
        }
    }
}

sealed interface WavOutput {
    fun write(consumer: Consumer<OutputStream>)
    class MediaCollection(val context: Context, val name: String) : WavOutput {
        override fun write(consumer: Consumer<OutputStream>) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                return
            }
            val resolver = context.contentResolver
            val songDetails = ContentValues().apply {
                put(MediaStore.Audio.Media.DISPLAY_NAME, name)
                put(MediaStore.Audio.Media.IS_PENDING, 1)
            }
            val audioCollection =
                MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            val songContentUri = resolver.insert(audioCollection, songDetails) ?: return

            resolver.openFileDescriptor(songContentUri, "w", null)?.use { pfd ->
                val fileOutputStream =
                    BufferedOutputStream(FileOutputStream(pfd.fileDescriptor), 16000)
                consumer.accept(fileOutputStream)
                fileOutputStream.flush()
                fileOutputStream.close()
                Timber.i("Audio is saved to $name")
            }

            songDetails.clear()
            songDetails.put(MediaStore.Audio.Media.IS_PENDING, 0)
            resolver.update(songContentUri, songDetails, null, null)
        }
    }

    class WavFile(val file: File) : WavOutput {
        override fun write(consumer: Consumer<OutputStream>) {
            BufferedOutputStream(FileOutputStream(file), 16000).use {
                consumer.accept(it)
                it.flush()
            }
        }
    }
}