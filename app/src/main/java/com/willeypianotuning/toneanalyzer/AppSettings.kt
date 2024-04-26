package com.willeypianotuning.toneanalyzer

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import androidx.annotation.FloatRange
import androidx.appcompat.app.AppCompatDelegate
import com.dropbox.core.oauth.DbxCredential
import com.willeypianotuning.toneanalyzer.audio.PreferredAudioInput
import com.willeypianotuning.toneanalyzer.audio.AudioInputType
import com.willeypianotuning.toneanalyzer.audio.enums.InfoBoxText
import com.willeypianotuning.toneanalyzer.audio.enums.InfoBoxTextDef
import com.willeypianotuning.toneanalyzer.audio.note_names.NoteNames
import com.willeypianotuning.toneanalyzer.generator.TrebleBassOptions
import com.willeypianotuning.toneanalyzer.ui.files.FileSortOrder
import timber.log.Timber
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * This is the wrapper around SharedPreferences, which
 * should provide a unified API for storing of the application settings
 */
@SuppressLint("ApplySharedPref")
class AppSettings(context: Context) {
    val prefs: SharedPreferences =
        context.getSharedPreferences("settings.xml", Context.MODE_PRIVATE)

    var preventSleep: Boolean
        get() = prefs.getBoolean(KEY_PREVENT_SLEEP, true)
        set(value) {
            prefs.edit()
                .putBoolean(KEY_PREVENT_SLEEP, value)
                .commit()
        }

    var appearance: Int
        get() {
            val defaultMode = AppCompatDelegate.MODE_NIGHT_NO
            if (prefs.contains(KEY_APPEARANCE)) {
                return prefs.getInt(KEY_APPEARANCE, defaultMode)
            }
            @Suppress("DEPRECATION")
            if (prefs.contains(KEY_NIGHT_MODE)) {
                val newValue = if (prefs.getBoolean(
                        KEY_NIGHT_MODE,
                        false
                    )
                ) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
                prefs.edit()
                    .putInt(KEY_APPEARANCE, newValue)
                    .remove(KEY_NIGHT_MODE)
                    .commit()
                return newValue
            }
            return defaultMode
        }
        set(value) {
            prefs.edit()
                .putInt(KEY_APPEARANCE, value)
                .commit()
        }

    var filesSortOrder: Int
        get() = prefs.getInt(KEY_FILES_SORT_ORDER, FileSortOrder.DATE_DESCENDING)
        set(value) {
            prefs.edit()
                .putInt(KEY_FILES_SORT_ORDER, value)
                .commit()
        }

    var hideNavigationBar: Boolean
        get() = prefs.getBoolean(KEY_HIDE_NAVIGATION_BAR, false)
        set(value) {
            prefs.edit()
                .putBoolean(KEY_HIDE_NAVIGATION_BAR, value)
                .commit()
        }

    var pitchOffset: Float
        get() = prefs.getFloat(KEY_PITCH_OFFSET, 0.0f)
        set(value) {
            prefs.edit()
                .putFloat(KEY_PITCH_OFFSET, value)
                .commit()
        }

    @get:FloatRange(from = 0.0, to = 1.0)
    var tonePlayerVolume: Float
        get() = prefs.getFloat(KEY_TONE_PLAYER_VOLUME, 1.0f)
        set(value) {
            prefs.edit()
                .putFloat(KEY_TONE_PLAYER_VOLUME, value)
                .commit()
        }

    var trebleBassOptions: TrebleBassOptions
        get() {
            return TrebleBassOptions(
                bassVolume = prefs.getFloat(
                    KEY_TONE_BASS_VOLUME,
                    TrebleBassOptions.DEFAULT_BASS_VOLUME
                ),
                bassEdge = prefs.getInt(
                    KEY_TONE_BASS_EDGE,
                    TrebleBassOptions.DEFAULT_BASS_EDGE.toInt()
                ).toShort(),
                trebleVolume = prefs.getFloat(
                    KEY_TONE_TREBLE_VOLUME,
                    TrebleBassOptions.DEFAULT_TREBLE_VOLUME
                ),
                trebleEdge = prefs.getInt(
                    KEY_TONE_TREBLE_EDGE,
                    TrebleBassOptions.DEFAULT_TREBLE_EDGE.toInt()
                ).toShort()
            )
        }
        set(value) {
            prefs.edit()
                .putFloat(KEY_TONE_BASS_VOLUME, value.bassVolume)
                .putInt(KEY_TONE_BASS_EDGE, value.bassEdge.toInt())
                .putFloat(KEY_TONE_TREBLE_VOLUME, value.trebleVolume)
                .putInt(KEY_TONE_TREBLE_EDGE, value.trebleEdge.toInt())
                .commit()
        }

