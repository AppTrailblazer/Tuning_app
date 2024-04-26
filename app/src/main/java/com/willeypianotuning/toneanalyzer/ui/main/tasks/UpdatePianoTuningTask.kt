package com.willeypianotuning.toneanalyzer.ui.main.tasks

import com.willeypianotuning.toneanalyzer.ToneDetectorWrapper
import com.willeypianotuning.toneanalyzer.audio.AudioRecorder
import com.willeypianotuning.toneanalyzer.audio.enums.PitchRaiseMode
import com.willeypianotuning.toneanalyzer.store.db.tunings.PianoTuningMeasurements
import javax.inject.Inject

/**
 * Reads measurement data from Native Tone Detector and writes it into the tuning
 */
class UpdatePianoTuningTask @Inject constructor(
    private val audioRecorder: AudioRecorder,
    private val toneAnalyzer: ToneDetectorWrapper
) {
    var inharmonicityAndPeakHeightsLocked: Boolean = false

    fun run() {
        val tuning = audioRecorder.tuning ?: return

        val bxFit = toneAnalyzer.bxfit
        val delta = toneAnalyzer.delta

        val inharmonicity =
            if (inharmonicityAndPeakHeightsLocked) tuning.measurements.inharmonicity else toneAnalyzer.inharmonicity
        val peakHeights =
            if (inharmonicityAndPeakHeightsLocked) tuning.measurements.peakHeights else toneAnalyzer.peaksHeight

        var fx = tuning.measurements.fx
        var harmonics = tuning.measurements.harmonics
        // Do not save fx and harmonics during pitch raise measurement
        synchronized(toneAnalyzer.pitchRaiseModeLock) {
            if (toneAnalyzer.pitchRaiseMode != PitchRaiseMode.MEASUREMENT) {
                fx = toneAnalyzer.fx
                harmonics = toneAnalyzer.harmonics
            }
        }

        tuning.measurements = PianoTuningMeasurements(
            inharmonicity = inharmonicity,
            peakHeights = peakHeights,
            bxFit = bxFit,
            delta = delta,
            fx = fx,
            harmonics = harmonics
        )
    }

}