package com.willeypianotuning.toneanalyzer.generator

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import com.willeypianotuning.toneanalyzer.AppSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.concurrent.CancellationException
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import kotlin.coroutines.coroutineContext
import kotlin.math.abs

private const val MS_TO_NS: Long = 1000000

class TonePlayer @Inject constructor(
    private val toneGenerator: ToneGenerator,
    private val appSettings: AppSettings,
) {

    private val rampUpTimeMs: Long = 10
    private val rampDownTimeMs: Long = 100

    @Volatile
    private var playerJob: TonePlayerJob? = null

    private var playToneDurationMs: Long = 500
    private var playToneEnabled: Boolean = false
    private var currentNote: Int = 49

    var trebleBassOptions: TrebleBassOptions
        get() = toneGenerator.trebleBassOptions
        set(value) {
            toneGenerator.trebleBassOptions = value
            appSettings.trebleBassOptions = value
        }

    var volume: Float = 1.0f
        set(value) {
            field = value
            appSettings.tonePlayerVolume = value
            playerJob?.maxVolume = value
        }

    private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private var notePlayController: NotePlayController = AlwaysPlayController

    init {
        toneGenerator.audioFrequency = 16000
        volume = appSettings.tonePlayerVolume
        trebleBassOptions = appSettings.trebleBassOptions
    }

    fun isTonePlayEnabled(): Boolean = playToneEnabled && playToneDurationMs != 0L

    fun disableTonePlay() {
        stop()
        playToneEnabled = false
    }

    fun enableTonePlay() {
        playToneEnabled = true
        notePlayController = if (isEndlessPlaying()) {
            PlayOnceController()
        } else {
            AlwaysPlayController
        }
        play(currentNote)
    }

    fun setPlayToneDuration(durationMs: Long) {
        val wasPlayingEndlessly = isEndlessPlaying()
        playToneDurationMs = durationMs
        notePlayController = AlwaysPlayController
        if (wasPlayingEndlessly) {
            stop()
        }
    }

    fun isEndlessPlaying(): Boolean {
        return playToneDurationMs == -1L
    }

    fun setPlayToneEndlessly() {
        playToneDurationMs = -1
        notePlayController = PlayOnceController()
    }

    fun setCurrentNote(note: Int) {
        if (currentNote == note) {
            return
        }
        val previousNote = currentNote
        currentNote = note
        if (notePlayController.shouldPlay(previousNote, note)) {
            play(note)
            notePlayController.onNotePlayed(previousNote, note)
        } else {
            if (playerJob != null) {
                Timber.d("Already playing")
                stop(immediately = true)
            }
        }
    }

    @Synchronized
    private fun play(note: Int) {
        if (!isTonePlayEnabled()) {
            return
        }

        if (playerJob != null) {
            Timber.d("Already playing")
            stop(immediately = true)
        }

        playerJob = TonePlayerJob(
            toneGenerator,
            note - 1,
            rampUpTimeMs,
            rampDownTimeMs,
            playToneDurationMs.takeUnless { it == -1L } ?: 60_000L,
        ).apply {
            play(coroutineScope)
        }
    }

    /**
     * Stops the tone player
     * @param immediately - if true, tone playing stops immediately, otherwise it will fade out
     */
    @Synchronized
    private fun stop(immediately: Boolean = false) {
        Timber.d("Stopping tone player")
        playerJob?.stop(immediately)
        playerJob = null
    }
}

