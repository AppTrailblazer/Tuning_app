package com.willeypianotuning.toneanalyzer.ui.main.dialogs

import android.app.Activity
import android.app.Dialog
import android.content.res.Configuration
import android.os.Bundle
import android.view.ViewGroup
import android.view.Window
import androidx.annotation.FloatRange
import com.willeypianotuning.toneanalyzer.audio.note_names.NoteNamingConvention
import com.willeypianotuning.toneanalyzer.databinding.DialogTonePlayerLevelingBinding
import com.willeypianotuning.toneanalyzer.generator.TrebleBassOptions
import com.willeypianotuning.toneanalyzer.utils.DisplayUtils
import kotlin.math.roundToInt

fun interface OnTonePlayerLevelingConfigReadyListener {
    fun onTonePlayerLevelingConfigReady(
        dialog: TonePlayerLevelingDialog,
        volume: Float,
        trebleBassOptions: TrebleBassOptions
    )
}

class TonePlayerLevelingDialog(
    private val activity: Activity,
    private var trebleBassOptions: TrebleBassOptions,
    @FloatRange(from = 0.0, to = 1.0)
    private var mainVolume: Float,
    val noteNamingConvention: NoteNamingConvention
) : Dialog(activity) {
    private var onPositiveClicked: OnTonePlayerLevelingConfigReadyListener? = null

    private lateinit var binding: DialogTonePlayerLevelingBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        binding = DialogTonePlayerLevelingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUi()
        setupActionButtons()

        val size = DisplayUtils.screenSize(activity)
        val orientation = activity.resources.configuration.orientation
        if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
            window?.setLayout((0.5 * size.x).toInt(), ViewGroup.LayoutParams.WRAP_CONTENT)
        } else {
            window?.setLayout((0.9 * size.x).toInt(), ViewGroup.LayoutParams.WRAP_CONTENT)
        }
    }

    private fun Float.roundVolume(): Float {
        return (this * 10).roundToInt() / 10f
    }

    private fun setupUi() {
        binding.mainVolumeSlider.addOnChangeListener { slider, value, fromUser ->
            if (!fromUser) {
                return@addOnChangeListener
            }
            mainVolume = value / 100.0f
        }
        binding.bassVolumeSlider.max = TrebleBassOptions.bassVolumeRange.upper.toInt() * 100
        binding.bassVolumeSlider.onProgressChangeListener =
            VerticalSlider.OnSliderProgressChangeListener { progress, _ ->
                trebleBassOptions =
                    trebleBassOptions.copy(bassVolume = (progress / 100f).roundVolume())
                binding.bassVolumeValueLabel.text = "${trebleBassOptions.bassVolume}"
            }

        binding.trebleVolumeSlider.max = TrebleBassOptions.trebleVolumeRange.upper.toInt() * 100
        binding.trebleVolumeSlider.onProgressChangeListener =
            VerticalSlider.OnSliderProgressChangeListener { progress, _ ->
                trebleBassOptions =
                    trebleBassOptions.copy(trebleVolume = (progress / 100f).roundVolume())
                binding.trebleVolumeValueLabel.text = "${trebleBassOptions.trebleVolume * 5f}"
            }

        binding.bassTrebleNotesVolumeSlider.setLabelFormatter {
            val noteZeroIndexed = it.toInt()
            noteNamingConvention.pianoNoteName(noteZeroIndexed).toString()
        }
        binding.bassTrebleNotesVolumeSlider.addOnChangeListener { slider, value, fromUser ->
            if (!fromUser) {
                return@addOnChangeListener
            }
            var bassEdge = slider.values.first().toInt()
            var trebleEdge = slider.values.last().toInt()
            var shouldReset = false
            if (!TrebleBassOptions.bassEdgeRange.contains(bassEdge)) {
                bassEdge = TrebleBassOptions.bassEdgeRange.upper
                shouldReset = true
            }
            if (!TrebleBassOptions.trebleEdgeRange.contains(trebleEdge)) {
                trebleEdge = TrebleBassOptions.trebleEdgeRange.lower
                shouldReset = true
            }
            if (shouldReset) {
                binding.bassTrebleNotesVolumeSlider.values = listOf(
                    bassEdge.toFloat(),
                    trebleEdge.toFloat()
                )
            }

            trebleBassOptions = trebleBassOptions.copy(
                bassEdge = bassEdge.toShort(),
                trebleEdge = trebleEdge.toShort()
            )
        }
        applyValues()
    }

    private fun applyValues() {
        binding.mainVolumeSlider.value = mainVolume * 100f
        binding.bassVolumeSlider.progress = (trebleBassOptions.bassVolume * 100).toInt()
        binding.bassVolumeValueLabel.text = "${trebleBassOptions.bassVolume}"
        binding.trebleVolumeSlider.progress = (trebleBassOptions.trebleVolume * 100).toInt()
        binding.trebleVolumeValueLabel.text = "${trebleBassOptions.trebleVolume * 5f}"
        binding.bassTrebleNotesVolumeSlider.values = listOf(
            trebleBassOptions.bassEdge.toFloat(),
            trebleBassOptions.trebleEdge.toFloat()
        )
    }

    private fun setupActionButtons() {
        binding.buttonNeutral.setOnClickListener {
            mainVolume = 1f
            trebleBassOptions = TrebleBassOptions()
            applyValues()
        }
        binding.buttonPositive.setOnClickListener {
            onPositiveClicked?.onTonePlayerLevelingConfigReady(
                this@TonePlayerLevelingDialog,
                mainVolume,
                trebleBassOptions
            )
            dismiss()
        }
        binding.buttonNegative.setOnClickListener {
            cancel()
        }
    }

    fun setOnPositiveClicked(onPositiveClicked: OnTonePlayerLevelingConfigReadyListener): TonePlayerLevelingDialog {
        this.onPositiveClicked = onPositiveClicked
        return this
    }

}