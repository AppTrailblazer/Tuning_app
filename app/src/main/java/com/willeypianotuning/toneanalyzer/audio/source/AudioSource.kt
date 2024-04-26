package com.willeypianotuning.toneanalyzer.audio.source

/**
 * This is the abstraction over the source of audio data
 * It's purpose is to have a single interface for different possible audio sources.
 */
interface AudioSource {
    val samplingRate: Int
    val bufferSizeSamples: Int

    /**
     * Initializes audio source to return audio data
     * @return true if the audio source initialized properly and data can be read
     */
    fun start(): Boolean

    /**
     * Reads audio data into `audioData` array
     * Note, the number of points read can be less or equal then the length of audioData
     *
     * @return number of points read
     */
    fun read(audioData: ShortArray): Int

    fun stop()
}