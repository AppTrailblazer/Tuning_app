package com.willeypianotuning.toneanalyzer.audio.note_names

import android.text.Spanned

class Latin1NoteNamingConvention : NoteNamingConvention() {
    override fun usesSolmizationSystem(): Boolean {
        return true
    }

    override fun noteName(noteIndex: Int): String {
        return LaSiDo[noteIndex]
    }

    override fun pianoNoteName(noteIndex: Int): Spanned {
        val octave = (noteIndex + 9) / 12
        val sharp = isSharp(noteIndex)
        val supSharp = if (sharp) sup("#") else ""
        return fromHtml(LaSiDo[noteScaleIndex(noteIndex)] + supSharp + octave)
    }

    companion object {
        private val LaSiDo = arrayOf("La", "Si", "Do", "Re", "Mi", "Fa", "Sol")
    }
}