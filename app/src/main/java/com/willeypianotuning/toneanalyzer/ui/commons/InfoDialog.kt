package com.willeypianotuning.toneanalyzer.ui.commons

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.Window
import androidx.core.view.isVisible
import com.willeypianotuning.toneanalyzer.databinding.DialogInfoBinding

class InfoDialog(
    activity: Activity,
    private val title: String,
    private val message: String,
    private val doNotShowAgain: DoNotShowAgain = DoNotShowAgain.Hidden,
    private val onOkClick: (() -> Unit)? = null
) : Dialog(activity) {
    private val dialogSettings = InfoDialogSettings(context)

    private lateinit var binding: DialogInfoBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        binding = DialogInfoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.dialogTitle.text = title
        binding.dialogMessage.text = message

        when (doNotShowAgain) {
            is DoNotShowAgain.Hidden -> binding.layoutDoNotShowAgain.isVisible = false
            is DoNotShowAgain.Shown -> binding.layoutDoNotShowAgain.isVisible = true
        }

        binding.layoutDoNotShowAgain.setOnClickListener {
            binding.doNotShowAgainSwitch.isChecked = !binding.doNotShowAgainSwitch.isChecked
        }

        binding.buttonPositive.setOnClickListener {
            if (doNotShowAgain is DoNotShowAgain.Shown && binding.doNotShowAgainSwitch.isChecked) {
                dialogSettings.setDoNotShowAgain(doNotShowAgain.key)
            }
            onOkClick?.invoke()
            dismiss()
        }
    }

    companion object {
        fun show(
            activity: Activity,
            title: String,
            message: String,
            doNotShowAgain: DoNotShowAgain = DoNotShowAgain.Hidden,
            onOkClick: (() -> Unit)? = null
        ) {
            val shouldShow = when (doNotShowAgain) {
                is DoNotShowAgain.Hidden -> true
                is DoNotShowAgain.Shown -> InfoDialogSettings(activity).shouldShow(doNotShowAgain.key)
            }
            if (!shouldShow) {
                onOkClick?.invoke()
                return
            }

            val dialog = InfoDialog(
                activity,
                title,
                message,
                doNotShowAgain,
                onOkClick
            )
            dialog.show()
        }
    }
}

class InfoDialogSettings(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("info_dialogs.xml", Context.MODE_PRIVATE)

    fun shouldShow(key: String): Boolean {
        return !prefs.getBoolean(key, false)
    }

    fun setDoNotShowAgain(key: String) {
        prefs.edit().putBoolean(key, true).apply()
    }
}

sealed interface DoNotShowAgain {
    object Hidden : DoNotShowAgain
    data class Shown(val key: String) : DoNotShowAgain
}