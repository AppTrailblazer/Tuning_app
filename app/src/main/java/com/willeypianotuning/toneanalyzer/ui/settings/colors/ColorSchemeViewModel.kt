package com.willeypianotuning.toneanalyzer.ui.settings.colors

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.willeypianotuning.toneanalyzer.R
import com.willeypianotuning.toneanalyzer.ui.colors.ColorScheme
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import javax.inject.Inject

@HiltViewModel
class ColorSchemeViewModel @Inject constructor(
    private val colorSchemeProvider: ColorSchemeProvider
) : ViewModel() {
    private val colorSchemeFlow = MutableStateFlow(colorSchemeProvider.value)
    val colorScheme: ColorScheme get() = colorSchemeFlow.value

    val colorSettings: StateFlow<List<ColorSetting>> =
        colorSchemeFlow.map { toColorSettings(it, ColorScheme.Default) }
            .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val colorSchemeStateFlow: MutableStateFlow<ColorSetting?> = MutableStateFlow(null)
    val colorSchemeEditor: StateFlow<ColorSetting?> = colorSchemeStateFlow.asStateFlow()

    fun edit(setting: ColorSetting) {
        colorSchemeStateFlow.value = setting
    }

    fun cancelEdit() {
        colorSchemeStateFlow.value = null
    }

    fun update(colorScheme: ColorScheme) {
        colorSchemeStateFlow.value = null
        colorSchemeFlow.value = colorScheme
    }

    fun restoreDefaultScheme() {
        val defaultScheme = ColorScheme.Default
        colorSchemeFlow.value = defaultScheme
        colorSchemeProvider.update(defaultScheme)
    }

    fun saveCurrentScheme() {
        colorSchemeProvider.update(colorScheme)
    }

    private fun toColorSettings(value: ColorScheme, defaultColorScheme: ColorScheme): List<ColorSetting> {
        return listOf(
            ColorSetting(
                title = R.string.color_scheme_note_name,
                icon = R.drawable.icon_color_note_name,
                color = value.noteName,
                defaultColor = defaultColorScheme.noteName,
                update = { scheme, color -> scheme.copy(noteName = color) }
            ),
            ColorSetting(
                title = R.string.color_scheme_note_name_background,
                icon = R.drawable.icon_color_note_name_background,
                color = value.noteNameBackground,
                defaultColor = defaultColorScheme.noteNameBackground,
                update = { scheme, color -> scheme.copy(noteNameBackground = color) }
            ),
            ColorSetting(
                title = R.string.color_scheme_rings,
                icon = R.drawable.icon_color_rings,
                color = value.innerAndOuterRings,
                defaultColor = defaultColorScheme.innerAndOuterRings,
                alphaAllowed = true,
                update = { scheme, color -> scheme.copy(innerAndOuterRings = color) }
            ),
            ColorSetting(
                title = R.string.color_scheme_strobe_wheels,
                icon = R.drawable.icon_color_strobe_wheels,
                color = value.strobeWheels,
                defaultColor = defaultColorScheme.strobeWheels,
                update = { scheme, color -> scheme.copy(strobeWheels = color) }
            ),
            ColorSetting(
                title = R.string.color_scheme_strobe_background,
                icon = R.drawable.icon_color_strobe_background,
                color = value.strobeBackground,
                defaultColor = defaultColorScheme.strobeBackground,
                update = { scheme, color -> scheme.copy(strobeBackground = color) }
            ),
            ColorSetting(
                title = R.string.color_scheme_dial_markings,
                icon = R.drawable.icon_color_dial_markings,
                color = value.dialMarkings,
                defaultColor = defaultColorScheme.dialMarkings,
                alphaAllowed = true,
                update = { scheme, color -> scheme.copy(dialMarkings = color) }
            ),
            ColorSetting(
                title = R.string.color_scheme_needle,
                icon = R.drawable.icon_color_needle,
                color = value.needle,
                defaultColor = defaultColorScheme.needle,
                alphaAllowed = true,
                update = { scheme, color -> scheme.copy(needle = color) }
            ),
            ColorSetting(
                title = R.string.color_scheme_graph_background,
                icon = R.drawable.icon_color_graph_background,
                color = value.graphBackground,
                defaultColor = defaultColorScheme.graphBackground,
                update = { scheme, color -> scheme.copy(graphBackground = color) }
            ),
            ColorSetting(
                title = R.string.color_scheme_tuning_curve_line,
                icon = R.drawable.icon_color_tuning_curve_line,
                color = value.tuningCurveLine,
                defaultColor = defaultColorScheme.tuningCurveLine,
                update = { scheme, color -> scheme.copy(tuningCurveLine = color) }
            ),
            ColorSetting(
                title = R.string.color_scheme_tuning_curve_dots,
                icon = R.drawable.icon_color_tuning_curve_dots,
                color = value.tuningCurveDots,
                defaultColor = defaultColorScheme.tuningCurveDots,
                update = { scheme, color -> scheme.copy(tuningCurveDots = color) }
            ),
            ColorSetting(
                title = R.string.color_scheme_inharmonicity_line,
                icon = R.drawable.icon_color_inharmonicity_line,
                color = value.inharmonicityLine,
                defaultColor = defaultColorScheme.inharmonicityLine,
                update = { scheme, color -> scheme.copy(inharmonicityLine = color) }
            ),
            ColorSetting(
                title = R.string.color_scheme_inharmonicity_dots,
                icon = R.drawable.icon_color_inharmonicity_dots,
                color = value.inharmonicityDots,
                defaultColor = defaultColorScheme.inharmonicityDots,
                update = { scheme, color -> scheme.copy(inharmonicityDots = color) }
            ),
            ColorSetting(
                title = R.string.color_scheme_spectrum_line,
                icon = R.drawable.icon_color_frequency_spectrum_line,
                color = value.spectrumLine,
                defaultColor = defaultColorScheme.spectrumLine,
                update = { scheme, color -> scheme.copy(spectrumLine = color) }
            ),
            ColorSetting(
                title = R.string.color_scheme_current_note_indicator,
                icon = R.drawable.icon_color_current_note_indicator,
                color = value.currentNoteIndicator,
                defaultColor = defaultColorScheme.currentNoteIndicator,
                update = { scheme, color -> scheme.copy(currentNoteIndicator = color) }
            ),
            ColorSetting(
                title = R.string.color_scheme_menu_main_color,
                icon = R.drawable.icon_color_menu_background,
                color = value.menuPrimary,
                defaultColor = defaultColorScheme.menuPrimary,
                update = { scheme, color -> scheme.copy(menuPrimary = color) }
            ),
            ColorSetting(
                title = R.string.color_scheme_menu_text,
                icon = R.drawable.icon_color_menu_text,
                color = value.menuTextPrimary,
                defaultColor = defaultColorScheme.menuTextPrimary,
                update = { scheme, color -> scheme.copy(menuTextPrimary = color) }
            ),
            ColorSetting(
                title = R.string.color_scheme_back_panel,
                icon = R.drawable.icon_color_back_panel,
                color = value.backPanel,
                defaultColor = defaultColorScheme.backPanel,
                update = { scheme, color -> scheme.copy(backPanel = color) }
            ),
            ColorSetting(
                title = R.string.color_scheme_top_panel,
                icon = R.drawable.icon_color_top_panel,
                color = value.topPanel,
                defaultColor = defaultColorScheme.topPanel,
                alphaAllowed = true,
                update = { scheme, color -> scheme.copy(topPanel = color) }
            ),
            ColorSetting(
                title = R.string.color_scheme_auto_step_lock,
                icon = R.drawable.icon_color_note_switching_mode_portrait,
                color = value.autoStepLock,
                defaultColor = defaultColorScheme.autoStepLock,
                update = { scheme, color -> scheme.copy(autoStepLock = color) }
            ),
            ColorSetting(
                title = R.string.color_scheme_auto_step_lock_land,
                icon = R.drawable.icon_color_note_switching_mode_landscape,
                color = value.autoStepLockLand,
                defaultColor = defaultColorScheme.autoStepLockLand,
                update = { scheme, color -> scheme.copy(autoStepLockLand = color) }
            ),
        )
    }
}