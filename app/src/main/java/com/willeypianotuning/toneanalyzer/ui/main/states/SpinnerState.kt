package com.willeypianotuning.toneanalyzer.ui.main.states

import com.willeypianotuning.toneanalyzer.spinners.Spinner
import com.willeypianotuning.toneanalyzer.ui.views.RingView
import timber.log.Timber

data class SpinnersState(
    val note: Int,
    val spinners: List<SpinnerState>
) {
    fun applyTo(spinnerViews: List<RingView>) {
        // update spinner rotation and label
        for (i in spinnerViews.indices) {
            var spinnerViewParametersChanged = false
            if (spinners[i].enabled) {
                val ringDivision = spinners[i].divisions
                val ringPhase =
                    if (spinners[i].phase >= 0.0f) spinners[i].phase else spinnerViews[i].getRingPhase()
                if (ringDivision != spinnerViews[i].getNumberOfDashes()
                    || spinnerViews[i].getRingPhase() != ringPhase
                ) {
                    spinnerViews[i].setNumberOfDashesAndRingPhase(ringDivision, ringPhase)
                    spinnerViewParametersChanged = true
                }
            }
            val visible =
                spinners[i].enabled && (spinners[i].phase >= 0.0f || (spinners[i].phase == Spinner.EMPTY && spinnerViews[i].isShowRing))
            if (spinners[i].enabled && spinnerViews[i].isShowRing) {
                Timber.tag("SpinnerHandler").v("Phase: %f", spinners[i].phase)
            }
            if (spinnerViews[i].label != spinners[i].label) {
                spinnerViews[i].label = spinners[i].label
                spinnerViewParametersChanged = true
            }
            if (spinnerViews[i].isShowRing != visible) {
                spinnerViews[i].isShowRing = visible
                spinnerViewParametersChanged = true
            }
            if (spinnerViews[i].ringAlpha != spinners[i].alpha && spinners[i].phase >= 0.0f) {
                spinnerViews[i].ringAlpha = spinners[i].alpha
                spinnerViewParametersChanged = true
            }
            if (spinnerViewParametersChanged) {
                // spinner view parameters changed, we need to redraw it
                spinnerViews[i].invalidate()
            }
        }
    }
}

data class SpinnerState(
    val phase: Float = Spinner.INACTIVE,
    val alpha: Float = 0f,
    val label: String = "",
    val divisions: Int = 0,
    val enabled: Boolean = false
)
