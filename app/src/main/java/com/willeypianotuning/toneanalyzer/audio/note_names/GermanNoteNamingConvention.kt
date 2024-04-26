package com.willeypianotuning.toneanalyzer.audio.note_names

import android.text.Spanned

class GermanNoteNamingConvention : NoteNamingConvention() {
    override fun usesSolmizationSystem(): Boolean {
        return false
    }

    override fun noteName(noteIndex: Int): String {
        return AHC[noteIndex]
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
        return fromHtml(octavePre + AHC[noteScaleIndex(noteIndex)] + subSharp + octavePost)
    }

    companion object {
        private val AHC = arrayOf("A", "H", "C", "D", "E", "F", "G")
    }
}