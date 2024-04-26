package com.willeypianotuning.toneanalyzer.ui.settings.colors

import androidx.core.graphics.toColorInt
import com.willeypianotuning.toneanalyzer.sync.json.ObjectSerializer
import com.willeypianotuning.toneanalyzer.ui.colors.ColorScheme
import org.json.JSONObject
import java.util.*
import javax.inject.Inject

class ColorSchemeSerializer @Inject constructor() : ObjectSerializer<ColorScheme>() {
    override fun fromJson(json: JSONObject): ColorScheme {
        return ColorScheme(
            noteName = json.getString(KEY_NOTE_NAME).toColorInt(),
            noteNameBackground = json.getString(KEY_NOTE_NAME_BACKGROUND).toColorInt(),
            innerAndOuterRings = json.getString(KEY_INNER_AND_OUTER_RINGS).toColorInt(),
            strobeWheels = json.getString(KEY_STROBE_WHEELS).toColorInt(),
            strobeBackground = json.getString(KEY_STROBE_BACKGROUND).toColorInt(),
            dialMarkings = json.getString(KEY_DIAL_MARKINGS).toColorInt(),
            needle = json.getString(KEY_NEEDLE).toColorInt(),
            graphBackground = json.getString(KEY_GRAPH_BACKGROUND).toColorInt(),
            tuningCurveLine = json.getString(KEY_TUNING_CURVE_LINE).toColorInt(),
            tuningCurveDots = json.getString(KEY_TUNING_CURVE_DOTS).toColorInt(),
            inharmonicityLine = json.getString(KEY_INHARMONICITY_LINE).toColorInt(),
            inharmonicityDots = json.getString(KEY_INHARMONICITY_DOTS).toColorInt(),
            spectrumLine = json.getString(KEY_SPECTRUM_LINE).toColorInt(),
            currentNoteIndicator = json.getString(KEY_CURRENT_NOTE_INDICATOR).toColorInt(),
            menuPrimary = json.getString(KEY_MENU_MAIN_COLOR).toColorInt(),
            menuTextPrimary = json.getString(KEY_MENU_TEXT).toColorInt(),
            backPanel = json.getString(KEY_BACK_PANEL).toColorInt(),
            topPanel = json.getString(KEY_TOP_PANEL).toColorInt(),
            autoStepLock = json.getString(KEY_AUTO_STEP_LOCK).toColorInt(),
            autoStepLockLand = json.getString(KEY_AUTO_STEP_LOCK_LAND).toColorInt(),
        )
    }

    override fun toJson(obj: ColorScheme): JSONObject {
        val res = JSONObject()
        res.put(KEY_NOTE_NAME, obj.noteName.toHexColor())
        res.put(KEY_NOTE_NAME_BACKGROUND, obj.noteNameBackground.toHexColor())
        res.put(KEY_INNER_AND_OUTER_RINGS, obj.innerAndOuterRings.toHexColor())
        res.put(KEY_STROBE_WHEELS, obj.strobeWheels.toHexColor())
        res.put(KEY_STROBE_BACKGROUND, obj.strobeBackground.toHexColor())
        res.put(KEY_DIAL_MARKINGS, obj.dialMarkings.toHexColor())
        res.put(KEY_NEEDLE, obj.needle.toHexColor())
        res.put(KEY_GRAPH_BACKGROUND, obj.graphBackground.toHexColor())
        res.put(KEY_TUNING_CURVE_LINE, obj.tuningCurveLine.toHexColor())
        res.put(KEY_TUNING_CURVE_DOTS, obj.tuningCurveDots.toHexColor())
        res.put(KEY_INHARMONICITY_LINE, obj.inharmonicityLine.toHexColor())
        res.put(KEY_INHARMONICITY_DOTS, obj.inharmonicityDots.toHexColor())
        res.put(KEY_SPECTRUM_LINE, obj.spectrumLine.toHexColor())
        res.put(KEY_CURRENT_NOTE_INDICATOR, obj.currentNoteIndicator.toHexColor())
        res.put(KEY_MENU_MAIN_COLOR, obj.menuPrimary.toHexColor())
        res.put(KEY_MENU_TEXT, obj.menuTextPrimary.toHexColor())
        res.put(KEY_BACK_PANEL, obj.backPanel.toHexColor())
        res.put(KEY_TOP_PANEL, obj.topPanel.toHexColor())
        res.put(KEY_AUTO_STEP_LOCK, obj.autoStepLock.toHexColor())
        res.put(KEY_AUTO_STEP_LOCK_LAND, obj.autoStepLockLand.toHexColor())
        return res
    }

    private fun Int.toHexColor(): String {
        return "#${Integer.toHexString(this).padStart(8, '0').uppercase(Locale.US)}"
    }

    companion object {
        private const val KEY_NOTE_NAME = "noteName"
        private const val KEY_NOTE_NAME_BACKGROUND = "noteNameBackground"
        private const val KEY_INNER_AND_OUTER_RINGS = "innerAndOuterRings"
        private const val KEY_STROBE_WHEELS = "strobeWheels"
        private const val KEY_STROBE_BACKGROUND = "strobeBackground"
        private const val KEY_DIAL_MARKINGS = "dialMarkings"
        private const val KEY_NEEDLE = "needle"
        private const val KEY_GRAPH_BACKGROUND = "graphBackground"
        private const val KEY_TUNING_CURVE_LINE = "tuningCurveLine"
        private const val KEY_TUNING_CURVE_DOTS = "tuningCurveDots"
        private const val KEY_INHARMONICITY_LINE = "inharmonicityLine"
        private const val KEY_INHARMONICITY_DOTS = "inharmonicityDots"
        private const val KEY_SPECTRUM_LINE = "spectrumLine"
        private const val KEY_CURRENT_NOTE_INDICATOR = "currentNoteIndicator"
        private const val KEY_MENU_MAIN_COLOR = "menuMainColor"
        private const val KEY_MENU_TEXT = "menuText"
        private const val KEY_BACK_PANEL = "backPanel"
        private const val KEY_TOP_PANEL = "topPanel"
        private const val KEY_AUTO_STEP_LOCK = "autoStepLock"
        private const val KEY_AUTO_STEP_LOCK_LAND = "autoStepLockLand"
    }
}