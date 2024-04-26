package com.willeypianotuning.toneanalyzer.ui.main

import android.annotation.SuppressLint
import android.content.DialogInterface
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.*
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.tracing.trace
import com.willeypianotuning.toneanalyzer.R
import com.willeypianotuning.toneanalyzer.ToneDetectorWrapper
import com.willeypianotuning.toneanalyzer.audio.AudioRecorder
import com.willeypianotuning.toneanalyzer.audio.PitchRaiseOptions
import com.willeypianotuning.toneanalyzer.audio.TuningStyleHelper
import com.willeypianotuning.toneanalyzer.audio.enums.NoteChangeMode
import com.willeypianotuning.toneanalyzer.audio.enums.PitchRaiseMode
import com.willeypianotuning.toneanalyzer.audio.note_names.NoteNames
import com.willeypianotuning.toneanalyzer.billing.InAppPurchase
import com.willeypianotuning.toneanalyzer.billing.PurchaseRestoreTask
import com.willeypianotuning.toneanalyzer.billing.security.AntiTamper
import com.willeypianotuning.toneanalyzer.databinding.ActivityMainBinding
import com.willeypianotuning.toneanalyzer.extensions.dpToPx
import com.willeypianotuning.toneanalyzer.extensions.hasAudioPermission
import com.willeypianotuning.toneanalyzer.extensions.keepScreenOn
import com.willeypianotuning.toneanalyzer.extensions.setDebounceOnClickListener
import com.willeypianotuning.toneanalyzer.extensions.withAudioPermission
import com.willeypianotuning.toneanalyzer.generator.TonePlayer
import com.willeypianotuning.toneanalyzer.spinners.TuningSpinnerHandler
import com.willeypianotuning.toneanalyzer.store.PianoTuningDataStore
import com.willeypianotuning.toneanalyzer.store.db.tunings.PianoTuning
import com.willeypianotuning.toneanalyzer.ui.colors.ColorFilter
import com.willeypianotuning.toneanalyzer.ui.colors.ColorScheme
import com.willeypianotuning.toneanalyzer.ui.commons.BaseActivity
import com.willeypianotuning.toneanalyzer.ui.commons.DoNotShowAgain
import com.willeypianotuning.toneanalyzer.ui.commons.InfoDialog
import com.willeypianotuning.toneanalyzer.ui.files.FilesActivity
import com.willeypianotuning.toneanalyzer.ui.help.HelpActivity
import com.willeypianotuning.toneanalyzer.ui.main.MainUiHelper.configurePanels
import com.willeypianotuning.toneanalyzer.ui.main.charts.MainChartsManager
import com.willeypianotuning.toneanalyzer.ui.main.charts.MainChartsManager.OnChartChangedListener
import com.willeypianotuning.toneanalyzer.ui.main.charts.MainChartsManager.OnChartLongClickListener
import com.willeypianotuning.toneanalyzer.ui.main.dialogs.ToneDurationDialog
import com.willeypianotuning.toneanalyzer.ui.main.dialogs.TonePlayerLevelingDialog
import com.willeypianotuning.toneanalyzer.ui.main.menu.MainMenu
import com.willeypianotuning.toneanalyzer.ui.main.menu.MainMenuItem
import com.willeypianotuning.toneanalyzer.ui.main.menu.OnMainMenuItemClickListener
import com.willeypianotuning.toneanalyzer.ui.main.swipe.OnSwipeTouchListener
import com.willeypianotuning.toneanalyzer.ui.main.tasks.AutomaticTuningSaver
import com.willeypianotuning.toneanalyzer.ui.main.tasks.PianoTuningInitializer
import com.willeypianotuning.toneanalyzer.ui.main.tasks.UpdatePianoTuningTask
import com.willeypianotuning.toneanalyzer.ui.main.views.DisabledKeyProvider
import com.willeypianotuning.toneanalyzer.ui.main.views.PianoKeyboardView.PianoKeyClickListener
import com.willeypianotuning.toneanalyzer.ui.pitch_raise.PitchRaiseConfigManager
import com.willeypianotuning.toneanalyzer.ui.pitch_raise.PitchRaiseConfigManager.OnPitchRaiseConfigReadyListener
import com.willeypianotuning.toneanalyzer.ui.settings.GlobalSettingsActivity
import com.willeypianotuning.toneanalyzer.ui.settings.colors.ColorSchemeProvider
import com.willeypianotuning.toneanalyzer.ui.tuning.TuningSettingsActivity
import com.willeypianotuning.toneanalyzer.ui.upgrade.UpgradeActivity
import com.willeypianotuning.toneanalyzer.utils.Hardware.checkAudioHardwareAsync
import com.willeypianotuning.toneanalyzer.utils.IntentUtils.openPlayStoreSubscriptions
import com.willeypianotuning.toneanalyzer.utils.IntentUtils.startActivitySafe
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.util.*
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject
import kotlin.math.abs
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.sin

