package com.willeypianotuning.toneanalyzer.ui.settings.weights

import android.os.Bundle
import android.util.DisplayMetrics
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.willeypianotuning.toneanalyzer.R
import com.willeypianotuning.toneanalyzer.ToneDetectorWrapper
import com.willeypianotuning.toneanalyzer.audio.TuningStyleHelper
import com.willeypianotuning.toneanalyzer.databinding.ActivityAdjustWeightsBinding
import com.willeypianotuning.toneanalyzer.store.TuningStyleDataStore
import com.willeypianotuning.toneanalyzer.store.db.tuning_styles.IntervalWeights
import com.willeypianotuning.toneanalyzer.store.db.tuning_styles.TuningStyle
import com.willeypianotuning.toneanalyzer.ui.commons.BaseActivity
import com.willeypianotuning.toneanalyzer.ui.settings.weights.chart.TuningCurveChartManager
import com.willeypianotuning.toneanalyzer.ui.settings.weights.list.LoadTuningStyleActivity
import com.willeypianotuning.toneanalyzer.utils.SubmitDialogOnDoneEditorActionListener
import com.willeypianotuning.toneanalyzer.utils.SubmitDialogOnEnterPressKeyListener
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.cachapa.expandablelayout.ExpandableLayout
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import javax.inject.Inject

@AndroidEntryPoint
class AdjustWeightsActivity : BaseActivity() {

    private val octaveSubViews: Array<WeightTuneView> by lazy {
        arrayOf(
            binding.weightTuneViewOctave21,
            binding.weightTuneViewOctave42,
            binding.weightTuneViewOctave63,
            binding.weightTuneViewOctave84,
            binding.weightTuneViewOctave105
        )
    }

    private val twelfthSubViews: Array<WeightTuneView> by lazy {
        arrayOf(
            binding.weightTuneViewTwelfth31,
            binding.weightTuneViewTwelfth62,
            binding.weightTuneViewTwelfth93
        )
    }

    private val doubleOctaveSubViews: Array<WeightTuneView> by lazy {
        arrayOf(
            binding.weightTuneViewDoubleOctave41,
            binding.weightTuneViewDoubleOctave82
        )
    }

    private val nineteenthSubViews: Array<WeightTuneView> by lazy {
        arrayOf(
            binding.weightTuneViewNineteenth61
        )
    }

    private val tripleOctaveSubViews: Array<WeightTuneView> by lazy {
        arrayOf(
            binding.weightTuneViewTripleOctave81
        )
    }

    private val fifthSubViews: Array<WeightTuneView> by lazy {
        arrayOf(
            binding.weightTuneViewFifth32,
            binding.weightTuneViewFifth64
        )
    }

    private val fourthSubViews: Array<WeightTuneView> by lazy {
        arrayOf(
            binding.weightTuneViewFourth43,
            binding.weightTuneViewFourth86
        )
    }

    private val extraTrebleStretchSubViews: Array<WeightTuneView> by lazy {
        arrayOf(
            binding.weightTuneViewExtraTrebleStretchCentsPerOctave,
            binding.weightTuneViewExtraTrebleStretchOctaves
        )
    }

    private val extraBassStretchSubViews: Array<WeightTuneView> by lazy {
        arrayOf(
            binding.weightTuneViewExtraBassStretchCentsPerOctave,
            binding.weightTuneViewExtraBassStretchOctaves
        )
    }

