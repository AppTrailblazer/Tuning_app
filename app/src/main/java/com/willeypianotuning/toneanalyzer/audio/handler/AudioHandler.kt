package com.willeypianotuning.toneanalyzer.audio.handler

interface AudioHandler {
    fun prepare()
    fun handle(audioData: ShortArray, read: Int)
    fun finish()
}