@AndroidEntryPoint
class MainActivity : BaseActivity(), OnMainMenuItemClickListener, OnPitchRaiseConfigReadyListener {
    @Inject
    lateinit var analyzerWrapper: ToneDetectorWrapper

    @Inject
    lateinit var automaticTuningSaver: AutomaticTuningSaver

    @Inject
    lateinit var antiTamper: AntiTamper

    @Inject
    lateinit var audioRecorder: AudioRecorder

    @Inject
    lateinit var pianoTuningDataStore: PianoTuningDataStore

    @Inject
    lateinit var pitchRaiseConfigManager: PitchRaiseConfigManager

    @Inject
    lateinit var tuningStyleHelper: TuningStyleHelper

    @Inject
    lateinit var updatePianoTuningTask: UpdatePianoTuningTask

    @Inject
    lateinit var pianoTuningInitializer: PianoTuningInitializer

    @Inject
    lateinit var purchaseRestoreTask: PurchaseRestoreTask

    @Inject
    lateinit var infoBoxUpdatesHandler: InfoBoxUpdatesHandler

    @Inject
    lateinit var colorSchemeProvider: ColorSchemeProvider

    @Inject
    lateinit var tonePlayer: TonePlayer

    // timer to update fft graph periodically
    private val mainHandler = Handler(Looper.getMainLooper())

    private val spinnerHandler: TuningSpinnerHandler by lazy {
        TuningSpinnerHandler(
            noteLock,
            analyzerWrapper
        )
    }

    private lateinit var binding: ActivityMainBinding

    private lateinit var mainMenu: MainMenu

    private val chartsManager: MainChartsManager by lazy { MainChartsManager(binding.chartsLayout) }

    private val noteLock = NoteLock()

    private var freeVersionNotificationDialog: AlertDialog? = null

    private val chartsUpdateRunnable = Runnable {
        trace("updateCharts") {
            updateCharts()
        }
        scheduleChartsUpdate(500)
    }

    private val actionOpenTuningFile = registerForActivityResult(
        FilesActivity.Contract(),
        ::onOpenTuningFileResultReceived
    )

    private val actionConfigureNewTuning = registerForActivityResult(
        TuningSettingsActivity.Contract(),
        ::onConfigureNewTuningResultReceived
    )