    var pitchOffsetTargetFreq: Float
        get() = prefs.getFloat(KEY_PITCH_OFFSET_TARGET, 440.0f)
        set(value) {
            prefs.edit()
                .putFloat(KEY_PITCH_OFFSET_TARGET, value)
                .commit()
        }

    var globalPitchOffset: Double
        get() = prefs.getFloat(KEY_GLOBAL_PITCH_OFFSET, 440f).toDouble()
        set(value) {
            prefs.edit()
                .putFloat(KEY_GLOBAL_PITCH_OFFSET, value.toFloat())
                .commit()
        }

    var maximumOverpull: Int
        get() = prefs.getInt(KEY_MAXIMUM_OVERPULL, 35)
        set(value) {
            prefs.edit()
                .putInt(KEY_MAXIMUM_OVERPULL, value)
                .commit()
        }

    var preferredAudioInput: PreferredAudioInput
        get() = PreferredAudioInput(
            type = AudioInputType.entries.firstOrNull {
                it.key == prefs.getString(
                    KEY_PREFERRED_AUDIO_INPUT,
                    null
                )
            } ?: AudioInputType.EXTERNAL_MIC,
            allowBluetooth = prefs.getBoolean(KEY_ALLOW_BLUETOOTH_AUDIO_INPUT, false)
        )
        set(value) {
            prefs.edit()
                .putString(KEY_PREFERRED_AUDIO_INPUT, value.type.key)
                .putBoolean(KEY_ALLOW_BLUETOOTH_AUDIO_INPUT, value.allowBluetooth)
                .commit()
        }

    var pitchRaiseOvershootFactor: Double
        get() = prefs.getFloat(KEY_PITCH_RAISE_OVERSHOOT_FACTOR, 1.0f).toDouble()
        set(value) {
            prefs.edit()
                .putFloat(KEY_PITCH_RAISE_OVERSHOOT_FACTOR, value.toFloat())
                .commit()
        }

    var inharmonicityWeight: Float
        get() = prefs.getFloat(KEY_INHARMONICITY_WEIGHT, 0.75f)
        set(value) {
            prefs.edit()
                .putFloat(KEY_INHARMONICITY_WEIGHT, value)
                .commit()
        }

    var useCents: Boolean
        get() = prefs.getBoolean(KEY_USE_CENTS, true)
        set(value) {
            prefs.edit()
                .putBoolean(KEY_USE_CENTS, value)
                .commit()
        }

    var colorSchemeJson: String
        get() = requireNotNull(prefs.getString(KEY_COLOR_SCHEME_JSON, ""))
        set(value) {
            prefs.edit()
                .putString(KEY_COLOR_SCHEME_JSON, value)
                .commit()
        }

    @InfoBoxTextDef
    var infoBoxText: Int
        get() = prefs.getInt(
            KEY_INFO_BOX_TEXT,
            InfoBoxText.MAKE or InfoBoxText.MODEL or InfoBoxText.PITCH_OFFSET
        )
        set(value) {
            prefs.edit()
                .putInt(KEY_INFO_BOX_TEXT, value)
                .commit()
        }

    var noteNames: Int
        get() = prefs.getInt(KEY_NOTE_NAMES, NoteNames.LOCALE)
        set(value) {
            prefs.edit()
                .putInt(KEY_NOTE_NAMES, value)
                .commit()
        }

    var tonePlayerDuration: Long
        get() = prefs.getLong(KEY_TONE_PLAYER_DURATION, 1_000L)
        set(value) {
            prefs.edit()
                .putLong(KEY_TONE_PLAYER_DURATION, value)
                .commit()
        }

    /**
     * Use #currentTuningId
     * [Deprecation Notice] Previously tunings were stored in separate tuning files
     * but loading those took a lot of time. So they were replaced with the database.
     * This method is only used for migration
     */
    @Deprecated("")
    fun currentFile(): String? {
        return prefs.getString(KEY_CURRENT_FILE, null)
    }

