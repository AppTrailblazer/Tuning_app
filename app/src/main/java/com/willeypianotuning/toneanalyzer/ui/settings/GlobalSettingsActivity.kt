package com.willeypianotuning.toneanalyzer.ui.settings

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import android.content.SharedPreferences
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.os.Build
import android.os.Bundle
import android.text.Spanned
import android.text.TextUtils
import android.view.Gravity
import android.view.View
import android.widget.CompoundButton
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import android.widget.TextView
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.core.widget.TextViewCompat
import com.willeypianotuning.toneanalyzer.R
import com.willeypianotuning.toneanalyzer.audio.AudioInputType
import com.willeypianotuning.toneanalyzer.audio.TuningStyleHelper
import com.willeypianotuning.toneanalyzer.audio.note_names.NoteNames.getNamingConvention
import com.willeypianotuning.toneanalyzer.audio.note_names.NoteNames.noteNamingConventions
import com.willeypianotuning.toneanalyzer.billing.InAppPurchase
import com.willeypianotuning.toneanalyzer.databinding.ActivityGlobalSettingsBinding
import com.willeypianotuning.toneanalyzer.ui.calibration.CalibrateActivity
import com.willeypianotuning.toneanalyzer.ui.commons.BaseActivity
import com.willeypianotuning.toneanalyzer.ui.settings.PitchOffsetDialog.Companion.convertFreqToCents
import com.willeypianotuning.toneanalyzer.ui.settings.PitchOffsetDialog.OnValueEnteredListener
import com.willeypianotuning.toneanalyzer.ui.settings.backups.DropBoxBackupRestoreActivity
import com.willeypianotuning.toneanalyzer.ui.settings.colors.ColorSchemeActivity
import com.willeypianotuning.toneanalyzer.ui.settings.weights.AdjustWeightsActivity
import com.willeypianotuning.toneanalyzer.ui.upgrade.UpgradeActivity
import com.willeypianotuning.toneanalyzer.utils.LocaleUtils
import dagger.hilt.android.AndroidEntryPoint
import java.util.*
import javax.inject.Inject

@AndroidEntryPoint
class GlobalSettingsActivity : BaseActivity() {
    private val noteNames: Array<Spanned> by lazy { noteNamingConventions(this) }

    private val settingsChangeListener =
        OnSharedPreferenceChangeListener { _: SharedPreferences?, _: String? -> setLabels() }

    private lateinit var binding: ActivityGlobalSettingsBinding

    private val appearanceOptions = listOf(
        R.string.global_settings_appearance_auto to AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM,
        R.string.global_settings_appearance_light to AppCompatDelegate.MODE_NIGHT_NO,
        R.string.global_settings_appearance_dark to AppCompatDelegate.MODE_NIGHT_YES,
        R.string.global_settings_appearance_custom to AppCompatDelegate.MODE_NIGHT_UNSPECIFIED
    )

    private val preferredAudioInput = listOf(
        R.string.dialog_audio_input_option_built_in_mic to AudioInputType.BUILT_IN_MIC,
        R.string.dialog_audio_input_option_external_wired_mic to AudioInputType.EXTERNAL_MIC,
    )

    private val appLocales: Array<Locale> by lazy {
        val locales = LocaleUtils.getLocaleListFromXml(this)
        Array(locales.size()) { locales[it]!! }
    }

