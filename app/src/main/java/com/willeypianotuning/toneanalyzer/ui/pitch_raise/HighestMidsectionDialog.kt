package com.willeypianotuning.toneanalyzer.ui.pitch_raise

import android.app.Activity
import android.app.Dialog
import android.content.DialogInterface
import android.content.res.Configuration
import android.os.Bundle
import android.view.ViewGroup
import android.view.Window
import com.willeypianotuning.toneanalyzer.AppSettings
import com.willeypianotuning.toneanalyzer.R
import com.willeypianotuning.toneanalyzer.audio.PitchRaiseOptions
import com.willeypianotuning.toneanalyzer.audio.note_names.NoteNames.getNamingConvention
import com.willeypianotuning.toneanalyzer.databinding.DialogPitchRaiseLowestHighestBinding
import com.willeypianotuning.toneanalyzer.utils.DisplayUtils

class HighestMidsectionDialog(
    private val activity: Activity,
    private val pitchRaiseOptions: PitchRaiseOptions,
    private val appSettings: AppSettings
) : Dialog(activity) {
    private var onPositiveClicked: DialogInterface.OnClickListener? = null

    private lateinit var binding: DialogPitchRaiseLowestHighestBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        binding = DialogPitchRaiseLowestHighestBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.dialogTitle.setText(R.string.dialog_highest_midsection_title)
        binding.headerImageView.setImageResource(R.drawable.ic_highest_string)

        setupListView()
        setupActionButtons()

        val size = DisplayUtils.screenSize(activity)
        val orientation = activity.resources.configuration.orientation
        if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
            window?.setLayout((0.5 * size.x).toInt(), ViewGroup.LayoutParams.WRAP_CONTENT)
        } else {
            window?.setLayout((0.9 * size.x).toInt(), ViewGroup.LayoutParams.WRAP_CONTENT)
        }
    }

    private fun setupListView() {
        val noteNamingConvention = getNamingConvention(activity, appSettings.noteNames)
        val items = Array<CharSequence>(PitchRaiseOptions.HIGHEST_MIDSECTION_LENGTH) { i ->
            noteNamingConvention.pianoNoteName(i + PitchRaiseOptions.HIGHEST_MIDSECTION_START - 1)
        }
        val selectedPosition =
            pitchRaiseOptions.highestMidsection - PitchRaiseOptions.HIGHEST_MIDSECTION_START
        val pitchRaiseAdapter = SingleChoiceArrayAdapter(activity, items).apply {
            setSelectedPosition(selectedPosition)
            setOnSelectionChangedListener { position ->
                pitchRaiseOptions.highestMidsection =
                    position + PitchRaiseOptions.HIGHEST_MIDSECTION_START
            }
        }
        binding.contentListView.apply {
            adapter = pitchRaiseAdapter
            postDelayed({
                binding.contentListView.smoothScrollToPositionFromTop(
                    selectedPosition,
                    binding.contentListView.height / 2,
                    50
                )
            }, 50)
        }
    }

    private fun setupActionButtons() {
        binding.buttonPositive.apply {
            setText(R.string.action_continue)
            setOnClickListener {
                onPositiveClicked?.onClick(this@HighestMidsectionDialog, 1)
                dismiss()
            }
        }
        binding.buttonNegative.setOnClickListener {
            cancel()
        }
    }

    fun setOnPositiveClicked(onPositiveClicked: DialogInterface.OnClickListener?): HighestMidsectionDialog {
        this.onPositiveClicked = onPositiveClicked
        return this
    }

}