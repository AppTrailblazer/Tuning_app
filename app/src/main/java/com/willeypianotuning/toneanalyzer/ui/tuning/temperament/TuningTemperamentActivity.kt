package com.willeypianotuning.toneanalyzer.ui.tuning.temperament

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.InputFilter
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContract
import androidx.lifecycle.lifecycleScope
import com.willeypianotuning.toneanalyzer.R
import com.willeypianotuning.toneanalyzer.billing.InAppPurchase
import com.willeypianotuning.toneanalyzer.databinding.ActivityTuningTemperamentBinding
import com.willeypianotuning.toneanalyzer.extensions.round
import com.willeypianotuning.toneanalyzer.extensions.setCompoundDrawableTop
import com.willeypianotuning.toneanalyzer.extensions.setDebounceOnClickListener
import com.willeypianotuning.toneanalyzer.store.TemperamentDataStore
import com.willeypianotuning.toneanalyzer.store.db.temperaments.Temperament
import com.willeypianotuning.toneanalyzer.store.db.temperaments.Temperament.Companion.EQUAL
import com.willeypianotuning.toneanalyzer.ui.commons.BaseActivity
import com.willeypianotuning.toneanalyzer.ui.upgrade.UpgradeActivity
import com.willeypianotuning.toneanalyzer.utils.decimal.AppInputDecimalFormat
import com.willeypianotuning.toneanalyzer.utils.decimal.DecimalInputFilter
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@AndroidEntryPoint
class TuningTemperamentActivity : BaseActivity() {

    @Inject
    lateinit var temperamentDataStore: TemperamentDataStore

    @Inject
    lateinit var saveTemperamentAsync: SaveTemperamentAsyncUseCase

    private lateinit var temperament: Temperament

    private val decimalInputFilter: InputFilter = DecimalInputFilter()

    private lateinit var binding: ActivityTuningTemperamentBinding

    private val offsetsInputFields: List<EditText>
        get() = listOf(
            binding.aNoteTemperamentOffset,
            binding.aSharpNoteTemperamentOffset,
            binding.bNoteTemperamentOffset,
            binding.cNoteTemperamentOffset,
            binding.cSharpNoteTemperamentOffset,
            binding.dNoteTemperamentOffset,
            binding.dSharpNoteTemperamentOffset,
            binding.eNoteTemperamentOffset,
            binding.fNoteTemperamentOffset,
            binding.fSharpNoteTemperamentOffset,
            binding.gNoteTemperamentOffset,
            binding.gSharpNoteTemperamentOffset
        )