    private val activeLocale: Locale
        get() {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                resources.configuration.locales.get(0)
            } else {
                resources.configuration.locale
            }
        }

    @Inject
    lateinit var tuningStyleHelper: TuningStyleHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGlobalSettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupToolbar()
        setTitle(R.string.activity_global_settings_title)

        binding.listItemSettingPreventSleep.setOnClickListener {
            binding.preventSleepSwitch.isChecked = !binding.preventSleepSwitch.isChecked
        }
        binding.listItemSettingHideNavigationBar.setOnClickListener {
            binding.hideNavigationBarSwitch.isChecked = !binding.hideNavigationBarSwitch.isChecked
        }
        binding.listItemSettingAppearance.setOnClickListener { onAppearancePressed() }
        binding.listItemSettingNoteNames.setOnClickListener { onNoteNamesPressed() }
        binding.listItemSettingShowInfoBox.setOnClickListener { onShowInfoBoxPressed() }
        binding.listItemSettingMaximumOverpull.setOnClickListener { onMaxOverpullPressed() }
        binding.listItemSettingInharmonicityWeight.setOnClickListener { onInharmonicityPressed() }
        binding.listItemSettingGlobalPitchOffset.setOnClickListener { onGlobalPitchOffsetPressed() }
        binding.listItemSettingPitchRaiseOvershootFactor.setOnClickListener { onPitchRaiseOvershootFactorClicked() }
        binding.listItemSettingAppLanguage.visibility = View.VISIBLE
        binding.listItemSettingAppLanguage.setOnClickListener { onAppLanguageClicked() }
        binding.listItemSettingCalibration.setOnClickListener { onRecalibratePressed() }
        binding.listItemSettingAdjustWeights.setOnClickListener { onAdjustWeightsClicked() }
        binding.listItemSettingBackupDropbox.setOnClickListener { onBackupToDropboxClicked() }
        binding.listItemSettingAudioInput.setOnClickListener { onShowAudioInputSelector() }
        setLabels()
    }

    private fun onAppLanguageClicked() {
        val selectedIndex = appLocales.indexOfFirst { it.language == activeLocale.language }
        val dialog = AlertDialog.Builder(this)
        dialog.setTitle(getString(R.string.global_settings_language))
            .setSingleChoiceItems(
                appLocales.map { it.localizedDisplayName }.toTypedArray(),
                selectedIndex
            ) { dialogInterface: DialogInterface, which: Int ->
                if (selectedIndex == which) {
                    dialogInterface.dismiss()
                    return@setSingleChoiceItems
                }

                LocaleUtils.setLocale(appLocales[which])
            }
            .show()
    }

    private fun onAdjustWeightsClicked() {
        if (isPro) {
            val intent = Intent(this, AdjustWeightsActivity::class.java)
            startActivity(intent)
        } else {
            startActivity(Intent(this, UpgradeActivity::class.java))
        }
    }

    override fun onStart() {
        super.onStart()
        appSettings.prefs.registerOnSharedPreferenceChangeListener(settingsChangeListener)
        updateUi()
    }

    override fun onPurchasesUpdated(purchases: List<InAppPurchase>) {
        super.onPurchasesUpdated(purchases)
        updateUi()
    }

    private fun updateUi() {
        binding.itemLock.isGone = isPlus
        binding.itemPlus.isGone = isPlus
        binding.listItemSettingGlobalPitchOffset.isVisible = isPro
        binding.listItemSettingBackupDropboxLock.isGone = isPro
        binding.listItemSettingBackupDropboxLockPlan.isGone = isPro
        binding.listItemSettingAdjustWeightsLockIcon.isGone = isPro
        binding.listItemSettingAdjustWeightsLockLabel.isGone = isPro
        binding.listItemSettingAudioInput.isVisible = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
        binding.listItemSettingAudioInputDivider.isVisible =
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
    }

    override fun onStop() {
        super.onStop()
        appSettings.prefs.unregisterOnSharedPreferenceChangeListener(settingsChangeListener)
    }

    public override fun onResume() {
        super.onResume()
        setLabels()
    }

    private fun onBackupToDropboxClicked() {
        if (isPro) {
            startActivity(Intent(this, DropBoxBackupRestoreActivity::class.java))
        } else {
            startActivity(Intent(this, UpgradeActivity::class.java))
        }
    }

    private fun setLabels() {
        binding.preventSleepSwitch.setOnCheckedChangeListener(null)
        binding.hideNavigationBarSwitch.setOnCheckedChangeListener(null)
        binding.preventSleepSwitch.isChecked = appSettings.preventSleep
        binding.hideNavigationBarSwitch.isChecked = appSettings.hideNavigationBar
        updateAppearanceLabel()
        updatePitchOffsetLabel()
        binding.textViewSettingCalibration.text =
            getString(R.string.global_settings_calibration, appSettings.pitchOffset)
        binding.textViewSettingMaximumOverpull.text =
            getString(R.string.global_settings_maximum_overpull, appSettings.maximumOverpull)
        binding.textViewSettingTuningStyle.text = getString(
            R.string.global_settings_adjust_weights,
            tuningStyleHelper.getGlobalIntervalWeights().name
        )
        binding.textViewSettingInharmonicityWeight.text = getString(
            R.string.global_settings_inharmonicity_weight_factor,
            appSettings.inharmonicityWeight
        )
        binding.textViewSettingPitchRaiseOvershootFactor.text = getString(
            R.string.global_settings_pitch_raise_overshoot_factor,
            appSettings.pitchRaiseOvershootFactor
        )
        updateInfoBoxLabel()
        binding.textViewSettingNoteNames.text = TextUtils.concat(
            getString(R.string.global_settings_note_names) + ":" + " ",
            noteNames[appSettings.noteNames]
        )
        @SuppressLint("SetTextI18n")
        binding.textViewSettingAppLanguage.text =
            "${getString(R.string.global_settings_language)}: ${
                activeLocale.localizedDisplayName
            }"
        binding.textViewSettingAudioInput.text = getString(
            R.string.global_settings_audio_input,
            getString(preferredAudioInput.first { it.second == appSettings.preferredAudioInput.type }.first)
        )
        binding.preventSleepSwitch.setOnCheckedChangeListener { _: CompoundButton?, isChecked: Boolean ->
            appSettings.preventSleep = isChecked
        }
        binding.hideNavigationBarSwitch.setOnCheckedChangeListener { _: CompoundButton?, isChecked: Boolean ->
            appSettings.hideNavigationBar = isChecked
        }
    }

    private fun updateInfoBoxLabel() {
        val text = mutableListOf<String>()
        val labels = resources.getStringArray(R.array.info_box_labels)
        for (i in labels.indices) {
            if (appSettings.infoBoxText and (1 shl i) != 0) {
                text.add(labels[i])
            }
        }
        binding.textViewSettingShowInfoBox.text =
            getString(R.string.global_settings_show_in_info_box, TextUtils.join(", ", text))
    }

    private fun updatePitchOffsetLabel() {
        val pitchHertz = appSettings.globalPitchOffset
        val pitchCents = convertFreqToCents(pitchHertz)
        binding.textViewSettingGlobalPitchOffset.text = String.format(
            "%s: %s= %s %s",
            getString(R.string.global_settings_global_pitch_offset),
            getNamingConvention(this, appSettings.noteNames).noteName(0),
            getString(R.string.pitch_offset_summary_hz, pitchHertz),
            getString(R.string.pitch_offset_summary_cents, pitchCents)
        )
    }

    private fun updateAppearanceLabel() {
        val selectedAppearanceResId =
            appearanceOptions.firstOrNull { it.second == appSettings.appearance }?.first
                ?: R.string.global_settings_appearance_auto

        binding.textViewSettingAppearance.text = String.format(
            "%s %s",
            TextUtils.concat(getString(R.string.global_settings_appearance) + ":"),
            getString(selectedAppearanceResId)
        )
    }

    private fun onAppearancePressed() {
        val dialog = AlertDialog.Builder(this)
        val selected = appearanceOptions.indexOfFirst { it.second == appSettings.appearance }
        dialog.setTitle(getString(R.string.global_settings_appearance))
            .setSingleChoiceItems(
                appearanceOptions.map { getString(it.first) }.toTypedArray(),
                selected
            ) { _: DialogInterface?, which: Int ->
                appSettings.appearance = appearanceOptions[which].second
            }
            .setPositiveButton(android.R.string.ok) { _: DialogInterface?, _: Int ->
                setLabels()
                notifyNightModeChange()
                if (appSettings.appearance == AppCompatDelegate.MODE_NIGHT_UNSPECIFIED) {
                    startActivity(Intent(this, ColorSchemeActivity::class.java))
                }
            }
            .show()
    }

    private fun onRecalibratePressed() {
        if (isPlus) {
            startActivity(Intent(this, CalibrateActivity::class.java))
        } else {
            startActivity(Intent(this, UpgradeActivity::class.java))
        }
    }

    private fun onGlobalPitchOffsetPressed() {
        val pitchOffsetDialog = PitchOffsetDialog(this)
        pitchOffsetDialog.onValueEnteredListener = OnValueEnteredListener { pitch, _ ->
            appSettings.globalPitchOffset = pitch
            setLabels()
        }
        pitchOffsetDialog.show(
            getString(R.string.global_settings_global_pitch_offset),
            getString(R.string.global_settings_global_pitch_offset_message),
            appSettings.globalPitchOffset
        )
    }

    private fun onMaxOverpullPressed() {
        val overpullDialog = AlertDialog.Builder(this)
        val layout = LinearLayout(this)
        layout.orientation = LinearLayout.VERTICAL
        val centsLabel = TextView(this)
        val params = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT
        )
        params.gravity = Gravity.CENTER
        centsLabel.layoutParams = params
        TextViewCompat.setTextAppearance(centsLabel, android.R.style.TextAppearance_Medium)
        centsLabel.text = String.format(
            Locale.getDefault(),
            "%d %s",
            appSettings.maximumOverpull,
            getString(R.string.dimension_cents)
        )
        val seekBar = SeekBar(this)
        // 0 - 25 progress values map to 25 - 50 cents
        seekBar.max = 25
        seekBar.progress = appSettings.maximumOverpull - 25
        seekBar.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                val newMaximumOverpull = progress + 25
                appSettings.maximumOverpull = newMaximumOverpull
                centsLabel.text = String.format(
                    Locale.getDefault(),
                    "%d %s",
                    newMaximumOverpull,
                    getString(R.string.dimension_cents)
                )
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {}
        })
        layout.addView(centsLabel)
        layout.addView(seekBar)
        overpullDialog.setTitle(getString(R.string.dialog_change_maximum_overpull_title))
            .setMessage(getString(R.string.dialog_change_maximum_overpull_message))
            .setPositiveButton(android.R.string.ok) { _: DialogInterface?, _: Int -> setLabels() }
            .setView(layout)
            .show()
    }

    private fun onPitchRaiseOvershootFactorClicked() {
        val overpullDialog = AlertDialog.Builder(this)
        val layout = LinearLayout(this)
        layout.orientation = LinearLayout.VERTICAL
        val centsLabel = TextView(this)
        val params = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT
        )
        params.gravity = Gravity.CENTER
        centsLabel.layoutParams = params
        TextViewCompat.setTextAppearance(centsLabel, android.R.style.TextAppearance_Medium)
        centsLabel.text =
            String.format(Locale.getDefault(), "%.2f", appSettings.pitchRaiseOvershootFactor)
        val seekBar = SeekBar(this)
        // 0 - 100 progress values map to 0.5 - 1.5
        seekBar.max = 100
        seekBar.progress = (100 * appSettings.pitchRaiseOvershootFactor).toInt() - 50
        seekBar.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                val newPitchRaiseOvershootFactor = 0.5 + (progress * 1.0 / 100)
                appSettings.pitchRaiseOvershootFactor = newPitchRaiseOvershootFactor
                centsLabel.text =
                    String.format(Locale.getDefault(), "%.2f", newPitchRaiseOvershootFactor)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {}
        })
        layout.addView(centsLabel)
        layout.addView(seekBar)
        overpullDialog.setTitle(getString(R.string.dialog_pitch_raise_overshoot_factor_title))
            .setMessage(getString(R.string.dialog_pitch_raise_overshoot_factor_message))
            .setPositiveButton(android.R.string.ok) { _: DialogInterface?, _: Int -> setLabels() }
            .setView(layout)
            .show()
    }

    private fun onInharmonicityPressed() {
        val inharmonicityDialog = AlertDialog.Builder(this)
        val layout = LinearLayout(this)
        layout.orientation = LinearLayout.VERTICAL
        val weightLabel = TextView(this)
        val params = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT
        )
        params.gravity = Gravity.CENTER
        weightLabel.layoutParams = params
        TextViewCompat.setTextAppearance(weightLabel, android.R.style.TextAppearance_Medium)
        weightLabel.text =
            String.format(Locale.getDefault(), "%.2f", appSettings.inharmonicityWeight)
        val seekBar = SeekBar(this)
        // 0 - 70 progress values map to 0.25 - 0.95
        seekBar.max = 70
        seekBar.progress = (100 * appSettings.inharmonicityWeight).toInt() - 25
        seekBar.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                val newWeight = (progress + 25) / 100.0f
                appSettings.inharmonicityWeight = newWeight
                weightLabel.text = String.format(Locale.getDefault(), "%.2f", newWeight)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {}
        })
        layout.addView(weightLabel)
        layout.addView(seekBar)
        inharmonicityDialog.setTitle(getString(R.string.dialog_change_inharmonicity_weight_factor_title))
            .setMessage(getString(R.string.dialog_change_inharmonicity_weight_factor_message))
            .setPositiveButton(android.R.string.ok) { _: DialogInterface?, _: Int -> setLabels() }
            .setView(layout)
            .show()
    }

    private fun onShowInfoBoxPressed() {
        val dialog = AlertDialog.Builder(this)
        val labels = resources.getStringArray(R.array.info_box_labels)
        val checked = BooleanArray(labels.size)
        val infoBoxText = appSettings.infoBoxText
        for (i in checked.indices) {
            checked[i] = infoBoxText and (1 shl i) != 0
        }
        dialog.setTitle(getString(R.string.dialog_show_in_info_box_title))
            .setMultiChoiceItems(
                labels,
                checked
            ) { _: DialogInterface?, which: Int, isChecked: Boolean ->
                var newInfoBoxText = appSettings.infoBoxText
                newInfoBoxText = if (isChecked) {
                    newInfoBoxText or (1 shl which)
                } else {
                    newInfoBoxText and (1 shl which).inv()
                }
                appSettings.infoBoxText = newInfoBoxText
            }
            .setPositiveButton(android.R.string.ok) { _: DialogInterface?, _: Int -> setLabels() }
            .show()
    }

    private fun onShowAudioInputSelector() {
        val dialog = AudioInputDialog(
            this,
            appSettings.preferredAudioInput
        ) {
            appSettings.preferredAudioInput = it
            setLabels()
        }
        dialog.show()
    }

    private fun onNoteNamesPressed() {
        val dialog = AlertDialog.Builder(this)
        dialog.setTitle(getString(R.string.dialog_note_names_title))
            .setSingleChoiceItems(
                noteNames,
                appSettings.noteNames
            ) { _: DialogInterface?, which: Int -> appSettings.noteNames = which }
            .setPositiveButton(android.R.string.ok) { _: DialogInterface?, _: Int -> setLabels() }
            .show()
    }
}

private val Locale.localizedDisplayName: String
    get() = getDisplayLanguage(this).replaceFirstChar { if (it.isLowerCase()) it.titlecase(this) else it.toString() }