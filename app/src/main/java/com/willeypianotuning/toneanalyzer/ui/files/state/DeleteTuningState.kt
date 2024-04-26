package com.willeypianotuning.toneanalyzer.ui.files.state

sealed class DeleteTuningState {
    object NotSet : DeleteTuningState()
    class Error(val error: Throwable) : DeleteTuningState()
    object Loading : DeleteTuningState()
    object Success : DeleteTuningState()
}