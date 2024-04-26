package com.willeypianotuning.toneanalyzer.ui.main.tasks

import android.annotation.SuppressLint
import com.willeypianotuning.toneanalyzer.audio.AudioRecorder
import com.willeypianotuning.toneanalyzer.store.PianoTuningDataStore
import com.willeypianotuning.toneanalyzer.store.db.tunings.PianoTuning
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import timber.log.Timber
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject

class AutomaticTuningSaver @Inject constructor(
    private val audioRecorder: AudioRecorder, private val dataStore: PianoTuningDataStore
) {
    private var saveTuningDisposable: Job? = null

    private val saverScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val automaticSaveTuningEnabled = AtomicBoolean(false)

    /**
     * Lock prevents updates of inharmonicity and peak heights
     */
    private val writingInProgress = AtomicBoolean(false)

    fun automaticSavingActive(): Boolean {
        return automaticSaveTuningEnabled.get()
    }

    fun setAutomaticSavingActive(automaticSavingActive: Boolean) {
        automaticSaveTuningEnabled.getAndSet(automaticSavingActive)
    }

    private fun tickerFlow(period: Long, initialDelay: Long = 0) = flow {
        delay(initialDelay)
        while (true) {
            emit(Unit)
            delay(period)
        }
    }

    fun start() {
        saveTuningDisposable?.cancel()
        saveTuningDisposable = tickerFlow(1000, 10000).onEach {
            if (automaticSaveTuningEnabled.get()) {
                saveTuning()
            }
        }.launchIn(saverScope)
    }

    fun stop() {
        saveTuningDisposable?.let {
            automaticSaveTuningEnabled.getAndSet(false)
            it.cancel()
        }
    }

    private suspend fun saveTuning() {
        val data = audioRecorder.tuning
        if (data == null) {
            Timber.w("No piano tuning available. Tuning Writing skipped")
            return
        }
        if (writingInProgress.get()) {
            Timber.v("Tuning Writing is in progress")
            return
        }
        writingInProgress.getAndSet(true)
        try {
            if (data.id == PianoTuning.NO_ID) {
                dataStore.addTuning(data)
            } else {
                dataStore.updateTuning(data)
            }
            Timber.tag("AutomaticTuningSaver")
                .v("Tuning data saved (${data.id}, ${data.make}, ${data.model}, ${data.name})")
        } catch (e: Exception) {
            Timber.e(e, "Cannot update tuning data")
        }
        writingInProgress.getAndSet(false)
    }

    @SuppressLint("CheckResult")
    fun saveTuningNowAsync() {
        saverScope.launch {
            kotlin.runCatching {
                saveTuning()
            }.onFailure {
                Timber.e(it, "Cannot write to file")
            }
        }
    }

}