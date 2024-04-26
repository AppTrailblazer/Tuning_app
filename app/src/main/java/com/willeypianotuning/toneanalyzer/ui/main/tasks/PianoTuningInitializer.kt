package com.willeypianotuning.toneanalyzer.ui.main.tasks

import com.willeypianotuning.toneanalyzer.AppSettings
import com.willeypianotuning.toneanalyzer.ToneDetectorWrapper
import com.willeypianotuning.toneanalyzer.audio.TuningStyleHelper
import com.willeypianotuning.toneanalyzer.store.db.tunings.PianoTuning
import com.willeypianotuning.toneanalyzer.store.db.tunings.PianoTuningMeasurements
import javax.inject.Inject

class PianoTuningInitializer @Inject constructor(
    private val appSettings: AppSettings,
    private val tuningStyleHelper: TuningStyleHelper
) {
    fun initializeNewTuning(): PianoTuning {
        val tuning = PianoTuning()
        tuning.tuningStyle = tuningStyleHelper.getGlobalIntervalWeights()
        tuning.pitch = appSettings.globalPitchOffset

        ToneDetectorWrapper.newInstance().use {
            tuning.measurements = PianoTuningMeasurements(
                inharmonicity = it.inharmonicity,
                peakHeights = it.peaksHeight,
                harmonics = it.harmonics,
                bxFit = it.bxfit,
                delta = it.delta,
                fx = it.fx
            )
        }

        return tuning
    }
}