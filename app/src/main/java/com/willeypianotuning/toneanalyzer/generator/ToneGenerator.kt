package com.willeypianotuning.toneanalyzer.generator

interface ToneGenerator {
    var audioFrequency: Int

    var trebleBassOptions: TrebleBassOptions

    fun initTone(note: Int)
    fun generateTone(buffer: ShortArray)
}