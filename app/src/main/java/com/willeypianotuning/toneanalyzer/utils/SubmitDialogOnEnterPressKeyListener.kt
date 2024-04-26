package com.willeypianotuning.toneanalyzer.utils

import android.view.KeyEvent
import android.view.View
import androidx.appcompat.app.AlertDialog

class SubmitDialogOnEnterPressKeyListener(private val dialog: AlertDialog) : View.OnKeyListener {
    override fun onKey(v: View, keyCode: Int, event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN &&
            keyCode == KeyEvent.KEYCODE_ENTER
        ) {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick()
            return true
        }
        return false
    }
}