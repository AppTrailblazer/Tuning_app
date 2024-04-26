package com.willeypianotuning.toneanalyzer.audio

data class PreferredAudioInput(
    val type: AudioInputType,
    val allowBluetooth: Boolean,
)

enum class AudioInputType(val key: String) {
    BUILT_IN_MIC("built-in-mic"),
    EXTERNAL_MIC("external-mic"),
}