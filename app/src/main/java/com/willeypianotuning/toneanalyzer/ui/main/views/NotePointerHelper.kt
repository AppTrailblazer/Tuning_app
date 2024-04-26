package com.willeypianotuning.toneanalyzer.ui.main.views

import android.graphics.PointF
import androidx.annotation.IntRange

class NotePointerHelper {
    companion object {
        const val WHITE_KEYS = 52
        const val BLACK_KEYS = 36
        private const val KEY_DIVIDER_PROPORTION_TO_WIDTH = 0.001f
        private const val BORDER_PROPORTION_TO_WIDTH = 0.008f
        private const val WIDTH_PROPORTION_BLACK_KEY_TO_WHITE_KEY = 0.6f
    }

    var customBorderWidth: Float? = null

    private fun isBlackKey(@IntRange(from = 1, to = 88) note: Int): Boolean {
        return note % 12 == 2 || note % 12 == 5 || note % 12 == 7 || note % 12 == 10 || note % 12 == 0
    }

    private fun shouldAlignLeft(@IntRange(from = 1, to = 88) note: Int): Boolean {
        return note == 1 || note % 12 == 4 || note % 12 == 9
    }

    private fun shouldAlignRight(@IntRange(from = 1, to = 88) note: Int): Boolean {
        return note % 12 == 3 || note % 12 == 8
    }

    private fun shouldAlignCenter(@IntRange(from = 1, to = 88) note: Int): Boolean {
        return note % 12 == 1 || note % 12 == 6 || note % 12 == 11
    }

    fun computeNotePointerWidth(width: Float): Float {
        val localBorder = BORDER_PROPORTION_TO_WIDTH * width
        val localDivider = KEY_DIVIDER_PROPORTION_TO_WIDTH * width
        return 0.6f * (width - 2 * localBorder - localDivider * (WHITE_KEYS - 1)) / WHITE_KEYS
    }

    fun borderWidth(width: Float): Float {
        return customBorderWidth ?: BORDER_PROPORTION_TO_WIDTH * width
    }

    fun dividerWidth(width: Float): Float {
        return KEY_DIVIDER_PROPORTION_TO_WIDTH * width
    }

    private fun whiteKeyWidth(width: Float): Float {
        return (width - 2 * borderWidth(width) - dividerWidth(width) * (WHITE_KEYS - 1)) / WHITE_KEYS
    }

    private fun blackKeyWidth(width: Float): Float {
        return whiteKeyWidth(width) * WIDTH_PROPORTION_BLACK_KEY_TO_WHITE_KEY
    }

    fun noteOffset(width: Float, @IntRange(from = 1, to = 88) note: Int): PointF {
        val localBorder = borderWidth(width)
        val localDivider = dividerWidth(width)

        val whiteKeyWidth = whiteKeyWidth(width)
        val blackKeyWidth = blackKeyWidth(width)

        var keyIndex = 1
        for (i in 0 until WHITE_KEYS) {
            if (i != 0 && (i % 7 == 1 || i % 7 == 3 || i % 7 == 4 || i % 7 == 6 || i % 7 == 0)) {
                // black key
                if (keyIndex == note) {
                    val shiftedLeft = i % 7 == 3 || i % 7 == 6
                    val shiftedRight = i % 7 == 1 || i % 7 == 4
                    val leftShift = when {
                        shiftedLeft -> 0.7f * blackKeyWidth
                        shiftedRight -> 0.3f * blackKeyWidth
                        else -> 0.5f * blackKeyWidth
                    }
                    val rightShift = when {
                        shiftedLeft -> 0.3f * blackKeyWidth
                        shiftedRight -> 0.7f * blackKeyWidth
                        else -> 0.5f * blackKeyWidth
                    }
                    val left = localBorder + i * whiteKeyWidth + (i - 1) * localDivider - leftShift
                    val right = localBorder + i * whiteKeyWidth + i * localDivider + rightShift
                    return PointF(left, right)
                }
                keyIndex++
            }

            if (keyIndex == note) {
                val left = localBorder + i * whiteKeyWidth + i * localDivider
                val right = localBorder + (i + 1) * whiteKeyWidth + i * localDivider
                return PointF(left, right)
            }
            keyIndex++
        }
        throw IllegalArgumentException("Cannot find position for note $note")
    }

    fun notePointerOffsetLeft(width: Float, @IntRange(from = 1, to = 88) note: Int): Float {
        var noteOffset = noteOffset(width, note).x
        when {
            shouldAlignRight(note) -> noteOffset += 0.7f * blackKeyWidth(width)
            shouldAlignCenter(note) -> noteOffset += 0.4f * blackKeyWidth(width)
        }
        return noteOffset
    }

}