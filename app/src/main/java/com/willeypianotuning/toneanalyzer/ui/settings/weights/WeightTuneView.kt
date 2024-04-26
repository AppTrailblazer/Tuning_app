package com.willeypianotuning.toneanalyzer.ui.settings.weights

import android.content.Context
import android.graphics.Color
import android.text.InputType
import android.util.AttributeSet
import android.util.DisplayMetrics
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.SeekBar
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.AppCompatTextView
import com.willeypianotuning.toneanalyzer.R
import com.willeypianotuning.toneanalyzer.utils.SubmitDialogOnDoneEditorActionListener
import com.willeypianotuning.toneanalyzer.utils.SubmitDialogOnEnterPressKeyListener
import com.willeypianotuning.toneanalyzer.utils.decimal.AppInputDecimalFormat
import com.willeypianotuning.toneanalyzer.utils.decimal.DecimalInputFilter

class WeightTuneView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private val titleTextView: AppCompatTextView
    private val seekBar: SeekBar
    private val seekBarValueTextView: AppCompatTextView

    var title: String
        get() {
            return titleTextView.text.toString()
        }
        set(value) {
            titleTextView.text = value
        }

    private var _minValue: Float = 0.0f
    private var _maxValue: Float = 1.0f
    private var _currentValue: Float = 0.5f
    private var _valueStepSize: Float = 0.1f

    var valuePrecision: Int = 2
    var valueFormatter: ValueFormatter = DefaultValueFormatter()

    private var editDialogTitle: String

    var valueTextClickOpensTextInput: Boolean = true
    var minValue: Float
        get() = _minValue
        set(value) {
            assert(value < _maxValue)
            _minValue = value
            _currentValue = maxOf(_minValue, _currentValue)
            updateSeekBar()
        }
    var maxValue: Float
        get() = _maxValue
        set(value) {
            assert(value > minValue)
            _maxValue = value
            _currentValue = minOf(_maxValue, _currentValue)
            updateSeekBar()
        }
    var currentValue: Float
        get() = _currentValue
        set(value) {
            _currentValue = minOf(maxOf(value, _minValue), _maxValue)
            updateSeekBar()
        }
    var valueStepSize: Float
        get() = _valueStepSize
        set(value) {
            assert(value < _maxValue - _minValue)
            _valueStepSize = value
            updateSeekBar()
        }

    var onValueChangeListener: OnValueChangeListener? = null

    private val seekBarChangeListener = object : SeekBar.OnSeekBarChangeListener {
        private var initialValue: Int? = 0

        override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
            if (fromUser) {
                val oldValue = _currentValue
                _currentValue = _minValue + progress * _valueStepSize
                onValueChangeListener?.onValueChanged(oldValue, _currentValue, true)
            }
            updateSeekBarValueText()
        }

        override fun onStartTrackingTouch(seekBar: SeekBar) {
            initialValue = seekBar.progress
        }

        override fun onStopTrackingTouch(seekBar: SeekBar) {
            onValueChangeListener?.onEditFinished()
        }
    }

    init {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL

        editDialogTitle = context.getString(R.string.dialog_change_interval_weight_value_title)

        titleTextView = AppCompatTextView(context)
        titleTextView.textDirection = View.TEXT_DIRECTION_LOCALE
        titleTextView.textAlignment = View.TEXT_ALIGNMENT_GRAVITY
        val titleParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
        addView(titleTextView, titleParams)

        seekBar = SeekBar(context)
        val seekBarParams = LayoutParams(0, LayoutParams.WRAP_CONTENT)
        seekBarParams.weight = 1f
        addView(seekBar, seekBarParams)

        seekBar.setOnSeekBarChangeListener(seekBarChangeListener)

        seekBarValueTextView = AppCompatTextView(context)
        val valueParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
        addView(seekBarValueTextView, valueParams)

        seekBarValueTextView.setOnClickListener { onValueTextClicked() }

        if (attrs != null) {
            processAttributes(attrs)
        }
    }

    private fun processAttributes(attrs: AttributeSet?) {
        val ta = context.obtainStyledAttributes(attrs, R.styleable.WeightTuneView)
        val pxPerDp = context.resources.displayMetrics.densityDpi / DisplayMetrics.DENSITY_DEFAULT

        titleTextView.setTextSize(
            TypedValue.COMPLEX_UNIT_PX,
            ta.getDimensionPixelSize(R.styleable.WeightTuneView_titleTextSize, 14 * pxPerDp)
                .toFloat()
        )
        titleTextView.setTextColor(
            ta.getColor(
                R.styleable.WeightTuneView_titleTextColor,
                Color.WHITE
            )
        )
        titleTextView.text = ta.getText(R.styleable.WeightTuneView_title)

        seekBarValueTextView.setTextSize(
            TypedValue.COMPLEX_UNIT_PX,
            ta.getDimensionPixelSize(R.styleable.WeightTuneView_valueTextSize, 12 * pxPerDp)
                .toFloat()
        )
        seekBarValueTextView.setTextColor(
            ta.getColor(
                R.styleable.WeightTuneView_valueTextColor,
                Color.WHITE
            )
        )

        _minValue = ta.getFloat(R.styleable.WeightTuneView_minValue, _minValue)
        _maxValue = ta.getFloat(R.styleable.WeightTuneView_maxValue, _maxValue)
        _currentValue = ta.getFloat(R.styleable.WeightTuneView_currentValue, _currentValue)
        _valueStepSize = ta.getFloat(R.styleable.WeightTuneView_valueStepSize, _valueStepSize)
        valuePrecision = ta.getInteger(R.styleable.WeightTuneView_valuePrecision, valuePrecision)
        editDialogTitle = ta.getString(R.styleable.WeightTuneView_editDialogTitle)
            ?: editDialogTitle

        updateSeekBar()

        ta.recycle()
    }

    private fun onValueTextClicked() {
        if (!valueTextClickOpensTextInput) {
            return
        }

        val builder = AlertDialog.Builder(context)
        builder.setTitle(editDialogTitle)
        val layout = LinearLayout(context)
        val dpToPx = resources.displayMetrics.densityDpi.toFloat() / DisplayMetrics.DENSITY_DEFAULT
        layout.setPadding(
            (16 * dpToPx).toInt(),
            (4 * dpToPx).toInt(),
            (16 * dpToPx).toInt(),
            (4 * dpToPx).toInt()
        )
        val input = EditText(context).apply {
            setText(AppInputDecimalFormat.formatDouble(_currentValue.toDouble()))
            maxLines = 1
            isSingleLine = true
            filters = arrayOf(DecimalInputFilter(minValue < 0, valuePrecision))
            setSelectAllOnFocus(true)
            imeOptions = EditorInfo.IME_ACTION_DONE
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }
        var inputType = InputType.TYPE_CLASS_NUMBER + InputType.TYPE_NUMBER_FLAG_DECIMAL
        if (minValue < 0) {
            inputType += InputType.TYPE_NUMBER_FLAG_SIGNED
        }
        input.inputType = inputType

        layout.addView(input)
        builder.setView(layout)
        builder.setPositiveButton(context.getString(R.string.action_ok)) { _, _ ->
            val value = AppInputDecimalFormat.parseDouble(input.text.toString())?.toFloat()
                ?: return@setPositiveButton
            val oldValue = _currentValue
            currentValue = value
            onValueChangeListener?.onValueChanged(oldValue, _currentValue, true)
            updateSeekBar()
        }
        builder.setNegativeButton(context.getString(R.string.action_cancel)) { dialog, _ -> dialog.cancel() }

        val dialog = builder.show()
        input.setOnKeyListener(SubmitDialogOnEnterPressKeyListener(dialog))
        input.setOnEditorActionListener(SubmitDialogOnDoneEditorActionListener(dialog))
        dialog.window!!.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
    }

    override fun setEnabled(enabled: Boolean) {
        super.setEnabled(enabled)
        seekBar.isEnabled = enabled
        seekBarValueTextView.isEnabled = enabled
    }

    private fun updateSeekBar() {
        val range = _maxValue - _minValue
        val steps = (range / _valueStepSize).toInt()

        seekBar.max = steps
        seekBar.progress = ((_currentValue - _minValue) / _valueStepSize).toInt()
        updateSeekBarValueText()
    }

    private fun updateSeekBarValueText() {
        seekBarValueTextView.text = valueFormatter.format(_currentValue, valuePrecision)
    }

    interface OnValueChangeListener {
        fun onValueChanged(oldValue: Float, newValue: Float, fromUser: Boolean)
        fun onEditFinished()
    }

    interface ValueFormatter {
        fun parse(value: String): Float?
        fun format(value: Float, precision: Int): String
    }

    private class DefaultValueFormatter : ValueFormatter {
        override fun parse(value: String): Float? {
            return value.toFloatOrNull()
        }

        override fun format(value: Float, precision: Int): String {
            return if (precision > 0) {
                ("%.${precision}f").format(value)
            } else {
                "%d".format(value.toInt())
            }
        }
    }
}