    private val actionConfigureExistingTuning = registerForActivityResult(
        TuningSettingsActivity.Contract(),
        ::onConfigureExistingTuningResultReceived
    )

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.noteInfo.setNoteNaming(appSettings.noteNames)
        chartsManager.restoreState(savedInstanceState)
        chartsManager.onChartLongClickListener = OnChartLongClickListener { chartIndex: Int ->
            when (chartIndex) {
                1 -> showFrequencyChartCleanupMenu()
                2 -> showInharmonicityChartCleanupMenu()
            }
        }
        chartsManager.onChartChangedListener = OnChartChangedListener { chartIndex: Int ->
            updateCharts()
            showToast(chartsManager.chartName(chartIndex), true)
        }
        binding.modesLayout.onValueChangeListener = { mode: Int ->
            applyNoteChangeMode(mode)
        }
        binding.modesLayout.setMode(analyzerWrapper.noteDetectMode)
        configureLockedNotes()
        binding.lockInharmonicityButton.setDebounceOnClickListener { onLockClick() }
        binding.pitchRaiseModeButton.setDebounceOnClickListener { onPitchRaiseModeButtonClicked() }
        binding.prevOctaveButton.setDebounceOnClickListener { changeNoteButtonClicked(it, -12) }
        binding.prevNoteButton.setDebounceOnClickListener { changeNoteButtonClicked(it, -1) }
        binding.nextNoteButton.setDebounceOnClickListener { changeNoteButtonClicked(it, 1) }
        binding.nextOctaveButton.setDebounceOnClickListener { changeNoteButtonClicked(it, 12) }
        binding.topSink.setOnClickListener { showPianoNoteSwitchingButtons() }
        binding.noteInfo.onLockClicked { openUpgradeScreen() }
        binding.tuningInfoBoxTextView.setDebounceOnClickListener { onInfoBoxClick() }
        binding.pianoKeyboard.onKeyPressedListener = PianoKeyClickListener { key: Int ->
            analyzerWrapper.currentNote = key
            tonePlayer.setCurrentNote(key)
            chartsManager.setCurrentNote(key)
            showPianoNoteSwitchingButtons()
        }
        infoBoxUpdatesHandler.infoBoxTextLiveData.observe(this) { infoBoxText ->
            binding.tuningInfoBoxTextView.text = infoBoxText
        }
        spinnerHandler.spinnersStateLiveData.observe(this) { state ->
            state?.applyTo(binding.tuningWheel.ringViews)
        }
        mainMenu = MainMenu(binding.mainMenu, purchaseStore)
        mainMenu.onItemClickListener = this
        binding.menuButton.setDebounceOnClickListener {
            mainMenu.toggleMenu()
        }
        configurePanels(this, binding.backPanel, binding.topPanel, shouldUseNightMode())
        binding.rootView.setOnTouchListener(object :
            OnSwipeTouchListener(this@MainActivity) {
            override fun onSwipeRight() {
                super.onSwipeRight()
                if (!mainMenu.menuShown) {
                    mainMenu.toggleMenu()
                }
            }
        })
        checkAudioHardware()
        audioRecorder.audioFrameProcessed.observe(this) {
            updatePianoTuningTask.run()
            updateArrowRunnable.run()
            antiTamper.runAntihackingCheck(this@MainActivity)
        }
        onBackPressedDispatcher.addCallback(this, mainMenu.backPressedCallback)
        binding.playToneButton.setDebounceOnClickListener {
            if (tonePlayer.isTonePlayEnabled()) {
                toggleTonePlayer()
            } else {
                InfoDialog.show(
                    this,
                    getString(R.string.dialog_tone_generator_active_title),
                    getString(R.string.dialog_tone_generator_active_message),
                    doNotShowAgain = DoNotShowAgain.Shown("tone_played_enabled_infobox")
                ) {
                    toggleTonePlayer()
                }
            }
        }
        binding.toneLevelsButton.setOnClickListener {
            InfoDialog.show(
                this,
                getString(R.string.dialog_tone_generator_leveling_title),
                getString(R.string.dialog_tone_generator_leveling_message),
                doNotShowAgain = DoNotShowAgain.Shown("tone_levels_dialog_infobox")
            ) {
                showTonePlayerLevelsDialog()
            }
        }
        tonePlayer.setPlayToneDuration(appSettings.tonePlayerDuration)
        binding.playDurationButton.setDebounceOnClickListener {
            InfoDialog.show(
                this,
                getString(R.string.dialog_timed_play_once_title),
                getString(R.string.dialog_timed_play_once_message),
                doNotShowAgain = DoNotShowAgain.Shown("tone_timed_vs_play_once_dialog_infobox")
            ) {
                if (tonePlayer.isEndlessPlaying()) {
                    tonePlayer.setPlayToneDuration(appSettings.tonePlayerDuration)
                } else {
                    tonePlayer.setPlayToneEndlessly()
                }
                updatePlayToneButton()
            }
        }
        binding.playDurationButton.setOnLongClickListener {
            if (!tonePlayer.isEndlessPlaying()) {
                val dialog = ToneDurationDialog(this, durationMs = appSettings.tonePlayerDuration)
                dialog.setOnPositiveClicked { _, _ ->
                    tonePlayer.setPlayToneDuration(dialog.durationMs)
                    appSettings.tonePlayerDuration = dialog.durationMs
                }
                dialog.show()
                return@setOnLongClickListener true
            }
            return@setOnLongClickListener false
        }
        updatePlayToneButton()

