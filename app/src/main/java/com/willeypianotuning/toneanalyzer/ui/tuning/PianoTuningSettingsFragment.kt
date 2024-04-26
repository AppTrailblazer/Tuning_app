package com.willeypianotuning.toneanalyzer.ui.tuning

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.text.SpannableString
import android.text.Spanned
import android.text.TextPaint
import android.text.TextUtils
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.util.DisplayMetrics
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.willeypianotuning.toneanalyzer.AppSettings
import com.willeypianotuning.toneanalyzer.R
import com.willeypianotuning.toneanalyzer.audio.PitchRaiseOptions
import com.willeypianotuning.toneanalyzer.audio.TuningStyleHelper
import com.willeypianotuning.toneanalyzer.audio.note_names.NoteNames
import com.willeypianotuning.toneanalyzer.billing.PurchaseStore
import com.willeypianotuning.toneanalyzer.databinding.FragmentTuningSettingsBinding
import com.willeypianotuning.toneanalyzer.extensions.setBackgroundResourceCompat
import com.willeypianotuning.toneanalyzer.store.db.temperaments.Temperament
import com.willeypianotuning.toneanalyzer.store.db.tuning_styles.TuningStyle
import com.willeypianotuning.toneanalyzer.store.db.tunings.PianoTuning
import com.willeypianotuning.toneanalyzer.ui.pitch_raise.PianoTypeDialog
import com.willeypianotuning.toneanalyzer.ui.settings.PitchOffsetDialog
import com.willeypianotuning.toneanalyzer.ui.settings.weights.list.LoadTuningStyleActivity
import com.willeypianotuning.toneanalyzer.ui.tuning.temperament.TuningTemperamentActivity
import com.willeypianotuning.toneanalyzer.ui.upgrade.UpgradeActivity
import com.willeypianotuning.toneanalyzer.utils.SubmitDialogOnDoneEditorActionListener
import com.willeypianotuning.toneanalyzer.utils.SubmitDialogOnEnterPressKeyListener
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class PianoTuningSettingsFragment : Fragment() {

    private var _binding: FragmentTuningSettingsBinding? = null
    private val binding: FragmentTuningSettingsBinding get() = requireNotNull(_binding)

    private val pianoTypeNames by lazy { resources.getStringArray(R.array.piano_types) }
    private val pianoTypeImages by lazy {
        intArrayOf(
            R.drawable.ic_piano_concert_grand,
            R.drawable.ic_piano_medium_grand,
            R.drawable.ic_piano_baby_grand,
            R.drawable.ic_piano_full_upright,
            R.drawable.ic_piano_studio_upright,
            R.drawable.ic_piano_console,
            R.drawable.ic_piano_spinet,
            R.drawable.ic_piano_other,
            R.drawable.ic_piano_noname
        )
    }

    @Inject
    lateinit var appSettings: AppSettings

    @Inject
    lateinit var tuningStyleHelper: TuningStyleHelper

    @Inject
    lateinit var purchaseStore: PurchaseStore

    private val noteNamingConvention by lazy {
        NoteNames.getNamingConvention(requireContext(), appSettings.noteNames)
    }

    private val viewModel: PianoTuningSettingsViewModel by activityViewModels()

    private val currentTuning: PianoTuning get() = requireNotNull(viewModel.tuning.value)

    private val actionSelectTemperament = registerForActivityResult(
        TuningTemperamentActivity.Contract(),
        ::onLoadTemperamentResultReceived
    )

    private val actionSelectTuningStyle = registerForActivityResult(
        LoadTuningStyleActivity.Contract(),
        ::onLoadTuningStyleResultReceived
    )

    private fun requireCallback(): Callback {
        return requireActivity() as Callback
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_tuning_settings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentTuningSettingsBinding.bind(view)

        binding.listItemTuningSettingMake.setOnClickListener { onMakeClicked() }
        binding.listItemTuningSettingModel.setOnClickListener { onModelClicked() }
        binding.listItemTuningSettingCustomerName.setOnClickListener { onCustomerNameClicked() }
        binding.listItemTuningSettingSerial.setOnClickListener { onSerialClicked() }
        binding.listItemTuningSettingType.setOnClickListener { onTypeClicked() }
        binding.listItemTuningSettingTypeImage.setBackgroundResourceCompat(R.drawable.piano_type_item_bg_pressed)
        binding.listItemTuningSettingPitchOffset.setOnClickListener {
            onPitchOffsetClicked(
                PitchOffsetDialog.SELECTION_NONE
            )
        }
        binding.listItemTuningSettingTemperament.setOnClickListener { onTemperamentClicked() }
        binding.listItemTuningSettingTuningStyle.setOnClickListener { onTuningStyleClicked() }
        binding.listItemTuningSettingLowestUnwoundString.setOnClickListener { onLowerUnwoundStringClicked() }
        binding.listItemTuningSettingNotes.setOnClickListener { onNotesClicked() }

        viewLifecycleOwner.lifecycleScope.launch {
            purchaseStore.purchases.collect {
                updateUi(viewModel.tuning.value)
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.tuning.collect {
                updateUi(it)
            }
        }
    }

    private fun createPitchRaiseSpan(selection: Int): ClickableSpan {
        return object : ClickableSpan() {
            override fun updateDrawState(ds: TextPaint) {
            }

            override fun onClick(widget: View) {
                onPitchOffsetClicked(selection)
            }
        }
    }

    private fun formatPitchOffsetValue(tuning: PianoTuning) {
        val pitchOffsetHz = String.format(
            "%s= %s",
            noteNamingConvention.noteName(0),
            getString(R.string.pitch_offset_summary_hz, tuning.pitch)
        )
        val pitchOffsetCents = getString(
            R.string.pitch_offset_summary_cents,
            PitchOffsetDialog.convertFreqToCents(tuning.pitch)
        )
        val separator = "   "
        val spannableString = SpannableString("$pitchOffsetHz$separator$pitchOffsetCents")

        spannableString.setSpan(
            createPitchRaiseSpan(PitchOffsetDialog.SELECTION_HERTZ),
            0, pitchOffsetHz.length,
            Spanned.SPAN_INCLUSIVE_EXCLUSIVE
        )
        spannableString.setSpan(
            createPitchRaiseSpan(PitchOffsetDialog.SELECTION_CENTS),
            pitchOffsetHz.length + separator.length, spannableString.length,
            Spanned.SPAN_INCLUSIVE_EXCLUSIVE
        )

        binding.listItemTuningSettingPitchOffsetValue.isClickable = true
        binding.listItemTuningSettingPitchOffsetValue.linksClickable = true
        binding.listItemTuningSettingPitchOffsetValue.movementMethod =
            LinkMovementMethod.getInstance()
        binding.listItemTuningSettingPitchOffsetValue.text = spannableString
    }

    private fun updateUi(tuning: PianoTuning) {
        binding.listItemTuningSettingMakeValue.text = tuning.make
        binding.listItemTuningSettingModelValue.text = tuning.model
        binding.listItemTuningSettingCustomerNameValue.text = tuning.name
        binding.listItemTuningSettingSerialValue.text = tuning.serial

        binding.listItemTuningSettingTypeValue.text = pianoTypeNames[tuning.type]
        binding.listItemTuningSettingTypeImage.setImageResource(pianoTypeImages[tuning.type])

        formatPitchOffsetValue(tuning)
        binding.listItemTuningSettingPitchOffsetLock.visibility =
            if (purchaseStore.isPlus) View.GONE else View.VISIBLE

        binding.listItemTuningSettingTemperamentValue.text = tuning.temperament?.name
            ?: getString(R.string.temperament_equal)

        binding.listItemTuningSettingTuningStyleValue.text = tuning.tuningStyle?.name
            ?: getString(
                R.string.tuning_style_default,
                tuningStyleHelper.getGlobalIntervalWeights().name
            )
        binding.listItemTuningSettingTuningStyleLock.visibility =
            if (purchaseStore.isPro) View.GONE else View.VISIBLE

        binding.listItemTuningSettingLowestUnwoundStringValue.text = if (tuning.tenorBreak == -1)
            " "
        else
            noteNamingConvention.pianoNoteName(tuning.tenorBreak - 1)

        binding.listItemTuningSettingNotesValue.text = tuning.notes
    }

    private fun getDialogTitle(label: TextView): String {
        val title = label.text.toString()
        val indexOfColon = title.indexOf(":")
        return if (indexOfColon >= 0) {
            title.substring(0, indexOfColon)
        } else {
            title
        }
    }

    private fun onMakeClicked() {
        showSimplePopup(
            binding.listItemTuningSettingMakeValue,
            POSITION_MAKE,
            getDialogTitle(binding.listItemTuningSettingMakeLabel),
            currentTuning.make
        )
    }

    private fun onModelClicked() {
        showSimplePopup(
            binding.listItemTuningSettingModelValue,
            POSITION_MODEL,
            getDialogTitle(binding.listItemTuningSettingModelLabel),
            currentTuning.model
        )
    }

    private fun onCustomerNameClicked() {
        showNamePopup(
            binding.listItemTuningSettingCustomerNameValue,
            getDialogTitle(binding.listItemTuningSettingCustomerNameLabel)
        )
    }

    private fun onSerialClicked() {
        showSimplePopup(
            binding.listItemTuningSettingSerialValue,
            POSITION_SERIAL,
            getDialogTitle(binding.listItemTuningSettingSerialLabel),
            currentTuning.serial
        )
    }

    private fun onTypeClicked() {
        val dialog = PianoTypeDialog(
            requireActivity(),
            currentTuning.type
        )
        dialog.onPianoTypeChangeListener = PianoTypeDialog.OnPianoTypeChangeListener { pianoType ->
            currentTuning.type = pianoType
            requireCallback().onTuningChanged(currentTuning)
        }
        dialog.show()
    }

    private fun onPitchOffsetClicked(selection: Int) {
        if (purchaseStore.isPlus) {
            showPitchPopup(selection)
        } else {
            startActivity(Intent(requireActivity(), UpgradeActivity::class.java))
        }
    }

    private fun onTemperamentClicked() {
        actionSelectTemperament.launch(currentTuning.customTemperamentOrDefault)
    }

    private fun onTuningStyleClicked() {
        if (purchaseStore.isPro) {
            actionSelectTuningStyle.launch(null)
        } else {
            startActivity(Intent(requireContext(), UpgradeActivity::class.java))
        }
    }

    private fun onLowerUnwoundStringClicked() {
        showTenorPopup(
            binding.listItemTuningSettingLowestUnwoundStringValue,
            getDialogTitle(binding.listItemTuningSettingLowestUnwoundStringLabel)
        )
    }

    private fun onNotesClicked() {
        showSimplePopup(
            binding.listItemTuningSettingNotesValue,
            POSITION_NOTE,
            getDialogTitle(binding.listItemTuningSettingNotesLabel),
            currentTuning.notes
        )
    }

    private fun showNamePopup(txt: TextView, title: String) {
        val layout = LinearLayout(requireContext())
        val dpToPx = resources.displayMetrics.densityDpi.toFloat() / DisplayMetrics.DENSITY_DEFAULT
        layout.setPadding(
            (16 * dpToPx).toInt(),
            (4 * dpToPx).toInt(),
            (16 * dpToPx).toInt(),
            (4 * dpToPx).toInt()
        )

        val inputField = EditText(requireContext()).apply {
            setText(currentTuning.name)
            setSelectAllOnFocus(true)
            isSingleLine = true
            imeOptions = EditorInfo.IME_ACTION_DONE
            inputType = InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }

        layout.addView(inputField)

        val dialog = AlertDialog.Builder(requireActivity())
            .setTitle(title)
            .setNegativeButton(getString(R.string.action_cancel), null)
            .setPositiveButton(getString(R.string.action_ok), null)
            .setView(layout)
            .create()

        inputField.setOnKeyListener(SubmitDialogOnEnterPressKeyListener(dialog))
        inputField.setOnEditorActionListener(SubmitDialogOnDoneEditorActionListener(dialog))

        dialog.setOnShowListener {
            val okButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            okButton.setOnClickListener {
                val name = inputField.text.toString()

                if (TextUtils.isEmpty(name)) {
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.error_name_cannot_be_empty),
                        Toast.LENGTH_SHORT
                    ).show()
                    return@setOnClickListener
                }

                requireCallback().onTuningChanged(currentTuning.copy(name = name))
                txt.text = name
                dialog.dismiss()
            }
        }
        dialog.show()
        inputField.requestFocus()
        dialog.window!!.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)

    }

    private fun onLoadTemperamentResultReceived(temperament: Temperament?) {
        if (temperament == null) {
            return
        }
        requireCallback().onTuningChanged(currentTuning.copy(temperament = temperament))
    }

    private fun onLoadTuningStyleResultReceived(tuningStyle: TuningStyle?) {
        if (tuningStyle == null) {
            return
        }
        requireCallback().onTuningChanged(currentTuning.copy(tuningStyle = tuningStyle))
    }

    private fun showSimplePopup(txt: TextView, position: Int, title: String, oldValue: String) {
        val builder = AlertDialog.Builder(requireActivity())
        builder.setTitle(title)

        val dpToPx = resources.displayMetrics.densityDpi.toFloat() / DisplayMetrics.DENSITY_DEFAULT
        val layout = LinearLayout(requireContext()).apply {
            setPadding(
                (16 * dpToPx).toInt(),
                (4 * dpToPx).toInt(),
                (16 * dpToPx).toInt(),
                (4 * dpToPx).toInt()
            )
        }

        val inputField = EditText(requireContext()).apply {
            setText(oldValue)
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }
        if (position == POSITION_MAKE) {
            inputField.inputType = InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
        }

        layout.addView(inputField)
        builder.setView(layout)
        builder.setPositiveButton(getString(R.string.action_ok)) { _, _ ->
            val out = inputField.text.toString()
            txt.text = out

            val newTuning = when (position) {
                POSITION_MAKE -> currentTuning.copy(make = out)
                POSITION_MODEL -> currentTuning.copy(model = out)
                POSITION_CUSTOMER_NAME -> currentTuning.copy(name = out)
                POSITION_SERIAL -> currentTuning.copy(serial = out)
                POSITION_NOTE -> currentTuning.copy(notes = out)
                else -> currentTuning
            }
            requireCallback().onTuningChanged(newTuning)
        }
        builder.setNegativeButton(getString(R.string.action_cancel)) { dialog, _ -> dialog.cancel() }

        val dialog = builder.show()
        if (position == POSITION_NOTE) {
            inputField.setLines(4)
            inputField.setSelection(inputField.text.length)
        } else {
            if (position == POSITION_SERIAL) {
                // shows text and numbers keyboard
                inputField.inputType = InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            }
            inputField.setSelectAllOnFocus(true)
            inputField.isSingleLine = true
            inputField.imeOptions = EditorInfo.IME_ACTION_DONE
            inputField.setOnKeyListener(SubmitDialogOnEnterPressKeyListener(dialog))
            inputField.setOnEditorActionListener(SubmitDialogOnDoneEditorActionListener(dialog))
        }
        inputField.requestFocus()
        dialog.window!!.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
    }

    private fun showTenorPopup(txt: TextView, title: String) {
        val builder = AlertDialog.Builder(requireActivity())
        builder.setTitle(title)

        val items = Array(PitchRaiseOptions.TENOR_BREAK_LENGTH) { i ->
            noteNamingConvention.pianoNoteName(i + PitchRaiseOptions.TENOR_BREAK_START - 1)
        }

        var ind = currentTuning.tenorBreak - PitchRaiseOptions.TENOR_BREAK_START
        if (ind < 0 || ind >= PitchRaiseOptions.TENOR_BREAK_LENGTH) {
            ind = PitchRaiseOptions.LOWEST_UNWOUND_DEFAULT - PitchRaiseOptions.TENOR_BREAK_START
        }
        builder.setSingleChoiceItems(items, ind) { dialog, which ->
            txt.text = items[which]
            requireCallback().onTuningChanged(currentTuning.copy(tenorBreak = which + PitchRaiseOptions.TENOR_BREAK_START))
            dialog.dismiss()
        }

        builder.show()
    }

    private fun showPitchPopup(selection: Int = PitchOffsetDialog.SELECTION_NONE) {
        val pitchOffsetDialog = PitchOffsetDialog(requireActivity(), selection)
        pitchOffsetDialog.onValueEnteredListener =
            PitchOffsetDialog.OnValueEnteredListener { pitch, _ ->
                requireCallback().onTuningChanged(currentTuning.copy(pitch = pitch))
            }
        pitchOffsetDialog.show(
            getDialogTitle(binding.listItemTuningSettingPitchOffsetLabel),
            null,
            currentTuning.pitch
        )
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    interface Callback {
        fun onTuningChanged(tuning: PianoTuning)
    }

    companion object {
        const val POSITION_MAKE = 0
        const val POSITION_MODEL = 1
        const val POSITION_CUSTOMER_NAME = 2
        const val POSITION_SERIAL = 3
        const val POSITION_NOTE = 9
    }

}