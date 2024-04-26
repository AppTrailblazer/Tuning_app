package com.willeypianotuning.toneanalyzer.audio.note_names

import android.text.Spanned

class English2NoteNamingConvention : NoteNamingConvention() {
    override fun usesSolmizationSystem(): Boolean {
        return false
    }

    override fun noteName(noteIndex: Int): String {
        return ABC[noteIndex]
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
            octavePost = sub((octave - 3).toString())
        }
        return fromHtml(octavePre + ABC[noteScaleIndex(noteIndex)] + subSharp + octavePost)
    }

    companion object {
        private val ABC = arrayOf("A", "B", "C", "D", "E", "F", "G")
    }
}