    private var offsetChangeWatcherDisabled = false
    private val offsetChangeWatcher: TextWatcher = object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
            if (!temperament.mutable || offsetChangeWatcherDisabled) {
                return
            }
            temperament = temperament.copy(offsets = formatOffsetsFromForm())
            updateChartView(temperament)
        }

        override fun afterTextChanged(s: Editable) {}
    }

    private val actionLoadTuningTemperament = registerForActivityResult(
        LoadTuningTemperamentActivity.Contract(),
        ::onLoadTemperamentResultReceived
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTuningTemperamentBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupToolbar()
        setTitle(R.string.activity_tuning_temperament_title)

        binding.temperamentNameEditText.setOnClickListener {
            if (!temperament.mutable) {
                onLoadTemperamentClicked()
            }
        }
        val offsetFilters = arrayOf(decimalInputFilter)
        for (editText in offsetsInputFields) {
            editText.hint = AppInputDecimalFormat.formatDouble(0.0)
            editText.filters = offsetFilters
            editText.addTextChangedListener(offsetChangeWatcher)
        }
        binding.temperamentChartView.setDebounceOnClickListener { onSwitchTemperamentCommaClicked() }
        binding.loadButton.setDebounceOnClickListener { onLoadTemperamentClicked() }
        binding.newButton.setDebounceOnClickListener { onNewTemperamentButtonClicked() }
        binding.saveButton.setDebounceOnClickListener { onSaveButtonClicked() }
        binding.deleteButton.setDebounceOnClickListener { onDeleteButtonClicked() }

        val restoredTemperament = savedInstanceState?.getParcelable(EXTRA_TEMPERAMENT)
            ?: intent.getParcelableExtra<Temperament>(EXTRA_TEMPERAMENT)
        if (restoredTemperament == null) {
            Timber.d("Cannot load temperament. Piano tuning is null")
            finish()
            return
        } else {
            temperament = Temperament(restoredTemperament)
        }
        updateView(temperament)
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                saveTemperament()
                finish()
            }
        })
    }

    private fun onSwitchTemperamentCommaClicked() {
        if (!temperament.mutable) {
            return
        }
        temperament = temperament.copy(
            comma = when (temperament.comma) {
                "PC" -> "SC"
                else -> "PC"
            }
        )
        updateChartView(temperament)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        if (binding.temperamentNameEditText.text.toString().isNotEmpty()) {
            temperament = temperament.copy(name = binding.temperamentNameEditText.text.toString())
        }
        super.onSaveInstanceState(outState)
        outState.putParcelable(EXTRA_TEMPERAMENT, temperament)
    }

    private fun onSaveButtonClicked() {
        if (!checkFormValid()) {
            return
        }
        saveTemperament()
    }

    private fun onDeleteButtonClicked() {
        if (!temperament.mutable) {
            return
        }
        lifecycleScope.launch {
            kotlin.runCatching {
                withContext(Dispatchers.IO) {
                    temperamentDataStore.deleteTemperament(temperament)
                }
                Timber.d("Temperament is deleted")
                withContext(Dispatchers.Main) {
                    temperament = Temperament(EQUAL)
                    updateView(temperament)
                }
            }.onFailure {
                Timber.e(it, "Cannot delete temperaments")
            }
        }
    }

    override fun onResume() {
        super.onResume()
        updateUi()
    }

    override fun onPurchasesUpdated(purchases: List<InAppPurchase>) {
        super.onPurchasesUpdated(purchases)
        updateUi()
    }

    private fun updateUi() {
        if (isPlus) {
            binding.loadButton.setCompoundDrawableTop(R.drawable.ic_files_open)
            binding.newButton.setCompoundDrawableTop(R.drawable.ic_files_new)
        } else {
            binding.loadButton.setCompoundDrawableTop(R.drawable.ic_lock)
            binding.newButton.setCompoundDrawableTop(R.drawable.ic_lock)
        }
        val wasEnabled = binding.aNoteTemperamentOffset.isEnabled
        if (!wasEnabled) {
            updateView(temperament)
        }
    }

    private fun checkFieldContainsNumber(field: EditText): Boolean {
        val value = AppInputDecimalFormat.parseDouble(field.text.toString())
        field.error = if (value == null) {
            getString(R.string.error_not_a_number)
        } else {
            null
        }
        return value != null
    }

    private fun checkFormValid(): Boolean {
        var valid = true
        for (offsetField in offsetsInputFields) {
            valid = valid && checkFieldContainsNumber(offsetField)
        }
        if (binding.temperamentNameEditText.text.toString().trim { it <= ' ' }.isEmpty()) {
            binding.temperamentNameEditText.error = getString(R.string.error_name_cannot_be_empty)
            valid = false
        }
        return valid
    }

    private fun formatOffsetsFromForm(): DoubleArray {
        return offsetsInputFields.map { field ->
            AppInputDecimalFormat.parseDouble(field.text.toString()) ?: 0.0
        }.toDoubleArray()
    }

    private fun setActivityResult(temperament: Temperament) {
        val data = Intent()
        data.putExtra(EXTRA_TEMPERAMENT, temperament)
        setResult(Activity.RESULT_OK, data)
    }

    private fun saveTemperament() {
        if (!temperament.hasOffsets()) {
            setActivityResult(EQUAL)
            return
        }
        if (!temperament.mutable) {
            setActivityResult(temperament)
            return
        }
        if (binding.temperamentNameEditText.text.toString().isNotEmpty()) {
            temperament = temperament.copy(name = binding.temperamentNameEditText.text.toString())
        }
        setActivityResult(temperament)
        saveTemperamentAsync(temperament)
    }

    private fun onNewTemperamentButtonClicked() {
        if (!isPlus) {
            startActivity(Intent(this, UpgradeActivity::class.java))
            return
        }
        val offsets = EQUAL.offsets.copyOf(12)
        val date = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date())
        temperament = Temperament("custom $date", "Custom $date", "", null, "PC", offsets, true)
        updateView(temperament)
    }

    private fun updateView(temperament: Temperament) {
        val enabled = temperament.mutable && isPlus
        binding.temperamentNameEditText.apply {
            isFocusable = enabled
            isFocusableInTouchMode = enabled
            isLongClickable = enabled
            setText(temperament.name)
        }
        for (field in offsetsInputFields) {
            field.isEnabled = enabled
        }
        offsetChangeWatcherDisabled = true
        offsetsInputFields.forEachIndexed { index, editText ->
            editText.setText(
                String.format(
                    Locale.getDefault(),
                    "%.2f",
                    temperament.offsets[index].round(2)
                )
            )
        }
        offsetChangeWatcherDisabled = false
        updateChartView(temperament)
        binding.saveButton.visibility = if (temperament.mutable) View.VISIBLE else View.GONE
        binding.deleteButton.visibility = if (temperament.mutable) View.VISIBLE else View.GONE
    }

    private fun updateChartView(temperament: Temperament) {
        binding.temperamentChartView.setFifths(temperament.fifths)
        when (temperament.comma) {
            "PC" -> binding.temperamentChartView.setComma("P.C.")
            "SC" -> binding.temperamentChartView.setComma("S.C.")
            else -> binding.temperamentChartView.setComma("")
        }
        binding.temperamentChartView.setFractions(temperament.fractions)
        binding.temperamentChartView.setThirds(temperament.thirds)
        binding.temperamentChartView.setDrawInnerLines(true)
    }

    private fun onLoadTemperamentResultReceived(temperament: Temperament?) {
        if (temperament == null) {
            return
        }
        this.temperament = temperament
        updateView(temperament)
    }

    private fun onLoadTemperamentClicked() {
        if (!isPlus) {
            startActivity(Intent(this, UpgradeActivity::class.java))
            return
        }
        actionLoadTuningTemperament.launch(null)
    }

    companion object {
        private const val EXTRA_TEMPERAMENT = "temperament"
    }

    class Contract : ActivityResultContract<Temperament, Temperament?>() {
        override fun createIntent(context: Context, input: Temperament): Intent {
            return Intent(context, TuningTemperamentActivity::class.java).apply {
                putExtra(EXTRA_TEMPERAMENT, input)
            }
        }

        override fun parseResult(resultCode: Int, intent: Intent?): Temperament? {
            if (resultCode != Activity.RESULT_OK || intent == null) {
                return null
            }
            return intent.getParcelableExtra(EXTRA_TEMPERAMENT)
        }
    }
}