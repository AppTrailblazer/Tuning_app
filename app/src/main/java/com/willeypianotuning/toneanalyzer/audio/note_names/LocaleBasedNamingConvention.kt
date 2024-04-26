package com.willeypianotuning.toneanalyzer.audio.note_names

import android.content.Context
import android.text.Spanned
import android.text.TextUtils
import com.willeypianotuning.toneanalyzer.R
import java.util.Locale

/**
 * Based on
 * {@link https://en.wikipedia.org/wiki/Musical_note#History_of_note_names}
 * {@link https://en.wikipedia.org/wiki/Key_signature_names_and_translations}
 */
class LocaleBasedNamingConvention(val context: Context) : NoteNamingConvention() {

    private val sharpSign
        get() = context.resources.getString(R.string.sign_sharp)

    private val sharpGoesAfterNote
        get() = context.resources.getBoolean(R.bool.sharp_after_note)

    private fun formatSharp(sharp: String): String {
        return when (context.resources.getInteger(R.integer.sharp_vertical_position)) {
            1 -> sup(sharp)
            -1 -> sub(sharp)
            else -> sharp
        }
    }

    private fun formatOctave(octave: Int): String {
        val octaveString = String.format(Locale.getDefault(), "%d", octave)
        return when (context.resources.getInteger(R.integer.octave_vertical_position)) {
            1 -> sup(octaveString)
            -1 -> sub(octaveString)
            else -> octaveString
        }
    }

    private fun alphabet(): Array<String> {
        return if (usesSolmizationSystem()) {
            arrayOf(
                context.getString(R.string.solmization_la),
                context.getString(R.string.solmization_si),
                context.getString(R.string.solmization_do),
                context.getString(R.string.solmization_re),
                context.getString(R.string.solmization_mi),
                context.getString(R.string.solmization_fa),
                context.getString(R.string.solmization_sol)
            )
        } else {
            arrayOf(
                context.getString(R.string.alphabetic_a),
                context.getString(R.string.alphabetic_b),
                context.getString(R.string.alphabetic_c),
                context.getString(R.string.alphabetic_d),
                context.getString(R.string.alphabetic_e),
                context.getString(R.string.alphabetic_f),
                context.getString(R.string.alphabetic_g)
            )
        }
    }

    override fun usesSolmizationSystem(): Boolean {
        return !context.resources.getBoolean(R.bool.use_alphabetic)
    }

    override fun name(): Spanned {
        return TextUtils.concat(
            context.getString(R.string.note_naming_local),
            " (",
            super.name(),
            ")"
        ) as Spanned
    }

    override fun noteName(noteIndex: Int): String {
        return alphabet()[noteIndex]
    }

    override fun pianoNoteName(noteIndex: Int): Spanned {
        val alphabet = alphabet()

        val octave = (noteIndex + 9) / 12
        val sharp = if (isSharp(noteIndex))
            formatSharp(sharpSign)
        else ""

        val format = if (sharpGoesAfterNote)
            alphabet[noteScaleIndex(noteIndex)] + sharp + formatOctave(octave)
        else
            sharp + alphabet[noteScaleIndex(noteIndex)] + formatOctave(octave)

        return fromHtml(format)
    }
}