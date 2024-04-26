package com.willeypianotuning.toneanalyzer.extensions

import androidx.appcompat.widget.PopupMenu
import timber.log.Timber

fun PopupMenu.forceShowIcons() {
    try {
        val fields = this.javaClass.declaredFields
        for (field in fields) {
            if ("mPopup" == field.name) {
                field.isAccessible = true
                val menuPopupHelper = field[this]
                val classPopupHelper = Class.forName(
                    menuPopupHelper
                        .javaClass.name
                )
                val setForceIcons = classPopupHelper.getMethod(
                    "setForceShowIcon", Boolean::class.javaPrimitiveType
                )
                setForceIcons.invoke(menuPopupHelper, true)
                break
            }
        }
    } catch (e: Throwable) {
        Timber.e(e, "Cannot prepare popup window")
    }
}