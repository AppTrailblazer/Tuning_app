package com.willeypianotuning.toneanalyzer.utils

import android.media.AudioFormat
import android.media.AudioRecord
import android.os.Build
import androidx.annotation.WorkerThread
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.willeypianotuning.toneanalyzer.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

object Hardware {
    // set channel config to mono
    private const val channelConfig = AudioFormat.CHANNEL_IN_MONO

    //	set encoding to 16 bit, 8bit is unsupported both on HTC Wilfire S and emulator
    private const val audioFormat = AudioFormat.ENCODING_PCM_16BIT
    private val supportedSampleRates = intArrayOf(16000, 48000, 44100)
    private val minBufSizeBytes = intArrayOf(2048, 4096, 4096)

    /**
     * Sometimes `AudioRecord.getMinBufferSize` just hangs
     * Faced on KitKat and Lollipop.
     * Not sure but might be related either with the other app recording data
     * or incorrectly release audio record
     */
    @WorkerThread
    fun audioRecordingParams(): AudioRecordingParams? {
        for (i in supportedSampleRates.indices) {
            val minSize =
                AudioRecord.getMinBufferSize(supportedSampleRates[i], channelConfig, audioFormat)
            if (minSize < 0) {
                continue
            }
            val bufSizeBytes = maxOf(minSize, minBufSizeBytes[i])
            return AudioRecordingParams(
                channelConfig,
                audioFormat,
                supportedSampleRates[i],
                bufSizeBytes,
                minSize
            )
        }
        return null
    }

    fun minBufferSizeForSampleRate(sampleRate: Int): Int {
        var index = -1
        for (i in supportedSampleRates.indices) {
            if (supportedSampleRates[i] == sampleRate) {
                index = i
                break
            }
        }
        return if (index != -1) {
            minBufSizeBytes[index]
        } else {
            val requirement = sampleRate / 16
            for (size in minBufSizeBytes) {
                if (size > requirement) {
                    return size
                }
            }
            2048
        }
    }

    private fun printHardwareSupportedSampleRates() {
        val rates = allSupportedSampleRates
        if (rates.isEmpty()) {
            Timber.d("Hardware has no supported sampling rates")
            return
        }
        for (rate in rates) {
            Timber.d("Hardware supports %d sample rate", rate)
        }
    }

    /*
     * Valid Audio Sample rates
     *
     * @see <a href="http://en.wikipedia.org/wiki/Sampling_%28signal_processing%29">Wikipedia</a>
     */
    private val allSupportedSampleRates: List<Int>
        get() {
            val validSampleRates = intArrayOf(
                8000, 11025, 16000, 22050,
                32000, 37800, 44056, 44100, 47250, 48000, 50000, 50400, 88200,
                96000, 176400, 192000, 352800, 2822400, 5644800
            )
            return validSampleRates.filter {
                AudioRecord.getMinBufferSize(it, channelConfig, audioFormat) > 0
            }
        }

    @WorkerThread
    @JvmStatic
    fun isAudioHardwareSupported(): Boolean {
        val params = audioRecordingParams()
        if (params == null) {
            Timber.d("None of the app supported hardware rates is supported by the hardware.")
            printHardwareSupportedSampleRates()
        }
        return params != null
    }

    @JvmStatic
    fun checkAudioHardwareAsync(activity: AppCompatActivity) {
        activity.lifecycleScope.launch {
            kotlin.runCatching {
                val supported = withContext(Dispatchers.IO) {
                    isAudioHardwareSupported()
                }
                withContext(Dispatchers.Main) {
                    if (!supported) {
                        AlertDialog.Builder(activity)
                            .setTitle(R.string.app_name)
                            .setMessage(activity.getString(R.string.error_device_not_supported))
                            .setCancelable(false)
                            .setPositiveButton(R.string.action_ok) { _, _ -> activity.finish() }
                            .create()
                            .show()
                    }
                }
            }.onFailure {
                Timber.e(it, "Cannot check audio hardware")
            }
        }
    }

    val deviceName: String
        get() {
            val manufacturer = Build.MANUFACTURER
            val model = Build.MODEL
            return if (model.startsWith(manufacturer)) {
                model
            } else {
                "$manufacturer $model"
            }
        }

    data class AudioRecordingParams(
        val channelConfig: Int,
        val audioFormat: Int,
        val sampleRate: Int,
        val bufferSize: Int,
        val reportedMinBufferSize: Int
    )
}