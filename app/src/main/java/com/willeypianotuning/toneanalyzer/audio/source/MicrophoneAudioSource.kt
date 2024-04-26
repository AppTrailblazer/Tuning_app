package com.willeypianotuning.toneanalyzer.audio.source

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioManager
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Build
import com.willeypianotuning.toneanalyzer.AppSettings
import com.willeypianotuning.toneanalyzer.audio.AudioInputType
import com.willeypianotuning.toneanalyzer.audio.source.util.inputDevicesFlow
import com.willeypianotuning.toneanalyzer.audio.source.util.isBluetoothMic
import com.willeypianotuning.toneanalyzer.audio.source.util.isBuiltInMic
import com.willeypianotuning.toneanalyzer.audio.source.util.isExternalMic
import com.willeypianotuning.toneanalyzer.audio.source.util.toLogString
import com.willeypianotuning.toneanalyzer.utils.Hardware
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject


private const val TAG = "MicrophoneAudioSource"

/**
 * This class implements audio source which provides real time audio data from microphone
 */
class MicrophoneAudioSource @Inject constructor(
    @ApplicationContext private val context: Context,
    private val appSettings: AppSettings,
) : AudioSource {

    private var isRecordingEnabled = false

    private var _samplingRate = 16000
    override val samplingRate: Int get() = _samplingRate

    private var _bufSizeSamples: Int = 0
    override val bufferSizeSamples: Int get() = _bufSizeSamples

    private val audioManager: AudioManager by lazy { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }

    @Volatile
    private var audioRecord: AudioRecord? = null

    private var monitoringScope: CoroutineScope? = null

    private fun AudioRecord.logActiveMicrophones() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            activeMicrophones?.forEach {
                Timber.tag(TAG).d("activeMicrophone: id=${it.id} type=${it.type}")
            }
        }
    }

    private fun AudioRecord.setUserPreferredDevice() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val devices = audioManager.getDevices(AudioManager.GET_DEVICES_INPUTS)
            devices.forEach {
                Timber.tag(TAG).d("Input Device: ${it.toLogString()}")
            }
            Timber.tag(TAG).d("Preferred device settings: ${appSettings.preferredAudioInput}")
            val builtInMic = devices.firstOrNull { it.isBuiltInMic() }
            val externalMic = devices.firstOrNull { it.isExternalMic() }
            val bluetoothMic = devices.firstOrNull { it.isBluetoothMic() }
            val newPreferredDevice = when (appSettings.preferredAudioInput.type) {
                AudioInputType.BUILT_IN_MIC -> builtInMic
                AudioInputType.EXTERNAL_MIC -> if (appSettings.preferredAudioInput.allowBluetooth) {
                    externalMic ?: bluetoothMic ?: builtInMic
                } else {
                    externalMic ?: builtInMic
                }

                else -> null
            }

            Timber.tag(TAG)
                .i("Setting preferred device to: ${newPreferredDevice?.toLogString()}")
            @Suppress("UsePropertyAccessSyntax")
            if (!setPreferredDevice(newPreferredDevice)) {
                Timber.tag(TAG).w("Failed to set preferred device")
            }
        }
    }

    @SuppressLint("MissingPermission")
    override fun start(): Boolean {
        val params = Hardware.audioRecordingParams()
        if (params == null) {
            Timber.e(
                IllegalStateException("None of the minBufferSize is valid for this hardware"),
                "Cannot initiate recording"
            )
            isRecordingEnabled = false
            return false
        }

        Timber.tag(TAG).d("AudioRecord params: $params")

        _samplingRate = params.sampleRate
        _bufSizeSamples = params.bufferSize / 2

        // initialize audio recorder
        val record = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            _samplingRate,
            params.channelConfig,
            params.audioFormat,
            params.bufferSize
        )

        record.setUserPreferredDevice()
        record.logActiveMicrophones()

        // run fail action if initialization failed.
        if (record.state != AudioRecord.STATE_INITIALIZED) {
            Timber.tag(TAG).w("AudioRecord is not in the initialized state: ${record.state}")
            isRecordingEnabled = false
            return false
        }

        record.startRecording()
        if (record.recordingState != AudioRecord.RECORDSTATE_RECORDING) {
            Timber.tag(TAG).w("AudioRecord is not in the recording state: ${record.recordingState}")
            isRecordingEnabled = false
            return false
        }

        isRecordingEnabled = true
        audioRecord = record

        initMonitoring()

        return true
    }

    private fun initMonitoring() {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            scope.launch {
                audioManager.inputDevicesFlow().collect {
                    Timber.tag(TAG)
                        .d("======================= Input Devices Changed ==============================")
                    audioRecord?.setUserPreferredDevice()
                    audioRecord?.logActiveMicrophones()
                }
            }
        }
        monitoringScope = scope
    }

    private fun cancelMonitoring() {
        monitoringScope?.cancel()
        monitoringScope = null
    }

    override fun read(audioData: ShortArray): Int {
        if (!isRecordingEnabled) {
            return 0
        }
        val read = audioRecord!!.read(audioData, 0, audioData.size)
        if (read < 0) {
            Timber.tag(TAG).w("Failed to read data from microphone: %d", read)
            return 0
        }
        if (read < audioData.size) {
            Timber.tag(TAG).d("Read less audio data than expected: %d vs %d", read, audioData.size)
        }
        return read
    }

    override fun stop() {
        cancelMonitoring()

        isRecordingEnabled = false
        audioRecord?.stop()
        audioRecord?.release()
        audioRecord = null
    }

}