    @Deprecated("")
    fun clearCurrentFile() {
        prefs.edit()
            .remove(KEY_CURRENT_FILE)
            .commit()
    }

    var lastBackupDate: Date?
        get() {
            val timestamp = prefs.getLong(KEY_LAST_BACKUP_DATE, 0)
            return if (timestamp == 0L) {
                null
            } else Date(timestamp)
        }
        set(value) {
            if (value == null) {
                prefs.edit().remove(KEY_LAST_BACKUP_DATE).commit()
            } else {
                prefs.edit()
                    .putLong(KEY_LAST_BACKUP_DATE, value.time)
                    .commit()
            }
        }

    /**
     * Returns backup repeat interval
     * if 0, no backup should be done
     * values larger than zero considered to be backup interval
     */
    fun backupRepeatInterval(): Long {
        return prefs.getLong(KEY_BACKUP_REPEAT_INTERVAL, DEFAULT_BACKUP_REPEAT_INTERVAL)
    }

    fun automaticBackupEnabled(): Boolean {
        return backupRepeatInterval() > 0
    }

    fun setBackupRepeatInterval(backupRepeatInterval: Long) {
        prefs.edit()
            .putLong(KEY_BACKUP_REPEAT_INTERVAL, backupRepeatInterval)
            .commit()
    }

    var currentTuningId: String?
        get() = prefs.getString(KEY_CURRENT_TUNING_ID, null)
        set(value) {
            prefs.edit()
                .putString(KEY_CURRENT_TUNING_ID, value)
                .commit()
        }

    @Deprecated("Use dropboxCredentials instead")
    val dropboxAccessToken: String?
        get() = prefs.getString(KEY_DROPBOX_TOKEN, null)

    var dropboxCredentials: DbxCredential?
        get() {
            return kotlin.runCatching {
                val serializedCredentials = prefs.getString(KEY_DROPBOX_CREDENTIALS, null)
                    ?: return@runCatching null
                DbxCredential.Reader.readFully(serializedCredentials)
            }
                .onFailure { Timber.e(it, "Dropbox Credentials is data corrupted") }
                .getOrNull()
        }
        set(value) {
            // when new token is set, remove old token
            val editor = prefs.edit().remove(KEY_DROPBOX_TOKEN)
            if (value != null) {
                editor.putString(KEY_DROPBOX_CREDENTIALS, value.toString())
            } else {
                editor.remove(KEY_DROPBOX_CREDENTIALS).commit()
            }
            editor.commit()
        }

    var tuningStyleId: String?
        get() = prefs.getString(KEY_TUNING_STYLE_ID, null)
        set(value) {
            prefs.edit()
                .putString(KEY_TUNING_STYLE_ID, value)
                .commit()
        }

    var tuningFilesToDbMigrationDone: Boolean
        get() = prefs.getBoolean(KEY_TUNING_FILES_TO_DB_MIGRATION_DONE, false)
        set(value) {
            prefs.edit()
                .putBoolean(KEY_TUNING_FILES_TO_DB_MIGRATION_DONE, value)
                .commit()
        }

    var temperamentsToDbMigrationDone: Boolean
        get() = prefs.getBoolean(KEY_TEMPERAMENTS_TO_DB_MIGRATION_DONE, false)
        set(value) {
            prefs.edit()
                .putBoolean(KEY_TEMPERAMENTS_TO_DB_MIGRATION_DONE, value)
                .commit()
        }

    var tuningStylesToDbMigrationDone: Boolean
        get() = prefs.getBoolean(KEY_TUNING_STYLES_TO_DB_MIGRATION_DONE, false)
        set(value) {
            prefs.edit()
                .putBoolean(KEY_TUNING_STYLES_TO_DB_MIGRATION_DONE, value)
                .commit()
        }

    fun noteSwitchTrigger(): Boolean {
        return prefs.getInt(KEY_NOTE_SWITCH_TRIGGER, 0) > 0
    }

    fun setFreeVersionNotificationShown(shown: Boolean) {
        prefs.edit()
            .putBoolean(KEY_FREE_VERSION_NOTIFICATION_SHOWN, shown)
            .commit()
    }

    fun freeVersionNotificationShown(): Boolean {
        return prefs.getBoolean(KEY_FREE_VERSION_NOTIFICATION_SHOWN, false)
    }