    private val currentOctaveWeights: DoubleArray
        get() = octaveSubViews.map { 1.0 / it.currentValue }.toDoubleArray()
    private val currentTwelfthWeights: DoubleArray
        get() = twelfthSubViews.map { 1.0 / it.currentValue }.toDoubleArray()
    private val currentDoubleOctaveWeights: DoubleArray
        get() = doubleOctaveSubViews.map { 1.0 / it.currentValue }.toDoubleArray()
    private val currentNineteenthWeights: DoubleArray
        get() = nineteenthSubViews.map { 1.0 / it.currentValue }.toDoubleArray()
    private val currentTripleOctaveWeights: DoubleArray
        get() = tripleOctaveSubViews.map { 1.0 / it.currentValue }.toDoubleArray()
    private val currentFifthWeights: DoubleArray
        get() = fifthSubViews.map { 1.0 / it.currentValue }.toDoubleArray()
    private val currentFourthWeights: DoubleArray
        get() = fourthSubViews.map { 1.0 / it.currentValue }.toDoubleArray()
    private val currentExtraTrebleStretch: DoubleArray
        get() = extraTrebleStretchSubViews.map { it.currentValue.toDouble() }.toDoubleArray()
    private val currentExtraBassStretch: DoubleArray
        get() = extraBassStretchSubViews.map { it.currentValue.toDouble() }.toDoubleArray()

    private lateinit var tuningCurveChartManager: TuningCurveChartManager

    @Inject
    lateinit var analyzerWrapper: ToneDetectorWrapper

    @Inject
    lateinit var tuningStyleDs: TuningStyleDataStore

    @Inject
    lateinit var tuningStyleHelper: TuningStyleHelper

    private var tuningStyle = TuningStyle.DEFAULT

    private val actionLoadTuningStyle = registerForActivityResult(
        LoadTuningStyleActivity.Contract(),
        ::onLoadTuningStyleResultReceived
    )

    private lateinit var historyManager: TuningStyleHistoryManager
    private lateinit var binding: ActivityAdjustWeightsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAdjustWeightsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupToolbar()
        setTitle(R.string.activity_adjust_weights_title)

        tuningCurveChartManager = TuningCurveChartManager(binding.tuningCurveChart, appSettings)

        tuningStyle = savedInstanceState?.getParcelable(KEY_INTERVAL_WEIGHTS)
            ?: tuningStyleHelper.getTemporalIntervalWeights()
                    ?: tuningStyleHelper.getGlobalIntervalWeights()
        historyManager = TuningStyleHistoryManager(tuningStyle.copy())
        savedInstanceState?.let {
            historyManager.restoreInstanceState(it)
        }
        applyIntervalWeights(tuningStyle, permanent = false)
        tuningCurveChartManager.setNewTuningCurve(analyzerWrapper.delta)

        applyIntervalWeights(TuningStyle.DEFAULT, permanent = false)
        val fx = analyzerWrapper.fx
        val delta = analyzerWrapper.delta
        applyIntervalWeights(tuningStyle, permanent = false)

        tuningCurveChartManager.setOriginalTuningCurve(delta)
        tuningCurveChartManager.setFrequencies(fx, delta)
        tuningCurveChartManager.intervalWidth =
            analyzerWrapper.getIntervalWidths(appSettings.useCents)
        binding.tuningStyleLeftAxisTitle.setText(
            if (appSettings.useCents) R.string.symbol_cents else R.string.symbol_hertz
        )

