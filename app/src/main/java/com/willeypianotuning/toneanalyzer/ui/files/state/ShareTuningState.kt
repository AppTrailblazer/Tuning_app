package com.willeypianotuning.toneanalyzer.ui.files.state

import com.willeypianotuning.toneanalyzer.ui.files.export_tunings.ExportLocation

sealed class ShareTuningState {
    object NotSet : ShareTuningState()
    object Loading : ShareTuningState()
    class TuningsSelected(val tuningIds: List<String>) : ShareTuningState()
    class Error(val error: Throwable) : ShareTuningState()
    class Success(val exportLocation: ExportLocation, val share: Boolean) : ShareTuningState()
}