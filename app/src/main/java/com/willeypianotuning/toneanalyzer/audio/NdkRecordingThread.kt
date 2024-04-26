package com.willeypianotuning.toneanalyzer.audio

import android.os.Process
import androidx.tracing.trace
import com.willeypianotuning.toneanalyzer.ToneDetectorWrapper
import com.willeypianotuning.toneanalyzer.audio.source.AudioSource
import com.willeypianotuning.toneanalyzer.audio.source.MicrophoneAudioSource
import com.willeypianotuning.toneanalyzer.billing.security.AntiTamper
import com.willeypianotuning.toneanalyzer.spinners.Spinners
import timber.log.Timber
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.ThreadFactory
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

class NdkRecordingThread(
    private val spinners: Spinners,
    private val audioSource: AudioSource,
    private val analyzerWrapper: ToneDetectorWrapper,
    private val antiTamper: AntiTamper,
    private var callback: OnAudioFrameProcessedCallback?
) : Thread("Audio") {

    @Volatile
    private var lastDetectNotesExecutionTimestamp: Long = 0

    val processingOn = AtomicBoolean(true)

    @Volatile
    private var destroyed = false
    val samplingRate: Int
        get() = audioSource.samplingRate

    // the thread start here (run method)
    override fun run() {
        Process.setThreadPriority(Process.THREAD_PRIORITY_AUDIO)

        // initializes audio source
        // here we find optimal parameters like buffer size and sample rate
        if (!audioSource.start()) {
            Timber.e("Cannot initialize recording")
            return
        }

        // we know the sample rate, so we can initialize spinners
        spinners.configure(audioSource.samplingRate)
        // we know the buffer size, we can create a buffer for audio data
        val readBuffer = ShortArray(audioSource.bufferSizeSamples)

        // initializes antihacking mechanism
        antiTamper.updateAppHackedState()

        Timber.d("SR: %s", audioSource.samplingRate)
        analyzerWrapper.setInputSamplingRate(audioSource.samplingRate.toDouble())

        // notes detection runs async
        // here we create an async executor which would run notes detection
        val executor = Executors.newSingleThreadExecutor(DetectNotesThreadFactory())

        // this is utility class which is used to compute execution time for audio processing
        val fastloopStats = RuntimeStats("Fastloop", 5)
        val detectNotesStats = RuntimeStats("DetectNotes", 5)
        val detectNotesRunnable = Runnable {
            trace("DetectNotes") {
                lastDetectNotesExecutionTimestamp = System.currentTimeMillis()
                detectNotesStats.start()
                val currentNote = analyzerWrapper.currentNote
                if (analyzerWrapper.detectNotes()) {
                    if (analyzerWrapper.currentNote != currentNote) {
                        antiTamper.onNoteAutomaticallyChanged()
                    }
                    detectNotesStats.stop()
                }
            }
        }
        var detectNotesFuture: Future<*>? = null

        // we repeatedly read audio data and process it
        // until the thread is interrupted (was asked to stop execution)
        while (!destroyed) {
            val samplesRead = audioSource.read(readBuffer)
            if (samplesRead <= 0) {
                // no data read
                // either we don't have permission
                // or audio has finished
                continue
            }
            fastloopStats.start()

            trace("fastloop") {
                // here we add read audio data to the native tone detector
                analyzerWrapper.addData(readBuffer, samplesRead)
                if (processingOn.get()) {
                    // lets save current note before audio data is processed
                    // we need it for antihacking stat
                    val currentNote = analyzerWrapper.currentNote

                    // fast loop
                    val processFrame = analyzerWrapper.processFrame()
                    // zero crossing
                    analyzerWrapper.processZeroCrossing()
                    if (processFrame) {
                        // update UI
                        callback?.onAudioFrameProcessed()
                    }

                    // update spinners (old code, only used during calibration)
                    spinners.processFrame(readBuffer)

                    // here we run notes detection (schedule execution on another thread)
                    val noteDetectionDone = detectNotesFuture?.isDone ?: true
                    if (noteDetectionDone && lastDetectNotesExecutionTimestamp < System.currentTimeMillis() - 1000 / DETECT_NOTES_MAX_EXECUTIONS_PER_SECOND) {
                        // only schedule next notes detection task if the previous one had finished
                        detectNotesFuture = executor.submit(detectNotesRunnable)
                    }

                    // if current note is has changed after processing audio data
                    // we notify antihacking tools
                    if (analyzerWrapper.currentNote != currentNote) {
                        antiTamper.onNoteAutomaticallyChanged()
                    }
                }
            }
            fastloopStats.stop()
        }

        // the audio processing is stopped here
        // running cleanup
        callback = null
        audioSource.stop()
        executor.shutdown()
    }

    override fun interrupt() {
        destroyed = true
        super.interrupt()
    }

    private class DetectNotesThreadFactory : ThreadFactory {
        override fun newThread(r: Runnable): Thread {
            val t = Thread(r)
            t.name = "NotesDetectionThread"
            t.priority = MIN_PRIORITY
            return t
        }
    }

    fun interface OnAudioFrameProcessedCallback {
        fun onAudioFrameProcessed()
    }

    companion object {
        private const val DETECT_NOTES_MAX_EXECUTIONS_PER_SECOND = 4
    }
}