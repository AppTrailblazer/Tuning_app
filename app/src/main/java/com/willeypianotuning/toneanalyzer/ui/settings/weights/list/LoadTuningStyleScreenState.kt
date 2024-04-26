package com.willeypianotuning.toneanalyzer.ui.settings.weights.list

import com.willeypianotuning.toneanalyzer.store.db.tuning_styles.TuningStyle

sealed class LoadTuningStyleScreenState {
    object Loading : LoadTuningStyleScreenState()
    class Success(val styles: List<TuningStyle>) : LoadTuningStyleScreenState()
    class Error(val error: Throwable) : LoadTuningStyleScreenState()
}