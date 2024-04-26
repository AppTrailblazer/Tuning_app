package com.willeypianotuning.toneanalyzer.audio

import com.willeypianotuning.toneanalyzer.AppSettings
import com.willeypianotuning.toneanalyzer.ToneDetectorWrapper
import com.willeypianotuning.toneanalyzer.store.TuningStyleDataStore
import com.willeypianotuning.toneanalyzer.store.db.tuning_styles.TuningStyle
import kotlinx.coroutines.runBlocking
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TuningStyleHelper @Inject constructor(
    private val tuningStyleDataStore: TuningStyleDataStore,
    private val appSettings: AppSettings,
    private val audioRecorder: AudioRecorder,
    private val toneAnalyzer: ToneDetectorWrapper
) {
    private var temporalStyle: TuningStyle? = null
    private var globalStyle: TuningStyle? = null

    init {
        loadTuningStyles()
    }

    private fun loadTuningStyles() {
        val styleId = appSettings.tuningStyleId
        if (styleId != null) {
            val style = runBlocking { tuningStyleDataStore.getById(styleId) }
                ?: TuningStyle.DEFAULT
            setGlobalIntervalWeights(style, updateTuning = false)
        } else {
            setGlobalIntervalWeights(TuningStyle.DEFAULT, updateTuning = false)
        }
    }

    fun refreshWeights() {
        val stylesOrder = arrayOf(
            temporalStyle,
            audioRecorder.tuning?.tuningStyle,
            globalStyle
        )
        val styleToApply = stylesOrder.firstOrNull() ?: TuningStyle.DEFAULT
        styleToApply.let {
            toneAnalyzer.setIntervalWeights(it.intervalWeights)
        }
    }

    fun setTemporalIntervalWeights(style: TuningStyle?) {
        temporalStyle = style
        refreshWeights()
    }

    fun getTemporalIntervalWeights(): TuningStyle? {
        return temporalStyle
    }

    fun getGlobalIntervalWeights(): TuningStyle {
        return globalStyle ?: TuningStyle.DEFAULT
    }

    fun setGlobalIntervalWeights(style: TuningStyle, updateTuning: Boolean = false) {
        globalStyle = style
        if (updateTuning) {
            setTuningIntervalWeights(style)
        } else {
            refreshWeights()
        }
        appSettings.tuningStyleId = style.id
    }

    fun setTuningIntervalWeights(style: TuningStyle?) {
        audioRecorder.tuning?.tuningStyle = style
        refreshWeights()
    }

}