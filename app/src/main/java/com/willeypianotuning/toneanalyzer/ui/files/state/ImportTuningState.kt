package com.willeypianotuning.toneanalyzer.ui.files.state

sealed class ImportTuningState {
    object NotSet : ImportTuningState()
    class Error(val error: Throwable) : ImportTuningState()
    object Loading : ImportTuningState()
    object Success : ImportTuningState()
}