package com.willeypianotuning.toneanalyzer.di

import com.willeypianotuning.toneanalyzer.ToneDetectorWrapper
import com.willeypianotuning.toneanalyzer.audio.source.AudioSource
import com.willeypianotuning.toneanalyzer.audio.source.MicrophoneAudioSource
import com.willeypianotuning.toneanalyzer.generator.ToneGenerator
import com.willeypianotuning.toneanalyzer.generator.ToneGeneratorWrapper
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AudioProcessingModule {
    @Provides
    fun provideAudioSource(microphone: MicrophoneAudioSource): AudioSource {
        return microphone
    }

    @Provides
    @Singleton
    fun provideToneDetector(): ToneDetectorWrapper {
        return ToneDetectorWrapper.newInstance()
    }

    @Provides
    @Singleton
    fun provideToneGenerator(toneDetector: ToneDetectorWrapper): ToneGenerator {
        return ToneGeneratorWrapper(toneDetector.pianoKeyFrequenciesPtr)
    }
}