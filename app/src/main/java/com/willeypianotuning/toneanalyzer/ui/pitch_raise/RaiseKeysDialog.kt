package com.willeypianotuning.toneanalyzer.ui.pitch_raise

import android.app.Activity
import android.app.Dialog
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.Button
import android.widget.Toast
import com.willeypianotuning.toneanalyzer.R
import com.willeypianotuning.toneanalyzer.audio.PitchRaiseOptions
import com.willeypianotuning.toneanalyzer.databinding.PitchRaiseDialogBinding
import com.willeypianotuning.toneanalyzer.extensions.setCompoundDrawableBottom
import timber.log.Timber

class RaiseKeysDialog(
    activity: Activity,
    private val pitchRaiseOptions: PitchRaiseOptions,
    private val lastUsedPitchRaiseKeys: List<Int>
) : Dialog(activity) {
    companion object {
        private const val MAX_NOTES_USER_CAN_SELECT = 4
    }

    private var onPitchRaiseKeysSelectedListener: OnPitchRaiseKeysSelectedListener? = null
    private lateinit var binding: PitchRaiseDialogBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        binding = PitchRaiseDialogBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val tws = listOf(
            binding.pitchRaiseA,
            binding.pitchRaiseAsharp,
            binding.pitchRaiseB,
            binding.pitchRaiseC,
            binding.pitchRaiseCsharp,
            binding.pitchRaiseD,
            binding.pitchRaiseDsharp,
            binding.pitchRaiseE,
            binding.pitchRaiseF,
            binding.pitchRaiseFsharp,
            binding.pitchRaiseG,
            binding.pitchRaiseGsharp,
        )
        pitchRaiseOptions.notesToRaise = ArrayList(lastUsedPitchRaiseKeys)
        val listener = View.OnClickListener { v: View ->
            val b = v as Button
            val position = tws.indexOf(v)
            Timber.d("Button: %s", position)
            if (pitchRaiseOptions.notesToRaise.contains(position)) {
                pitchRaiseOptions.notesToRaise.remove(Integer.valueOf(position))
                setPitchRaiseNoteStatus(b, false)
                updatePitchRaiseStatus(pitchRaiseOptions.notesToRaise, tws)
                Timber.d("Button:$position false")
            } else if (pitchRaiseOptions.notesToRaise.size < MAX_NOTES_USER_CAN_SELECT) {
                pitchRaiseOptions.notesToRaise.add(position)
                setPitchRaiseNoteStatus(b, true)
                updatePitchRaiseStatus(pitchRaiseOptions.notesToRaise, tws)
                Timber.d("Button:$position true")
            }
        }
        tws.forEachIndexed { index, button ->
            button.setOnClickListener(listener)
            setPitchRaiseNoteStatus(button, pitchRaiseOptions.notesToRaise.contains(index))
        }
        updatePitchRaiseStatus(pitchRaiseOptions.notesToRaise, tws)

        setupActionButtons()
        window?.setLayout(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    }

    private fun setupActionButtons() {
        binding.buttonPositive.setText(R.string.action_continue)
        binding.buttonPositive.setOnClickListener {
            if (pitchRaiseOptions.notesToRaise.isEmpty()) {
                Toast.makeText(
                    context,
                    R.string.dialog_pitch_error_no_keys_selected,
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                onPitchRaiseKeysSelectedListener?.onPitchRaiseKeysSelected(pitchRaiseOptions.notesToRaise)
                dismiss()
            }
        }
        binding.buttonNegative.setOnClickListener {
            cancel()
        }
    }

    fun setOnPitchRaiseKeysSelected(onPositiveClicked: OnPitchRaiseKeysSelectedListener?): RaiseKeysDialog {
        this.onPitchRaiseKeysSelectedListener = onPositiveClicked
        return this
    }

    private fun setPitchRaiseNoteStatus(b: Button, isSelected: Boolean) {
        val drawable = if (isSelected)
            android.R.drawable.checkbox_on_background
        else
            android.R.drawable.checkbox_off_background
        b.setCompoundDrawableBottom(drawable)
    }

    private fun updatePitchRaiseStatus(selected: List<Int>, buttons: List<Button>) {
        val values = BooleanArray(12) { false }
        for (position in selected) {
            var p = position - 1
            if (p < 0) p += values.size
            if (p >= 0 && p < values.size) values[p] = true
            p--
            if (p < 0) p += values.size
            if (p >= 0 && p < values.size) values[p] = true
            p = position + 1
            if (p >= values.size) p -= values.size
            if (p < values.size && p >= 0) values[p] = true
            p++
            if (p >= values.size) p -= values.size
            if (p < values.size && p >= 0) values[p] = true
        }
        for (i in buttons.indices) {
            val b = buttons[i]
            b.isEnabled = !values[i]
        }
    }

    fun interface OnPitchRaiseKeysSelectedListener {
        fun onPitchRaiseKeysSelected(notes: List<Int>)
    }

}