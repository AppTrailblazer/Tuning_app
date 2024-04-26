package com.willeypianotuning.toneanalyzer.audio.enums

import android.annotation.SuppressLint
import androidx.annotation.IntDef

@SuppressLint("ShiftFlags")
object InfoBoxText {
    const val MAKE = 1
    const val MODEL = 2
    const val PITCH_OFFSET = 4
    const val CLOCK = 8
}

@Retention(AnnotationRetention.SOURCE)
@IntDef(
    flag = true,
    value = [InfoBoxText.MAKE, InfoBoxText.MODEL, InfoBoxText.PITCH_OFFSET, InfoBoxText.CLOCK]
)
annotation class InfoBoxTextDef