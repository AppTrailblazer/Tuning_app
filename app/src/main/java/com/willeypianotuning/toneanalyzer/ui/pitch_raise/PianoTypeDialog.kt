package com.willeypianotuning.toneanalyzer.ui.pitch_raise

import android.app.Activity
import android.app.Dialog
import android.content.res.Configuration
import android.os.Bundle
import android.view.ViewGroup
import android.view.Window
import com.willeypianotuning.toneanalyzer.R
import com.willeypianotuning.toneanalyzer.databinding.DialogPitchRaisePianoTypeBinding
import com.willeypianotuning.toneanalyzer.utils.DisplayUtils

class PianoTypeDialog(
    private val activity: Activity,
    private var pianoType: Int
) : Dialog(activity) {
    var onPianoTypeChangeListener: OnPianoTypeChangeListener? = null
    var okButtonText: String? = null
    private lateinit var binding: DialogPitchRaisePianoTypeBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        binding = DialogPitchRaisePianoTypeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.dialogTitle.setText(R.string.dialog_piano_type_title)
        val adapter = PianoTypeAdapter(context)
        binding.contentListView.adapter = adapter
        adapter.setSelectedType(pianoType)
        adapter.onSelectionChangedListener =
            PianoTypeAdapter.OnSelectionChangedListener { pianoType ->
                this.pianoType = pianoType
            }

        setupActionButtons()

        val size = DisplayUtils.screenSize(activity)
        val orientation = activity.resources.configuration.orientation
        if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
            window?.setLayout((0.5 * size.x).toInt(), ViewGroup.LayoutParams.WRAP_CONTENT)
        } else {
            window?.setLayout((0.9 * size.x).toInt(), ViewGroup.LayoutParams.WRAP_CONTENT)
        }
    }

    private fun setupActionButtons() {
        binding.buttonPositive.text =
            okButtonText ?: binding.buttonPositive.context.getString(R.string.action_ok)
        binding.buttonPositive.setOnClickListener {
            onPianoTypeChangeListener?.onPianoTypeChanged(pianoType)
            dismiss()
        }
        binding.buttonNegative.setOnClickListener {
            cancel()
        }
    }

    fun interface OnPianoTypeChangeListener {
        fun onPianoTypeChanged(pianoType: Int)
    }

}