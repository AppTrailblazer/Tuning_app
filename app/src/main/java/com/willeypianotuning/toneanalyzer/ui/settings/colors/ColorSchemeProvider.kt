package com.willeypianotuning.toneanalyzer.ui.settings.colors

import com.willeypianotuning.toneanalyzer.AppSettings
import com.willeypianotuning.toneanalyzer.ui.colors.ColorScheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ColorSchemeProvider @Inject constructor(
    private val appSettings: AppSettings,
    private val serializer: ColorSchemeSerializer,
) {
    private val state: MutableStateFlow<ColorScheme> = MutableStateFlow(restoreColorScheme())

    val value: ColorScheme get() = state.value

    fun asFlow(): StateFlow<ColorScheme> = state.asStateFlow()

    fun update(colorScheme: ColorScheme) {
        state.value = colorScheme
        saveColorScheme(colorScheme)
    }

    private fun restoreColorScheme(): ColorScheme {
        val default = ColorScheme.Default
        return runCatching {
            val jsonString = appSettings.colorSchemeJson
            if (jsonString.isBlank()) {
                return default
            }
            val json = JSONObject(jsonString)
            serializer.fromJson(json)
        }
            .onFailure {
                Timber.e(
                    it,
                    "Failed to restore color scheme from ${appSettings.colorSchemeJson}"
                )
            }
            .getOrDefault(default)
    }

    private fun saveColorScheme(scheme: ColorScheme) {
        val json = serializer.toJson(scheme).toString()
        appSettings.colorSchemeJson = json
        Timber.d("Color scheme saved $json")
    }
}