    fun setNoteSwitchTrigger(done: Boolean) {
        prefs.edit()
            .putInt(KEY_NOTE_SWITCH_TRIGGER, if (done) 1 else 0)
            .commit()
    }

    var lastPitchRaiseKeys: List<Int>
        get() {
            return (prefs.getString(KEY_LAST_PITCH_RAISE_KEYS, "") ?: "")
                .split(",").mapNotNull { it.toIntOrNull() }.toList()
        }
        set(value) {
            prefs.edit()
                .putString(KEY_LAST_PITCH_RAISE_KEYS, value.joinToString(","))
                .commit()
        }

    companion object {
        private val DEFAULT_BACKUP_REPEAT_INTERVAL = TimeUnit.DAYS.toMillis(7)
        private const val KEY_PREVENT_SLEEP = "PreventSleep"
        const val KEY_APPEARANCE = "Appearance"

        @Deprecated("Replaced with Appearance")
        private const val KEY_NIGHT_MODE = "NightMode"

        private const val KEY_HIDE_NAVIGATION_BAR = "HideNavigationBar"
        private const val KEY_PITCH_OFFSET = "CalibrationPitchOffset"
        private const val KEY_TONE_PLAYER_VOLUME = "TonePlayerVolume"
        private const val KEY_TONE_BASS_VOLUME = "TonePlayerBassVolume"
        private const val KEY_TONE_BASS_EDGE = "TonePlayerBassEdge"
        private const val KEY_TONE_TREBLE_VOLUME = "TonePlayerTrebleVolume"
        private const val KEY_TONE_TREBLE_EDGE = "TonePlayerTrebleEdge"
        private const val KEY_PITCH_OFFSET_TARGET = "CalibrationPitchOffsetTarget"
        private const val KEY_MAXIMUM_OVERPULL = "MaxOverpullCents"
        private const val KEY_PITCH_RAISE_OVERSHOOT_FACTOR = "PitchRaiseOvershootFactor"
        private const val KEY_INHARMONICITY_WEIGHT = "InharmonicityWeight"
        private const val KEY_INFO_BOX_TEXT = "displayText"
        private const val KEY_NOTE_NAMES = "noteNames"
        private const val KEY_TONE_PLAYER_DURATION = "tonePlayerDuration"
        private const val KEY_GLOBAL_PITCH_OFFSET = "GlobalPitchOffset"
        private const val KEY_CURRENT_FILE = "CurrentFile"
        private const val KEY_CURRENT_TUNING_ID = "CurrentTuningId"
        private const val KEY_TUNING_STYLE_ID = "TuningStyleId"
        private const val KEY_USE_CENTS = "UseCents"
        private const val KEY_FREE_VERSION_NOTIFICATION_SHOWN = "FreeVersionNotificationShown"
        private const val KEY_FILES_SORT_ORDER = "FilesSortOrder"
        private const val KEY_TUNING_FILES_TO_DB_MIGRATION_DONE = "TuningFilesToDbMigrationDone"
        private const val KEY_TEMPERAMENTS_TO_DB_MIGRATION_DONE = "TemperamentsToDbMigrationDone"
        private const val KEY_TUNING_STYLES_TO_DB_MIGRATION_DONE = "TuningStylesToDbMigrationDone"
        private const val KEY_LAST_BACKUP_DATE = "LastBackupDate"

        @Deprecated("Kept for backward compatibility. Used to store now deprecated Dropbox Long-Lived Token")
        private const val KEY_DROPBOX_TOKEN = "DropBoxAccessToken"

        private const val KEY_DROPBOX_CREDENTIALS = "DropBoxCredentials"
        private const val KEY_PREFERRED_AUDIO_INPUT = "PreferredAudioInput"
        private const val KEY_ALLOW_BLUETOOTH_AUDIO_INPUT = "AllowBluetoothAudioInput"
        private const val KEY_BACKUP_REPEAT_INTERVAL = "BackupRepeatInterval"
        private const val KEY_NOTE_SWITCH_TRIGGER = "NoteSwitch"
        private const val KEY_LAST_PITCH_RAISE_KEYS = "LastPitchRaiseKeys"
        private const val KEY_COLOR_SCHEME_JSON = "ColorSchemeJson"
    }

}