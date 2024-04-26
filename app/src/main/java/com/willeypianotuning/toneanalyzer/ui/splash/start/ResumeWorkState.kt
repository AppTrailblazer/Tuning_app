package com.willeypianotuning.toneanalyzer.ui.splash.start

import com.willeypianotuning.toneanalyzer.audio.PitchRaiseOptions
import com.willeypianotuning.toneanalyzer.store.db.tunings.PianoTuning

sealed class ResumeWorkState {
    object NotSet : ResumeWorkState()
    object Loading : ResumeWorkState()
    class Error(val error: Throwable) : ResumeWorkState()
    class Success(val tuning: PianoTuning, val pitchRaiseOptions: PitchRaiseOptions?) :
        ResumeWorkState()
}