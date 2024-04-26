package com.willeypianotuning.toneanalyzer.ui.splash.start

import com.willeypianotuning.toneanalyzer.store.db.tunings.PianoTuning

sealed class StartWorkState {
    object NotSet : StartWorkState()
    class TuningSelected(val tuning: PianoTuning) : StartWorkState()
}
