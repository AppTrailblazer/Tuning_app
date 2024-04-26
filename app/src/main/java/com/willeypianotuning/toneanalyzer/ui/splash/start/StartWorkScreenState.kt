package com.willeypianotuning.toneanalyzer.ui.splash.start

import com.willeypianotuning.toneanalyzer.store.db.tunings.PianoTuning

sealed class StartWorkScreenState {
    object NotSet : StartWorkScreenState()
    object Loading : StartWorkScreenState()
    data class Error(val error: Throwable) : StartWorkScreenState()
    data class Success(val lastTuning: PianoTuning?, val isPro: Boolean) : StartWorkScreenState()
}