private class TonePlayerJob(
    private val toneGenerator: ToneGenerator,
    private val noteZeroIndexed: Int,
    private val rampUpTimeMs: Long,
    private val rampDownTimeMs: Long,
    private val durationMs: Long,
) {
    private val audioAttributes =
        AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME).build()

    private val audioFormat: AudioFormat =
        AudioFormat.Builder().setEncoding(AudioFormat.ENCODING_PCM_16BIT)
            .setSampleRate(toneGenerator.audioFrequency)
            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build()

    @Volatile
    private var playerJob: Job? = null

    @Volatile
    private var outputVolume: Float = 0f
        set(value) {
            field = value
            val outputVolume = minOf(value, maxVolume)
            audioTrack?.setVolume(outputVolume)
            Timber.v("Setting volume to $outputVolume")
        }

    @Volatile
    var maxVolume: Float = 1.0f
        set(value) {
            field = value
            val outputVolume = minOf(outputVolume, value)
            audioTrack?.setVolume(outputVolume)
            Timber.v("Setting volume to $outputVolume")
        }

    @Volatile
    private var audioTrack: AudioTrack? = null

    private val stopped = AtomicBoolean(false)

    init {
        assert(durationMs != -1L || durationMs >= rampUpTimeMs + rampDownTimeMs) { "Duration should be larger than sum of rampUpTime and rampDownTime" }
    }

    private suspend fun rampUpVolume(
        durationMs: Long,
        from: Float = 0.0f,
        to: Float = 1.0f,
        steps: Int = 10,
    ) {
        Timber.v("Ramping up volume")
        val startTimeNs = System.nanoTime()
        val durationNs = durationMs * MS_TO_NS
        val waitStepNs = maxOf(1, durationMs / steps)
        var dt: Long
        outputVolume = from
        do {
            coroutineContext.ensureActive()
            delay(waitStepNs)
            dt = System.nanoTime() - startTimeNs
            val volume = minOf(from + (to - from) * dt.toFloat() / durationNs, to)
            outputVolume = volume
            if (volume >= to) {
                return
            }
        } while (dt < durationNs && !stopped.get())
    }

    private fun rampDownVolume(
        durationMs: Long,
        from: Float = 1.0f,
        to: Float = 0.0f,
        steps: Int = 10,
    ) {
        Timber.v("Ramping down volume")
        val startTimeNs = System.nanoTime()
        val durationNs = durationMs * MS_TO_NS
        val waitStepMs = maxOf(1, durationMs / steps)
        var dt: Long
        outputVolume = from
        do {
            Thread.sleep(waitStepMs)
            dt = System.nanoTime() - startTimeNs
            val volume = maxOf(from - (from - to) * dt.toFloat() / durationNs, to)
            outputVolume = volume
            if (volume <= to) {
                return
            }
        } while (dt < durationNs)
    }

    fun play(scope: CoroutineScope) {
        stopped.set(false)
        playerJob = scope.launch {
            coroutineContext.ensureActive()

            toneGenerator.initTone(noteZeroIndexed)
            val initialBuffer = ShortArray(toneGenerator.audioFrequency)
            val updateBuffer = ShortArray(toneGenerator.audioFrequency / 250)
            val audio = AudioTrack(
                audioAttributes,
                audioFormat,
                Short.SIZE_BYTES * toneGenerator.audioFrequency,
                AudioTrack.MODE_STREAM,
                AudioManager.AUDIO_SESSION_ID_GENERATE
            )
            audioTrack = audio

            try {
                coroutineContext.ensureActive()
                toneGenerator.generateTone(initialBuffer)

                Timber.v("Writing initial ${initialBuffer.size} samples")
                coroutineContext.ensureActive()
                audio.write(initialBuffer, 0, initialBuffer.size)

                coroutineContext.ensureActive()

                audio.setVolume(0f)
                val startTimeNs = System.nanoTime()
                audio.play()
                try {
                    rampUpVolume(rampUpTimeMs)
                    while (!stopped.get()) {
                        coroutineContext.ensureActive()
                        toneGenerator.generateTone(updateBuffer)
                        coroutineContext.ensureActive()
                        audio.write(updateBuffer, 0, updateBuffer.size)

                        if (durationMs > -1L) {
                            val remainingNs: Long =
                                durationMs * MS_TO_NS - (System.nanoTime() - startTimeNs + rampDownTimeMs * MS_TO_NS)
                            if (remainingNs <= 0) {
                                break
                            }
                        }
                    }
                } finally {
                    if (outputVolume > 0f) {
                        rampDownVolume(rampDownTimeMs, from = outputVolume)
                    }
                    Timber.v("Stopping audio")
                    audio.stop()
                }
            } finally {
                Timber.v("Releasing audio")
                audio.release()
                audioTrack = null
            }
        }
    }

    fun stop(immediately: Boolean) {
        stopped.set(true)
        if (immediately) {
            outputVolume = 0f
        }
        playerJob?.cancel(CancellationException("Stopping tone player"))
        playerJob = null
    }
}

interface NotePlayController {
    fun shouldPlay(currentNote: Int, newNote: Int): Boolean
    fun onNotePlayed(currentNote: Int, newNote: Int)
}

private object AlwaysPlayController : NotePlayController {
    override fun shouldPlay(currentNote: Int, newNote: Int) = true
    override fun onNotePlayed(currentNote: Int, newNote: Int) {}
}

private class PlayOnceController : NotePlayController {
    private val burnedNotes = ArrayList<Int>(LAST_PLAYED_NOTES_COUNT)

    override fun shouldPlay(currentNote: Int, newNote: Int): Boolean {
        if (burnedNotes.contains(newNote)) {
            Timber.d("Not playing the note $newNote, was played recently")
            return false
        }
        Timber.d("Playing the note $newNote")
        return true
    }

    override fun onNotePlayed(currentNote: Int, newNote: Int) {
        if (abs(currentNote - newNote) > 1) {
            Timber.d("Not playing the note $newNote, non-consecutive (previous $currentNote)")
            return
        }

        val index = burnedNotes.indexOf(newNote)
        if (index != -1) {
            // Move note to the end of the queue
            burnedNotes.removeAt(index)
            burnedNotes.add(newNote)
            return
        }
        if (burnedNotes.size >= LAST_PLAYED_NOTES_COUNT) {
            burnedNotes.removeFirst()
        }
        burnedNotes.add(newNote)
    }

    override fun toString(): String {
        return "PlayOnceController(burnedNotes=$burnedNotes)"
    }

    companion object {
        private const val LAST_PLAYED_NOTES_COUNT = 10
    }
}