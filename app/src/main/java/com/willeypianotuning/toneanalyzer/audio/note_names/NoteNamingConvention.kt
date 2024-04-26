package com.willeypianotuning.toneanalyzer.audio.note_names

import android.os.Build
import android.text.Html
import android.text.Spanned
import android.text.TextUtils
import androidx.annotation.IntRange

abstract class NoteNamingConvention {
    protected fun isSharp(noteIndex: Int): Boolean {
        return SHARP_NOTES[noteIndex % 12]
    }

    protected fun noteScaleIndex(noteIndex: Int): Int {
        return NOTES_SCALE_INDICES[noteIndex % 12]
    }

    protected fun sup(data: String): String {
        return "<sup><small>$data</small></sup>"
    }

    protected fun sub(data: String): String {
        return "<sub><small>$data</small></sub>"
    }

    protected fun fromHtml(html: String): Spanned {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Html.fromHtml(html, Html.FROM_HTML_MODE_LEGACY)
        } else {
            @Suppress("DEPRECATION")
            Html.fromHtml(html)
        }
    }

    abstract fun usesSolmizationSystem(): Boolean

    open fun name(): Spanned {
        return TextUtils.concat(
            pianoNoteName(0),
            "…",
            pianoNoteName(38),
            ", ",
            pianoNoteName(39),
            "…",
            pianoNoteName(87)
        ) as Spanned
    }

    abstract fun noteName(@IntRange(from = 0, to = 6) noteIndex: Int): String

    abstract fun pianoNoteName(@IntRange(from = 0, to = 87) noteIndex: Int): Spanned

    companion object {
        private val SHARP_NOTES = booleanArrayOf(
            false, true, false, false,
            true, false, true, false,
            false, true, false, true
        )
        private val NOTES_SCALE_INDICES = intArrayOf(
            0, 0, 1, 2, 2, 3, 3, 4, 5, 5, 6, 6
        )
    }
}