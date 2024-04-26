package com.willeypianotuning.toneanalyzer.audio.handler

import androidx.annotation.Keep

@Keep
@Suppress("unused")
class EmptyAudioHandler : AudioHandler {
    override fun prepare() {

    }

    override fun handle(audioData: ShortArray, read: Int) {

    }

    override fun finish() {

    }
}
