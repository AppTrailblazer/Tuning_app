package com.willeypianotuning.toneanalyzer.audio

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.willeypianotuning.toneanalyzer.ToneDetectorWrapper
import com.willeypianotuning.toneanalyzer.audio.source.AudioSource
import com.willeypianotuning.toneanalyzer.billing.security.AntiTamper
import com.willeypianotuning.toneanalyzer.spinners.Spinners
import com.willeypianotuning.toneanalyzer.store.db.tunings.PianoTuning
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AudioRecorder @Inject constructor(
    private val audioSource: AudioSource,
    private val spinners: Spinners,
    private val analyzerWrapper: ToneDetectorWrapper,
    private val antiTamper: AntiTamper
) {
    private var recordingThread: NdkRecordingThread? = null

    private val _audioFrameProcessed = MutableLiveData<Long>()
    val audioFrameProcessed: LiveData<Long> get() = _audioFrameProcessed

    private val audioFrameProcessedCallback = NdkRecordingThread.OnAudioFrameProcessedCallback {
        _audioFrameProcessed.postValue(0L)
    }

    val sampleRate: Int get() = recordingThread?.samplingRate ?: 0

    var tuning: PianoTuning? = null

    fun start() {
        if (recordingThread != null) {
            stop()
        }
        Timber.d("Starting audio processing")
        recordingThread = NdkRecordingThread(
            spinners,
            audioSource,
            analyzerWrapper,
            antiTamper,
            audioFrameProcessedCallback
        ).also {
            it.start()
        }
    }

    fun pause() {
        recordingThread?.processingOn?.getAndSet(false)
    }

    fun resume() {
        recordingThread?.processingOn?.getAndSet(true)
    }

    fun stop() {
        Timber.d("Stopping audio processing")
        pause()
        try {
            recordingThread?.interrupt() // stop recording thread
            recordingThread?.join()
        } catch (e: InterruptedException) {
            Timber.e(e, "Cannot stop threads")
        }
        recordingThread = null
    }
}