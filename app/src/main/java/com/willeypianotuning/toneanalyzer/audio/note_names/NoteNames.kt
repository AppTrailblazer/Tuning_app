package com.willeypianotuning.toneanalyzer.audio.note_names

import android.content.Context
import android.text.Spanned
import androidx.annotation.IntDef

object NoteNames {
    const val NAME_A0_B3_C4_C8 = 0
    const val NAME_LA0_SI3_DO4_DO8 = 1
    const val NAME_2A_B0_C1_C5 = 2
    const val NAME_2LA_SS0_DO1_DO5 = 3
    const val NAME_2A_H0_C1_C5 = 4
    const val LOCALE = 5

    private val NOTE_NAMING_CONVENTIONS = mapOf<Int, Function1<Context, NoteNamingConvention>>(
        NAME_A0_B3_C4_C8 to { English1NoteNamingConvention() },
        NAME_LA0_SI3_DO4_DO8 to { Latin1NoteNamingConvention() },
        NAME_2A_B0_C1_C5 to { English2NoteNamingConvention() },
        NAME_2LA_SS0_DO1_DO5 to { Latin2NoteNamingConvention() },
        NAME_2A_H0_C1_C5 to { GermanNoteNamingConvention() },
        LOCALE to { context -> LocaleBasedNamingConvention(context) }
    )

    @JvmStatic
    fun noteNamingConventions(context: Context): Array<Spanned> {
        return NOTE_NAMING_CONVENTIONS.entries.sortedBy { it.key }.map {
            return@map it.value.invoke(context).name()
        }.toTypedArray()
    }

    @JvmStatic
    fun getNamingConvention(
        context: Context,
        @NoteNaming naming: Int = NAME_A0_B3_C4_C8
    ): NoteNamingConvention {
        var checkedNaming = naming
        if (!NOTE_NAMING_CONVENTIONS.containsKey(checkedNaming)) {
            checkedNaming = LOCALE
        }
        return requireNotNull(NOTE_NAMING_CONVENTIONS[checkedNaming]).invoke(context)
    }

    @Retention(AnnotationRetention.SOURCE)
    @IntDef(
        NAME_A0_B3_C4_C8,
        NAME_LA0_SI3_DO4_DO8,
        NAME_2A_B0_C1_C5,
        NAME_2LA_SS0_DO1_DO5,
        NAME_2A_H0_C1_C5,
        LOCALE
    )
    annotation class NoteNaming
}