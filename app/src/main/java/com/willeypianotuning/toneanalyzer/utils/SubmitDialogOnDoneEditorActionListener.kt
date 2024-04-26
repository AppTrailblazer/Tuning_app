package com.willeypianotuning.toneanalyzer.utils

import android.view.KeyEvent
import android.view.inputmethod.EditorInfo
import android.widget.TextView
import androidx.appcompat.app.AlertDialog

class SubmitDialogOnDoneEditorActionListener(private val dialog: AlertDialog) :
    TextView.OnEditorActionListener {
    override fun onEditorAction(v: TextView, actionId: Int, event: KeyEvent?): Boolean {
        if (actionId == EditorInfo.IME_ACTION_DONE) {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick()
            return true
        }
        return false
    }
}