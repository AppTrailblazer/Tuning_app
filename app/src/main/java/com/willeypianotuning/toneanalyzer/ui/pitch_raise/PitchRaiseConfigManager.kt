package com.willeypianotuning.toneanalyzer.ui.pitch_raise

import android.app.Activity
import android.content.DialogInterface
import android.content.Intent
import com.willeypianotuning.toneanalyzer.AppSettings
import com.willeypianotuning.toneanalyzer.R
import com.willeypianotuning.toneanalyzer.audio.PitchRaiseOptions
import com.willeypianotuning.toneanalyzer.audio.enums.PitchRaiseDialog
import com.willeypianotuning.toneanalyzer.billing.PurchaseStore
import com.willeypianotuning.toneanalyzer.store.db.tunings.PianoTuning
import com.willeypianotuning.toneanalyzer.ui.upgrade.UpgradeActivity
import dagger.hilt.android.scopes.ActivityRetainedScoped
import timber.log.Timber
import javax.inject.Inject

@ActivityRetainedScoped
class PitchRaiseConfigManager @Inject constructor(
    private val appSettings: AppSettings,
    private val purchaseStore: PurchaseStore
) {
    private var pitchRaiseOptions: PitchRaiseOptions? = null
    private var pitchRaiseDialog: Int = PitchRaiseDialog.NONE

    var onPitchRaiseConfigReadyListener: OnPitchRaiseConfigReadyListener? = null

    private inline val isPro: Boolean get() = purchaseStore.isPro

    fun startNewConfiguration(activity: Activity, currentTuning: PianoTuning) {
        onPitchRaiseConfigurationCancelled()
        pitchRaiseOptions = PitchRaiseOptions(currentTuning)
        openPianoTypeDialog(activity)
    }

    fun resumeConfiguration(activity: Activity) {
        pitchRaiseOptions ?: return

        when (pitchRaiseDialog) {
            PitchRaiseDialog.PIANO_TYPE -> openPianoTypeDialog(activity)
            PitchRaiseDialog.LOWEST_UNWOUND -> openLowestUnwoundDialog(activity)
            PitchRaiseDialog.HIGHEST_MIDSECTION -> openHighestMidsectionDialog(activity)
            PitchRaiseDialog.NOTES -> openPitchRaiseDialog(activity)
        }
    }

    fun reset() {
        pitchRaiseDialog = PitchRaiseDialog.NONE
        pitchRaiseOptions = null
    }

    private fun onPitchRaiseConfigurationCancelled() {
        val pitchRaiseCancelled = pitchRaiseDialog != PitchRaiseDialog.NONE
        reset()
        if (pitchRaiseCancelled) {
            onPitchRaiseConfigReadyListener?.onPitchRaiseCancelled()
        }
    }

    private fun openPianoTypeDialog(activity: Activity) {
        if (!isPro) {
            val intent = Intent(activity, UpgradeActivity::class.java)
            activity.startActivity(intent)
            return
        }

        Timber.d("Showing piano type selection")
        val options: PitchRaiseOptions = pitchRaiseOptions ?: return
        pitchRaiseDialog = PitchRaiseDialog.PIANO_TYPE
        val dialog = PianoTypeDialog(
            activity,
            options.pianoType
        )
        dialog.onPianoTypeChangeListener = PianoTypeDialog.OnPianoTypeChangeListener { pianoType ->
            options.pianoType = pianoType
            openLowestUnwoundDialog(activity)
        }
        dialog.okButtonText = activity.getString(R.string.action_continue)
        dialog.setOnCancelListener { onPitchRaiseConfigurationCancelled() }
        dialog.show()
    }

    private fun openLowestUnwoundDialog(activity: Activity) {
        Timber.d("Showing lowest unwound string selection")
        val options: PitchRaiseOptions = pitchRaiseOptions ?: return
        pitchRaiseDialog = PitchRaiseDialog.LOWEST_UNWOUND
        val dialog = LowestUnwoundDialog(
            activity,
            options,
            appSettings
        )
            .setOnPositiveClicked { _: DialogInterface?, _: Int ->
                openHighestMidsectionDialog(activity)
            }
        dialog.setOnCancelListener { onPitchRaiseConfigurationCancelled() }
        dialog.show()
    }

    private fun openHighestMidsectionDialog(activity: Activity) {
        Timber.d("Showing highest midsection string selection")
        val options: PitchRaiseOptions = pitchRaiseOptions ?: return
        pitchRaiseDialog = PitchRaiseDialog.HIGHEST_MIDSECTION
        val dialog = HighestMidsectionDialog(
            activity,
            options,
            appSettings
        )
            .setOnPositiveClicked { _: DialogInterface?, _: Int ->
                openPitchRaiseDialog(activity)
            }
        dialog.setOnCancelListener { onPitchRaiseConfigurationCancelled() }
        dialog.show()
    }

    private fun openPitchRaiseDialog(activity: Activity) {
        Timber.d("Showing pitch raise keys selection")
        val options: PitchRaiseOptions = pitchRaiseOptions ?: return
        pitchRaiseDialog = PitchRaiseDialog.NOTES
        val dialog = RaiseKeysDialog(
            activity,
            options,
            appSettings.lastPitchRaiseKeys
        )
        dialog.setOnPitchRaiseKeysSelected {
            if (options.notesToRaise.isNotEmpty()) {
                appSettings.lastPitchRaiseKeys = options.notesToRaise
                onPitchRaiseConfigReadyListener?.onPitchRaiseConfigReady(options)
                reset()
            } else {
                Timber.i("No pitch raise keys selected")
                onPitchRaiseConfigurationCancelled()
            }
            dialog.dismiss()
        }
        dialog.setOnCancelListener { onPitchRaiseConfigurationCancelled() }
        dialog.show()
    }

    interface OnPitchRaiseConfigReadyListener {
        fun onPitchRaiseConfigReady(config: PitchRaiseOptions)
        fun onPitchRaiseCancelled()
    }
}