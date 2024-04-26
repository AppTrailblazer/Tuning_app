package com.willeypianotuning.toneanalyzer.ui.settings

import android.app.Activity
import android.text.InputType
import android.util.DisplayMetrics
import android.util.TypedValue
import android.view.KeyEvent
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.widget.AppCompatEditText
import androidx.core.widget.doAfterTextChanged
import com.willeypianotuning.toneanalyzer.R
import com.willeypianotuning.toneanalyzer.extensions.round
import com.willeypianotuning.toneanalyzer.utils.decimal.AppInputDecimalFormat
import kotlin.math.log10
import kotlin.math.pow

class PitchOffsetDialog @JvmOverloads constructor(
    private val activity: Activity,
    private val selection: Int = SELECTION_NONE
) {
    var onValueEnteredListener: OnValueEnteredListener? = null
    private var fromOtherChange = false

    private fun convertFreqToCents(freq: String): String {
        val value = AppInputDecimalFormat.parseDouble(freq) ?: return ""
        return AppInputDecimalFormat.formatDouble(convertFreqToCents(value))
    }

    private fun convertCentsToFreq(cents: String): String {
        val value = AppInputDecimalFormat.parseDouble(cents) ?: return ""
        return AppInputDecimalFormat.formatDouble(convertCentsToFreq(value))
    }

    fun show(title: String, message: String?, pitch: Double): android.app.AlertDialog {
        val builder = android.app.AlertDialog.Builder(activity)
        builder.setTitle(title)

        val dpToPx =
            activity.resources.displayMetrics.densityDpi.toFloat() / DisplayMetrics.DENSITY_DEFAULT

        val layout = LinearLayout(activity)
        layout.orientation = LinearLayout.VERTICAL
        layout.setPadding(
            (16 * dpToPx).toInt(),
            (8 * dpToPx).toInt(),
            (16 * dpToPx).toInt(),
            (8 * dpToPx).toInt()
        )
        val freqTitle = TextView(activity)
        freqTitle.text = activity.getString(R.string.message_frequency)
        val freqInput = AppCompatEditText(activity)
        freqInput.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        freqInput.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
        val centsTitle = TextView(activity)
        centsTitle.text = activity.getString(R.string.message_cents)
        val centsInput = AppCompatEditText(activity)
        centsInput.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        centsInput.inputType =
            InputType.TYPE_NUMBER_FLAG_SIGNED or InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL

        if (message != null) {
            val dialogMessage = TextView(activity)
            dialogMessage.text = message
            dialogMessage.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            dialogMessage.setPadding(0, 0, 0, (8 * dpToPx).toInt())
            layout.addView(dialogMessage)
        }

        layout.addView(freqTitle)
        layout.addView(freqInput)
        layout.addView(centsTitle)
        layout.addView(centsInput)

        centsInput.setSelectAllOnFocus(true)
        freqInput.setSelectAllOnFocus(true)
        freqInput.setText(AppInputDecimalFormat.formatDouble(pitch))
        centsInput.setText(AppInputDecimalFormat.formatDouble(convertFreqToCents(pitch)))
        builder.setView(layout)
        freqInput.doAfterTextChanged {
            if (!fromOtherChange && freqInput.text!!.toString().isNotEmpty()) {
                fromOtherChange = true

                centsInput.setText(convertFreqToCents(freqInput.text!!.toString()))

                fromOtherChange = false
            }
        }

        centsInput.doAfterTextChanged {
            if (!fromOtherChange) {
                fromOtherChange = true
                freqInput.setText(convertCentsToFreq(centsInput.text!!.toString()))
                fromOtherChange = false
            }
        }

        builder.setPositiveButton(activity.getString(R.string.action_ok)) { _, _ ->
            val input = freqInput.text?.toString() ?: return@setPositiveButton
            val value = AppInputDecimalFormat.parseDouble(input) ?: return@setPositiveButton

            onValueEnteredListener?.onValueEntered(value, convertFreqToCents(value))
        }
        builder.setNegativeButton(activity.getString(R.string.action_cancel)) { dialog, _ -> dialog.cancel() }
        val dialog = builder.show()
        freqInput.setOnKeyListener { _, keyCode, event ->
            // If the event is a key-down event on the "enter" button
            if (event.action == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_ENTER) {
                dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).performClick()
            }
            false
        }
        centsInput.setOnKeyListener { _, keyCode, event ->
            // If the event is a key-down event on the "enter" button
            if (event.action == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_ENTER) {
                dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).performClick()
            }
            false
        }
        if (selection == SELECTION_HERTZ) {
            freqInput.requestFocus()
        } else if (selection == SELECTION_CENTS) {
            centsInput.requestFocus()
        }
        dialog.window!!.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
        return dialog
    }

    fun interface OnValueEnteredListener {
        fun onValueEntered(pitch: Double, cents: Double)
    }

    companion object {
        const val SELECTION_NONE = 0
        const val SELECTION_HERTZ = 1
        const val SELECTION_CENTS = 2

        @JvmStatic
        fun convertFreqToCents(freq: Double): Double {
            val cents = log10(freq / 440.0) * 3986.0
            return cents.round(3)
        }

        @JvmStatic
        fun convertCentsToFreq(cents: Double): Double {
            val freq = 440.0 * 2.0.pow(cents / 1200.0)
            return freq.round(3)
        }
    }

}
