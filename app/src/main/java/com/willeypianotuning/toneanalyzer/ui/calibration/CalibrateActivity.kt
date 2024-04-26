package com.willeypianotuning.toneanalyzer.ui.calibration

import android.app.AlertDialog
import android.content.DialogInterface
import android.os.Bundle
import android.text.InputType
import android.util.DisplayMetrics
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import android.widget.Toast
import com.willeypianotuning.toneanalyzer.R
import com.willeypianotuning.toneanalyzer.ToneDetectorWrapper
import com.willeypianotuning.toneanalyzer.audio.AudioRecorder
import com.willeypianotuning.toneanalyzer.databinding.ActivityCalibrateBinding
import com.willeypianotuning.toneanalyzer.extensions.keepScreenOn
import com.willeypianotuning.toneanalyzer.extensions.round
import com.willeypianotuning.toneanalyzer.spinners.CalibrationSpinnerHandler
import com.willeypianotuning.toneanalyzer.spinners.Spinners
import com.willeypianotuning.toneanalyzer.ui.commons.BaseActivity
import com.willeypianotuning.toneanalyzer.ui.main.MainUiHelper
import com.willeypianotuning.toneanalyzer.utils.decimal.AppInputDecimalFormat
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import java.util.*
import javax.inject.Inject
import kotlin.math.pow
import kotlin.math.roundToInt

@AndroidEntryPoint
class CalibrateActivity : BaseActivity() {
    companion object {
        private const val MAX_OFFSET = 10.0f
        private const val PRECISE_STEP = 0.01f
    }

    private var spinnerHandler: CalibrationSpinnerHandler? = null

    @Inject
    lateinit var analyzerWrapper: ToneDetectorWrapper

    @Inject
    lateinit var audioRecorder: AudioRecorder

    @Inject
    lateinit var spinners: Spinners

    private lateinit var binding: ActivityCalibrateBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCalibrateBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupToolbar()
        setTitle(R.string.activity_calibrate_title)

        binding.calibrationReferenceButton.setOnClickListener { onReferenceClick() }
        binding.calibrationMinusButton.setOnClickListener { changePitchOffset(-PRECISE_STEP) }
        binding.calibrationPlusButton.setOnClickListener { changePitchOffset(PRECISE_STEP) }
        binding.calibrationSlider.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    val pitchOffset = (progress * (2.0 * MAX_OFFSET) / 100.0 - MAX_OFFSET).toFloat()
                    Timber.d("seek off: %s", pitchOffset)
                    appSettings.pitchOffset = pitchOffset
                    updateLabel()
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {}
        })
        // Reset pitch offset to 0 when the calibrate label is clicked
        binding.calibrationLabel.setOnClickListener {
            appSettings.pitchOffset = 0f
            updateLabel()
            updateSlider()
        }
        binding.topPanel.post { MainUiHelper.prepareTopPanelImage(binding.topPanel) }
    }

    public override fun onResume() {
        super.onResume()
        try {
            keepScreenOn(appSettings.preventSleep)
            Timber.d("Start processing")
            val freqFormatted = AppInputDecimalFormat.formatDouble(
                appSettings.pitchOffsetTargetFreq.toDouble().round(1)
            )
            binding.calibrationReferenceButton.text = getString(R.string.hz_format, freqFormatted)
            updateSlider()
            updateLabel()
            spinners.processSpinners = true
            audioRecorder.start()
            val rings = arrayOf(binding.ring4)
            for (ring in rings) {
                ring.ringAlpha = 1.0f
            }
            spinnerHandler = CalibrationSpinnerHandler(spinners, rings)
            spinnerHandler?.start()
        } catch (e: Exception) {
            Timber.e(e, "Exception in onResume")
        }
    }

    // this method called before closing the app
    override fun onPause() {
        super.onPause()
        spinnerHandler?.cancel()
        audioRecorder.stop()
        spinners.processSpinners = false
    }

    private fun onReferenceClick() {
        val layout = LinearLayout(this)
        val dpToPx = resources.displayMetrics.densityDpi.toFloat() / DisplayMetrics.DENSITY_DEFAULT
        layout.setPadding(
            (16 * dpToPx).toInt(),
            (4 * dpToPx).toInt(),
            (16 * dpToPx).toInt(),
            (4 * dpToPx).toInt()
        )
        val inputField = EditText(this).apply {
            inputType = InputType.TYPE_CLASS_TEXT
            isSingleLine = true
            imeOptions = EditorInfo.IME_ACTION_DONE
            val freqFormatted = AppInputDecimalFormat.formatDouble(
                appSettings.pitchOffsetTargetFreq.toDouble().round(1)
            )
            setText(freqFormatted)
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }
        layout.addView(inputField)

        val builder = AlertDialog.Builder(this)
            .setTitle(getString(R.string.dialog_reference_frequency_title))
            .setMessage(getString(R.string.dialog_reference_frequency_message))
            .setView(layout)
        builder.setPositiveButton(getString(R.string.action_ok)) { _: DialogInterface?, _: Int ->
            val value = AppInputDecimalFormat.parseDouble(inputField.text.toString())?.toFloat()
            if (value != null) {
                if (value > 27.5 && value < 4200) {
                    appSettings.pitchOffsetTargetFreq = value.toFloat()
                    val freqFormatted = AppInputDecimalFormat.formatDouble(
                        appSettings.pitchOffsetTargetFreq.toDouble().round(1)
                    )
                    binding.calibrationReferenceButton.text =
                        getString(R.string.hz_format, freqFormatted)
                }
            } else {
                Toast.makeText(
                    this@CalibrateActivity,
                    getString(R.string.dialog_reference_frequency_error_only_digits_allowed),
                    Toast.LENGTH_LONG
                ).show()
            }
        }
        builder.setNegativeButton(getString(R.string.action_cancel)) { dialog: DialogInterface, _: Int -> dialog.cancel() }
        builder.show()
    }

    private fun changePitchOffset(step: Float) {
        val newValue = minOf(maxOf(appSettings.pitchOffset + step, -MAX_OFFSET), MAX_OFFSET)
        Timber.d("off: ${appSettings.pitchOffset} ps: $PRECISE_STEP nv: $newValue")
        appSettings.pitchOffset = newValue
        updateLabel()
        updateSlider()
    }

    private fun updateLabel() {
        val numf = appSettings.pitchOffset.toDouble().round(2)
        binding.calibrationLabel.text =
            String.format(Locale.getDefault(), "%.2f %s", numf, getString(R.string.dimension_cents))
        val offsetFreq = 2.0.pow(appSettings.pitchOffset / 1200.0)
        analyzerWrapper.calibrationFactor = offsetFreq
        Timber.d("onset of: %s", offsetFreq)
    }

    private fun updateSlider() {
        val initialValue =
            ((appSettings.pitchOffset + MAX_OFFSET) * 100.0 / (2.0 * MAX_OFFSET)).roundToInt()
                .toDouble()
        binding.calibrationSlider.progress = initialValue.toInt()
    }
}