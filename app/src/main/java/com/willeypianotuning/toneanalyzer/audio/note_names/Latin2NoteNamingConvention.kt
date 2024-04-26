package com.willeypianotuning.toneanalyzer.audio.note_names

import android.text.Spanned

class Latin2NoteNamingConvention : NoteNamingConvention() {
    override fun usesSolmizationSystem(): Boolean {
        return true
    }

    override fun noteName(noteIndex: Int): String {
        return LaSiDo[noteIndex]
    }

    override fun pianoNoteName(noteIndex: Int): Spanned {
        val octave = (noteIndex + 9) / 12
        val sharp = isSharp(noteIndex)
        val subSharp = if (sharp) sub("#") else ""
        var octavePre = ""
        var octavePost = ""
        if (octave < 2) {
            octavePre = sub((2 - octave).toString())
        } else if (octave > 2) {
            octavePost = sup((octave - 3).toString())
        }
        return fromHtml(octavePre + LaSiDo[noteScaleIndex(noteIndex)] + subSharp + octavePost)
    }

    companion object {
        private val LaSiDo = arrayOf("La", "Si", "Do", "Re", "Mi", "Fa", "Sol")
    }
}