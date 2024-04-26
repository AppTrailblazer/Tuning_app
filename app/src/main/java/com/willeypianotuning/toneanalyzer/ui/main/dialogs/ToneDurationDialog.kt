package com.willeypianotuning.toneanalyzer.ui.main.dialogs

import android.app.Activity
import android.app.Dialog
import android.content.DialogInterface
import android.content.res.Configuration
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.Window
import com.willeypianotuning.toneanalyzer.R
import com.willeypianotuning.toneanalyzer.databinding.DialogToneDurationBinding
import com.willeypianotuning.toneanalyzer.utils.DisplayUtils

class ToneDurationDialog(
    private val activity: Activity,
    var durationMs: Long,
    private var minDurationMs: Long = 1_000L,
    private var maxDurationMs: Long = 60_000L,
) : Dialog(activity), View.OnClickListener {
    private var onPositiveClicked: DialogInterface.OnClickListener? = null
    private lateinit var binding: DialogToneDurationBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)

        binding = DialogToneDurationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.minusButton.setOnClickListener {
            adjustDuration(-1000L)
        }
        binding.minusButton.onContinuousPressListener =
            ContinuousPressEmittingImageButton.OnContinuousPressListener {
                adjustDuration(-1000L)
            }
        binding.plusButton.setOnClickListener {
            adjustDuration(1000L)
        }
        binding.plusButton.onContinuousPressListener =
            ContinuousPressEmittingImageButton.OnContinuousPressListener {
                adjustDuration(1000L)
            }
        updateUi()

        binding.buttonPositive.setText(R.string.action_ok)
        binding.buttonPositive.setOnClickListener(this)
        val size = DisplayUtils.screenSize(activity)

        val orientation = activity.resources.configuration.orientation
        if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
            window?.setLayout((0.5 * size.x).toInt(), ViewGroup.LayoutParams.WRAP_CONTENT)
        } else {
            window?.setLayout((0.9 * size.x).toInt(), ViewGroup.LayoutParams.WRAP_CONTENT)
        }
    }

    private fun adjustDuration(delta: Long) {
        durationMs = maxOf(minOf(durationMs + delta, maxDurationMs), minDurationMs)
        updateUi()
    }

    private fun updateUi() {
        binding.durationText.text = formatToneDuration()
        binding.minusButton.isEnabled = durationMs > minDurationMs
        binding.plusButton.isEnabled = durationMs < maxDurationMs
    }

    private fun formatToneDuration(): String {
        return (durationMs / 1000).toString()
    }

    fun setOnPositiveClicked(onPositiveClicked: DialogInterface.OnClickListener?): ToneDurationDialog {
        this.onPositiveClicked = onPositiveClicked
        return this
    }

    override fun onClick(v: View) {
        when (v.id) {
            R.id.buttonPositive -> {
                onPositiveClicked?.onClick(this, 1)
                dismiss()
            }
        }
    }

}