        val state = savedInstanceState ?: Bundle()
        configureExpansion(
            R.id.listItemWeightsOctave,
            R.id.listItemWeightsOctaveDropDown,
            R.id.listItemWeightsOctaveExpanded,
            state.getBoolean(KEY_EXPANDED_OCTAVE, false)
        )
        configureExpansion(
            R.id.listItemWeightsTwelfth,
            R.id.listItemWeightsTwelfthDropDown,
            R.id.listItemWeightsTwelfthExpanded,
            state.getBoolean(KEY_EXPANDED_TWELFTH, false)
        )
        configureExpansion(
            R.id.listItemWeightsDoubleOctave,
            R.id.listItemWeightsDoubleOctaveDropDown,
            R.id.listItemWeightsDoubleOctaveExpanded,
            state.getBoolean(KEY_EXPANDED_DOUBLE_OCTAVE, false)
        )
        configureExpansion(
            R.id.listItemWeightsNineteenth,
            R.id.listItemWeightsNineteenthDropDown,
            R.id.listItemWeightsNineteenthExpanded,
            state.getBoolean(KEY_EXPANDED_NINETEENTH, false)
        )
        configureExpansion(
            R.id.listItemWeightsTripleOctave,
            R.id.listItemWeightsTripleOctaveDropDown,
            R.id.listItemWeightsTripleOctaveExpanded,
            state.getBoolean(KEY_EXPANDED_TRIPLE_OCTAVE, false)
        )
        configureExpansion(
            R.id.listItemWeightsFifth,
            R.id.listItemWeightsFifthDropDown,
            R.id.listItemWeightsFifthExpanded,
            state.getBoolean(KEY_EXPANDED_FIFTH, false)
        )
        configureExpansion(
            R.id.listItemWeightsFourth,
            R.id.listItemWeightsFourthDropDown,
            R.id.listItemWeightsFourthExpanded,
            state.getBoolean(KEY_EXPANDED_FOURTH, false)
        )
        configureExpansion(
            R.id.listItemWeightsExtraTrebleStretch,
            R.id.listItemWeightsExtraTrebleStretchDropDown,
            R.id.listItemWeightsExtraTrebleStretchExpanded,
            state.getBoolean(KEY_EXPANDED_TREBLE_STRETCH, false)
        )
        configureExpansion(
            R.id.listItemWeightsExtraBassStretch,
            R.id.listItemWeightsExtraBassStretchDropDown,
            R.id.listItemWeightsExtraBassStretchExpanded,
            state.getBoolean(KEY_EXPANDED_BASS_STRETCH, false)
        )
        updateIntervalWidthGraph()

        initializeWeightsGroup(
            binding.weightTuneViewOctave,
            octaveSubViews,
            tuningStyle.intervalWeights.octave
        )
        initializeWeightsGroup(
            binding.weightTuneViewTwelfth,
            twelfthSubViews,
            tuningStyle.intervalWeights.twelfth
        )
        initializeWeightsGroup(
            binding.weightTuneViewDoubleOctave,
            doubleOctaveSubViews,
            tuningStyle.intervalWeights.doubleOctave
        )
        initializeWeightsGroup(
            binding.weightTuneViewNineteenth,
            nineteenthSubViews,
            tuningStyle.intervalWeights.nineteenth
        )
        initializeWeightsGroup(
            binding.weightTuneViewTripleOctave,
            tripleOctaveSubViews,
            tuningStyle.intervalWeights.tripleOctave
        )
        initializeWeightsGroup(
            binding.weightTuneViewFifth,
            fifthSubViews,
            tuningStyle.intervalWeights.fifth
        )
        initializeWeightsGroup(
            binding.weightTuneViewFourth,
            fourthSubViews,
            tuningStyle.intervalWeights.fourth
        )
        binding.weightTuneViewExtraBassStretchOctaves.valueFormatter = OctavesValueFormatter()
        binding.weightTuneViewExtraTrebleStretchOctaves.valueFormatter = OctavesValueFormatter()
        initializeStretchGroup(
            extraTrebleStretchSubViews,
            tuningStyle.intervalWeights.extraTrebleStretch
        )
        initializeStretchGroup(
            extraBassStretchSubViews,
            tuningStyle.intervalWeights.extraBassStretch
        )