        listenToColorScheme()
        listenToPurchaseUpdates()
    }

    private fun listenToPurchaseUpdates() {
        lifecycleScope.launch {
            purchaseStore.purchases.collect {
                binding.playToneButton.isVisible = purchaseStore.isPro
            }
        }
    }

    private fun toggleTonePlayer() {
        if (tonePlayer.isTonePlayEnabled()) {
            tonePlayer.disableTonePlay()
        } else {
            tonePlayer.enableTonePlay()
        }
        updatePlayToneButton()

        val currentTuning = audioRecorder.tuning ?: return
        if (!currentTuning.lock && tonePlayer.isTonePlayEnabled()) {
            lockTuningCurve(true)
        }
    }

    private fun showTonePlayerLevelsDialog() {
        val dialog = TonePlayerLevelingDialog(
            this,
            tonePlayer.trebleBassOptions,
            tonePlayer.volume,
            NoteNames.getNamingConvention(this, appSettings.noteNames)
        )
        dialog.setOnPositiveClicked { dialog, volume, trebleBassOptions ->
            tonePlayer.volume = volume
            tonePlayer.trebleBassOptions = trebleBassOptions
            dialog.dismiss()
        }
        dialog.show()
    }

    private fun updatePlayToneButton() {
        binding.playToneButton.setImageResource(
            when {
                tonePlayer.isTonePlayEnabled() -> R.drawable.ic_button_tone_on
                else -> R.drawable.ic_button_tone_off
            }
        )
        binding.toneLevelsButton.isVisible = tonePlayer.isTonePlayEnabled()
        binding.playDurationButton.isVisible = tonePlayer.isTonePlayEnabled()
        binding.playDurationButton.setImageResource(
            when {
                tonePlayer.isEndlessPlaying() -> R.drawable.ic_button_tone_repeat
                else -> R.drawable.ic_button_tone_duration
            }
        )
    }

    private fun listenToColorScheme() {
        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                val colorScheme = when (appSettings.appearance) {
                    AppCompatDelegate.MODE_NIGHT_UNSPECIFIED -> colorSchemeProvider.value
                    AppCompatDelegate.MODE_NIGHT_YES -> ColorScheme.Dark
                    AppCompatDelegate.MODE_NIGHT_NO -> ColorScheme.Default
                    else -> {
                        val isDarkMode =
                            (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
                        if (isDarkMode) ColorScheme.Dark else ColorScheme.Default
                    }
                }

                Timber.v("Updating color scheme to $colorScheme")
                binding.noteInfo.apply {
                    setTextColor(colorScheme.noteName)
                    setBackgroundColor(colorScheme.noteNameBackground)
                    setBorderColor(colorScheme.innerAndOuterRings)
                }

                binding.tuningWheel.apply {
                    setRingColor(colorScheme.strobeWheels)
                    setRingLabelColor(colorScheme.ringLabelColor)
                    setRingLabelTextColor(colorScheme.ringLabelTextColor)
                    setDialColor(colorScheme.strobeBackground)
                    setDialMarkingsColor(colorScheme.dialMarkings)
                    setOuterRingColor(colorScheme.innerAndOuterRings)
                    setArrowColor(colorScheme.needle)
                }
                binding.currentNotePointer.color = colorScheme.currentNoteIndicator
                binding.pianoKeyboard.selectedKeyColor = colorScheme.currentNoteIndicator
                binding.modesLayout.setColor(
                    if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                        colorScheme.autoStepLockLand
                    } else {
                        colorScheme.autoStepLock
                    }
                )
                chartsManager.updateColors(colorScheme)

                binding.chartsLayout.background =
                    ChartBackgroundDrawable(10.dpToPx(), colorScheme.graphBackground)

                ColorFilter(colorScheme.backPanel).applyTo(binding.backPanel)
                ColorFilter(colorScheme.topPanel).applyTo(binding.topPanel)
                MainUiHelper.prepareTopPanelImage(binding.topPanel)

                mainMenu.updateColors(colorScheme)
            }
        }
    }

    private fun configureLockedNotes() {
        val options = analyzerWrapper.pitchRaiseOptions
        if (options == null || options.mode != PitchRaiseMode.MEASUREMENT) {
            binding.pianoKeyboard.disabledKeysProvider = DisabledKeyProvider { key: Int ->
                return@DisabledKeyProvider !noteLock.isNoteUnlocked(key)
            }
            return
        }
        binding.pianoKeyboard.disabledKeysProvider = DisabledKeyProvider { key: Int ->
            if (!noteLock.isNoteUnlocked(key)) {
                return@DisabledKeyProvider true
            }
            return@DisabledKeyProvider !options.raiseKeys[key - 1]
        }
    }

    private fun checkAudioHardware() {
        checkAudioHardwareAsync(this)
    }

    override fun onStart() {
        super.onStart()
        pitchRaiseConfigManager.onPitchRaiseConfigReadyListener = this
        binding.noteInfo.setNoteNaming(appSettings.noteNames)
        infoBoxUpdatesHandler.start()
        updateUi()
        if (purchaseRestoreTask.shouldRestore()) {
            purchaseRestoreTask.restore()
        }
        this.withAudioPermission()
            .onDenied { finish() }
            .check()
    }

    private fun showFreeVersionNotificationIfNeeded() {
        if (appSettings.freeVersionNotificationShown()) {
            return
        }
        if (isPlus) {
            return
        }
        if (freeVersionNotificationDialog != null) {
            return
        }
        freeVersionNotificationDialog = AlertDialog.Builder(this)
            .setTitle(R.string.app_name)
            .setMessage(R.string.evaluation_version_popup_message)
            .setCancelable(false)
            .setPositiveButton(R.string.action_ok) { dialog: DialogInterface, _: Int ->
                appSettings.setFreeVersionNotificationShown(true)
                dialog.dismiss()
            }
            .create()
        freeVersionNotificationDialog?.setOnDismissListener {
            freeVersionNotificationDialog = null
        }
        freeVersionNotificationDialog?.show()
    }

    override fun onPurchasesUpdated(purchases: List<InAppPurchase>) {
        super.onPurchasesUpdated(purchases)
        updateUi()
        showFreeVersionNotificationIfNeeded()
    }

    private fun updateUi() {
        infoBoxUpdatesHandler.updateNow()
        mainMenu.updateMenu()
        noteLock.setPlus(isPlus)
        configureLockedNotes()
    }

    public override fun onSaveInstanceState(savedInstanceState: Bundle) {
        Timber.d("onSaveInstanceState")
        chartsManager.saveState(savedInstanceState)
        super.onSaveInstanceState(savedInstanceState)
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            if (appSettings.hideNavigationBar) {
                hideSystemUI()
            } else {
                showSystemUI()
            }
        }
    }

    private fun hideSystemUI() {
        val uiMode = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
        window.decorView.systemUiVisibility = uiMode
    }

    private fun showSystemUI() {
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
    }

    private suspend fun createNewTuning(): PianoTuning {
        val pianoTuning = pianoTuningInitializer.initializeNewTuning()
        pianoTuningDataStore.addTuning(pianoTuning)
        appSettings.currentTuningId = pianoTuning.id
        return pianoTuning
    }

    private fun loadOrInitTuning() {
        runBlocking {
            val currentTuningId = appSettings.currentTuningId
            val loadedTuning = if (currentTuningId == null) {
                Timber.d("Creating a new tuning")
                createNewTuning()
            } else {
                try {
                    Timber.d("Loading tuning $currentTuningId")
                    pianoTuningDataStore.getTuning(currentTuningId)
                } catch (e: Exception) {
                    Timber.d("Cannot load latest tuning. Creating new one")
                    createNewTuning()
                }
            }
            audioRecorder.tuning = loadedTuning
            updateTuningData(loadedTuning, true)
        }
    }

    private fun scheduleChartsUpdate(delay: Long) {
        mainHandler.postDelayed(chartsUpdateRunnable, delay)
    }

    // this method is called after onCreate(), also it is called once the settings screen closed.
    override fun onResume() {
        super.onResume()
        Timber.d("onResume")
        keepScreenOn(appSettings.preventSleep)
        tuningStyleHelper.refreshWeights()
        if (audioRecorder.tuning == null) {
            loadOrInitTuning()
        } else {
            updateTuningData(requireNotNull(audioRecorder.tuning), false)
        }
        resumePitchRaise()
        chartsManager.updateAxis(analyzerWrapper.pitchRaiseMode)
        updateCharts()
        Timber.d("Start processing")
        if (this.hasAudioPermission()) {
            audioRecorder.start()
            automaticTuningSaver.start()
            scheduleChartsUpdate(500)
            spinnerHandler.start()
        }
    }

    private fun resumePitchRaise() {
        when (analyzerWrapper.pitchRaiseMode) {
            PitchRaiseMode.MEASUREMENT -> {
                binding.pitchRaiseModeButton.text =
                    getString(R.string.activity_main_start_pitch_raise)
                binding.pitchRaiseModeButton.isVisible = true
                binding.pitchRaiseMeasurementHintTextView.isVisible = true
                binding.lockInharmonicityButton.isVisible = false
                chartsManager.chartsLocked = true
                configureLockedNotes()
            }

            PitchRaiseMode.TUNING -> {
                binding.pitchRaiseModeButton.text =
                    getString(R.string.activity_main_exit_pitch_raise)
                binding.pitchRaiseMeasurementHintTextView.isVisible = false
                binding.pitchRaiseModeButton.isVisible = true
                binding.lockInharmonicityButton.isVisible = true
                chartsManager.chartsLocked = false
                configureLockedNotes()
            }

            else -> {
                chartsManager.chartsLocked = false
                pitchRaiseConfigManager.resumeConfiguration(this)
            }
        }
    }

    private fun scaleFunc(cents: Double): Float {
        val sign = if (cents >= 0) 1 else -1
        val val1 = abs(cents).pow(0.7)
        val val2 = 100.0.pow(0.7)
        return 90.0f * sin(sign * val1 * Math.PI / 2.0 / val2).toFloat()
    }

    private fun updateArrow() {
        try {
            val note = analyzerWrapper.currentNote
            val isOffsetOver = analyzerWrapper.offsetOver
            val centsOffset = analyzerWrapper.centsOffsetCombined

            val isLocked = !noteLock.isNoteUnlocked(note)
            var showValues = !isLocked

            // If pitch raise mode, show only selected keys
            val options = analyzerWrapper.pitchRaiseOptions
            if (options != null && options.mode == PitchRaiseMode.MEASUREMENT &&
                !options.notesToRaise.contains((note - 1) % 12)
            ) {
                showValues = false
            }

            binding.noteInfo.apply {
                setLocked(isLocked)
                setNoteOffset(centsOffset)
                setNote(note)
            }

            binding.pianoKeyboard.selectedKey = note
            binding.currentNotePointer.setNote(note)
            chartsManager.setCurrentNote(note)
            tonePlayer.setCurrentNote(note)
            var dialAngle = 0.0f
            if (analyzerWrapper.pitchRaiseMode == PitchRaiseMode.TUNING) {
                dialAngle = scaleFunc(analyzerWrapper.pitchRaiseData.overpullCents[note - 1])
            }
            binding.tuningWheel.setDialAngle(dialAngle)
            if (isOffsetOver || analyzerWrapper.isTargetLengthCounted || !analyzerWrapper.isQualityTestOk || !showValues) {
                binding.tuningWheel.hideNeedle()
            } else {
                val angle = scaleFunc(centsOffset.toDouble())
                binding.tuningWheel.setArrowAngle(angle)
            }
        } catch (e: Exception) {
            Timber.e(e, "Cannot update arrow")
        }
    }

    private val hideNoteArrowsRunnable = Runnable { setNotesSwitchingButtonsAlpha(0.0f) }
    private fun setNotesSwitchingButtonsAlpha(alpha: Float) {
        binding.prevNoteButton.alpha = alpha
        binding.prevOctaveButton.alpha = alpha
        binding.nextNoteButton.alpha = alpha
        binding.nextOctaveButton.alpha = alpha
    }

    private fun showPianoNoteSwitchingButtons() {
        setNotesSwitchingButtonsAlpha(1.0f)
        mainHandler.removeCallbacks(hideNoteArrowsRunnable)
        // set the handler action to hide the arrows after 5 seconds
        mainHandler.postDelayed(hideNoteArrowsRunnable, 5000)
    }

    override fun onPause() {
        super.onPause()
        Timber.d("onPause")
        spinnerHandler.cancel()
        audioRecorder.pause()
        mainHandler.removeCallbacks(chartsUpdateRunnable)
        automaticTuningSaver.stop()
        audioRecorder.stop()
    }

    override fun onStop() {
        super.onStop()
        Timber.d("onStop")
        tonePlayer.disableTonePlay()
        updatePlayToneButton()
        pitchRaiseConfigManager.onPitchRaiseConfigReadyListener = null
        infoBoxUpdatesHandler.stop()
        audioRecorder.tuning?.let { appSettings.currentTuningId = it.id }
    }

    override fun onDestroy() {
        super.onDestroy()
        chartsManager.onDestroy()
        freeVersionNotificationDialog?.dismiss()
    }

    private val lastUpdateArrowCall = AtomicLong(0)

    private val updateArrowRunnable = Runnable {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastUpdateArrowCall.get() < 16) {
            return@Runnable
        }
        lastUpdateArrowCall.getAndSet(currentTime)
        trace("updateArrow") {
            updateArrow()
        }
    }

    private fun updatePitchRaiseData() {
        val options = analyzerWrapper.pitchRaiseOptions
        if (options != null) {
            val pitchRaiseData = analyzerWrapper.pitchRaiseData
            pitchRaiseData.updateOverpull(
                options,
                audioRecorder.tuning!!,
                appSettings.maximumOverpull.toDouble(),
                appSettings.pitchRaiseOvershootFactor
            )
            if (analyzerWrapper.pitchRaiseMode == PitchRaiseMode.TUNING) {
                analyzerWrapper.setOverpullCents(pitchRaiseData.overpullCents)
            } else {
                analyzerWrapper.setOverpullCents(null)
            }
        } else {
            analyzerWrapper.setOverpullCents(null)
        }
    }

    private fun updateCharts() {
        try {
            updatePitchRaiseData()
            chartsManager.updateCharts(analyzerWrapper)
        } catch (e: Exception) {
            Timber.e(e, "Cannot update charts")
        }
    }

    private fun changeNoteButtonClicked(v: View, change: Int) {
        val currentNote = analyzerWrapper.currentNote
        val newNote = maxOf(1, minOf(88, analyzerWrapper.currentNote + change))
        if (newNote != currentNote) {
            tonePlayer.setCurrentNote(newNote)
            analyzerWrapper.currentNote = newNote
            chartsManager.setCurrentNote(newNote)
            val symbol = if (change < 0) "-" else "+"
            showToast(getString(R.string.status_note_change, symbol + abs(change)), false)
        }
        showPianoNoteSwitchingButtons()
    }

    private fun onLockClick() {
        val currentTuning = audioRecorder.tuning ?: return
        lockTuningCurve(!currentTuning.lock)
    }

    private fun lockTuningCurve(lock: Boolean) {
        val currentTuning = audioRecorder.tuning ?: return
        currentTuning.lock = lock
        analyzerWrapper.setRecalculateTuning(!lock)
        if (lock) {
            binding.lockInharmonicityButton.setImageResource(R.drawable.ic_button_lock)
            updatePianoTuningTask.inharmonicityAndPeakHeightsLocked = true
            showToast(getString(R.string.status_tuning_curve_locked), false)
        } else {
            binding.lockInharmonicityButton.setImageResource(R.drawable.ic_button_ear)
            updatePianoTuningTask.inharmonicityAndPeakHeightsLocked = false
            analyzerWrapper.forceRecalculate()
            showToast(getString(R.string.status_tuning_curve_unlocked), false)
        }
        automaticTuningSaver.saveTuningNowAsync()
    }

    private fun applyNoteChangeMode(noteChangeMode: Int) {
        analyzerWrapper.noteDetectMode = noteChangeMode
        val notificationMessage = when (noteChangeMode) {
            NoteChangeMode.AUTO -> getString(R.string.activity_main_note_switching_automatic)
            NoteChangeMode.STEP -> getString(R.string.activity_main_note_switching_stepwise)
            NoteChangeMode.LOCK -> getString(R.string.activity_main_note_switching_disabled)
            else -> return
        }
        showToast(notificationMessage, false)
    }

    private fun showFrequencyChartCleanupMenu() {
        val anchor = findViewById<View>(R.id.chartsLayout)
        val popupMenu = DeltaChartPopupMenu()
        popupMenu.show(anchor) { _: Int?, _: Int? ->
            clearHarmonicsData()
        }
    }

    private fun showInharmonicityChartCleanupMenu() {
        val currentNote = analyzerWrapper.currentNote
        val anchor = findViewById<View>(R.id.chartsLayout)
        val popupMenu = InharmonicityChartPopupMenu(appSettings)
        popupMenu.show(anchor, currentNote) { _: Int, note: Int ->
            clearInharmonicityData(note - 1)
        }
    }

    private fun clearHarmonicsData() {
        audioRecorder.tuning?.let {
            it.measurements = it.measurements.resetHarmonics()
            analyzerWrapper.harmonics = it.measurements.harmonics
            automaticTuningSaver.saveTuningNowAsync()
            updateTuningData(it, false)
        }
    }

    private fun clearInharmonicityData(note: Int) {
        audioRecorder.tuning?.let {
            it.measurements = it.measurements.resetInharmonicity(note)
            analyzerWrapper.inharmonicity = it.measurements.inharmonicity
            analyzerWrapper.forceRecalculate()
            automaticTuningSaver.saveTuningNowAsync()
        }
    }

    private fun loadTuning(id: String?) {
        if (id == null) {
            Timber.w("Id cannot be null. Tuning loading skipped")
            return
        }
        lifecycleScope.launch {
            kotlin.runCatching {
                val tuning = pianoTuningDataStore.getTuning(id)
                withContext(Dispatchers.Main) {
                    updateTuningData(tuning, true)
                }
            }.onFailure {
                Timber.e(it, "Cannot load tuning")
            }
        }
    }

    private fun updateTuningData(data: PianoTuning, reset: Boolean) {
        // stop processing before continuing
        audioRecorder.pause()
        automaticTuningSaver.setAutomaticSavingActive(false)
        if (reset) {
            endPitchRaise()
            analyzerWrapper.reset()
        }
        audioRecorder.tuning = data
        analyzerWrapper.setInharmonicityWeight(appSettings.inharmonicityWeight.toDouble())
        val currentNote = analyzerWrapper.currentNote
        analyzerWrapper.setData(data.measurements)

        val newCurrentNote = if (reset) ToneDetectorWrapper.DEFAULT_NOTE else currentNote
        analyzerWrapper.currentNote = newCurrentNote
        chartsManager.setCurrentNote(newCurrentNote)

        analyzerWrapper.setTemperament(data.customTemperamentOrDefault.offsets)
        analyzerWrapper.pitchOffsetFactor = data.pitch / 440.0f
        val calibration = 2.0.pow(appSettings.pitchOffset / 1200.0)
        analyzerWrapper.calibrationFactor = calibration
        if (data.forceRecalculateDelta) {
            analyzerWrapper.setRecalculateTuning(true)
            analyzerWrapper.forceRecalculate()
            analyzerWrapper.setRecalculateTuning(!data.lock)
            data.forceRecalculateDelta = false
        } else {
            analyzerWrapper.setRecalculateTuning(!data.lock)
            analyzerWrapper.forceRecalculate()
        }
        binding.lockInharmonicityButton.setImageResource(if (data.lock) R.drawable.ic_button_lock else R.drawable.ic_button_ear)
        appSettings.currentTuningId = data.id
        tuningStyleHelper.setTuningIntervalWeights(data.tuningStyle)
        infoBoxUpdatesHandler.updateNow()

        // restore processing
        updatePianoTuningTask.inharmonicityAndPeakHeightsLocked = data.lock
        automaticTuningSaver.setAutomaticSavingActive(true)
        audioRecorder.resume()
    }

    private fun onPitchRaiseModeButtonClicked() {
        val options = analyzerWrapper.pitchRaiseOptions ?: return
        when (options.mode) {
            PitchRaiseMode.MEASUREMENT -> startPitchRaise(options)
            PitchRaiseMode.TUNING -> endPitchRaise()
        }
    }

    private fun startPitchRaiseMeasurement(pitchRaiseOptions: PitchRaiseOptions) {
        synchronized(analyzerWrapper.pitchRaiseModeLock) {
            analyzerWrapper.startPitchRaiseMeasurement(pitchRaiseOptions)
        }
        binding.pitchRaiseModeButton.text = getString(R.string.activity_main_start_pitch_raise)
        binding.pitchRaiseModeButton.isVisible = true
        binding.pitchRaiseMeasurementHintTextView.isVisible = true
        chartsManager.updateAxis(pitchRaiseOptions.mode)
        chartsManager.setCurrentChart(1, true)
        chartsManager.chartsLocked = true
        binding.lockInharmonicityButton.isVisible = false
        configureLockedNotes()
        updateCharts()
    }

    private fun startPitchRaise(options: PitchRaiseOptions) {
        val fx = analyzerWrapper.fx
        // restore measurement data
        synchronized(analyzerWrapper.pitchRaiseModeLock) { analyzerWrapper.startPitchRaiseTuning() }
        val lastIndex = analyzerWrapper.pitchRaiseData.interpolate(options, fx)
        chartsManager.chartsLocked = false
        if (lastIndex < 0) {
            endPitchRaise()
            showToast(getString(R.string.pitch_raise_error_not_enough_data), true)
            return
        }
        binding.pitchRaiseModeButton.text = getString(R.string.activity_main_exit_pitch_raise)
        binding.lockInharmonicityButton.isVisible = true
        binding.pitchRaiseMeasurementHintTextView.isVisible = false
        var maxOP = 11.355 * ln(analyzerWrapper.bave[21]) + 147.89
        maxOP = maxOf(minOf(35.0, maxOP), 60.0)
        analyzerWrapper.pitchRaiseData.maxOP = maxOP
        chartsManager.updateAxis(analyzerWrapper.pitchRaiseMode)
    }

    private fun endPitchRaise() {
        binding.lockInharmonicityButton.isVisible = true
        analyzerWrapper.stopPitchRaise()
        binding.pitchRaiseModeButton.isVisible = false
        binding.pitchRaiseMeasurementHintTextView.isVisible = false
        chartsManager.updateAxis(analyzerWrapper.pitchRaiseMode)
        analyzerWrapper.forceRecalculate()
        configureLockedNotes()
    }

    private fun openUpgradeScreen() = startActivity(Intent(this, UpgradeActivity::class.java))

    private fun onInfoBoxClick() {
        if (isPlus || isPro) {
            openTuningSettings()
        } else {
            openUpgradeScreen()
        }
    }

    override fun onMainMenuItemClicked(id: Int) {
        when (id) {
            MainMenuItem.ID_NEW_TUNING -> menuNewTuningFileClicked()
            MainMenuItem.ID_OPEN_TUNING -> menuLoadTuningFileClicked()
            MainMenuItem.ID_TUNING_SETTINGS -> menuTuningFileSettingsClicked()
            MainMenuItem.ID_PITCH_RAISE -> menuPitchRaiseClicked()
            MainMenuItem.ID_GLOBAL_SETTINGS -> menuGlobalSettingsClicked()
            MainMenuItem.ID_HELP -> menuHelpClicked()
            MainMenuItem.ID_UPGRADE -> menuUpgradeClicked()
            else -> {
                Timber.w("Unknown menu item %d clicked", id)
                return
            }
        }
    }

    private fun menuNewTuningFileClicked() {
        actionConfigureNewTuning.launch(pianoTuningInitializer.initializeNewTuning())
    }

    private fun menuLoadTuningFileClicked() {
        if (!isPro) {
            openUpgradeScreen()
            return
        }
        actionOpenTuningFile.launch(null)
    }

    private fun menuTuningFileSettingsClicked() {
        openTuningSettings()
    }

    private fun openTuningSettings() {
        val tuning = audioRecorder.tuning ?: return
        actionConfigureExistingTuning.launch(tuning)
    }

    private fun menuPitchRaiseClicked() {
        if (!isPro) {
            openUpgradeScreen()
            return
        }
        pitchRaiseConfigManager.startNewConfiguration(this, audioRecorder.tuning!!)
    }

    private fun menuGlobalSettingsClicked() {
        startActivity(Intent(this, GlobalSettingsActivity::class.java))
    }

    private fun menuHelpClicked() {
        startActivity(Intent(this, HelpActivity::class.java))
    }

    private fun menuUpgradeClicked() {
        if (purchaseStore.isProSubscription) {
            startActivitySafe(this, openPlayStoreSubscriptions(packageName))
        } else {
            startActivity(Intent(this, UpgradeActivity::class.java))
        }
    }

    private fun onConfigureNewTuningResultReceived(tuning: PianoTuning?) {
        if (tuning == null) {
            return
        }
        lifecycleScope.launch {
            val savedTuning = pianoTuningDataStore.addTuning(tuning)
            withContext(Dispatchers.Main) {
                updateTuningData(savedTuning, true)
                chartsManager.setCurrentChart(1, true)
            }
        }
    }

    private fun onConfigureExistingTuningResultReceived(tuning: PianoTuning?) {
        if (tuning == null) {
            return
        }
        updateTuningData(tuning, false)
    }

    private fun onOpenTuningFileResultReceived(tuningId: String?) {
        if (tuningId == null) {
            return
        }

        loadTuning(tuningId)
    }

    override fun onPitchRaiseConfigReady(config: PitchRaiseOptions) {
        startPitchRaiseMeasurement(config)
    }

    override fun onPitchRaiseCancelled() {}
}