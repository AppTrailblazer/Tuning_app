package com.willeypianotuning.toneanalyzer.audio.note_names

import android.text.Spanned

class English1NoteNamingConvention : NoteNamingConvention() {
    override fun usesSolmizationSystem(): Boolean {
        return false
    }

    override fun noteName(noteIndex: Int): String {
        return ABC[noteIndex]
    }

    override fun pianoNoteName(noteIndex: Int): Spanned {
        val octave = (noteIndex + 9) / 12
        val sharp = isSharp(noteIndex)
        val supSharp = if (sharp) sup("#") else ""
        return fromHtml(ABC[noteScaleIndex(noteIndex)] + supSharp + octave)
    }

    companion object {
        private val ABC = arrayOf("A", "B", "C", "D", "E", "F", "G")
    }
}