        binding.actionLoad.setOnClickListener { loadIntervalWeights() }
        binding.actionAdd.setOnClickListener { addIntervalWeights() }
        binding.actionSave.setOnClickListener { saveIntervalWeights() }
        binding.actionDelete.setOnClickListener { deleteIntervalWeights() }
        binding.actionReset.setOnClickListener { resetToDefaults() }
        binding.tuningStyleNameLabel.setOnClickListener { renameTuningStyle() }
        binding.tuningStyleLeftAxisTitle.setOnClickListener { toggleCentsHertz() }
        updateUi()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_tuning_style, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        val switchCentsHertz = menu?.findItem(R.id.actionSwitchCentsHertz)
        switchCentsHertz?.setTitle(
            if (appSettings.useCents) R.string.activity_tuning_style_action_use_hertz else R.string.activity_tuning_style_action_use_cents
        )
        return super.onPrepareOptionsMenu(menu)
    }

    private fun toggleCentsHertz() {
        val useCents = !appSettings.useCents
        Timber.d("Switching tuning style graph to use ${if (useCents) "Cents" else "Hertz"}")
        appSettings.useCents = useCents
        tuningCurveChartManager.intervalWidth = analyzerWrapper.getIntervalWidths(useCents)
        binding.tuningStyleLeftAxisTitle.setText(
            if (useCents) R.string.symbol_cents else R.string.symbol_hertz
        )
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.actionSwitchCentsHertz -> toggleCentsHertz()
            else -> return super.onOptionsItemSelected(item)
        }
        return true
    }

    private fun applyIntervalWeights(weights: TuningStyle, permanent: Boolean) {
        if (permanent) {
            tuningStyleHelper.setTemporalIntervalWeights(null)
            tuningStyleHelper.setGlobalIntervalWeights(weights)
        } else {
            tuningStyleHelper.setTemporalIntervalWeights(weights)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean(KEY_EXPANDED_OCTAVE, binding.listItemWeightsOctaveExpanded.isExpanded)
        outState.putBoolean(
            KEY_EXPANDED_DOUBLE_OCTAVE,
            binding.listItemWeightsDoubleOctaveExpanded.isExpanded
        )
        outState.putBoolean(
            KEY_EXPANDED_TRIPLE_OCTAVE,
            binding.listItemWeightsTripleOctaveExpanded.isExpanded
        )
        outState.putBoolean(KEY_EXPANDED_FIFTH, binding.listItemWeightsFifthExpanded.isExpanded)
        outState.putBoolean(KEY_EXPANDED_FOURTH, binding.listItemWeightsFourthExpanded.isExpanded)
        outState.putBoolean(KEY_EXPANDED_TWELFTH, binding.listItemWeightsTwelfthExpanded.isExpanded)
        outState.putBoolean(
            KEY_EXPANDED_NINETEENTH,
            binding.listItemWeightsNineteenthExpanded.isExpanded
        )
        outState.putBoolean(
            KEY_EXPANDED_TREBLE_STRETCH,
            binding.listItemWeightsExtraTrebleStretchExpanded.isExpanded
        )
        outState.putBoolean(
            KEY_EXPANDED_BASS_STRETCH,
            binding.listItemWeightsExtraBassStretchExpanded.isExpanded
        )
        outState.putParcelable(KEY_INTERVAL_WEIGHTS, tuningStyle)
        historyManager.saveInstanceState(outState)
        super.onSaveInstanceState(outState)
    }

    private fun renameTuningStyle() {
        if (!tuningStyle.mutable) {
            return
        }

        val builder = AlertDialog.Builder(this)
        builder.setTitle(getString(R.string.activity_tuning_styles_rename_tuning_style_dialog_title))
        val layout = LinearLayout(this)
        val dpToPx = resources.displayMetrics.densityDpi.toFloat() / DisplayMetrics.DENSITY_DEFAULT
        layout.setPadding(
            (16 * dpToPx).toInt(),
            (4 * dpToPx).toInt(),
            (16 * dpToPx).toInt(),
            (4 * dpToPx).toInt()
        )
        val input = EditText(this)
        input.setText(tuningStyle.name)
        input.setSelectAllOnFocus(true)
        input.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )

        layout.addView(input)
        builder.setView(layout)
        builder.setPositiveButton(getString(R.string.action_ok)) { _, _ ->
            tuningStyle = tuningStyle.copy(name = input.text.toString())
            historyManager.onModified(tuningStyle.copy())
            updateTuningStyleLabel()
            updateUndoButton()
        }
        builder.setNegativeButton(getString(R.string.action_cancel)) { dialog, _ -> dialog.cancel() }

        val dialog = builder.show()

        input.isSingleLine = true
        input.imeOptions = EditorInfo.IME_ACTION_DONE
        input.setOnKeyListener(SubmitDialogOnEnterPressKeyListener(dialog))
        input.setOnEditorActionListener(SubmitDialogOnDoneEditorActionListener(dialog))
        dialog.window!!.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
    }

    private fun loadIntervalWeights() {
        actionLoadTuningStyle.launch(null)
    }

    private fun addIntervalWeights() {
        val date = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date())
        val defaultWeights = tuningStyleHelper.getGlobalIntervalWeights()
        val newWeights = TuningStyle(
            UUID.randomUUID().toString(),
            name = "Custom $date",
            mutable = true,
            intervalWeights = defaultWeights.intervalWeights.copy()
        )
        tuningStyle = newWeights
        historyManager = TuningStyleHistoryManager(tuningStyle.copy())
        tuningStyleHelper.setTemporalIntervalWeights(tuningStyle)
        updateUi()
    }

    private fun saveIntervalWeights() {
        if (!tuningStyle.mutable) {
            return
        }

        val newWeights = tuningStyle.copy(
            intervalWeights = IntervalWeights(
                currentOctaveWeights,
                currentTwelfthWeights,
                currentDoubleOctaveWeights,
                currentNineteenthWeights,
                currentTripleOctaveWeights,
                currentFifthWeights,
                currentFourthWeights,
                currentExtraTrebleStretch,
                currentExtraBassStretch
            )
        )
        writeWeightsToFile(newWeights)
    }

    private fun writeWeightsToFile(newWeights: TuningStyle, close: Boolean = true) {
        lifecycleScope.launch {
            kotlin.runCatching {
                val style = withContext(Dispatchers.IO) {
                    tuningStyleDs.addStyle(newWeights)
                }
                withContext(Dispatchers.Main) {
                    tuningStyle = style
                    applyIntervalWeights(tuningStyle, permanent = true)
                    tuningStyleHelper.setTuningIntervalWeights(tuningStyle)
                    updateUi()
                    if (close) {
                        Toast.makeText(
                            this@AdjustWeightsActivity,
                            R.string.message_tuning_style_implemented,
                            Toast.LENGTH_LONG
                        ).show()
                        finish()
                    }
                }
            }.onFailure {
                Timber.e(it, "Cannot save tuning style")
            }
        }
    }

    private fun deleteIntervalWeights() {
        if (!tuningStyle.mutable) {
            return
        }
        lifecycleScope.launch {
            kotlin.runCatching {
                tuningStyleDs.deleteStyle(tuningStyle)
                withContext(Dispatchers.Main) {
                    tuningStyle = TuningStyle.DEFAULT
                    historyManager = TuningStyleHistoryManager(tuningStyle)
                    applyIntervalWeights(tuningStyle, permanent = true)
                    appSettings.tuningStyleId = tuningStyle.id
                    updateUi()
                }
            }.onFailure {
                Timber.e(it, "Cannot delete tuning style")
            }
        }
    }

    private fun updateTuningStyleLabel() {
        binding.tuningStyleNameLabel.text = tuningStyle.name
    }

    private fun updateUi() {
        val mutable = tuningStyle.mutable
        octaveSubViews.forEach { it.isEnabled = mutable }
        binding.weightTuneViewOctave.isEnabled = mutable
        twelfthSubViews.forEach { it.isEnabled = mutable }
        binding.weightTuneViewTwelfth.isEnabled = mutable
        doubleOctaveSubViews.forEach { it.isEnabled = mutable }
        binding.weightTuneViewDoubleOctave.isEnabled = mutable
        nineteenthSubViews.forEach { it.isEnabled = mutable }
        binding.weightTuneViewNineteenth.isEnabled = mutable
        tripleOctaveSubViews.forEach { it.isEnabled = mutable }
        binding.weightTuneViewTripleOctave.isEnabled = mutable
        fifthSubViews.forEach { it.isEnabled = mutable }
        binding.weightTuneViewFifth.isEnabled = mutable
        fourthSubViews.forEach { it.isEnabled = mutable }
        binding.weightTuneViewFourth.isEnabled = mutable
        extraTrebleStretchSubViews.forEach { it.isEnabled = mutable }
        extraBassStretchSubViews.forEach { it.isEnabled = mutable }

        setWeightsForGroup(
            binding.weightTuneViewOctave,
            octaveSubViews,
            tuningStyle.intervalWeights.octave
        )
        setWeightsForGroup(
            binding.weightTuneViewTwelfth,
            twelfthSubViews,
            tuningStyle.intervalWeights.twelfth
        )
        setWeightsForGroup(
            binding.weightTuneViewDoubleOctave,
            doubleOctaveSubViews,
            tuningStyle.intervalWeights.doubleOctave
        )
        setWeightsForGroup(
            binding.weightTuneViewNineteenth,
            nineteenthSubViews,
            tuningStyle.intervalWeights.nineteenth
        )
        setWeightsForGroup(
            binding.weightTuneViewTripleOctave,
            tripleOctaveSubViews,
            tuningStyle.intervalWeights.tripleOctave
        )
        setWeightsForGroup(
            binding.weightTuneViewFifth,
            fifthSubViews,
            tuningStyle.intervalWeights.fifth
        )
        setWeightsForGroup(
            binding.weightTuneViewFourth,
            fourthSubViews,
            tuningStyle.intervalWeights.fourth
        )
        setWeightsForStretchGroup(
            extraTrebleStretchSubViews,
            tuningStyle.intervalWeights.extraTrebleStretch
        )
        setWeightsForStretchGroup(
            extraBassStretchSubViews,
            tuningStyle.intervalWeights.extraBassStretch
        )

        updateTuningStyleLabel()

        tuningCurveChartManager.originalTuningCurveDashed = tuningStyle.mutable
        updateTuningCurveForNewWeights()

        binding.actionSave.visibility = if (mutable) View.VISIBLE else View.GONE
        binding.actionDelete.visibility = if (mutable) View.VISIBLE else View.GONE
        binding.actionReset.visibility = if (mutable) View.VISIBLE else View.GONE
        updateUndoButton()
    }

    private fun updateUndoButton() {
        binding.actionReset.isEnabled = historyManager.canRestore()
        val drawable =
            DrawableCompat.wrap(
                AppCompatResources.getDrawable(
                    this,
                    R.drawable.ic_reset_white_24dp
                )!!
            )
        if (!historyManager.canRestore()) {
            DrawableCompat.setTint(
                drawable,
                ContextCompat.getColor(this, R.color.action_menu_icon_color_disabled)
            )
        } else {
            DrawableCompat.setTint(
                drawable,
                ContextCompat.getColor(this, R.color.action_menu_icon_color_active)
            )
        }
        binding.actionReset.setCompoundDrawablesWithIntrinsicBounds(null, drawable, null, null)
    }

    private fun resetToDefaults() {
        if (historyManager.canRestore()) {
            val lastState = historyManager.restore()!!
            tuningStyle = lastState
            tuningStyleHelper.setTemporalIntervalWeights(lastState)
            Timber.d("Restoring previous state: $lastState")
            updateUi()
        }
    }

    private fun onLoadTuningStyleResultReceived(tuningStyle: TuningStyle?) {
        if (tuningStyle == null) {
            return
        }
        this.tuningStyle = tuningStyle
        historyManager = TuningStyleHistoryManager(tuningStyle)
        applyIntervalWeights(tuningStyle, permanent = true)
        appSettings.tuningStyleId = tuningStyle.id
        updateUi()
    }

    private fun updateIntervalWidthGraph() {
        binding.tuningStyleLeftAxisTitle.isVisible = binding.listItemWeightsOctaveExpanded.isExpanded
                || binding.listItemWeightsTwelfthExpanded.isExpanded
                || binding.listItemWeightsFifthExpanded.isExpanded
                || binding.listItemWeightsFourthExpanded.isExpanded
                || binding.listItemWeightsDoubleOctaveExpanded.isExpanded
                || binding.listItemWeightsTripleOctaveExpanded.isExpanded
                || binding.listItemWeightsNineteenthExpanded.isExpanded
        tuningCurveChartManager.drawIntervalWidth(
            binding.listItemWeightsOctaveExpanded.isExpanded,
            binding.listItemWeightsTwelfthExpanded.isExpanded,
            binding.listItemWeightsFifthExpanded.isExpanded,
            binding.listItemWeightsFourthExpanded.isExpanded,
            binding.listItemWeightsDoubleOctaveExpanded.isExpanded,
            binding.listItemWeightsTripleOctaveExpanded.isExpanded,
            binding.listItemWeightsNineteenthExpanded.isExpanded
        )
    }

    private fun setWeightsForGroup(
        type: WeightTuneView,
        subTypes: Array<WeightTuneView>,
        subTypesWeights: DoubleArray
    ) {
        for (i in subTypes.indices) {
            subTypes[i].currentValue = (1.0 / subTypesWeights[i]).toFloat()
        }
        type.currentValue = subTypes.maxOf { it.currentValue }
    }

    private fun initializeWeightsGroup(
        type: WeightTuneView,
        subTypes: Array<WeightTuneView>,
        subTypesWeights: DoubleArray
    ) {
        setWeightsForGroup(type, subTypes, subTypesWeights)
        configureChangesOfSubWeightsFor(type, subTypes)
        val changeListener = object : WeightTuneView.OnValueChangeListener {
            override fun onValueChanged(oldValue: Float, newValue: Float, fromUser: Boolean) {
                type.currentValue = subTypes.maxOf { it.currentValue }
                updateTuningCurveForNewWeights()
            }

            override fun onEditFinished() {
                historyManager.onModified(tuningStyle.copy())
                updateUndoButton()
            }
        }
        type.valueTextClickOpensTextInput = false
        subTypes.forEach {
            it.onValueChangeListener = changeListener
            it.valueTextClickOpensTextInput = true
        }
    }

    private fun initializeStretchGroup(
        subTypes: Array<WeightTuneView>,
        subTypesWeights: DoubleArray
    ) {
        setWeightsForStretchGroup(subTypes, subTypesWeights)
        val changeListener = object : WeightTuneView.OnValueChangeListener {
            override fun onValueChanged(oldValue: Float, newValue: Float, fromUser: Boolean) {
                updateTuningCurveForNewWeights()
            }

            override fun onEditFinished() {
                historyManager.onModified(tuningStyle.copy())
                updateUndoButton()
            }
        }
        subTypes.forEach {
            it.onValueChangeListener = changeListener
            it.valueTextClickOpensTextInput = true
        }
    }

    private fun setWeightsForStretchGroup(
        subTypes: Array<WeightTuneView>,
        subTypesWeights: DoubleArray
    ) {
        for (i in subTypes.indices) {
            subTypes[i].currentValue = subTypesWeights[i].toFloat()
        }
    }

    private fun hasUnsavedChanges(): Boolean {
        val weights = tuningStyleHelper.getGlobalIntervalWeights()
        if (!weights.mutable) {
            return false
        }

        return !weights.intervalWeights.octave.contentEquals(currentOctaveWeights)
                || !weights.intervalWeights.twelfth.contentEquals(currentTwelfthWeights)
                || !weights.intervalWeights.doubleOctave.contentEquals(currentDoubleOctaveWeights)
                || !weights.intervalWeights.nineteenth.contentEquals(currentNineteenthWeights)
                || !weights.intervalWeights.tripleOctave.contentEquals(currentTripleOctaveWeights)
                || !weights.intervalWeights.fifth.contentEquals(currentFifthWeights)
                || !weights.intervalWeights.fourth.contentEquals(currentFourthWeights)
                || !weights.intervalWeights.extraTrebleStretch.contentEquals(
            currentExtraTrebleStretch
        )
                || !weights.intervalWeights.extraBassStretch.contentEquals(currentExtraBassStretch)
                || weights.name != binding.tuningStyleNameLabel.text.toString()
    }

    private fun updateTuningCurveForNewWeights() {
        tuningStyle = tuningStyle.copy(
            intervalWeights = IntervalWeights(
                currentOctaveWeights,
                currentTwelfthWeights,
                currentDoubleOctaveWeights,
                currentNineteenthWeights,
                currentTripleOctaveWeights,
                currentFifthWeights,
                currentFourthWeights,
                currentExtraTrebleStretch,
                currentExtraBassStretch
            )
        )
        applyIntervalWeights(tuningStyle, permanent = false)
        val delta = analyzerWrapper.delta
        tuningCurveChartManager.setNewTuningCurve(delta)
        tuningCurveChartManager.intervalWidth =
            analyzerWrapper.getIntervalWidths(appSettings.useCents)
    }

    private fun configureChangesOfSubWeightsFor(
        main: WeightTuneView,
        subWeights: Array<WeightTuneView>
    ) {
        main.onValueChangeListener = object : WeightTuneView.OnValueChangeListener {
            override fun onValueChanged(oldValue: Float, newValue: Float, fromUser: Boolean) {
                val change = newValue / oldValue
                subWeights.forEach {
                    it.currentValue = it.currentValue * change
                }
                updateTuningCurveForNewWeights()
            }

            override fun onEditFinished() {
                historyManager.onModified(tuningStyle.copy())
                updateUndoButton()
            }
        }
    }

    private fun configureExpansion(layout: Int, dropDown: Int, expandable: Int, expanded: Boolean) {
        val parentLayout = findViewById<RelativeLayout>(layout)
        val expandableLayout = findViewById<ExpandableLayout>(expandable)
        expandableLayout.isExpanded = expanded
        val dropDownButton = findViewById<ImageButton>(dropDown)
        dropDownButton.setImageResource(
            if (expandableLayout.isExpanded) {
                R.drawable.ic_arrow_drop_up_white_24dp
            } else {
                R.drawable.ic_arrow_drop_down_white_24dp
            }
        )
        parentLayout.setOnClickListener {
            dropDownButton.performClick()
        }
        dropDownButton.setOnClickListener {
            if (expandableLayout.isExpanded) {
                expandableLayout.collapse(true)
                dropDownButton.setImageResource(R.drawable.ic_arrow_drop_down_white_24dp)
            } else {
                expandableLayout.expand(true)
                dropDownButton.setImageResource(R.drawable.ic_arrow_drop_up_white_24dp)
            }
            updateIntervalWidthGraph()
        }
    }

    private fun showUnsavedChangesDialog() {
        AlertDialog.Builder(this)
            .setTitle(R.string.app_name)
            .setMessage(getString(R.string.message_unsaved_tuning_style_changes))
            .setPositiveButton(getString(R.string.unsaved_changes_action_save)) { dialog, _ ->
                saveIntervalWeights()
                dialog.dismiss()
            }
            .setNegativeButton(getString(R.string.unsaved_changes_action_discard)) { dialog, _ ->
                dialog.dismiss()
                finish()
            }
            .show()
    }

    override fun onBackPressed() {
        if (hasUnsavedChanges()) {
            showUnsavedChangesDialog()
        } else {
            super.onBackPressed()
            applyIntervalWeights(tuningStyle, permanent = true)
            tuningStyleHelper.setTuningIntervalWeights(tuningStyle)
            Toast.makeText(this, R.string.message_tuning_style_implemented, Toast.LENGTH_LONG)
                .show()
        }
    }

    companion object {
        private const val KEY_INTERVAL_WEIGHTS = "IntervalWeights"
        private const val KEY_EXPANDED_OCTAVE = "OctaveExpanded"
        private const val KEY_EXPANDED_DOUBLE_OCTAVE = "DoubleOctaveExpanded"
        private const val KEY_EXPANDED_TRIPLE_OCTAVE = "TripleOctaveExpanded"
        private const val KEY_EXPANDED_FIFTH = "FifthExpanded"
        private const val KEY_EXPANDED_FOURTH = "FourthExpanded"
        private const val KEY_EXPANDED_TWELFTH = "TwelfthExpanded"
        private const val KEY_EXPANDED_NINETEENTH = "NineteenthExpanded"
        private const val KEY_EXPANDED_TREBLE_STRETCH = "TrebleStretchExpanded"
        private const val KEY_EXPANDED_BASS_STRETCH = "BassStretchExpanded"
    }

}