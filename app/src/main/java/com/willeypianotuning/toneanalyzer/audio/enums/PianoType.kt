package com.willeypianotuning.toneanalyzer.audio.enums

import androidx.annotation.IntDef

object PianoType {
    const val CONCERT_GRAND = 0
    const val MEDIUM_GRAND = 1
    const val BABY_GRAND = 2
    const val FULL_UPRIGHT = 3
    const val STUDIO_UPRIGHT = 4
    const val CONSOLE = 5
    const val SPINET = 6
    const val OTHER = 7
    const val UNSPECIFIED = 8
}

@Retention(AnnotationRetention.SOURCE)
@IntDef(
    PianoType.CONCERT_GRAND,
    PianoType.MEDIUM_GRAND,
    PianoType.BABY_GRAND,
    PianoType.FULL_UPRIGHT,
    PianoType.STUDIO_UPRIGHT,
    PianoType.CONSOLE,
    PianoType.SPINET,
    PianoType.OTHER,
    PianoType.UNSPECIFIED
)
annotation class PianoTypeEnum