package com.willeypianotuning.toneanalyzer.ui.main

class NoteLock @JvmOverloads constructor(private var isPlus: Boolean = false) {

    fun setPlus(plus: Boolean) {
        isPlus = plus
    }

    fun isNoteUnlocked(note: Int): Boolean {
        return isPlus || note in 28..52 || note % 12 == 1
    }

}
